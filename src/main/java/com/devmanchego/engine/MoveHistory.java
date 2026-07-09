package com.devmanchego.engine;

import java.util.ArrayList;
import java.util.List;

public class MoveHistory {

    private final List<String> moves =
            new ArrayList<>();

    public void addMove(String move) {
        moves.add(move);
    }

    public void clear() {
        moves.clear();
    }

    /** Keep only the first {@code size} moves, discarding the rest. */
    public void trimTo(int size) {
        if (size < moves.size())
            moves.subList(size, moves.size()).clear();
    }

    public int size() { return moves.size(); }

    public List<String> getMoves() {
        return moves;
    }

    public String toPGN() {

        StringBuilder builder =
                new StringBuilder();

        int moveNumber = 1;

        for (int i = 0; i < moves.size(); i += 2) {

            builder.append(moveNumber++)
                    .append(".")
                    .append(" ")
                    .append(moves.get(i));

            if (i + 1 < moves.size()) {
                builder.append(" ")
                        .append(moves.get(i + 1));
            }

            builder.append("\n");
        }

        return builder.toString();
    }
}
