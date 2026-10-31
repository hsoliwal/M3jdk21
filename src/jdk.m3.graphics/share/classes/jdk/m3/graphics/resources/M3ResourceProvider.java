package jdk.m3.graphics.resources;

import jdk.m3.graphics.impl.*;

public abstract class M3ResourceProvider extends M3RefCnt {
    public M3ResourceProvider(long ptr) {
        super(ptr);
    }
}