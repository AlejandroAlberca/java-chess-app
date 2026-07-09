
package com.devmanchego.engine;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GameState {

    private final Piece[][] board =
            new Piece[8][8];

    private boolean whiteTurn = true;

    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;

    private boolean whiteLeftRookMoved = false;
    private boolean whiteRightRookMoved = false;

    private boolean blackLeftRookMoved = false;
    private boolean blackRightRookMoved = false;

    // En passant: square where a capturing pawn would land (-1 if none)
    private int enPassantTargetRow = -1;
    private int enPassantTargetCol = -1;

    private final MoveHistory moveHistory =
            new MoveHistory();

    // ── Snapshot history for undo ─────────────────────────────────────────────
    // Each entry is saved BEFORE the corresponding move is applied.
    private final List<Piece[][]>  boardSnapshots   = new ArrayList<>();
    private final List<boolean[]>  flagSnapshots    = new ArrayList<>(); // see saveSnapshot()
    private final List<int[]>      captureSnapshots = new ArrayList<>();
    private final List<int[]>      epSnapshots      = new ArrayList<>(); // {enPassantTargetRow, enPassantTargetCol}
    private final List<Long>       hashSnapshots    = new ArrayList<>();
    private final List<Integer>    halfmoveSnapshots = new ArrayList<>();

    // ── Captured pieces (tracked as moves are played) ─────────────────────────
    private final List<Piece> capturedByWhite = new ArrayList<>();
    private final List<Piece> capturedByBlack = new ArrayList<>();

    // Zobrist hash of the current position, maintained incrementally by
    // applyMove() so the AI search doesn't need an O(64) recompute per node.
    private long zobristHash;

    // ── Draw-rule bookkeeping ──────────────────────────────────────────────────
    // Half-moves since the last pawn move or capture (50-move rule: draw at 100).
    private int halfmoveClock = 0;
    // Hash of every position reached, in order, including the current one
    // (threefold repetition: draw once any hash appears 3 times).
    private final List<Long> positionHistory = new ArrayList<>();

    public GameState() {
        initializeBoard();
        zobristHash = ZobristHasher.compute(this);
        positionHistory.add(zobristHash);
    }

    /** Deep copy used by the AI search tree. */
    public GameState cloneState() {
        GameState copy = new GameState();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                copy.board[r][c] = this.board[r][c]; // Piece is immutable

        copy.whiteTurn           = this.whiteTurn;
        copy.whiteKingMoved      = this.whiteKingMoved;
        copy.blackKingMoved      = this.blackKingMoved;
        copy.whiteLeftRookMoved  = this.whiteLeftRookMoved;
        copy.whiteRightRookMoved = this.whiteRightRookMoved;
        copy.blackLeftRookMoved  = this.blackLeftRookMoved;
        copy.blackRightRookMoved = this.blackRightRookMoved;
        copy.enPassantTargetRow  = this.enPassantTargetRow;
        copy.enPassantTargetCol  = this.enPassantTargetCol;
        copy.zobristHash         = this.zobristHash;
        // move history not needed inside search
        return copy;
    }

    public void resetBoard() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                board[r][c] = null;

        whiteTurn = true;
        whiteKingMoved = false;
        blackKingMoved = false;
        whiteLeftRookMoved = false;
        whiteRightRookMoved = false;
        blackLeftRookMoved = false;
        blackRightRookMoved = false;
        enPassantTargetRow = -1;
        enPassantTargetCol = -1;
        moveHistory.clear();
        boardSnapshots.clear();
        flagSnapshots.clear();
        captureSnapshots.clear();
        hashSnapshots.clear();
        epSnapshots.clear();
        halfmoveSnapshots.clear();
        capturedByWhite.clear();
        capturedByBlack.clear();
        halfmoveClock = 0;
        positionHistory.clear();

        initializeBoard();
        zobristHash = ZobristHasher.compute(this);
        positionHistory.add(zobristHash);
    }

    public enum PracticeMode { KQ_VS_K, KR_VS_K, KBB_VS_K, KBN_VS_K }

    public enum BattleType { CAVALRY_DUEL, HEAVY_ARTILLERY, PAWN_WALL, MINOR_PIECE_CLASH }

    /**
     * Clears the board and sets up a checkmate-drill position.
     * White has the mating material; Black has only a King on e8.
     * White King on e1, pieces on their standard starting squares.
     */
    public void setupPracticePosition(PracticeMode mode) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                board[r][c] = null;

        whiteTurn            = true;
        whiteKingMoved       = false;
        blackKingMoved       = false;
        whiteLeftRookMoved   = false;
        whiteRightRookMoved  = false;
        blackLeftRookMoved   = false;
        blackRightRookMoved  = false;
        enPassantTargetRow   = -1;
        enPassantTargetCol   = -1;
        moveHistory.clear();
        boardSnapshots.clear();
        flagSnapshots.clear();
        captureSnapshots.clear();
        hashSnapshots.clear();
        epSnapshots.clear();
        halfmoveSnapshots.clear();
        capturedByWhite.clear();
        capturedByBlack.clear();
        halfmoveClock = 0;
        positionHistory.clear();

        // White King always on e1 (row 7, col 4)
        board[7][4] = new Piece(PieceType.KING,   true);
        // Black King on e8 (row 0, col 4)
        board[0][4] = new Piece(PieceType.KING,   false);

        switch (mode) {
            case KQ_VS_K  -> board[7][3] = new Piece(PieceType.QUEEN,  true);  // d1
            case KR_VS_K  -> board[7][0] = new Piece(PieceType.ROOK,   true);  // a1
            case KBB_VS_K -> {
                board[7][2] = new Piece(PieceType.BISHOP, true);  // c1
                board[7][5] = new Piece(PieceType.BISHOP, true);  // f1
            }
            case KBN_VS_K -> {
                board[7][5] = new Piece(PieceType.BISHOP, true);  // f1
                board[7][6] = new Piece(PieceType.KNIGHT, true);  // g1
            }
        }

        zobristHash = ZobristHasher.compute(this);
        positionHistory.add(zobristHash);
    }

    /**
     * Sets up a special battle position.
     * @param whiteIsA true → Side A pieces placed as white, Side B as black.
     */
    public void setupBattlePosition(BattleType type, boolean whiteIsA) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                board[r][c] = null;

        whiteTurn            = true;
        whiteKingMoved       = false;
        blackKingMoved       = false;
        whiteLeftRookMoved   = false;
        whiteRightRookMoved  = false;
        blackLeftRookMoved   = false;
        blackRightRookMoved  = false;
        enPassantTargetRow   = -1;
        enPassantTargetCol   = -1;
        moveHistory.clear();
        boardSnapshots.clear();
        flagSnapshots.clear();
        captureSnapshots.clear();
        hashSnapshots.clear();
        epSnapshots.clear();
        halfmoveSnapshots.clear();
        capturedByWhite.clear();
        capturedByBlack.clear();
        halfmoveClock = 0;
        positionHistory.clear();

        board[7][4] = new Piece(PieceType.KING, true);
        board[0][4] = new Piece(PieceType.KING, false);

        placeArmy(type, true,  whiteIsA);   // Side A as white or black
        placeArmy(type, false, !whiteIsA);  // Side B on the other side

        zobristHash = ZobristHasher.compute(this);
        positionHistory.add(zobristHash);
    }

    private void placeArmy(BattleType type, boolean sideA, boolean isWhite) {
        int back = isWhite ? 7 : 0;
        int p1   = isWhite ? 6 : 1;
        int p2   = isWhite ? 5 : 2;
        int p3   = isWhite ? 4 : 3;

        switch (type) {
            case CAVALRY_DUEL -> {
                if (sideA) {
                    // 10 Knights: 2 on d+f (flanking king) + 5 on second rank + 3 on third rank
                    board[back][3] = new Piece(PieceType.KNIGHT, isWhite); // d1/d8
                    board[back][5] = new Piece(PieceType.KNIGHT, isWhite); // f1/f8
                    for (int c = 0; c < 8; c++)
                        board[p1][c] = new Piece(PieceType.KNIGHT, isWhite); // full second rank (8)
                    // total: 2 + 8 = 10 ✓
                } else {
                    // 6 Bishops on back rank flanking king + 12 Pawns (full 2nd rank + 4 on 3rd)
                    for (int c : new int[]{0,1,2,5,6,7})
                        board[back][c] = new Piece(PieceType.BISHOP, isWhite);
                    for (int c = 0; c < 8; c++)
                        board[p1][c] = new Piece(PieceType.PAWN, isWhite);
                    for (int c = 2; c < 6; c++)
                        board[p2][c] = new Piece(PieceType.PAWN, isWhite); // c,d,e,f on 3rd rank
                }
            }
            case HEAVY_ARTILLERY -> {
                if (sideA) {
                    // 5 Rooks: corners on back rank (a+h) + 3 on second rank (c,f,h)
                    board[back][0] = new Piece(PieceType.ROOK, isWhite); // a1/a8 corner
                    board[back][7] = new Piece(PieceType.ROOK, isWhite); // h1/h8 corner
                    board[p1][2]   = new Piece(PieceType.ROOK, isWhite); // c2/c7 king guard
                    board[p1][5]   = new Piece(PieceType.ROOK, isWhite); // f2/f7 king guard
                    board[p1][7]   = new Piece(PieceType.ROOK, isWhite); // h2/h7 battery
                } else {
                    // 3 Queens flanking king (b8,d8,g8) + 3 Pawns shielding king (d7,e7,g7)
                    board[back][1] = new Piece(PieceType.QUEEN, isWhite); // b1/b8
                    board[back][3] = new Piece(PieceType.QUEEN, isWhite); // d1/d8 near king
                    board[back][6] = new Piece(PieceType.QUEEN, isWhite); // g1/g8
                    board[p1][3]   = new Piece(PieceType.PAWN,  isWhite); // d2/d7 king shield
                    board[p1][4]   = new Piece(PieceType.PAWN,  isWhite); // e2/e7 king shield
                    board[p1][6]   = new Piece(PieceType.PAWN,  isWhite); // g2/g7 below queen
                }
            }
            case PAWN_WALL -> {
                if (sideA) {
                    // 24 Pawns across 3 rows
                    for (int c = 0; c < 8; c++) board[p1][c] = new Piece(PieceType.PAWN, isWhite);
                    for (int c = 0; c < 8; c++) board[p2][c] = new Piece(PieceType.PAWN, isWhite);
                    for (int c = 0; c < 8; c++) board[p3][c] = new Piece(PieceType.PAWN, isWhite);
                } else {
                    // 4 Knights + 4 Bishops (symmetric formation)
                    board[back][1] = new Piece(PieceType.KNIGHT, isWhite);
                    board[back][6] = new Piece(PieceType.KNIGHT, isWhite);
                    board[back][2] = new Piece(PieceType.BISHOP, isWhite);
                    board[back][5] = new Piece(PieceType.BISHOP, isWhite);
                    board[p1][1]   = new Piece(PieceType.KNIGHT, isWhite);
                    board[p1][6]   = new Piece(PieceType.KNIGHT, isWhite);
                    board[p1][2]   = new Piece(PieceType.BISHOP, isWhite);
                    board[p1][5]   = new Piece(PieceType.BISHOP, isWhite);
                }
            }
            case MINOR_PIECE_CLASH -> {
                if (sideA) {
                    // 8 Knights: 2 flanking king (d+f) + 6 on second rank
                    board[back][3] = new Piece(PieceType.KNIGHT, isWhite); // d1/d8
                    board[back][5] = new Piece(PieceType.KNIGHT, isWhite); // f1/f8
                    for (int c : new int[]{0,1,2,5,6,7})
                        board[p1][c] = new Piece(PieceType.KNIGHT, isWhite); // 6 on 2nd rank
                } else {
                    // 8 Bishops: 2 flanking king (c+f, different colors) + 6 on second rank
                    board[back][2] = new Piece(PieceType.BISHOP, isWhite); // c1/c8
                    board[back][5] = new Piece(PieceType.BISHOP, isWhite); // f1/f8
                    for (int c : new int[]{0,1,3,4,6,7})
                        board[p1][c] = new Piece(PieceType.BISHOP, isWhite); // 6 on 2nd rank
                }
            }
        }
    }

    private void initializeBoard() {

        setupWhitePieces();
        setupBlackPieces();
    }

    private void setupWhitePieces() {

        board[7][0] =
                new Piece(PieceType.ROOK, true);

        board[7][1] =
                new Piece(PieceType.KNIGHT, true);

        board[7][2] =
                new Piece(PieceType.BISHOP, true);

        board[7][3] =
                new Piece(PieceType.QUEEN, true);

        board[7][4] =
                new Piece(PieceType.KING, true);

        board[7][5] =
                new Piece(PieceType.BISHOP, true);

        board[7][6] =
                new Piece(PieceType.KNIGHT, true);

        board[7][7] =
                new Piece(PieceType.ROOK, true);

        for (int column = 0; column < 8; column++) {

            board[6][column] =
                    new Piece(PieceType.PAWN, true);
        }
    }

    private void setupBlackPieces() {

        board[0][0] =
                new Piece(PieceType.ROOK, false);

        board[0][1] =
                new Piece(PieceType.KNIGHT, false);

        board[0][2] =
                new Piece(PieceType.BISHOP, false);

        board[0][3] =
                new Piece(PieceType.QUEEN, false);

        board[0][4] =
                new Piece(PieceType.KING, false);

        board[0][5] =
                new Piece(PieceType.BISHOP, false);

        board[0][6] =
                new Piece(PieceType.KNIGHT, false);

        board[0][7] =
                new Piece(PieceType.ROOK, false);

        for (int column = 0; column < 8; column++) {

            board[1][column] =
                    new Piece(PieceType.PAWN, false);
        }
    }

    public Piece getPiece(int row,
                          int column) {

        return board[row][column];
    }

    public Piece[][] getBoard() {
        return board;
    }

    public boolean isWhiteTurn() {
        return whiteTurn;
    }

    public MoveHistory getMoveHistory() {
        return moveHistory;
    }

    public List<Piece> getCapturedByWhite() { return List.copyOf(capturedByWhite); }
    public List<Piece> getCapturedByBlack() { return List.copyOf(capturedByBlack); }

    /** Zobrist hash of the current position (piece placement + side + castling + en passant). */
    public long getZobristHash() { return zobristHash; }

    /** Bitmask of castling rights: bit0=white O-O, bit1=white O-O-O, bit2=black O-O, bit3=black O-O-O. */
    public int getCastlingRightsMask() {
        int mask = 0;
        if (!whiteKingMoved && !whiteRightRookMoved) mask |= 1;
        if (!whiteKingMoved && !whiteLeftRookMoved)  mask |= 2;
        if (!blackKingMoved && !blackRightRookMoved) mask |= 4;
        if (!blackKingMoved && !blackLeftRookMoved)  mask |= 8;
        return mask;
    }

    /** The en-passant target file, or -1 if no en-passant capture is currently available. */
    public int getEnPassantTargetCol() { return enPassantTargetRow == -1 ? -1 : enPassantTargetCol; }

    public boolean isLegalMove(Move move) {
        return isLegalMove(move, false);
    }

    public boolean isLegalMove(Move move,
                               boolean ignoreTurn) {

        if (!isInsideBoard(move.fromRow,
                           move.fromColumn)) {

            return false;
        }

        if (!isInsideBoard(move.toRow,
                           move.toColumn)) {

            return false;
        }

        Piece piece =
                board[move.fromRow][move.fromColumn];

        if (piece == null) {
            return false;
        }

        if (!ignoreTurn
                &&
                piece.isWhite() != whiteTurn) {

            return false;
        }

        Piece target =
                board[move.toRow][move.toColumn];

        // cannot capture own piece
        if (target != null
                &&
                target.isWhite() == piece.isWhite()) {

            return false;
        }

        boolean valid = switch (piece.getType()) {

            case PAWN ->
                    validatePawnMove(move, piece);

            case KNIGHT ->
                    validateKnightMove(move);

            case ROOK -> {
                if (!isPathClear(move)) {
                    yield false;
                }
                yield validateRookMove(move);
            }

            case BISHOP -> {
                if (!isPathClear(move)) {
                    yield false;
                }
                yield validateBishopMove(move);
            }

            case QUEEN -> {
                if (!isPathClear(move)) {
                    yield false;
                }
                yield validateQueenMove(move);
            }

            case KING ->
                    validateKingMove(move, piece);
        };

        if (!valid) {
            return false;
        }

        return !wouldLeaveKingInCheck(move);
    }

    private boolean validatePawnMove(Move move,
                                     Piece piece) {

        int direction =
                piece.isWhite() ? -1 : 1;

        int startRow =
                piece.isWhite() ? 6 : 1;

        Piece target =
                board[move.toRow][move.toColumn];

        // forward move
        if (move.fromColumn == move.toColumn) {

            if (target != null) {
                return false;
            }

            // single move
            if (move.toRow
                    == move.fromRow + direction) {

                return true;
            }

            // double move
            if (move.fromRow == startRow
                    &&
                    move.toRow ==
                            move.fromRow
                                    + 2 * direction) {

                int middleRow =
                        move.fromRow + direction;

                return board[middleRow]
                            [move.fromColumn]
                        == null;
            }

            return false;
        }

        // normal capture
        if (Math.abs(move.toColumn - move.fromColumn) == 1
                && move.toRow == move.fromRow + direction
                && target != null
                && target.isWhite() != piece.isWhite()) {
            return true;
        }

        // en passant capture
        if (Math.abs(move.toColumn - move.fromColumn) == 1
                && move.toRow == move.fromRow + direction
                && target == null
                && move.toRow == enPassantTargetRow
                && move.toColumn == enPassantTargetCol) {
            move.enPassantCapture = true;
            return true;
        }

        return false;
    }

    private boolean isPawnAttacking(Move move,
                                    Piece piece) {

        int direction =
                piece.isWhite() ? -1 : 1;

        return Math.abs(
                move.toColumn
                        - move.fromColumn
        ) == 1
                &&
                move.toRow
                        == move.fromRow
                        + direction;
    }

    private boolean validateRookMove(Move move) {

        return move.fromRow == move.toRow
                ||
                move.fromColumn == move.toColumn;
    }

    private boolean validateKnightMove(Move move) {

        int rowDiff =
                Math.abs(
                        move.toRow
                                - move.fromRow
                );

        int columnDiff =
                Math.abs(
                        move.toColumn
                                - move.fromColumn
                );

        return (rowDiff == 2
                &&
                columnDiff == 1)

                ||

                (rowDiff == 1
                &&
                columnDiff == 2);
    }

    private boolean validateBishopMove(Move move) {

        return Math.abs(
                move.toRow
                        - move.fromRow
        )
                ==
                Math.abs(
                        move.toColumn
                                - move.fromColumn
                );
    }

    private boolean validateQueenMove(Move move) {

        return validateRookMove(move)
                ||
                validateBishopMove(move);
    }

    private boolean validateKingMove(Move move,
                                     Piece piece) {

        int rowDiff =
                Math.abs(
                        move.toRow
                                - move.fromRow
                );

        int columnDiff =
                Math.abs(
                        move.toColumn
                                - move.fromColumn
                );

        if (rowDiff <= 1
                &&
                columnDiff <= 1) {

            return true;
        }

        return validateCastling(move, piece);
    }

    private boolean validateCastling(Move move,
                                     Piece piece) {

        if (piece.isWhite() && whiteKingMoved)   return false;
        if (!piece.isWhite() && blackKingMoved)  return false;
        if (move.fromRow != move.toRow)          return false;

        int colDiff = move.toColumn - move.fromColumn;
        if (Math.abs(colDiff) != 2)              return false;

        boolean kingSide = colDiff > 0;
        int row = move.fromRow;
        int rookCol = kingSide ? 7 : 0;

        // Bug 4 fix: verify rook still exists at corner
        Piece rook = board[row][rookCol];
        if (rook == null
                || rook.getType() != PieceType.ROOK
                || rook.isWhite() != piece.isWhite()) {
            return false;
        }

        // Bug 4 fix: verify rook-moved flag
        if (piece.isWhite()) {
            if (kingSide  && whiteRightRookMoved) return false;
            if (!kingSide && whiteLeftRookMoved)  return false;
        } else {
            if (kingSide  && blackRightRookMoved) return false;
            if (!kingSide && blackLeftRookMoved)  return false;
        }

        // Bug 3 fix: for queenside, b-file square must also be empty
        if (!kingSide && board[row][1] != null) return false;

        // Path between king and destination must be clear
        if (!isPathClear(move)) return false;

        // Cannot castle while in check
        if (isKingInCheck(piece.isWhite())) return false;

        // Bug 2 fix: king cannot pass through an attacked square
        int transitCol = move.fromColumn + (kingSide ? 1 : -1);
        if (isSquareAttacked(row, transitCol, !piece.isWhite())) return false;

        move.castlingMove = true;
        return true;
    }

    /** Returns true if the given square is attacked by any piece of color `byWhite`. */
    private boolean isSquareAttacked(int targetRow, int targetCol, boolean byWhite) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p == null || p.isWhite() != byWhite) continue;
                Move atk = new Move(r, c, targetRow, targetCol);
                boolean attacks = switch (p.getType()) {
                    case PAWN   -> isPawnAttacking(atk, p);
                    case KNIGHT -> validateKnightMove(atk);
                    case BISHOP -> validateBishopMove(atk) && isPathClear(atk);
                    case ROOK   -> validateRookMove(atk)   && isPathClear(atk);
                    case QUEEN  -> validateQueenMove(atk)  && isPathClear(atk);
                    case KING   -> Math.abs(atk.toRow - atk.fromRow) <= 1
                                   && Math.abs(atk.toColumn - atk.fromColumn) <= 1;
                };
                if (attacks) return true;
            }
        }
        return false;
    }

    private boolean isPathClear(Move move) {

        int dr = Integer.compare(
                move.toRow,
                move.fromRow
        );

        int dc = Integer.compare(
                move.toColumn,
                move.fromColumn
        );

        int row = move.fromRow + dr;
        int column = move.fromColumn + dc;

        while (row != move.toRow
                ||
                column != move.toColumn) {

            // 🔒 protección crítica
            if (!isInsideBoard(row, column)) {
                return false;
            }

            if (board[row][column] != null) {
                return false;
            }

            row += dr;
            column += dc;
        }

        return true;
    }

    private boolean wouldLeaveKingInCheck(Move move) {

        Piece movingPiece  = board[move.fromRow][move.fromColumn];
        Piece capturedPiece = board[move.toRow][move.toColumn];

        // For en passant, the captured pawn is NOT at the destination square
        int epCaptureRow = -1, epCaptureCol = -1;
        Piece epCapturedPawn = null;
        if (move.enPassantCapture) {
            int direction = movingPiece.isWhite() ? 1 : -1; // pawn behind target row
            epCaptureRow = move.toRow + direction;
            epCaptureCol = move.toColumn;
            epCapturedPawn = board[epCaptureRow][epCaptureCol];
            board[epCaptureRow][epCaptureCol] = null;
        }

        board[move.toRow][move.toColumn]     = movingPiece;
        board[move.fromRow][move.fromColumn] = null;

        boolean inCheck = isKingInCheck(movingPiece.isWhite());

        // Restore board
        board[move.fromRow][move.fromColumn] = movingPiece;
        board[move.toRow][move.toColumn]     = capturedPiece;
        if (move.enPassantCapture) {
            board[epCaptureRow][epCaptureCol] = epCapturedPawn;
        }

        return inCheck;
    }

    public boolean isKingInCheck(
            boolean white) {

        int kingRow = -1;
        int kingColumn = -1;

        // find king
        for (int row = 0; row < 8; row++) {

            for (int column = 0;
                 column < 8;
                 column++) {

                Piece piece =
                        board[row][column];

                if (piece != null
                        &&
                        piece.getType()
                                == PieceType.KING
                        &&
                        piece.isWhite() == white) {

                    kingRow = row;
                    kingColumn = column;
                }
            }
        }

        // scan enemy attacks
        for (int row = 0; row < 8; row++) {

            for (int column = 0;
                 column < 8;
                 column++) {

                Piece piece =
                        board[row][column];

                if (piece == null
                        ||
                        piece.isWhite() == white) {

                    continue;
                }

                Move attackMove =
                        new Move(
                                row,
                                column,
                                kingRow,
                                kingColumn
                        );

                boolean attacks =
                        switch (piece.getType()) {

                            case PAWN ->
                                    isPawnAttacking(
                                            attackMove,
                                            piece
                                    );

                            case ROOK ->
                                    validateRookMove(
                                            attackMove
                                    )
                                    &&
                                    isPathClear(
                                            attackMove
                                    );

                            case KNIGHT ->
                                    validateKnightMove(
                                            attackMove
                                    );

                            case BISHOP ->
                                    validateBishopMove(
                                            attackMove
                                    )
                                    &&
                                    isPathClear(
                                            attackMove
                                    );

                            case QUEEN ->
                                    validateQueenMove(
                                            attackMove
                                    )
                                    &&
                                    isPathClear(
                                            attackMove
                                    );

                            case KING ->
                                    validateKingMove(
                                            attackMove,
                                            piece
                                    );
                        };

                if (attacks) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isCheckmate(
            boolean white) {

        if (!isKingInCheck(white)) {
            return false;
        }

        for (int row = 0; row < 8; row++) {

            for (int column = 0;
                 column < 8;
                 column++) {

                Piece piece =
                        board[row][column];

                if (piece == null
                        ||
                        piece.isWhite() != white) {

                    continue;
                }

                for (int targetRow = 0;
                     targetRow < 8;
                     targetRow++) {

                    for (int targetColumn = 0;
                         targetColumn < 8;
                         targetColumn++) {

                        Move move =
                                new Move(
                                        row,
                                        column,
                                        targetRow,
                                        targetColumn
                                );

                        if (isLegalMove(
                                move,
                                true
                        )) {

                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    public void applyMove(Move move) {

        if (!isLegalMove(move)) {
            return;
        }

        saveSnapshot(); // ← must come after legality check

        Piece piece =
                board[move.fromRow]
                     [move.fromColumn];

        int fromSq = move.fromRow * 8 + move.fromColumn;
        int toSq   = move.toRow   * 8 + move.toColumn;

        // ── Hash: remove state that is about to change ────────────────────────
        zobristHash ^= ZobristHasher.pieceKey(piece, fromSq);
        int oldCastleMask = getCastlingRightsMask();
        int oldEpCol       = getEnPassantTargetCol();

        // update king moved flags
        if (piece.getType()
                == PieceType.KING) {

            if (piece.isWhite()) {
                whiteKingMoved = true;
            } else {
                blackKingMoved = true;
            }
        }

        // update rook moved flags
        if (piece.getType()
                == PieceType.ROOK) {

            if (piece.isWhite()) {

                if (move.fromColumn == 0) {
                    whiteLeftRookMoved = true;
                }

                if (move.fromColumn == 7) {
                    whiteRightRookMoved = true;
                }

            } else {

                if (move.fromColumn == 0) {
                    blackLeftRookMoved = true;
                }

                if (move.fromColumn == 7) {
                    blackRightRookMoved = true;
                }
            }
        }

        // Track regular captures
        Piece regularCapture = board[move.toRow][move.toColumn];
        if (regularCapture != null) {
            if (piece.isWhite()) capturedByWhite.add(regularCapture);
            else                 capturedByBlack.add(regularCapture);
            zobristHash ^= ZobristHasher.pieceKey(regularCapture, toSq);
        }

        // pawn promotion: defaults to queen unless the caller chose otherwise
        Piece placedPiece = piece;
        if (piece.getType() == PieceType.PAWN) {
            boolean promotionRow = piece.isWhite()
                    ? move.toRow == 0
                    : move.toRow == 7;
            if (promotionRow) {
                PieceType promoteTo = move.promotionType != null ? move.promotionType : PieceType.QUEEN;
                placedPiece = new Piece(promoteTo, piece.isWhite());
            }
        }

        board[move.toRow][move.toColumn]     = placedPiece;
        board[move.fromRow][move.fromColumn] = null;
        zobristHash ^= ZobristHasher.pieceKey(placedPiece, toSq);

        // En passant: remove and track the captured pawn
        if (move.enPassantCapture) {
            int direction = piece.isWhite() ? 1 : -1;
            int epRow = move.toRow + direction;
            Piece epPawn = board[epRow][move.toColumn];
            if (epPawn != null) {
                if (piece.isWhite()) capturedByWhite.add(epPawn);
                else                 capturedByBlack.add(epPawn);
                zobristHash ^= ZobristHasher.pieceKey(epPawn, epRow * 8 + move.toColumn);
            }
            board[epRow][move.toColumn] = null;
        }

        // Update en passant target for next move
        if (piece.getType() == PieceType.PAWN
                && Math.abs(move.toRow - move.fromRow) == 2) {
            // Pawn just double-moved: set target to the square it passed through
            enPassantTargetRow = (move.fromRow + move.toRow) / 2;
            enPassantTargetCol = move.fromColumn;
        } else {
            enPassantTargetRow = -1;
            enPassantTargetCol = -1;
        }

        // castling rook move
        if (move.castlingMove) {

            int rookRow = move.toRow;

            // king side
            if (move.toColumn == 6) {

                Piece rook = board[rookRow][7];
                zobristHash ^= ZobristHasher.pieceKey(rook, rookRow * 8 + 7);
                board[rookRow][5] = rook;
                board[rookRow][7] = null;
                zobristHash ^= ZobristHasher.pieceKey(rook, rookRow * 8 + 5);
            }

            // queen side
            if (move.toColumn == 2) {

                Piece rook = board[rookRow][0];
                zobristHash ^= ZobristHasher.pieceKey(rook, rookRow * 8 + 0);
                board[rookRow][3] = rook;
                board[rookRow][0] = null;
                zobristHash ^= ZobristHasher.pieceKey(rook, rookRow * 8 + 3);
            }
        }

        // ── Hash: add back the state that changed ──────────────────────────────
        int newCastleMask = getCastlingRightsMask();
        for (int i = 0; i < 4; i++) {
            boolean had = (oldCastleMask & (1 << i)) != 0;
            boolean has = (newCastleMask & (1 << i)) != 0;
            if (had != has) zobristHash ^= ZobristHasher.castleKey(i);
        }
        if (oldEpCol >= 0) zobristHash ^= ZobristHasher.epFileKey(oldEpCol);
        int newEpCol = getEnPassantTargetCol();
        if (newEpCol >= 0) zobristHash ^= ZobristHasher.epFileKey(newEpCol);
        zobristHash ^= ZobristHasher.sideKey();

        // 50-move rule: the clock resets on any pawn move or capture.
        boolean irreversible = piece.getType() == PieceType.PAWN
                || regularCapture != null || move.enPassantCapture;
        halfmoveClock = irreversible ? 0 : halfmoveClock + 1;

        positionHistory.add(zobristHash);

        moveHistory.addMove(move.toUCI());

        whiteTurn = !whiteTurn;
    }

    public List<Move> getLegalMovesFrom(
            int row,
            int column) {

        List<Move> moves =
                new ArrayList<>();

        Piece piece =
                board[row][column];

        if (piece == null) {
            return moves;
        }

        for (int targetRow = 0;
             targetRow < 8;
             targetRow++) {

            for (int targetColumn = 0;
                 targetColumn < 8;
                 targetColumn++) {

                Move move =
                        new Move(
                                row,
                                column,
                                targetRow,
                                targetColumn
                        );

                if (isLegalMove(move,
                                true)) {

                    moves.add(move);
                }
            }
        }

        return moves;
    }

    public boolean isStalemate(boolean white) {
        if (isKingInCheck(white)) return false;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p != null && p.isWhite() == white
                        && !getLegalMovesFrom(r, c).isEmpty())
                    return false;
            }
        return true;
    }

    // ── Draw rules ────────────────────────────────────────────────────────────

    /** True once the current position has occurred 3 or more times. */
    public boolean isThreefoldRepetition() {
        int count = 0;
        for (long h : positionHistory)
            if (h == zobristHash) count++;
        return count >= 3;
    }

    /** True once 50 full moves (100 half-moves) have passed with no pawn move or capture. */
    public boolean isFiftyMoveRule() {
        return halfmoveClock >= 100;
    }

    /**
     * True when neither side has enough material to force checkmate:
     * K vs K, K+minor vs K, or K+B vs K+B with same-colored bishops.
     * Any pawn, rook, or queen on the board always counts as sufficient material.
     */
    public boolean isInsufficientMaterial() {
        Integer whiteBishopSq = null, blackBishopSq = null;
        int whiteMinors = 0, blackMinors = 0;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p == null || p.getType() == PieceType.KING) continue;
                if (p.getType() == PieceType.QUEEN
                        || p.getType() == PieceType.ROOK
                        || p.getType() == PieceType.PAWN) {
                    return false;
                }
                if (p.isWhite()) {
                    whiteMinors++;
                    if (p.getType() == PieceType.BISHOP) whiteBishopSq = r * 8 + c;
                } else {
                    blackMinors++;
                    if (p.getType() == PieceType.BISHOP) blackBishopSq = r * 8 + c;
                }
            }
        }

        int totalMinors = whiteMinors + blackMinors;
        if (totalMinors == 0) return true;  // K vs K
        if (totalMinors == 1) return true;  // K+minor vs K

        if (whiteMinors == 1 && blackMinors == 1
                && whiteBishopSq != null && blackBishopSq != null) {
            boolean sameColorSquare =
                    ((whiteBishopSq / 8 + whiteBishopSq % 8) % 2)
                            == ((blackBishopSq / 8 + blackBishopSq % 8) % 2);
            return sameColorSquare;
        }
        return false;
    }

    /** True if the game is drawn by any automatic rule (repetition, 50-move, or material). */
    public boolean isAutomaticDraw() {
        return isThreefoldRepetition() || isFiftyMoveRule() || isInsufficientMaterial();
    }

    // ── Snapshot / Undo ───────────────────────────────────────────────────────

    private void saveSnapshot() {
        Piece[][] snap = new Piece[8][8];
        for (int r = 0; r < 8; r++) snap[r] = board[r].clone();
        boardSnapshots.add(snap);
        flagSnapshots.add(new boolean[]{
                whiteTurn,
                whiteKingMoved, blackKingMoved,
                whiteLeftRookMoved, whiteRightRookMoved,
                blackLeftRookMoved, blackRightRookMoved
        });
        captureSnapshots.add(new int[]{capturedByWhite.size(), capturedByBlack.size()});
        epSnapshots.add(new int[]{enPassantTargetRow, enPassantTargetCol});
        hashSnapshots.add(zobristHash);
        halfmoveSnapshots.add(halfmoveClock);
    }

    /** Number of half-moves (plies) played so far. */
    public int getMoveCount() { return boardSnapshots.size(); }

    /**
     * Undo the last {@code plies} half-moves.
     * @return true if successful, false if not enough moves in history.
     */
    public boolean undoMoves(int plies) {
        if (boardSnapshots.size() < plies) return false;

        int target = boardSnapshots.size() - plies;
        Piece[][] snap = boardSnapshots.get(target);
        for (int r = 0; r < 8; r++) board[r] = snap[r].clone();

        boolean[] flags = flagSnapshots.get(target);
        whiteTurn           = flags[0];
        whiteKingMoved      = flags[1];
        blackKingMoved      = flags[2];
        whiteLeftRookMoved  = flags[3];
        whiteRightRookMoved = flags[4];
        blackLeftRookMoved  = flags[5];
        blackRightRookMoved = flags[6];

        int[] capSnap = captureSnapshots.get(target);
        capturedByWhite.subList(capSnap[0], capturedByWhite.size()).clear();
        capturedByBlack.subList(capSnap[1], capturedByBlack.size()).clear();

        int[] epSnap = epSnapshots.get(target);
        enPassantTargetRow = epSnap[0];
        enPassantTargetCol = epSnap[1];

        zobristHash = hashSnapshots.get(target);
        halfmoveClock = halfmoveSnapshots.get(target);

        // positionHistory[i] is the position reached after i plies — same indexing
        // as boardSnapshots, plus one extra leading entry for the initial position.
        positionHistory.subList(target + 1, positionHistory.size()).clear();

        boardSnapshots.subList(target,   boardSnapshots.size()).clear();
        flagSnapshots.subList(target,    flagSnapshots.size()).clear();
        captureSnapshots.subList(target, captureSnapshots.size()).clear();
        epSnapshots.subList(target,      epSnapshots.size()).clear();
        hashSnapshots.subList(target,    hashSnapshots.size()).clear();
        halfmoveSnapshots.subList(target, halfmoveSnapshots.size()).clear();
        moveHistory.trimTo(target);
        return true;
    }

    private boolean isInsideBoard(int row,
                                  int column) {

        return row >= 0
                &&
                row < 8
                &&
                column >= 0
                &&
                column < 8;
    }

    // Format per line:
    //   BOARD:<64 chars, '.' for empty or piece char>
    //   FLAGS:<whiteTurn>,<wKingMoved>,<bKingMoved>,<wLR>,<wRR>,<bLR>,<bRR>
    //   MOVES:<uci1> <uci2> ...
    public void saveToFile(File file) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {

            // board row by row, left to right
            StringBuilder boardLine = new StringBuilder("BOARD:");
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    Piece p = board[r][c];
                    boardLine.append(p == null ? '.' : p.toString());
                }
            }
            w.write(boardLine.toString());
            w.newLine();

            w.write("FLAGS:"
                    + whiteTurn + ","
                    + whiteKingMoved + ","
                    + blackKingMoved + ","
                    + whiteLeftRookMoved + ","
                    + whiteRightRookMoved + ","
                    + blackLeftRookMoved + ","
                    + blackRightRookMoved);
            w.newLine();

            List<String> history = moveHistory.getMoves();
            w.write("MOVES:" + String.join(" ", history));
            w.newLine();
        }
    }

    public void loadFromFile(File file) throws IOException {
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String boardLine = r.readLine();
            String flagsLine = r.readLine();
            String movesLine = r.readLine();

            if (boardLine == null || !boardLine.startsWith("BOARD:")
                    || flagsLine == null || !flagsLine.startsWith("FLAGS:")
                    || movesLine == null || !movesLine.startsWith("MOVES:")) {
                throw new IOException("Invalid save file format.");
            }

            // restore board
            String boardData = boardLine.substring(6);
            if (boardData.length() != 64)
                throw new IOException("Corrupt board data.");

            for (int r2 = 0; r2 < 8; r2++) {
                for (int c = 0; c < 8; c++) {
                    char ch = boardData.charAt(r2 * 8 + c);
                    board[r2][c] = charToPiece(ch);
                }
            }

            // restore flags
            String[] flags = flagsLine.substring(6).split(",");
            whiteTurn           = Boolean.parseBoolean(flags[0]);
            whiteKingMoved      = Boolean.parseBoolean(flags[1]);
            blackKingMoved      = Boolean.parseBoolean(flags[2]);
            whiteLeftRookMoved  = Boolean.parseBoolean(flags[3]);
            whiteRightRookMoved = Boolean.parseBoolean(flags[4]);
            blackLeftRookMoved  = Boolean.parseBoolean(flags[5]);
            blackRightRookMoved = Boolean.parseBoolean(flags[6]);

            // restore move history
            moveHistory.clear();
            String movesData = movesLine.substring(6).trim();
            if (!movesData.isEmpty()) {
                for (String uci : movesData.split(" ")) {
                    moveHistory.addMove(uci);
                }
            }

            // Loaded games have no undo history, en passant target, or capture
            // tracking of their own — discard whatever the previous game left behind.
            boardSnapshots.clear();
            flagSnapshots.clear();
            captureSnapshots.clear();
            hashSnapshots.clear();
            epSnapshots.clear();
            halfmoveSnapshots.clear();
            capturedByWhite.clear();
            capturedByBlack.clear();
            enPassantTargetRow = -1;
            enPassantTargetCol = -1;
            // Saved games don't persist the halfmove clock or position history —
            // both restart from this loaded position (a minor, low-risk simplification).
            halfmoveClock = 0;
            zobristHash = ZobristHasher.compute(this);
            positionHistory.clear();
            positionHistory.add(zobristHash);
        }
    }

    private static Piece charToPiece(char ch) {
        return switch (ch) {
            case 'K' -> new Piece(PieceType.KING,   true);
            case 'Q' -> new Piece(PieceType.QUEEN,  true);
            case 'R' -> new Piece(PieceType.ROOK,   true);
            case 'B' -> new Piece(PieceType.BISHOP, true);
            case 'N' -> new Piece(PieceType.KNIGHT, true);
            case 'P' -> new Piece(PieceType.PAWN,   true);
            case 'k' -> new Piece(PieceType.KING,   false);
            case 'q' -> new Piece(PieceType.QUEEN,  false);
            case 'r' -> new Piece(PieceType.ROOK,   false);
            case 'b' -> new Piece(PieceType.BISHOP, false);
            case 'n' -> new Piece(PieceType.KNIGHT, false);
            case 'p' -> new Piece(PieceType.PAWN,   false);
            default  -> null;
        };
    }
}

