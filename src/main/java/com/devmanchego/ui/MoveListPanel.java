package com.devmanchego.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MoveListPanel extends JPanel {

    private final List<String> moves =
            new ArrayList();

    private final JTextArea area =
            new JTextArea();

    public MoveListPanel() {

        setLayout(new BorderLayout());

        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));

        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    public void addMove(String move) {

        moves.add(move);

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < moves.size(); i += 2) {

            sb.append((i/2 + 1) + ".")
              .append(" ")
              .append(moves.get(i));

            if (i + 1 < moves.size()) {
                sb.append(" ")
                  .append(moves.get(i+1));
            }

            sb.append("\n");
        }

        area.setText(sb.toString());
    }
}
