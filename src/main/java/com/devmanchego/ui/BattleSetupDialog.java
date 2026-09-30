package com.devmanchego.ui;

import com.devmanchego.app.FontScale;
import com.devmanchego.app.I18n;
import com.devmanchego.engine.GameState.BattleType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Dialog for choosing Side (A/B) and Color (White/Black) before a Special Battle.
 */
public class BattleSetupDialog extends JDialog {

    public record BattleChoice(boolean playerSideA, boolean playerIsWhite) {}

    private BattleChoice result = null;

    private BattleSetupDialog(Frame owner, BattleType type) {
        super(owner, battleName(type), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(40, 40, 40));
        root.setBorder(new EmptyBorder(24, 32, 20, 32));
        setContentPane(root);

        // ── Title ────────────────────────────────────────────────────────────
        JLabel title = new JLabel(battleName(type), SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, FontScale.scale(20)));
        title.setForeground(new Color(240, 200, 80));
        root.add(title, BorderLayout.NORTH);

        // ── Center ───────────────────────────────────────────────────────────
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(16, 0, 10, 0));
        root.add(center, BorderLayout.CENTER);

        // Side cards
        JPanel cardsRow = new JPanel(new GridLayout(1, 2, 14, 0));
        cardsRow.setOpaque(false);
        cardsRow.setMaximumSize(new Dimension(500, 90));
        cardsRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JToggleButton cardA = makeSideCard(
                I18n.get("battle.dialog.sideA"), sideADesc(type), new Color(70, 110, 180));
        JToggleButton cardB = makeSideCard(
                I18n.get("battle.dialog.sideB"), sideBDesc(type), new Color(160, 80, 80));

        final boolean[] playerSideA = {true};
        cardA.setSelected(true);
        highlightCard(cardA, true);
        highlightCard(cardB, false);

        ButtonGroup sideGroup = new ButtonGroup();
        sideGroup.add(cardA);
        sideGroup.add(cardB);

        cardA.addActionListener(e -> { playerSideA[0] = true;  highlightCard(cardA, true);  highlightCard(cardB, false); });
        cardB.addActionListener(e -> { playerSideA[0] = false; highlightCard(cardA, false); highlightCard(cardB, true);  });

        cardsRow.add(cardA);
        cardsRow.add(cardB);
        center.add(cardsRow);
        center.add(Box.createVerticalStrut(18));

        // VS label
        JLabel vs = new JLabel(I18n.get("battle.dialog.vs"), SwingConstants.CENTER);
        vs.setFont(new Font("Arial", Font.BOLD, FontScale.scale(11)));
        vs.setForeground(new Color(140, 140, 140));
        vs.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Color row
        JPanel colorRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        colorRow.setOpaque(false);
        colorRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel colorLbl = new JLabel(I18n.get("battle.dialog.color"));
        colorLbl.setForeground(new Color(200, 200, 200));
        colorLbl.setFont(new Font("Arial", Font.PLAIN, FontScale.scale(14)));

        final boolean[] playerIsWhite = {true};

        JToggleButton btnWhite = makeColorToggle(I18n.get("welcome.white"),
                new Color(240, 230, 200), new Color(40, 40, 40));
        JToggleButton btnBlack = makeColorToggle(I18n.get("welcome.black"),
                new Color(60, 60, 60), Color.WHITE);

        btnWhite.setSelected(true);
        markSelected(btnWhite, true);
        markSelected(btnBlack, false);

        ButtonGroup colorGroup = new ButtonGroup();
        colorGroup.add(btnWhite);
        colorGroup.add(btnBlack);

        btnWhite.addActionListener(e -> { playerIsWhite[0] = true;  markSelected(btnWhite, true);  markSelected(btnBlack, false); });
        btnBlack.addActionListener(e -> { playerIsWhite[0] = false; markSelected(btnWhite, false); markSelected(btnBlack, true);  });

        colorRow.add(colorLbl);
        colorRow.add(btnWhite);
        colorRow.add(btnBlack);
        center.add(colorRow);
        center.add(Box.createVerticalStrut(20));

        // Start button
        JButton startBtn = new JButton(I18n.get("battle.dialog.start"));
        startBtn.setFont(new Font("Arial", Font.BOLD, FontScale.scale(14)));
        startBtn.setBackground(new Color(60, 160, 80));
        startBtn.setForeground(Color.WHITE);
        startBtn.setFocusPainted(false);
        startBtn.setBorderPainted(false);
        startBtn.setOpaque(true);
        startBtn.setPreferredSize(new Dimension(180, 38));
        startBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        Color startHover = new Color(80, 185, 100);
        startBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { startBtn.setBackground(startHover); }
            public void mouseExited (java.awt.event.MouseEvent e) { startBtn.setBackground(new Color(60, 160, 80)); }
        });
        startBtn.addActionListener(e -> {
            result = new BattleChoice(playerSideA[0], playerIsWhite[0]);
            dispose();
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setOpaque(false);
        btnPanel.add(startBtn);
        center.add(btnPanel);

        pack();
        setLocationRelativeTo(owner);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static JToggleButton makeSideCard(String title, String desc, Color bg) {
        JToggleButton b = new JToggleButton(
                "<html><center><b>" + title + "</b><br><small>" + desc + "</small></center></html>");
        b.setFont(new Font("Arial", Font.PLAIN, FontScale.scale(12)));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(220, 80));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static void highlightCard(JToggleButton b, boolean on) {
        b.setBorder(on ? new LineBorder(new Color(240, 200, 80), 3)
                       : new LineBorder(new Color(80, 80, 80), 1));
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

    private static void markSelected(JToggleButton b, boolean on) {
        b.setBorder(on ? new LineBorder(new Color(240, 200, 80), 2)
                       : BorderFactory.createEmptyBorder(2, 2, 2, 2));
    }

    private static String battleName(BattleType t) {
        return switch (t) {
            case CAVALRY_DUEL       -> I18n.get("battle.cavalry.name");
            case HEAVY_ARTILLERY    -> I18n.get("battle.artillery.name");
            case PAWN_WALL          -> I18n.get("battle.pawnwall.name");
            case MINOR_PIECE_CLASH  -> I18n.get("battle.minor.name");
        };
    }

    private static String sideADesc(BattleType t) {
        return switch (t) {
            case CAVALRY_DUEL       -> I18n.get("battle.cavalry.sideA");
            case HEAVY_ARTILLERY    -> I18n.get("battle.artillery.sideA");
            case PAWN_WALL          -> I18n.get("battle.pawnwall.sideA");
            case MINOR_PIECE_CLASH  -> I18n.get("battle.minor.sideA");
        };
    }

    private static String sideBDesc(BattleType t) {
        return switch (t) {
            case CAVALRY_DUEL       -> I18n.get("battle.cavalry.sideB");
            case HEAVY_ARTILLERY    -> I18n.get("battle.artillery.sideB");
            case PAWN_WALL          -> I18n.get("battle.pawnwall.sideB");
            case MINOR_PIECE_CLASH  -> I18n.get("battle.minor.sideB");
        };
    }

    // ── Public factory ────────────────────────────────────────────────────────

    /** Shows the dialog; returns null if the user closed it without choosing. */
    public static BattleChoice show(Frame owner, BattleType type) {
        BattleSetupDialog dlg = new BattleSetupDialog(owner, type);
        dlg.setVisible(true);
        return dlg.result;
    }
}
