/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * @test
 * @summary Monitored byte and UTF-16 I/O: cancellation, progress, identity and failure
 * @run main/othervm MonitoredIoOperations
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ProgressMonitor;

public class MonitoredIoOperations {
    static final class Counting implements ProgressMonitor {
        long completed;
        long cancelAt = Long.MAX_VALUE;
        int done;
        @Override public void worked(long amount) {
            if (amount < 0) throw new AssertionError("negative");
            completed += amount;
        }
        @Override public boolean isCancelled() { return completed >= cancelAt; }
        @Override public void done() { done++; }
    }

    static void check(boolean yes, String message) {
        if (!yes) throw new AssertionError(message);
    }

    static void expectCancel(IoAction action) throws Exception {
        try { action.call(); } catch (CancellationException expected) { return; }
        throw new AssertionError("missing cancellation");
    }

    interface IoAction { void call() throws Exception; }

    public static void main(String[] args) throws Exception {
        byte[] input = new byte[30000];
        for (int i = 0; i < input.length; i++) input[i] = (byte) (i * 11 + 3);
        byte[] output = new byte[input.length];
        Counting monitor = new Counting();
        InputStream shortReads = new ByteArrayInputStream(input) {
            @Override public synchronized int read(byte[] b, int off, int len) {
                return super.read(b, off, Math.min(len, 3));
            }
        };
        check(shortReads.readNBytesMonitored(output, 0, output.length, monitor)
                == input.length, "short reads");
        check(Arrays.equals(input, output), "byte identity");
        check(monitor.completed == input.length && monitor.done == 0, "read lifecycle");

        monitor = new Counting();
        monitor.cancelAt = 16384;
        Counting readCancel = monitor;
        expectCancel(() -> new ByteArrayInputStream(input).readNBytesMonitored(
                new byte[input.length], 0, input.length, readCancel));
        check(readCancel.completed == 16384, "read cancellation boundary");

        Counting transfer = new Counting();
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        long count = new ByteArrayInputStream(input).transferToMonitored(sink, transfer);
        check(count == input.length && transfer.completed == count, "transfer count");
        check(Arrays.equals(input, sink.toByteArray()), "transfer payload");

        Counting cancelledWrite = new Counting();
        cancelledWrite.cancelAt = 8192;
        ByteArrayOutputStream partial = new ByteArrayOutputStream();
        expectCancel(() -> partial.writeMonitored(input, 0, input.length, cancelledWrite));
        check(partial.size() == 8192 && cancelledWrite.completed == 8192,
                "write cancellation");
        check(cancelledWrite.done == 0, "borrowed monitor completed");

        byte[] prefix = Arrays.copyOf(input, 16384);
        Counting failed = new Counting();
        OutputStream rejecting = new ByteArrayOutputStream() {
            @Override public synchronized void write(byte[] b, int off, int len)
                    throws IndexOutOfBoundsException {
                throw new IndexOutOfBoundsException("refused");
            }
        };
        try {
            new ByteArrayInputStream(prefix).transferToMonitored(rejecting, failed);
            throw new AssertionError("write failure not propagated");
        } catch (IndexOutOfBoundsException expected) {
            check(failed.completed == 0, "failure credited incomplete output");
        }

        String text = "\uD83D\uDE00" + "Z".repeat(19000) + "\u0000";
        Counting charTransfer = new Counting();
        StringWriter charSink = new StringWriter();
        long chars = new StringReader(text).transferToMonitored(charSink, charTransfer);
        check(chars == text.length() && charTransfer.completed == chars, "char work");
        check(text.equals(charSink.toString()), "UTF16 identity");

        Counting charCancel = new Counting();
        charCancel.cancelAt = 8192;
        StringWriter charPartial = new StringWriter();
        expectCancel(() -> charPartial.writeMonitored(text.toCharArray(), 0,
                text.length(), charCancel));
        check(charPartial.toString().equals(text.substring(0, 8192)), "char cancel");
        check(charCancel.completed == 8192 && charCancel.done == 0, "char lifecycle");

        Counting charRead = new Counting();
        char[] dest = new char[40];
        Reader r = new StringReader(text);
        check(r.readMonitored(dest, 0, dest.length, charRead) == 40,
                "reader count");
        check(charRead.completed == 40, "reader progress");


        // A broken delegate returning zero for a positive-size request must not
        // spin a monitored long-running operation forever.
        InputStream zeroBytes = new InputStream() {
            @Override public int read() { return 0; }
            @Override public int read(byte[] b, int off, int len) { return 0; }
        };
        Counting zeroByteProgress = new Counting();
        try {
            zeroBytes.readMonitored(new byte[8], 0, 8, zeroByteProgress);
            throw new AssertionError("zero-byte single read not refused");
        } catch (IOException expected) { }
        try {
            zeroBytes.readNBytesMonitored(new byte[8], 0, 8, zeroByteProgress);
            throw new AssertionError("zero-byte bounded read not refused");
        } catch (IOException expected) { }
        try {
            zeroBytes.transferToMonitored(new ByteArrayOutputStream(), zeroByteProgress);
            throw new AssertionError("zero-byte transfer not refused");
        } catch (IOException expected) { }
        check(zeroByteProgress.completed == 0 && zeroByteProgress.done == 0,
                "zero-byte monitor ownership");

        Reader zeroChars = new Reader() {
            @Override public int read(char[] b, int off, int len) { return 0; }
            @Override public void close() { }
        };
        Counting zeroCharProgress = new Counting();
        try {
            zeroChars.readMonitored(new char[8], 0, 8, zeroCharProgress);
            throw new AssertionError("zero-char single read not refused");
        } catch (IOException expected) { }
        try {
            zeroChars.transferToMonitored(new StringWriter(), zeroCharProgress);
            throw new AssertionError("zero-char transfer not refused");
        } catch (IOException expected) { }
        check(zeroCharProgress.completed == 0 && zeroCharProgress.done == 0,
                "zero-char monitor ownership");

        System.out.println("M3_MONITORED_IO_PASS");
    }
}
