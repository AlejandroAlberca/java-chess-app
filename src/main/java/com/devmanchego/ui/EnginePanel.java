package com.devmanchego.ui;

import com.devmanchego.app.I18n;
import com.devmanchego.engine.AnalysisResult;
import com.devmanchego.engine.ChessAI;
import com.devmanchego.engine.GameState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

public class EnginePanel extends JPanel {

    private final ChessAI        ai;
    private final GameState      gameState;
    private final AnalysisPanel  analysisPanel;

    private final JButton   analyzeBtn  = new JButton(I18n.get("engine.analyze"));
    private final JButton   stopBtn     = new JButton(I18n.get("engine.stop"));
    private final JCheckBox autoCheck   = new JCheckBox(I18n.get("engine.auto"), false);
    private final JComboBox<Integer> multiPVBox = new JComboBox<>(new Integer[]{1, 2, 3});
    private final JLabel    multiPVLabel = label(I18n.get("engine.multipv"));
    private final JLabel    statusLabel = new JLabel(I18n.get("engine.status.idle"));

    private volatile boolean running = false;
    private volatile boolean stopRequested = false;

    /** Optional callback so the board can receive the result (for arrow drawing). */
    private Consumer<AnalysisResult> onResult;

    /** Score from white's perspective before the last analyzed position.
     *  Used for move-quality classification. */
    private Integer prevScoreWhite = null;

    public EnginePanel(ChessAI ai, GameState gameState, AnalysisPanel analysisPanel) {
        this.ai            = ai;
        this.gameState     = gameState;
        this.analysisPanel = analysisPanel;

        setLayout(new GridLayout(0, 1, 0, 4));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setBackground(new Color(50, 50, 50));

        // Row 1: buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btnRow.setBackground(getBackground());
        btnRow.add(analyzeBtn);
        btnRow.add(stopBtn);
        btnRow.add(autoCheck);
        add(btnRow);

        // Row 2: MultiPV
        JPanel mpvRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        mpvRow.setBackground(getBackground());
        mpvRow.add(multiPVLabel);
        multiPVBox.setPreferredSize(new Dimension(55, 24));
        mpvRow.add(multiPVBox);
        add(mpvRow);

        // Row 3: status
        statusLabel.setForeground(Color.LIGHT_GRAY);
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        add(statusLabel);

        stopBtn.setEnabled(false);

        analyzeBtn.addActionListener(e -> triggerAnalysis());
        stopBtn.addActionListener(e -> {
            stopRequested = true;
            setStatus(I18n.get("engine.status.stopped"));
        });
    }

    public void setOnResult(Consumer<AnalysisResult> cb) { this.onResult = cb; }

    public boolean isAutoEnabled() { return autoCheck.isSelected(); }

    /** Called by ChessBoardUI after each move when auto-analyze is on. */
    public void triggerAnalysis() {
        if (running) return;
        running       = true;
        stopRequested = false;

        analyzeBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        setStatus(I18n.get("engine.status.analyzing"));
        analysisPanel.setStatus(I18n.get("engine.status.analyzing"));

        int multiPV = (Integer) multiPVBox.getSelectedItem();
        Integer prevScore = prevScoreWhite;

        new Thread(() -> {
            AnalysisResult result = ai.analyze(gameState, multiPV, prevScore);

            SwingUtilities.invokeLater(() -> {
                running = false;
                analyzeBtn.setEnabled(true);
                stopBtn.setEnabled(false);

                if (stopRequested) {
                    setStatus(I18n.get("engine.status.stopped"));
                    analysisPanel.setStatus(I18n.get("engine.status.stopped"));
                    return;
                }

                prevScoreWhite = result.scoreCP;

                analysisPanel.update(result);
                String doneMsg = I18n.get("engine.status.done", result.depth);
                analysisPanel.setStatus(doneMsg);
                setStatus(doneMsg);

                if (onResult != null) onResult.accept(result);
            });
        }, "analysis-thread").start();
    }

    /** Reset stored previous score (call on new game). */
    public void resetPrevScore() { prevScoreWhite = null; }

    public void refreshTexts() {
        analyzeBtn.setText(I18n.get("engine.analyze"));
        stopBtn.setText(I18n.get("engine.stop"));
        autoCheck.setText(I18n.get("engine.auto"));
        multiPVLabel.setText(I18n.get("engine.multipv"));
        if (!running) statusLabel.setText(I18n.get("engine.status.idle"));
    }

    private void setStatus(String text) { statusLabel.setText(text); }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.LIGHT_GRAY);
        return l;
    }
}
