package com.devmanchego.engine;

import java.util.Random;

/**
 * Computes Zobrist hashes for a board position.
 * Keys are generated once with a fixed seed so they are reproducible.
 *
 * Includes piece placement, side to move, castling rights, and the en-passant
 * target file — two positions differing only in castling rights or en-passant
 * availability must not collide in the transposition table.
 *
 * {@link #compute} does a full O(64) recomputation and is only used when a
 * position is set up from scratch. During search, {@link GameState#applyMove}
 * maintains the hash incrementally via the package-private key accessors below.
 */
public final class ZobristHasher {

    // 12 piece indices (6 types × 2 colours) × 64 squares
    private static final long[][] PIECE_KEYS  = new long[12][64];
    private static final long     SIDE_KEY;           // XOR when black to move
    private static final long[]   CASTLE_KEYS = new long[4]; // wK-side, wQ-side, bK-side, bQ-side
    private static final long[]   EP_FILE_KEYS = new long[8]; // en-passant target file

    static {
        Random rng = new Random(0xCAFEBABEDEADL);
        for (int p = 0; p < 12; p++)
            for (int sq = 0; sq < 64; sq++)
                PIECE_KEYS[p][sq] = rng.nextLong();
        SIDE_KEY = rng.nextLong();
        for (int i = 0; i < 4; i++) CASTLE_KEYS[i] = rng.nextLong();
        for (int i = 0; i < 8; i++) EP_FILE_KEYS[i] = rng.nextLong();
    }

    private ZobristHasher() {}

    /** Full recomputation from scratch — used only when a position is set up. */
    public static long compute(GameState state) {
        long hash = 0L;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = state.getPiece(r, c);
                if (p != null)
                    hash ^= PIECE_KEYS[pieceIndex(p)][r * 8 + c];
            }
        if (!state.isWhiteTurn()) hash ^= SIDE_KEY;

        int castleMask = state.getCastlingRightsMask();
        for (int i = 0; i < 4; i++)
            if ((castleMask & (1 << i)) != 0) hash ^= CASTLE_KEYS[i];

        int epCol = state.getEnPassantTargetCol();
        if (epCol >= 0) hash ^= EP_FILE_KEYS[epCol];

        return hash;
    }

    // ── Package-private accessors for incremental updates in GameState ────────

    static long pieceKey(Piece p, int square) { return PIECE_KEYS[pieceIndex(p)][square]; }
    static long sideKey()                     { return SIDE_KEY; }
    static long castleKey(int i)               { return CASTLE_KEYS[i]; }
    static long epFileKey(int col)              { return EP_FILE_KEYS[col]; }

    private static int pieceIndex(Piece p) {
        int t = switch (p.getType()) {
            case KING   -> 0; case QUEEN  -> 1; case ROOK   -> 2;
            case BISHOP -> 3; case KNIGHT -> 4; case PAWN   -> 5;
        };
        return t + (p.isWhite() ? 0 : 6);
    }
}
