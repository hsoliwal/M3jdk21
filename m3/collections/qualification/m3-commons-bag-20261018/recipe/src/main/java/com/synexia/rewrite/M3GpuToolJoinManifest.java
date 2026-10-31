// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/** Internal data/contract atom of M3GpuToolJoinRecipe, not a filesystem writer or rewrite engine. */
final class M3GpuToolJoinManifest {
    static final String ABSENT = "ABSENT";
    static final int MAX_TARGETS = 65_536;
    static final int MAX_MANIFEST_BYTES = 16 * 1024 * 1024;
    static final int MAX_TEMPLATE_BYTES = 64 * 1024 * 1024;
    static final long MAX_TOTAL_TEMPLATE_BYTES = 512L * 1024 * 1024;
    static final Runnable NOOP = () -> { };

    @FunctionalInterface
    interface Resources {
        InputStream open(String absoluteResource) throws IOException;
    }

    record Target(String path, String before, String after, String text) {
        boolean removal() { return after.equals(ABSENT); }
        boolean addition() { return before.equals(ABSENT); }
    }

    private record Payload(String text, String hash, int bytes) { }

    private final Map<String, Target> targets;
    private final Runnable checkpoint;

    private M3GpuToolJoinManifest(Map<String, Target> targets, Runnable checkpoint) {
        // Transfer the private builder; manifest order is part of deterministic generation.
        this.targets = Collections.unmodifiableMap(targets);
        this.checkpoint = checkpoint;
    }

    static M3GpuToolJoinManifest load(String root, Resources resources, Runnable checkpoint) {
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(checkpoint, "checkpoint");
        check(checkpoint);
        if (root == null || !root.startsWith("/") || !root.endsWith("/") || root.length() < 3) {
            throw new IllegalStateException("invalid template resource root");
        }
        relative(root.substring(1, root.length() - 1));
        String manifest = read(resources, root + "manifest.tsv", MAX_MANIFEST_BYTES, checkpoint).text();
        Map<String, Target> index = new LinkedHashMap<>();
        Map<String, Payload> texts = new HashMap<>();
        long total = 0;
        var lines = manifest.lines().iterator();
        while (lines.hasNext()) {
            check(checkpoint);
            String[] cells = lines.next().split("\t", -1);
            if (cells.length != 4) throw new IllegalStateException("invalid template manifest row");
            String path = relative(cells[0]);
            if (index.size() == MAX_TARGETS) throw new IllegalStateException("template target budget");
            if (index.containsKey(path)) throw new IllegalStateException("duplicate template target: " + path);
            String before = cells[1], after = cells[2];
            if ((!before.equals(ABSENT) && !hash(before)) || (!after.equals(ABSENT) && !hash(after))) {
                throw new IllegalStateException("invalid template content hash");
            }
            boolean removal = after.equals(ABSENT), keep = cells[3].equals("KEEP");
            if (before.equals(ABSENT) && removal || keep && (before.equals(ABSENT) || !before.equals(after))) {
                throw new IllegalStateException("invalid removal or owner pin");
            }
            String text = null;
            if (!removal && !keep) {
                String name = relative(cells[3]);
                Payload payload = texts.get(name);
                if (payload == null) {
                    int remaining = (int) Math.min(MAX_TEMPLATE_BYTES, MAX_TOTAL_TEMPLATE_BYTES - total);
                    payload = read(resources, root + name, remaining, checkpoint);
                    total = Math.addExact(total, payload.bytes());
                    texts.put(name, payload);
                }
                text = payload.text();
                if (!payload.hash().equals(after)) throw new IllegalStateException("template hash mismatch: " + path);
            }
            index.put(path, new Target(path, before, after, text));
        }
        check(checkpoint);
        return new M3GpuToolJoinManifest(index, checkpoint);
    }

    Collection<Target> targets() { return targets.values(); }
    Target target(String path) { return targets.get(path); }
    Scan scan() { return new Scan(this); }
    void checkpoint() { check(checkpoint); }

    /** One accumulator per recipe run; unrelated files consume no retained entries. */
    static final class Scan {
        private final M3GpuToolJoinManifest manifest;
        private final Map<String, String> seen = new HashMap<>();
        private String failure;
        private boolean admitted;

        private Scan(M3GpuToolJoinManifest manifest) { this.manifest = manifest; }

        synchronized void observe(String path, String actualHash) {
            manifest.checkpoint();
            Target target = manifest.target(path);
            if (target == null) return;
            admitted = false;
            if (seen.putIfAbsent(path, actualHash) != null) fail("duplicate source target: " + path);
            if (!actualHash.equals(target.before()) && !actualHash.equals(target.after())) {
                fail("source drift: " + path);
            }
        }

        synchronized void requireAdmissible() {
            manifest.checkpoint();
            for (Target target : manifest.targets()) {
                manifest.checkpoint();
                if (!target.addition() && !target.removal() && !seen.containsKey(target.path())) {
                    fail("required source missing: " + target.path());
                }
            }
            requireNoFailure();
            admitted = true;
        }

        synchronized boolean missing(String path) {
            requireAdmitted();
            return !seen.containsKey(path);
        }

        /** Recheck at edit time: another recipe may have edited after our scanning phase. */
        synchronized Target editing(String path, String actualHash) {
            manifest.checkpoint();
            requireAdmitted();
            Target target = manifest.target(path);
            if (target == null) return null;
            // Includes an already-generated addition and a second run's fixed point.
            if (actualHash.equals(target.after())) return target;
            String observed = seen.get(path);
            if (!actualHash.equals(target.before()) || !actualHash.equals(observed)) {
                fail("source changed since scan: " + path);
                requireNoFailure();
            }
            return target;
        }

        private void requireAdmitted() {
            requireNoFailure();
            if (!admitted) throw new IllegalStateException("manifest scan has not been admitted");
        }
        private void requireNoFailure() {
            if (failure != null) throw new IllegalStateException(failure);
        }
        private void fail(String reason) { if (failure == null) failure = reason; }
        synchronized int observedTargets() { return seen.size(); }
    }

    static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }

    private static String relative(String value) {
        if (value == null || value.isEmpty() || value.length() > 4096 || value.startsWith("/")
                || value.indexOf('\\') >= 0 || value.indexOf(':') >= 0
                || value.chars().anyMatch(c -> c < 32 || c == 127)) {
            throw new IllegalStateException("invalid template path");
        }
        for (String part : value.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..") || part.equalsIgnoreCase(".git")) {
                throw new IllegalStateException("noncanonical template path");
            }
        }
        return value;
    }

    private static boolean hash(String value) {
        if (value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f')) return false;
        }
        return true;
    }

    private static Payload read(Resources resources, String path, int maximum, Runnable checkpoint) {
        check(checkpoint);
        try (InputStream input = resources.open(path)) {
            if (input == null) throw new IllegalStateException("missing recipe resource: " + path);
            var output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (;;) {
                check(checkpoint);
                int read = input.read(buffer, 0, Math.min(buffer.length, maximum - output.size() + 1));
                if (read < 0) break;
                if (read == 0) {
                    int unit = input.read();
                    if (unit < 0) break;
                    if (output.size() == maximum) throw new IllegalStateException("recipe resource size budget");
                    output.write(unit);
                } else {
                    if (read > maximum - output.size()) throw new IllegalStateException("recipe resource size budget");
                    output.write(buffer, 0, read);
                }
            }
            check(checkpoint);
            byte[] bytes = output.toByteArray();
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            return new Payload(text, digest(bytes), bytes.length);
        } catch (CharacterCodingException invalid) {
            throw new IllegalStateException("invalid UTF-8 recipe resource: " + path, invalid);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read recipe resource: " + path, failure);
        }
    }

    static String sha256(String value) { return digest(value.getBytes(StandardCharsets.UTF_8)); }

    private static String digest(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    static void check(Runnable checkpoint) {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("recipe interrupted");
        checkpoint.run();
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("recipe interrupted");
    }
}
