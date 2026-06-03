import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

public class Panels2 extends JFrame {

    private final User currentUser;
    private JPanel contentPanel;
    private final CardLayout cardLayout = new CardLayout();
    private final NumberFormat rupiah = NumberFormat.getInstance(new Locale("id", "ID"));

    // Sidebar buttons
    private JButton btnDashboard, btnProduk, btnTransaksi, btnRiwayat, btnKeluar;
    private JButton activeSidebarBtn;

    // Kasir / Transaksi state
    private final List<ItemKeranjang> keranjang = new ArrayList<>();
    private DefaultListModel<ItemKeranjang> keranjangModel;
    private JLabel lblSubtotal, lblTotal, lblJumlahItem;
    private JPanel productGridPanel;
    private JTextField txtCariKasir;
    private List<Produk> allProduk = new ArrayList<>();

    // Produk CRUD
    private DefaultTableModel produkTableModel;

    public Panels2(User user) {
        this.currentUser = user;
        setTitle("Larisole — " + user.getNamaLengkap());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 580));
        initComponents();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ROOT LAYOUT
    // ═══════════════════════════════════════════════════════════════════════
    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppTheme.BG_DARK);
        root.add(buildSidebar(), BorderLayout.WEST);

        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(AppTheme.BG_DARK);
        contentPanel.add(buildDashboardPanel(), "dashboard");
        contentPanel.add(buildKasirPanel(),     "transaksi");
        contentPanel.add(buildProdukPanel(),    "produk");
        contentPanel.add(buildRiwayatPanel(),   "riwayat");

        root.add(contentPanel, BorderLayout.CENTER);
        setContentPane(root);
        showPanel("transaksi", btnTransaksi);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SIDEBAR
    // ═══════════════════════════════════════════════════════════════════════
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(AppTheme.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, AppTheme.BORDER_COLOR));

        // Logo
        JLabel logo = new JLabel("Larisole", SwingConstants.CENTER);
        logo.setFont(AppTheme.FONT_HUGE);
        logo.setForeground(AppTheme.ACCENT_ORANGE);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setBorder(BorderFactory.createEmptyBorder(26, 0, 2, 0));
        sidebar.add(logo);

        JLabel sub = new JLabel("Sistem Kasir Modern", SwingConstants.CENTER);
        sub.setFont(AppTheme.FONT_SMALL);
        sub.setForeground(AppTheme.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(sub);

        sidebar.add(Box.createVerticalStrut(18));
        sidebar.add(makeSep());
        sidebar.add(Box.createVerticalStrut(8));

        btnDashboard = makeSidebarBtn("🏠", "Dashboard");
        btnTransaksi = makeSidebarBtn("🛒", "Kasir");
        btnProduk    = makeSidebarBtn("📦", "Produk");
        btnRiwayat   = makeSidebarBtn("📋", "Riwayat");

        sidebar.add(btnDashboard);
        sidebar.add(btnTransaksi);
        sidebar.add(btnProduk);
        sidebar.add(btnRiwayat);

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(makeSep());

        // Info user
        JPanel userInfo = new JPanel();
        userInfo.setOpaque(false);
        userInfo.setLayout(new BoxLayout(userInfo, BoxLayout.Y_AXIS));
        userInfo.setBorder(BorderFactory.createEmptyBorder(10, 16, 6, 16));
        userInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        userInfo.setMaximumSize(new Dimension(210, 70));

        JLabel lblNama = new JLabel(currentUser.getNamaLengkap());
        lblNama.setFont(AppTheme.FONT_BODY);
        lblNama.setForeground(AppTheme.TEXT_PRIMARY);
        JLabel lblRole = new JLabel(currentUser.getRole().toUpperCase());
        lblRole.setFont(AppTheme.FONT_SMALL);
        lblRole.setForeground(AppTheme.ACCENT_ORANGE);
        userInfo.add(lblNama);
        userInfo.add(lblRole);
        sidebar.add(userInfo);

        btnKeluar = makeSidebarBtn("🚪", "Keluar");
        btnKeluar.setForeground(AppTheme.ACCENT_RED);
        sidebar.add(btnKeluar);
        sidebar.add(Box.createVerticalStrut(12));

        // Actions
        btnDashboard.addActionListener(e -> showPanel("dashboard", btnDashboard));
        btnTransaksi.addActionListener(e -> showPanel("transaksi", btnTransaksi));
        btnProduk.addActionListener(e    -> showPanel("produk",    btnProduk));
        btnRiwayat.addActionListener(e   -> showPanel("riwayat",   btnRiwayat));
        btnKeluar.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                "Yakin ingin keluar?", "Konfirmasi",
                JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                DatabaseConnection.getInstance().closeConnection();
                dispose();
                new LoginFrame().setVisible(true);
            }
        });

        return sidebar;
    }

    private JButton makeSidebarBtn(String icon, String text) {
        JButton btn = new JButton(icon + "  " + text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = (this == activeSidebarBtn);
                if (active) {
                    g2.setColor(new Color(226, 135, 67, 45));
                    g2.fillRoundRect(6, 2, getWidth() - 12, getHeight() - 4, 10, 10);
                    g2.setColor(AppTheme.ACCENT_ORANGE);
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawLine(2, 4, 2, getHeight() - 4);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(226, 135, 67, 20));
                    g2.fillRoundRect(6, 2, getWidth() - 12, getHeight() - 4, 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.FONT_BODY);
        btn.setForeground(AppTheme.TEXT_PRIMARY);
        btn.setBackground(new Color(0, 0, 0, 0));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(11, 22, 11, 8));
        btn.setMaximumSize(new Dimension(210, 46));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JSeparator makeSep() {
        JSeparator sep = new JSeparator();
        sep.setForeground(AppTheme.SEPARATOR);
        sep.setMaximumSize(new Dimension(210, 1));
        return sep;
    }

    private void showPanel(String name, JButton btn) {
        cardLayout.show(contentPanel, name);
        activeSidebarBtn = btn;
        repaint();
        if ("produk".equals(name) && produkTableModel != null) {
            loadProdukTable(produkTableModel, null);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DASHBOARD PANEL
    // ═══════════════════════════════════════════════════════════════════════
    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppTheme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        // Header
        JLabel title = new JLabel("Selamat datang, " + currentUser.getNamaLengkap() + " 👋");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        JLabel subtitle = new JLabel("Ringkasan aktivitas hari ini");
        subtitle.setFont(AppTheme.FONT_BODY);
        subtitle.setForeground(AppTheme.TEXT_MUTED);
        JPanel hdr = new JPanel();
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        hdr.setOpaque(false);
        hdr.add(title);
        hdr.add(Box.createVerticalStrut(4));
        hdr.add(subtitle);
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        panel.add(hdr, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(2, 3, 16, 16));
        cards.setBackground(AppTheme.BG_DARK);

        cards.add(makeStatCard("Total Produk",       "...", AppTheme.ACCENT_ORANGE));
        cards.add(makeStatCard("Stok Menipis",        "...", AppTheme.ACCENT_RED));
        cards.add(makeStatCard("Transaksi Hari Ini",  "...", AppTheme.ACCENT_GREEN));
        cards.add(makeStatCard("Kasir Aktif",          currentUser.getNamaLengkap(), AppTheme.ACCENT_ORANGE));
        cards.add(makeStatCard("Role",                 currentUser.getRole().toUpperCase(), new Color(90, 150, 210)));
        cards.add(makeStatCard("Status DB",            "Terhubung ✓", AppTheme.ACCENT_GREEN));

        panel.add(cards, BorderLayout.CENTER);
        return panel;
    }

    private JPanel makeStatCard(String label, String value, Color accent) {
        JPanel card = AppTheme.makeCard(14);
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(170, 95));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(4, 8, 2, 8);

        // Accent bar di atas
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(40, 4));
        gbc.gridy = 0; gbc.insets = new Insets(14, 8, 6, 8);
        card.add(bar, gbc);

        JLabel valLabel = new JLabel(value, SwingConstants.CENTER);
        valLabel.setFont(AppTheme.FONT_PRICE);
        valLabel.setForeground(accent);
        gbc.gridy = 1; gbc.insets = new Insets(0, 8, 2, 8);
        card.add(valLabel, gbc);

        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(AppTheme.FONT_SMALL);
        lbl.setForeground(AppTheme.TEXT_MUTED);
        gbc.gridy = 2; gbc.insets = new Insets(0, 8, 14, 8);
        card.add(lbl, gbc);

        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // KASIR (TRANSAKSI) PANEL — layout sesuai sketsa: kiri grid, kanan cart
    // ═══════════════════════════════════════════════════════════════════════
    private JPanel buildKasirPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppTheme.BG_DARK);

        // ── Top bar ─────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setBackground(AppTheme.BG_CARD);
        topBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_COLOR),
            BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        JLabel lblKasir = new JLabel("🛒  Kasir");
        lblKasir.setFont(AppTheme.FONT_TITLE);
        lblKasir.setForeground(AppTheme.TEXT_PRIMARY);
        topBar.add(lblKasir, BorderLayout.WEST);

        txtCariKasir = AppTheme.makeTextField(20);
        txtCariKasir.setPreferredSize(new Dimension(260, 36));
        txtCariKasir.putClientProperty("hint", "Cari produk...");
        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(txtCariKasir);
        topBar.add(searchWrap, BorderLayout.EAST);

        root.add(topBar, BorderLayout.NORTH);

        // ── Body: kiri produk, kanan cart ───────────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.62);
        split.setDividerSize(4);
        split.setBorder(null);

        split.setLeftComponent(buildProductSide());
        split.setRightComponent(buildCartSide());

        root.add(split, BorderLayout.CENTER);

        // Search listener
        txtCariKasir.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { refreshProductGrid(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { refreshProductGrid(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshProductGrid(); }
        });

        // Load produk dari DB
        SwingUtilities.invokeLater(this::loadAllProdukKasir);
        return root;
    }

    // Kiri: grid produk
    private JPanel buildProductSide() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(AppTheme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 8));

        productGridPanel = new JPanel();
        productGridPanel.setBackground(AppTheme.BG_DARK);
        productGridPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 10, 10));

        JScrollPane scroll = new JScrollPane(productGridPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(AppTheme.BG_DARK);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void loadAllProdukKasir() {
        SwingWorker<List<Produk>, Void> worker = new SwingWorker<>() {
            @Override protected List<Produk> doInBackground() {
                return new ProdukDAO().getAllProduk();
            }
            @Override protected void done() {
                try {
                    allProduk = get();
                    refreshProductGrid();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void refreshProductGrid() {
        String kw = txtCariKasir != null ? txtCariKasir.getText().trim().toLowerCase() : "";
        productGridPanel.removeAll();

        List<Produk> filtered = new ArrayList<>();
        for (Produk p : allProduk) {
            if (p.isAktif() && (kw.isEmpty()
                    || p.getNama().toLowerCase().contains(kw)
                    || p.getKode().toLowerCase().contains(kw))) {
                filtered.add(p);
            }
        }

        if (filtered.isEmpty()) {
            JLabel empty = new JLabel("Tidak ada produk ditemukan");
            empty.setForeground(AppTheme.TEXT_MUTED);
            empty.setFont(AppTheme.FONT_BODY);
            productGridPanel.add(empty);
        } else {
            for (Produk p : filtered) {
                productGridPanel.add(buildProductCard(p));
            }
        }

        productGridPanel.revalidate();
        productGridPanel.repaint();
    }

    // Kartu produk individual
    private JPanel buildProductCard(Produk p) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(AppTheme.BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(138, 140));
        card.setLayout(new GridBagLayout());
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 8, 0, 8);

        // Ikon/emoji area
        JLabel ico = new JLabel("🍱", SwingConstants.CENTER);
        ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 30));
        ico.setPreferredSize(new Dimension(120, 44));
        g.gridy = 0; g.insets = new Insets(10, 8, 4, 8);
        card.add(ico, g);

        // Nama
        JLabel nama = new JLabel("<html><center>" + p.getNama() + "</center></html>", SwingConstants.CENTER);
        nama.setFont(AppTheme.FONT_SMALL);
        nama.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nama.setForeground(AppTheme.TEXT_PRIMARY);
        g.gridy = 1; g.insets = new Insets(0, 6, 2, 6);
        card.add(nama, g);

        // Harga
        JLabel harga = new JLabel("Rp " + rupiah.format(p.getHarga()), SwingConstants.CENTER);
        harga.setFont(new Font("Segoe UI", Font.BOLD, 11));
        harga.setForeground(new Color(181, 98, 42));
        g.gridy = 2; g.insets = new Insets(0, 6, 2, 6);
        card.add(harga, g);

        // Stok
        JLabel stok = new JLabel("Stok: " + p.getStok(), SwingConstants.CENTER);
        stok.setFont(AppTheme.FONT_SMALL);
        stok.setForeground(AppTheme.TEXT_MUTED);
        g.gridy = 3; g.insets = new Insets(0, 6, 6, 6);
        card.add(stok, g);

        // Tombol +
        JButton btnAdd = new JButton("+") {
            @Override protected void paintComponent(Graphics g2) {
                Graphics2D g2d = (Graphics2D) g2.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getModel().isRollover() ? AppTheme.ACCENT_ORANGE.darker() : AppTheme.ACCENT_ORANGE);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 18));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString("+", (getWidth() - fm.stringWidth("+")) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2d.dispose();
            }
        };
        btnAdd.setPreferredSize(new Dimension(28, 28));
        btnAdd.setContentAreaFilled(false); btnAdd.setBorderPainted(false);
        btnAdd.setFocusPainted(false);
        btnAdd.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAdd.addActionListener(e -> tambahKeKeranjang(p));
        g.gridy = 4; g.insets = new Insets(0, 48, 10, 48);
        card.add(btnAdd, g);

        // Hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createLineBorder(AppTheme.ACCENT_YELLOW, 2, true));
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setBorder(null);
            }
            @Override public void mouseClicked(MouseEvent e) {
                tambahKeKeranjang(p);
            }
        });

        return card;
    }

    // Kanan: cart / order summary
    private JPanel buildCartSide() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppTheme.BG_CARD);
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, AppTheme.BORDER_COLOR));

        // Header cart
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(AppTheme.BG_CARD);
        hdr.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_COLOR),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        JLabel lblTitle = new JLabel("Order Summary");
        lblTitle.setFont(AppTheme.FONT_SUBTITLE);
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        lblJumlahItem = new JLabel("0 item");
        lblJumlahItem.setFont(AppTheme.FONT_SMALL);
        lblJumlahItem.setForeground(new Color(255, 255, 255));
        lblJumlahItem.setBackground(AppTheme.ACCENT_ORANGE);
        lblJumlahItem.setOpaque(true);
        lblJumlahItem.setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
        hdr.add(lblTitle, BorderLayout.WEST);
        hdr.add(lblJumlahItem, BorderLayout.EAST);
        panel.add(hdr, BorderLayout.NORTH);

        // List keranjang
        keranjangModel = new DefaultListModel<>();
        JList<ItemKeranjang> cartList = new JList<>(keranjangModel);
        cartList.setCellRenderer(new CartCellRenderer());
        cartList.setBackground(AppTheme.BG_CARD);
        cartList.setSelectionBackground(new Color(226, 135, 67, 30));
        cartList.setFixedCellHeight(62);

        JScrollPane cartScroll = new JScrollPane(cartList);
        cartScroll.setBorder(null);
        cartScroll.getViewport().setBackground(AppTheme.BG_CARD);
        panel.add(cartScroll, BorderLayout.CENTER);

        // Popup menu untuk hapus item
        JPopupMenu popup = new JPopupMenu();
        JMenuItem miHapus = new JMenuItem("Hapus item");
        miHapus.setForeground(AppTheme.ACCENT_RED);
        popup.add(miHapus);
        cartList.setComponentPopupMenu(popup);
        miHapus.addActionListener(e -> {
            int idx = cartList.getSelectedIndex();
            if (idx >= 0) { keranjangModel.remove(idx); keranjang.remove(idx); updateTotals(); }
        });

        // Bottom: subtotal + tombol
        JPanel bottom = new JPanel();
        bottom.setBackground(AppTheme.BG_CARD);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER_COLOR),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        // Subtotal row
        JPanel rowSub = makeRingkasanRow("Subtotal", "Rp 0");
        lblSubtotal = (JLabel) ((BorderLayout)rowSub.getLayout() != null
            ? rowSub.getComponent(1) : rowSub.getComponent(1));
        bottom.add(rowSub);

        // Diskon row
        JPanel rowDisk = makeRingkasanRow("Diskon", "Rp 0");
        bottom.add(rowDisk);
        bottom.add(Box.createVerticalStrut(6));

        // Garis putus-putus
        JSeparator dash = new JSeparator() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(AppTheme.BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{6f, 4f}, 0f));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        dash.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        bottom.add(dash);
        bottom.add(Box.createVerticalStrut(6));

        // Total row — lebih besar
        JPanel rowTotal = new JPanel(new BorderLayout());
        rowTotal.setOpaque(false);
        rowTotal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel lblTotalKey = new JLabel("TOTAL");
        lblTotalKey.setFont(AppTheme.FONT_SUBTITLE);
        lblTotalKey.setForeground(AppTheme.TEXT_PRIMARY);
        lblTotal = new JLabel("Rp 0");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTotal.setForeground(new Color(181, 98, 42));
        rowTotal.add(lblTotalKey, BorderLayout.WEST);
        rowTotal.add(lblTotal, BorderLayout.EAST);
        bottom.add(rowTotal);
        bottom.add(Box.createVerticalStrut(12));

        // Tombol aksi
        JPanel btnRow = new JPanel(new GridLayout(1, 2, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JButton btnBayar = AppTheme.makeButton("💳 Bayar", AppTheme.ACCENT_ORANGE);
        btnBayar.setPreferredSize(new Dimension(10, 42));

        JButton btnUpdate = AppTheme.makeOutlineButton("↻ Update", AppTheme.TEXT_SECONDARY);
        btnUpdate.setPreferredSize(new Dimension(10, 42));

        btnRow.add(btnBayar);
        btnRow.add(btnUpdate);
        bottom.add(btnRow);

        // Tombol bersihkan keranjang
        JButton btnClear = AppTheme.makeOutlineButton("🗑 Kosongkan", AppTheme.ACCENT_RED);
        btnClear.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnClear.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        bottom.add(Box.createVerticalStrut(6));
        bottom.add(btnClear);

        panel.add(bottom, BorderLayout.SOUTH);

        // Button listeners
        btnBayar.addActionListener(e -> prosesPayment());
        btnUpdate.addActionListener(e -> {
            updateTotals();
            JOptionPane.showMessageDialog(this, "Keranjang diperbarui!", "Info",
                JOptionPane.INFORMATION_MESSAGE);
        });
        btnClear.addActionListener(e -> {
            if (keranjang.isEmpty()) return;
            int ok = JOptionPane.showConfirmDialog(this,
                "Kosongkan semua item?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                keranjang.clear(); keranjangModel.clear(); updateTotals();
            }
        });

        return panel;
    }

    private JPanel makeRingkasanRow(String key, String val) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel k = new JLabel(key);
        k.setFont(AppTheme.FONT_BODY);
        k.setForeground(AppTheme.TEXT_MUTED);
        JLabel v = new JLabel(val);
        v.setFont(AppTheme.FONT_BODY);
        v.setForeground(AppTheme.TEXT_PRIMARY);
        row.add(k, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        return row;
    }

    private void tambahKeKeranjang(Produk p) {
        for (ItemKeranjang item : keranjang) {
            if (item.getProdukId() == p.getId()) {
                item.setQty(item.getQty() + 1);
                int idx = keranjang.indexOf(item);
                keranjangModel.set(idx, item);
                updateTotals();
                return;
            }
        }
        ItemKeranjang item = new ItemKeranjang(p, 1);
        keranjang.add(item);
        keranjangModel.addElement(item);
        updateTotals();
    }

    private void updateTotals() {
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (ItemKeranjang item : keranjang) {
            item.hitungSubtotal();
            total = total.add(item.getSubtotal());
            count += item.getQty();
        }
        String fmt = "Rp " + rupiah.format(total);
        if (lblSubtotal != null) lblSubtotal.setText(fmt);
        if (lblTotal    != null) lblTotal.setText(fmt);
        if (lblJumlahItem != null) lblJumlahItem.setText(count + " item");
    }

private void prosesPayment() {
    if (keranjang.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Keranjang masih kosong!", 
            "Info", JOptionPane.WARNING_MESSAGE);
        return;
    }

    BigDecimal total = keranjang.stream()
        .map(ItemKeranjang::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Dialog pembayaran
    JDialog dlg = new JDialog(this, "💳  Proses Pembayaran", true);
    dlg.setSize(360, 300);
    dlg.setLocationRelativeTo(this);

    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBackground(AppTheme.BG_CARD);
    panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
    GridBagConstraints g = new GridBagConstraints();
    g.fill = GridBagConstraints.HORIZONTAL;
    g.insets = new Insets(6, 4, 6, 4);
    g.gridx = 0;

    NumberFormat rupiah = NumberFormat.getInstance(new java.util.Locale("id","ID"));

    JLabel lblTotal = new JLabel("Total: Rp " + rupiah.format(total));
    lblTotal.setFont(AppTheme.FONT_PRICE);
    lblTotal.setForeground(new Color(181, 98, 42));
    g.gridy = 0; panel.add(lblTotal, g);

    // Metode bayar
    g.gridy = 1; panel.add(AppTheme.makeLabel("Metode Pembayaran"), g);
    JComboBox<String> cmbMetode = AppTheme.makeComboBox();
    cmbMetode.addItem("Tunai");
    cmbMetode.addItem("QRIS");
    cmbMetode.addItem("Transfer");
    g.gridy = 2; panel.add(cmbMetode, g);

    // Jumlah dibayar
    g.gridy = 3; panel.add(AppTheme.makeLabel("Jumlah Dibayar (Rp)"), g);
    JTextField txtDibayar = AppTheme.makeTextField(14);
    txtDibayar.setText(total.toPlainString());
    g.gridy = 4; panel.add(txtDibayar, g);

    JLabel lblKembalian = new JLabel("Kembalian: Rp 0");
    lblKembalian.setFont(AppTheme.FONT_BODY);
    lblKembalian.setForeground(AppTheme.ACCENT_GREEN);
    g.gridy = 5; panel.add(lblKembalian, g);

    // Hitung kembalian realtime
    txtDibayar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
        void update() {
            try {
                BigDecimal dibayar = new BigDecimal(txtDibayar.getText().trim());
                BigDecimal kembalian = dibayar.subtract(total);
                lblKembalian.setText("Kembalian: Rp " + rupiah.format(
                    kembalian.compareTo(BigDecimal.ZERO) >= 0 ? kembalian : BigDecimal.ZERO));
            } catch (Exception ignored) {}
        }
        public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
    });

    JButton btnProses = AppTheme.makeButton("✓ Proses", AppTheme.ACCENT_GREEN);
    btnProses.setPreferredSize(new Dimension(280, 40));
    g.gridy = 6; panel.add(btnProses, g);

    btnProses.addActionListener(e -> {
        try {
            BigDecimal dibayar = new BigDecimal(txtDibayar.getText().trim());
            if (dibayar.compareTo(total) < 0) {
                JOptionPane.showMessageDialog(dlg, 
                    "Jumlah dibayar kurang dari total!", "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            Transaksi t = new Transaksi();
            t.setNoTransaksi("TRX-" + System.currentTimeMillis());
            t.setKasirId(currentUser.getId());
            t.setKasirNama(currentUser.getNamaLengkap());
            t.setSubtotal(total);
            t.setDiskon(BigDecimal.ZERO);
            t.setTotalBayar(total);
            t.setDibayar(dibayar);
            t.setKembalian(dibayar.subtract(total));
            t.setMetodeBayar((String) cmbMetode.getSelectedItem());
            t.setStatus("lunas");

            boolean ok = new TransaksiDAO().simpanTransaksi(t, keranjang);
            if (ok) {
                JOptionPane.showMessageDialog(dlg,
                    "✓ Transaksi berhasil!\nKembalian: Rp " + 
                    rupiah.format(t.getKembalian()),
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);
                keranjang.clear();
                keranjangModel.clear();
                updateTotals();
                loadAllProdukKasir(); // refresh stok
                dlg.dispose();
            } else {
                JOptionPane.showMessageDialog(dlg, 
                    "Gagal menyimpan transaksi!", "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(dlg, 
                "Masukkan jumlah yang valid!", "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    });

    dlg.setContentPane(panel);
    dlg.setVisible(true);
}

    // Custom renderer untuk cart list
    private class CartCellRenderer implements ListCellRenderer<ItemKeranjang> {
        @Override
        public Component getListCellRendererComponent(JList<? extends ItemKeranjang> list,
                ItemKeranjang item, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel cell = new JPanel(new BorderLayout(10, 0));
            cell.setBackground(isSelected
                ? new Color(226, 135, 67, 25) : AppTheme.BG_CARD);
            cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
            ));

            // Ikon
            JLabel ico = new JLabel("🍱");
            ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
            ico.setPreferredSize(new Dimension(36, 36));
            ico.setHorizontalAlignment(SwingConstants.CENTER);
            JPanel icoWrap = new JPanel(new BorderLayout());
            icoWrap.setBackground(AppTheme.BG_DARK);
            icoWrap.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_COLOR, 1));
            icoWrap.setPreferredSize(new Dimension(40, 40));
            icoWrap.add(ico, BorderLayout.CENTER);
            cell.add(icoWrap, BorderLayout.WEST);

            // Info
            JPanel info = new JPanel(new GridLayout(2, 1, 0, 2));
            info.setOpaque(false);
            JLabel nama = new JLabel(item.getNamaProduk());
            nama.setFont(new Font("Segoe UI", Font.BOLD, 12));
            nama.setForeground(AppTheme.TEXT_PRIMARY);
            JLabel price = new JLabel("Rp " + rupiah.format(item.getHargaSatuan())
                + "  ×  " + item.getQty());
            price.setFont(AppTheme.FONT_SMALL);
            price.setForeground(AppTheme.TEXT_MUTED);
            info.add(nama); info.add(price);
            cell.add(info, BorderLayout.CENTER);

            // Subtotal + kontrol qty
            JPanel right = new JPanel();
            right.setOpaque(false);
            right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

            JLabel sub = new JLabel("Rp " + rupiah.format(item.getSubtotal()));
            sub.setFont(new Font("Segoe UI", Font.BOLD, 12));
            sub.setForeground(new Color(181, 98, 42));
            sub.setAlignmentX(Component.RIGHT_ALIGNMENT);
            right.add(sub);
            right.add(Box.createVerticalStrut(4));

            JPanel qtyCtrl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            qtyCtrl.setOpaque(false);
            JButton minus = makeQtyBtn("−");
            JLabel qty   = new JLabel(String.valueOf(item.getQty()));
            qty.setFont(new Font("Segoe UI", Font.BOLD, 13));
            qty.setForeground(AppTheme.TEXT_PRIMARY);
            qty.setPreferredSize(new Dimension(20, 20));
            qty.setHorizontalAlignment(SwingConstants.CENTER);
            JButton plus = makeQtyBtn("+");

            minus.addActionListener(e -> {
                if (item.getQty() > 1) { item.setQty(item.getQty() - 1); }
                else { keranjang.remove(item); keranjangModel.removeElement(item); }
                updateTotals();
                list.repaint();
            });
            plus.addActionListener(e -> {
                item.setQty(item.getQty() + 1);
                updateTotals();
                list.repaint();
            });

            qtyCtrl.add(minus); qtyCtrl.add(qty); qtyCtrl.add(plus);
            right.add(qtyCtrl);
            cell.add(right, BorderLayout.EAST);
            return cell;
        }

        private JButton makeQtyBtn(String txt) {
            JButton b = new JButton(txt) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getModel().isRollover()
                        ? AppTheme.BORDER_COLOR : new Color(240, 232, 220));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.setColor(AppTheme.TEXT_PRIMARY);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(getText(),
                        (getWidth()-fm.stringWidth(getText()))/2,
                        (getHeight()+fm.getAscent()-fm.getDescent())/2);
                    g2.dispose();
                }
            };
            b.setPreferredSize(new Dimension(24, 24));
            b.setContentAreaFilled(false); b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRODUK PANEL (CRUD)
    // ═══════════════════════════════════════════════════════════════════════
    private JPanel buildProdukPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(AppTheme.BG_DARK);

        // ── Header ──────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(AppTheme.BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_COLOR),
            BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));

        JLabel title = new JLabel("📦  Manajemen Produk");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        // Tombol CRUD
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);

        JTextField txtCari = AppTheme.makeTextField(16);
        txtCari.setPreferredSize(new Dimension(200, 36));

        JButton btnCari    = AppTheme.makeOutlineButton("🔍 Cari",    AppTheme.ACCENT_ORANGE);
        JButton btnRefresh = AppTheme.makeOutlineButton("↻ Refresh",  AppTheme.TEXT_SECONDARY);
        JButton btnTambah  = AppTheme.makeButton(      "+ Tambah",    AppTheme.ACCENT_GREEN);
        JButton btnEdit    = AppTheme.makeOutlineButton("✏ Edit",     new Color(90, 150, 210));
        JButton btnHapus   = AppTheme.makeOutlineButton("🗑 Hapus",   AppTheme.ACCENT_RED);

        btnCari.setPreferredSize(new Dimension(100, 36));
        btnRefresh.setPreferredSize(new Dimension(100, 36));
        btnTambah.setPreferredSize(new Dimension(110, 36));
        btnEdit.setPreferredSize(new Dimension(90, 36));
        btnHapus.setPreferredSize(new Dimension(90, 36));

        btnBar.add(txtCari);
        btnBar.add(btnCari);
        btnBar.add(btnRefresh);
        btnBar.add(makeDivider());
        btnBar.add(btnTambah);
        btnBar.add(btnEdit);
        btnBar.add(btnHapus);
        header.add(btnBar, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // ── Tabel ───────────────────────────────────────────────────────────
        String[] cols = {"ID", "Kode", "Nama Produk", "Kategori", "Harga", "Stok", "Satuan", "Aktif"};
        produkTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(produkTableModel);
        styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(55);
        table.getColumnModel().getColumn(6).setPreferredWidth(65);
        table.getColumnModel().getColumn(7).setPreferredWidth(45);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_COLOR));
        scroll.getViewport().setBackground(AppTheme.BG_TABLE_ROW);
        panel.add(scroll, BorderLayout.CENTER);

        // Status bar bawah
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(AppTheme.BG_SIDEBAR);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER_COLOR),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        JLabel lblStatus = new JLabel("Pilih baris untuk edit/hapus · Double-click untuk detail");
        lblStatus.setFont(AppTheme.FONT_SMALL);
        lblStatus.setForeground(AppTheme.TEXT_MUTED);
        statusBar.add(lblStatus, BorderLayout.WEST);
        panel.add(statusBar, BorderLayout.SOUTH);

        // ── Event listeners ─────────────────────────────────────────────────
        btnRefresh.addActionListener(e -> loadProdukTable(produkTableModel, null));
        btnCari.addActionListener(e    -> loadProdukTable(produkTableModel, txtCari.getText()));
        txtCari.addActionListener(e    -> loadProdukTable(produkTableModel, txtCari.getText()));

        btnTambah.addActionListener(e  -> showProdukDialog(null, produkTableModel));

        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Pilih produk terlebih dahulu!", "Info",
                    JOptionPane.WARNING_MESSAGE); return;
            }
            int id = (int) produkTableModel.getValueAt(row, 0);
            Produk p = new ProdukDAO().getProdukById(id);
            if (p != null) showProdukDialog(p, produkTableModel);
        });

        btnHapus.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Pilih produk terlebih dahulu!", "Info",
                    JOptionPane.WARNING_MESSAGE); return;
            }
            int id = (int) produkTableModel.getValueAt(row, 0);
            String nama = (String) produkTableModel.getValueAt(row, 2);
            int ok = JOptionPane.showConfirmDialog(this,
                "Hapus produk \"" + nama + "\"?\nAksi ini tidak dapat dibatalkan.",
                "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) {
                boolean berhasil = new ProdukDAO().hapusProduk(id);
                JOptionPane.showMessageDialog(this,
                    berhasil ? "✓ Produk berhasil dihapus." : "✗ Gagal menghapus produk.",
                    berhasil ? "Sukses" : "Error",
                    berhasil ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                loadProdukTable(produkTableModel, null);
            }
        });

        // Double-click untuk detail
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    int id = (int) produkTableModel.getValueAt(table.getSelectedRow(), 0);
                    Produk p = new ProdukDAO().getProdukById(id);
                    if (p != null) showProdukDialog(p, produkTableModel);
                }
            }
        });

        SwingUtilities.invokeLater(() -> loadProdukTable(produkTableModel, null));
        return panel;
    }

    private JComponent makeDivider() {
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setForeground(AppTheme.BORDER_COLOR);
        sep.setPreferredSize(new Dimension(1, 28));
        return sep;
    }

    private void loadProdukTable(DefaultTableModel model, String keyword) {
        model.setRowCount(0);
        ProdukDAO dao = new ProdukDAO();
        List<Produk> list = (keyword == null || keyword.isBlank())
            ? dao.getAllProduk() : dao.cariProduk(keyword);
        for (Produk p : list) {
            model.addRow(new Object[]{
                p.getId(), p.getKode(), p.getNama(), p.getKategoriNama(),
                "Rp " + rupiah.format(p.getHarga()),
                p.getStok(), p.getSatuan(), p.isAktif() ? "✔" : "✘"
            });
        }
    }

    // ── Dialog Tambah/Edit Produk ────────────────────────────────────────
    private void showProdukDialog(Produk existing, DefaultTableModel model) {
        boolean isEdit = existing != null;
        JDialog dialog = new JDialog(this,
            isEdit ? "✏  Edit Produk" : "+  Tambah Produk", true);
        dialog.setSize(440, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        // Panel utama
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppTheme.BG_CARD);

        // Header dialog
        JPanel dlgHdr = new JPanel(new BorderLayout());
        dlgHdr.setBackground(isEdit ? new Color(245, 245, 255) : new Color(240, 255, 245));
        dlgHdr.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_COLOR),
            BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));
        JLabel dlgTitle = new JLabel(isEdit ? "Edit Data Produk" : "Tambah Produk Baru");
        dlgTitle.setFont(AppTheme.FONT_SUBTITLE);
        dlgTitle.setForeground(AppTheme.TEXT_PRIMARY);
        dlgHdr.add(dlgTitle);
        panel.add(dlgHdr, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(AppTheme.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(16, 22, 8, 22));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 4, 5, 4);

        JTextField txtKode   = AppTheme.makeTextField(14);
        JTextField txtNama   = AppTheme.makeTextField(14);
        JTextField txtHarga  = AppTheme.makeTextField(14);
        JTextField txtStok   = AppTheme.makeTextField(14);
        JTextField txtSatuan = AppTheme.makeTextField(14);
        JTextField txtDesk   = AppTheme.makeTextField(14);
        JComboBox<KategoriProduk> cmbKat = AppTheme.makeComboBox();
        JCheckBox  chkAktif  = new JCheckBox("Produk Aktif", true);
        chkAktif.setBackground(AppTheme.BG_CARD);
        chkAktif.setForeground(AppTheme.TEXT_PRIMARY);
        chkAktif.setFont(AppTheme.FONT_BODY);

        List<KategoriProduk> kats = new ProdukDAO().getAllKategori();
        for (KategoriProduk k : kats) cmbKat.addItem(k);

        if (isEdit) {
            txtKode.setText(existing.getKode());
            txtNama.setText(existing.getNama());
            txtHarga.setText(existing.getHarga().toPlainString());
            txtStok.setText(String.valueOf(existing.getStok()));
            txtSatuan.setText(existing.getSatuan());
            txtDesk.setText(existing.getDeskripsi());
            chkAktif.setSelected(existing.isAktif());
            for (int i = 0; i < cmbKat.getItemCount(); i++) {
                if (cmbKat.getItemAt(i).getId() == existing.getKategoriId()) {
                    cmbKat.setSelectedIndex(i); break;
                }
            }
        }

        Object[][] rows = {
            {"Kode Produk *", txtKode},
            {"Nama Produk *",  txtNama},
            {"Kategori",       cmbKat},
            {"Harga (Rp) *",   txtHarga},
            {"Stok",           txtStok},
            {"Satuan",         txtSatuan},
            {"Deskripsi",      txtDesk},
            {"",               chkAktif}
        };

        for (int i = 0; i < rows.length; i++) {
            gbc.gridy = i;
            gbc.gridx = 0; gbc.weightx = 0.32;
            JLabel lbl = AppTheme.makeLabel((String) rows[i][0]);
            form.add(lbl, gbc);
            gbc.gridx = 1; gbc.weightx = 0.68;
            form.add((Component) rows[i][1], gbc);
        }
        panel.add(form, BorderLayout.CENTER);

        // Footer tombol
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(AppTheme.BG_SIDEBAR);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER_COLOR));

        JButton btnCancel = AppTheme.makeOutlineButton("Batal", AppTheme.ACCENT_RED);
        JButton btnSave   = AppTheme.makeButton(isEdit ? "💾 Simpan" : "➕ Tambah",
            isEdit ? new Color(90, 150, 210) : AppTheme.ACCENT_GREEN);
        btnSave.setPreferredSize(new Dimension(130, 38));

        footer.add(btnCancel);
        footer.add(btnSave);
        panel.add(footer, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
            try {
                if (txtKode.getText().isBlank() || txtNama.getText().isBlank()
                        || txtHarga.getText().isBlank()) {
                    JOptionPane.showMessageDialog(dialog,
                        "Kode, Nama, dan Harga wajib diisi!", "Validasi",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Produk p = isEdit ? existing : new Produk();
                p.setKode(txtKode.getText().trim());
                p.setNama(txtNama.getText().trim());
                p.setHarga(new BigDecimal(txtHarga.getText().trim()));
                p.setStok(txtStok.getText().isBlank() ? 0 : Integer.parseInt(txtStok.getText().trim()));
                p.setSatuan(txtSatuan.getText().trim());
                p.setDeskripsi(txtDesk.getText().trim());
                p.setAktif(chkAktif.isSelected());
                KategoriProduk kat = (KategoriProduk) cmbKat.getSelectedItem();
                if (kat == null) {
                    JOptionPane.showMessageDialog(dialog, "Pilih kategori terlebih dahulu!", "Validasi",JOptionPane.WARNING_MESSAGE);
                    return;
                }
                p.setKategoriId(kat.getId());

                ProdukDAO dao = new ProdukDAO();
                boolean ok = isEdit ? dao.updateProduk(p) : dao.tambahProduk(p);
                JOptionPane.showMessageDialog(dialog,
                    ok ? "✓ Data berhasil disimpan!" : "✗ Gagal menyimpan data.",
                    ok ? "Sukses" : "Error",
                    ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                if (ok) {
                    loadProdukTable(model, null);
                    loadAllProdukKasir(); // refresh juga di panel kasir
                    dialog.dispose();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Harga dan stok harus berupa angka!", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RIWAYAT TRANSAKSI PANEL
    // ═══════════════════════════════════════════════════════════════════════
private JPanel buildRiwayatPanel() {
    JPanel panel = new JPanel(new BorderLayout(0, 0));
    panel.setBackground(AppTheme.BG_DARK);

    // Header
    JPanel header = new JPanel(new BorderLayout(10, 0));
    header.setBackground(AppTheme.BG_CARD);
    header.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_COLOR),
        BorderFactory.createEmptyBorder(12, 18, 12, 18)));
    JLabel title = new JLabel("📋  Riwayat Transaksi");
    title.setFont(AppTheme.FONT_TITLE);
    title.setForeground(AppTheme.TEXT_PRIMARY);
    header.add(title, BorderLayout.WEST);

    JButton btnRefresh = AppTheme.makeOutlineButton("↻ Refresh", AppTheme.TEXT_SECONDARY);
    JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    btnWrap.setOpaque(false);
    btnWrap.add(btnRefresh);
    header.add(btnWrap, BorderLayout.EAST);
    panel.add(header, BorderLayout.NORTH);

    // Tabel
    String[] cols = {"No Transaksi", "Tanggal", "Kasir", 
                     "Total", "Metode", "Status"};
    DefaultTableModel riwayatModel = new DefaultTableModel(cols, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    JTable table = new JTable(riwayatModel);
    styleTable(table);
    table.getColumnModel().getColumn(0).setPreferredWidth(140);
    table.getColumnModel().getColumn(1).setPreferredWidth(130);
    table.getColumnModel().getColumn(2).setPreferredWidth(120);
    table.getColumnModel().getColumn(3).setPreferredWidth(110);
    table.getColumnModel().getColumn(4).setPreferredWidth(80);
    table.getColumnModel().getColumn(5).setPreferredWidth(70);

    JScrollPane scroll = new JScrollPane(table);
    scroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_COLOR));
    scroll.getViewport().setBackground(AppTheme.BG_TABLE_ROW);
    panel.add(scroll, BorderLayout.CENTER);

    // Load data
    Runnable loadData = () -> {
        riwayatModel.setRowCount(0);
        NumberFormat rupiah = NumberFormat.getInstance(new java.util.Locale("id","ID"));
        List<Transaksi> list = new TransaksiDAO().getAllTransaksi();
        for (Transaksi t : list) {
            riwayatModel.addRow(new Object[]{
                t.getNoTransaksi(),
                t.getTanggalFormatted(),
                t.getKasirNama(),
                "Rp " + rupiah.format(t.getTotalBayar()),
                t.getMetodeBayar(),
                t.getStatus()
            });
        }
    };
    SwingUtilities.invokeLater(loadData);
    btnRefresh.addActionListener(e -> loadData.run());

    return panel;
}

    // ═══════════════════════════════════════════════════════════════════════
    // STYLING TABLE
    // ═══════════════════════════════════════════════════════════════════════
    private void styleTable(JTable table) {
        table.setBackground(AppTheme.BG_TABLE_ROW);
        table.setForeground(AppTheme.TEXT_PRIMARY);
        table.setGridColor(AppTheme.BORDER_COLOR);
        table.setFont(AppTheme.FONT_BODY);
        table.setRowHeight(33);
        table.setSelectionBackground(new Color(226, 135, 67, 50));
        table.setSelectionForeground(AppTheme.TEXT_PRIMARY);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setBackground(AppTheme.BG_SIDEBAR);
        table.getTableHeader().setForeground(new Color(181, 98, 42));
        table.getTableHeader().setFont(AppTheme.FONT_SUBTITLE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.setFillsViewportHeight(true);

        // Alternating row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    setBackground(row % 2 == 0 ? AppTheme.BG_TABLE_ROW : AppTheme.BG_TABLE_ALT);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                setForeground(AppTheme.TEXT_PRIMARY);
                return this;
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WrapLayout — untuk product grid agar auto-wrap
    // ═══════════════════════════════════════════════════════════════════════
    private static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);
                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0, rowHeight = 0;
                int nmembers = target.getComponentCount();
                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                            dim.height += rowHeight + vgap;
                            dim.width = Math.max(dim.width, rowWidth);
                            rowWidth = 0; rowHeight = 0;
                        }
                        if (rowWidth > 0) rowWidth += hgap;
                        rowWidth += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }
                dim.height += rowHeight;
                dim.height += insets.top + insets.bottom + vgap * 2;
                dim.width = Math.max(dim.width, rowWidth) + insets.left + insets.right + hgap * 2;
                return dim;
            }
        }
    }
}