package com.devmanchego.ui;

import com.devmanchego.app.FontScale;
import com.devmanchego.app.I18n;
import com.devmanchego.engine.ChessAI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * Modal welcome dialog shown at startup.
 * Lets the user choose 1-player vs 2-players, difficulty and piece color.
 * Persists "don't show again" via Java Preferences.
 */
public class WelcomeDialog extends JDialog {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(WelcomeDialog.class);
    private static final String PREF_SKIP = "welcomeSkip";

    public record Choice(boolean twoPlayers, ChessAI.Difficulty difficulty, boolean playerIsWhite) {}

    private Choice result = null;

    private WelcomeDialog(Frame owner, boolean showDontAskAgain) {
        super(owner, I18n.get("welcome.title"), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        // ── Root panel ───────────────────────────────────────────────────────
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(40, 40, 40));
        root.setBorder(new EmptyBorder(28, 36, 24, 36));
        setContentPane(root);

        // ── Title ────────────────────────────────────────────────────────────
        JLabel title = new JLabel("♟  DevManchego Chess", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, FontScale.scale(24)));
        title.setForeground(new Color(240, 200, 80));
        title.setBorder(new EmptyBorder(0, 0, 6, 0));
        root.add(title, BorderLayout.NORTH);

        // ── Center panel ─────────────────────────────────────────────────────
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(12, 0, 12, 0));
        root.add(center, BorderLayout.CENTER);

        // Subtitle
        JLabel subtitle = new JLabel(I18n.get("welcome.subtitle"), SwingConstants.CENTER);
        subtitle.setFont(new Font("Arial", Font.PLAIN, FontScale.scale(15)));
        subtitle.setForeground(new Color(200, 200, 200));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(subtitle);
        center.add(Box.createVerticalStrut(20));

        // ── Mode buttons ─────────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        btnPanel.setOpaque(false);
        btnPanel.setMaximumSize(new Dimension(380, 70));
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btn1P = makeButton(I18n.get("welcome.1player"), "♟",  new Color(80, 130, 200));
        JButton btn2P = makeButton(I18n.get("welcome.2players"), "♟♟", new Color(80, 160, 100));
        btnPanel.add(btn1P);
        btnPanel.add(btn2P);
        center.add(btnPanel);
        center.add(Box.createVerticalStrut(20));

        // ── Options panel (difficulty + color) — shown only for 1-player ─────
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setOpaque(false);
        optionsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Difficulty row
        JPanel diffRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        diffRow.setOpaque(false);

        JLabel diffLabel = new JLabel(I18n.get("welcome.difficulty"));
        diffLabel.setForeground(new Color(200, 200, 200));
        diffLabel.setFont(new Font("Arial", Font.PLAIN, FontScale.scale(14)));

        ChessAI.Difficulty[] diffs = ChessAI.Difficulty.values();
        String[] diffNames = new String[diffs.length];
        for (int i = 0; i < diffs.length; i++)
            diffNames[i] = I18n.get("difficulty." + diffs[i].name().toLowerCase());

        JComboBox<String> diffCombo = new JComboBox<>(diffNames);
        diffCombo.setSelectedIndex(3); // default: Hard
        diffCombo.setFont(new Font("Arial", Font.PLAIN, FontScale.scale(13)));
        diffCombo.setPreferredSize(new Dimension(200, 28));

        diffRow.add(diffLabel);
        diffRow.add(diffCombo);

        // Color row
        JPanel colorRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        colorRow.setOpaque(false);

        JLabel colorLabel = new JLabel(I18n.get("welcome.color"));
        colorLabel.setForeground(new Color(200, 200, 200));
        colorLabel.setFont(new Font("Arial", Font.PLAIN, FontScale.scale(14)));

        // Two toggle buttons for White / Black
        final boolean[] playerIsWhite = {true};

        JToggleButton btnWhite = makeColorToggle(I18n.get("welcome.white"),
                new Color(240, 230, 200), new Color(40, 40, 40));
        JToggleButton btnBlack = makeColorToggle(I18n.get("welcome.black"),
                new Color(60, 60, 60),   Color.WHITE);

        btnWhite.setSelected(true);
        markSelected(btnWhite, true);
        markSelected(btnBlack, false);

        ButtonGroup colorGroup = new ButtonGroup();
        colorGroup.add(btnWhite);
        colorGroup.add(btnBlack);

        btnWhite.addActionListener(e -> {
            playerIsWhite[0] = true;
            markSelected(btnWhite, true);
            markSelected(btnBlack, false);
        });
        btnBlack.addActionListener(e -> {
            playerIsWhite[0] = false;
            markSelected(btnWhite, false);
            markSelected(btnBlack, true);
        });

        colorRow.add(colorLabel);
        colorRow.add(btnWhite);
        colorRow.add(btnBlack);

        optionsPanel.add(diffRow);
        optionsPanel.add(Box.createVerticalStrut(10));
        optionsPanel.add(colorRow);

        center.add(optionsPanel);

        // ── "Don't show again" (hidden when launched from New Game menu) ────────
        JCheckBox dontShow = new JCheckBox(I18n.get("welcome.dontshow"));
        dontShow.setOpaque(false);
        dontShow.setForeground(new Color(160, 160, 160));
        dontShow.setFont(new Font("Arial", Font.PLAIN, FontScale.scale(12)));
        dontShow.setAlignmentX(Component.CENTER_ALIGNMENT);
        dontShow.setVisible(showDontAskAgain);
        center.add(Box.createVerticalStrut(showDontAskAgain ? 14 : 6));
        center.add(dontShow);

        // ── Button actions ────────────────────────────────────────────────────
        btn1P.addActionListener(e -> {
            PREFS.putBoolean(PREF_SKIP, dontShow.isSelected());
            ChessAI.Difficulty chosen = diffs[diffCombo.getSelectedIndex()];
            result = new Choice(false, chosen, playerIsWhite[0]);
            dispose();
        });

        btn2P.addActionListener(e -> {
            PREFS.putBoolean(PREF_SKIP, dontShow.isSelected());
            result = new Choice(true, ChessAI.Difficulty.HARD, true);
            dispose();
        });

        pack();
        setLocationRelativeTo(owner);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static JButton makeButton(String text, String icon, Color bg) {
        JButton b = new JButton("<html><center><span style='font-size:18pt'>"
                + icon + "</span><br>" + text + "</center></html>");
        b.setFont(new Font("Arial", Font.BOLD, FontScale.scale(13)));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(170, 70));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Color hover = bg.brighter();
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(hover); }
            public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }

    private static JToggleButton makeColorToggle(String text, Color bg, Color fg) {
        JToggleButton b = new JToggleButton(text);
        b.setFont(new Font("Arial", Font.BOLD, FontScale.scale(13)));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(110, 32));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static void markSelected(JToggleButton b, boolean selected) {
        b.setBorder(selected
                ? new LineBorder(new Color(240, 200, 80), 2)
                : BorderFactory.createEmptyBorder(2, 2, 2, 2));
    }

    // ── Public factory ────────────────────────────────────────────────────────

    /**
     * Shows the dialog if not suppressed.
     * Returns null if suppressed — caller should apply saved defaults.
     */
    /** Shows at startup — respects "don't show again" and shows the checkbox. */
    public static Choice show(Frame owner) {
        if (PREFS.getBoolean(PREF_SKIP, false)) return null;
        WelcomeDialog dlg = new WelcomeDialog(owner, true);
        dlg.setVisible(true);
        return dlg.result;
    }

    /** Shows from New Game menu — always visible, checkbox hidden. */
    public static Choice showForNewGame(Frame owner) {
        WelcomeDialog dlg = new WelcomeDialog(owner, false);
        dlg.setVisible(true);
        return dlg.result;
    }

    public static void resetPreference() {
        PREFS.remove(PREF_SKIP);
    }
}
