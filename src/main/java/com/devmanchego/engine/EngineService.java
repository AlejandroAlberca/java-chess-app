package com.devmanchego.engine;

import java.util.function.Consumer;

public class EngineService {

	private final StockfishEngine engine = new StockfishEngine();

	private volatile boolean running = false;

	private EngineDifficulty difficulty = EngineDifficulty.MEDIUM;

	public void start(String path) throws Exception {
		engine.start(path);
	}

	public void setDifficulty(EngineDifficulty difficulty) {
		this.difficulty = difficulty;
	}

	public void analyzeAsync(GameState state, Consumer<EngineResult> callback) {

		running = true;

		new Thread(() -> {
			try {

				engine.setPosition(state.toString());

				int depth = switch (difficulty) {
				case EASY -> 8;
				case MEDIUM -> 12;
				case HARD -> 18;
				case MASTER -> 22;
				};

				engine.goDepth(depth);

				EngineResult result = new EngineResult();

				String line;

				while ((line = engine.readLine()) != null && running) {

					if (line.contains("bestmove")) {
						result.bestMoveUCI = line.split(" ")[1];
					}

					if (line.contains("score cp")) {
						try {
							result.evalCp = Integer.parseInt(line.split("score cp ")[1].split(" ")[0]);
						} catch (Exception ignored) {
						}
					}

					if (line.contains("depth")) {
						try {
							result.depth = Integer.parseInt(line.split("depth ")[1].split(" ")[0]);
						} catch (Exception ignored) {
						}
					}

					callback.accept(result);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	public void stop() {
	    running = false;
	    try {
	        engine.stop();
	    } catch (Exception ignored) {}
	}
}
