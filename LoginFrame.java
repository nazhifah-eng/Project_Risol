import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JLabel lblStatus;

    public LoginFrame() {
        setTitle("Larisole — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 520);
        setLocationRelativeTo(null);
        setResizable(false);
        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AppTheme.BG_DARK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setOpaque(true);

        // Card tengah
        JPanel card = AppTheme.makeCard(16);
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(340, 400));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 20, 8, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        // ── Logo / Judul
        JLabel lblTitle = new JLabel("LARISOLE", SwingConstants.CENTER);
        lblTitle.setFont(AppTheme.FONT_HUGE);
        lblTitle.setForeground(AppTheme.ACCENT_ORANGE);
        gbc.gridy = 0;
        gbc.insets = new Insets(28, 20, 4, 20);
        card.add(lblTitle, gbc);

        JLabel lblSub = new JLabel("Sistem Kasir Modern", SwingConstants.CENTER);
        lblSub.setFont(AppTheme.FONT_SMALL);
        lblSub.setForeground(AppTheme.TEXT_MUTED);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 20, 20, 20);
        card.add(lblSub, gbc);

        // ── Username
        JLabel lblUser = AppTheme.makeLabel("Username");
        gbc.gridy = 2;
        gbc.insets = new Insets(8, 20, 2, 20);
        card.add(lblUser, gbc);

        txtUsername = AppTheme.makeTextField(20);
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 20, 8, 20);
        card.add(txtUsername, gbc);

        // ── Password
        JLabel lblPass = AppTheme.makeLabel("Password");
        gbc.gridy = 4;
        gbc.insets = new Insets(8, 20, 2, 20);
        card.add(lblPass, gbc);

        txtPassword = new JPasswordField(20);
        txtPassword.setBackground(AppTheme.BG_INPUT);
        txtPassword.setForeground(AppTheme.TEXT_PRIMARY);
        txtPassword.setCaretColor(AppTheme.ACCENT_ORANGE);
        txtPassword.setFont(AppTheme.FONT_BODY);
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 20, 16, 20);
        card.add(txtPassword, gbc);

        // ── Status label
        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(AppTheme.FONT_SMALL);
        lblStatus.setForeground(AppTheme.ACCENT_RED);
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 20, 8, 20);
        card.add(lblStatus, gbc);

        // ── Tombol Login
        btnLogin = AppTheme.makeButton("Login", AppTheme.ACCENT_ORANGE);
        btnLogin.setPreferredSize(new Dimension(280, 42));
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 20, 24, 20);
        card.add(btnLogin, gbc);

        mainPanel.add(card);
        setContentPane(mainPanel);

        // ── Action
        btnLogin.addActionListener(e -> doLogin());
        txtPassword.addActionListener(e -> doLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocus());

        // ── Fokus awal
        SwingUtilities.invokeLater(() -> txtUsername.requestFocus());
    }

    private void doLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Username dan password wajib diisi!");
            return;
        }

        btnLogin.setEnabled(false);
        lblStatus.setForeground(AppTheme.ACCENT_YELLOW);
        lblStatus.setText("Memeriksa...");

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() {
                return authenticate(username, password);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    if (user != null) {
                        lblStatus.setForeground(AppTheme.ACCENT_GREEN);
                        lblStatus.setText("Login berhasil! Selamat datang, " + user.getNamaLengkap());
                        Timer t = new Timer(800, ev -> {
                            dispose();
                            new Panels2(user).setVisible(true);
                        });
                        t.setRepeats(false);
                        t.start();
                    } else {
                        lblStatus.setForeground(AppTheme.ACCENT_RED);
                        lblStatus.setText("Username atau password salah!");
                        txtPassword.setText("");
                        txtPassword.requestFocus();
                        btnLogin.setEnabled(true);
                    }
                } catch (Exception ex) {
                    lblStatus.setForeground(AppTheme.ACCENT_RED);
                    lblStatus.setText("Gagal terhubung ke database!");
                    btnLogin.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private User authenticate(String username, String password) {
        String sql = "SELECT id, username, nama_lengkap, role, aktif " +
                     "FROM Users WHERE username = ? AND password = ? AND aktif = 1";
        try (PreparedStatement ps = DatabaseConnection.getInstance()
                                        .getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("nama_lengkap"),
                        rs.getString("role")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("authenticate error: " + e.getMessage());
        }
        return null;
    }
}