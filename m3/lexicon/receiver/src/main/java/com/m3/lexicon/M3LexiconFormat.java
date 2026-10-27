// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

/**
 * Every lexicon/word/language image format known on the Synexia and M3JDK21 pins, keyed by its 8-byte
 * ASCII magic at offset 0. Field facts are recorded only where verified at a pin (see
 * {@code m3/lexicon/LEXICON_RECEIVER_LEDGER.tsv}); {@code versionOffset < 0} means "not verified".
 */
public enum M3LexiconFormat {
    /** M3StringPool mmap lexicon; sorted UTF-16LE records, SHA-256, at most 1 GiB. */
    M3LEX001(0x4d334c4558303031L, ByteOrder.BIG_ENDIAN, 64, 8, 1, 3, "java.lang.M3StringPool.Lexicon"),
    /** SharedArrayPool crash-safe arena; CRC32C, UTF-16BE. */
    SYNARR01(0x53594e4152523031L, ByteOrder.BIG_ENDIAN, 64, 8, 1, 1, "java.lang.M3StringPool.Lexicon"),
    /** IndexWordRowsImage format 3, 128-byte rows. */
    MIWROWS1(0x4d4957524f575331L, ByteOrder.BIG_ENDIAN, 104, 8, 3, 3, "com.m3.text.M3WordRows"),
    /** dictshared packed multi-language lexicon (little-endian). */
    SYNDICT1(0x53594e4449435431L, ByteOrder.LITTLE_ENDIAN, 128, 8, 1, 1, "NONE"),
    /** dictshared frozen tuples (layout not inspected). */
    SYNTULP1(0x53594e54554c5031L, ByteOrder.LITTLE_ENDIAN, 8, -1, 0, 0, "NONE"),
    /** dictshared semantic graph (layout not inspected). */
    SYNSEMG1(0x53594e53454d4731L, ByteOrder.LITTLE_ENDIAN, 8, -1, 0, 0, "NONE"),
    /** interned-language string store (72-byte header). */
    SNXSTR01(0x534e585354523031L, ByteOrder.BIG_ENDIAN, 72, -1, 0, 0, "NONE"),
    /** interned-language indexed lexicon artifact (40-byte header). */
    SNXLEX01(0x534e584c45583031L, ByteOrder.BIG_ENDIAN, 40, -1, 0, 0, "NONE"),
    /** MIndex compiled bigram image (176-byte header). */
    M3PAIR01(0x4d33504149523031L, ByteOrder.BIG_ENDIAN, 176, 8, 1, 1, "NONE"),
    /** MIndex compiled language image (224-byte header). */
    M3LIMG01(0x4d334c494d473031L, ByteOrder.BIG_ENDIAN, 224, 8, 1, 1, "NONE"),
    /** MIndex language geometry image (256-byte header; byte order not verified). */
    M3GEO001(0x4d3347454f303031L, ByteOrder.BIG_ENDIAN, 256, -1, 0, 0, "NONE"),
    /** PROPOSED fold-id sidecar (this receiver). */
    M3FOLD01(0x4d33464f4c443031L, ByteOrder.BIG_ENDIAN, 64, 8, 1, 1, "com.m3.lexicon.M3LexiconFoldIds"),
    /** PROPOSED (pattern, word id) regex transfer rows (this receiver). */
    M3RXW001(0x4d33525857303031L, ByteOrder.BIG_ENDIAN, 128, 8, 1, 1, "com.m3.lexicon.M3RegexWordTransferRows"),
    /** PROPOSED separator boundary sidecar (this receiver). */
    M3SEPB01(0x4d33534550423031L, ByteOrder.BIG_ENDIAN, 64, 8, 1, 1, "com.m3.lexicon.M3SeparatorLexicon");

    private final long magic;
    private final ByteOrder order;
    private final int headerBytes;
    private final int versionOffset;
    private final int minVersion;
    private final int maxVersion;
    private final String m3Owner;

    M3LexiconFormat(long magic, ByteOrder order, int headerBytes, int versionOffset,
            int minVersion, int maxVersion, String m3Owner) {
        this.magic = magic;
        this.order = order;
        this.headerBytes = headerBytes;
        this.versionOffset = versionOffset;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.m3Owner = m3Owner;
    }

    /** @return the 8 magic bytes read as one big-endian long */
    public long magic() {
        return magic;
    }

    /** @return byte order of the header fields after the magic */
    public ByteOrder order() {
        return order;
    }

    /** @return fixed header size in bytes */
    public int headerBytes() {
        return headerBytes;
    }

    /** @return the M3JDK21 receiving owner, or {@code NONE} */
    public String m3Owner() {
        return m3Owner;
    }

    /** Identifies the format by its magic, without reading past the first 8 bytes. */
    public static Optional<M3LexiconFormat> sniff(ByteBuffer image) {
        if (image.limit() < 8) {
            return Optional.empty();
        }
        long head = image.duplicate().order(ByteOrder.BIG_ENDIAN).getLong(0);
        for (M3LexiconFormat format : values()) {
            if (format.magic == head) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    /** Header gate: magic, minimum size and (where verified) the version field. */
    public static M3LexiconFormat checkHeader(ByteBuffer image) throws M3LexiconFormatException {
        M3LexiconFormat format = sniff(image)
                .orElseThrow(() -> new M3LexiconFormatException("unknown lexicon image magic"));
        M3LexiconFormatException.require(image.limit() >= format.headerBytes, "truncated header");
        if (format.versionOffset >= 0) {
            int version = image.duplicate().order(format.order).getInt(format.versionOffset);
            M3LexiconFormatException.require(
                    version >= format.minVersion && version <= format.maxVersion, "unsupported version");
        }
        return format;
    }
}
