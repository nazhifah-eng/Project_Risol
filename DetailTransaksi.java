import java.math.BigDecimal;

public class DetailTransaksi {
    private int id;
    private int transaksiId;
    private int produkId;
    private String namaProduk;
    private BigDecimal hargaSatuan;
    private int qty;
    private BigDecimal diskonItem;
    private BigDecimal subtotal;

    public int getId()                      { 
        return id; 
    }
    public void setId(int id)               
    { this.id = id; 

    }
    public int getTransaksiId()             { 
        return transaksiId; 
    }
    public void setTransaksiId(int i)       { 
        this.transaksiId = i; 
    }
    public int getProdukId()                { 
        return produkId; 
    }
    public void setProdukId(int i)          { 
        this.produkId = i; 
    }
    public String getNamaProduk()           { 
        return namaProduk; 
    }
    public void setNamaProduk(String n)     { 
        this.namaProduk = n; 
    }
    public BigDecimal getHargaSatuan()      { 
        return hargaSatuan; 
    }
    public void setHargaSatuan(BigDecimal h){ 
        this.hargaSatuan = h; 
    }
    public int getQty()                     { 
        return qty; 
    }
    public void setQty(int q)               { 
        this.qty = q; 
    }
    public BigDecimal getDiskonItem()       { 
        return diskonItem; 
    }
    public void setDiskonItem(BigDecimal d) { 
        this.diskonItem = d; 
    }
    public BigDecimal getSubtotal()         { 
        return subtotal; 
    }
    public void setSubtotal(BigDecimal s)   { 
        this.subtotal = s; 
    }
}