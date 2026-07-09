package com.devmanchego.engine;

import java.io.*;

public class UCIProtocolHandler {

    private final Process engine;
    private final BufferedWriter writer;
    private final BufferedReader reader;

    public UCIProtocolHandler(String enginePath) throws Exception {

        engine = new ProcessBuilder(enginePath).start();

        writer = new BufferedWriter(
                new OutputStreamWriter(engine.getOutputStream())
        );

        reader = new BufferedReader(
                new InputStreamReader(engine.getInputStream())
        );

        send("uci");
        send("isready");
    }

    public void send(String cmd) throws Exception {
        writer.write(cmd + "\n");
        writer.flush();
    }

    public void setPosition(String fen) throws Exception {
        send("position fen " + fen);
    }

    public void goDepth(int depth) throws Exception {
        send("go depth " + depth);
    }

    public String readLine() throws Exception {
        return reader.readLine();
    }

    public void stop() throws Exception {
        send("stop");
    }
}
