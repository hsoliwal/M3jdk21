// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
public final class M3FileObservationContract {
    /** Preserve the previously implicit public no-argument constructor under strict Java 21 lint. */
    public M3FileObservationContract() { }

    private static int checks;
    private static void check(boolean x, String name) {
        checks++;
        if (!x) throw new AssertionError(name);
    }
    private static void expect(Class<? extends Throwable> cls, RunnableCheck action, String why) {
        checks++;
        try {action.run(); } catch(Throwable t) { if(cls.isInstance(t)) return; throw new AssertionError(why+": "+t,t); }
        throw new AssertionError("missing: "+why);
    }
    @FunctionalInterface interface RunnableCheck { void run() throws Exception; }
    private static byte[] digest(M3File.Fingerprint f) {
        return ByteBuffer.allocate(32).putLong(f.digest0()).putLong(f.digest1())
                .putLong(f.digest2()).putLong(f.digest3()).array();
    }
    public static void main(String[] argv) throws Exception {
        Path directory = Files.createTempDirectory("m3-original-file-proof-");
        try {
            Random random = new Random(20261021);
            Path path = directory.resolve("input.bin");
            for (int i = 0; i < 750; i++) {
                int len = switch(i % 7) { case 0 -> 0; case 1 -> 1; case 2 -> 8192; case 3 -> 8193;
                    case 4 -> 65536; default -> random.nextInt(32000); };
                byte[] content = new byte[len]; random.nextBytes(content);
                Files.write(path, content);
                M3File file = M3File.of(path);
                AtomicLong progress = new AtomicLong();
                AtomicInteger chunks = new AtomicInteger();
                ByteArrayOutputStream shadow = new ByteArrayOutputStream();
                M3File.Observation observed = file.observe(content.length, progress::addAndGet,
                    (bytes, from, count) -> {check(count > 0 && count <= 8192, "valid chunk");
                        chunks.incrementAndGet(); shadow.write(bytes,from,count);});
                check(observed.fingerprint().size()==content.length, "file length");
                check(progress.get()==content.length, "progress counts bytes");
                check(Arrays.equals(shadow.toByteArray(),content), "shadow exact bytes");
                check(Arrays.equals(digest(observed.fingerprint()),MessageDigest.getInstance("SHA-256").digest(content)), "SHA correctness");
                check(file.fingerprint(content.length).equals(observed.fingerprint()), "unchanged original entry");
                check(content.length == 0 || chunks.get()==(content.length+8191)/8192, "all byte callback chunks");
                if (len > 0) {
                    expect(IOException.class, ()->file.observe(content.length-1, M3StreamProgress.none(),
                        (b,o,n)->{}), "bounded input read");
                }
            }
            check(M3File.contentDelta(null,null)==0, "known absence");
            if (Files.size(path) == 0) Files.write(path,new byte[]{77});
            M3File f=M3File.of(path);
            M3File.Fingerprint before=f.fingerprint(100000);
            FileTime stamp=Files.getLastModifiedTime(path);
            byte[] now=Files.readAllBytes(path); now[0]^=1;
            Files.write(path,now); Files.setLastModifiedTime(path,stamp);
            M3File.Fingerprint after=f.fingerprint(100000);
            check(M3File.contentDelta(before,after)==M3File.CONTENT_CHANGED,"same mtime length changed");
            expect(IOException.class, ()-> f.observe(100000,M3StreamProgress.none(),
                    (bytes, from, n)->{throw new IOException("observer aborted");}),"observer abort");
            M3StreamProgress cancelled = new M3StreamProgress() {
                @Override public void worked(long units) { }
                @Override public boolean cancelled() {return true;}
            };
            expect(CancellationException.class,()->f.observe(100000,cancelled,
                (bytes,from,n)->{}),"cancelled source");
            Path symlink=directory.resolve("symlink");
            try {
                Files.createSymbolicLink(symlink,path.getFileName());
                expect(IOException.class,()->M3File.of(symlink).fingerprint(100000),"symlink rejected");
            } catch (UnsupportedOperationException | SecurityException | IOException unavailable) {
                // Not every host platform allows test-process symlink creation.
            }
            expect(IllegalArgumentException.class,()->f.observe(-1,M3StreamProgress.none(),
                (bytes,from,n)->{}),"negative byte budget");
            System.out.println("M3_FILE_OBSERVATION_PASS checks="+checks);
        } finally {
            try(var files=Files.list(directory)){ for(Path p: files.toList())Files.deleteIfExists(p); }
            Files.deleteIfExists(directory);
        }
    }
}
