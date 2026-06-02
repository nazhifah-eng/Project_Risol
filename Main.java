import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        AppTheme.applyTheme();
        SwingUtilities.invokeLater(() -> {
            try {
                new LoginFrame().setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Gagal memulai aplikasi:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}
