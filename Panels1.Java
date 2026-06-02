import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

// ============================================================
//  LoginFrame — Layar login pertama kali membuka aplikasi
//
//  FUNGSI:
//   initUI()       : bangun tampilan kartu login dengan gradient background
//   doLogin()      : ambil username+password, cocokkan ke DB lewat UserDAO
//   shakeWindow()  : animasi goyang jendela saat login gagal
// ============================================================
class LoginFrame extends JFrame {
    private JTextField    txtUser;
    private JPasswordField txtPass;
    private JLabel        lblError;
    private final UserDAO userDAO = new UserDAO();

    public LoginFrame() {
        AppTheme.applyTheme();
        setTitle("Larisole POS — Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 540);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppTheme.BG_DARK);
        initUI();
    }

    private void initUI() {
        // Background panel dengan gradient + lingkaran dekoratif
        JPanel center = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                    0, 0, AppTheme.BG_DARK, 0, getHeight(), new Color(30, 22, 10));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 120, 40, 18));
                g2.fillOval(-80, -80, 280, 280);
                g2.fillOval(getWidth() - 100, getHeight() - 100, 200, 200);
                g2.dispose();
            }
        };
        center.setLayout(new GridBagLayout());
        center.setOpaque(false);

        // Kartu putih (gelap) di tengah
        JPanel card = AppTheme.makeCard(18);
        card.setPreferredSize(new Dimension(340, 410));
        card.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.gridx   = 0;
        gbc.gridy   = 0;
        gbc.gridwidth = 1;

        // Logo & judul
        JLabel logo = new JLabel("🥐", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        gbc.insets = new Insets(24, 0, 4, 0);
        card.add(logo, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 2, 0);
        JLabel title = new JLabel("LARISOLE POS", SwingConstants.CENTER);
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.ACCENT_ORANGE);
        card.add(title, gbc);

        gbc.gridy++;
        JLabel sub = new JLabel("Sistem Kasir Modern", SwingConstants.CENTER);
        sub.setFont(AppTheme.FONT_SMALL);
        sub.setForeground(AppTheme.TEXT_MUTED);
        card.add(sub, gbc);

        // Form input
        gbc.gridy++;
        gbc.insets = new Insets(20, 24, 4, 24);
        card.add(AppTheme.makeLabel("Username"), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 24, 10, 24);
        txtUser = AppTheme.makeTextField(15);
        card.add(txtUser, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(4, 24, 4, 24);
        card.add(AppTheme.makeLabel("Password"), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 24, 4, 24);
        txtPass = new JPasswordField(15);
        txtPass.setBackground(AppTheme.BG_INPUT);
        txtPass.setForeground(AppTheme.TEXT_PRIMARY);
        txtPass.setCaretColor(AppTheme.ACCENT_ORANGE);
        txtPass.setFont(AppTheme.FONT_BODY);
        txtPass.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER_COLOR),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        card.add(txtPass, gbc);

        // Label error (awalnya kosong)
        gbc.gridy++;
        lblError = new JLabel(" ", SwingConstants.CENTER);
        lblError.setFont(AppTheme.FONT_SMALL);
        lblError.setForeground(AppTheme.ACCENT_RED);
        card.add(lblError, gbc);

        // Tombol masuk
        gbc.gridy++;
        gbc.insets = new Insets(4, 24, 24, 24);
        JButton btnLogin = AppTheme.makeButton("MASUK", AppTheme.ACCENT_ORANGE);
        btnLogin.setPreferredSize(new Dimension(280, 42));
        card.add(btnLogin, gbc);

        // Event
        txtUser.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) txtPass.requestFocus();
            }
        });
        txtPass.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        });
        btnLogin.addActionListener(e -> doLogin());

        center.add(card);
        add(center, BorderLayout.CENTER);

        // Footer
        JLabel footer = new JLabel("© 2026 Larisole Malang", SwingConstants.CENTER);
        footer.setFont(AppTheme.FONT_SMALL);
        footer.setForeground(AppTheme.TEXT_MUTED);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 0, 12, 0));
        add(footer, BorderLayout.SOUTH);
    }

    /** Proses login: validasi input → cek ke DB → buka MainFrame atau tampilkan error */
    private void doLogin() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            lblError.setText("Username dan password wajib diisi!");
            return;
        }
        lblError.setText("Memeriksa...");
        try {
            User u = userDAO.login(user, pass);
            if (u != null) {
                dispose();
                final User finalU = u;
                SwingUtilities.invokeLater(() -> new MainFrame(finalU).setVisible(true));
            } else {
                lblError.setText("Username atau password salah!");
                shakeWindow();
                txtPass.setText("");
                txtPass.requestFocus();
            }
        } catch (Exception ex) {
            lblError.setText("Koneksi database gagal!");
            ex.printStackTrace();
        }
    }

    /** Animasi shake saat login gagal */
    private void shakeWindow() {
        final Point original = getLocation();
        final int[] count = {0};
        Timer timer = new Timer(40, null);
        timer.addActionListener(e -> {
            int offset = (count[0] % 2 == 0) ? 8 : -8;
            setLocation(original.x + offset, original.y);
            count[0]++;
            if (count[0] >= 8) {
                ((Timer) e.getSource()).stop();
                setLocation(original);
            }
        });
        timer.start();
    }
}

// ============================================================
//  TransaksiPanel — Panel utama untuk membuat transaksi baru
//
//  FUNGSI:
//   buildFormPanel()       : panel kiri (info trx, tambah item, summary, tombol aksi)
//   buildInfoCard()        : no transaksi, antrian, tanggal, customer, metode, kasir
//   buildItemInputCard()   : combobox produk, spinner qty, tombol Tambah/Update/Hapus
//   buildSummaryCard()     : subtotal, diskon, total, dibayar, kembalian
//   buildActionButtons()   : Proses, Struk, Bersih, Pending
//   buildRightPanel()      : tabel keranjang + tabel riwayat hari ini
//   tambahItem()           : tambah produk ke keranjang (cek stok, merge jika sama)
//   updateItem()           : update qty item yang dipilih di tabel keranjang
//   hapusItem()            : hapus item dari keranjang
//   hitungTotal()          : hitung subtotal & total setelah diskon berubah
//   hitungKembalian()      : hitung kembalian dari input dibayar
//   prosesTransaksi()      : simpan transaksi ke DB lewat TransaksiDAO
//   cetakStruk()           : buka StrukDialog
//   bersihkanForm()        : reset semua field + keranjang
//   generateNoTrx()        : preview nomor transaksi berikutnya dari DB
//   muatRiwayatHariIni()   : isi tabel riwayat dari DB (background thread)
// ============================================================
class TransaksiPanel extends JPanel {
    private User currentUser;
    private List<ItemKeranjang> keranjang    = new ArrayList<>();
    private List<Produk>        semuaProduk  = new ArrayList<>();

    // Komponen form kiri
    private JTextField         txtCustomer, txtDibayar, txtDiskon;
    private JComboBox<Produk>  cmbProduk;
    private JSpinner           spnQty;
    private JComboBox<String>  cmbMetodeBayar;
    private JLabel             lblNoTrx, lblAntrian, lblTanggal;
    private JLabel             lblSubtotal, lblTotal, lblKembalian;

    // Tabel keranjang & riwayat
    private JTable             tblKeranjang;
    private DefaultTableModel  tblModel;
    private JTable             tblRiwayat;
    private DefaultTableModel  riwayatModel;

    private final ProdukDAO     produkDAO     = new ProdukDAO();
    private final TransaksiDAO  transaksiDAO  = new TransaksiDAO();
    private final PengaturanDAO pengaturanDAO = new PengaturanDAO();

    private static final NumberFormat     CURRENCY = NumberFormat.getInstance(new Locale("id","ID"));
    private static final DateTimeFormatter FMT      = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public TransaksiPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout(12, 0));
        setOpaque(false);
        loadProduk();
        initUI();
        muatRiwayatHariIni();
        generateNoTrx();
    }

    private void loadProduk() {
        try { semuaProduk = produkDAO.getSemuaProduk(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void initUI() {
        add(buildFormPanel(),  BorderLayout.WEST);
        add(buildRightPanel(), BorderLayout.CENTER);
    }

    // ── PANEL KIRI ─────────────────────────────────────────────────
    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(310, 0));

        JPanel top = new JPanel(new GridLayout(0, 1, 0, 8));
        top.setOpaque(false);
        top.add(buildInfoCard());
        top.add(buildItemInputCard());
        top.add(buildSummaryCard());
        top.add(buildActionButtons());

        panel.add(top, BorderLayout.NORTH);
        return panel;
    }

    private JPanel buildInfoCard() {
        JPanel card = AppTheme.makeCard(12);
        card.setLayout(new GridLayout(0, 2, 6, 6));
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        lblNoTrx   = makeValueLabel("#-");
        lblAntrian = makeValueLabel("-");
        lblTanggal = makeValueLabel(LocalDateTime.now().format(FMT));

        addFormRow(card, "No. Transaksi", lblNoTrx);
        addFormRow(card, "No. Antrian",   lblAntrian);
        addFormRow(card, "Tanggal",        lblTanggal);

        txtCustomer = AppTheme.makeTextField(10);
        txtCustomer.setPreferredSize(new Dimension(140, 30));
        addFormRow(card, "Nama Customer", txtCustomer);

        String[] metodes = {"Tunai", "QRIS", "Transfer", "Kartu"};
        cmbMetodeBayar = new JComboBox<>(metodes);
        styleCombo(cmbMetodeBayar);
        addFormRow(card, "Metode Bayar", cmbMetodeBayar);

        addFormRow(card, "Kasir", makeValueLabel(currentUser.getNamaLengkap()));
        return card;
    }

    private JPanel buildItemInputCard() {
        JPanel card = AppTheme.makeCard(12);
        card.setLayout(new GridLayout(0, 1, 0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel header = new JLabel("+ Tambah Item");
        header.setFont(AppTheme.FONT_SUBTITLE);
        header.setForeground(AppTheme.ACCENT_ORANGE);
        card.add(header);

        cmbProduk = new JComboBox<>();
        for (Produk p : semuaProduk) cmbProduk.addItem(p);
        styleCombo(cmbProduk);
        card.add(cmbProduk);

        JPanel qtyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        qtyRow.setOpaque(false);
        spnQty = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        spnQty.setPreferredSize(new Dimension(80, 30));
        styleSpinner(spnQty);
        qtyRow.add(AppTheme.makeLabel("Qty: "));
        qtyRow.add(spnQty);
        card.add(qtyRow);

        JPanel btnRow = new JPanel(new GridLayout(1, 3, 4, 0));
        btnRow.setOpaque(false);
        JButton btnTambah = AppTheme.makeButton("Tambah", AppTheme.ACCENT_ORANGE);
        JButton btnUpdate = AppTheme.makeButton("Update",  AppTheme.ACCENT_BLUE);
        JButton btnHapus  = AppTheme.makeButton("Hapus",   AppTheme.ACCENT_RED);
        btnTambah.setPreferredSize(new Dimension(85, 34));
        btnUpdate.setPreferredSize(new Dimension(85, 34));
        btnHapus.setPreferredSize(new Dimension(85, 34));
        btnRow.add(btnTambah);
        btnRow.add(btnUpdate);
        btnRow.add(btnHapus);
        card.add(btnRow);

        btnTambah.addActionListener(e -> tambahItem());
        btnUpdate.addActionListener(e -> updateItem());
        btnHapus.addActionListener(e ->  hapusItem());
        return card;
    }

    private JPanel buildSummaryCard() {
        JPanel card = AppTheme.makeCard(12);
        card.setLayout(new GridLayout(0, 2, 6, 8));
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        lblSubtotal = makeValueLabel("Rp 0");
        txtDiskon   = AppTheme.makeTextField(8);
        txtDiskon.setText("0");
        lblTotal    = makeValueLabel("Rp 0");
        lblTotal.setFont(AppTheme.FONT_PRICE);
        lblTotal.setForeground(AppTheme.ACCENT_ORANGE);
        txtDibayar  = AppTheme.makeTextField(8);
        lblKembalian = makeValueLabel("Rp 0");
        lblKembalian.setForeground(AppTheme.ACCENT_GREEN);

        addFormRow(card, "Subtotal",    lblSubtotal);
        addFormRow(card, "Diskon (Rp)", txtDiskon);
        addFormRow(card, "TOTAL",       lblTotal);
        addFormRow(card, "Dibayar",     txtDibayar);
        addFormRow(card, "Kembalian",   lblKembalian);

        txtDibayar.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { hitungKembalian(); }
        });
        txtDiskon.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { hitungTotal(); }
        });
        return card;
    }

    private JPanel buildActionButtons() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.setOpaque(false);

        JButton btnProses  = AppTheme.makeButton("Proses Bayar", AppTheme.ACCENT_ORANGE);
        JButton btnStruk   = AppTheme.makeButton("Cetak Struk",  AppTheme.ACCENT_BLUE);
        JButton btnBersih  = AppTheme.makeButton("Bersihkan",    AppTheme.TEXT_MUTED);
        JButton btnPending = AppTheme.makeButton("Simpan Pending",AppTheme.ACCENT_YELLOW);

        btnProses.setPreferredSize(new Dimension(130, 42));
        btnStruk.setPreferredSize(new Dimension(130, 42));
        btnBersih.setPreferredSize(new Dimension(130, 42));
        btnPending.setPreferredSize(new Dimension(130, 42));

        panel.add(btnProses);
        panel.add(btnStruk);
        panel.add(btnBersih);
        panel.add(btnPending);

        btnProses.addActionListener(e  -> prosesTransaksi("Lunas"));
        btnPending.addActionListener(e -> prosesTransaksi("Pending"));
        btnBersih.addActionListener(e  -> bersihkanForm());
        btnStruk.addActionListener(e   -> cetakStruk(null));
        return panel;
    }

    // ── PANEL KANAN ────────────────────────────────────────────────
    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        // Tabel keranjang (item yang sedang dipesan)
        JLabel hdr = new JLabel("Item dalam transaksi ini");
        hdr.setFont(AppTheme.FONT_SUBTITLE);
        hdr.setForeground(AppTheme.TEXT_PRIMARY);
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        String[] cols = {"#", "Nama Produk", "Qty", "Harga", "Subtotal"};
        tblModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblKeranjang = buildStyledTable(tblModel);
        tblKeranjang.getColumnModel().getColumn(0).setPreferredWidth(30);
        tblKeranjang.getColumnModel().getColumn(1).setPreferredWidth(160);
        tblKeranjang.getColumnModel().getColumn(2).setPreferredWidth(45);
        tblKeranjang.getColumnModel().getColumn(3).setPreferredWidth(85);
        tblKeranjang.getColumnModel().getColumn(4).setPreferredWidth(90);

        JScrollPane spKeranjang = styledScroll(tblKeranjang);
        spKeranjang.setPreferredSize(new Dimension(0, 220));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(hdr, BorderLayout.NORTH);
        topPanel.add(spKeranjang, BorderLayout.CENTER);

        // Tabel riwayat hari ini
        JLabel hdrR = new JLabel("Riwayat transaksi hari ini");
        hdrR.setFont(AppTheme.FONT_SUBTITLE);
        hdrR.setForeground(AppTheme.TEXT_PRIMARY);
        hdrR.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        String[] colsR = {"No.Trx", "Customer", "Waktu", "Total", "Metode", "Status"};
        riwayatModel = new DefaultTableModel(colsR, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblRiwayat = buildStyledTable(riwayatModel);
        // Renderer warna kolom Status
        tblRiwayat.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                lbl.setForeground(AppTheme.statusColor(v != null ? v.toString() : ""));
                lbl.setFont(AppTheme.FONT_BODY);
                return lbl;
            }
        });

        JPanel botPanel = new JPanel(new BorderLayout());
        botPanel.setOpaque(false);
        botPanel.add(hdrR,                       BorderLayout.NORTH);
        botPanel.add(styledScroll(tblRiwayat),   BorderLayout.CENTER);

        // Split pane vertikal
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, botPanel);
        split.setDividerLocation(260);
        split.setBorder(null);
        split.setOpaque(false);
        split.setBackground(AppTheme.BG_DARK);

        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    // ── OPERASI KERANJANG ──────────────────────────────────────────
    private void tambahItem() {
        Produk p = (Produk) cmbProduk.getSelectedItem();
        if (p == null) return;
        int qty = (Integer) spnQty.getValue();
        if (qty > p.getStok()) {
            showWarning("Stok tidak cukup! Tersedia: " + p.getStok() + " " + p.getSatuan());
            return;
        }
        // Jika produk sudah ada di keranjang, tambah qty-nya saja
        for (ItemKeranjang item : keranjang) {
            if (item.getProdukId() == p.getId()) {
                item.setQty(item.getQty() + qty);
                refreshTabelKeranjang();
                hitungTotal();
                return;
            }
        }
        keranjang.add(new ItemKeranjang(p, qty));
        refreshTabelKeranjang();
        hitungTotal();
    }

    private void updateItem() {
        int row = tblKeranjang.getSelectedRow();
        if (row < 0) { showWarning("Pilih item yang ingin diupdate!"); return; }
        keranjang.get(row).setQty((Integer) spnQty.getValue());
        refreshTabelKeranjang();
        hitungTotal();
    }

    private void hapusItem() {
        int row = tblKeranjang.getSelectedRow();
        if (row < 0) { showWarning("Pilih item yang ingin dihapus!"); return; }
        keranjang.remove(row);
        refreshTabelKeranjang();
        hitungTotal();
    }

    private void refreshTabelKeranjang() {
        tblModel.setRowCount(0);
        int no = 1;
        for (ItemKeranjang item : keranjang) {
            tblModel.addRow(new Object[]{
                no++,
                item.getNamaProduk(),
                item.getQty(),
                "Rp " + CURRENCY.format(item.getHargaSatuan()),
                "Rp " + CURRENCY.format(item.getSubtotal())
            });
        }
    }

    private void hitungTotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemKeranjang item : keranjang) subtotal = subtotal.add(item.getSubtotal());
        BigDecimal diskon = BigDecimal.ZERO;
        try { diskon = new BigDecimal(txtDiskon.getText().replaceAll("[^0-9]", "")); }
        catch (Exception ignored) {}
        BigDecimal total = subtotal.subtract(diskon);
        lblSubtotal.setText("Rp " + CURRENCY.format(subtotal));
        lblTotal.setText("Rp " + CURRENCY.format(total));
        hitungKembalian();
    }

    private void hitungKembalian() {
        try {
            String ts = lblTotal.getText().replaceAll("[^0-9]", "");
            String ds = txtDibayar.getText().replaceAll("[^0-9]", "");
            BigDecimal total   = new BigDecimal(ts.isEmpty() ? "0" : ts);
            BigDecimal dibayar = new BigDecimal(ds.isEmpty() ? "0" : ds);
            BigDecimal kembalian = dibayar.subtract(total);
            lblKembalian.setText("Rp " + CURRENCY.format(kembalian.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : kembalian));
            lblKembalian.setForeground(kembalian.compareTo(BigDecimal.ZERO) >= 0 ? AppTheme.ACCENT_GREEN : AppTheme.ACCENT_RED);
        } catch (Exception ignored) {}
    }

    // ── PROSES TRANSAKSI ───────────────────────────────────────────
    private void prosesTransaksi(String status) {
        if (keranjang.isEmpty()) { showWarning("Keranjang masih kosong!"); return; }

        BigDecimal dibayar = BigDecimal.ZERO;
        try { dibayar = new BigDecimal(txtDibayar.getText().replaceAll("[^0-9]", "")); }
        catch (Exception ignored) {}

        BigDecimal total = BigDecimal.ZERO;
        try { total = new BigDecimal(lblTotal.getText().replaceAll("[^0-9]", "")); }
        catch (Exception ignored) {}

        if ("Lunas".equals(status) && dibayar.compareTo(total) < 0) {
            showWarning("Uang yang dibayarkan kurang dari total!");
            return;
        }

        BigDecimal diskon = BigDecimal.ZERO;
        try { diskon = new BigDecimal(txtDiskon.getText().replaceAll("[^0-9]", "")); }
        catch (Exception ignored) {}

        Transaksi trx = new Transaksi();
        trx.setNamaCustomer(txtCustomer.getText().trim().isEmpty() ? "Umum" : txtCustomer.getText().trim());
        trx.setKasirId(currentUser.getId());
        trx.setMetodeBayar(cmbMetodeBayar.getSelectedItem().toString());
        trx.setDibayar(dibayar);
        trx.setDiskon(diskon);
        trx.setStatus(status);
        trx.setCatatan("");

        try {
            Transaksi hasil = transaksiDAO.simpanTransaksi(trx, keranjang);
            JOptionPane.showMessageDialog(this,
                "Transaksi " + hasil.getNoTransaksi() + " berhasil!\nAntrian: " + hasil.getNoAntrian(),
                "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            cetakStruk(hasil);
            bersihkanForm();
            muatRiwayatHariIni();
        } catch (Exception e) {
            showError("Gagal menyimpan transaksi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cetakStruk(Transaksi trx) {
        if (trx == null && keranjang.isEmpty()) {
            showWarning("Tidak ada data untuk dicetak!");
            return;
        }
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        new StrukDialog(parent, trx, keranjang, currentUser, pengaturanDAO).setVisible(true);
    }

    private void bersihkanForm() {
        keranjang.clear();
        tblModel.setRowCount(0);
        txtCustomer.setText("");
        txtDiskon.setText("0");
        txtDibayar.setText("");
        lblSubtotal.setText("Rp 0");
        lblTotal.setText("Rp 0");
        lblKembalian.setText("Rp 0");
        generateNoTrx();
    }

    private void generateNoTrx() {
        try {
            int noTrx = Integer.parseInt(pengaturanDAO.getNilai("no_transaksi")) + 1;
            int noAnt = Integer.parseInt(pengaturanDAO.getNilai("no_antrian")) + 1;
            lblNoTrx.setText("#" + noTrx);
            lblAntrian.setText(String.valueOf(noAnt));
            lblTanggal.setText(LocalDateTime.now().format(FMT));
        } catch (Exception e) {
            lblNoTrx.setText("#???");
        }
    }

    private void muatRiwayatHariIni() {
        new SwingWorker<List<Transaksi>, Void>() {
            @Override protected List<Transaksi> doInBackground() throws Exception {
                return transaksiDAO.getTransaksiHariIni();
            }
            @Override protected void done() {
                try {
                    List<Transaksi> list = get();
                    riwayatModel.setRowCount(0);
                    DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
                    for (Transaksi trx : list) {
                        riwayatModel.addRow(new Object[]{
                            trx.getNoTransaksi(),
                            trx.getNamaCustomer(),
                            trx.getTanggal() != null ? trx.getTanggal().format(tf) : "-",
                            "Rp " + CURRENCY.format(trx.getTotalBayar()),
                            trx.getMetodeBayar(),
                            trx.getStatus()
                        });
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }.execute();
    }

    // ── HELPERS ────────────────────────────────────────────────────
    private JLabel makeValueLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(AppTheme.FONT_MONO);
        lbl.setForeground(AppTheme.TEXT_PRIMARY);
        return lbl;
    }

    private void addFormRow(JPanel card, String label, Component comp) {
        card.add(AppTheme.makeLabel(label));
        card.add(comp);
    }

    private JTable buildStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(AppTheme.BG_TABLE_ROW);
        table.setForeground(AppTheme.TEXT_PRIMARY);
        table.setFont(AppTheme.FONT_BODY);
        table.setRowHeight(30);
        table.setGridColor(AppTheme.SEPARATOR);
        table.setSelectionBackground(new Color(255, 120, 40, 60));
        table.setSelectionForeground(AppTheme.TEXT_PRIMARY);
        table.setShowVerticalLines(false);
        table.getTableHeader().setBackground(AppTheme.BG_SIDEBAR);
        table.getTableHeader().setForeground(AppTheme.ACCENT_ORANGE);
        table.getTableHeader().setFont(AppTheme.FONT_SUBTITLE);
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
        return table;
    }

    private JScrollPane styledScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(AppTheme.BG_CARD);
        sp.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_COLOR));
        sp.getViewport().setBackground(AppTheme.BG_TABLE_ROW);
        return sp;
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setBackground(AppTheme.BG_INPUT);
        combo.setForeground(AppTheme.TEXT_PRIMARY);
        combo.setFont(AppTheme.FONT_BODY);
        combo.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_COLOR));
    }

    private void styleSpinner(JSpinner spinner) {
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getComponent(0).setBackground(AppTheme.BG_INPUT);
        editor.getTextField().setForeground(AppTheme.TEXT_PRIMARY);
        editor.getTextField().setFont(AppTheme.FONT_BODY);
    }

    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Perhatian", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}