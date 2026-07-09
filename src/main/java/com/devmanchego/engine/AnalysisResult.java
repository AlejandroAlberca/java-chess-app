package com.devmanchego.engine;

import java.util.List;

public class AnalysisResult {

    public final int        depth;
    public final int        scoreCP;      // centipawns, positive = white is better
    public final Move       bestMove;
    public final List<Move> pv;           // principal variation
    public final List<TopMove> topMoves;  // MultiPV
    public final MoveQuality quality;     // nullable — quality of last move played
    public final int        cpLoss;       // centipawn loss of last move (0 if unknown)

    public AnalysisResult(int depth, int scoreCP, Move bestMove,
                          List<Move> pv, List<TopMove> topMoves,
                          MoveQuality quality, int cpLoss) {
        this.depth     = depth;
        this.scoreCP   = scoreCP;
        this.bestMove  = bestMove;
        this.pv        = pv;
        this.topMoves  = topMoves;
        this.quality   = quality;
        this.cpLoss    = cpLoss;
    }

    // -------------------------------------------------------------------------

    public static class TopMove {
        public final Move move;
        public final int  scoreCP;
        public TopMove(Move move, int scoreCP) {
            this.move    = move;
            this.scoreCP = scoreCP;
        }
    }

    public enum MoveQuality {
        BEST      ("✓ Best Move",   new java.awt.Color(  0, 180,  80)),
        EXCELLENT ("✓ Excellent",   new java.awt.Color(  0, 180,  80)),
        GOOD      ("✓ Good",        new java.awt.Color( 80, 160, 255)),
        INACCURACY("⚠ Inaccuracy",  new java.awt.Color(255, 200,   0)),
        MISTAKE   ("⚠ Mistake",     new java.awt.Color(255, 140,   0)),
        BLUNDER   ("❌ Blunder",     new java.awt.Color(220,  50,  50));

        public final String           label;
        public final java.awt.Color   color;

        MoveQuality(String label, java.awt.Color color) {
            this.label = label;
            this.color = color;
        }

        /** cpLoss is always ≥ 0, measured from the side that just moved. */
        public static MoveQuality classify(int cpLoss) {
            if (cpLoss <=   5) return BEST;
            if (cpLoss <=  20) return EXCELLENT;
            if (cpLoss <=  50) return GOOD;
            if (cpLoss <= 100) return INACCURACY;
            if (cpLoss <= 200) return MISTAKE;
            return BLUNDER;
        }
    }

    /** Human-readable score string: "+0.85", "-1.20", "#3", etc. */
    public String scoreLabel() {
        int abs = Math.abs(scoreCP);
        if (abs >= 28000) {
            int mateIn = (30000 - abs + 1) / 2;
            return (scoreCP > 0 ? "#" : "-#") + mateIn;
        }
        double pawns = scoreCP / 100.0;
        return String.format("%+.2f", pawns);
    }
}
