package jdk.m3.graphics.skottie;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.resources.*;

public class M3AnimationBuilder extends M3Managed {
    static { M3Library.staticLoad(); }

    public M3AnimationBuilder(long ptr) {
        super(ptr, _FinalizerHolder.PTR);
    }

    public static class _FinalizerHolder {
        public static final long PTR = _nGetFinalizer();
    }

    public M3AnimationBuilder() {
        this(new M3AnimationBuilderFlag[0]);
    }

    public M3AnimationBuilder(M3AnimationBuilderFlag... builderFlags) {
        this(_nMake(M3AnimationBuilderFlag._collect(builderFlags)));
        M3Stats.onNativeCall();
    }

    /**
     * <p>Specify a font manager for loading animation fonts.</p>
     */
    public M3AnimationBuilder setFontManager(M3FontMgr fontMgr) {
        try {
            M3Stats.onNativeCall();
            _nSetFontManager(_ptr, M3Native.getPtr(fontMgr));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(fontMgr);
        }
    }

    /**
     * <p>Register a {@link Logger} with this builder.</p>
     */
    public M3AnimationBuilder setLogger(M3Logger logger) {
        try {
            M3Stats.onNativeCall();
            _nSetLogger(_ptr, M3Native.getPtr(logger));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(logger);
        }
    }

    public M3AnimationBuilder setResourceProvider(M3ResourceProvider resourceProvider) {
        try {
            M3Stats.onNativeCall();
            _nSetResourceProvider(_ptr, M3Native.getPtr(resourceProvider));
            return this;
        } finally {
            M3ReferenceUtil.reachabilityFence(resourceProvider);
        }
    }

    public M3Animation buildFromString(String data) {
        try {
            assert data != null : "Can’t buildFromString with data == null";
            M3Stats.onNativeCall();
            long ptr = _nBuildFromString(_ptr, data);
            if (ptr == 0)
                throw new IllegalArgumentException("Failed to create Animation from string: \"" + data + "\"");
            return new M3Animation(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Animation buildFromFile(String path) {
        try {
            assert path != null : "Can’t buildFromFile with path == null";
            M3Stats.onNativeCall();
            long ptr = _nBuildFromFile(_ptr, path);
            if (ptr == 0)
                throw new IllegalArgumentException("Failed to create Animation from path: " + path);
            return new M3Animation(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public M3Animation buildFromData(M3Data data) {
        try {
            assert data != null : "Can’t buildFromData with data == null";
            M3Stats.onNativeCall();
            long ptr = _nBuildFromData(_ptr, M3Native.getPtr(data));
            if (ptr == 0)
                throw new IllegalArgumentException("Failed to create Animation from data");
            return new M3Animation(ptr);
        } finally {
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    public static native long _nGetFinalizer();
    public static native long _nMake(int flags);
    public static native void _nSetFontManager(long ptr, long fontMgrPtr);
    public static native void _nSetLogger(long ptr, long loggerPtr);
    public static native void _nSetResourceProvider(long ptr, long resourceProviderPtr);
    public static native long _nBuildFromString(long ptr, String data);
    public static native long _nBuildFromFile(long ptr, String path);
    public static native long _nBuildFromData(long ptr, long dataPtr);
}