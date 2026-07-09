package com.devmanchego.engine;

public class Piece {

    private final PieceType type;
    private final boolean white;

    public Piece(PieceType type, boolean white) {
        this.type = type;
        this.white = white;
    }

    public PieceType getType() {
        return type;
    }

    public boolean isWhite() {
        return white;
    }

    @Override
    public String toString() {

        return switch (type) {
            case KING -> white ? "K" : "k";
            case QUEEN -> white ? "Q" : "q";
            case ROOK -> white ? "R" : "r";
            case BISHOP -> white ? "B" : "b";
            case KNIGHT -> white ? "N" : "n";
            case PAWN -> white ? "P" : "p";
        };
    }
    
}
