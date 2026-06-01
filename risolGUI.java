import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class risolGUI extends JFrame {
    JTextField txtID;
    JTextField txtNama;
    JTextField txtHarga;
    JTextField txtCari;

    JButton btnTambah;
    JButton btnUpdate;
    JButton btnDelete;
    JButton btnSearch;

    JTable tabel;
    DefaultTableModel model;

    JLabel lblUser;

    public risolGUI() {

        setTitle("Sistem Kasir Risol");
        setSize(1000,700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        lblUser = new JLabel("User : Owner");

        txtID = new JTextField(10);
        txtNama = new JTextField(15);
        txtHarga = new JTextField(10);
        txtCari = new JTextField(15);

        btnTambah = new JButton("Tambah");
        btnUpdate = new JButton("Update");
        btnDelete = new JButton("Delete");
        btnSearch = new JButton("Search");

        model = new DefaultTableModel();

        model.addColumn("ID");
        model.addColumn("Nama Produk");
        model.addColumn("Harga");

        tabel = new JTable(model);

        JPanel panel = new JPanel();

        panel.add(lblUser);

        panel.add(new JLabel("ID"));
        panel.add(txtID);

        panel.add(new JLabel("Nama"));
        panel.add(txtNama);

        panel.add(new JLabel("Harga"));
        panel.add(txtHarga);

        panel.add(btnTambah);
        panel.add(btnUpdate);
        panel.add(btnDelete);

        panel.add(txtCari);
        panel.add(btnSearch);

        add(panel, BorderLayout.NORTH);
        add(new JScrollPane(tabel),
                BorderLayout.CENTER);

        setVisible(true);
    }
}