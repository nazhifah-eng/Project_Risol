package ui;

import dao.*;
import model.*;
import util.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;


public class MainFrame extends JFrame {

    private User currentUser;
    private JPanel contentArea;
    private JLabel lblJam;
    private JLabel lblOmzet;
    private JLabel lblJmlTrx;

    // DAO instances
    private final TransaksiDAO transaksiDAO = new TransaksiDAO();
    private final ProdukDAO produkDAO = new ProdukDAO();
    private final PengaturanDAO pengaturanDAO = new PengaturanDAO();

    private static final NumberFormat CURRENCY = NumberFormat.getInstance(new Locale("id", "ID"));

    public MainFrame(User user) {
        this.currentUser = user;
        AppTheme.applyTheme();
        initComponents();
        startClock();
        showPanel(new TransaksiPanel(currentUser));
    }

    private void initComponents() {
        setTitle("🥐 Larisole POS — " + currentUser.getNamaLengkap());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppTheme.BG_DARK);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);

        contentArea = new JPanel(new BorderLayout());
        contentArea.setBackground(AppTheme.BG_DARK);
        contentArea.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(contentArea, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(AppTheme.ACCENT_ORANGE);
                g2.fillRect(0, getHeight()-2, getWidth(), 2);
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 58));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoPanel.setOpaque(false);
        JLabel logoIcon = new JLabel("🥐");
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        JLabel logoText = new JLabel("  LARISOLE POS");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logoText.setForeground(AppTheme.ACCENT_ORANGE);
        logoPanel.add(logoIcon);
        logoPanel.add(logoText);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 0));
        infoPanel.setOpaque(false);

        lblOmzet = makeHeaderStat("Omzet Hari Ini", "Rp 0");
        lblJmlTrx = makeHeaderStat("Transaksi", "0");
        lblJam = makeHeaderStat("Waktu", getCurrentTime());

        infoPanel.add(lblOmzet.getParent());
        infoPanel.add(new JSeparator(SwingConstants.VERTICAL) {{ setForeground(AppTheme.BORDER_COLOR); setPreferredSize(new Dimension(1,30)); }});
        infoPanel.add(lblJmlTrx.getParent());
        infoPanel.add(new JSeparator(SwingConstants.VERTICAL) {{ setForeground(AppTheme.BORDER_COLOR); setPreferredSize(new Dimension(1,30)); }});
        infoPanel.add(lblJam.getParent());

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        userPanel.setOpaque(false);
        JLabel userIcon = new JLabel(currentUser.isOwner() ? "👑" : "👤");
        userIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        JPanel userInfo = new JPanel();
        userInfo.setLayout(new BoxLayout(userInfo, BoxLayout.Y_AXIS));
        userInfo.setOpaque(false);
        JLabel userName = new JLabel(currentUser.getNamaLengkap());
        userName.setFont(AppTheme.FONT_SUBTITLE);
        userName.setForeground(AppTheme.TEXT_PRIMARY);
        JLabel userRole = new JLabel(currentUser.getRole().toUpperCase());
        userRole.setFont(AppTheme.FONT_SMALL);
        userRole.setForeground(AppTheme.ACCENT_ORANGE);
        userInfo.add(userName);
        userInfo.add(userRole);

        JButton btnLogout = AppTheme.makeOutlineButton("Keluar", AppTheme.ACCENT_RED);
        btnLogout.setPreferredSize(new Dimension(80, 30));
        btnLogout.addActionListener(e -> konfirmasiLogout());

        userPanel.add(userIcon);
        userPanel.add(userInfo);
        userPanel.add(btnLogout);

        header.add(logoPanel, BorderLayout.WEST);
        header.add(infoPanel, BorderLayout.CENTER);
        header.add(userPanel, BorderLayout.EAST);

        updateHeaderStats();
        return header;
    }

    private JLabel makeHeaderStat(String label, String value) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(AppTheme.FONT_SMALL);
        lbl.setForeground(AppTheme.TEXT_MUTED);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel val = new JLabel(value);
        val.setFont(AppTheme.FONT_SUBTITLE);
        val.setForeground(AppTheme.TEXT_PRIMARY);
        val.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(lbl);
        panel.add(val);
        return val; // return value label agar bisa diupdate
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AppTheme.BG_SIDEBAR);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(AppTheme.BORDER_COLOR);
                g2.fillRect(getWidth()-1, 0, 1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setPreferredSize(new Dimension(190, 0));
        sidebar.setOpaque(false);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        String[][] menus = {
            {"🛒", "Transaksi",   "Buat transaksi baru"},
            {"📋", "Daftar Trx",  "Riwayat semua transaksi"},
            {"📦", "Produk",      "Kelola produk & stok"},
            {"👥", "Pelanggan",   "Data pelanggan tetap"},
            {"📊", "Laporan",     "Analisis & laporan harian"},
            {"⚙️", "Pengaturan",  "Konfigurasi sistem"},
        };

        ButtonGroup group = new ButtonGroup();
        boolean first = true;
        for (String[] menu : menus) {
            JToggleButton btn = makeSidebarButton(menu[0], menu[1], menu[2]);
            group.add(btn);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(2));
            if (first) { btn.setSelected(true); first = false; }
            String name = menu[1];
            btn.addActionListener(e -> navigateTo(name));
        }

        sidebar.add(Box.createVerticalGlue());

        JLabel ver = new JLabel("v2.0 — Larisole POS");
        ver.setFont(AppTheme.FONT_SMALL);
        ver.setForeground(AppTheme.TEXT_MUTED);
        ver.setBorder(BorderFactory.createEmptyBorder(8, 16, 4, 0));
        sidebar.add(ver);

        return sidebar;
    }

    private JToggleButton makeSidebarButton(String icon, String label, String tooltip) {
        JToggleButton btn = new JToggleButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isSelected()) {
                    g2.setColor(new Color(255, 120, 40, 30));
                    g2.fillRoundRect(8, 2, getWidth()-16, getHeight()-4, 8, 8);
                    g2.setColor(AppTheme.ACCENT_ORANGE);
                    g2.fillRoundRect(0, 6, 3, getHeight()-12, 3, 3);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(255,255,255,8));
                    g2.fillRoundRect(8, 2, getWidth()-16, getHeight()-4, 8, 8);
                }
                // Icon
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 17));
                g2.setColor(isSelected() ? AppTheme.ACCENT_ORANGE : AppTheme.TEXT_SECONDARY);
                g2.drawString(icon, 20, getHeight()/2 + 6);
                // Label
                g2.setFont(isSelected() ? AppTheme.FONT_SUBTITLE : AppTheme.FONT_BODY);
                g2.setColor(isSelected() ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY);
                g2.drawString(label, 46, getHeight()/2 + 5);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(190, 44));
        btn.setMaximumSize(new Dimension(190, 44));
        btn.setToolTipText(tooltip);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void navigateTo(String name) {
        JPanel panel = switch (name) {
            case "Transaksi"   -> new TransaksiPanel(currentUser);
            case "Daftar Trx"  -> new DaftarTransaksiPanel(currentUser);
            case "Produk"      -> new ProdukPanel(currentUser);
            case "Pelanggan"   -> new PelangganPanel();
            case "Laporan"     -> new LaporanPanel();
            case "Pengaturan"  -> new PengaturanPanel(currentUser);
            default            -> new TransaksiPanel(currentUser);
        };
        showPanel(panel);
        updateHeaderStats();
    }

    public void showPanel(JPanel panel) {
        contentArea.removeAll();
        contentArea.add(panel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    private void startClock() {
        Timer timer = new Timer(1000, e -> {
            lblJam.setText(getCurrentTime());
        });
        timer.start();

        Timer statsTimer = new Timer(30000, e -> updateHeaderStats());
        statsTimer.start();
    }

    private void updateHeaderStats() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    BigDecimal omzet = transaksiDAO.getOmzetHariIni();
                    int jmlTrx = transaksiDAO.getJumlahTransaksiHariIni();
                    SwingUtilities.invokeLater(() -> {
                        lblOmzet.setText("Rp " + CURRENCY.format(omzet));
                        lblJmlTrx.setText(String.valueOf(jmlTrx));
                    });
                } catch (Exception e) { /* ignore */ }
                return null;
            }
        };
        worker.execute();
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private void konfirmasiLogout() {
        int opt = JOptionPane.showConfirmDialog(this,
            "Yakin ingin keluar dari sistem?", "Konfirmasi Logout",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            DatabaseConnection.getInstance().closeConnection();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}