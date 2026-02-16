// src/main/java/vroddon/claipo/Main.java
package vroddon.claipo;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Start UI on EDT
        System.out.println("Claipo. Java : " + Runtime.version());
        SwingUtilities.invokeLater(() -> {
            Agent agent = new Agent();
            agent.showUI();
        });
    }
}