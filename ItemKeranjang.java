import java.math.BigDecimal;

public class ItemKeranjang {
    private Produk produk;
    private int qty;
    private BigDecimal diskonItem;
    private BigDecimal subtotal;

    public ItemKeranjang(Produk produk, int qty) {
        this.produk = produk;
        this.qty = qty;
        this.diskonItem = BigDecimal.ZERO;
        hitungSubtotal();
    }

    public void hitungSubtotal() {
        subtotal = produk.getHarga()
            .multiply(new BigDecimal(qty))
            .subtract(diskonItem);
    }

    public Produk getProduk()               { return produk; }
    public int getQty()                     { return qty; }
    public void setQty(int qty)             { this.qty = qty; hitungSubtotal(); }
    public BigDecimal getDiskonItem()       { return diskonItem; }
    public void setDiskonItem(BigDecimal d) { this.diskonItem = d; hitungSubtotal(); }
    public BigDecimal getSubtotal()         { return subtotal; }
    public BigDecimal getHargaSatuan()      { return produk.getHarga(); }
    public String getNamaProduk()           { return produk.getNama(); }
    public int getProdukId()                { return produk.getId(); }
}