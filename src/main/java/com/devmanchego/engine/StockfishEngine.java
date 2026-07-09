package com.devmanchego.engine;

public class StockfishEngine {

    private UCIProtocolHandler uci;

    public void start(String path) throws Exception {
        uci = new UCIProtocolHandler(path);
    }

    public void setPosition(String fen) throws Exception {
        uci.setPosition(fen);
    }

    public void goDepth(int depth) throws Exception {
        uci.goDepth(depth);
    }

    public String readLine() throws Exception {
        return uci.readLine();
    }

    public void stop() throws Exception {
        uci.stop();
    }
    
}
