package ui;

import dao.*;
import model.*;
import util.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

// ============================================================
//  ProdukPanel - Manajemen data produk
//  Fitur: Tampilkan daftar, Tambah, Edit, Hapus (soft-delete)
//         Cari produk, Filter kategori
// ============================================================
class ProdukPanel extends JPanel {
    private User currentUser;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtCari;
    private JComboBox<String> cmbFilter;
    private List<Produk> semuaProduk = new ArrayList<>();

    private final ProdukDAO produkDAO = new ProdukDAO();
    private final KategoriDAO kategoriDAO = new KategoriDAO();
    private final NumberFormat CURRENCY = NumberFormat.getInstance(new Locale("id","ID"));

    public ProdukPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        initUI();
        muatData();
    }

    private void initUI() {
        add(buildToolbar(),  BorderLayout.NORTH);
        add(buildTable(),    BorderLayout.CENTER);
        add(buildInfo(),     BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);

        JLabel title = new JLabel("📦 Manajemen Produk");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        txtCari = AppTheme.makeTextField(14);
        txtCari.putClientProperty("JTextField.placeholderText", "Cari produk...");
        txtCari.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { filterTabel(); }
        });

        String[] filters = {"Semua","Risol","Minuman","Snack"};
        cmbFilter = new JComboBox<>(filters);
        cmbFilter.setBackground(AppTheme.BG_INPUT);
        cmbFilter.setForeground(AppTheme.TEXT_PRIMARY);
        cmbFilter.addActionListener(e -> filterTabel());

        JButton btnTambah = AppTheme.makeButton("➕ Tambah", AppTheme.ACCENT_ORANGE);
        JButton btnEdit   = AppTheme.makeButton("✏️ Edit",   AppTheme.ACCENT_BLUE);
        JButton btnHapus  = AppTheme.makeButton("🗑️ Hapus",  AppTheme.ACCENT_RED);
        JButton btnRefresh = AppTheme.makeOutlineButton("🔄 Refresh", AppTheme.TEXT_SECONDARY);

        btnTambah.addActionListener(e -> dialogTambahEdit(null));
        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { showWarning("Pilih produk terlebih dahulu!"); return; }
            int modelRow = table.convertRowIndexToModel(row);
            dialogTambahEdit(semuaProduk.get(modelRow));
        });
        btnHapus.addActionListener(e -> hapusProduk());
        btnRefresh.addActionListener(e -> muatData());

        right.add(new JLabel("🔍") {{ setForeground(AppTheme.TEXT_MUTED); }});
        right.add(txtCari);
        right.add(cmbFilter);
        right.add(btnRefresh);
        right.add(btnTambah);
        if (currentUser.isOwner()) right.add(btnEdit);
        if (currentUser.isOwner()) right.add(btnHapus);

        panel.add(title, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JScrollPane buildTable() {
        String[] cols = {"Kode","Nama Produk","Kategori","Harga","Stok","Satuan","Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setBackground(AppTheme.BG_TABLE_ROW);
        table.setForeground(AppTheme.TEXT_PRIMARY);
        table.setFont(AppTheme.FONT_BODY);
        table.setRowHeight(32);
        table.setGridColor(AppTheme.SEPARATOR);
        table.setSelectionBackground(new Color(255, 120, 40, 50));
        table.setSelectionForeground(AppTheme.TEXT_PRIMARY);
        table.setShowVerticalLines(false);
        table.setAutoCreateRowSorter(true);

        table.getTableHeader().setBackground(AppTheme.BG_SIDEBAR);
        table.getTableHeader().setForeground(AppTheme.ACCENT_ORANGE);
        table.getTableHeader().setFont(AppTheme.FONT_SUBTITLE);

        // Renderer warna stok rendah
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                try {
                    int stok = Integer.parseInt(v.toString());
                    lbl.setForeground(stok < 10 ? AppTheme.ACCENT_RED :
                                      stok < 30 ? AppTheme.ACCENT_YELLOW : AppTheme.ACCENT_GREEN);
                } catch (Exception ignored) {}
                return lbl;
            }
        });

        // Renderer kolom status
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                boolean aktif = "Aktif".equals(v);
                lbl.setForeground(aktif ? AppTheme.ACCENT_GREEN : AppTheme.TEXT_MUTED);
                return lbl;
            }
        });

        // Double click untuk edit
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && currentUser.isOwner()) {
                    int row = table.getSelectedRow();
                    if (row >= 0) dialogTambahEdit(semuaProduk.get(table.convertRowIndexToModel(row)));
                }
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(AppTheme.BG_CARD);
        sp.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_COLOR));
        sp.getViewport().setBackground(AppTheme.BG_TABLE_ROW);
        return sp;
    }

    private JPanel buildInfo() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setOpaque(false);
        JLabel info = new JLabel("💡 Double click baris untuk edit cepat | Merah = stok < 10 | Kuning = stok < 30");
        info.setFont(AppTheme.FONT_SMALL);
        info.setForeground(AppTheme.TEXT_MUTED);
        p.add(info);
        return p;
    }

    private void muatData() {
        SwingWorker<List<Produk>, Void> w = new SwingWorker<>() {
            @Override protected List<Produk> doInBackground() throws Exception {
                return produkDAO.getSemuaProdukAdmin();
            }
            @Override protected void done() {
                try {
                    semuaProduk = get();
                    refreshTabel(semuaProduk);
                } catch (Exception e) { e.printStackTrace(); }
            }
        };
        w.execute();
    }

    private void refreshTabel(List<Produk> list) {
        tableModel.setRowCount(0);
        for (Produk p : list) {
            tableModel.addRow(new Object[]{
                p.getKode(), p.getNama(), p.getKategoriNama(),
                "Rp " + CURRENCY.format(p.getHarga()),
                p.getStok(), p.getSatuan(),
                p.isAktif() ? "Aktif" : "Nonaktif"
            });
        }
    }

    private void filterTabel() {
        String keyword = txtCari.getText().toLowerCase();
        String kategori = cmbFilter.getSelectedItem().toString();
        List<Produk> filtered = semuaProduk.stream()
            .filter(p -> p.getNama().toLowerCase().contains(keyword) || p.getKode().toLowerCase().contains(keyword))
            .filter(p -> "Semua".equals(kategori) ||
                        (p.getKategoriNama() != null && p.getKategoriNama().equalsIgnoreCase(kategori)))
            .toList();
        refreshTabel(filtered);
    }

    private void dialogTambahEdit(Produk produk) {
        boolean editMode = (produk != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            editMode ? "Edit Produk" : "Tambah Produk Baru", true);
        dialog.setSize(420, 420);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(AppTheme.BG_DARK);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 10));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));

        JTextField txtKode  = AppTheme.makeTextField(10);
        JTextField txtNama  = AppTheme.makeTextField(10);
        JTextField txtHarga = AppTheme.makeTextField(10);
        JTextField txtStok  = AppTheme.makeTextField(10);
        JTextField txtSat   = AppTheme.makeTextField(10);
        JTextArea  txtDesk  = new JTextArea(2, 10);
        txtDesk.setBackground(AppTheme.BG_INPUT);
        txtDesk.setForeground(AppTheme.TEXT_PRIMARY);
        txtDesk.setFont(AppTheme.FONT_BODY);

        List<KategoriProduk> kategoriList = new ArrayList<>();
        JComboBox<KategoriProduk> cmbKat = new JComboBox<>();
        cmbKat.setBackground(AppTheme.BG_INPUT);
        cmbKat.setForeground(AppTheme.TEXT_PRIMARY);
        try {
            kategoriList = kategoriDAO.getSemuaKategori();
            for (KategoriProduk k : kategoriList) cmbKat.addItem(k);
        } catch (Exception e) { e.printStackTrace(); }

        if (editMode) {
            txtKode.setText(produk.getKode());
            txtNama.setText(produk.getNama());
            txtHarga.setText(produk.getHarga().toString());
            txtStok.setText(String.valueOf(produk.getStok()));
            txtSat.setText(produk.getSatuan());
            txtDesk.setText(produk.getDeskripsi());
            for (int i = 0; i < cmbKat.getItemCount(); i++) {
                if (cmbKat.getItemAt(i).getId() == produk.getKategoriId()) {
                    cmbKat.setSelectedIndex(i); break;
                }
            }
        }

        addRow(form, "Kode Produk", txtKode);
        addRow(form, "Nama Produk", txtNama);
        addRow(form, "Kategori",    cmbKat);
        addRow(form, "Harga (Rp)",  txtHarga);
        addRow(form, "Stok",        txtStok);
        addRow(form, "Satuan",      txtSat);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 16, 0));
        JButton btnSimpan = AppTheme.makeButton("💾 Simpan", AppTheme.ACCENT_ORANGE);
        JButton btnBatal  = AppTheme.makeOutlineButton("Batal", AppTheme.TEXT_MUTED);
        btnRow.add(btnSimpan);
        btnRow.add(btnBatal);

        btnSimpan.addActionListener(e -> {
            try {
                Produk p = editMode ? produk : new Produk();
                p.setKode(txtKode.getText().trim());
                p.setNama(txtNama.getText().trim());
                p.setKategoriId(((KategoriProduk)cmbKat.getSelectedItem()).getId());
                p.setHarga(new BigDecimal(txtHarga.getText().replaceAll("[^0-9.]","")));
                p.setStok(Integer.parseInt(txtStok.getText().trim()));
                p.setSatuan(txtSat.getText().trim());
                p.setDeskripsi(txtDesk.getText().trim());
                p.setAktif(true);
                if (editMode) produkDAO.updateProduk(p);
                else          produkDAO.tambahProduk(p);
                dialog.dispose();
                muatData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });
        btnBatal.addActionListener(e -> dialog.dispose());

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void hapusProduk() {
        int row = table.getSelectedRow();
        if (row < 0) { showWarning("Pilih produk terlebih dahulu!"); return; }
        Produk p = semuaProduk.get(table.convertRowIndexToModel(row));
        int opt = JOptionPane.showConfirmDialog(this,
            "Nonaktifkan produk \"" + p.getNama() + "\"?\n(Produk tidak akan dihapus permanen)",
            "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            try { produkDAO.hapusProduk(p.getId()); muatData(); }
            catch (Exception e) { showError("Gagal: " + e.getMessage()); }
        }
    }

    private void addRow(JPanel p, String label, Component comp) {
        JLabel lbl = AppTheme.makeLabel(label);
        p.add(lbl); p.add(comp);
    }
    private void showWarning(String m) { JOptionPane.showMessageDialog(this,m,"Perhatian",JOptionPane.WARNING_MESSAGE); }
    private void showError(String m)   { JOptionPane.showMessageDialog(this,m,"Error",JOptionPane.ERROR_MESSAGE); }
}

// ============================================================
//  DaftarTransaksiPanel - Riwayat semua transaksi
//  Fitur: Filter tanggal, Cari, Lihat detail, Update status,
//         Batalkan transaksi, Export CSV
// ============================================================
class DaftarTransaksiPanel extends JPanel {
    private User currentUser;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtDari, txtSampai, txtCari;
    private JLabel lblSummary;

    private final TransaksiDAO transaksiDAO = new TransaksiDAO();
    private final NumberFormat CURRENCY = NumberFormat.getInstance(new Locale("id","ID"));
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DaftarTransaksiPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        initUI();
        muatData();
    }

    private void initUI() {
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTable(),   BorderLayout.CENTER);
        add(buildSummary(), BorderLayout.SOUTH);
    }

    private JPanel buildToolbar() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JLabel title = new JLabel("📋 Daftar Transaksi");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterRow.setOpaque(false);

        String today = LocalDateTime.now().format(FMT_DATE);
        txtDari   = AppTheme.makeTextField(10); txtDari.setText(today);
        txtSampai = AppTheme.makeTextField(10); txtSampai.setText(today);
        txtCari   = AppTheme.makeTextField(12);

        JButton btnCari    = AppTheme.makeButton("🔍 Cari", AppTheme.ACCENT_ORANGE);
        JButton btnDetail  = AppTheme.makeOutlineButton("📄 Detail", AppTheme.ACCENT_BLUE);
        JButton btnBayar   = AppTheme.makeOutlineButton("💳 Bayar",  AppTheme.ACCENT_GREEN);
        JButton btnBatal   = AppTheme.makeOutlineButton("❌ Batal",  AppTheme.ACCENT_RED);

        filterRow.add(AppTheme.makeLabel("Dari:"));    filterRow.add(txtDari);
        filterRow.add(AppTheme.makeLabel("Sampai:"));  filterRow.add(txtSampai);
        filterRow.add(AppTheme.makeLabel("Cari:"));    filterRow.add(txtCari);
        filterRow.add(btnCari);
        filterRow.add(new JSeparator(SwingConstants.VERTICAL) {{ setPreferredSize(new Dimension(1,24)); setForeground(AppTheme.BORDER_COLOR); }});
        filterRow.add(btnDetail);
        if (currentUser.isOwner()) {
            filterRow.add(btnBayar);
            filterRow.add(btnBatal);
        }

        btnCari.addActionListener(e -> muatData());
        btnDetail.addActionListener(e -> lihatDetail());
        btnBayar.addActionListener(e -> prosesBayar());
        btnBatal.addActionListener(e -> batalTransaksi());

        panel.add(title, BorderLayout.NORTH);
        panel.add(filterRow, BorderLayout.SOUTH);
        return panel;
    }

    private JScrollPane buildTable() {
        String[] cols = {"ID","No.Trx","Antrian","Tanggal","Customer","Kasir","Total","Metode","Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setBackground(AppTheme.BG_TABLE_ROW);
        table.setForeground(AppTheme.TEXT_PRIMARY);
        table.setFont(AppTheme.FONT_BODY);
        table.setRowHeight(30);
        table.setGridColor(AppTheme.SEPARATOR);
        table.setSelectionBackground(new Color(255, 120, 40, 50));
        table.setShowVerticalLines(false);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setMaxWidth(0); // sembunyikan ID
        table.getTableHeader().setBackground(AppTheme.BG_SIDEBAR);
        table.getTableHeader().setForeground(AppTheme.ACCENT_ORANGE);
        table.getTableHeader().setFont(AppTheme.FONT_SUBTITLE);

        // Warna status
        table.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                lbl.setForeground(AppTheme.statusColor(v != null ? v.toString() : ""));
                return lbl;
            }
        });
        // Warna metode bayar
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                lbl.setForeground(AppTheme.metodeBayarColor(v != null ? v.toString() : ""));
                return lbl;
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) lihatDetail();
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBackground(AppTheme.BG_CARD);
        sp.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_COLOR));
        sp.getViewport().setBackground(AppTheme.BG_TABLE_ROW);
        return sp;
    }

    private JPanel buildSummary() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setOpaque(false);
        lblSummary = new JLabel("Total: Rp 0 | 0 transaksi");
        lblSummary.setFont(AppTheme.FONT_BODY);
        lblSummary.setForeground(AppTheme.TEXT_SECONDARY);
        p.add(lblSummary);
        return p;
    }

    private void muatData() {
        SwingWorker<List<Transaksi>, Void> w = new SwingWorker<>() {
            @Override protected List<Transaksi> doInBackground() throws Exception {
                return transaksiDAO.getTransaksiBerdasarkanTanggal(
                    txtDari.getText().trim(), txtSampai.getText().trim());
            }
            @Override protected void done() {
                try {
                    List<Transaksi> list = get();
                    tableModel.setRowCount(0);
                    BigDecimal totalOmzet = BigDecimal.ZERO;
                    String keyword = txtCari.getText().toLowerCase();
                    for (Transaksi t : list) {
                        if (!keyword.isEmpty() &&
                            !t.getNoTransaksi().toLowerCase().contains(keyword) &&
                            !t.getNamaCustomer().toLowerCase().contains(keyword)) continue;
                        tableModel.addRow(new Object[]{
                            t.getId(), t.getNoTransaksi(), t.getNoAntrian(),
                            t.getTanggalFormatted(),
                            t.getNamaCustomer(), t.getKasirNama(),
                            "Rp " + CURRENCY.format(t.getTotalBayar()),
                            t.getMetodeBayar(), t.getStatus()
                        });
                        if ("Lunas".equals(t.getStatus())) totalOmzet = totalOmzet.add(t.getTotalBayar());
                    }
                    lblSummary.setText("Omzet: Rp " + CURRENCY.format(totalOmzet) +
                        " | " + tableModel.getRowCount() + " transaksi");
                } catch (Exception e) { e.printStackTrace(); }
            }
        };
        w.execute();
    }

    private void lihatDetail() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this,"Pilih transaksi!"); return; }
        int id = (Integer) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        new DetailTransaksiDialog((Frame)SwingUtilities.getWindowAncestor(this),
            id, transaksiDAO).setVisible(true);
    }

    private void prosesBayar() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (Integer) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        String dibayarStr = JOptionPane.showInputDialog(this, "Masukkan jumlah dibayar:", "Proses Bayar", JOptionPane.PLAIN_MESSAGE);
        if (dibayarStr == null) return;
        try {
            BigDecimal dibayar = new BigDecimal(dibayarStr.replaceAll("[^0-9]",""));
            transaksiDAO.updateStatusTransaksi(id, "Lunas", dibayar, "Tunai");
            muatData();
            JOptionPane.showMessageDialog(this,"✅ Transaksi berhasil dilunasi!");
        } catch (Exception e) { JOptionPane.showMessageDialog(this,"Error: "+e.getMessage()); }
    }

    private void batalTransaksi() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (Integer) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
        int opt = JOptionPane.showConfirmDialog(this,
            "Yakin membatalkan transaksi ini?\nStok produk akan dikembalikan.",
            "Konfirmasi Batal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            try { transaksiDAO.batalTransaksi(id); muatData(); }
            catch (Exception e) { JOptionPane.showMessageDialog(this,"Error: "+e.getMessage()); }
        }
    }
}

// ============================================================
//  DetailTransaksiDialog - Dialog popup detail 1 transaksi
// ============================================================
class DetailTransaksiDialog extends JDialog {
    private final int transaksiId;
    private final TransaksiDAO dao;
    private final NumberFormat CURRENCY = NumberFormat.getInstance(new Locale("id","ID"));

    public DetailTransaksiDialog(Frame parent, int transaksiId, TransaksiDAO dao) {
        super(parent, "Detail Transaksi", true);
        this.transaksiId = transaksiId;
        this.dao = dao;
        setSize(480, 420);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(AppTheme.BG_DARK);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 8));

        JLabel title = new JLabel("  📄 Detail Transaksi #" + transaksiId);
        title.setFont(AppTheme.FONT_SUBTITLE);
        title.setForeground(AppTheme.ACCENT_ORANGE);
        title.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 0));
        add(title, BorderLayout.NORTH);

        String[] cols = {"Nama Produk","Qty","Harga","Subtotal"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try {
            List<DetailTransaksi> details = dao.getDetailTransaksi(transaksiId);
            for (DetailTransaksi dt : details) {
                model.addRow(new Object[]{
                    dt.getNamaProduk(), dt.getQty(),
                    "Rp " + CURRENCY.format(dt.getHargaSatuan()),
                    "Rp " + CURRENCY.format(dt.getSubtotal())
                });
            }
        } catch (Exception e) { e.printStackTrace(); }

        JTable table = new JTable(model);
        table.setBackground(AppTheme.BG_TABLE_ROW);
        table.setForeground(AppTheme.TEXT_PRIMARY);
        table.setFont(AppTheme.FONT_BODY);
        table.setRowHeight(30);
        table.setGridColor(AppTheme.SEPARATOR);
        table.getTableHeader().setBackground(AppTheme.BG_SIDEBAR);
        table.getTableHeader().setForeground(AppTheme.ACCENT_ORANGE);
        table.getTableHeader().setFont(AppTheme.FONT_SUBTITLE);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_COLOR));
        sp.getViewport().setBackground(AppTheme.BG_TABLE_ROW);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        add(sp, BorderLayout.CENTER);

        JButton btnTutup = AppTheme.makeButton("Tutup", AppTheme.TEXT_MUTED);
        btnTutup.addActionListener(e -> dispose());
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 12, 0));
        btnPanel.add(btnTutup);
        add(btnPanel, BorderLayout.SOUTH);
    }
}

// ============================================================
//  StrukDialog - Tampilan dan cetak struk thermal
//  Fitur: Preview struk, tombol cetak ke printer
// ============================================================
class StrukDialog extends JDialog {
    private Transaksi transaksi;
    private List<ItemKeranjang> keranjang;
    private User kasir;
    private PengaturanDAO dao;
    private final NumberFormat CURRENCY = NumberFormat.getInstance(new Locale("id","ID"));
    private JTextArea txtStruk;

    public StrukDialog(Frame parent, Transaksi trx, List<ItemKeranjang> keranjang,
                       User kasir, PengaturanDAO dao) {
        super(parent, "🖨️ Preview Struk", true);
        this.transaksi = trx;
        this.keranjang = keranjang;
        this.kasir = kasir;
        this.dao   = dao;
        setSize(400, 620);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(AppTheme.BG_DARK);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 8));

        txtStruk = new JTextArea();
        txtStruk.setFont(new Font("Courier New", Font.PLAIN, 12));
        txtStruk.setBackground(Color.WHITE);
        txtStruk.setForeground(Color.BLACK);
        txtStruk.setEditable(false);
        txtStruk.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        txtStruk.setText(buildStrukText());

        JScrollPane sp = new JScrollPane(txtStruk);
        sp.setBorder(BorderFactory.createEmptyBorder(12, 16, 0, 16));
        add(sp, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnRow.setOpaque(false);
        JButton btnCetak = AppTheme.makeButton("🖨️ Cetak", AppTheme.ACCENT_ORANGE);
        JButton btnTutup = AppTheme.makeOutlineButton("Tutup", AppTheme.TEXT_MUTED);
        btnCetak.addActionListener(e -> cetakKePrinter());
        btnTutup.addActionListener(e -> dispose());
        btnRow.add(btnCetak);
        btnRow.add(btnTutup);
        add(btnRow, BorderLayout.SOUTH);
    }

    private String buildStrukText() {
        StringBuilder sb = new StringBuilder();
        String sep = "--------------------------------\n";
        String sep2 = "================================\n";

        try {
            String namaToko = dao.getNilai("nama_toko");
            String alamat   = dao.getNilai("alamat_toko");
            String ig       = dao.getNilai("instagram");
            String pesan    = dao.getNilai("pesan_struk");

            sb.append(center(namaToko, 32)).append("\n");
            sb.append(center(alamat, 32)).append("\n");
            sb.append(center("IG: " + ig, 32)).append("\n");
            sb.append(sep2);
        } catch (Exception e) {
            sb.append(center("LARISOLE MALANG", 32)).append("\n");
            sb.append(sep2);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");
        String tanggal = transaksi != null && transaksi.getTanggal() != null
            ? transaksi.getTanggal().format(fmt)
            : LocalDateTime.now().format(fmt);

        sb.append(padRight("Id Transaksi", 14)).append(padLeft(transaksi != null ? transaksi.getNoTransaksi() : "#-", 18)).append("\n");
        sb.append(padRight("Antrian",      14)).append(padLeft(transaksi != null ? String.valueOf(transaksi.getNoAntrian()) : "-", 18)).append("\n");
        sb.append(padRight("Tanggal",      14)).append(padLeft(tanggal, 18)).append("\n");
        sb.append(padRight("Kasir",        14)).append(padLeft(kasir.getNamaLengkap(), 18)).append("\n");
        sb.append(sep);

        // Items
        if (transaksi != null && transaksi.getDetails() != null && !transaksi.getDetails().isEmpty()) {
            for (DetailTransaksi dt : transaksi.getDetails()) {
                sb.append(dt.getNamaProduk()).append("\n");
                sb.append("  ").append(dt.getQty()).append(" x ")
                  .append(CURRENCY.format(dt.getHargaSatuan()))
                  .append(padLeft("Rp " + CURRENCY.format(dt.getSubtotal()), 28 - String.valueOf(dt.getQty()).length() - 5))
                  .append("\n");
            }
        } else if (keranjang != null) {
            for (ItemKeranjang item : keranjang) {
                sb.append(item.getNamaProduk()).append("\n");
                sb.append("  ").append(item.getQty()).append(" x ")
                  .append(CURRENCY.format(item.getHargaSatuan()))
                  .append(padLeft("Rp " + CURRENCY.format(item.getSubtotal()), 26))
                  .append("\n");
            }
        }

        sb.append(sep);

        BigDecimal total   = transaksi != null ? transaksi.getTotalBayar() : BigDecimal.ZERO;
        BigDecimal diskon  = transaksi != null ? transaksi.getDiskon() : BigDecimal.ZERO;
        BigDecimal dibayar = transaksi != null ? transaksi.getDibayar() : BigDecimal.ZERO;
        BigDecimal kembali = transaksi != null ? transaksi.getKembalian() : BigDecimal.ZERO;
        String metode      = transaksi != null ? transaksi.getMetodeBayar() : "Tunai";

        sb.append(twoCol("Status",       transaksi != null ? transaksi.getStatus() : "Lunas")).append("\n");
        sb.append(twoCol("Metode Bayar", metode)).append("\n");
        sb.append(twoCol("SubTotal",     CURRENCY.format(total.add(diskon)))).append("\n");
        sb.append(twoCol("Diskon",       CURRENCY.format(diskon))).append("\n");
        sb.append(twoCol("Total",        CURRENCY.format(total))).append("\n");
        sb.append(twoCol("DiBayar",      CURRENCY.format(dibayar))).append("\n");
        sb.append(twoCol("Kembalian",    CURRENCY.format(kembali))).append("\n");
        sb.append(sep);

        if (transaksi != null && transaksi.getNamaCustomer() != null)
            sb.append("Customer : ").append(transaksi.getNamaCustomer()).append("\n");
        sb.append("\n");

        try {
            sb.append(center(dao.getNilai("pesan_struk"), 32)).append("\n");
        } catch (Exception ignored) {
            sb.append(center("Terima kasih sudah berbelanja!", 32)).append("\n");
        }

        return sb.toString();
    }

    private void cetakKePrinter() {
        try {
            txtStruk.print();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mencetak: " + e.getMessage());
        }
    }

    private String center(String text, int width) {
        if (text == null || text.length() >= width) return text;
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }
    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
    private String padLeft(String s, int n) {
        return String.format("%" + n + "s", s);
    }
    private String twoCol(String k, String v) {
        return padRight(k, 16) + padLeft(v, 16);
    }
}

// ============================================================
//  LaporanPanel - Laporan penjualan dan statistik
//  Fitur: Ringkasan harian, produk terlaris, grafik omzet
// ============================================================
class LaporanPanel extends JPanel {
    private final TransaksiDAO transaksiDAO = new TransaksiDAO();
    private final NumberFormat CURRENCY = NumberFormat.getInstance(new Locale("id","ID"));

    public LaporanPanel() {
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        initUI();
    }

    private void initUI() {
        JLabel title = new JLabel("📊 Laporan & Analisis");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(AppTheme.BG_CARD);
        tabs.setForeground(AppTheme.TEXT_PRIMARY);
        tabs.setFont(AppTheme.FONT_BODY);

        tabs.addTab("📅 Ringkasan Hari Ini", buildRingkasanPanel());
        tabs.addTab("📦 Produk Terlaris",    buildProdukLarisPanel());
        tabs.addTab("💰 Metode Pembayaran",  buildMetodeBayarPanel());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildRingkasanPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3, 12, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));

        try {
            BigDecimal omzet = transaksiDAO.getOmzetHariIni();
            int jmlTrx = transaksiDAO.getJumlahTransaksiHariIni();
            BigDecimal avgTrx = jmlTrx > 0 ? omzet.divide(new BigDecimal(jmlTrx), 0, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

            panel.add(buildStatCard("💰 Omzet Hari Ini",   "Rp " + CURRENCY.format(omzet),  AppTheme.ACCENT_ORANGE));
            panel.add(buildStatCard("🛒 Total Transaksi",  String.valueOf(jmlTrx),            AppTheme.ACCENT_BLUE));
            panel.add(buildStatCard("📈 Rata-rata/Trx",    "Rp " + CURRENCY.format(avgTrx),  AppTheme.ACCENT_GREEN));
            panel.add(buildStatCard("🗓️ Periode",          "Hari Ini",                        AppTheme.ACCENT_YELLOW));
            panel.add(buildStatCard("👤 Kasir Aktif",      "1",                               AppTheme.TEXT_SECONDARY));
            panel.add(buildStatCard("📦 Produk Terjual",   "—",                               AppTheme.TEXT_SECONDARY));
        } catch (Exception e) {
            panel.add(new JLabel("Error: " + e.getMessage()));
        }

        return panel;
    }

    private JPanel buildStatCard(String label, String value, Color color) {
        JPanel card = AppTheme.makeCard(12);
        card.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(16, 16, 4, 16);

        JLabel lblVal = new JLabel(value, SwingConstants.CENTER);
        lblVal.setFont(AppTheme.FONT_HUGE);
        lblVal.setForeground(color);

        JLabel lblLbl = new JLabel(label, SwingConstants.CENTER);
        lblLbl.setFont(AppTheme.FONT_BODY);
        lblLbl.setForeground(AppTheme.TEXT_SECONDARY);

        card.add(lblVal, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 16, 16, 16);
        card.add(lblLbl, gbc);
        return card;
    }

    private JPanel buildProdukLarisPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel msg = new JLabel("📦 Fitur laporan produk terlaris tersedia dari view v_laporan_produk_terlaris", SwingConstants.CENTER);
        msg.setForeground(AppTheme.TEXT_SECONDARY);
        msg.setFont(AppTheme.FONT_BODY);
        panel.add(msg);
        return panel;
    }

    private JPanel buildMetodeBayarPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel msg = new JLabel("💳 Statistik metode pembayaran — Query dari tabel transaksi", SwingConstants.CENTER);
        msg.setForeground(AppTheme.TEXT_SECONDARY);
        msg.setFont(AppTheme.FONT_BODY);
        panel.add(msg);
        return panel;
    }
}

// ============================================================
//  PelangganPanel - Stub panel pelanggan tetap
// ============================================================
class PelangganPanel extends JPanel {
    public PelangganPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        JLabel msg = new JLabel("👥 Manajemen Pelanggan — Coming Soon", SwingConstants.CENTER);
        msg.setFont(AppTheme.FONT_TITLE);
        msg.setForeground(AppTheme.TEXT_SECONDARY);
        add(msg);
    }
}

// ============================================================
//  PengaturanPanel - Konfigurasi sistem dan manajemen user
//  Fitur: Edit info toko, ganti password, manajemen akun
// ============================================================
class PengaturanPanel extends JPanel {
    private User currentUser;
    private final PengaturanDAO dao = new PengaturanDAO();
    private final UserDAO userDAO   = new UserDAO();

    private JTextField txtNamaToko, txtAlamat, txtIG, txtPesanStruk;

    public PengaturanPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        initUI();
    }

    private void initUI() {
        JLabel title = new JLabel("⚙️ Pengaturan Sistem");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(AppTheme.BG_CARD);
        tabs.setForeground(AppTheme.TEXT_PRIMARY);
        tabs.setFont(AppTheme.FONT_BODY);

        tabs.addTab("🏪 Info Toko",   buildInfoTokoPanel());
        tabs.addTab("🔑 Ganti Kata Sandi", buildGantiPasswordPanel());
        if (currentUser.isOwner()) tabs.addTab("👥 Kelola Akun", buildKelolaAkunPanel());
        tabs.addTab("🗄️ Database",    buildDbInfoPanel());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildInfoTokoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 16, 8, 16);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        txtNamaToko   = AppTheme.makeTextField(20);
        txtAlamat     = AppTheme.makeTextField(20);
        txtIG         = AppTheme.makeTextField(20);
        txtPesanStruk = AppTheme.makeTextField(20);

        try {
            txtNamaToko.setText(dao.getNilai("nama_toko"));
            txtAlamat.setText(dao.getNilai("alamat_toko"));
            txtIG.setText(dao.getNilai("instagram"));
            txtPesanStruk.setText(dao.getNilai("pesan_struk"));
        } catch (Exception e) { e.printStackTrace(); }

        addRow(panel, gbc, "Nama Toko",     txtNamaToko);
        addRow(panel, gbc, "Alamat Toko",   txtAlamat);
        addRow(panel, gbc, "Instagram",     txtIG);
        addRow(panel, gbc, "Pesan di Struk",txtPesanStruk);

        gbc.gridy++; gbc.gridx = 1;
        JButton btnSimpan = AppTheme.makeButton("💾 Simpan Pengaturan", AppTheme.ACCENT_ORANGE);
        btnSimpan.addActionListener(e -> {
            try {
                dao.setNilai("nama_toko",   txtNamaToko.getText());
                dao.setNilai("alamat_toko", txtAlamat.getText());
                dao.setNilai("instagram",   txtIG.getText());
                dao.setNilai("pesan_struk", txtPesanStruk.getText());
                JOptionPane.showMessageDialog(this, "✅ Pengaturan disimpan!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });
        panel.add(btnSimpan, gbc);

        return panel;
    }

    private JPanel buildGantiPasswordPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 16, 8, 16);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        JPasswordField txtLama  = new JPasswordField(16);
        JPasswordField txtBaru  = new JPasswordField(16);
        JPasswordField txtUlang = new JPasswordField(16);
        stylePass(txtLama); stylePass(txtBaru); stylePass(txtUlang);

        addRow(panel, gbc, "Password Lama",   txtLama);
        addRow(panel, gbc, "Password Baru",   txtBaru);
        addRow(panel, gbc, "Ulangi Password", txtUlang);

        gbc.gridy++; gbc.gridx = 1;
        JButton btn = AppTheme.makeButton("🔑 Ganti Password", AppTheme.ACCENT_ORANGE);
        btn.addActionListener(e -> {
            String baru  = new String(txtBaru.getPassword());
            String ulang = new String(txtUlang.getPassword());
            if (!baru.equals(ulang)) {
                JOptionPane.showMessageDialog(this,"Password tidak cocok!"); return;
            }
            if (baru.length() < 6) {
                JOptionPane.showMessageDialog(this,"Password minimal 6 karakter!"); return;
            }
            try {
                userDAO.gantiPassword(currentUser.getId(), baru);
                JOptionPane.showMessageDialog(this,"✅ Password berhasil diubah!");
                txtLama.setText(""); txtBaru.setText(""); txtUlang.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,"Error: "+ex.getMessage());
            }
        });
        panel.add(btn, gbc);
        return panel;
    }

    private JPanel buildKelolaAkunPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JLabel info = new JLabel("Daftar akun kasir yang terdaftar di sistem:");
        info.setForeground(AppTheme.TEXT_SECONDARY);
        info.setFont(AppTheme.FONT_BODY);

        String[] cols = {"ID","Username","Nama Lengkap","Role","Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        try {
            for (User u : userDAO.getSemuaUser()) {
                model.addRow(new Object[]{u.getId(),u.getUsername(),u.getNamaLengkap(),u.getRole(),u.isAktif()?"Aktif":"Nonaktif"});
            }
        } catch (Exception e) { e.printStackTrace(); }

        JTable table = new JTable(model);
        table.setBackground(AppTheme.BG_TABLE_ROW);
        table.setForeground(AppTheme.TEXT_PRIMARY);
        table.setFont(AppTheme.FONT_BODY);
        table.setRowHeight(30);
        table.getTableHeader().setBackground(AppTheme.BG_SIDEBAR);
        table.getTableHeader().setForeground(AppTheme.ACCENT_ORANGE);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_COLOR));
        sp.getViewport().setBackground(AppTheme.BG_TABLE_ROW);

        panel.add(info, BorderLayout.NORTH);
        panel.add(sp,   BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildDbInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        DatabaseConnection db = DatabaseConnection.getInstance();
        JLabel info = new JLabel(String.format(
            "<html><pre style='font-family:Consolas'>Host     : %s<br>Database : %s<br>Status   : ✅ Terhubung</pre></html>",
            db.getHost(), db.getDatabase()));
        info.setForeground(AppTheme.ACCENT_GREEN);
        info.setFont(AppTheme.FONT_MONO);
        panel.add(info);
        return panel;
    }

    private void addRow(JPanel p, GridBagConstraints gbc, String label, Component comp) {
        gbc.gridx = 0;
        JLabel lbl = AppTheme.makeLabel(label + ":");
        lbl.setPreferredSize(new Dimension(150, 30));
        p.add(lbl, gbc);
        gbc.gridx = 1;
        p.add(comp, gbc);
        gbc.gridy++;
    }

    private void stylePass(JPasswordField f) {
        f.setBackground(AppTheme.BG_INPUT);
        f.setForeground(AppTheme.TEXT_PRIMARY);
        f.setCaretColor(AppTheme.ACCENT_ORANGE);
        f.setFont(AppTheme.FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER_COLOR),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }
}