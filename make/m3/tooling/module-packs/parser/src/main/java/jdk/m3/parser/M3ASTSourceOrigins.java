// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.util.Arrays;
import java.util.Objects;

/**
 * Primitive multi-source provenance for a single original M3ASTIndex.
 * SPELLING, EXPANSION, GENERATED are labels from the original parser,
 * never independently verified claims about macro provenance or input text.
 */
public final class M3ASTSourceOrigins {
    public enum Role { SPELLING, EXPANSION, GENERATED }
    private static final Role[] ROLES = Role.values();
    public static final String SCHEMA = "M3_AST_SOURCE_ORIGINS_V1";
    private final M3ASTIndex index;
    private final M3File[] sources;
    private final int[] offsets, owners, sourceIds, starts, ends;
    private final byte[] roles;
    private M3ASTSourceOrigins(Builder b) {
        index = b.index;
        sources = Arrays.copyOf(b.sources, b.sourceCount);
        owners = Arrays.copyOf(b.owners, b.count);
        sourceIds = Arrays.copyOf(b.sourceIds, b.count);
        starts = Arrays.copyOf(b.starts, b.count);
        ends = Arrays.copyOf(b.ends, b.count);
        roles = Arrays.copyOf(b.roles, b.count);
        offsets = new int[index.nodeCount() + 1];
        for (int owner : owners) offsets[owner + 1]++;
        for (int i = 1; i < offsets.length; i++) offsets[i] += offsets[i - 1];
    }
    public static Builder builder(M3ASTIndex index, int maxOrigins, int maxSources,
                                  long maxRetainedSourceUnits) {
        return new Builder(index, maxOrigins, maxSources, maxRetainedSourceUnits);
    }
    public M3ASTIndex index() { return index; }
    public int sourceCount() { return sources.length; }
    public M3File sourceAt(int sourceId) { return sources[Objects.checkIndex(sourceId, sources.length)]; }
    public int originCount() { return owners.length; }
    public int firstOrigin(int node) { return offsets[Objects.checkIndex(node, index.nodeCount())]; }
    public int endOrigin(int node) { return offsets[Objects.checkIndex(node, index.nodeCount()) + 1]; }
    public int owner(int origin) { return owners[check(origin)]; }
    public int sourceId(int origin) { return sourceIds[check(origin)]; }
    public M3File source(int origin) { return sources[sourceId(origin)]; }
    public Role role(int origin) { return ROLES[roles[check(origin)]]; }
    public int start(int origin) { return starts[check(origin)]; }
    public int end(int origin) { return ends[check(origin)]; }
    /** Explicit materialization from a retained original source generation. */
    public String sourceText(int origin) { return source(origin).subSequence(start(origin), end(origin)); }
    public long retainedPrimitiveBytes() {
        return (long) offsets.length * Integer.BYTES + (long) originCount()
                * (Integer.BYTES * 4L + Byte.BYTES);
    }
    private int check(int origin) { return Objects.checkIndex(origin, owners.length); }

    /** Builders accept occurrences in preorder and source files by object identity. */
    public static final class Builder {
        private final M3ASTIndex index;
        private final int maxOrigins, maxSources;
        private final long maxRetainedSourceUnits;
        private M3File[] sources = new M3File[4];
        private int sourceCount = 1;
        private long retainedUnits;
        private int[] owners, sourceIds, starts, ends;
        private byte[] roles;
        private int count, previousOwner = -1;
        private boolean sealed;
        private Builder(M3ASTIndex index, int maxOrigins, int maxSources, long maxUnits) {
            this.index = Objects.requireNonNull(index, "original AST index");
            if (maxOrigins < 0 || maxOrigins > 20_000_000 || maxSources < 1
                    || maxSources > 1_000_000 || maxUnits < 0)
                throw new IllegalArgumentException("invalid source provenance budgets");
            this.maxOrigins = maxOrigins;
            this.maxSources = maxSources;
            this.maxRetainedSourceUnits = maxUnits;
            sources[0] = index.source();
            retainedUnits = sources[0].length();
            if (retainedUnits > maxUnits) throw new IllegalArgumentException("primary source budget exceeded");
            int initial = Math.max(1, Math.min(16, maxOrigins));
            owners = new int[initial]; sourceIds = new int[initial];
            starts = new int[initial]; ends = new int[initial]; roles = new byte[initial];
        }
        /** A source location is exact only in that source's original UTF-16 units. */
        public Builder add(int node, Role role, M3File source, int start, int end) {
            if (sealed) throw new IllegalStateException("source origins already sealed");
            Objects.checkIndex(node, index.nodeCount());
            Objects.requireNonNull(role, "origin role");
            Objects.requireNonNull(source, "origin source");
            if (node < previousOwner) throw new IllegalArgumentException("origins must follow index occurrence order");
            if (count == maxOrigins) throw new IllegalArgumentException("source origin budget exceeded");
            Objects.checkFromToIndex(start, end, source.length());
            int sourceId = -1;
            for (int i = 0; i < sourceCount; i++) if (sources[i] == source) { sourceId = i; break; }
            if (sourceId < 0) {
                if (sourceCount == maxSources || source.length() > maxRetainedSourceUnits - retainedUnits)
                    throw new IllegalArgumentException("source catalogue budget exceeded");
                if (sourceCount == sources.length) sources = Arrays.copyOf(sources, sources.length * 2);
                sourceId = sourceCount++;
                sources[sourceId] = source;
                retainedUnits += source.length();
            }
            if (count == owners.length) grow();
            owners[count] = node; sourceIds[count] = sourceId;
            starts[count] = start; ends[count] = end;
            roles[count] = (byte) role.ordinal();
            count++; previousOwner = node;
            return this;
        }
        private void grow() {
            int capacity = (int) Math.min(maxOrigins, Math.max((long) count + 1,
                    count + (long) (count >>> 1) + 1));
            owners = Arrays.copyOf(owners, capacity);
            sourceIds = Arrays.copyOf(sourceIds, capacity);
            starts = Arrays.copyOf(starts, capacity);
            ends = Arrays.copyOf(ends, capacity);
            roles = Arrays.copyOf(roles, capacity);
        }
        public M3ASTSourceOrigins seal() {
            if (sealed) throw new IllegalStateException("source origins already sealed");
            sealed = true;
            return new M3ASTSourceOrigins(this);
        }
    }
}
