package com.devmanchego.ui;

import com.devmanchego.app.FontScale;
import com.devmanchego.app.I18n;
import com.devmanchego.engine.AnalysisResult;
import com.devmanchego.engine.AnalysisResult.MoveQuality;
import com.devmanchego.engine.GameState;
import com.devmanchego.engine.Move;
import com.devmanchego.engine.Piece;
import com.devmanchego.engine.PieceType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AnalysisPanel extends JPanel {

    // ── Engine info labels ────────────────────────────────────────────────────
    private final JLabel engineLabel  = label(I18n.get("analysis.engine"), Font.BOLD, 13);
    private final JLabel depthLabel   = mono(I18n.get("analysis.depth", "—"));
    private final JLabel scoreLabel   = mono(I18n.get("analysis.score", "—"));
    private final JLabel qualityLabel = new JLabel(" ");

    // ── PV (left half of centre) ──────────────────────────────────────────────
    private final JLabel    pvTitle = label(I18n.get("analysis.pv"), Font.BOLD, 12);
    private final JTextArea pvArea  = new JTextArea();

    // ── Captured pieces (right half of centre) ────────────────────────────────
    private final JLabel capturedTitle       = label(I18n.get("captured.title"),  Font.BOLD, 12);
    private final JLabel capturedWhiteLabel  = label(I18n.get("captured.white"),  Font.BOLD, 11);
    private final JLabel capturedBlackLabel  = label(I18n.get("captured.black"),  Font.BOLD, 11);
    private final CapturedPiecesPanel capturedWhitePanel = new CapturedPiecesPanel();
    private final CapturedPiecesPanel capturedBlackPanel = new CapturedPiecesPanel();

    // ── Game history (bottom) ─────────────────────────────────────────────────
    private final JLabel    gameHistTitle = label(I18n.get("analysis.gamehistory"), Font.BOLD, 12);
    private final JTextArea gameHistArea  = new JTextArea();

    // ── Quality-dot history ───────────────────────────────────────────────────
    private final List<MoveQuality> whiteHistory = new ArrayList<>();
    private final List<MoveQuality> blackHistory = new ArrayList<>();
    private final MoveHistoryPanel  historyPanel = new MoveHistoryPanel();


    // ─────────────────────────────────────────────────────────────────────────

    public AnalysisPanel() {
        setLayout(new BorderLayout(0, 4));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setBackground(new Color(40, 40, 40));

        // ── NORTH: quality dots + engine info ─────────────────────────────────
        JPanel engineInfo = new JPanel(new GridLayout(0, 1, 0, 2));
        engineInfo.setBackground(getBackground());
        engineInfo.add(engineLabel);
        engineInfo.add(depthLabel);
        engineInfo.add(scoreLabel);
        engineInfo.add(qualityLabel);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setBackground(getBackground());
        north.add(historyPanel);
        north.add(Box.createVerticalStrut(6));
        north.add(engineInfo);
        north.add(Box.createVerticalStrut(4));

        // ── CENTRE LEFT: PV ──────────────────────────────────────────────────
        pvArea.setFont(new Font("Monospaced", Font.PLAIN, FontScale.scale(12)));
        pvArea.setEditable(false);
        pvArea.setBackground(new Color(55, 55, 55));
        pvArea.setForeground(Color.LIGHT_GRAY);
        pvArea.setBorder(new EmptyBorder(4, 4, 4, 4));
        pvArea.setLineWrap(true);
        pvArea.setWrapStyleWord(false);
        pvArea.setText(I18n.get("analysis.placeholder"));

        JScrollPane pvScroll = new JScrollPane(pvArea);
        pvScroll.setBorder(null);

        JPanel pvWrapper = new JPanel(new BorderLayout(0, 3));
        pvWrapper.setBackground(getBackground());
        pvWrapper.add(pvTitle, BorderLayout.NORTH);
        pvWrapper.add(pvScroll, BorderLayout.CENTER);

        // ── CENTRE RIGHT: captured pieces ─────────────────────────────────────
        JPanel capturedWrapper = new JPanel(new BorderLayout(0, 6));
        capturedWrapper.setBackground(getBackground());
        capturedWrapper.setBorder(new EmptyBorder(0, 6, 0, 0));

        JPanel capturedInner = new JPanel();
        capturedInner.setLayout(new BoxLayout(capturedInner, BoxLayout.Y_AXIS));
        capturedInner.setBackground(getBackground());

        capturedWhiteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        capturedBlackLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        capturedWhitePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        capturedBlackPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        capturedInner.add(capturedWhiteLabel);
        capturedInner.add(Box.createVerticalStrut(4));
        capturedInner.add(capturedWhitePanel);
        capturedInner.add(Box.createVerticalStrut(10));
        capturedInner.add(capturedBlackLabel);
        capturedInner.add(Box.createVerticalStrut(4));
        capturedInner.add(capturedBlackPanel);

        capturedWrapper.add(capturedTitle, BorderLayout.NORTH);
        capturedWrapper.add(capturedInner, BorderLayout.CENTER);

        // ── CENTRE: split PV | Captured ──────────────────────────────────────
        JPanel centrePanel = new JPanel(new GridLayout(1, 2, 8, 0));
        centrePanel.setBackground(getBackground());
        centrePanel.add(pvWrapper);
        centrePanel.add(capturedWrapper);

        // ── SOUTH: game move history ──────────────────────────────────────────
        gameHistArea.setFont(new Font("Monospaced", Font.PLAIN, FontScale.scale(12)));
        gameHistArea.setEditable(false);
        gameHistArea.setBackground(new Color(45, 45, 45));
        gameHistArea.setForeground(new Color(180, 210, 255));
        gameHistArea.setBorder(new EmptyBorder(4, 4, 4, 4));
        gameHistArea.setLineWrap(true);
        gameHistArea.setWrapStyleWord(false);

        JScrollPane gameHistScroll = new JScrollPane(gameHistArea);
        gameHistScroll.setBorder(null);
        gameHistScroll.setPreferredSize(new Dimension(0, 160));

        JPanel gameHistWrapper = new JPanel(new BorderLayout(0, 3));
        gameHistWrapper.setBackground(getBackground());
        gameHistWrapper.add(gameHistTitle, BorderLayout.NORTH);
        gameHistWrapper.add(gameHistScroll, BorderLayout.CENTER);

        // ── Assemble ──────────────────────────────────────────────────────────
        add(north,           BorderLayout.NORTH);
        add(centrePanel,     BorderLayout.CENTER);
        add(gameHistWrapper, BorderLayout.SOUTH);

        FontScale.addChangeListener(this::refreshFonts);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Full analysis update (from Analyze button). */
    public void update(AnalysisResult r) {
        if (r == null) return;
        depthLabel.setText(I18n.get("analysis.depth",  r.depth));
        scoreLabel.setText(I18n.get("analysis.score",  r.scoreLabel()));
        pvArea.setText(r.pv != null && !r.pv.isEmpty() ? formatPV(r.pv) : "—");
        pvArea.setCaretPosition(0);
        updateQualityLabel(r.quality, r.cpLoss);
        revalidate();
        repaint();
    }

    /** Redraws captured pieces from the move-tracked capture lists in GameState. */
    public void updateCapturedPieces(GameState state) {
        if (state == null) {
            capturedWhitePanel.setPieces(List.of());
            capturedBlackPanel.setPieces(List.of());
            return;
        }
        capturedWhitePanel.setPieces(sortedTypes(state.getCapturedByWhite()));
        capturedBlackPanel.setPieces(sortedTypes(state.getCapturedByBlack()));
        revalidate();
        repaint();
    }

    /** Update the game-history area with the full PGN of moves played so far. */
    public void updateGameHistory(String pgn) {
        gameHistArea.setText(pgn == null || pgn.isBlank() ? "—" : pgn);
        gameHistArea.setCaretPosition(gameHistArea.getDocument().getLength());
    }

    /** Silent per-move quality update (no Analyze needed). */
    public void addMoveQuality(boolean white, MoveQuality q, int cpLoss) {
        if (q == null) return;
        if (white) whiteHistory.add(q);
        else       blackHistory.add(q);
        updateQualityLabel(q, cpLoss);
        historyPanel.refresh();
        revalidate();
        repaint();
    }

    public void resetMoveHistory() {
        whiteHistory.clear();
        blackHistory.clear();
        qualityLabel.setText(" ");
        gameHistArea.setText("—");
        capturedWhitePanel.setPieces(List.of());
        capturedBlackPanel.setPieces(List.of());
        historyPanel.refresh();
        revalidate();
        repaint();
    }

    public void setStatus(String text) {
        engineLabel.setText(I18n.get("analysis.engine") + " — " + text);
    }

    /** Re-applies all explicit fonts using the current FontScale level. */
    public void refreshFonts() {
        engineLabel.setFont(new Font("Arial", Font.BOLD, FontScale.scale(13)));
        depthLabel.setFont(new Font("Monospaced", Font.PLAIN, FontScale.scale(12)));
        scoreLabel.setFont(new Font("Monospaced", Font.PLAIN, FontScale.scale(12)));
        pvTitle.setFont(new Font("Arial", Font.BOLD, FontScale.scale(12)));
        pvArea.setFont(new Font("Monospaced", Font.PLAIN, FontScale.scale(12)));
        capturedTitle.setFont(new Font("Arial", Font.BOLD, FontScale.scale(12)));
        capturedWhiteLabel.setFont(new Font("Arial", Font.BOLD, FontScale.scale(11)));
        capturedBlackLabel.setFont(new Font("Arial", Font.BOLD, FontScale.scale(11)));
        gameHistTitle.setFont(new Font("Arial", Font.BOLD, FontScale.scale(12)));
        gameHistArea.setFont(new Font("Monospaced", Font.PLAIN, FontScale.scale(12)));
        historyPanel.revalidate();
        capturedWhitePanel.revalidate();
        capturedBlackPanel.revalidate();
        revalidate();
        repaint();
    }

    public void refreshTexts() {
        engineLabel.setText(I18n.get("analysis.engine"));
        if (depthLabel.getText().contains("—"))
            depthLabel.setText(I18n.get("analysis.depth", "—"));
        if (scoreLabel.getText().contains("—"))
            scoreLabel.setText(I18n.get("analysis.score", "—"));
        pvTitle.setText(I18n.get("analysis.pv"));
        capturedTitle.setText(I18n.get("captured.title"));
        capturedWhiteLabel.setText(I18n.get("captured.white"));
        capturedBlackLabel.setText(I18n.get("captured.black"));
        gameHistTitle.setText(I18n.get("analysis.gamehistory"));
        if (pvArea.getText().isBlank())
            pvArea.setText(I18n.get("analysis.placeholder"));
        historyPanel.refresh();
        revalidate();
        repaint();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static List<PieceType> sortedTypes(List<Piece> pieces) {
        PieceType[] order = {
            PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP,
            PieceType.KNIGHT, PieceType.PAWN
        };
        List<PieceType> result = new ArrayList<>();
        for (PieceType t : order)
            for (Piece p : pieces)
                if (p.getType() == t) result.add(t);
        return result;
    }

    private void updateQualityLabel(MoveQuality q, int cpLoss) {
        if (q == null) { qualityLabel.setText(" "); return; }
        String qlabel = I18n.get("quality." + q.name().toLowerCase());
        qualityLabel.setText(qlabel + (cpLoss > 0 ? "  (−" + cpLoss + " cp)" : ""));
        qualityLabel.setForeground(q.color);
    }

    private String formatPV(List<Move> pv) {
        StringBuilder sb = new StringBuilder();
        int moveNum = 1;
        for (int i = 0; i < pv.size(); i++) {
            if (i % 2 == 0) sb.append(moveNum++).append(". ");
            sb.append(pv.get(i).toUCI());
            sb.append(i % 2 == 0 ? "  " : "\n");
        }
        return sb.toString().trim();
    }

    private static JLabel label(String text, int style, int size) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", style, FontScale.scale(size)));
        l.setForeground(Color.LIGHT_GRAY);
        return l;
    }

    private static JLabel mono(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.PLAIN, FontScale.scale(12)));
        l.setForeground(Color.LIGHT_GRAY);
        return l;
    }

    // ── Inner: captured pieces panel ──────────────────────────────────────────

    private static class CapturedPiecesPanel extends JPanel {

        private List<PieceType> pieces = new ArrayList<>();

        CapturedPiecesPanel() {
            setBackground(new Color(50, 50, 50));
            setBorder(new EmptyBorder(4, 4, 4, 4));
        }

        void setPieces(List<PieceType> p) {
            this.pieces = new ArrayList<>(p);
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (pieces.isEmpty()) {
                g.setColor(new Color(130, 130, 130));
                g.setFont(new Font("Arial", Font.PLAIN, FontScale.scale(12)));
                g.drawString(I18n.get("captured.none"), 6, 20);
                return;
            }
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // Draw pieces as Unicode chess symbols, two rows max
            int size = FontScale.scale(20);
            int gap  = 2;
            int x = 4, y = size + 2;
            int maxW = getWidth() - 8;
            g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, size));
            g2.setColor(new Color(220, 220, 220));
            for (PieceType t : pieces) {
                String sym = symbol(t);
                g2.drawString(sym, x, y);
                x += size + gap;
                if (x + size > maxW) { x = 4; y += size + gap; }
            }
        }

        @Override
        public Dimension getPreferredSize() { return new Dimension(0, FontScale.scale(52)); }

        @Override
        public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, FontScale.scale(52)); }

        private static String symbol(PieceType t) {
            return switch (t) {
                case QUEEN  -> "♛";
                case ROOK   -> "♜";
                case BISHOP -> "♝";
                case KNIGHT -> "♞";
                case PAWN   -> "♟";
                case KING   -> "♚";
            };
        }
    }

    // ── Inner: quality-dot history panel ─────────────────────────────────────

    private class MoveHistoryPanel extends JPanel {

        MoveHistoryPanel() {
            setBackground(new Color(40, 40, 40));
        }

        void refresh() { revalidate(); repaint(); }

        @Override
        public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, FontScale.scale(120)); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int dotSize = FontScale.scale(12), gap = 4, colW = w / 2 - 8;

            drawPlayerRow(g2, I18n.get("history.white"), whiteHistory,  4,   0, colW, dotSize, gap);
            drawPlayerRow(g2, I18n.get("history.black"), blackHistory, w/2,  0, colW, dotSize, gap);
        }

        private void drawPlayerRow(Graphics2D g2, String title,
                                   List<MoveQuality> history,
                                   int x, int y, int maxW, int dotSize, int gap) {
            g2.setFont(new Font("Arial", Font.BOLD, FontScale.scale(11)));
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString(title, x, y + 14);

            String acc = history.isEmpty() ? "—" : accuracy(history) + "%";
            g2.setFont(new Font("Arial", Font.PLAIN, FontScale.scale(11)));
            g2.setColor(Color.GRAY);
            g2.drawString(I18n.get("history.accuracy", acc), x, y + 28);

            int dx = x, dy = y + 38;
            for (MoveQuality q : history) {
                g2.setColor(q.color);
                g2.fillRoundRect(dx, dy, dotSize, dotSize, 4, 4);
                dx += dotSize + gap;
                if (dx + dotSize > x + maxW) { dx = x; dy += dotSize + gap; }
            }
        }

        private int accuracy(List<MoveQuality> history) {
            if (history.isEmpty()) return 0;
            int total = 0;
            for (MoveQuality q : history)
                total += switch (q) {
                    case BEST -> 100; case EXCELLENT -> 90; case GOOD -> 75;
                    case INACCURACY -> 50; case MISTAKE -> 25; case BLUNDER -> 0;
                };
            return total / history.size();
        }

        @Override
        public Dimension getPreferredSize() {
            double rowH = FontScale.scale(12) + 4; // dotSize + gap, matches drawPlayerRow
            int rows = Math.max(
                (int) Math.ceil(whiteHistory.size() * rowH / Math.max(1, getWidth() / 2 - 8)),
                (int) Math.ceil(blackHistory.size() * rowH / Math.max(1, getWidth() / 2 - 8)));
            return new Dimension(0, FontScale.scale(50) + (int) Math.round(rows * rowH));
        }
    }
}
