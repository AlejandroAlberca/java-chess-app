package com.devmanchego.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure-Java chess AI.
 *
 * Search:     iterative-deepening negamax, alpha-beta pruning, quiescence search
 * TT:         Zobrist-hashed transposition table (1M entries)
 * Evaluation: material + piece-square tables + pawn structure + king safety
 *             + rook on open files + bishop pair + game-phase interpolation
 */
public class ChessAI {

    // ── Material values (centipawns) ──────────────────────────────────────────
    private static final int VAL_KING   = 20000;
    private static final int VAL_QUEEN  =   900;
    private static final int VAL_ROOK   =   500;
    private static final int VAL_BISHOP =   330;
    private static final int VAL_KNIGHT =   320;
    private static final int VAL_PAWN   =   100;

    // ── Piece-square tables (white's perspective, row 0 = rank 8) ─────────────
    private static final int[] PST_PAWN = {
         0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    };
    private static final int[] PST_KNIGHT = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };
    private static final int[] PST_BISHOP = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };
    private static final int[] PST_ROOK = {
         0,  0,  0,  0,  0,  0,  0,  0,
         5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
         0,  0,  0,  5,  5,  0,  0,  0
    };
    private static final int[] PST_QUEEN = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    };
    // Middlegame king: stay castled
    private static final int[] PST_KING_MID = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    };
    // Endgame king: centralise
    private static final int[] PST_KING_END = {
        -50,-40,-30,-20,-20,-30,-40,-50,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -50,-30,-30,-30,-30,-30,-30,-50
    };

    // ── Difficulty ────────────────────────────────────────────────────────────
    public enum Difficulty {
        BEGINNER(1, 0.40),
        EASY    (2, 0.20),
        MEDIUM  (4, 0.05),
        HARD    (6, 0.00),
        EXPERT  (8, 0.00);

        public final int maxDepth;
        public final double errorRate; // probability of picking a random legal move
        Difficulty(int d, double e) { maxDepth = d; errorRate = e; }
    }

    // ── Search constants ──────────────────────────────────────────────────────
    private static final long TIME_LIMIT     = 2_500;
    private static final int  MATE_SCORE     = 29000;
    private static final int  MATE_THRESHOLD = MATE_SCORE - 100; // scores beyond this are "mate in N"
    private static final int  INF            = Integer.MAX_VALUE / 2;

    // ── Instance state ────────────────────────────────────────────────────────
    private final boolean aiIsWhite;
    private volatile boolean stopSearch;
    private final TranspositionTable tt = new TranspositionTable();
    private Difficulty difficulty = Difficulty.HARD;

    // ── Move-ordering heuristics (search-scoped: reset at the start of every
    //    top-level search so ply-indexed entries never refer to a stale tree) ──
    private static final int MAX_KILLER_PLY = 64;
    private final Move[][] killerMoves  = new Move[MAX_KILLER_PLY][2];
    private final int[][]  historyTable = new int[64][64];

    public ChessAI(boolean aiIsWhite) { this.aiIsWhite = aiIsWhite; }

    public void setDifficulty(Difficulty d) { this.difficulty = d; }
    public Difficulty getDifficulty()        { return difficulty; }

    public boolean isAiTurn(GameState state) {
        return state.isWhiteTurn() == aiIsWhite;
    }

    public boolean isWhite() { return aiIsWhite; }

    public void clearTT() { tt.clear(); }

    /** Killer moves and history scores are only meaningful within a single search tree. */
    private void resetOrderingHeuristics() {
        for (Move[] km : killerMoves) { km[0] = null; km[1] = null; }
        for (int[] row : historyTable) Arrays.fill(row, 0);
    }

    // =========================================================================
    // SEARCH ENTRY POINTS
    // =========================================================================

    /** Used by game AI to pick the best move to play. */
    public Move findBestMove(GameState state) {
        Move bookMove = tryBookMove(state);
        if (bookMove != null) return bookMove;

        // At error-prone levels, occasionally play a random legal move
        if (difficulty.errorRate > 0 && Math.random() < difficulty.errorRate) {
            List<Move> legal = new ArrayList<>();
            for (int r = 0; r < 8; r++)
                for (int c = 0; c < 8; c++) {
                    Piece p = state.getPiece(r, c);
                    if (p != null && p.isWhite() == aiIsWhite)
                        legal.addAll(state.getLegalMovesFrom(r, c));
                }
            if (!legal.isEmpty())
                return legal.get((int)(Math.random() * legal.size()));
        }
        stopSearch = false;
        resetOrderingHeuristics();
        long deadline = System.currentTimeMillis() + TIME_LIMIT;
        Move best = null;
        for (int depth = 1; depth <= difficulty.maxDepth; depth++) {
            SearchResult sr = searchRoot(state, depth, deadline, null);
            if (stopSearch) break;
            if (sr != null) best = sr.move;
        }
        return best;
    }

    // =========================================================================
    // OPENING BOOK
    // =========================================================================

    // A handful of well-known main lines, keyed by the space-joined UCI moves
    // played so far. Each key maps to the possible book replies from that
    // position; when several lines share a prefix, one reply is picked at
    // random. Deliberately small and low-risk: if the played game ever
    // deviates from these lines, lookups simply miss and the normal search
    // takes over — this can never produce an illegal or unintended move
    // because every candidate is re-validated against the current legal
    // moves before being played.
    private static final String[] OPENING_BOOK_LINES = {
        "e2e4 e7e5 g1f3 b8c6 f1b5",               // Ruy Lopez
        "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5",           // Italian Game
        "e2e4 e7e5 g1f3 g8f6",                     // Petrov Defence
        "e2e4 c7c5 g1f3 d7d6",                     // Sicilian, Najdorf-ish
        "e2e4 c7c5 g1f3 b8c6",                     // Sicilian, Old Sicilian
        "e2e4 e7e6 d2d4 d7d5",                     // French Defence
        "e2e4 c7c6 d2d4 d7d5",                     // Caro-Kann
        "d2d4 d7d5 c2c4 e7e6",                     // Queen's Gambit Declined
        "d2d4 d7d5 c2c4 c7c6",                     // Slav Defence
        "d2d4 g8f6 c2c4 e7e6 b1c3 f8b4",           // Nimzo-Indian
        "d2d4 g8f6 c2c4 g7g6",                     // King's Indian / Gruenfeld family
        "g1f3 d7d5 c2c4 c7c6",                     // Reti Opening
        "e2e4 g7g6",                                // Modern Defence
    };

    private static final Map<String, List<String>> OPENING_BOOK = buildOpeningBook();

    private static Map<String, List<String>> buildOpeningBook() {
        Map<String, List<String>> book = new HashMap<>();
        for (String line : OPENING_BOOK_LINES) {
            String[] moves = line.split(" ");
            StringBuilder prefix = new StringBuilder();
            for (int i = 0; i < moves.length; i++) {
                book.computeIfAbsent(prefix.toString(), k -> new ArrayList<>()).add(moves[i]);
                if (prefix.length() > 0) prefix.append(' ');
                prefix.append(moves[i]);
            }
        }
        return book;
    }

    /** Returns a book move for the current position, or null if out of book. */
    private Move tryBookMove(GameState state) {
        String key = String.join(" ", state.getMoveHistory().getMoves());
        List<String> candidates = OPENING_BOOK.get(key);
        if (candidates == null || candidates.isEmpty()) return null;

        String chosenUci = candidates.get((int) (Math.random() * candidates.size()));
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = state.getPiece(r, c);
                if (p == null || p.isWhite() != aiIsWhite) continue;
                for (Move m : state.getLegalMovesFrom(r, c))
                    if (m.toUCI().equals(chosenUci)) return m;
            }
        return null; // book move wasn't legal here (shouldn't happen) — fall back to search
    }

    /** Full analysis: returns depth, score, best move, PV, MultiPV, quality. */
    public AnalysisResult analyze(GameState state, int multiPVCount, Integer prevScoreWhite) {
        stopSearch = false;
        resetOrderingHeuristics();
        long deadline = System.currentTimeMillis() + TIME_LIMIT;
        SearchResult best = null;
        int depthReached  = 0;
        for (int depth = 1; depth <= 6; depth++) {
            SearchResult sr = searchRoot(state, depth, deadline, null);
            if (stopSearch) break;
            if (sr != null) { best = sr; depthReached = depth; }
        }
        if (best == null)
            return new AnalysisResult(0, 0, null, List.of(), List.of(), null, 0);

        int scoreWhite = state.isWhiteTurn() ? best.score : -best.score;
        List<Move> pv  = extractPV(state, best.move, depthReached);
        List<AnalysisResult.TopMove> top = findTopMoves(state, multiPVCount, depthReached, deadline);

        AnalysisResult.MoveQuality quality = null;
        int cpLoss = 0;
        if (prevScoreWhite != null) {
            boolean lastWasWhite = !state.isWhiteTurn();
            cpLoss = lastWasWhite ? (prevScoreWhite - scoreWhite) : (scoreWhite - prevScoreWhite);
            cpLoss = Math.max(0, cpLoss);
            quality = AnalysisResult.MoveQuality.classify(cpLoss);
        }
        return new AnalysisResult(depthReached, scoreWhite, best.move, pv, top, quality, cpLoss);
    }

    /** Quick depth-5 classification (~800ms). */
    public AnalysisResult quickClassify(GameState state, Integer prevScoreWhite) {
        stopSearch = false;
        resetOrderingHeuristics();
        long deadline = System.currentTimeMillis() + 300; // fast: only for move classification
        SearchResult best = null;
        int depthReached  = 0;
        for (int depth = 1; depth <= 3; depth++) {
            SearchResult sr = searchRoot(state, depth, deadline, null);
            if (stopSearch) break;
            if (sr != null) { best = sr; depthReached = depth; }
        }
        if (best == null)
            return new AnalysisResult(0, 0, null, List.of(), List.of(), null, 0);

        int scoreWhite = state.isWhiteTurn() ? best.score : -best.score;
        int cpLoss = 0;
        AnalysisResult.MoveQuality quality;
        if (prevScoreWhite != null) {
            boolean lastWasWhite = !state.isWhiteTurn();
            cpLoss = lastWasWhite ? (prevScoreWhite - scoreWhite) : (scoreWhite - prevScoreWhite);
            cpLoss = Math.max(0, cpLoss);
            quality = AnalysisResult.MoveQuality.classify(cpLoss);
        } else {
            // No baseline yet (very first move) — classify as BEST (no penalty possible).
            quality = AnalysisResult.MoveQuality.BEST;
        }
        return new AnalysisResult(depthReached, scoreWhite, best.move, List.of(), List.of(), quality, cpLoss);
    }

    /**
     * Runs a depth-limited iterative-deepening search and returns the score
     * from White's perspective. Falls back to static eval if search fails.
     * Used by the move-quality classifier.
     */
    public int evalPosition(GameState state, int maxDepth) {
        stopSearch = false;
        resetOrderingHeuristics();
        long deadline = System.currentTimeMillis() + 400;
        SearchResult best = null;
        for (int depth = 1; depth <= maxDepth; depth++) {
            SearchResult sr = searchRoot(state, depth, deadline, null);
            if (stopSearch) break;
            if (sr != null) best = sr;
        }
        if (best == null) return staticEvalFromWhite(state);
        return state.isWhiteTurn() ? best.score : -best.score;
    }

    /** Always-from-white evaluation (for draw offer / external use). */
    public int staticEvalFromWhite(GameState state) {
        int score = 0;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = state.getPiece(r, c);
                if (p == null) continue;
                int v = material(p) + positional(p, r, c);
                if (p.isWhite()) score += v; else score -= v;
            }
        score += advancedEval(state);
        return score;
    }

    // =========================================================================
    // INTERNAL SEARCH
    // =========================================================================

    private record SearchResult(Move move, int score) {}

    private SearchResult searchRoot(GameState state, int depth, long deadline,
                                    Set<String> excluded) {
        boolean side = state.isWhiteTurn();
        List<Move> moves = allLegalMoves(state, side);
        if (moves.isEmpty()) return null;
        if (excluded != null && !excluded.isEmpty())
            moves.removeIf(m -> excluded.contains(m.toUCI()));
        if (moves.isEmpty()) return null;

        orderMovesForSearch(moves, state, state.getZobristHash(), 0);

        int bestScore = -INF;
        Move bestMove = moves.get(0);

        for (Move move : moves) {
            if (System.currentTimeMillis() >= deadline) { stopSearch = true; break; }
            GameState child = state.cloneState();
            child.applyMove(move);
            int score = -negamax(child, depth - 1, -INF, -bestScore, deadline, 1);
            if (score > bestScore) { bestScore = score; bestMove = move; }
        }
        return new SearchResult(bestMove, bestScore);
    }

    /**
     * @param ply distance (in plies) from the root of the current search —
     *            used to convert between root-relative mate scores (needed so the
     *            engine prefers faster mates) and ply-independent mate scores
     *            (needed so the same TT entry is valid at any depth in the tree).
     */
    private int negamax(GameState state, int depth, int alpha, int beta, long deadline, int ply) {
        if (stopSearch || System.currentTimeMillis() >= deadline) {
            stopSearch = true; return 0;
        }

        // ── Transposition table lookup ────────────────────────────────────────
        long hash = state.getZobristHash();
        int alphaOrig = alpha;
        TranspositionTable.Entry tte = tt.probe(hash);
        if (tte != null && tte.depth() >= depth) {
            int ttScore = fromTTScore(tte.score(), ply);
            switch (tte.flag()) {
                case TranspositionTable.EXACT -> { return ttScore; }
                case TranspositionTable.LOWER -> alpha = Math.max(alpha, ttScore);
                case TranspositionTable.UPPER -> beta  = Math.min(beta,  ttScore);
            }
            if (alpha >= beta) return ttScore;
        }

        boolean side = state.isWhiteTurn();
        // isCheckmate() would re-generate the same legal-move list internally
        // (it's the single most expensive call in the search); generate once
        // here and use emptiness + check status to distinguish mate/stalemate.
        List<Move> moves = allLegalMoves(state, side);
        if (moves.isEmpty())
            return state.isKingInCheck(side) ? -(MATE_SCORE - ply) : 0;

        if (depth == 0) return quiescence(state, alpha, beta);

        orderMovesForSearch(moves, state, hash, ply);

        int bestScore = -INF;
        Move bestMove = moves.get(0);

        for (Move move : moves) {
            GameState child = state.cloneState();
            child.applyMove(move);
            int score = -negamax(child, depth - 1, -beta, -alpha, deadline, ply + 1);
            if (score > bestScore) { bestScore = score; bestMove = move; }
            if (score > alpha) alpha = score;
            if (alpha >= beta) {
                recordCutoff(move, state, depth, ply);
                break;  // beta cut-off
            }
        }

        // ── Store in TT ───────────────────────────────────────────────────────
        if (!stopSearch) {
            byte flag = bestScore <= alphaOrig ? TranspositionTable.UPPER
                      : bestScore >= beta       ? TranspositionTable.LOWER
                      :                           TranspositionTable.EXACT;
            tt.store(hash, depth, toTTScore(bestScore, ply), flag, bestMove);
        }
        return bestScore;
    }

    /** Converts a root-relative mate score to a ply-independent one before storing in the TT. */
    private int toTTScore(int score, int ply) {
        if (score > MATE_THRESHOLD)  return score + ply;
        if (score < -MATE_THRESHOLD) return score - ply;
        return score;
    }

    /** Converts a ply-independent TT mate score back to root-relative for use at the current ply. */
    private int fromTTScore(int score, int ply) {
        if (score > MATE_THRESHOLD)  return score - ply;
        if (score < -MATE_THRESHOLD) return score + ply;
        return score;
    }

    private int quiescence(GameState state, int alpha, int beta) {
        int standPat = evaluate(state);
        if (standPat >= beta) return beta;
        if (standPat > alpha) alpha = standPat;

        boolean side = state.isWhiteTurn();
        List<Move> captures = captureMoves(state, side);
        orderMoves(captures, state);

        for (Move move : captures) {
            GameState child = state.cloneState();
            child.applyMove(move);
            int score = -quiescence(child, -beta, -alpha);
            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        return alpha;
    }

    // =========================================================================
    // EVALUATION
    // =========================================================================

    /** Score from current-side-to-move perspective (negamax convention). */
    private int evaluate(GameState state) {
        boolean side = state.isWhiteTurn();
        int score = 0;

        int phase = gamePhase(state);

        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = state.getPiece(r, c);
                if (p == null) continue;
                int v = material(p) + positional(p, r, c, phase);
                if (p.isWhite() == side) score += v; else score -= v;
            }

        // Advanced terms (always from white's perspective, then flip)
        int adv = advancedEval(state);
        score += side ? adv : -adv;

        return score;
    }

    /** Advanced positional terms — returns score from white's perspective. */
    private int advancedEval(GameState state) {
        int score = 0;
        int phase = gamePhase(state); // 0=endgame … 256=opening

        // ── Collect pawn data ─────────────────────────────────────────────────
        int[] wp = new int[8]; // white pawn count per file
        int[] bp = new int[8]; // black pawn count per file
        int[] wAdvRow = new int[8]; // most advanced white pawn row (smallest = closer to rank 8)
        int[] bAdvRow = new int[8]; // most advanced black pawn row (largest = closer to rank 1)
        Arrays.fill(wAdvRow, 7);
        Arrays.fill(bAdvRow, 0);

        int wBishops = 0, bBishops = 0;
        int wKr = 7, wKc = 4, bKr = 0, bKc = 4;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = state.getPiece(r, c);
                if (p == null) continue;
                switch (p.getType()) {
                    case PAWN -> {
                        if (p.isWhite()) { wp[c]++; if (r < wAdvRow[c]) wAdvRow[c] = r; }
                        else             { bp[c]++; if (r > bAdvRow[c]) bAdvRow[c] = r; }
                    }
                    case BISHOP -> { if (p.isWhite()) wBishops++; else bBishops++; }
                    case KING   -> { if (p.isWhite()) { wKr = r; wKc = c; }
                                     else             { bKr = r; bKc = c; } }
                    default -> {}
                }
            }
        }

        // ── Doubled pawns (-15 cp per extra pawn on same file) ────────────────
        for (int c = 0; c < 8; c++) {
            if (wp[c] > 1) score -= 15 * (wp[c] - 1);
            if (bp[c] > 1) score += 15 * (bp[c] - 1);
        }

        // ── Isolated pawns (-20 cp each) ──────────────────────────────────────
        for (int c = 0; c < 8; c++) {
            boolean lw = c == 0 || wp[c-1] == 0, rw = c == 7 || wp[c+1] == 0;
            if (wp[c] > 0 && lw && rw) score -= 20;
            boolean lb = c == 0 || bp[c-1] == 0, rb = c == 7 || bp[c+1] == 0;
            if (bp[c] > 0 && lb && rb) score += 20;
        }

        // ── Passed pawns (+10 to +50 cp, more when advanced) ─────────────────
        for (int c = 0; c < 8; c++) {
            if (wp[c] > 0) {
                boolean passed = true;
                for (int fc = Math.max(0, c-1); fc <= Math.min(7, c+1) && passed; fc++)
                    if (bp[fc] > 0 && bAdvRow[fc] > wAdvRow[c]) passed = false;
                if (passed) { int adv = 7 - wAdvRow[c]; score += 10 + adv * 8; }
            }
            if (bp[c] > 0) {
                boolean passed = true;
                for (int fc = Math.max(0, c-1); fc <= Math.min(7, c+1) && passed; fc++)
                    if (wp[fc] > 0 && wAdvRow[fc] < bAdvRow[c]) passed = false;
                if (passed) { int adv = bAdvRow[c]; score -= 10 + adv * 8; }
            }
        }

        // ── Bishop pair (+30 cp) ──────────────────────────────────────────────
        if (wBishops >= 2) score += 30;
        if (bBishops >= 2) score -= 30;

        // ── Rooks on open / semi-open files ───────────────────────────────────
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = state.getPiece(r, c);
                if (p == null || p.getType() != PieceType.ROOK) continue;
                boolean open = (wp[c] == 0 && bp[c] == 0);
                boolean semi = p.isWhite() ? wp[c] == 0 : bp[c] == 0;
                int bonus = open ? 20 : semi ? 10 : 0;
                if (p.isWhite()) score += bonus; else score -= bonus;
            }
        }

        // ── King safety (middlegame only, scaled by phase) ────────────────────
        if (phase > 30) {
            int wSafety = kingSafety(state, wKr, wKc, true,  wp, bp);
            int bSafety = kingSafety(state, bKr, bKc, false, bp, wp);
            score += (wSafety - bSafety) * phase / 256;
        }

        // ── King centralisation in endgame ────────────────────────────────────
        if (phase < 150) {
            int wCen = centralityBonus(wKr, wKc);
            int bCen = centralityBonus(bKr, bKc);
            score += (wCen - bCen) * (150 - phase) / 150;
        }

        return score;
    }

    private int kingSafety(GameState state, int kr, int kc,
                            boolean white, int[] ownPawns, int[] enemyPawns) {
        int bonus = 0;
        int dir = white ? -1 : 1; // direction toward enemy

        for (int dc = -1; dc <= 1; dc++) {
            int fc = kc + dc;
            if (fc < 0 || fc > 7) continue;

            boolean shielded = false;
            for (int dr = 1; dr <= 2; dr++) {
                int fr = kr + dir * dr;
                if (fr < 0 || fr >= 8) continue;
                Piece p = state.getPiece(fr, fc);
                if (p != null && p.getType() == PieceType.PAWN && p.isWhite() == white) {
                    bonus += dr == 1 ? 15 : 5;
                    shielded = true;
                    break;
                }
            }
            if (!shielded) bonus -= 20;

            if (ownPawns[fc] == 0)
                bonus -= enemyPawns[fc] == 0 ? 30 : 15; // open / semi-open file
        }
        return bonus;
    }

    private int centralityBonus(int r, int c) {
        // Closer to d4/e4 area = higher score (0–14)
        int dr = Math.abs(r - 3), dc = Math.abs(c - 3);
        return 14 - dr * 2 - dc * 2;
    }

    /** Returns game phase: 256 = full material, 0 = pure endgame. */
    private int gamePhase(GameState state) {
        int mat = 0;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = state.getPiece(r, c);
                if (p == null) continue;
                mat += switch (p.getType()) {
                    case QUEEN  -> 4; case ROOK -> 2;
                    case BISHOP, KNIGHT -> 1; default -> 0;
                };
            }
        return Math.min(256, mat * 256 / 24); // max material = 24 units
    }

    // =========================================================================
    // MATERIAL / PST
    // =========================================================================

    private int material(Piece p) {
        return switch (p.getType()) {
            case KING   -> VAL_KING;   case QUEEN  -> VAL_QUEEN;
            case ROOK   -> VAL_ROOK;   case BISHOP -> VAL_BISHOP;
            case KNIGHT -> VAL_KNIGHT; case PAWN   -> VAL_PAWN;
        };
    }

    private int positional(Piece p, int row, int col, int phase) {
        int idx = p.isWhite() ? row * 8 + col : (7 - row) * 8 + col;
        return switch (p.getType()) {
            case PAWN   -> PST_PAWN[idx];
            case KNIGHT -> PST_KNIGHT[idx];
            case BISHOP -> PST_BISHOP[idx];
            case ROOK   -> PST_ROOK[idx];
            case QUEEN  -> PST_QUEEN[idx];
            case KING   -> {
                // Interpolate between middlegame and endgame king tables
                int mid = PST_KING_MID[idx];
                int end = PST_KING_END[idx];
                yield (mid * phase + end * (256 - phase)) / 256;
            }
        };
    }

    // Legacy overload without phase (used by staticEvalFromWhite)
    private int positional(Piece p, int row, int col) {
        return positional(p, row, col, 200);
    }

    // =========================================================================
    // MOVE HELPERS
    // =========================================================================

    private List<Move> allLegalMoves(GameState state, boolean white) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = state.getPiece(r, c);
                if (p != null && p.isWhite() == white)
                    moves.addAll(state.getLegalMovesFrom(r, c));
            }
        return moves;
    }

    private List<Move> captureMoves(GameState state, boolean white) {
        List<Move> caps = new ArrayList<>();
        for (Move m : allLegalMoves(state, white))
            // En passant captures leave the destination square empty, so they
            // must be detected via the flag rather than an occupied target.
            if (state.getPiece(m.toRow, m.toColumn) != null || m.enPassantCapture) caps.add(m);
        return caps;
    }

    // MVV-LVA: most-valuable victim / least-valuable attacker first
    private void orderMoves(List<Move> moves, GameState state) {
        moves.sort((a, b) -> mvvLva(b, state) - mvvLva(a, state));
    }

    private int mvvLva(Move m, GameState state) {
        Piece victim = state.getPiece(m.toRow, m.toColumn);
        if (victim == null) return 0;
        Piece attacker = state.getPiece(m.fromRow, m.fromColumn);
        return material(victim) * 10 - (attacker == null ? 0 : material(attacker));
    }

    /**
     * Full move ordering for the main search (as opposed to quiescence's plain
     * MVV-LVA): TT best move first, then captures by MVV-LVA, then killer
     * moves for this ply, then quiet moves by history-heuristic score.
     */
    private void orderMovesForSearch(List<Move> moves, GameState state, long hash, int ply) {
        TranspositionTable.Entry tte = tt.probe(hash);
        Move ttBest = (tte != null) ? tte.bestMove() : null;
        moves.sort((a, b) -> moveOrderScore(b, state, ttBest, ply) - moveOrderScore(a, state, ttBest, ply));
    }

    private int moveOrderScore(Move m, GameState state, Move ttBest, int ply) {
        if (ttBest != null && sameSquares(m, ttBest)) return 2_000_000;

        Piece victim = state.getPiece(m.toRow, m.toColumn);
        if (victim != null) {
            Piece attacker = state.getPiece(m.fromRow, m.fromColumn);
            return 1_000_000 + material(victim) * 10 - (attacker == null ? 0 : material(attacker));
        }

        if (ply < killerMoves.length) {
            Move[] killers = killerMoves[ply];
            if (killers[0] != null && sameSquares(m, killers[0])) return 900_000;
            if (killers[1] != null && sameSquares(m, killers[1])) return 800_000;
        }

        return historyTable[m.fromRow * 8 + m.fromColumn][m.toRow * 8 + m.toColumn];
    }

    private boolean sameSquares(Move a, Move b) {
        return a.fromRow == b.fromRow && a.fromColumn == b.fromColumn
                && a.toRow == b.toRow && a.toColumn == b.toColumn;
    }

    /** Records a killer move / history bonus when a quiet move causes a beta cutoff. */
    private void recordCutoff(Move move, GameState state, int depth, int ply) {
        if (state.getPiece(move.toRow, move.toColumn) != null) return; // only quiet moves
        if (ply < killerMoves.length) {
            Move[] killers = killerMoves[ply];
            if (killers[0] == null || !sameSquares(move, killers[0])) {
                killers[1] = killers[0];
                killers[0] = move;
            }
        }
        historyTable[move.fromRow * 8 + move.fromColumn][move.toRow * 8 + move.toColumn] += depth * depth;
    }

    // =========================================================================
    // PV / MULTI-PV
    // =========================================================================

    private List<Move> extractPV(GameState root, Move firstMove, int maxDepth) {
        List<Move> pv = new ArrayList<>();
        if (firstMove == null) return pv;
        GameState cur = root.cloneState();
        Move next = firstMove;
        for (int d = 0; d < maxDepth && next != null; d++) {
            pv.add(next);
            cur.applyMove(next);
            if (cur.isCheckmate(cur.isWhiteTurn())) break;
            SearchResult resp = searchRoot(cur, 1, System.currentTimeMillis() + 500, null);
            next = resp != null ? resp.move : null;
        }
        return pv;
    }

    private List<AnalysisResult.TopMove> findTopMoves(GameState state, int n,
                                                       int depth, long deadline) {
        List<AnalysisResult.TopMove> result = new ArrayList<>();
        Set<String> excluded = new HashSet<>();
        for (int i = 0; i < n; i++) {
            SearchResult sr = searchRoot(state, depth, deadline, excluded);
            if (sr == null || sr.move() == null) break;
            int scoreWhite = state.isWhiteTurn() ? sr.score() : -sr.score();
            result.add(new AnalysisResult.TopMove(sr.move(), scoreWhite));
            excluded.add(sr.move().toUCI());
        }
        return result;
    }
}
