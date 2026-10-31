package jdk.m3.graphics.resources;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public class M3DataURIResourceProviderProxy extends M3ResourceProvider {
    static { M3Library.staticLoad(); }

    public M3DataURIResourceProviderProxy(long ptr) {
        super(ptr);
    }

    public static M3DataURIResourceProviderProxy make(M3ResourceProvider resourceProvider) {
        return make(resourceProvider, false);
    }

    public static M3DataURIResourceProviderProxy make(M3ResourceProvider resourceProvider, boolean predecode) {
        assert resourceProvider != null : "Can’t DataURIResourceProviderProxy::make with resourceProvider == null";
        M3Stats.onNativeCall();
        return new M3DataURIResourceProviderProxy(_nMake(M3Native.getPtr(resourceProvider), predecode));
    }

    public static native long _nMake(long resourceProviderPtr, boolean predecode);
}