package com.devmanchego.ui;

import com.devmanchego.app.FontScale;
import com.devmanchego.app.I18n;
import com.devmanchego.engine.AnalysisResult;
import com.devmanchego.engine.ChessAI;
import com.devmanchego.engine.GameState;
import com.devmanchego.engine.Move;
import com.devmanchego.engine.Piece;
import com.devmanchego.engine.PieceType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ChessBoardUI extends JPanel {

    private final GameState gameState;
    private JLabel gameStatusLabel;
    JFrame frame;

    private ChessAI      ai         = null;  // null = 2-player mode
    private boolean      aiThinking = false;

    private EnginePanel    enginePanel   = null;
    private AnalysisPanel  analysisPanel = null;
    private AnalysisResult lastAnalysis  = null;

    // Move-quality classifier: single-threaded executor so tasks run in move order.
    // classifyPrevScore and classifyGeneration are managed INSIDE the executor thread
    // (AtomicReference/AtomicInteger for cross-thread visibility only; no EDT races).
    private final AtomicReference<Integer> classifyPrevScore  = new AtomicReference<>(null);
    private final AtomicInteger            classifyGeneration = new AtomicInteger(0);
    private ExecutorService classifyExecutor = newClassifyExecutor();

    private static ExecutorService newClassifyExecutor() {
        return Executors.newSingleThreadExecutor(
                r -> { Thread t = new Thread(r, "quick-classify"); t.setDaemon(true); return t; });
    }

    private static final int TILE_SIZE = 80;

    private boolean flipped = false; // true → Black at bottom

    private int selectedRow = -1;
    private int selectedColumn = -1;

    public ChessBoardUI(GameState gameState, JFrame jframe, JLabel gameStatusLabel) {

        this.gameState = gameState;
        this.frame = jframe;
        this.gameStatusLabel = gameStatusLabel;

        setPreferredSize(new Dimension(640, 640));

        FontScale.addChangeListener(this::repaint);

        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (aiThinking) return;
                int row = bRow(e.getY() / TILE_SIZE);
                int col = bCol(e.getX() / TILE_SIZE);
                Piece piece = gameState.getPiece(row, col);
                // Only select a piece belonging to the side to move — otherwise
                // getLegalMovesFrom() (which ignores turn) would draw legal-move
                // dots for a piece that can't actually move this turn.
                if (piece != null && piece.isWhite() != gameState.isWhiteTurn()) {
                    selectedRow    = -1;
                    selectedColumn = -1;
                } else {
                    selectedRow    = row;
                    selectedColumn = col;
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (aiThinking) return;

                int targetRow    = bRow(e.getY() / TILE_SIZE);
                int targetColumn = bCol(e.getX() / TILE_SIZE);

                Move move = new Move(
                        selectedRow,
                        selectedColumn,
                        targetRow,
                        targetColumn
                );

                Piece movingPiece = gameState.getPiece(selectedRow, selectedColumn);
                if (movingPiece != null && movingPiece.getType() == PieceType.PAWN
                        && isPromotionRow(movingPiece.isWhite(), targetRow)
                        && gameState.isLegalMove(move)) {
                    move.promotionType = choosePromotion(movingPiece.isWhite());
                }

                boolean wasWhite = gameState.isWhiteTurn(); // before applyMove flips turn
                int movesBefore  = gameState.getMoveCount();
                gameState.applyMove(move);
                boolean moveMade = gameState.getMoveCount() > movesBefore;

                selectedRow = -1;
                selectedColumn = -1;

                if (moveMade) {
                    lastAnalysis = null;
                    updateGameStatus();
                    updateGameHistory();
                    silentClassify(wasWhite);
                }

                repaint();

                if (moveMade) {
                    if (ai != null && ai.isAiTurn(gameState) && !isGameOver()) {
                        triggerAiMove();
                    } else {
                        maybeAutoAnalyze();
                    }
                }
            }
        });
    }

    private static boolean isPromotionRow(boolean white, int row) {
        return white ? row == 0 : row == 7;
    }

    /** Asks the human player which piece to promote to. AI moves always default to queen. */
    private PieceType choosePromotion(boolean white) {
        String[] options = {
                I18n.get("promotion.queen"),
                I18n.get("promotion.rook"),
                I18n.get("promotion.bishop"),
                I18n.get("promotion.knight")
        };
        int choice = JOptionPane.showOptionDialog(frame,
                I18n.get("promotion.message"),
                I18n.get("promotion.title"),
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
        return switch (choice) {
            case 1 -> PieceType.ROOK;
            case 2 -> PieceType.BISHOP;
            case 3 -> PieceType.KNIGHT;
            default -> PieceType.QUEEN; // includes -1 (dialog dismissed)
        };
    }

    /** Set to non-null to enable AI mode; null disables it. */
    public void setAI(ChessAI ai) { this.ai = ai; }

    /** The AI currently playing this game, or null in 2-player mode. */
    public ChessAI getAI() { return ai; }

    /** Flip the board so Black pieces appear at the bottom. */
    public void setFlipped(boolean flipped) { this.flipped = flipped; repaint(); }

    // Coordinate helpers
    private int vRow(int boardRow) { return flipped ? 7 - boardRow : boardRow; }
    private int vCol(int boardCol) { return flipped ? 7 - boardCol : boardCol; }
    private int bRow(int visualRow) { return flipped ? 7 - visualRow : visualRow; }
    private int bCol(int visualCol) { return flipped ? 7 - visualCol : visualCol; }

    public void refreshStatus() { updateGameStatus(); repaint(); }

    /** Refreshes the game-history and captured-pieces panels from current state
     *  (e.g. after loading a saved game). */
    public void refreshHistoryPanels() { updateGameHistory(); }

    /**
     * Renders the current board (squares + coordinates + pieces) to a
     * BufferedImage. Arrows and badges are intentionally omitted.
     */
    public java.awt.image.BufferedImage renderToImage() {
        int size = 8 * TILE_SIZE;
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(size, size,
                        java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBoard(g2);
        drawCoordinates(g2);
        drawPieces(g2);

        g2.dispose();
        return img;
    }

    public void resetSilentHistory() {
        classifyExecutor.shutdownNow();
        classifyExecutor = newClassifyExecutor();
        classifyGeneration.incrementAndGet();
        classifyPrevScore.set(null); // first move of new game uses null → BEST fallback
        if (analysisPanel != null) {
            analysisPanel.resetMoveHistory();
            analysisPanel.updateGameHistory(null);
        }
    }

    public void setEnginePanel(EnginePanel ep) { this.enginePanel = ep; }

    public void setAnalysisPanel(AnalysisPanel ap) { this.analysisPanel = ap; }

    public void setAnalysisResult(AnalysisResult r) {
        this.lastAnalysis = r;
        repaint();
    }

    /** Called after a board reset to let the AI move first if it plays white. */
    public void maybeStartAI() {
        if (ai != null && ai.isAiTurn(gameState) && !isGameOver()) {
            triggerAiMove();
        }
    }

    private void triggerAiMove() {
        aiThinking = true;
        gameStatusLabel.setText(I18n.get("status.ai.thinking"));
        new Thread(() -> {
            Move aiMove = ai.findBestMove(gameState);
            SwingUtilities.invokeLater(() -> {
                boolean wasWhite = gameState.isWhiteTurn();
                if (aiMove != null) {
                    gameState.applyMove(aiMove);
                    updateGameHistory();
                    silentClassify(wasWhite);
                }
                lastAnalysis = null;
                aiThinking = false;
                updateGameStatus();
                repaint();
                maybeAutoAnalyze();
            });
        }, "chess-ai").start();
    }

    private void updateGameHistory() {
        if (analysisPanel != null) {
            analysisPanel.updateGameHistory(gameState.getMoveHistory().toPGN());
            analysisPanel.updateCapturedPieces(gameState);
        }
    }

    private void maybeAutoAnalyze() {
        if (enginePanel != null && enginePanel.isAutoEnabled() && !isGameOver()) {
            enginePanel.triggerAnalysis();
        }
    }

    /**
     * Runs a quick depth-3 evaluation in background after every move,
     * regardless of whether Analyze is active, to feed the move history panel.
     * @param whiteJustMoved true if the move that triggered this was white's
     */
    /**
     * Queues a depth-2 move-quality classification in the single-threaded executor.
     * classifyPrevScore is updated INSIDE the executor thread (no EDT race).
     * Generation counter ensures stale results from old games are discarded.
     */
    private void silentClassify(boolean whiteJustMoved) {
        if (analysisPanel == null) return;
        final int     gen      = classifyGeneration.get();
        final GameState snap   = gameState.cloneState();

        classifyExecutor.submit(() -> {
            // Read and update prevScore only inside this single thread — no races.
            Integer prev    = classifyPrevScore.get();
            int     current = new ChessAI(true).evalPosition(snap, 2);
            classifyPrevScore.set(current);

            int cpLoss = 0;
            AnalysisResult.MoveQuality quality;
            if (prev != null) {
                cpLoss  = whiteJustMoved ? (prev - current) : (current - prev);
                cpLoss  = Math.max(0, cpLoss);
                quality = AnalysisResult.MoveQuality.classify(cpLoss);
            } else {
                quality = AnalysisResult.MoveQuality.BEST; // first move: no baseline
            }

            final int    loss = cpLoss;
            final var    q    = quality;
            SwingUtilities.invokeLater(() -> {
                if (classifyGeneration.get() != gen) return; // discard stale result
                analysisPanel.addMoveQuality(whiteJustMoved, q, loss);
            });
        });
    }

    private boolean isGameOver() {
        boolean whiteTurn = gameState.isWhiteTurn();
        return gameState.isCheckmate(whiteTurn) || gameState.isStalemate(whiteTurn)
                || gameState.isAutomaticDraw();
    }

    @Override
    protected void paintComponent(Graphics graphics) {

        super.paintComponent(graphics);

        drawBoard(graphics);
        drawCoordinates(graphics);
        drawLegalMoves(graphics);
        drawPieces(graphics);
        drawAnalysisArrows(graphics);
        drawMoveQualityBadge(graphics);
    }

    private void drawBoard(Graphics g) {

        for (int row = 0; row < 8; row++) {

            for (int col = 0; col < 8; col++) {

                boolean lightSquare = (row + col) % 2 == 0;

                g.setColor(lightSquare
                        ? new Color(240, 217, 181)
                        : new Color(181, 136, 99));

                g.fillRect(
                        col * TILE_SIZE,
                        row * TILE_SIZE,
                        TILE_SIZE,
                        TILE_SIZE
                );

                if (row == selectedRow && col == selectedColumn) {
                    g.setColor(new Color(255, 255, 0, 120));
                    g.fillRect(
                            vCol(col) * TILE_SIZE,
                            vRow(row) * TILE_SIZE,
                            TILE_SIZE,
                            TILE_SIZE
                    );
                }
            }
        }
    }

    private void drawLegalMoves(Graphics graphics) {

        if (selectedRow == -1) {
            return;
        }

        List<Move> moves =
                gameState.getLegalMovesFrom(
                        selectedRow,
                        selectedColumn
                );

        graphics.setColor(new Color(0, 0, 255, 90));

        for (Move move : moves) {
            graphics.fillOval(
                    vCol(move.toColumn) * TILE_SIZE + 25,
                    vRow(move.toRow)    * TILE_SIZE + 25,
                    30,
                    30
            );
        }
    }

    
    
    private void drawPieces(Graphics g) {

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {

                Piece piece = gameState.getPiece(row, col);

                if (piece != null) {

                	String key = mapPiece(piece);

                    BufferedImage img =
                            PieceImageLoader.get(key);

                    if (img != null) {
                        g.drawImage(
                                img,
                                vCol(col) * TILE_SIZE,
                                vRow(row) * TILE_SIZE,
                                TILE_SIZE,
                                TILE_SIZE,
                                null
                        );
                    }
                }
            }
        }
    }
    
    private String mapPiece(Piece piece) {

        String color = piece.isWhite() ? "w" : "b";

        return switch (piece.getType()) {

            case KING   -> color + "K";
            case QUEEN  -> color + "Q";
            case ROOK   -> color + "R";
            case BISHOP -> color + "B";
            case KNIGHT -> color + "N";
            case PAWN   -> color + "P";
        };
    }
    
    private void drawCoordinates(Graphics g) {
        Font font = new Font("Arial", Font.BOLD, FontScale.scale(14));
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        for (int i = 0; i < 8; i++) {
            // Rank numbers on the left edge: top visual row = rank 8 (normal) or rank 1 (flipped)
            int rankNum = flipped ? (i + 1) : (8 - i);
            String rank = String.valueOf(rankNum);
            boolean lightRank = (i % 2 == 0);
            g.setColor(lightRank ? new Color(181, 136, 99) : new Color(240, 217, 181));
            g.drawString(rank, 3, i * TILE_SIZE + fm.getAscent() + 3);

            // File letters on the bottom edge: left col = 'a' (normal) or 'h' (flipped)
            char fileCh = flipped ? (char)('h' - i) : (char)('a' + i);
            String file = String.valueOf(fileCh);
            boolean lightFile = ((7 + i) % 2 == 0);
            g.setColor(lightFile ? new Color(181, 136, 99) : new Color(240, 217, 181));
            int textX = i * TILE_SIZE + TILE_SIZE - fm.stringWidth(file) - 3;
            g.drawString(file, textX, 8 * TILE_SIZE - 3);
        }
    }

    private void drawAnalysisArrows(Graphics g) {
        if (lastAnalysis == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // PV second move (blue)
        if (lastAnalysis.pv != null && lastAnalysis.pv.size() >= 2) {
            drawArrow(g2, lastAnalysis.pv.get(1), new Color(50, 130, 255, 160));
        }
        // Best move (green, drawn on top)
        if (lastAnalysis.bestMove != null) {
            drawArrow(g2, lastAnalysis.bestMove, new Color(0, 210, 90, 200));
        }
    }

    private void drawArrow(Graphics2D g2, Move m, Color color) {
        int half = TILE_SIZE / 2;
        float x1 = vCol(m.fromColumn) * TILE_SIZE + half;
        float y1 = vRow(m.fromRow)    * TILE_SIZE + half;
        float x2 = vCol(m.toColumn)   * TILE_SIZE + half;
        float y2 = vRow(m.toRow)      * TILE_SIZE + half;

        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;

        float ux = dx / len, uy = dy / len; // unit vector
        float headSize = 22f;
        float shaftEnd = len - headSize;

        // Shaft
        g2.setColor(color);
        g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int) x1, (int) y1,
                (int) (x1 + ux * shaftEnd), (int) (y1 + uy * shaftEnd));

        // Arrowhead
        float ax = x1 + ux * shaftEnd;
        float ay = y1 + uy * shaftEnd;
        float px = -uy, py = ux; // perpendicular
        GeneralPath head = new GeneralPath();
        head.moveTo(x2, y2);
        head.lineTo(ax + px * headSize * 0.5f, ay + py * headSize * 0.5f);
        head.lineTo(ax - px * headSize * 0.5f, ay - py * headSize * 0.5f);
        head.closePath();
        g2.fill(head);
    }

    private void drawMoveQualityBadge(Graphics g) {
        if (lastAnalysis == null || lastAnalysis.quality == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String text = I18n.get("quality." + lastAnalysis.quality.name().toLowerCase());
        if (lastAnalysis.cpLoss > 0)
            text += "  −" + lastAnalysis.cpLoss + " cp";

        Font font = new Font("Arial", Font.BOLD, FontScale.scale(14));
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        int padX = 12, padY = 7;
        int w = fm.stringWidth(text) + padX * 2;
        int h = fm.getHeight() + padY * 2;
        int x = 10;
        int y = 640 - h - 10;

        // Background pill
        Color bg = new Color(
                lastAnalysis.quality.color.getRed(),
                lastAnalysis.quality.color.getGreen(),
                lastAnalysis.quality.color.getBlue(), 210);
        g2.setColor(bg);
        g2.fillRoundRect(x, y, w, h, 14, 14);

        // Text
        g2.setColor(Color.WHITE);
        g2.drawString(text, x + padX, y + padY + fm.getAscent());
    }

    private void updateGameStatus() {

        boolean whiteTurn = gameState.isWhiteTurn();

        if (gameState.isCheckmate(whiteTurn)) {
            String winnerKey = whiteTurn ? "winner.black" : "winner.white";
            String winner = I18n.get(winnerKey);
            gameStatusLabel.setText(I18n.get("status.checkmate", winner));
            JOptionPane.showMessageDialog(this.frame,
                    I18n.get("dialog.checkmate", winner),
                    I18n.get("dialog.gameover"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (gameState.isStalemate(whiteTurn)) {
            gameStatusLabel.setText(I18n.get("status.stalemate"));
            JOptionPane.showMessageDialog(this.frame,
                    I18n.get("dialog.stalemate"),
                    I18n.get("dialog.gameover"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (gameState.isThreefoldRepetition()) {
            gameStatusLabel.setText(I18n.get("status.draw"));
            JOptionPane.showMessageDialog(this.frame,
                    I18n.get("dialog.draw.repetition"),
                    I18n.get("dialog.gameover"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (gameState.isFiftyMoveRule()) {
            gameStatusLabel.setText(I18n.get("status.draw"));
            JOptionPane.showMessageDialog(this.frame,
                    I18n.get("dialog.draw.fiftymove"),
                    I18n.get("dialog.gameover"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (gameState.isInsufficientMaterial()) {
            gameStatusLabel.setText(I18n.get("status.draw"));
            JOptionPane.showMessageDialog(this.frame,
                    I18n.get("dialog.draw.material"),
                    I18n.get("dialog.gameover"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (gameState.isKingInCheck(whiteTurn)) {
            gameStatusLabel.setText(I18n.get(whiteTurn ? "status.white.check" : "status.black.check"));
            return;
        }

        gameStatusLabel.setText(I18n.get(whiteTurn ? "status.white.move" : "status.black.move"));
    }
}
