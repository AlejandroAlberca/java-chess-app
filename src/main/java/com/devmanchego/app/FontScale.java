package com.devmanchego.app;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Global UI font-scaling factor. Components multiply their design-time base
 * font size by the current level's factor via {@link #scale(int)}, so a
 * single menu choice resizes text across the whole application.
 * Persisted across sessions via Java Preferences (same pattern as
 * {@link WelcomeDialog}'s "don't show again" flag).
 */
public final class FontScale {

    public enum Level {
        SMALL(0.85f), MEDIUM(1.0f), LARGE(1.15f), XLARGE(1.35f);
        public final float factor;
        Level(float factor) { this.factor = factor; }
    }

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(FontScale.class);
    private static final String PREF_LEVEL = "fontScaleLevel";

    private static Level current = loadInitial();
    private static final List<Runnable> listeners = new ArrayList<>();

    private FontScale() {}

    private static Level loadInitial() {
        String name = PREFS.get(PREF_LEVEL, Level.MEDIUM.name());
        try {
            return Level.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Level.MEDIUM;
        }
    }

    /** Scales a base (design-time) font size by the current level's factor. */
    public static int scale(int baseSize) {
        return Math.round(baseSize * current.factor);
    }

    public static Level getLevel() { return current; }

    public static void setLevel(Level level) {
        if (level == current) return;
        current = level;
        PREFS.put(PREF_LEVEL, level.name());
        listeners.forEach(Runnable::run);
    }

    /** Registers a callback invoked whenever the font scale changes. */
    public static void addChangeListener(Runnable r) { listeners.add(r); }
}
