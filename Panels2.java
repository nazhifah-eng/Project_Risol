import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class Panels2 extends JFrame {

    private final User currentUser;
    private JPanel contentPanel;
    private final CardLayout cardLayout = new CardLayout();
    private final NumberFormat rupiah = NumberFormat.getInstance(new Locale("id", "ID"));

    private JButton btnDashboard, btnProduk, btnTransaksi, btnKeluar;

    public Panels2(User user) {
        this.currentUser = user;
        setTitle("Larisole — " + user.getNamaLengkap());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 540));
        initComponents();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(AppTheme.BG_DARK);

        root.add(buildSidebar(), BorderLayout.WEST);

        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(AppTheme.BG_DARK);
        contentPanel.add(buildDashboardPanel(), "dashboard");
        contentPanel.add(buildProdukPanel(),    "produk");
        contentPanel.add(buildTransaksiPanel(), "transaksi");

        root.add(contentPanel, BorderLayout.CENTER);
        setContentPane(root);

        showPanel("dashboard");
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(AppTheme.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, AppTheme.BORDER_COLOR));

        JLabel logo = new JLabel("LARISOLE", SwingConstants.CENTER);
        logo.setFont(AppTheme.FONT_TITLE);
        logo.setForeground(AppTheme.ACCENT_ORANGE);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setBorder(BorderFactory.createEmptyBorder(24, 0, 8, 0));
        sidebar.add(logo);

        JLabel ver = new JLabel("v1.0", SwingConstants.CENTER);
        ver.setFont(AppTheme.FONT_SMALL);
        ver.setForeground(AppTheme.TEXT_MUTED);
        ver.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(ver);

        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(makeSep());

        btnDashboard  = makeSidebarBtn("🏠  Dashboard");
        btnProduk     = makeSidebarBtn("📦  Produk");
        btnTransaksi  = makeSidebarBtn("🛒  Transaksi");

        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(btnDashboard);
        sidebar.add(btnProduk);
        sidebar.add(btnTransaksi);

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(makeSep());

        JLabel lblUser = new JLabel(currentUser.getNamaLengkap(), SwingConstants.CENTER);
        lblUser.setFont(AppTheme.FONT_SMALL);
        lblUser.setForeground(AppTheme.TEXT_SECONDARY);
        lblUser.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblUser.setBorder(BorderFactory.createEmptyBorder(8, 4, 2, 4));
        sidebar.add(lblUser);

        JLabel lblRole = new JLabel("[" + currentUser.getRole() + "]", SwingConstants.CENTER);
        lblRole.setFont(AppTheme.FONT_SMALL);
        lblRole.setForeground(AppTheme.ACCENT_ORANGE);
        lblRole.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblRole);

        btnKeluar = makeSidebarBtn("🚪  Keluar");
        btnKeluar.setForeground(AppTheme.ACCENT_RED);
        sidebar.add(btnKeluar);
        sidebar.add(Box.createVerticalStrut(12));

        btnDashboard.addActionListener(e -> showPanel("dashboard"));
        btnProduk.addActionListener(e    -> showPanel("produk"));
        btnTransaksi.addActionListener(e -> showPanel("transaksi"));
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

    private JButton makeSidebarBtn(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover() || getModel().isSelected()) {
                    g2.setColor(new Color(255, 120, 40, 30));
                    g2.fillRoundRect(4, 2, getWidth() - 8, getHeight() - 4, 8, 8);
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
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 8));
        btn.setMaximumSize(new Dimension(200, 44));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JSeparator makeSep() {
        JSeparator sep = new JSeparator();
        sep.setForeground(AppTheme.SEPARATOR);
        sep.setMaximumSize(new Dimension(200, 1));
        return sep;
    }

    private void showPanel(String name) {
        cardLayout.show(contentPanel, name);
    }

    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppTheme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("Dashboard");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(2, 3, 16, 16));
        cards.setBackground(AppTheme.BG_DARK);
        cards.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        cards.add(makeStatCard("Total Produk",  "...", AppTheme.ACCENT_BLUE));
        cards.add(makeStatCard("Stok Menipis",  "...", AppTheme.ACCENT_YELLOW));
        cards.add(makeStatCard("Transaksi Hari Ini", "...", AppTheme.ACCENT_GREEN));
        cards.add(makeStatCard("Kasir Aktif",   currentUser.getNamaLengkap(), AppTheme.ACCENT_ORANGE));
        cards.add(makeStatCard("Role",          currentUser.getRole().toUpperCase(), AppTheme.ACCENT_BLUE));
        cards.add(makeStatCard("Status DB",     "Connected", AppTheme.ACCENT_GREEN));

        panel.add(cards, BorderLayout.CENTER);
        return panel;
    }

    private JPanel makeStatCard(String label, String value, Color accent) {
        JPanel card = AppTheme.makeCard(12);
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(160, 90));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel valLabel = new JLabel(value, SwingConstants.CENTER);
        valLabel.setFont(AppTheme.FONT_PRICE);
        valLabel.setForeground(accent);
        card.add(valLabel, gbc);

        gbc.gridy = 1;
        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(AppTheme.FONT_SMALL);
        lbl.setForeground(AppTheme.TEXT_MUTED);
        card.add(lbl, gbc);

        return card;
    }

    private DefaultTableModel produkTableModel;

    private JPanel buildProdukPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(AppTheme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Manajemen Produk");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);
        JTextField txtCari = AppTheme.makeTextField(16);
        txtCari.setPreferredSize(new Dimension(180, 34));
        JButton btnCari      = AppTheme.makeOutlineButton("🔍 Cari", AppTheme.ACCENT_BLUE);
        JButton btnTambah    = AppTheme.makeButton("+ Tambah", AppTheme.ACCENT_GREEN);
        JButton btnEdit      = AppTheme.makeOutlineButton("✏ Edit", AppTheme.ACCENT_YELLOW);
        JButton btnHapus     = AppTheme.makeOutlineButton("🗑 Hapus", AppTheme.ACCENT_RED);
        JButton btnRefresh   = AppTheme.makeOutlineButton("↻ Refresh", AppTheme.TEXT_SECONDARY);

        btnBar.add(txtCari);
        btnBar.add(btnCari);
        btnBar.add(btnRefresh);
        btnBar.add(btnTambah);
        btnBar.add(btnEdit);
        btnBar.add(btnHapus);
        header.add(btnBar, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        String[] cols = {"ID", "Kode", "Nama Produk", "Kategori", "Harga", "Stok", "Satuan", "Aktif"};
        produkTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(produkTableModel);
        styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(AppTheme.BG_TABLE_ROW);
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_COLOR));
        panel.add(scroll, BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> loadProdukTable(produkTableModel, null));
        btnCari.addActionListener(e -> loadProdukTable(produkTableModel, txtCari.getText()));
        txtCari.addActionListener(e -> loadProdukTable(produkTableModel, txtCari.getText()));

        btnTambah.addActionListener(e -> showProdukDialog(null, produkTableModel));
        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Pilih produk terlebih dahulu!"); return; }
            int id = (int) produkTableModel.getValueAt(row, 0);
            Produk p = new ProdukDAO().getProdukById(id);
            if (p != null) showProdukDialog(p, produkTableModel);
        });
        btnHapus.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Pilih produk terlebih dahulu!"); return; }
            int id = (int) produkTableModel.getValueAt(row, 0);
            String nama = (String) produkTableModel.getValueAt(row, 2);
            int ok = JOptionPane.showConfirmDialog(this,
                "Hapus produk \"" + nama + "\"?", "Konfirmasi",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) {
                boolean berhasil = new ProdukDAO().hapusProduk(id);
                JOptionPane.showMessageDialog(this,
                    berhasil ? "Produk dihapus." : "Gagal menghapus produk.");
                loadProdukTable(produkTableModel, null);
            }
        });

        SwingUtilities.invokeLater(() -> loadProdukTable(produkTableModel, null));
        return panel;
    }

    private void loadProdukTable(DefaultTableModel model, String keyword) {
        model.setRowCount(0);
        List<Produk> list;
        ProdukDAO dao = new ProdukDAO();
        list = (keyword == null || keyword.isBlank()) ? dao.getAllProduk() : dao.cariProduk(keyword);
        for (Produk p : list) {
            model.addRow(new Object[]{
                p.getId(), p.getKode(), p.getNama(), p.getKategoriNama(),
                "Rp " + rupiah.format(p.getHarga()),
                p.getStok(), p.getSatuan(), p.isAktif() ? "✔" : "✘"
            });
        }
    }

    private void showProdukDialog(Produk existing, DefaultTableModel model) {
        boolean isEdit = existing != null;
        JDialog dialog = new JDialog(this, isEdit ? "Edit Produk" : "Tambah Produk", true);
        dialog.setSize(420, 460);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(AppTheme.BG_CARD);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.gridx = 0; gbc.weightx = 0.35;

        JTextField txtKode  = AppTheme.makeTextField(14);
        JTextField txtNama  = AppTheme.makeTextField(14);
        JTextField txtHarga = AppTheme.makeTextField(14);
        JTextField txtStok  = AppTheme.makeTextField(14);
        JTextField txtSatuan= AppTheme.makeTextField(14);
        JTextField txtDesk  = AppTheme.makeTextField(14);
        JComboBox<KategoriProduk> cmbKat = AppTheme.makeComboBox();

        List<KategoriProduk> kats = new ProdukDAO().getAllKategori();
        for (KategoriProduk k : kats) cmbKat.addItem(k);

        if (isEdit) {
            txtKode.setText(existing.getKode());
            txtNama.setText(existing.getNama());
            txtHarga.setText(existing.getHarga().toPlainString());
            txtStok.setText(String.valueOf(existing.getStok()));
            txtSatuan.setText(existing.getSatuan());
            txtDesk.setText(existing.getDeskripsi());
            for (int i = 0; i < cmbKat.getItemCount(); i++) {
                if (cmbKat.getItemAt(i).getId() == existing.getKategoriId()) {
                    cmbKat.setSelectedIndex(i); break;
                }
            }
        }

        Object[][] rows = {
            {"Kode",     txtKode},
            {"Nama",     txtNama},
            {"Kategori", cmbKat},
            {"Harga",    txtHarga},
            {"Stok",     txtStok},
            {"Satuan",   txtSatuan},
            {"Deskripsi",txtDesk}
        };

        for (int i = 0; i < rows.length; i++) {
            gbc.gridy = i;
            gbc.gridx = 0; gbc.weightx = 0.3;
            JLabel lbl = AppTheme.makeLabel((String) rows[i][0]);
            panel.add(lbl, gbc);
            gbc.gridx = 1; gbc.weightx = 0.7;
            panel.add((Component) rows[i][1], gbc);
        }

        JButton btnSave = AppTheme.makeButton(isEdit ? "Simpan" : "Tambah", AppTheme.ACCENT_GREEN);
        JButton btnCancel = AppTheme.makeOutlineButton("Batal", AppTheme.ACCENT_RED);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(AppTheme.BG_CARD);
        btnRow.add(btnCancel);
        btnRow.add(btnSave);
        gbc.gridy = rows.length; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(14, 4, 4, 4);
        panel.add(btnRow, gbc);

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
            try {
                Produk p = isEdit ? existing : new Produk();
                p.setKode(txtKode.getText().trim());
                p.setNama(txtNama.getText().trim());
                p.setHarga(new BigDecimal(txtHarga.getText().trim()));
                p.setStok(Integer.parseInt(txtStok.getText().trim()));
                p.setSatuan(txtSatuan.getText().trim());
                p.setDeskripsi(txtDesk.getText().trim());
                KategoriProduk kat = (KategoriProduk) cmbKat.getSelectedItem();
                if (kat != null) p.setKategoriId(kat.getId());
                p.setAktif(true);

                ProdukDAO dao = new ProdukDAO();
                boolean ok = isEdit ? dao.updateProduk(p) : dao.tambahProduk(p);
                JOptionPane.showMessageDialog(dialog,
                    ok ? "Berhasil disimpan!" : "Gagal menyimpan data.");
                if (ok) { loadProdukTable(model, null); dialog.dispose(); }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Harga dan stok harus berupa angka!", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private JPanel buildTransaksiPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(AppTheme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Transaksi");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        JLabel placeholder = new JLabel(
            "<html><center><br><br>Panel Transaksi<br>" +
            "<span style='color:#666'>Fitur ini dapat dikembangkan lebih lanjut.</span></center></html>",
            SwingConstants.CENTER);
        placeholder.setFont(AppTheme.FONT_SUBTITLE);
        placeholder.setForeground(AppTheme.TEXT_MUTED);
        panel.add(placeholder, BorderLayout.CENTER);

        return panel;
    }

    private void styleTable(JTable table) {
        table.setBackground(AppTheme.BG_TABLE_ROW);
        table.setForeground(AppTheme.TEXT_PRIMARY);
        table.setGridColor(AppTheme.BORDER_COLOR);
        table.setFont(AppTheme.FONT_BODY);
        table.setRowHeight(30);
        table.setSelectionBackground(new Color(255, 120, 40, 60));
        table.setSelectionForeground(AppTheme.TEXT_PRIMARY);
        table.setShowGrid(true);
        table.getTableHeader().setBackground(AppTheme.BG_SIDEBAR);
        table.getTableHeader().setForeground(AppTheme.ACCENT_ORANGE);
        table.getTableHeader().setFont(AppTheme.FONT_SUBTITLE);
        table.setFillsViewportHeight(true);
    }
}
