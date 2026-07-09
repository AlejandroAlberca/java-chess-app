# DevManchego Chess

A feature-rich chess application built in Java with Swing, featuring a from-scratch AI engine, live analysis, training modes, and full draw detection.

## Features

### Core Gameplay
- **1-player vs AI** or **2-player** modes
- Full legal-move validation with castling, en passant, and pawn promotion (including underpromotion)
- Board flip for playing as Black
- Undo moves (1, 2, 5, or 10 at a time)
- Save/load games in custom binary format
- Export moves as PGN or board as PNG image

### AI Engine
- **Negamax search** with alpha-beta pruning and iterative deepening
- **Transposition table** (1M entries) with Zobrist hashing (including castling rights and en passant)
- **Killer moves** and **history heuristic** for move ordering
- **Quiescence search** to evaluate captures after horizon
- **Piece-square tables** and positional evaluation (pawn structure, king safety, rook open files, bishop pair)
- **Game-phase interpolation** for middlegame/endgame king placement
- **Opening book** with 13 main-line theoretical positions (user can play or AI auto-selects)
- **5 difficulty levels** (Beginner–Expert) with optional blunders at lower levels

### Analysis & Training
- **Live move analysis**: per-move quality rating (Best → Excellent → Good → Inaccuracy → Mistake → Blunder)
- **Analyze button** for depth-6 search with principal variation (PV) and MultiPV
- **Auto-analyze** mode when enabled
- **Best-move arrow** overlay on board
- **Move quality history** with accuracy % per side
- **Checkmate drills**: KQ vs K, KR vs K, KBB vs K, KBN vs K (AI plays the lone king)
- **Special battles**: Cavalry Duel (10 Knights vs 6 Bishops + Pawns), Heavy Artillery (5 Rooks vs 3 Queens), Pawn Wall (24 Pawns vs 4 Knights + Bishops), Minor Piece Clash (8 Knights vs 8 Bishops)

### Draw Detection
- **Threefold repetition**: same position appears 3 times
- **50-move rule**: 50 moves with no pawn move or capture
- **Insufficient material**: K vs K, K+minor vs K, K+B vs K+B (same-colored bishops)
- All draw conditions are automatically detected and announced

### Internationalization
- **5 languages**: English, Spanish, French, German, Italian
- Language can be switched at runtime via the menu

## Requirements

- **Java 17+**
- **Maven 3.6+** (for building)

## Building & Running

```bash
cd java-chess-app
mvn clean install
java -jar target/java-chess-app-1.0-SNAPSHOT.jar
```

Or run directly from the IDE:
```bash
mvn clean javafx:run
```

(Note: The app uses Swing, not JavaFX, so `mvn clean compile && mvn exec:java` is the fallback if `javafx:run` doesn't work.)

## Project Structure

```
src/main/java/com/devmanchego/
├── app/
│   ├── ChessApp.java          Main application entry point
│   └── I18n.java              Internationalization strings
├── engine/
│   ├── ChessAI.java           AI search (negamax, TT, opening book)
│   ├── GameState.java         Board state, legality, move application
│   ├── Move.java              Move representation
│   ├── Piece.java             Piece abstraction
│   ├── PieceType.java         Enum for 6 piece types
│   ├── ZobristHasher.java     Zobrist hashing (incremental + full)
│   ├── TranspositionTable.java Fixed-size TT with depth-based replacement
│   ├── MoveHistory.java       PGN generation from moves
│   ├── AnalysisResult.java    Result of search (move, score, PV, quality)
│   └── EngineDifficulty.java  Difficulty level enum
└── ui/
    ├── ChessBoardUI.java      Board rendering, move input, quality dots
    ├── AnalysisPanel.java     PV display, captured pieces, move history
    ├── EnginePanel.java       Analyze/Stop buttons, MultiPV selector
    ├── WelcomeDialog.java     Game mode selection
    ├── BattleSetupDialog.java Battle mode side/color selection
    ├── MoveListPanel.java     Move list display
    └── PieceImageLoader.java  Unicode chess symbols
```

## How to Play

1. **Start a game**: "New Game" → select 1-player (choose difficulty) or 2-player, then choose your color (AI always plays opposite).
2. **Make a move**: Click a piece to highlight legal moves (blue dots), then click the destination square.
3. **Pawn promotion**: When a pawn reaches the last rank, a dialog appears to choose the piece (Queen/Rook/Bishop/Knight). AI defaults to Queen.
4. **Analysis**: Click "Analyze" to search at depth 6. Toggle "Auto" to analyze after each move.
5. **Undo**: Use the Game menu to undo 1, 2, 5, or 10 half-moves.
6. **Training**: Try Checkmate Drills (force checkmate with limited material) or Special Battles (asymmetric army setups).
7. **Language**: Change at runtime via the menu.

## Performance Notes

- The AI can search to **depth 6–8** in 2.5 seconds (difficulty-dependent).
- Zobrist hash is maintained **incrementally** during play (no O(64) recompute per node).
- Killer moves and history heuristic **significantly reduce branching factor** compared to plain MVV-LVA.
- For endgames or tactics, use the Analyze button; the move classifier runs at depth 2 for speed.

## Known Limitations

- Opening book is manually curated and small; no auto-generation from PGN databases.
- No UCI engine protocol (not meant to integrate with external tools).
- No network play.
- Saved games do not persist position history (halfmove clock and threefold repetition restart from the loaded position).

## License

MIT License — see LICENSE file for details.

## Author

DevManchego Chess — a clean, educational chess engine and UI in pure Java.

---

**Enjoy!** Feel free to fork, modify, or use this as a learning resource for chess AI and Swing UI design.
