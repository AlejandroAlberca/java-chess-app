package com.devmanchego.engine;

import java.util.Arrays;

/**
 * Fixed-size transposition table using Zobrist hashes.
 * Replacement strategy: overwrite if new entry has equal or greater depth.
 *
 * Memory: 1M entries × ~21 bytes ≈ 21 MB
 */
public final class TranspositionTable {

    public static final byte EXACT = 0;
    public static final byte LOWER = 1;   // score is a lower bound (beta cutoff)
    public static final byte UPPER = 2;   // score is an upper bound (all moves failed low)

    private static final int SIZE = 1 << 20; // 1 048 576 entries
    private static final int MASK = SIZE - 1;

    // Parallel arrays — avoids object overhead
    private final long[] hashes = new long[SIZE];
    private final int[]  depths = new int[SIZE];
    private final int[]  scores = new int[SIZE];
    private final byte[] flags  = new byte[SIZE];
    private final int[]  moves  = new int[SIZE]; // packed move (fromRow|fromCol|toRow|toCol)

    // ── API ──────────────────────────────────────────────────────────────────

    public void store(long hash, int depth, int score, byte flag, Move bestMove) {
        int idx = index(hash);
        if (depths[idx] > depth) return; // keep deeper entry
        hashes[idx] = hash;
        depths[idx] = depth;
        scores[idx] = score;
        flags[idx]  = flag;
        moves[idx]  = bestMove != null ? pack(bestMove) : 0;
    }

    /** Returns null on miss or hash collision. */
    public Entry probe(long hash) {
        int idx = index(hash);
        if (hashes[idx] != hash) return null;
        return new Entry(depths[idx], scores[idx], flags[idx], unpack(moves[idx]));
    }

    public void clear() {
        Arrays.fill(hashes, 0L);
        Arrays.fill(depths, 0);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int index(long hash) { return (int)(hash & MASK); }

    private int pack(Move m) {
        return (m.fromRow << 12) | (m.fromColumn << 8) | (m.toRow << 4) | m.toColumn;
    }

    private Move unpack(int packed) {
        if (packed == 0) return null;
        return new Move(
                (packed >> 12) & 0xF,
                (packed >> 8)  & 0xF,
                (packed >> 4)  & 0xF,
                 packed        & 0xF);
    }

    // ── Result record ─────────────────────────────────────────────────────────

    public record Entry(int depth, int score, byte flag, Move bestMove) {}
}
