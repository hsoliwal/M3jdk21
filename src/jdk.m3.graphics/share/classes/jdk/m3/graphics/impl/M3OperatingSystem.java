package jdk.m3.graphics.impl;

import java.util.Locale;

public enum M3OperatingSystem {
    WINDOWS,
    LINUX,
    MACOS,
    UNKNOWN;

    public static final M3OperatingSystem CURRENT;

    static {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("mac") || os.contains("darwin")) {
            CURRENT = MACOS;
        } else if (os.contains("windows")) {
            CURRENT = WINDOWS;
        } else if (os.contains("nux") || os.contains("nix")) {
            CURRENT = LINUX;
        } else {
            CURRENT = UNKNOWN;
        }
    }
}
