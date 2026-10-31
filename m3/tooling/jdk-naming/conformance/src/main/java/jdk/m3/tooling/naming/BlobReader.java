/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.naming;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Streams the blobs of {@code rev:path} through one {@code git cat-file --batch}: a writer thread sends the names, the
 * caller's thread reads each {@code <oid> blob <size>} header and its bytes. GIT_NO_LAZY_FETCH keeps a partial clone
 * from fetching; a missing blob fails the read.
 */
final class BlobReader {

    private BlobReader() {
    }

    static void read(Path root, String rev, List<String> paths, BiConsumer<String, byte[]> sink)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("git", "-C", root.toString(), "cat-file", "--batch")
                .redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.environment().put("GIT_NO_LAZY_FETCH", "1");
        Process git = builder.start();
        Thread writer = Thread.ofPlatform().start(() -> request(git.getOutputStream(), rev, paths));
        try (InputStream in = new BufferedInputStream(git.getInputStream(), 1 << 16)) {
            for (String path : paths) {
                sink.accept(path, blob(in, path));
            }
        } finally {
            writer.join();
            git.waitFor();
        }
    }

    private static void request(OutputStream out, String rev, List<String> paths) {
        try (out) {
            for (String path : paths) {
                out.write((rev + ":" + path + "\n").getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] blob(InputStream in, String path) throws IOException {
        String[] header = line(in).split(" ");
        if (header.length != 3) {
            throw new IOException("blob not available locally: " + path + " (" + String.join(" ", header) + ")");
        }
        byte[] data = in.readNBytes(Integer.parseInt(header[2]));
        if (in.read() != '\n') {
            throw new IOException("malformed cat-file output after " + path);
        }
        return data;
    }

    private static String line(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(96);
        for (int b = in.read(); b != '\n'; b = in.read()) {
            if (b < 0) {
                throw new IOException("cat-file ended early");
            }
            bytes.write(b);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
