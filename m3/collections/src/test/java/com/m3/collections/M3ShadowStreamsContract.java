/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;

public final class M3ShadowStreamsContract {
    private static int checks;
    private M3ShadowStreamsContract() { }
    private static void check(boolean value, String name) {
        checks++; if (!value) throw new AssertionError(name);
    }
    private static void refusedIO(Throwing value) throws Exception {
        checks++; try { value.run(); throw new AssertionError("expected IOException"); }
        catch (IOException expected) { }
    }
    @FunctionalInterface private interface Throwing { void run() throws Exception; }
    private static final class Monitor implements M3ProgressMonitor {
        long worked, budget = -1L; int begin, split, done; boolean canceled;
        @Override public boolean isCanceled() { return canceled; }
        @Override public void setCanceled(boolean value) { canceled = value; }
        @Override public void worked(long count) { worked += count; }
        @Override public void done() { done++; }
        @Override public M3ProgressMonitor split(long weight) { split++; check(weight > 0L, "parent split work positive"); return this; }
        @Override public void beginTask(String task, long expected) { begin++; budget = expected; }
        @Override public void setProgress(long work, long total) { worked = work; budget = total; }
        @Override public void setTaskName(String name) { }
        @Override public void subTask(String name) { }
        @Override public M3ProgressState state() {
            return new M3ProgressState("shadow", "", worked, budget, canceled, done > 0, 0L, 0L);
        }
    }
    private static final class BorrowedSink extends ByteArrayOutputStream {
        boolean closed;
        @Override public void close() throws IOException { closed = true; super.close(); }
    }
    private static void input() throws Exception {
        byte[] values = new byte[16_391]; new Random(20261020L).nextBytes(values);
        Monitor monitor = new Monitor();
        BorrowedSink first = new BorrowedSink(), second = new BorrowedSink();
        ByteArrayOutputStream visible = new ByteArrayOutputStream();
        try (InputStream in = monitor.shadowInput(new ByteArrayInputStream(values), 100L, values.length, first, second)) {
            check(!in.markSupported(), "mark must be unavailable");
            in.mark(1);
            refusedIO(in::reset);
            visible.write(in.read());
            visible.write(in.readNBytes(128));
            check(in.skip(17) == 17, "skip passes through mirror");
            visible.write(in.readAllBytes());
            check(in.read() == -1, "eof");
        }
        byte[] witnessed = first.toByteArray();
        check(Arrays.equals(witnessed, values), "shadow input includes skipped bytes exactly once");
        check(Arrays.equals(witnessed, second.toByteArray()), "multiple observers see identical bytes");
        check(visible.size() == values.length - 17, "visible excludes skipped bytes");
        check(monitor.worked == values.length, "progress is physical read count");
        check(monitor.begin == 1 && monitor.split == 1 && monitor.done == 1, "submonitor lifecycle");
        check(!first.closed && !second.closed, "observers borrowed by contract");
        checks++;
        try { monitor.shadowInput(new ByteArrayInputStream(values), 1L, 2L, (OutputStream) null);
              throw new AssertionError("null shadow accepted"); }
        catch (NullPointerException expected) { }
    }
    private static void output() throws Exception {
        byte[] values = new byte[7_001]; new Random(777L).nextBytes(values);
        Monitor monitor = new Monitor();
        BorrowedSink first = new BorrowedSink(), second = new BorrowedSink();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStream shadow = monitor.shadowOutput(out, 75L, values.length, first, second)) {
            shadow.write(values[0]);
            shadow.write(values, 1, values.length - 1);
            shadow.flush();
        }
        check(Arrays.equals(out.toByteArray(), values), "original output exact bytes");
        check(Arrays.equals(first.toByteArray(), values), "first writer mirror exact bytes");
        check(Arrays.equals(second.toByteArray(), values), "second writer mirror exact bytes");
        check(monitor.worked == values.length, "output writes recorded once, not FilterOutputStream double credit");
        check(monitor.begin == 1 && monitor.split == 1 && monitor.done == 1, "writer submonitor lifecycle");
        check(!first.closed && !second.closed, "original output does not close shadow sinks");
    }
    private static void failClosed() throws Exception {
        Monitor inputProgress = new Monitor();
        OutputStream poisoned = new OutputStream() {
            @Override public void write(int x) throws IOException { throw new IOException("observer rejected"); }
            @Override public void write(byte[] b, int off, int len) throws IOException { throw new IOException("observer rejected"); }
        };
        InputStream source = new ByteArrayInputStream(new byte[] {7, 8, 9});
        try (InputStream in = inputProgress.shadowInput(source, 1L, 3L, poisoned)) {
            refusedIO(in::read);
            check(inputProgress.worked == 1, "source byte consumed before shadow failure");
            refusedIO(in::read);
            check(inputProgress.worked == 1, "no replay or another read on poisoned input");
        }
        Monitor outputProgress = new Monitor();
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try (OutputStream out = outputProgress.shadowOutput(sink, 1L, 3L, poisoned)) {
            refusedIO(() -> out.write(21));
            refusedIO(() -> out.write(22));
        }
        check(sink.size() == 1 && outputProgress.worked == 1, "poisoned output is not silently retried");
        Monitor canceled = new Monitor(); canceled.canceled = true;
        try (InputStream input = canceled.shadowInput(new ByteArrayInputStream(new byte[]{1}), 1L, 1L)) {
            checks++;
            try { input.read(); throw new AssertionError("cancellation ignored"); }
            catch (CancellationException expected) { }
        }
        check(canceled.worked == 0, "no I/O when canceled before read");
    }
    private static void transfer() throws Exception {
        byte[] data = new byte[100_000]; new Random(45L).nextBytes(data);
        Monitor m = new Monitor(); ByteArrayOutputStream mirror = new ByteArrayOutputStream();
        try (InputStream in = m.shadowInput(new ByteArrayInputStream(data), 1L, data.length, mirror)) {
            check(in.transferTo(OutputStream.nullOutputStream()) == data.length, "transferTo uses bounded read interception");
        }
        check(Arrays.equals(mirror.toByteArray(), data), "transferTo shadows actual bytes");
    }
    public static void main(String[] args) throws Exception {
        input(); output(); failClosed(); transfer();
        System.out.println("M3_SHADOW_STREAM_PASS checks=" + checks);
    }
}
