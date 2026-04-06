package vroddon.claipo;

import javax.swing.SwingUtilities;

/**    
 * Launches the claipo UI
*/
public class Main {
    public static void main(String[] args) {
        // Start UI on EDT
        System.out.println("Claipo. Java : " + Runtime.version());
        SwingUtilities.invokeLater(() -> {
            ClaipoUI agent = new ClaipoUI();
            agent.showUI();
        });
    }
}