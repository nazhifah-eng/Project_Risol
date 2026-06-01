public class Customer {
    private String idCustomer;
    private String namaCustomer;

    public Customer() {
    }

    public Customer(String idCustomer, String namaCustomer) {
        this.idCustomer = idCustomer;
        this.namaCustomer = namaCustomer;
    }

    public String getIdCustomer() {
        return idCustomer;
    }

    public void setIdCustomer(String idCustomer) {
        this.idCustomer = idCustomer;
    }

    public String getNamaCustomer() {
        return namaCustomer;
    }

    public void setNamaCustomer(String namaCustomer) {
        this.namaCustomer = namaCustomer;
    }
}