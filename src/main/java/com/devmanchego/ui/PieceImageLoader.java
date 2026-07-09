package com.devmanchego.ui;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PieceImageLoader {

    private static final Map<String, BufferedImage> images = new HashMap<>();

    static {
        load("wK");
        load("wQ");
        load("wR");
        load("wB");
        load("wN");
        load("wP");
        load("bK");
        load("bQ");
        load("bR");
        load("bB");
        load("bN");
        load("bP");
    }

    private static void load(String name) {
        try {
            var stream =
                    PieceImageLoader.class.getResourceAsStream(
                            "/pieces/" + name + ".png"
                    );

            if (stream == null) {
                System.err.println("Missing resource: " + name);
                return;
            }

            images.put(name, ImageIO.read(stream));

        } catch (Exception e) {
            System.err.println("Error loading: " + name);
        }
    }

    public static BufferedImage get(String key) {
        return images.get(key);
    }
}