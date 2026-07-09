package com.devmanchego.engine;

public class Move {

    public int fromRow;
    public int fromColumn;
    public int toRow;
    public int toColumn;

    public boolean castlingMove;
    public boolean enPassantCapture;

    /** Piece to promote to when this move reaches the last rank; null = queen (default). */
    public PieceType promotionType;

    public Move(int fromRow,
                int fromColumn,
                int toRow,
                int toColumn) {

        this.fromRow = fromRow;
        this.fromColumn = fromColumn;
        this.toRow = toRow;
        this.toColumn = toColumn;
    }

    public String toUCI() {

        return toSquare(fromRow, fromColumn)
                +
                toSquare(toRow, toColumn);
    }

    private String toSquare(int row,
                             int column) {

        char file = (char) ('a' + column);
        int rank = 8 - row;

        return "" + file + rank;
    }

    @Override
    public String toString() {
        return toUCI();
    }
}
