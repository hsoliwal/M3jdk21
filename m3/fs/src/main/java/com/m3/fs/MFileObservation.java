/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Immutable evidence sampled for one exact, fully resolvable provider address.
 * Metadata, a provider revision, and fully observed bytes are separate evidence.
 * None of the attachments asserts that the source stayed unchanged while it was
 * queried or read. The source string is retained by reference, without interning
 * or normalization; callers must avoid exposing addresses containing credentials.
 *
 * <p>A structural attachment shares the existing immutable manifest and precompute
 * key. This class stores no payload, parser result, per-file map, or history.</p>
 */
public final class MFileObservation {
    public static final int EXISTS_KNOWN = 1;
    public static final int EXISTS = 1 << 1;
    public static final int TYPE_KNOWN = 1 << 2;
    public static final int FILE = 1 << 3;
    public static final int DIRECTORY = 1 << 4;
    public static final int SYMBOLIC_LINK = 1 << 5;
    public static final int SIZE_KNOWN = 1 << 6;
    public static final int MODIFIED_KNOWN = 1 << 7;

    static final int TYPE_BITS = FILE | DIRECTORY | SYMBOLIC_LINK;
    static final int KNOWN_BITS = EXISTS_KNOWN | TYPE_KNOWN | SIZE_KNOWN | MODIFIED_KNOWN;
    private static final int ALL_BITS = KNOWN_BITS | EXISTS | TYPE_BITS;

    /** Absence is an explicit successful provider observation, never a failed I/O inference. */
    public enum Presence { UNKNOWN, ABSENT, PRESENT }

    private final String sourceKey;
    private final int flags;
    private final long size;
    private final long modifiedTimeMillis;
    private final ChangeStamp changeStamp;
    private final M3ObjectRef content;
    private final M3FileManifest manifest;
    private final M3PrecomputeKey structureKey;
    private final int structureRow;

    private MFileObservation(String sourceKey, int flags, long size, long modifiedTimeMillis,
            ChangeStamp changeStamp, M3ObjectRef content, M3FileManifest manifest,
            M3PrecomputeKey structureKey, int structureRow) {
        this.sourceKey = sourceKey;
        this.flags = flags;
        this.size = size;
        this.modifiedTimeMillis = modifiedTimeMillis;
        this.changeStamp = changeStamp;
        this.content = content;
        this.manifest = manifest;
        this.structureKey = structureKey;
        this.structureRow = structureRow;
    }

    /**
     * Records independently known metadata. Unknown numeric fields must be zero;
     * their known bits distinguish unknown from a real zero size or epoch time.
     * A known timestamp may be negative. FILE and DIRECTORY describe a provider's
     * content and children capabilities, so a hybrid file may carry both. A link
     * may also carry either capability when the provider classifies its target.
     *
     * @throws IllegalArgumentException for contradictory flags, an invalid size,
     *         hidden values in unknown fields, or a stamp for another source
     */
    public static MFileObservation metadata(String sourceKey, int flags, long size,
            long modifiedTimeMillis, ChangeStamp stamp) {
        requireText(sourceKey, "sourceKey");
        Objects.requireNonNull(stamp, "stamp");
        if ((flags & ~ALL_BITS) != 0) throw new IllegalArgumentException("unknown observation flags");
        if ((flags & EXISTS) != 0 && (flags & EXISTS_KNOWN) == 0) {
            throw new IllegalArgumentException("existence value needs EXISTS_KNOWN");
        }
        if ((flags & TYPE_BITS) != 0 && (flags & TYPE_KNOWN) == 0) {
            throw new IllegalArgumentException("type value needs TYPE_KNOWN");
        }
        if ((flags & EXISTS_KNOWN) != 0 && (flags & EXISTS) == 0
                && (flags & (TYPE_KNOWN | SIZE_KNOWN | MODIFIED_KNOWN)) != 0) {
            throw new IllegalArgumentException("absent source cannot carry live metadata");
        }
        if ((flags & SIZE_KNOWN) != 0 ? size < 0 : size != 0) {
            throw new IllegalArgumentException("invalid known or unknown size");
        }
        if ((flags & MODIFIED_KNOWN) == 0 && modifiedTimeMillis != 0) {
            throw new IllegalArgumentException("unknown modification time must be zero");
        }
        if (stamp.available() && !sourceKey.equals(stamp.sourceKey())) {
            throw new IllegalArgumentException("change stamp belongs to another source");
        }
        return new MFileObservation(sourceKey, flags, size, modifiedTimeMillis, stamp, null, null, null, -1);
    }

    public static MFileObservation unknown(String sourceKey) {
        return metadata(sourceKey, 0, 0, 0, ChangeStamp.unavailable());
    }

    public static MFileObservation absent(String sourceKey) {
        return metadata(sourceKey, EXISTS_KNOWN, 0, 0, ChangeStamp.unavailable());
    }

    public String sourceKey() { return sourceKey; }
    public int flags() { return flags; }
    public Presence presence() {
        return (flags & EXISTS_KNOWN) == 0 ? Presence.UNKNOWN
                : (flags & EXISTS) == 0 ? Presence.ABSENT : Presence.PRESENT;
    }
    public boolean typeKnown() { return (flags & TYPE_KNOWN) != 0; }
    public boolean isFile() { return (flags & FILE) != 0; }
    public boolean isDirectory() { return (flags & DIRECTORY) != 0; }
    public boolean isSymbolicLink() { return (flags & SYMBOLIC_LINK) != 0; }
    public OptionalLong size() { return (flags & SIZE_KNOWN) == 0 ? OptionalLong.empty() : OptionalLong.of(size); }
    public OptionalLong modifiedTimeMillis() {
        return (flags & MODIFIED_KNOWN) == 0 ? OptionalLong.empty() : OptionalLong.of(modifiedTimeMillis);
    }
    public ChangeStamp changeStamp() { return changeStamp; }
    public Optional<M3ObjectRef> content() { return Optional.ofNullable(content); }
    public Optional<M3FileManifest> manifest() { return Optional.ofNullable(manifest); }
    public Optional<M3PrecomputeKey> structureKey() { return Optional.ofNullable(structureKey); }
    public Optional<String> structureName() {
        return manifest == null ? Optional.empty() : Optional.of(manifest.structureName(structureRow));
    }
    public Optional<M3ObjectRef> structureReference() {
        if (manifest == null) return Optional.empty();
        byte[] id = new byte[M3ContentId.BYTES];
        manifest.copyStructureIdTo(structureRow, id, 0);
        return Optional.of(new M3ObjectRef(M3ContentId.of(id), manifest.structureSize(structureRow),
                M3ObjectKind.values()[manifest.structureKindOrdinal(structureRow)]));
    }

    /**
     * Attaches an existing RAW_BYTES identity for the entire observed file layer.
     * The caller attests that hashing began at offset zero and consumed that layer
     * through EOF without error. A completed wrapper that started at another
     * offset, or a decoded layer's EOF, does not establish full transport coverage.
     * This method neither reads bytes nor verifies that the source was stable.
     *
     * <p>A known metadata size must agree. Unknown size is allowed. Replacing an
     * already attached, different content reference requires a new observation.</p>
     */
    public MFileObservation withContent(M3ObjectRef completeRawContent) {
        Objects.requireNonNull(completeRawContent, "completeRawContent");
        if (completeRawContent.kind() != M3ObjectKind.RAW_BYTES || completeRawContent.size() < 0) {
            throw new IllegalArgumentException("complete content must be nonnegative RAW_BYTES");
        }
        if (!allowsContent()) throw new IllegalArgumentException("absent or known non-file source has no file content");
        if ((flags & SIZE_KNOWN) != 0 && size != completeRawContent.size()) {
            throw new IllegalArgumentException("complete content size differs from sampled metadata");
        }
        if (content != null) {
            if (!content.equals(completeRawContent)) throw new IllegalArgumentException("observation already has different complete content");
            return this;
        }
        return new MFileObservation(sourceKey, flags, size, modifiedTimeMillis, changeStamp,
                completeRawContent, null, null, -1);
    }

    /**
     * Selects one already computed structural object, sharing its immutable
     * manifest and derivation key. The manifest raw reference and the key's first
     * input must identify this observation's complete content. Additional key
     * inputs represent context and must match when comparing structures.
     *
     * <p>The caller attests that the selected object was produced by this key.
     * This method verifies the input binding; it does not run a parser, inspect
     * a stored result, or establish semantic equivalence of source programs.</p>
     */
    public MFileObservation withStructure(M3FileManifest sourceManifest, String name, M3PrecomputeKey key) {
        Objects.requireNonNull(sourceManifest, "sourceManifest");
        requireText(name, "name");
        Objects.requireNonNull(key, "key");
        if (content == null) throw new IllegalArgumentException("structure needs complete observed input content");
        if (!content.equals(sourceManifest.raw())) throw new IllegalArgumentException("manifest raw reference differs from observed content");
        int row = sourceManifest.structureIndex(name);
        if (row < 0) throw new IllegalArgumentException("manifest does not contain selected structure");
        if (key.inputs().isEmpty() || !key.inputs().get(0).equals(content.id())) {
            throw new IllegalArgumentException("structure key primary input differs from observed content");
        }
        return new MFileObservation(sourceKey, flags, size, modifiedTimeMillis, changeStamp,
                content, sourceManifest, key, row);
    }

    long sizeValue() { return size; }
    long modifiedValue() { return modifiedTimeMillis; }
    M3ObjectRef contentReferenceOrNull() { return content; }
    M3FileManifest manifestOrNull() { return manifest; }
    M3PrecomputeKey structureKeyOrNull() { return structureKey; }
    String structureNameValue() { return manifest.structureName(structureRow); }
    int structureKindValue() { return manifest.structureKindOrdinal(structureRow); }
    boolean allowsContent() {
        return presence() != Presence.ABSENT && (!typeKnown() || isFile());
    }
    boolean sameStructureReference(MFileObservation other) {
        if (manifest.structureSize(structureRow) != other.manifest.structureSize(other.structureRow)
                || structureKindValue() != other.structureKindValue()) return false;
        byte[] left = new byte[M3ContentId.BYTES], right = new byte[M3ContentId.BYTES];
        manifest.copyStructureIdTo(structureRow, left, 0);
        other.manifest.copyStructureIdTo(other.structureRow, right, 0);
        return Arrays.equals(left, right);
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label);
        if (value.isEmpty() || value.indexOf('\0') >= 0) throw new IllegalArgumentException("invalid " + label);
        return value;
    }

    /**
     * Provider-supplied revision evidence for one source and byte layer. An
     * authoritative token guarantees that equal tokens in the exact same scope,
     * layer, mechanism and epoch identify equal bytes for this source. A changed
     * token may still identify identical bytes. Metadata-derived or weak entity
     * tags belong in {@link #heuristic}, which never proves byte equality.
     *
     * <p>The provider owns the guarantee, domain selection and epoch rollover.
     * Do not mark a timestamp/size tuple or an unqualified cache tag authoritative.
     * Unavailable accessors return empty strings; no URI or token is printed by
     * the default object representation.</p>
     */
    public static final class ChangeStamp {
        private static final byte NONE = 0, WEAK = 1, STRONG = 2;
        private static final ChangeStamp UNAVAILABLE = new ChangeStamp(NONE, "", "", "", "", "", "");
        private final byte strength;
        private final String sourceKey;
        private final String scope;
        private final String layerKey;
        private final String mechanism;
        private final String epoch;
        private final String token;

        private ChangeStamp(byte strength, String sourceKey, String scope, String layerKey,
                String mechanism, String epoch, String token) {
            this.strength = strength;
            this.sourceKey = sourceKey;
            this.scope = scope;
            this.layerKey = layerKey;
            this.mechanism = mechanism;
            this.epoch = epoch;
            this.token = token;
        }

        public static ChangeStamp unavailable() { return UNAVAILABLE; }

        /** Creates a provider attestation, not a guarantee inferred by M3FS. */
        public static ChangeStamp authoritative(String sourceKey, String scope, String layerKey,
                String mechanism, String epoch, String token) {
            return checked(STRONG, sourceKey, scope, layerKey, mechanism, epoch, token);
        }

        public static ChangeStamp heuristic(String sourceKey, String scope, String layerKey,
                String mechanism, String epoch, String token) {
            return checked(WEAK, sourceKey, scope, layerKey, mechanism, epoch, token);
        }

        private static ChangeStamp checked(byte strength, String sourceKey, String scope, String layerKey,
                String mechanism, String epoch, String token) {
            return new ChangeStamp(strength, requireText(sourceKey, "sourceKey"), requireText(scope, "scope"),
                    requireText(layerKey, "layerKey"), requireText(mechanism, "mechanism"),
                    requireText(epoch, "epoch"), requireText(token, "token"));
        }

        public boolean available() { return strength != NONE; }
        public boolean authoritative() { return strength == STRONG; }
        public String sourceKey() { return sourceKey; }
        public String scope() { return scope; }
        public String layerKey() { return layerKey; }
        public String mechanism() { return mechanism; }
        public String epoch() { return epoch; }
        public String token() { return token; }

        boolean sameDomain(ChangeStamp other) {
            return sourceKey.equals(other.sourceKey) && scope.equals(other.scope)
                    && layerKey.equals(other.layerKey) && mechanism.equals(other.mechanism) && epoch.equals(other.epoch);
        }
    }
}
