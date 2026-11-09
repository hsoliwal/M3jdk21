package jdk.m3.graphics.resources;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public class M3CachingResourceProvider extends M3ResourceProvider {
    static { M3Library.staticLoad(); }

    public M3CachingResourceProvider(long ptr) {
        super(ptr);
    }

    public static M3CachingResourceProvider make(M3ResourceProvider resourceProvider) {
        assert resourceProvider != null : "Can’t CachingResourceProvider::make with resourceProvider == null";
        M3Stats.onNativeCall();
        return new M3CachingResourceProvider(_nMake(M3Native.getPtr(resourceProvider)));
    }

    public static native long _nMake(long resourceProviderPtr);
}