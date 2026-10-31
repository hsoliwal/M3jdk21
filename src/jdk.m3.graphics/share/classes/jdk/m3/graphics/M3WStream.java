package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

public abstract class M3WStream extends M3Managed {
    public M3WStream(long ptr, long finalizer) {
        super(ptr, finalizer);
    }

    public M3WStream(long ptr, long finalizer, boolean managed) {
        super(ptr, finalizer, managed);
    }
}