// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Two-phase, read-only JVM classfile symbol signal extractor.
 *
 * <p>Stage one scans a private immutable source snapshot into primitive constant-pool lanes.
 * Stage two may intern raw CONSTANT_Utf8 bytes in an existing ClassSpace, in constant-pool
 * order. Modified UTF-8 is intentionally <em>not</em> converted into Java String, and no
 * classfile verifier, loader, Klass, mirror, resolution or CDS state is created here.
 * Symbol equality is byte equality, not class, method, signature or Java semantic equivalence.
 *
 * <p>The scanner checks envelope lengths and grammar; it does not claim JVM verification.
 * Files larger than 16 MiB are not candidates for this bounded precompute lane.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d5ad5090d87ef8477a56317e60ac0d29c65029f7/synexia-openrewrite-recipes/recipe-crates/m3-class-space-canonical-images-20261030/src/main/java/com/synexia/m3/classspace/M3ClassFileAtoms.java blob 731992dea85f; recipe m3/intern/recipe/class-space-20261107/relocate.py v1
public final class ClassFileAtoms {
    private static final int MAX_BYTES = 16 * 1024 * 1024;
    private static final byte[] DOMAIN = {
        'M', '3', '-', 'C', 'P', '-', 'U', 'T', 'F', '8', '-', 'V', '1', 0
    };

    private ClassFileAtoms() {
    }

    /** Pure read-only precompute; no store is touched on malformed input. */
    public static Plan scan(byte[] classfile) {
        Objects.requireNonNull(classfile, "classfile");
        if (classfile.length > MAX_BYTES) {
            throw new IllegalArgumentException("Classfile exceeds bounded signal limit");
        }
        // Caller may change the input after invocation; never keep an aliased byte array.
        byte[] frozen = classfile.clone();
        Cursor c = new Cursor(frozen);
        if (c.u4() != 0xcafebabeL) {
            throw new IllegalArgumentException("Not a classfile image");
        }
        int minor = c.u2();
        int major = c.u2();
        int cpCount = c.u2();
        if (cpCount < 1 || major < 45) {
            throw new IllegalArgumentException("Invalid classfile header");
        }
        byte[] tags = new byte[cpCount];
        int[] utf8Offsets = new int[cpCount];
        int[] utf8Lengths = new int[cpCount];
        int utf8Count = 0;
        for (int i = 1; i < cpCount; i++) {
            int tag = c.u1();
            tags[i] = (byte) tag;
            switch (tag) {
                case 1 -> {
                    int length = c.u2();
                    utf8Offsets[i] = c.position();
                    utf8Lengths[i] = length;
                    c.skip(length);
                    utf8Count++;
                }
                case 3, 4, 9, 10, 11, 12, 17, 18 -> c.skip(4);
                case 5, 6 -> {
                    c.skip(8);
                    if (++i >= cpCount) {
                        throw new IllegalArgumentException("Long/double missing reserved CP slot");
                    }
                }
                case 7, 8, 16, 19, 20 -> c.skip(2);
                case 15 -> c.skip(3);
                default -> throw new IllegalArgumentException("Unsupported CP tag: " + tag);
            }
        }
        // Fully scan the enclosing structure before admitting any symbol to the store.
        c.skip(6); // access_flags, this_class, super_class; not semantic verification
        int interfaces = c.u2();
        c.skip((long) interfaces * 2);
        skipMembers(c);
        skipMembers(c);
        skipAttributes(c);
        if (c.position() != frozen.length) {
            throw new IllegalArgumentException("Trailing classfile bytes");
        }
        return new Plan(frozen, tags, utf8Offsets, utf8Lengths, minor, major, utf8Count);
    }

    private static void skipMembers(Cursor c) {
        int count = c.u2();
        for (int i = 0; i < count; i++) {
            c.skip(6); // access_flags, name_index, descriptor_index
            skipAttributes(c);
        }
    }

    private static void skipAttributes(Cursor c) {
        int count = c.u2();
        for (int i = 0; i < count; i++) {
            c.skip(2); // name_index
            c.skip(c.u4()); // declared unsigned length; strict bounds, including >2 GiB
        }
    }

    /** Immutable, packed read-only signal plan. Dispose it after consumption. */
    public static final class Plan {
        private final byte[] source;
        private final byte[] tags;
        private final int[] offsets;
        private final int[] lengths;
        private final int minor;
        private final int major;
        private final int utf8Count;
        private final String candidateHash;

        private Plan(byte[] source, byte[] tags, int[] offsets, int[] lengths,
                int minor, int major, int utf8Count) {
            this.source = source;
            this.tags = tags;
            this.offsets = offsets;
            this.lengths = lengths;
            this.minor = minor;
            this.major = major;
            this.utf8Count = utf8Count;
            this.candidateHash = candidateHash();
        }

        public int majorVersion() { return major; }

        public int minorVersion() { return minor; }

        public int constantPoolCount() { return tags.length; }

        public int utf8Count() { return utf8Count; }

        /** Returns 0 for the reserved unusable slot following LONG or DOUBLE. */
        public int tagAt(int cpIndex) {
            checkCpIndex(cpIndex);
            return tags[cpIndex] & 0xff;
        }

        /** Copied raw modified UTF-8 bytes; never an arena alias or a decoded Java string. */
        public byte[] copyUtf8At(int cpIndex) {
            requireUtf8(cpIndex);
            return Arrays.copyOfRange(source, offsets[cpIndex], offsets[cpIndex] + lengths[cpIndex]);
        }

        /** Candidate signal only; identical hashes DO NOT establish equivalent class behavior. */
        public String candidateHashHex() { return candidateHash; }

        /**
         * Serial deterministic CP-order interning in the existing primitive byte store.
         * Quota failure can leave already-admitted independent symbols in that owner; each
         * individual intern is atomic. No runtime class identity or Java reference is retained.
         */
        public Interned internInto(ClassSpace symbolStore) {
            Objects.requireNonNull(symbolStore, "symbolStore");
            int[] ids = new int[tags.length];
            Arrays.fill(ids, -1);
            for (int i = 1; i < tags.length; i++) {
                if ((tags[i] & 0xff) == 1) {
                    ids[i] = symbolStore.internRegion(source, offsets[i], lengths[i]);
                }
            }
            return new Interned(ids, utf8Count);
        }

        /**
         * Optional all-or-nothing CP symbolic admission for internal M3 owners.
         * Unlike internInto, quota or digest failure cannot publish only a prefix.
         * This package-local method is intentionally not a new public Java/JVM contract.
         */
        Interned internAtomicallyInto(ClassSpace symbolStore) {
            Objects.requireNonNull(symbolStore, "symbolStore");
            return new Interned(symbolStore.internUtf8Batch(source, tags, offsets, lengths),
                    utf8Count);
        }

        private void checkCpIndex(int cpIndex) {
            if (cpIndex <= 0 || cpIndex >= tags.length) {
                throw new IndexOutOfBoundsException("CP index: " + cpIndex);
            }
        }

        private void requireUtf8(int cpIndex) {
            checkCpIndex(cpIndex);
            if ((tags[cpIndex] & 0xff) != 1) {
                throw new IllegalArgumentException("CP entry is not CONSTANT_Utf8: " + cpIndex);
            }
        }

        private String candidateHash() {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException("Required SHA-256 provider missing", ex);
            }
            digest.update(DOMAIN);
            putU2(digest, major);
            putU2(digest, minor);
            putU2(digest, tags.length);
            for (int i = 1; i < tags.length; i++) {
                digest.update(tags[i]);
                if ((tags[i] & 0xff) == 1) {
                    putU2(digest, lengths[i]);
                    digest.update(source, offsets[i], lengths[i]);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        }

        private static void putU2(MessageDigest digest, int value) {
            digest.update((byte) (value >>> 8));
            digest.update((byte) value);
        }
    }

    /** Compact by-constant-pool symbolic ID map. No mutable backing array is exposed. */
    public static final class Interned {
        private final int[] ids;
        private final int utf8Count;

        private Interned(int[] ids, int utf8Count) {
            this.ids = ids;
            this.utf8Count = utf8Count;
        }

        public int constantPoolCount() { return ids.length; }

        public int utf8Count() { return utf8Count; }

        /** -1 means the CP slot is not a UTF8 symbol. IDs are owner-local, not JVM IDs. */
        public int symbolIdAt(int cpIndex) {
            if (cpIndex <= 0 || cpIndex >= ids.length) {
                throw new IndexOutOfBoundsException("CP index: " + cpIndex);
            }
            return ids[cpIndex];
        }
    }

    private static final class Cursor {
        private final byte[] data;
        private int offset;

        private Cursor(byte[] data) { this.data = data; }

        private int position() { return offset; }

        private int u1() {
            require(1);
            return data[offset++] & 0xff;
        }

        private int u2() { return (u1() << 8) | u1(); }

        private long u4() { return ((long) u2() << 16) | u2(); }

        private void skip(long length) {
            if (length < 0 || length > (long) data.length - offset) {
                throw new IllegalArgumentException("Truncated or oversized classfile component");
            }
            offset += (int) length;
        }

        private void require(int length) {
            if (offset > data.length - length) {
                throw new IllegalArgumentException("Truncated classfile component");
            }
        }
    }
}
