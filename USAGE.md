# DevManchego Chess - Usage Guide

**Version 1.0 | 2025**

## Table of Contents

1. [Installation](#installation)
2. [Quick Start](#quick-start)
3. [Game Modes](#game-modes)
4. [Main Interface](#main-interface)
5. [Playing a Game](#playing-a-game)
6. [Difficulty Levels](#difficulty-levels)
7. [Analysis and Training](#analysis-and-training)
8. [Checkmate Drills](#checkmate-drills)
9. [Special Battles](#special-battles)
10. [Saving and Loading](#saving-and-loading)
11. [Keyboard Shortcuts](#keyboard-shortcuts)
12. [Common Tasks](#common-tasks)
13. [Troubleshooting](#troubleshooting)
14. [FAQ](#faq)

---

## Installation

### System Requirements

- **Java Runtime Environment (JRE)**: 17 or higher
- **Operating System**: Windows 10/11, macOS, or Linux
- **RAM**: 512 MB minimum, 2 GB recommended
- **Display**: 1280 × 800 pixels minimum resolution
- **Disk Space**: ~50 MB for application

### Installation Steps

#### Option 1: Pre-built JAR (Easiest)

1. Download `java-chess-app-1.0-SNAPSHOT.jar`
2. Double-click the JAR file, or run from terminal:
   ```bash
   java -jar java-chess-app-1.0-SNAPSHOT.jar
   ```
3. The application launches immediately

#### Option 2: Build from Source

1. Install Maven 3.6+ and Java 17+
2. Clone the repository:
   ```bash
   git clone https://github.com/devmanchego/java-chess-app.git
   cd java-chess-app
   ```
3. Build the project:
   ```bash
   mvn clean install
   ```
4. Run the application:
   ```bash
   java -jar target/java-chess-app-1.0-SNAPSHOT.jar
   ```

### Verify Installation

After launch, you should see:
- A welcome dialog with game mode options
- An 8×8 chess board with pieces
- An analysis panel on the right
- Menu bar at the top (File, Game, Training, Language)

---

## Quick Start

### Start Your First Game (30 seconds)

1. **Launch the application**
   - Double-click the JAR file or run from terminal

2. **Configure the game**
   - Welcome dialog appears automatically
   - Select **1 Player** (vs. AI) or **2 Players**
   - Choose **Difficulty** (for 1-player only): Beginner through Expert
   - Choose your **Color**: White (bottom) or Black (top)
   - Click **Start Game**

3. **Make your first move**
   - Click on a piece (it highlights yellow)
   - Purple circles show legal moves
   - Click the destination square
   - AI responds automatically (if 1-player)

4. **Analyze the position** (optional)
   - Click **Analyze** button on right panel
   - Green arrow shows best move
   - Toggle **Auto** to analyze after every move

5. **Undo or save**
   - **Game Menu** → **Undo Move** to take back moves
   - **File Menu** → **Save Game** to save progress

---

## Game Modes

### 1 Player vs. AI

**Ideal for:** Learning, practicing against computer, testing analysis

**Setup:**
- Select **1 Player** in Welcome Dialog
- Choose **Difficulty**: Beginner (very weak), Easy, Medium, Hard, or Expert (very strong)
- Choose **Color**: White or Black
- AI always plays the opposite color

**How it works:**
- You make a move
- AI thinks and responds (time depends on difficulty)
- If you move a black piece on white's turn, selection clears (illegal)
- Game continues until checkmate, stalemate, or draw

**Changing difficulty mid-game:**
- Go to **Game Menu** → **Difficulty**
- Select new level
- Change applies to AI's next move (current position unchanged)

### 2 Players

**Ideal for:** Playing with a friend on same computer

**Setup:**
- Select **2 Players** in Welcome Dialog
- No difficulty selection (no AI involved)
- Choose starting **Color**: White (bottom) or Black (top, rotated)
- Click **Start Game**

**How it works:**
- Player 1 makes a move
- Board stays in same orientation (no automatic flip)
- Player 2 makes a response move
- Alternate until game ends

**Board Orientation:**
- If Player 1 started as White, board stays in standard orientation (white at bottom)
- To play from black's perspective, select **Black** in setup
- Board automatically rotates (black pieces at bottom)

---

## Main Interface

### Layout Overview

```
┌─────────────────────────────────────────────┐
│ File  Game  Training  Language              │ Menu Bar
├──────────────────────────┬──────────────────┤
│                          │  Move Quality    │
│  Chess Board             │  History         │
│  (left, ~2/3 width)      │                  │ Analysis Panel
│                          │  Engine Info     │ (right, ~1/3 width)
│                          │  Principal Var.  │
│                          │  Captured Pieces │
│                          │  Move History    │
│                          │                  │
│  Status Bar              │                  │
│  (below board)           │ Analyze | Stop   │
│                          │ ☑ Auto  │ MultiPV: [1]
│                          │ Status: Idle     │
└──────────────────────────┴──────────────────┘
```

### Chess Board

- **8×8 grid** with file labels (a–h) and rank labels (1–8)
- **Coordinates**: a1 (bottom-left white) to h8 (top-right black)
- **Piece symbols**: Unicode chess symbols (♔ ♕ ♖ ♗ ♘ ♙ for white, lowercase symbols for black)

### Status Bar (Below Board)

Displays current game state:
- `White to move` / `Black to move`
- `White in check` / `Black in check`
- `Checkmate - White wins` / `Checkmate - Black wins`
- `Stalemate - Draw`
- `Threefold repetition - Draw`
- `50-move rule - Draw`
- `Insufficient material - Draw`
- `AI is thinking...` (during AI's turn)

### Analysis Panel (Right Side)

**Move Quality History** (Top):
- Shows accuracy % for each player
- Colored dot per move:
  - 🟢 Green: Best move or excellent move
  - 🔵 Blue: Good move
  - 🟡 Yellow: Inaccuracy
  - 🟠 Orange: Mistake
  - 🔴 Red: Blunder

**Engine Information**:
- `Engine: Java AI` (the built-in engine)
- Depth (how many half-moves deep the analysis went)
- Score in centipawns (cp): Positive = White advantage, Negative = Black advantage

**Principal Variation (PV)**:
- Best sequence of moves according to the engine
- Updated after analysis completes
- Example: `1. e2e4 e7e5 2. g1f3 b8c6`

**Captured Pieces**:
- White's captures: `♛ ♜ ♝ ♞ ♟` (pieces white captured from black)
- Black's captures: `♕ ♖ ♗ ♘ ♙` (pieces black captured from white)

**Move History**:
- Every move in UCI notation (e.g., `e2e4`, `e7e5`)
- Grows as game progresses
- Can be exported as PGN

---

## Playing a Game

### Making a Move

**Step 1: Select a piece**
```
1. Click on the piece you want to move
2. The selected square highlights yellow
3. Legal destination squares show purple circles
```

**Step 2: Move to destination**
```
4. Click on any purple circle
5. The move executes
6. Selected piece clears
```

**Special Cases:**

#### Pawn Promotion
When a pawn reaches the last rank:
- A dialog appears asking which piece to promote to
- Options: Queen (default), Rook, Bishop, Knight
- Select and click OK
- AI defaults to Queen if AI is promoting

#### Castling
- To castle kingside: Click king, then click square two squares to the right
- To castle queenside: Click king, then click square two squares to the left
- Rook automatically moves (you only move the king)
- Castling is automatic if legal

#### En Passant
- Appears when opponent's pawn moves two squares forward, landing beside your pawn
- Your pawn can capture diagonally forward (even though destination square is empty)
- Capture is automatic in move generation

### Undoing Moves

**Undo one move:**
```
Game Menu → Undo Move
```
- In 1-player mode: Undoes your move and AI's response (returns to your turn)
- In 2-player mode: Undoes one move only

**Undo multiple moves:**
```
Game Menu → Undo Move (multiple times)
```
- You can undo repeatedly to reach any earlier position

**Cannot undo:**
- Once a new game starts, prior games cannot be recovered
- Use **Save Game** before starting a new game if you want to keep it

---

## Difficulty Levels

### Choosing Your Level

When starting a 1-player game, select from:

| Level | Strength | Best For | Play Style |
|-------|----------|----------|-----------|
| **Beginner** | Very weak | Young children, learning | 40% random moves, many blunders |
| **Easy** | Weak | Casual players | 20% random moves, occasional errors |
| **Medium** | Balanced | Intermediate players | No randomness, balanced play |
| **Hard** | Strong | Advanced players | Solid, no randomness, competitive |
| **Expert** | Very strong | Expert players | Maximum depth, strongest engine |

### Difficulty Characteristics

**Beginner (Depth 3)**
- Makes many random moves (40% chance of random move)
- Frequent blunders (loses material)
- Ideal for teaching children
- Time per move: ~0.5 seconds

**Easy (Depth 4)**
- Occasionally errs (20% random moves)
- Plays recognizable strategies
- Good for casual players learning chess
- Time per move: ~1 second

**Medium (Depth 5)**
- No randomness, balanced play
- Can play a reasonable game
- Suitable for players learning strategy
- Time per move: ~1.5 seconds

**Hard (Depth 6)**
- Strong, competitive play
- Default difficulty
- Plays solid chess
- Real challenge for most players
- Time per move: ~2.5 seconds

**Expert (Depth 8)**
- Maximum search depth
- Uses all optimizations (killer moves, transposition table, history heuristic)
- Very challenging even for strong players
- Time per move: ~4 seconds

### Changing Difficulty

**During a game:**
```
Game Menu → Difficulty → [Select Level]
```
- Change takes effect on AI's next move
- Current position and move history preserved
- Can switch multiple times in same game

---

## Analysis and Training

### Live Analysis

**Enable automatic analysis:**
1. Click the **Auto** checkbox (right panel)
2. After each move, the engine analyzes at depth 6
3. Green arrow appears on board showing best move
4. Principal Variation displays top sequence

**Disable automatic analysis:**
- Uncheck the **Auto** checkbox
- Analysis stops, arrow disappears

### Analyze Button

**Manual analysis:**
1. Click **Analyze** button
2. Status shows "Analyzing..."
3. Engine searches at depth 6
4. Results display:
   - Green arrow for best move
   - Principal Variation (PV)
   - Centipawn score
   - Depth reached

**Stop analysis:**
1. Click **Stop** button (replaces Analyze during analysis)
2. Current analysis results saved
3. Can click Analyze again

### Move Quality Classification

After each move, the engine quickly (in background) classifies the move quality:

| Category | Loss Range | Color | Example |
|----------|-----------|-------|---------|
| Best Move | < 5 cp | 🟢 Bright Green | Objectively best move, no better alternative |
| Excellent | 6–20 cp | 🟢 Teal Green | Nearly best, minor improvement possible |
| Good | 21–50 cp | 🔵 Blue | Solid move, maintaining advantage |
| Inaccuracy | 51–100 cp | 🟡 Yellow | Weak move, opponent gets advantage |
| Mistake | 101–200 cp | 🟠 Orange | Serious error, significant material/position loss |
| Blunder | > 200 cp | 🔴 Red | Terrible move, game-losing |

**Accuracy %:**
- Percentage of your moves classified as "Best Move" or "Excellent"
- Displayed per side in move quality history

### MultiPV

**What is MultiPV?**
- Show multiple top lines simultaneously
- Instead of just the best move, see 2nd, 3rd, 4th best moves

**How to use:**
1. Select **MultiPV** from **Game Menu** or dropdown in Analysis Panel
2. Choose **1**, **2**, **3**, or **4**
3. Click **Analyze**
4. Engine displays top N variations

**Trade-off:**
- More lines = shallower search in same time
- 1 line reaches deeper than 4 lines in 2.5 seconds

**Example output with MultiPV=2:**
```
Line 1: e2e4 e7e5 (Best, +0.45)
Line 2: d2d4 c7c6 (Alternative, +0.30)
```

---

## Checkmate Drills

### What are Checkmate Drills?

Practice exercises where you must checkmate an opponent with limited material. Ideal for learning endgame patterns.

### How to Access

```
Training Menu → Checkmate Drills → [Select Drill Type]
```

### Available Drills

#### KQ vs K (King and Queen vs. King)

**Goal:** Checkmate black king with queen

**Setup:**
- White: King + Queen
- Black: King only

**Difficulty:** Easy (3–5 moves to mate)

**Learning:** Fundamental mating pattern, teaches queen coordination

**Strategy:**
- Queen cuts off king's escape squares
- Coordinate queen and king to squeeze opponent to edge
- Deliver mate when king trapped in corner

---

#### KR vs K (King and Rook vs. King)

**Goal:** Checkmate black king with rook

**Setup:**
- White: King + Rook
- Black: King only

**Difficulty:** Intermediate (10–15 moves to mate)

**Learning:** More complex than queen; requires careful technique

**Strategy:**
- Rook controls files and ranks from safe distance
- Gradually push black king toward edge
- Use your king to support rook
- Avoid stalemate (king has no legal moves but not in check)

---

#### KBB vs K (King and Two Bishops vs. King)

**Goal:** Checkmate black king with two bishops

**Setup:**
- White: King + Two Bishops (on opposite color squares)
- Black: King only

**Difficulty:** Intermediate-Hard (15–20 moves to mate)

**Learning:** Bishops must coordinate; one cuts escape, other delivers mate

**Strategy:**
- Bishops control diagonals of opposite colors
- Squeeze king toward edge using both bishops
- Use king sparingly (mostly bishops do the work)
- Mate occurs when bishops cover all escape squares

---

#### KBN vs K (King, Bishop, and Knight vs. King)

**Goal:** Checkmate black king with bishop and knight

**Setup:**
- White: King + Bishop + Knight
- Black: King only

**Difficulty:** Hard (20–30 moves to mate)

**Learning:** Most complex basic endgame; king must be driven to corner of bishop's color

**Strategy:**
- Bishop controls one color (light or dark)
- Knight and king work to push opposing king toward bishop's corner
- The corner square must match bishop's color (otherwise no mate)
- Requires precise coordination of all three pieces

---

### Playing a Drill

**Step 1: Start the drill**
```
Training Menu → Checkmate Drills → [Select KQ/KR/KBB/KBN]
```

**Step 2: Play as White**
- Deliver checkmate by moving your pieces
- AI plays as black king (tries to escape)

**Step 3: Use analysis if stuck**
```
Click Analyze button → See best move (green arrow)
```

**Step 4: Reset drill**
- To restart same drill: Training Menu → Checkmate Drills → [Select same drill]
- Board resets to starting position

**Tips:**
- Don't rush; think through the sequence
- Use **Undo Move** to try different approaches
- **Auto-analyze** shows if your move is optimal
- The AI plays defensively, making mate harder (realistic practice)

---

## Special Battles

### What are Special Battles?

Non-standard chess games where each side has unusual armies (e.g., 10 Knights vs. 6 Bishops + Pawns). Test your chess knowledge against asymmetric material imbalances.

### How to Access

```
Training Menu → Special Battles → [Select Battle Type]
```

### Available Battles

#### Knight Duel

| Aspect | Description |
|--------|-------------|
| **Side A (Knights)** | 10 knights: 2 flanking king + 8 on second rank |
| **Side B (Bishops + Pawns)** | 6 bishops on back rank + 12 pawns staggered |
| **Theme** | Minor pieces vs. pawn majority |
| **Strategy (A)** | Activate knights quickly, coordinate attacks |
| **Strategy (B)** | Push pawns to create passed pawns; use bishops to support |

---

#### Heavy Artillery

| Aspect | Description |
|--------|-------------|
| **Side A (Rooks)** | 5 rooks: 2 corner squares of first rank + 3 on second rank |
| **Side B (Queens + Pawns)** | 3 queens flanking king + 3 defensive pawns |
| **Theme** | Quantity (rooks) vs. power (queens) |
| **Strategy (A)** | Coordinate rooks on files/ranks; control center |
| **Strategy (B)** | Queen mobility; sacrifice queens if needed |

---

#### Pawn Wall

| Aspect | Description |
|--------|-------------|
| **Side A (Pawns)** | 24 pawns: three complete ranks (rows 3, 4, 5 full) |
| **Side B (Knights + Bishops)** | 4 knights + 4 bishops in symmetric formation |
| **Theme** | Brute force pawns vs. minor pieces |
| **Strategy (A)** | Pawn breaks, try to reach promotion |
| **Strategy (B)** | Pick off pawns, control advance |

---

#### Minor Piece Clash

| Aspect | Description |
|--------|-------------|
| **Side A (Knights)** | 8 knights: 2 flanking king + 6 on second rank |
| **Side B (Bishops)** | 8 bishops: 2 flanking king + 6 on second rank |
| **Theme** | Knights vs. bishops in equal number |
| **Strategy (A)** | Control center (knights prefer central outposts) |
| **Strategy (B)** | Use long diagonals (bishops excel at range) |

---

### Playing a Battle

**Step 1: Select battle**
```
Training Menu → Special Battles → [Select battle type]
```

**Step 2: Configure**
- Battle setup dialog appears
- Choose **Side A** or **Side B**
- Choose your **Piece Color** (White or Black)
- Click **Start**

**Step 3: Play**
- Make moves with your army
- AI responds with opponent's army
- Standard chess rules apply (check, checkmate, draws)
- Analyze with **Analyze** button if needed

**Step 4: Win or learn**
- Win by checkmating AI's king
- Lose if AI checkmates your king
- Draw if position is drawn (stalemate, threefold, 50-move, insufficient material)

**Restart battle:**
```
Training Menu → Special Battles → [Select same battle]
```

---

## Saving and Loading

### Save a Game

**To save current game:**

1. Go to **File Menu** → **Save Game**
2. Choose location and filename
3. Click **Save**
4. File saved as `.chess` (DevManchego Chess format)

**Example:**
```
My Game vs Easy.chess
Tournament Round 1.chess
Practice Sicilian.chess
```

### Load a Game

**To resume saved game:**

1. Go to **File Menu** → **Load Game**
2. Navigate to `.chess` file
3. Click **Open**
4. Game restored to exact saved position
5. Move history restored (analysis panel shows all moves)

**Note:** Position history (for threefold repetition) starts fresh after loading. If you play 2 more moves, that becomes the new game history.

### Export as PGN

**To export moves in standard PGN format:**

1. Go to **File Menu** → **Export PGN**
2. Choose location and filename
3. Click **Save**
4. File saved as `.pgn` (standard chess notation)

**PGN files can be:**
- Imported into chess analysis programs (Lichess, Chess.com, Arena, etc.)
- Shared with other players
- Analyzed in external engines

**Example PGN output:**
```
1. e2e4 e7e5 2. g1f3 b8c6 3. f1c4 f8c5
```

### Export Board as Image

**To save current board position as image:**

1. Go to **File Menu** → **Export Image**
2. Choose location and filename
3. Click **Save**
4. File saved as `.png` (PNG image format)

**PNG images can be:**
- Shared on social media
- Embedded in documents
- Used for analysis screenshots

---

## Keyboard Shortcuts

While the application uses primarily mouse input, key combinations are available:

| Keyboard | Action |
|----------|--------|
| `Escape` | Cancel selection (clear highlighted piece) |
| `Escape` | Close dialogs (if not in game) |
| N/A | No keyboard move entry (click to move) |

**Note:** DevManchego Chess prioritizes GUI interaction (clicking). Keyboard shortcuts are minimal.

---

## Common Tasks

### Change Language

**To switch interface language:**

1. Go to **Language Menu**
2. Select: **English**, **Español**, **Français**, **Deutsch**, or **Italiano**
3. UI updates immediately
4. No restart needed
5. Applies to all menus, dialogs, and status messages

### Change Difficulty Mid-Game

**To adjust AI challenge:**

1. Go to **Game Menu** → **Difficulty**
2. Select new level (Beginner through Expert)
3. Change applies to AI's next move
4. Position and move history unchanged

### View Move Quality History

**To see accuracy % and move classifications:**

1. Look at top of **Analysis Panel** (right side)
2. Shows accuracy % for White and Black
3. Colored dots under accuracy show each move's quality

### Analyze a Single Position

**To get engine assessment:**

1. Reach desired position (make moves or load game)
2. Click **Analyze** button
3. Engine searches and displays:
   - Best move (green arrow)
   - Principal Variation
   - Centipawn score
   - Depth reached

### Play Against Different Difficulties

**To test against all levels:**

1. Start 1-player game
2. Select **Beginner** difficulty
3. After game ends: **File Menu** → **New Game**
4. Repeat with **Easy**, **Medium**, **Hard**, **Expert**

### Train Checkmate

**To practice specific mating patterns:**

1. **Training Menu** → **Checkmate Drills**
2. Select **KQ vs K**, **KR vs K**, **KBB vs K**, or **KBN vs K**
3. Play white, deliver mate
4. Repeat different drills to learn all patterns

---

## Troubleshooting

### Game won't start

**Problem:** JAR file doesn't open

**Solutions:**
1. Verify Java 17+ installed:
   ```bash
   java -version
   ```
2. Run from command line:
   ```bash
   java -jar java-chess-app-1.0-SNAPSHOT.jar
   ```
3. Ensure JRE (not just JDK) is installed
4. Restart your computer and try again

### Board appears frozen

**Problem:** Board doesn't respond to clicks or AI is "thinking" too long

**Solutions:**
1. **AI still calculating:** Wait 5-10 seconds (check status bar)
2. **Stuck:** Try closing and reopening (no unsaved progress lost)
3. **Undo a move:** Go to **Game Menu** → **Undo Move**

### Pawn reached last rank but no promotion dialog

**Problem:** Pawn automatically promoted to queen, no choice given

**Expected behavior:** Current version auto-promotes to queen. You can:
- Undo the move (**Game Menu** → **Undo Move**)
- Accept queen promotion (no UI for choosing)

### Can't select opponent's pieces

**Problem:** Clicked on black piece but selection didn't change (white's turn)

**Expected behavior:** You can only select pieces of the player whose turn it is. If it's white's turn, only white pieces are selectable.

### Save/Load not working

**Problem:** Save Game or Load Game buttons greyed out or unresponsive

**Solutions:**
1. Ensure game is in progress (not paused or over)
2. Use **File Menu** → **Save Game** or **Load Game**
3. Check file write permissions in chosen directory
4. Try saving to Desktop or Documents folder

### Analysis arrow doesn't appear

**Problem:** Green best-move arrow not showing after analysis

**Solutions:**
1. Click **Analyze** button explicitly (not just Auto-analyze)
2. Wait for analysis to complete (status shows "Done")
3. Restart application and try again

### Difficulty menu doesn't work

**Problem:** Changing difficulty doesn't seem to take effect

**Solutions:**
1. Change applies to AI's **next move** only
2. Make a move, then AI's response will use new difficulty
3. Can verify by playing a few moves at different difficulties

---

## FAQ

### Can I play as Black?

**Yes.** In the Welcome Dialog or Battle Setup Dialog, select **Black** for your piece color. The board automatically rotates so your pieces appear at the bottom.

### How strong is the AI?

**Depends on difficulty:**
- **Beginner/Easy:** Weak, designed for learning
- **Medium:** Casual player level
- **Hard:** Intermediate-advanced player (recommended default)
- **Expert:** Very strong, challenging for experienced players

### Can I play online with friends?

**No.** DevManchego Chess supports local play only:
- 1 Player (vs. AI)
- 2 Players (same computer)

No network/online multiplayer available in this version.

### Does the AI cheat?

**No.** The AI plays entirely by chess rules. No hidden moves or illegal tricks. All moves can be verified in move history.

### Can I undo an AI move?

**Yes.** In 1-player mode, "Undo Move" in Game Menu undoes your move AND the AI's response, returning to your turn. You can undo multiple times to reach any earlier position.

### How do I save the position without all moves?

**Use "Export Image":**
1. **File Menu** → **Export Image**
2. Saves PNG of current board
3. Can share or print

To save for later play:
- **File Menu** → **Save Game** (saves all moves + position)

### Can I analyze a position without playing?

**Yes.** Load a saved game, reach a position, then click **Analyze**.

You can also:
1. Start a new game
2. Play a few moves
3. Click **Analyze** to get engine assessment

### What happens if I close without saving?

**Current game is lost.** No auto-save in current version. Before closing:
- **File Menu** → **Save Game** to preserve progress
- Unsaved games cannot be recovered

### Can I import PGN files?

**No.** Current version doesn't import PGN. But you can:
1. Play moves manually (click to move)
2. Export your game as PGN
3. Use PGN in other chess programs

### Is there a undo limit?

**No.** You can undo unlimited times, all the way to the starting position. Each undo pops one move from history.

### How do I get better at chess?

**Recommended training progression:**

1. **Start with Beginner:** Learn rules and basic tactics
2. **Play vs. Easy/Medium:** Build strategic understanding
3. **Use Checkmate Drills:** Learn fundamental mating patterns
   - Start with KQ vs K (easiest)
   - Progress to KR vs K, KBB vs K, KBN vs K
4. **Play vs. Hard:** Test your skills against solid AI
5. **Use Analysis:** Click **Analyze** on your games to see mistakes
6. **Play Special Battles:** Learn to evaluate asymmetric positions

### Can I modify the AI?

**Not via UI.** To adjust AI behavior, modify source code:
- `ChessAI.java` - Search depth, blunder rates
- Recompile and rebuild JAR

### Where are saved games stored?

**Anywhere you choose.** When you click **Save Game**, a file dialog opens where you select the destination:
- Desktop
- Documents
- Project folder
- Any location with write permissions

Default extension: `.chess`

### Can I play the same person twice?

**Yes.** Load any saved game or start a new game. Each game is independent.

### What's the fastest way to start?

**3 seconds:**
1. Double-click JAR
2. Welcome dialog auto-appears
3. Accept defaults, click **Start**

### Can I use a chess engine like Stockfish instead of the built-in AI?

**No.** Current version uses built-in "Java AI" only. No UCI protocol support.

### How do I report bugs?

Contact DevManchego or check the repository for issue tracker. Include:
- Exact steps to reproduce
- Expected vs. actual behavior
- Java version and OS

---

## Tips & Tricks

### Master the Analysis Panel

- **Always check move quality dots:** Patterns of orange/red dots show your weak areas
- **Use MultiPV=4:** See all top candidate moves, learn why some fail
- **Compare PVs:** Understand what the AI thinks is critical

### Prepare in Training Mode

Before playing a real game:
1. Practice Checkmate Drills until all patterns are familiar
2. Play a Special Battle to test different material imbalances
3. Then challenge yourself vs. Hard difficulty

### Learn from Mistakes

1. Play a game against Hard AI
2. Export as PGN
3. Review in **Analyze mode** after game
4. Undo to a mistake, click **Analyze** to see better moves

### Use Color Flipping for Symmetry

Playing Black? Choose Black in setup → board flips automatically. Helps when learning openings from black's perspective.

---

## Quick Reference

```
START NEW GAME        → File Menu → New Game (or launch app)
SAVE GAME            → File Menu → Save Game
LOAD GAME            → File Menu → Load Game
UNDO MOVES           → Game Menu → Undo Move
CHANGE DIFFICULTY    → Game Menu → Difficulty → [Select]
ANALYZE POSITION     → Click Analyze button (right panel)
AUTO-ANALYZE         → Check Auto checkbox (right panel)
EXPORT PGN           → File Menu → Export PGN
EXPORT IMAGE         → File Menu → Export Image
CHANGE LANGUAGE      → Language Menu → [Select language]
PLAY CHECKMATE DRILL → Training Menu → Checkmate Drills → [Select]
PLAY SPECIAL BATTLE  → Training Menu → Special Battles → [Select]
```

---

## Next Steps

1. **Install and launch** the application (follow Installation section)
2. **Play your first game** vs. Easy AI (follow Quick Start)
3. **Enable Auto-analyze** to learn move quality patterns
4. **Practice Checkmate Drills** (KQ vs K is easiest start)
5. **Challenge yourself** against Hard or Expert
6. **Export your games** as PGN to analyze later

---

**Enjoy your chess with DevManchego Chess!**

For questions, bugs, or feature requests, refer to the project repository or documentation.

**Version:** 1.0  
**Last Updated:** 2025-07-09
