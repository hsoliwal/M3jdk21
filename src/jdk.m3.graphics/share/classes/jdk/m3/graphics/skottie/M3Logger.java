package jdk.m3.graphics.skottie;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

/**
 * <p>A Logger subclass can be used to receive
 * {@link io.github.humbleui.skija.skottie.AnimationBuilder} parsing errors and warnings.</p>
 */
public abstract class M3Logger extends M3RefCnt {
    static { M3Library.staticLoad(); }

    public M3Logger() {
        super(_nMake());
        M3Stats.onNativeCall();
        M3Stats.onNativeCall();
        _nInit(_ptr);
    }

    public abstract void log(M3LogLevel level, String message, String json);

    public static native long _nMake();
    public        native void _nInit(long ptr);
}