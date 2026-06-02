import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * AppTheme — Pusat tema visual seluruh aplikasi.
 *
 * FUNGSI:
 *  - Menyimpan konstanta warna (palet gelap + aksen oranye)
 *  - Menyimpan konstanta font
 *  - applyTheme()          : set UIManager agar semua komponen ikut tema gelap
 *  - makeButton()          : buat JButton solid berwarna dengan efek hover
 *  - makeOutlineButton()   : buat JButton outline transparan
 *  - makeTextField()       : buat JTextField bergaya gelap
 *  - makeComboBox()        : buat JComboBox bergaya gelap
 *  - makeLabel()           : buat JLabel warna sekunder
 *  - makeCard()            : buat JPanel rounded (kartu)
 *  - statusColor()         : warna sesuai status transaksi
 *  - metodeBayarColor()    : warna sesuai metode pembayaran
 */
public class AppTheme {

    // ── PALET WARNA ──────────────────────────────────────────────
    public static final Color BG_DARK        = new Color(18, 18, 24);
    public static final Color BG_CARD        = new Color(26, 27, 38);
    public static final Color BG_SIDEBAR     = new Color(22, 22, 32);
    public static final Color BG_INPUT       = new Color(35, 36, 52);
    public static final Color BG_TABLE_ROW   = new Color(30, 31, 45);
    public static final Color BG_TABLE_ALT   = new Color(34, 35, 52);

    public static final Color ACCENT_ORANGE  = new Color(255, 120, 40);
    public static final Color ACCENT_YELLOW  = new Color(255, 195, 60);
    public static final Color ACCENT_RED     = new Color(255, 75,  75);
    public static final Color ACCENT_GREEN   = new Color(60,  210, 130);
    public static final Color ACCENT_BLUE    = new Color(70,  160, 255);

    public static final Color TEXT_PRIMARY   = new Color(240, 240, 255);
    public static final Color TEXT_SECONDARY = new Color(155, 158, 180);
    public static final Color TEXT_MUTED     = new Color(90,  95,  120);
    public static final Color BORDER_COLOR   = new Color(50,  52,  75);
    public static final Color SEPARATOR      = new Color(45,  47,  68);

    // ── FONT ─────────────────────────────────────────────────────
    public static final Font FONT_TITLE    = new Font("Segoe UI", Font.BOLD,  20);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD,  14);
    public static final Font FONT_BODY     = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL    = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO     = new Font("Consolas",  Font.BOLD,  13);
    public static final Font FONT_PRICE    = new Font("Segoe UI", Font.BOLD,  18);
    public static final Font FONT_HUGE     = new Font("Segoe UI", Font.BOLD,  28);

    // ── INISIALISASI TEMA GLOBAL ──────────────────────────────────
    /**
     * Menerapkan warna tema ke UIManager sehingga komponen Swing
     * otomatis menggunakan warna gelap tanpa harus di-set satu per satu.
     */
    public static void applyTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        UIManager.put("Panel.background",             BG_CARD);
        UIManager.put("Frame.background",             BG_DARK);
        UIManager.put("Table.background",             BG_TABLE_ROW);
        UIManager.put("Table.alternateRowColor",      BG_TABLE_ALT);
        UIManager.put("Table.foreground",             TEXT_PRIMARY);
        UIManager.put("Table.gridColor",              BORDER_COLOR);
        UIManager.put("TableHeader.background",       BG_SIDEBAR);
        UIManager.put("TableHeader.foreground",       ACCENT_ORANGE);
        UIManager.put("ScrollPane.background",        BG_CARD);
        UIManager.put("ScrollBar.thumb",              new Color(60, 62, 88));
        UIManager.put("ScrollBar.track",              BG_CARD);
        UIManager.put("OptionPane.background",        BG_CARD);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
    }

    // ── FACTORY: TOMBOL SOLID ─────────────────────────────────────
    /**
     * Membuat JButton solid dengan warna custom.
     * Hover → lebih terang, Press → lebih gelap.
     *
     * @param text  label tombol
     * @param color warna latar tombol
     */
    public static JButton makeButton(String text, Color color) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(color.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(color.brighter());
                } else {
                    g2.setColor(color);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(FONT_SUBTITLE);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth()  - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(140, 38));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── FACTORY: TOMBOL OUTLINE ───────────────────────────────────
    /**
     * Membuat JButton transparan dengan border berwarna.
     * Cocok untuk tombol sekunder (Batal, Refresh, dll.)
     */
    public static JButton makeOutlineButton(String text, Color color) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover()
                    ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 40)
                    : new Color(color.getRed(), color.getGreen(), color.getBlue(), 15);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
                g2.setFont(FONT_BODY);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth()  - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(color);
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(120, 34));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── FACTORY: INPUT FIELD ──────────────────────────────────────
    /**
     * Membuat JTextField dengan latar gelap dan border tipis.
     * @param columns lebar kolom (perkiraan karakter)
     */
    public static JTextField makeTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setBackground(BG_INPUT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_ORANGE);
        field.setFont(FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return field;
    }

    /** Membuat JComboBox dengan latar gelap. */
    public static <T> JComboBox<T> makeComboBox() {
        JComboBox<T> combo = new JComboBox<>();
        combo.setBackground(BG_INPUT);
        combo.setForeground(TEXT_PRIMARY);
        combo.setFont(FONT_BODY);
        combo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        return combo;
    }

    /** Membuat JLabel dengan warna teks sekunder (abu-abu terang). */
    public static JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setFont(FONT_BODY);
        return lbl;
    }

    /**
     * Membuat JPanel berbentuk kartu (rounded corner).
     * Latar BG_CARD dengan border tipis BORDER_COLOR.
     * @param radius sudut melengkung (px)
     */
    public static JPanel makeCard(int radius) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    // ── HELPER WARNA DINAMIS ──────────────────────────────────────
    /**
     * Mengembalikan warna sesuai status transaksi.
     *   Lunas   → hijau
     *   Pending → kuning
     *   Batal   → merah
     */
    public static Color statusColor(String status) {
        if (status == null) return TEXT_MUTED;
        switch (status.toLowerCase()) {
            case "lunas":   return ACCENT_GREEN;
            case "pending": return ACCENT_YELLOW;
            case "batal":   return ACCENT_RED;
            default:        return TEXT_SECONDARY;
        }
    }

    /**
     * Mengembalikan warna sesuai metode pembayaran.
     *   QRIS     → biru
     *   Transfer → ungu
     *   Kartu    → oranye muda
     *   Tunai    → hijau
     */
    public static Color metodeBayarColor(String metode) {
        if (metode == null) return TEXT_SECONDARY;
        switch (metode.toLowerCase()) {
            case "qris":     return ACCENT_BLUE;
            case "transfer": return new Color(150, 100, 255);
            case "kartu":    return new Color(255, 160, 60);
            default:         return ACCENT_GREEN;
        }
    }
}