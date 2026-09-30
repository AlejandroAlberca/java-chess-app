package com.devmanchego.app;

import com.devmanchego.engine.*;
import com.devmanchego.ui.*;
import com.devmanchego.ui.BattleSetupDialog;
import com.devmanchego.ui.WelcomeDialog.Choice;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;

public class ChessApp {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame(I18n.get("app.title"));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JLabel gameStatusLabel = new JLabel(I18n.get("status.white.move"));
            gameStatusLabel.setFont(new Font("Arial", Font.BOLD, FontScale.scale(18)));

            GameState game   = new GameState();
            ChessAI   gameAI = new ChessAI(false); // default: AI plays black

            ChessBoardUI board = new ChessBoardUI(game, frame, gameStatusLabel);

            AnalysisPanel analysis = new AnalysisPanel();

            ChessAI    analysisAI  = new ChessAI(true);
            EnginePanel enginePanel = new EnginePanel(analysisAI, game, analysis);

            board.setEnginePanel(enginePanel);
            board.setAnalysisPanel(analysis);
            enginePanel.setOnResult(board::setAnalysisResult);

            MoveListPanel moves = new MoveListPanel();

            JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT, enginePanel, analysis);
            JSplitPane main  = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, board, right);

            // ── Menu Bar ─────────────────────────────────────────────────────
            JMenuBar menuBar = new JMenuBar();

            // Difficulty items declared early so both newGame and game menu lambdas can use them
            ChessAI.Difficulty[] difficulties = ChessAI.Difficulty.values();
            JRadioButtonMenuItem[] diffItems  = new JRadioButtonMenuItem[difficulties.length];

            // File menu
            JMenu fileMenu = new JMenu(I18n.get("menu.file"));
            fileMenu.setMnemonic(KeyEvent.VK_F);

            JMenuItem newGame     = new JMenuItem(I18n.get("menu.newgame"));
            JMenuItem saveGame    = new JMenuItem(I18n.get("menu.savegame"));
            JMenuItem loadGame    = new JMenuItem(I18n.get("menu.loadgame"));
            JMenuItem exportGame  = new JMenuItem(I18n.get("menu.exportgame"));
            JMenuItem exportImage = new JMenuItem(I18n.get("menu.exportimage"));
            JMenuItem exit        = new JMenuItem(I18n.get("menu.exit"));

            newGame.addActionListener(e -> {
                Choice choice = WelcomeDialog.showForNewGame(frame);
                if (choice == null) return; // user closed dialog
                game.resetBoard();
                board.setAnalysisResult(null);
                board.resetSilentHistory();
                enginePanel.resetPrevScore();
                gameStatusLabel.setText(I18n.get("status.white.move"));
                if (choice.twoPlayers()) {
                    board.setAI(null);
                    board.setFlipped(false);
                } else {
                    boolean playerWhite = choice.playerIsWhite();
                    ChessAI newAI = new ChessAI(!playerWhite);
                    newAI.setDifficulty(choice.difficulty());
                    ChessAI.Difficulty[] diffs = ChessAI.Difficulty.values();
                    for (int i = 0; i < diffs.length; i++)
                        if (diffs[i] == choice.difficulty()) diffItems[i].setSelected(true);
                    board.setFlipped(!playerWhite);
                    board.setAI(newAI);
                }
                board.repaint();
                board.maybeStartAI();
            });

            FileNameExtensionFilter chessFilter =
                    new FileNameExtensionFilter("Chess saves (*.chess)", "chess");

            saveGame.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(I18n.get("menu.savegame"));
                chooser.setFileFilter(chessFilter);
                if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    if (!file.getName().endsWith(".chess"))
                        file = new File(file.getAbsolutePath() + ".chess");
                    try {
                        game.saveToFile(file);
                        JOptionPane.showMessageDialog(frame,
                                I18n.get("dialog.save.success", file.getName()),
                                I18n.get("menu.savegame"), JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame,
                                I18n.get("dialog.save.error", ex.getMessage()),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            loadGame.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(I18n.get("menu.loadgame"));
                chooser.setFileFilter(chessFilter);
                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    try {
                        game.loadFromFile(chooser.getSelectedFile());
                        board.setAnalysisResult(null);
                        board.resetSilentHistory();
                        enginePanel.resetPrevScore();
                        board.refreshHistoryPanels();
                        board.refreshStatus();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame,
                                I18n.get("dialog.load.error", ex.getMessage()),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            exportGame.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(I18n.get("menu.exportgame"));
                chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
                if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;

                File file = chooser.getSelectedFile();
                if (!file.getName().endsWith(".txt"))
                    file = new File(file.getAbsolutePath() + ".txt");

                String date = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String pgn  = game.getMoveHistory().toPGN();

                try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8)) {
                    pw.println("DevManchego Chess");
                    pw.println("Date: " + date);
                    pw.println("Moves: " + game.getMoveCount());
                    pw.println();
                    pw.println("──────────────────────────");
                    pw.println();
                    if (pgn.isBlank()) pw.println("(no moves played)");
                    else               pw.print(pgn);
                    JOptionPane.showMessageDialog(frame,
                            I18n.get("dialog.export.success", file.getName()),
                            I18n.get("menu.exportgame"), JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame,
                            I18n.get("dialog.export.error", ex.getMessage()),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            exportImage.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(I18n.get("menu.exportimage"));
                chooser.setFileFilter(new FileNameExtensionFilter("PNG image (*.png)", "png"));
                if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;

                File file = chooser.getSelectedFile();
                if (!file.getName().endsWith(".png"))
                    file = new File(file.getAbsolutePath() + ".png");

                try {
                    ImageIO.write(board.renderToImage(), "PNG", file);
                    JOptionPane.showMessageDialog(frame,
                            I18n.get("dialog.image.success", file.getName()),
                            I18n.get("menu.exportimage"), JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame,
                            I18n.get("dialog.image.error", ex.getMessage()),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            exit.addActionListener(e -> System.exit(0));

            fileMenu.add(newGame);
            fileMenu.addSeparator();
            fileMenu.add(saveGame);
            fileMenu.add(loadGame);
            fileMenu.add(exportGame);
            fileMenu.add(exportImage);
            fileMenu.addSeparator();
            fileMenu.add(exit);

            // Language menu
            JMenu langMenu = new JMenu(I18n.get("menu.language"));

            ButtonGroup langGroup = new ButtonGroup();
            for (I18n.Lang lang : I18n.Lang.values()) {
                JRadioButtonMenuItem item = new JRadioButtonMenuItem(lang.displayName);
                item.setSelected(lang == I18n.getLanguage());
                langGroup.add(item);
                item.addActionListener(e -> I18n.setLanguage(lang));
                langMenu.add(item);
            }

            // View menu (font size)
            JMenu viewMenu = new JMenu(I18n.get("menu.view"));
            JMenu fontSizeMenu = new JMenu(I18n.get("menu.fontsize"));
            ButtonGroup fontSizeGroup = new ButtonGroup();
            FontScale.Level[] fontLevels = FontScale.Level.values();
            JRadioButtonMenuItem[] fontSizeItems = new JRadioButtonMenuItem[fontLevels.length];
            for (int i = 0; i < fontLevels.length; i++) {
                FontScale.Level level = fontLevels[i];
                String key = "fontsize." + level.name().toLowerCase();
                JRadioButtonMenuItem item = new JRadioButtonMenuItem(I18n.get(key));
                item.setSelected(level == FontScale.getLevel());
                fontSizeGroup.add(item);
                fontSizeItems[i] = item;
                item.addActionListener(e -> FontScale.setLevel(level));
                fontSizeMenu.add(item);
            }
            viewMenu.add(fontSizeMenu);

            // Game menu
            JMenu gameMenu = new JMenu(I18n.get("menu.game"));

            // Difficulty submenu
            JMenu diffMenu = new JMenu(I18n.get("menu.difficulty"));
            ButtonGroup diffGroup = new ButtonGroup();
            for (int i = 0; i < difficulties.length; i++) {
                ChessAI.Difficulty diff = difficulties[i];
                String key = "difficulty." + diff.name().toLowerCase();
                JRadioButtonMenuItem item = new JRadioButtonMenuItem(I18n.get(key));
                item.setSelected(diff == gameAI.getDifficulty());
                diffGroup.add(item);
                diffItems[i] = item;
                item.addActionListener(e -> {
                    gameAI.setDifficulty(diff);
                    // Also apply to whichever AI is actually playing right now —
                    // gameAI is only used as a fallback/default, not always the live instance.
                    if (board.getAI() != null) board.getAI().setDifficulty(diff);
                });
                diffMenu.add(item);
            }

            JMenuItem offerDraw = new JMenuItem(I18n.get("menu.offerdraw"));

            // Undo submenu
            JMenu undoMenu = new JMenu(I18n.get("menu.undomoves"));
            JMenuItem undoLast   = new JMenuItem(I18n.get("menu.undo.last"));
            JMenuItem undoTurn   = new JMenuItem(I18n.get("menu.undo.turn"));
            JMenuItem undo2turns = new JMenuItem(I18n.get("menu.undo.2turns"));
            JMenuItem undo5turns = new JMenuItem(I18n.get("menu.undo.5turns"));
            JMenuItem undo10turns= new JMenuItem(I18n.get("menu.undo.10turns"));
            undoMenu.add(undoLast);
            undoMenu.add(undoTurn);
            undoMenu.add(undo2turns);
            undoMenu.add(undo5turns);
            undoMenu.add(undo10turns);

            // Undo action helper
            // "turn" = 2 plies, but in 1-player mode a turn is AI+human = 2 plies
            // We always undo N plies as labelled
            java.util.function.BiConsumer<Integer, String> doUndo = (plies, label) -> {
                int available = game.getMoveCount();
                if (available < plies) {
                    JOptionPane.showMessageDialog(frame,
                            I18n.get("dialog.undo.notenough", plies, available),
                            I18n.get("menu.undomoves"), JOptionPane.WARNING_MESSAGE);
                    return;
                }
                game.undoMoves(plies);
                board.setAnalysisResult(null);
                board.resetSilentHistory();
                enginePanel.resetPrevScore();
                board.refreshStatus();
                board.repaint();
            };

            undoLast.addActionListener(e   -> doUndo.accept(1,  "1"));
            undoTurn.addActionListener(e   -> doUndo.accept(2,  "2"));
            undo2turns.addActionListener(e -> doUndo.accept(4,  "4"));
            undo5turns.addActionListener(e -> doUndo.accept(10, "10"));
            undo10turns.addActionListener(e-> doUndo.accept(20, "20"));

            // Draw offer
            offerDraw.addActionListener(e -> {
                ChessAI opponent = board.getAI();
                if (opponent == null) {
                    // 2-player mode: no AI opponent to decide for.
                    JOptionPane.showMessageDialog(frame,
                            I18n.get("dialog.draw.noopponent"),
                            I18n.get("menu.offerdraw"), JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(frame,
                        I18n.get("dialog.draw.offer"),
                        I18n.get("menu.offerdraw"), JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;

                // Score from white's perspective (positive = white winning),
                // converted to the opponent AI's own perspective regardless of its color.
                int scoreWhite  = analysisAI.staticEvalFromWhite(game);
                int aiAdvantage = opponent.isWhite() ? scoreWhite : -scoreWhite;

                boolean aiAccepts;
                if (aiAdvantage > 80) {
                    // AI is winning by >0.8 pawns → rejects
                    aiAccepts = false;
                } else if (aiAdvantage < -80) {
                    // AI is losing → accepts
                    aiAccepts = true;
                } else {
                    // Roughly equal → 60% chance to accept
                    aiAccepts = Math.random() < 0.60;
                }

                if (aiAccepts) {
                    gameStatusLabel.setText(I18n.get("status.draw"));
                    JOptionPane.showMessageDialog(frame,
                            I18n.get("dialog.draw.accepted"),
                            I18n.get("menu.offerdraw"), JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(frame,
                            I18n.get("dialog.draw.rejected"),
                            I18n.get("menu.offerdraw"), JOptionPane.INFORMATION_MESSAGE);
                }
            });

            gameMenu.add(diffMenu);
            gameMenu.addSeparator();
            gameMenu.add(offerDraw);
            gameMenu.addSeparator();
            gameMenu.add(undoMenu);

            // ── Training menu ──────────────────────────────────────────────────
            JMenu trainingMenu = new JMenu(I18n.get("menu.training"));

            // ── Special Battles submenu ────────────────────────────────────────
            JMenu battlesMenu = new JMenu(I18n.get("menu.special.battles"));

            JMenuItem battleCavalry   = new JMenuItem(I18n.get("battle.cavalry.name"));
            JMenuItem battleArtillery = new JMenuItem(I18n.get("battle.artillery.name"));
            JMenuItem battlePawnWall  = new JMenuItem(I18n.get("battle.pawnwall.name"));
            JMenuItem battleMinor     = new JMenuItem(I18n.get("battle.minor.name"));

            battlesMenu.add(battleCavalry);
            battlesMenu.add(battleArtillery);
            battlesMenu.add(battlePawnWall);
            battlesMenu.add(battleMinor);
            trainingMenu.add(battlesMenu);
            trainingMenu.addSeparator();

            // Helper: confirm abandon if game active, show setup dialog, start battle
            java.util.function.Consumer<GameState.BattleType> startBattle = type -> {
                boolean whiteTurnNow = game.isWhiteTurn();
                boolean gameActive   = game.getMoveCount() > 0
                        && !game.isCheckmate(whiteTurnNow)
                        && !game.isStalemate(whiteTurnNow)
                        && !game.isAutomaticDraw();
                if (gameActive) {
                    int ok = JOptionPane.showConfirmDialog(frame,
                            I18n.get("dialog.drill.confirm"),
                            I18n.get("menu.special.battles"),
                            JOptionPane.YES_NO_OPTION);
                    if (ok != JOptionPane.YES_OPTION) return;
                }
                BattleSetupDialog.BattleChoice choice = BattleSetupDialog.show(frame, type);
                if (choice == null) return;

                boolean whiteIsA = (choice.playerSideA() == choice.playerIsWhite());
                game.setupBattlePosition(type, whiteIsA);

                ChessAI battleAI = new ChessAI(!choice.playerIsWhite());
                battleAI.setDifficulty(ChessAI.Difficulty.HARD);
                board.setAI(battleAI);
                board.setFlipped(!choice.playerIsWhite());
                board.setAnalysisResult(null);
                board.resetSilentHistory();
                enginePanel.resetPrevScore();
                gameStatusLabel.setText(I18n.get("status.white.move"));
                board.repaint();
                board.maybeStartAI();
            };

            battleCavalry  .addActionListener(e -> startBattle.accept(GameState.BattleType.CAVALRY_DUEL));
            battleArtillery.addActionListener(e -> startBattle.accept(GameState.BattleType.HEAVY_ARTILLERY));
            battlePawnWall .addActionListener(e -> startBattle.accept(GameState.BattleType.PAWN_WALL));
            battleMinor    .addActionListener(e -> startBattle.accept(GameState.BattleType.MINOR_PIECE_CLASH));

            // Refresh battles menu texts on language change
            I18n.addChangeListener(() -> {
                battlesMenu.setText(I18n.get("menu.special.battles"));
                battleCavalry  .setText(I18n.get("battle.cavalry.name"));
                battleArtillery.setText(I18n.get("battle.artillery.name"));
                battlePawnWall .setText(I18n.get("battle.pawnwall.name"));
                battleMinor    .setText(I18n.get("battle.minor.name"));
            });

            // ── Checkmate Drills submenu ───────────────────────────────────────
            JMenu drillsMenu = new JMenu(I18n.get("menu.checkmate.drills"));
            JMenuItem drillKQ  = new JMenuItem(I18n.get("menu.drill.kq"));
            JMenuItem drillKR  = new JMenuItem(I18n.get("menu.drill.kr"));
            JMenuItem drillKBB = new JMenuItem(I18n.get("menu.drill.kbb"));
            JMenuItem drillKBN = new JMenuItem(I18n.get("menu.drill.kbn"));
            drillsMenu.add(drillKQ);
            drillsMenu.add(drillKR);
            drillsMenu.add(drillKBB);
            drillsMenu.add(drillKBN);
            trainingMenu.add(drillsMenu);

            // Helper: confirm abandon if a game is actively in progress (moves played AND not yet over)
            java.util.function.Consumer<GameState.PracticeMode> startDrill = mode -> {
                boolean whiteTurnNow = game.isWhiteTurn();
                boolean gameActive   = game.getMoveCount() > 0
                        && !game.isCheckmate(whiteTurnNow)
                        && !game.isStalemate(whiteTurnNow)
                        && !game.isAutomaticDraw();
                if (gameActive) {
                    int ok = JOptionPane.showConfirmDialog(frame,
                            I18n.get("dialog.drill.confirm"),
                            I18n.get("dialog.drill.title"),
                            JOptionPane.YES_NO_OPTION);
                    if (ok != JOptionPane.YES_OPTION) return;
                }
                game.setupPracticePosition(mode);
                // Drill: player is always White, AI plays the lone Black king
                ChessAI drillAI = new ChessAI(false); // false = AI is black
                drillAI.setDifficulty(ChessAI.Difficulty.EXPERT); // king plays optimally
                board.setAI(drillAI);
                board.setFlipped(false);
                board.setAnalysisResult(null);
                board.resetSilentHistory();
                enginePanel.resetPrevScore();
                gameStatusLabel.setText(I18n.get("status.white.move"));
                board.repaint();
            };

            drillKQ .addActionListener(e -> startDrill.accept(GameState.PracticeMode.KQ_VS_K));
            drillKR .addActionListener(e -> startDrill.accept(GameState.PracticeMode.KR_VS_K));
            drillKBB.addActionListener(e -> startDrill.accept(GameState.PracticeMode.KBB_VS_K));
            drillKBN.addActionListener(e -> startDrill.accept(GameState.PracticeMode.KBN_VS_K));

            // Refresh training menu texts on language change
            I18n.addChangeListener(() -> {
                trainingMenu.setText(I18n.get("menu.training"));
                drillsMenu.setText(I18n.get("menu.checkmate.drills"));
                drillKQ .setText(I18n.get("menu.drill.kq"));
                drillKR .setText(I18n.get("menu.drill.kr"));
                drillKBB.setText(I18n.get("menu.drill.kbb"));
                drillKBN.setText(I18n.get("menu.drill.kbn"));
            });

            menuBar.add(fileMenu);
            menuBar.add(gameMenu);
            menuBar.add(trainingMenu);
            menuBar.add(viewMenu);
            menuBar.add(langMenu);
            frame.setJMenuBar(menuBar);

            // Base (unscaled) menu font, captured once from the Look & Feel defaults
            // so repeated scale changes always derive from the same starting point.
            Font baseMenuFont = UIManager.getFont("MenuItem.font");
            if (baseMenuFont == null) baseMenuFont = menuBar.getFont();
            applyMenuFontScale(menuBar, baseMenuFont);

            // Refresh game menu texts on language change
            I18n.addChangeListener(() -> {
                gameMenu.setText(I18n.get("menu.game"));
                diffMenu.setText(I18n.get("menu.difficulty"));
                for (int i = 0; i < difficulties.length; i++)
                    diffItems[i].setText(I18n.get("difficulty." + difficulties[i].name().toLowerCase()));
                offerDraw.setText(I18n.get("menu.offerdraw"));
                undoMenu.setText(I18n.get("menu.undomoves"));
                undoLast.setText(I18n.get("menu.undo.last"));
                undoTurn.setText(I18n.get("menu.undo.turn"));
                undo2turns.setText(I18n.get("menu.undo.2turns"));
                undo5turns.setText(I18n.get("menu.undo.5turns"));
                undo10turns.setText(I18n.get("menu.undo.10turns"));
                viewMenu.setText(I18n.get("menu.view"));
                fontSizeMenu.setText(I18n.get("menu.fontsize"));
                for (int i = 0; i < fontLevels.length; i++)
                    fontSizeItems[i].setText(I18n.get("fontsize." + fontLevels[i].name().toLowerCase()));
            });

            // ── Language change listener ──────────────────────────────────────
            I18n.addChangeListener(() -> {
                frame.setTitle(I18n.get("app.title"));
                fileMenu.setText(I18n.get("menu.file"));
                langMenu.setText(I18n.get("menu.language"));
                newGame.setText(I18n.get("menu.newgame"));
                saveGame.setText(I18n.get("menu.savegame"));
                loadGame.setText(I18n.get("menu.loadgame"));
                exportGame.setText(I18n.get("menu.exportgame"));
                exportImage.setText(I18n.get("menu.exportimage"));
                exit.setText(I18n.get("menu.exit"));
                enginePanel.refreshTexts();
                analysis.refreshTexts();
                board.refreshStatus();
            });

            // ── Font scale change listener ────────────────────────────────────
            Font finalBaseMenuFont = baseMenuFont;
            FontScale.addChangeListener(() -> {
                gameStatusLabel.setFont(new Font("Arial", Font.BOLD, FontScale.scale(18)));
                applyMenuFontScale(menuBar, finalBaseMenuFont);
                frame.revalidate();
                frame.repaint();
            });

            frame.add(main, BorderLayout.CENTER);
            frame.add(gameStatusLabel, BorderLayout.SOUTH);
            frame.setSize(1200, 800);
            frame.setVisible(true);

            // ── Welcome dialog ────────────────────────────────────────────────
            Choice welcome = WelcomeDialog.show(frame);
            if (welcome == null) {
                // Suppressed or closed → default: 1-player, white, Hard
                board.setAI(gameAI);
                board.maybeStartAI();
            } else if (welcome.twoPlayers()) {
                board.setAI(null);
                board.setFlipped(false);
                gameStatusLabel.setText(I18n.get("status.white.move"));
            } else {
                // 1-player: create AI with correct color
                boolean playerWhite = welcome.playerIsWhite();
                ChessAI chosenAI = playerWhite ? gameAI : new ChessAI(true); // true = AI plays white
                chosenAI.setDifficulty(welcome.difficulty());
                board.setFlipped(!playerWhite);
                // Sync difficulty menu
                ChessAI.Difficulty[] diffs = ChessAI.Difficulty.values();
                for (int i = 0; i < diffs.length; i++)
                    if (diffs[i] == welcome.difficulty()) diffItems[i].setSelected(true);
                board.setAI(chosenAI);
                board.maybeStartAI();
            }
        });
    }

    /**
     * Recursively applies the current FontScale to every menu, menu item and
     * submenu reachable from {@code element}, always deriving from the pristine
     * {@code base} font so repeated scale changes never compound.
     */
    private static void applyMenuFontScale(MenuElement element, Font base) {
        Component c = element.getComponent();
        if (c != null) {
            c.setFont(base.deriveFont((float) FontScale.scale(base.getSize())));
        }
        for (MenuElement sub : element.getSubElements()) {
            applyMenuFontScale(sub, base);
        }
    }
}
