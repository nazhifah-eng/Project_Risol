import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// ============================================================
//  Produk — Model satu produk yang dijual di toko
//  Dipakai oleh: ProdukDAO, ProdukPanel, TransaksiPanel (combobox)
// ============================================================
class Produk {
    private int       id;
    private String    kode;
    private String    nama;
    private int       kategoriId;
    private String    kategoriNama;   // hasil JOIN, bukan kolom sendiri
    private BigDecimal harga;
    private int       stok;
    private String    satuan;
    private String    deskripsi;
    private boolean   aktif;

    /** Konstruktor kosong — dipakai saat membuat produk baru */
    public Produk() {}

    /** Konstruktor lengkap — dipakai oleh ProdukDAO.mapProduk() */
    public Produk(int id, String kode, String nama, int kategoriId,
                  String kategoriNama, BigDecimal harga, int stok,
                  String satuan, boolean aktif) {
        this.id = id; this.kode = kode; this.nama = nama;
        this.kategoriId = kategoriId; this.kategoriNama = kategoriNama;
        this.harga = harga; this.stok = stok;
        this.satuan = satuan; this.aktif = aktif;
    }

    public int        getId()                   { return id; }
    public void       setId(int id)             { this.id = id; }
    public String     getKode()                 { return kode; }
    public void       setKode(String kode)      { this.kode = kode; }
    public String     getNama()                 { return nama; }
    public void       setNama(String nama)      { this.nama = nama; }
    public int        getKategoriId()           { return kategoriId; }
    public void       setKategoriId(int id)     { this.kategoriId = id; }
    public String     getKategoriNama()         { return kategoriNama; }
    public void       setKategoriNama(String n) { this.kategoriNama = n; }
    public BigDecimal getHarga()                { return harga; }
    public void       setHarga(BigDecimal h)    { this.harga = h; }
    public int        getStok()                 { return stok; }
    public void       setStok(int s)            { this.stok = s; }
    public String     getSatuan()               { return satuan; }
    public void       setSatuan(String s)       { this.satuan = s; }
    public String     getDeskripsi()            { return deskripsi; }
    public void       setDeskripsi(String d)    { this.deskripsi = d; }
    public boolean    isAktif()                 { return aktif; }
    public void       setAktif(boolean a)       { this.aktif = a; }

    /** toString() dipakai agar nama produk tampil di JComboBox */
    @Override
    public String toString() { return nama; }
}

// ============================================================
//  ItemKeranjang — Satu baris item di keranjang belanja
//  Menyimpan referensi Produk + qty yang dipilih kasir.
//  Subtotal dihitung otomatis setiap qty/diskon berubah.
// ============================================================
class ItemKeranjang {
    private Produk    produk;
    private int       qty;
    private BigDecimal diskonItem;  // diskon per item (opsional)
    private BigDecimal subtotal;    // hasil hitungSubtotal()

    public ItemKeranjang(Produk produk, int qty) {
        this.produk    = produk;
        this.qty       = qty;
        this.diskonItem = BigDecimal.ZERO;
        hitungSubtotal();
    }

    /**
     * Menghitung subtotal = (harga × qty) − diskonItem.
     * Dipanggil otomatis setiap qty atau diskonItem berubah.
     */
    public void hitungSubtotal() {
        subtotal = produk.getHarga()
                         .multiply(new BigDecimal(qty))
                         .subtract(diskonItem);
    }

    public Produk     getProduk()              { return produk; }
    public int        getQty()                 { return qty; }
    public void       setQty(int qty)          { this.qty = qty; hitungSubtotal(); }
    public BigDecimal getDiskonItem()          { return diskonItem; }
    public void       setDiskonItem(BigDecimal d){ this.diskonItem = d; hitungSubtotal(); }
    public BigDecimal getSubtotal()            { return subtotal; }
    public BigDecimal getHargaSatuan()         { return produk.getHarga(); }
    public String     getNamaProduk()          { return produk.getNama(); }
    public int        getProdukId()            { return produk.getId(); }
}

// ============================================================
//  DetailTransaksi — Snapshot satu item dalam transaksi
//  yang sudah tersimpan di database.
//  Nama produk & harga disimpan sebagai snapshot agar struk
//  tidak berubah walau harga produk di-update kemudian.
// ============================================================
class DetailTransaksi {
    private int        id;
    private int        transaksiId;
    private int        produkId;
    private String     namaProduk;    // snapshot nama saat transaksi
    private BigDecimal hargaSatuan;   // snapshot harga saat transaksi
    private int        qty;
    private BigDecimal diskonItem;
    private BigDecimal subtotal;

    public int        getId()                       { return id; }
    public void       setId(int id)                 { this.id = id; }
    public int        getTransaksiId()              { return transaksiId; }
    public void       setTransaksiId(int i)         { this.transaksiId = i; }
    public int        getProdukId()                 { return produkId; }
    public void       setProdukId(int i)            { this.produkId = i; }
    public String     getNamaProduk()               { return namaProduk; }
    public void       setNamaProduk(String n)       { this.namaProduk = n; }
    public BigDecimal getHargaSatuan()              { return hargaSatuan; }
    public void       setHargaSatuan(BigDecimal h)  { this.hargaSatuan = h; }
    public int        getQty()                      { return qty; }
    public void       setQty(int q)                 { this.qty = q; }
    public BigDecimal getDiskonItem()               { return diskonItem; }
    public void       setDiskonItem(BigDecimal d)   { this.diskonItem = d; }
    public BigDecimal getSubtotal()                 { return subtotal; }
    public void       setSubtotal(BigDecimal s)     { this.subtotal = s; }
}

// ============================================================
//  Transaksi — Header penjualan beserta list detailnya.
//  Satu objek Transaksi = satu baris di tabel transaksi DB.
// ============================================================
class Transaksi {
    private int        id;
    private String     noTransaksi;
    private int        noAntrian;
    private LocalDateTime tanggal;
    private int        kasirId;
    private String     kasirNama;
    private String     namaCustomer;
    private BigDecimal subtotal;
    private BigDecimal diskon;
    private BigDecimal totalBayar;
    private BigDecimal dibayar;
    private BigDecimal kembalian;
    private String     metodeBayar;
    private String     status;
    private String     catatan;
    private List<DetailTransaksi> details = new ArrayList<>();

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Format tanggal untuk tampilan tabel (dd/MM/yyyy HH:mm) */
    public String getTanggalFormatted() {
        return tanggal != null ? tanggal.format(FMT) : "-";
    }

    public int        getId()                        { return id; }
    public void       setId(int id)                  { this.id = id; }
    public String     getNoTransaksi()               { return noTransaksi; }
    public void       setNoTransaksi(String n)       { this.noTransaksi = n; }
    public int        getNoAntrian()                 { return noAntrian; }
    public void       setNoAntrian(int n)            { this.noAntrian = n; }
    public LocalDateTime getTanggal()                { return tanggal; }
    public void       setTanggal(LocalDateTime t)    { this.tanggal = t; }
    public int        getKasirId()                   { return kasirId; }
    public void       setKasirId(int id)             { this.kasirId = id; }
    public String     getKasirNama()                 { return kasirNama; }
    public void       setKasirNama(String n)         { this.kasirNama = n; }
    public String     getNamaCustomer()              { return namaCustomer; }
    public void       setNamaCustomer(String n)      { this.namaCustomer = n; }
    public BigDecimal getSubtotal()                  { return subtotal; }
    public void       setSubtotal(BigDecimal s)      { this.subtotal = s; }
    public BigDecimal getDiskon()                    { return diskon; }
    public void       setDiskon(BigDecimal d)        { this.diskon = d; }
    public BigDecimal getTotalBayar()                { return totalBayar; }
    public void       setTotalBayar(BigDecimal t)    { this.totalBayar = t; }
    public BigDecimal getDibayar()                   { return dibayar; }
    public void       setDibayar(BigDecimal d)       { this.dibayar = d; }
    public BigDecimal getKembalian()                 { return kembalian; }
    public void       setKembalian(BigDecimal k)     { this.kembalian = k; }
    public String     getMetodeBayar()               { return metodeBayar; }
    public void       setMetodeBayar(String m)       { this.metodeBayar = m; }
    public String     getStatus()                    { return status; }
    public void       setStatus(String s)            { this.status = s; }
    public String     getCatatan()                   { return catatan; }
    public void       setCatatan(String c)           { this.catatan = c; }
    public List<DetailTransaksi> getDetails()        { return details; }
    public void       setDetails(List<DetailTransaksi> d){ this.details = d; }
}

// ============================================================
//  User — Data pengguna yang login ke sistem
//  role: "owner" punya akses penuh, "kasir" akses terbatas
// ============================================================
class User {
    private int    id;
    private String username;
    private String namaLengkap;
    private String role;
    private boolean aktif;

    public User() {}
    public User(int id, String username, String namaLengkap, String role) {
        this.id = id; this.username = username;
        this.namaLengkap = namaLengkap; this.role = role;
    }

    public int     getId()                      { return id; }
    public void    setId(int id)                { this.id = id; }
    public String  getUsername()                { return username; }
    public void    setUsername(String u)        { this.username = u; }
    public String  getNamaLengkap()             { return namaLengkap; }
    public void    setNamaLengkap(String n)     { this.namaLengkap = n; }
    public String  getRole()                    { return role; }
    public void    setRole(String r)            { this.role = r; }
    public boolean isAktif()                    { return aktif; }
    public void    setAktif(boolean a)          { this.aktif = a; }

    /** true jika role == "owner" — digunakan untuk tampilkan/sembunyikan tombol tertentu */
    public boolean isOwner()  { return "owner".equalsIgnoreCase(role); }

    @Override
    public String toString()  { return namaLengkap + " (" + role + ")"; }
}

// ============================================================
//  KategoriProduk — Data kategori produk
//  Dipakai di ComboBox filter & form tambah/edit produk
// ============================================================
class KategoriProduk {
    private int    id;
    private String nama;
    private boolean aktif;

    public KategoriProduk(int id, String nama) {
        this.id = id; this.nama = nama; this.aktif = true;
    }

    public int     getId()              { return id; }
    public String  getNama()            { return nama; }
    public boolean isAktif()            { return aktif; }
    public void    setAktif(boolean a)  { this.aktif = a; }

    /** toString() dipakai agar nama kategori tampil di JComboBox */
    @Override
    public String toString() { return nama; }
}