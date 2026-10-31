// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.filesystem;

import com.m3.fs.M3FS;
import java.io.IOException;
import java.io.ShadowInputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ProgressMonitor;
import org.eclipse.core.filesystem.EFS;

/** Executes the original Eclipse streams and JNI inode operations with M3 stream APIs. */
public final class M3NativeProgressShadow {
    private M3NativeProgressShadow() { }
    private static int checks;
    private static void require(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }
    private static final class Monitor implements ProgressMonitor {
        long work;
        int completions;
        boolean cancelled;
        @Override public void worked(long units) { work += units; }
        @Override public void done() { completions++; }
        @Override public boolean isCancelled() { return cancelled; }
    }
    public static void main(String[] args) throws Exception {
        require(M3FS.nativeAvailable());
        try (var filesystem = new M3EclipseFileSystem()) {
            filesystem.initialize("m3fs");
            M3FS image = M3FS.create(Path.of(args[0]));
            var root = filesystem.mount("work", image);
            var file = root.getChild("layers.bin");
            byte[] payload = new byte[4097];
            for (int i = 0; i < payload.length; i++) payload[i] = (byte) (i * 31);
            var writes = new Monitor();
            try (var output = file.openOutputStream(EFS.NONE, null)
                    .withProgress(writes.child("write", 100, payload.length))) {
                output.write(payload);
            }
            require(writes.work == 100 && writes.completions == 0);
            var reads = new Monitor();
            try (var main = new ShadowInputStream(file.openInputStream(EFS.NONE, null)
                    .withProgress(reads.child("read", 100, payload.length)), 64, 2);
                 var first = main.shadow(); var second = main.shadow()) {
                for (int at = 0; at < payload.length;) {
                    int size = Math.min(31, payload.length - at);
                    byte[] expected = Arrays.copyOfRange(payload, at, at + size);
                    require(Arrays.equals(expected, main.readNBytes(size)));
                    require(Arrays.equals(expected, first.readNBytes(size)));
                    require(Arrays.equals(expected, second.readNBytes(size)));
                    at += size;
                }
                require(main.read() == -1 && first.read() == -1 && second.read() == -1);
            }
            require(reads.work == 100 && reads.completions == 0);
            long generation = image.stat("/layers.bin").generation();
            try (var output = file.openOutputStream(EFS.APPEND, null)) { output.write(7); }
            require(image.stat("/layers.bin").generation() > generation);
            var cancelled = new Monitor();
            cancelled.cancelled = true;
            try (var input = file.openInputStream(EFS.NONE, null).withProgress(cancelled)) {
                try { input.read(); throw new AssertionError("cancellation ignored"); }
                catch (CancellationException expected) { require(cancelled.work == 0); }
            }
            try (var stale = file.openInputStream(EFS.NONE, null)) {
                file.delete(EFS.NONE, null);
                try (var output = file.openOutputStream(EFS.NONE, null)) { output.write(9); }
                try { stale.read(); throw new AssertionError("replacement ignored"); }
                catch (IOException expected) { require(true); }
            }
        }
        System.out.println("M3_NATIVE_PROGRESS_SHADOW_PASS checks=" + checks);
    }
}
