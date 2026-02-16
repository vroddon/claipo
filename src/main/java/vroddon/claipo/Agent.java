package vroddon.claipo;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class Agent {

    private final JFrame frame;
    private final JButton btnGenerate;
    private final JTextField txtHint;

    private final GeminiSimpleChat chat;

    public Agent() {
        this.chat = new GeminiSimpleChat();

        // Normal, movable window; keep always-on-top if you like it
        frame = new JFrame("claipo");
        frame.setAlwaysOnTop(true);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setIconImage(new ImageIcon(getClass().getResource("/icon.png")).getImage());        

        // Content: one-line hint field (center) + small icon-only button (right)
        JPanel content = new JPanel(new BorderLayout(6, 6));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // One-line text field (single line by default); columns ~ give compact width
        txtHint = new JTextField();
        txtHint.setColumns(24);
        txtHint.setToolTipText("Optional hint (e.g., \"meeting tomorrow\")");

        // Small, icon-only button on the right
        btnGenerate = new JButton("📨");
        btnGenerate.setMargin(new Insets(2, 6, 2, 6)); // keep it small
        btnGenerate.setFocusPainted(false);
        btnGenerate.setToolTipText("Generate reply and copy to clipboard");
        btnGenerate.addActionListener(this::onGenerate);

        content.add(txtHint, BorderLayout.CENTER);
        content.add(btnGenerate, BorderLayout.EAST);

        frame.setContentPane(content);
        frame.pack();

        // Position near top-right
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screen.width - frame.getWidth() - 50;
        int y = 40;
        frame.setLocation(x, y);
    }

    public void showUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(frame);
        } catch (Exception ignored) { }
        frame.setVisible(true);
    }

    private void onGenerate(ActionEvent e) {
        String text = readClipboardText();
        if (text == null || text.trim().isEmpty()) {
            beep("Clipboard is empty or not text.");
            return;
        }

        // Build prompt; must be final for usage inside SwingWorker
        final String base =
                "You are Victor and you have to answer this email thread. " +
                "Be concise, not too formal. ";
        final String hint = txtHint.getText().trim();
        final String prompt = hint.isEmpty()
                ? base
                : base + "The user added this hint: " + hint + ". ";

        setBusy(true);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                // NOTE: prompt is final (as requested)
                return chat.chat(prompt + text);
            }

            @Override
            protected void done() {
                try {
                    String reply = get();
                    if (reply != null && !reply.trim().isEmpty()) {
                        writeClipboardText(reply);
                        tooltip("Copied ✓");
                    } else {
                        beep("Empty model response.");
                    }
                } catch (Exception ex) {
                    beep("Error: " + ex.getMessage());
                } finally {
                    setBusy(false);
                }
            }
        };

        worker.execute();
    }

    private void setBusy(boolean busy) {
        btnGenerate.setEnabled(!busy);
        btnGenerate.setText(busy ? "…" : "📨");
        frame.setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                             : Cursor.getDefaultCursor());
    }

    private void tooltip(String message) {
        final JDialog tip = new JDialog(frame, false);
        tip.setUndecorated(true);

        JLabel lbl = new JLabel(message);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        tip.getContentPane().add(lbl);
        tip.pack();

        Point p = frame.getLocationOnScreen();
        tip.setLocation(p.x + (frame.getWidth() - tip.getWidth()) / 2,
                        p.y + frame.getHeight() + 6);

        new Timer(1500, ev -> tip.dispose()).start();
        tip.setVisible(true);
    }

    private void beep(String msg) {
        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(frame, msg, "claipo", JOptionPane.WARNING_MESSAGE);
    }

    private static String readClipboardText() {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return (String) cb.getData(DataFlavor.stringFlavor);
            }
        } catch (UnsupportedFlavorException | IOException ignored) { }
        return null;
    }

    private static void writeClipboardText(String s) {
        StringSelection sel = new StringSelection(s);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
    }
}