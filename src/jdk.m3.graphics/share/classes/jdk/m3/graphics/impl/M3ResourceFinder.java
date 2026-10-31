package jdk.m3.graphics.impl;

import java.io.*;
import java.net.*;

public final class M3ResourceFinder implements Closeable {
    public M3ResourceFinder(String moduleName) {
    }

    public URL findResource(String path) throws IOException {
        return M3ResourceFinder.class.getClassLoader().getResource(path);
    }

    @Override
    public void close() throws IOException {
    }
}
