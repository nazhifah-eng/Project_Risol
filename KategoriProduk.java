public class KategoriProduk {
    private int id;
    private String nama;
    private boolean aktif;

    public KategoriProduk(int id, String nama) {
        this.id = id;
        this.nama = nama;
        this.aktif = true;
    }

    public int getId()              { return id; }
    public String getNama()         { return nama; }
    public boolean isAktif()        { return aktif; }
    public void setAktif(boolean a) { this.aktif = a; }

    @Override
    public String toString() { return nama; }
}