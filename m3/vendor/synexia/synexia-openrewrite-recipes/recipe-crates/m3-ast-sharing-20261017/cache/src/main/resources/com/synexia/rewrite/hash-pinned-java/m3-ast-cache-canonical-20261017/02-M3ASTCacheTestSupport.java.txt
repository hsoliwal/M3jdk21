// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.mindex.compiler.ast;

import com.synexia.mindex.swap.SwapStore;
import com.synexia.mindex.swap.TieredSwapCache;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/** Test-only failure-injection store; real durable integration uses the existing SQLite JNI owner. */
final class M3ASTCacheTestSupport {
    static final String CONTEXT = M3FileSourceFacts.contextSha256(java.util.List.of("javac21", "source-set-1"));
    static final TieredSwapCache.Budget BUDGET = new TieredSwapCache.Budget(8_192, 2, 0);
    static final TieredSwapCache.Codec<String> TEXT = new TieredSwapCache.Codec<>() {
        @Override public byte[] encode(String value) throws IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(value.length());
                for (char c : value.toCharArray()) out.writeChar(c);
            }
            return bytes.toByteArray();
        }
        @Override public String decode(byte[] bytes) throws IOException {
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
                int length = in.readInt();
                if (length < 0 || length != in.available() / 2 || in.available() % 2 != 0) {
                    throw new IOException("text codec length");
                }
                char[] chars = new char[length];
                for (int i = 0; i < length; i++) chars[i] = in.readChar();
                return new String(chars);
            }
        }
        @Override public long weight(String value) { return 64L + 2L * value.length(); }
    };

    private M3ASTCacheTestSupport() { }

    static M3FileSourceFacts key(int id) {
        return M3FileSourceFacts.capture("memory:///Unit" + id + ".java", "class Unit" + id + " {}",
                CONTEXT, "javac-syntax-v1", 1, "PARSE");
    }

    static M3ASTWorkspaceCache<String> cache(SwapStore store) {
        return new M3ASTWorkspaceCache<>(CONTEXT, store, TEXT, BUDGET, false);
    }

    static final class MemoryStore implements SwapStore {
        final TreeMap<Long, byte[]> records = new TreeMap<>();
        final Map<String, Head> heads = new HashMap<>();
        long reads;
        long writes;
        boolean failWrite;
        boolean closed;

        @Override public synchronized byte[] read(long id) throws IOException {
            checkOpen();
            reads++;
            byte[] data = records.get(id);
            return data == null ? null : data.clone();
        }
        @Override public synchronized void write(Map<Long, byte[]> values, Publication publication)
                throws IOException {
            checkOpen();
            if (failWrite) throw new IOException("injected store write failure");
            if (values.size() > maxBatchRecords()) throw new IOException("batch count");
            long total = 0;
            for (var item : values.entrySet()) {
                SwapStore.requireId(item.getKey());
                byte[] old = records.get(item.getKey());
                if (old != null && !Arrays.equals(old, item.getValue())) throw new IOException("immutable record conflict");
                if (item.getValue().length > maxRecordBytes()) throw new IOException("record bytes");
                total += item.getValue().length;
            }
            if (total > maxBatchBytes()) throw new IOException("batch bytes");
            if (publication != null && head(publication.name()).revision() != publication.expectedRevision()) {
                throw new IOException("stale publication");
            }
            for (var item : values.entrySet()) records.putIfAbsent(item.getKey(), item.getValue().clone());
            if (publication != null) heads.put(publication.name(),
                    new Head(publication.expectedRevision() + 1, publication.recordId()));
            writes++;
        }
        @Override public synchronized Head head(String name) { return heads.getOrDefault(name, new Head(0, -1)); }
        @Override public synchronized long[] ids(long after, int limit) {
            return records.tailMap(after, false).keySet().stream().limit(limit).mapToLong(Long::longValue).toArray();
        }
        @Override public synchronized long count() { return records.size(); }
        @Override public int maxRecordBytes() { return 1 << 20; }
        @Override public int maxBatchBytes() { return 4 << 20; }
        @Override public int maxBatchRecords() { return 64; }
        @Override public synchronized void close() { closed = true; }
        private void checkOpen() throws IOException { if (closed) throw new IOException("closed store"); }
    }
}
