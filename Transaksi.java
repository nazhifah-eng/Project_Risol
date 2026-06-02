import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Transaksi {
    private int id;
    private String noTransaksi;
    private int noAntrian;
    private LocalDateTime tanggal;
    private int kasirId;
    private String kasirNama;
    private String namaCustomer;
    private BigDecimal subtotal;
    private BigDecimal diskon;
    private BigDecimal totalBayar;
    private BigDecimal dibayar;
    private BigDecimal kembalian;
    private String metodeBayar;
    private String status;
    private String catatan;
    private List<DetailTransaksi> details = new ArrayList<>();

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String getTanggalFormatted() {
        return tanggal != null ? tanggal.format(FMT) : "-";
    }

    public int getId()                        { 
        return id; 
    }
    public void setId(int id)                 { 
        this.id = id; 
    }
    public String getNoTransaksi()            { 
        return noTransaksi; 
    }
    public void setNoTransaksi(String n)      { 
        this.noTransaksi = n; 
    }
    public int getNoAntrian()                 { 
        return noAntrian; 
    }
    public void setNoAntrian(int n)           { 
        this.noAntrian = n; 
    }
    public LocalDateTime getTanggal()         { 
        return tanggal; 
    }
    public void setTanggal(LocalDateTime t)   { 
        this.tanggal = t; 
    }
    public int getKasirId()                   { 
        return kasirId; 
    }
    public void setKasirId(int id)            { 
        this.kasirId = id; 
    }
    public String getKasirNama()              { 
        return kasirNama; 
    }
    public void setKasirNama(String n)        { 
        this.kasirNama = n; 
    }
    public String getNamaCustomer()           { 
        return namaCustomer; 
    }
    public void setNamaCustomer(String n)     { 
        this.namaCustomer = n; 
    }
    public BigDecimal getSubtotal()           { 
        return subtotal; 
    }
    public void setSubtotal(BigDecimal s)     { 
        this.subtotal = s; 
    }
    public BigDecimal getDiskon()             { 
        return diskon; 
    }
    public void setDiskon(BigDecimal d)       { 
        this.diskon = d; 
    }
    public BigDecimal getTotalBayar()         { 
        return totalBayar; 
    }
    public void setTotalBayar(BigDecimal t)   { 
        this.totalBayar = t; 
    }
    public BigDecimal getDibayar()            { 
        return dibayar; 
    }
    public void setDibayar(BigDecimal d)      { 
        this.dibayar = d; 
    }
    public BigDecimal getKembalian()          { 
        return kembalian; 
    }
    public void setKembalian(BigDecimal k)    { 
        this.kembalian = k; 
    }
    public String getMetodeBayar()            { 
        return metodeBayar; 
    }
    public void setMetodeBayar(String m)      { 
        this.metodeBayar = m; 
    }
    public String getStatus()                 { 
        return status; 
    }
    public void setStatus(String s)           { 
        this.status = s; 
    }
    public String getCatatan()                { 
        return catatan; 
    }
    public void setCatatan(String c)          { 
        this.catatan = c; 
    }
    public List<DetailTransaksi> getDetails() { 
        return details; 
    }
    public void setDetails(List<DetailTransaksi> d){ 
        this.details = d; 
    }
}
