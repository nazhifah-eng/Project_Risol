import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class AppTheme {

    public static final Color BG_DARK        = new Color(255, 253, 249); 
    public static final Color BG_CARD        = new Color(255, 255, 255); 
    public static final Color BG_SIDEBAR     = new Color(255, 250, 240); 
    public static final Color BG_INPUT       = new Color(255, 244, 232); 
    public static final Color BG_TABLE_ROW   = new Color(255, 255, 255);
    public static final Color BG_TABLE_ALT   = new Color(255, 250, 243);

    public static final Color ACCENT_ORANGE  = new Color(226, 135,  67); 
    public static final Color ACCENT_YELLOW  = new Color(245, 192, 122); 
    public static final Color ACCENT_RED     = new Color(204,  72,  72); 
    public static final Color ACCENT_GREEN   = new Color( 82, 168, 104); 
    public static final Color ACCENT_BLUE    = new Color( 90, 150, 210); 

    public static final Color TEXT_PRIMARY   = new Color( 74,  62,  61); 
    public static final Color TEXT_SECONDARY = new Color(122, 106, 104); 
    public static final Color TEXT_MUTED     = new Color(176, 160, 158); 
    public static final Color BORDER_COLOR   = new Color(234, 224, 212); 
    public static final Color SEPARATOR      = new Color(240, 232, 220);

static {
    loadFonts();
}

private static void loadFonts() {
    try {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        
        Font nunito = Font.createFont(Font.TRUETYPE_FONT,
            AppTheme.class.getResourceAsStream("/fonts/Nunito-Regular.ttf"));
        Font nunitoBold = Font.createFont(Font.TRUETYPE_FONT,
            AppTheme.class.getResourceAsStream("/fonts/Nunito-Bold.ttf"));
        Font playfair = Font.createFont(Font.TRUETYPE_FONT,
            AppTheme.class.getResourceAsStream("/fonts/PlayfairDisplay-Bold.ttf"));
        
        ge.registerFont(nunito);
        ge.registerFont(nunitoBold);
        ge.registerFont(playfair);
        
        System.out.println("✓ Font loaded successfully");
    } catch (Exception e) {
        System.err.println("Font load failed, using fallback: " + e.getMessage());
    }
}

public static final Font FONT_TITLE    = new Font("Playfair Display", Font.BOLD,  20);
public static final Font FONT_SUBTITLE = new Font("Nunito",           Font.BOLD,  14);
public static final Font FONT_BODY     = new Font("Nunito",           Font.PLAIN, 13);
public static final Font FONT_SMALL    = new Font("Nunito",           Font.PLAIN, 11);
public static final Font FONT_MONO     = new Font("Consolas",         Font.BOLD,  13);
public static final Font FONT_PRICE    = new Font("Playfair Display", Font.BOLD,  18);
public static final Font FONT_HUGE     = new Font("Playfair Display", Font.BOLD,  28);

    public static void applyTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { /* gunakan default */ }

        UIManager.put("Panel.background",             BG_CARD);
        UIManager.put("Frame.background",             BG_DARK);
        UIManager.put("Table.background",             BG_TABLE_ROW);
        UIManager.put("Table.alternateRowColor",      BG_TABLE_ALT);
        UIManager.put("Table.foreground",             TEXT_PRIMARY);
        UIManager.put("Table.gridColor",              BORDER_COLOR);
        UIManager.put("TableHeader.background",       BG_SIDEBAR);
        UIManager.put("TableHeader.foreground",       new Color(181, 98, 42));
        UIManager.put("ScrollPane.background",        BG_CARD);
        UIManager.put("ScrollBar.thumb",              new Color(220, 200, 180));
        UIManager.put("ScrollBar.track",              BG_CARD);
        UIManager.put("OptionPane.background",        BG_CARD);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("Button.background",            BG_CARD);
        UIManager.put("Button.foreground",            TEXT_PRIMARY);
        UIManager.put("TextField.background",         BG_INPUT);
        UIManager.put("TextField.foreground",         TEXT_PRIMARY);
        UIManager.put("ComboBox.background",          BG_INPUT);
        UIManager.put("ComboBox.foreground",          TEXT_PRIMARY);
        UIManager.put("Label.foreground",             TEXT_PRIMARY);
    }

  
    public static JButton makeButton(String text, Color color) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed()  ? color.darker()   :
                           getModel().isRollover() ? color.brighter() : color;
                g2.setColor(bg);
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

    public static JButton makeOutlineButton(String text, Color color) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover()
                    ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 35)
                    : new Color(color.getRed(), color.getGreen(), color.getBlue(), 12);
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

    public static <T> JComboBox<T> makeComboBox() {
        JComboBox<T> combo = new JComboBox<>();
        combo.setBackground(BG_INPUT);
        combo.setForeground(TEXT_PRIMARY);
        combo.setFont(FONT_BODY);
        combo.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        return combo;
    }

    public static JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setFont(FONT_BODY);
        return lbl;
    }

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
    public static Color statusColor(String status) {
        if (status == null) return TEXT_MUTED;
        switch (status.toLowerCase()) {
            case "lunas":   return ACCENT_GREEN;
            case "pending": return new Color(226, 135, 67);
            case "batal":   return ACCENT_RED;
            default:        return TEXT_SECONDARY;
        }
    }

    public static Color metodeBayarColor(String metode) {
        if (metode == null) return TEXT_SECONDARY;
        switch (metode.toLowerCase()) {
            case "qris":     return ACCENT_BLUE;
            case "transfer": return new Color(150, 100, 200);
            case "kartu":    return new Color(226, 135,  67);
            default:         return ACCENT_GREEN;
        }
    }
}