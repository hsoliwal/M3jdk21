/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.LocalM3Arena;
import jdk.m3.text.LocalM3StringPiece;
import jdk.m3.text.M3NumberSpace;
import jdk.m3.text.M3RopeTextIndex;
import jdk.m3.text.M3StringPiece;
import jdk.m3.text.StorageIdentity;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Exported-API contract of owned string pieces: String rendering of leaf and joined pieces,
 * retained-backing accounting of range views, storage-identity refusals, number-space refusal
 * messages for non-String spellings and the physical node census of the rope text index.
 */
public final class M3StringPieceContractTest {
    private static long checks;

    private M3StringPieceContractTest() {}

    private interface Action {
        void run() throws Exception;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }

    private static void same(Object expected, Object actual, String message) {
        check(
                expected.equals(actual),
                message + ": expected <" + expected + "> but was <" + actual + ">");
    }

    private static void refused(Class<? extends Throwable> type, String reason, Action action) {
        try {
            action.run();
        } catch (Throwable thrown) {
            check(
                    type.isInstance(thrown)
                            && (reason == null || reason.equals(thrown.getMessage())),
                    "wrong refusal " + thrown);
            return;
        }
        throw new AssertionError("accepted: " + reason);
    }

    private static void pieces() {
        LocalM3Arena arena = new LocalM3Arena();
        LocalM3StringPiece latin =
                arena.copyLatin1("café au lait".getBytes(StandardCharsets.ISO_8859_1));
        LocalM3StringPiece utf16 = arena.copyUtf16("😀 \uDC00x".toCharArray());
        same("café au lait", latin.toString(), "Latin-1 leaf rendering");
        same("😀 \uDC00x", utf16.toString(), "UTF-16 leaf keeps unpaired surrogates");
        same(12, latin.retainedBackingBytes(), "Latin-1 backing is one byte per unit");
        same(10, utf16.retainedBackingBytes(), "UTF-16 backing is two bytes per unit");
        LocalM3StringPiece word = latin.subSequence(5, 7);
        same("au", word.toString(), "range view rendering");
        same(12, word.retainedBackingBytes(), "range view retains the whole owned payload");
        same(
                latin.storageIdentity(),
                word.storageIdentity(),
                "range view keeps its owner identity");
        check(
                !latin.storageIdentity().equals(utf16.storageIdentity()),
                "each copy has its own record");
        M3StringPiece joined =
                M3StringPiece.join(word, utf16.subSequence(2, 5), latin.subSequence(0, 0));
        same("au \uDC00x", joined.toString(), "joined pieces render in order");
        same(
                "au \uDC00x",
                M3StringPiece.join(joined, joined.subSequence(0, 0)).toString(),
                "joined of joined");
    }

    private static void identities() {
        UUID owner = new UUID(1, 2);
        StorageIdentity identity = new StorageIdentity(owner, 1, 1);
        same(owner, identity.owner(), "owner");
        refused(
                IllegalArgumentException.class,
                "nonpositive identity",
                () -> new StorageIdentity(owner, 0, 1));
        refused(
                IllegalArgumentException.class,
                "nonpositive identity",
                () -> new StorageIdentity(owner, 1, 0));
        refused(
                IllegalArgumentException.class,
                "nonpositive identity",
                () -> new StorageIdentity(owner, -1, -1));
        refused(NullPointerException.class, null, () -> new StorageIdentity(null, 1, 1));
    }

    private static void numberRefusals() {
        M3NumberSpace numbers = M3NumberSpace.INSTANCE;
        same(
                "42",
                numbers.parse(new StringBuilder("42")).toString(),
                "any CharSequence spelling parses");
        refused(
                NumberFormatException.class,
                "non-decimal number spelling: 4x",
                () -> numbers.parse(new StringBuilder("4x")));
        refused(
                NumberFormatException.class,
                "non-canonical number spelling: 07",
                () ->
                        numbers.parse(
                                new LocalM3Arena()
                                        .copyLatin1("07".getBytes(StandardCharsets.US_ASCII))));
        refused(
                NumberFormatException.class,
                "number outside 0..10000: 10001",
                () -> numbers.parse(new StringBuilder("10001")));
        refused(
                NumberFormatException.class,
                "non-decimal number spelling: -1",
                () -> numbers.parse("-1"));
    }

    private static void ropeNodes() {
        LocalM3Arena arena = new LocalM3Arena();
        same(0, M3RopeTextIndex.of().nodeCount(), "empty rope has no nodes");
        LocalM3StringPiece leaf = arena.copyLatin1("ab".getBytes(StandardCharsets.US_ASCII));
        same(1, M3RopeTextIndex.of(leaf).nodeCount(), "single leaf is the root");
        LocalM3StringPiece[] sixteen = new LocalM3StringPiece[16];
        java.util.Arrays.fill(sixteen, leaf);
        same(17, M3RopeTextIndex.of(sixteen).nodeCount(), "one full branch over sixteen leaves");
        LocalM3StringPiece[] seventeen = java.util.Arrays.copyOf(sixteen, 17);
        seventeen[16] = leaf;
        M3RopeTextIndex rope = M3RopeTextIndex.of(seventeen);
        same(20, rope.nodeCount(), "two branches plus a root over seventeen leaves");
        same(17, rope.segmentCount(), "segments are the non-empty leaves");
        same(34L, rope.utf16Length(), "units");
    }

    public static void main(String[] args) {
        pieces();
        identities();
        numberRefusals();
        ropeNodes();
        System.out.println("M3_STRING_PIECE_CONTRACT_PASS checks=" + checks);
    }
}
