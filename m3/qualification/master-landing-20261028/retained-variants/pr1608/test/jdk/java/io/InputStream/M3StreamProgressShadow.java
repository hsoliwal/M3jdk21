/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
/*
 * @test
 * @summary Progress ownership, cancellation and bounded consumption-driven shadows
 * @run main M3StreamProgressShadow
 */
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class M3StreamProgressShadow {
    private static int checks;
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }
    @FunctionalInterface interface Action { void run() throws Exception; }
    private static void fails(Class<? extends Throwable> type, Action action) throws Exception {
        try { action.run(); }
        catch (Throwable failure) { check(type.isInstance(failure)); return; }
        throw new AssertionError("expected " + type.getName());
    }
    static final class Monitor implements ProgressMonitor {
        long work;
        int done;
        boolean cancelled;
        @Override public void worked(long amount) { work += amount; }
        @Override public void done() { done++; }
        @Override public boolean isCancelled() { return cancelled; }
    }
    static final class Source extends ByteArrayInputStream {
        int reads, closes;
        Source(byte[] bytes) { super(bytes); }
        @Override public synchronized int read() { reads++; return super.read(); }
        @Override public synchronized int read(byte[] b, int o, int n) {
            reads++; return super.read(b, o, n);
        }
        @Override public void close() { closes++; }
    }
    static final class Sink extends OutputStream {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int bulk, scalar, closes, flushes;
        boolean partialFailure;
        @Override public void write(int value) { scalar++; bytes.write(value); }
        @Override public void write(byte[] b, int o, int n) throws IOException {
            bulk++;
            if (partialFailure) { bytes.write(b, o, 1); throw new IOException("partial"); }
            bytes.write(b, o, n);
        }
        @Override public void flush() { flushes++; }
        @Override public void close() { closes++; }
    }
    private static void progress() throws Exception {
        Monitor parent = new Monitor();
        ProgressMonitor child = parent.child("input", 20, 10);
        Source raw = new Source(new byte[]{1,2,3,4,5,6,7,8,9,10});
        InputStream input = raw.withProgress(child);
        check(input.markSupported());
        input.mark(100);
        check(input.read() == 1);
        byte[] bytes = new byte[4];
        check(input.read(bytes) == 4);
        check(parent.work == 10);
        check(input.skip(2) == 2);
        check(parent.work == 14);
        input.reset();
        check(input.read() == 1 && parent.work == 16);
        check(input.skip(-1) == 0);
        check(input.read(bytes, 0, 0) == 0);
        check(input.available() == 9);
        input.close(); input.close();
        check(raw.closes == 1 && parent.done == 0 && parent.work == 16);
        child.done();
        check(parent.work == 20 && parent.done == 0);
        fails(IOException.class, input::read);
        fails(NullPointerException.class, () -> raw.withProgress(null));

        Monitor count = new Monitor();
        try (InputStream all = new ByteArrayInputStream(new byte[33]).withProgress(count)) {
            check(all.readAllBytes().length == 33);
            check(all.read() == -1 && count.work == 33);
        }
        check(count.done == 0);
        Sink sink = new Sink();
        OutputStream output = sink.withProgress(count);
        output.write(new byte[7]);
        output.write(new byte[9], 2, 3);
        output.write(1);
        check(sink.bulk == 2 && sink.scalar == 1 && count.work == 44);
        output.flush();
        check(sink.flushes == 1);
        sink.partialFailure = true;
        fails(IOException.class, () -> output.write(new byte[8]));
        check(count.work == 44 && sink.bytes.size() == 12);
        count.cancelled = true;
        fails(CancellationException.class, () -> output.write(2));
        check(sink.bytes.size() == 12);
        output.write(new byte[0]);
        output.close(); output.close();
        check(sink.closes == 1 && count.done == 0);
        fails(IOException.class, () -> output.write(1));
        fails(NullPointerException.class, () -> sink.withProgress(null));

        Monitor cancelled = new Monitor();
        Source untouched = new Source(new byte[]{4});
        InputStream c = untouched.withProgress(cancelled);
        cancelled.cancelled = true;
        fails(CancellationException.class, c::read);
        fails(CancellationException.class, () -> c.skip(1));
        check(untouched.reads == 0);
        c.close();
        check(untouched.closes == 1);
        Thread.currentThread().interrupt();
        try {
            fails(CancellationException.class,
                    () -> new ByteArrayInputStream(new byte[]{1}).withProgress(ProgressMonitor.NONE).read());
            check(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }

        byte[] expected = new byte[257]; new Random(1).nextBytes(expected);
        Monitor inCount = new Monitor(), outCount = new Monitor();
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        check(new ByteArrayInputStream(expected).withProgress(inCount)
                .transferTo(target.withProgress(outCount)) == expected.length);
        check(Arrays.equals(expected, target.toByteArray()));
        check(inCount.work == expected.length && outCount.work == expected.length);

        InputStream failure = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("read"); }
        }.withProgress(inCount);
        fails(IOException.class, failure::read);
        check(inCount.work == expected.length);
        Source callbackRaw = new Source(new byte[]{1,2});
        InputStream callback = callbackRaw.withProgress(new ProgressMonitor() {
            @Override public void worked(long amount) { throw new IllegalStateException("callback"); }
        });
        fails(IllegalStateException.class, callback::read);
        check(callbackRaw.available() == 1); // successful I/O is not rolled back
    }
    private static void shadows() throws Exception {
        Source source = new Source(new byte[]{0,1,2,3,4,5,6,7,8});
        ShadowInputStream main = new ShadowInputStream(source, 4, 2);
        InputStream first = main.shadow(), second = main.shadow();
        fails(IOException.class, main::shadow);
        fails(IOException.class, first::read);
        check(source.reads == 0 && first.available() == 0);
        check(first.read(new byte[0]) == 0);
        check(!main.markSupported() && !first.markSupported());
        fails(IOException.class, main::reset);
        fails(IOException.class, first::reset);
        check(Arrays.equals(main.readNBytes(4), new byte[]{0,1,2,3}));
        check(main.available() == 0 && source.available() == 5);
        int calls = source.reads;
        fails(IOException.class, main::read);
        check(source.reads == calls);
        check(Arrays.equals(first.readNBytes(4), new byte[]{0,1,2,3}));
        fails(IOException.class, main::read); // slowest shadow owns backpressure
        second.close();
        InputStream late = main.shadow();
        second.close(); // old handle cannot detach a newly reused slot
        check(main.skip(3) == 3);
        check(Arrays.equals(first.readNBytes(3), new byte[]{4,5,6}));
        check(Arrays.equals(late.readNBytes(3), new byte[]{4,5,6}));
        check(main.read() == 7 && main.read() == 8 && main.read() == -1);
        check(Arrays.equals(first.readAllBytes(), new byte[]{7,8}));
        check(Arrays.equals(late.readAllBytes(), new byte[]{7,8}));
        check(first.read() == -1 && late.read() == -1);
        main.close(); main.close();
        check(source.closes == 1);
        fails(IOException.class, main::read);
        first.close();
        fails(IOException.class, first::read);
        fails(IOException.class, first::available);
        fails(IOException.class, () -> first.skip(1));

        ShadowInputStream layered = new ShadowInputStream(new ByteArrayInputStream(new byte[]{9,8}), 2, 1);
        ShadowInputStream middle = new ShadowInputStream(layered.shadow(), 2, 1);
        InputStream leaf = middle.shadow();
        fails(IOException.class, middle::read);
        check(layered.read() == 9 && layered.read() == 8);
        check(middle.read() == 9 && middle.read() == 8);
        check(leaf.read() == 9 && leaf.read() == 8);
        check(layered.read() == -1 && middle.read() == -1 && leaf.read() == -1);
        middle.close(); layered.close();

        ShadowInputStream early = new ShadowInputStream(new ByteArrayInputStream(new byte[]{2,3}), 2, 1);
        InputStream drain = early.shadow();
        check(early.read() == 2); early.close();
        check(drain.read() == 2 && drain.read() == -1);
        fails(IOException.class, early::shadow);
        for (int invalid : new int[]{0,-1,3,7}) {
            fails(IllegalArgumentException.class, () -> new ShadowInputStream(InputStream.nullInputStream(), invalid, 1));
        }
        fails(IllegalArgumentException.class, () -> new ShadowInputStream(InputStream.nullInputStream(), 1, 0));
        fails(NullPointerException.class, () -> new ShadowInputStream(null, 2, 1));
        ShadowInputStream zero = new ShadowInputStream(new InputStream() {
            int n;
            @Override public int read(byte[] b, int o, int l) { return 0; }
            @Override public int read() { return n++ == 0 ? 42 : -1; }
        }, 1, 1);
        InputStream z = zero.shadow();
        check(zero.read(new byte[1]) == 1 && z.read() == 42);
        check(zero.read(new byte[1]) == -1 && z.read() == -1);
        zero.close();
    }
    private static void spectrum() throws Exception {
        Random random = new Random(0x534841444f57L);
        for (int capacity : new int[]{1,2,4,8,16,64}) {
            byte[] data = new byte[4097]; random.nextBytes(data);
            Source raw = new Source(data);
            try (ShadowInputStream main = new ShadowInputStream(raw, capacity, 3)) {
                InputStream[] shadow = {main.shadow(), main.shadow(), main.shadow()};
                int[] cursors = new int[3];
                int end = 0;
                byte[] buffer = new byte[97];
                while (end < data.length) {
                    int oldest = Math.min(cursors[0], Math.min(cursors[1], cursors[2]));
                    int free = capacity - (end - oldest);
                    if (free == 0 || random.nextBoolean()) {
                        for (int i = 0; i < 3; i++) {
                            int n = Math.min(end - cursors[i], 1 + random.nextInt(buffer.length));
                            if (n == 0) continue;
                            int got = shadow[i].read(buffer, 0, n);
                            check(got == n);
                            for (int k = 0; k < got; k++) check(buffer[k] == data[cursors[i] + k]);
                            cursors[i] += got;
                        }
                    } else {
                        int wanted = 1 + random.nextInt(buffer.length);
                        int got = main.read(buffer, 0, wanted);
                        check(got == Math.min(Math.min(wanted, free), data.length - end));
                        for (int k = 0; k < got; k++) check(buffer[k] == data[end + k]);
                        end += got;
                    }
                }
                for (int i = 0; i < 3; i++) {
                    int n = end - cursors[i];
                    check(Arrays.equals(shadow[i].readNBytes(n), Arrays.copyOfRange(data, cursors[i], end)));
                }
                check(main.read() == -1);
                for (InputStream s : shadow) check(s.read() == -1);
            }
            check(raw.closes == 1);
        }
    }
    private static void nativeFile() throws Exception {
        Path file = Files.createTempFile("m3-stream-shadow", ".bin");
        byte[] bytes = new byte[1025]; new Random(42).nextBytes(bytes);
        Monitor output = new Monitor(), input = new Monitor();
        try {
            try (OutputStream out = Files.newOutputStream(file).withProgress(output)) { out.write(bytes); }
            check(output.work == bytes.length && output.done == 0);
            try (ShadowInputStream main = new ShadowInputStream(Files.newInputStream(file).withProgress(input), 64, 1)) {
                InputStream shadow = main.shadow();
                ByteArrayOutputStream replay = new ByteArrayOutputStream();
                byte[] chunk = new byte[64];
                int count;
                while ((count = main.read(chunk)) >= 0) {
                    check(Arrays.equals(Arrays.copyOf(chunk, count), shadow.readNBytes(count)));
                    replay.write(chunk, 0, count);
                }
                check(shadow.read() == -1 && Arrays.equals(replay.toByteArray(), bytes));
            }
            check(input.work == bytes.length && input.done == 0);
        } finally { Files.delete(file); }
    }
    public static void main(String[] args) throws Exception {
        progress(); shadows(); spectrum(); nativeFile();
        System.out.println("M3StreamProgressShadow checks=" + checks);
    }
}
