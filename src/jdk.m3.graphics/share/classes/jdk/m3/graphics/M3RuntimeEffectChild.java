package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

/**
 * Uniform child (shader or colorFilter) in the effect's SkSL
 */
public abstract class M3RuntimeEffectChild extends M3RefCnt implements M3Flattenable {
    protected M3RuntimeEffectChild(long ptr) {
        super(ptr);
    }

    protected M3RuntimeEffectChild(long ptr, boolean allowClose) {
        super(ptr, allowClose);
    }
}
