package jdk.m3.graphics.impl;

import java.lang.ref.Cleaner;

public final class M3Cleanable {
    private static final Cleaner cleaner = Cleaner.create();

    public static M3Cleanable register(Object obj, Runnable action) {
        return new M3Cleanable(cleaner.register(obj, action));
    }

    private final Cleaner.Cleanable cleanable;

    private M3Cleanable(Cleaner.Cleanable cleanable) {
        this.cleanable = cleanable;
    }

    public void clean() {
        this.cleanable.clean();
    }
}
