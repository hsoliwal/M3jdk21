// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// Derivation: EXTRACTED_FIRST_14_GROUPS; see SOURCE_PROVENANCE.tsv.
// SPDX-License-Identifier: Apache-2.0
// Derived from StructuredEntropySelfTest.java at 2ab439480ecb0643615e85b42def954dbf3b5627.
// The first 14 incumbent groups and shared helpers retain their bodies unchanged.
// Unrelated idea-source, link-rank, and merge-readiness groups are excluded.
package jdk.m3.llm.projection;

import jdk.m3.llm.internal.Sha256;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dependency-free executable contract tests; no remote merge or external service is invoked. */
public final class ExtractiveProjectionSelfTest {
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static int groups;
    private static int assertions;
    private static int fixtures;
    private static Path directory;
    private ExtractiveProjectionSelfTest() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("TEST_DIRECTORY_REQUIRED");
        directory = Path.of(args[0]);
        if (!Files.isDirectory(directory)) throw new IllegalArgumentException("EXISTING_DIRECTORY_REQUIRED");
        test("coordinate_validation_and_utf8_order", ExtractiveProjectionSelfTest::coordinates);
        test("cell_identity_and_conflicting_writes", ExtractiveProjectionSelfTest::identity);
        test("version_prefix_and_namespace_isolation", ExtractiveProjectionSelfTest::scans);
        test("permutation_roots_and_immutable_results", ExtractiveProjectionSelfTest::permutations);
        test("bloom_inserted_keys_and_false_positive_exact_check", ExtractiveProjectionSelfTest::bloom);
        test("snapshot_empty_roundtrip_and_idempotence", ExtractiveProjectionSelfTest::roundtrip);
        test("snapshot_corruption_and_truncation", ExtractiveProjectionSelfTest::corruption);
        test("snapshot_noncanonical_and_malformed_fields", ExtractiveProjectionSelfTest::malformed);
        test("snapshot_input_resource_bounds", ExtractiveProjectionSelfTest::bounds);
        test("bm25_latest_version_and_namespace_projection", ExtractiveProjectionSelfTest::searchProjection);
        test("bm25_ties_normalization_and_query_bounds", ExtractiveProjectionSelfTest::searchRules);
        test("bm25_document_limit", ExtractiveProjectionSelfTest::searchLimit);
        test("extractive_mandatory_evidence_and_byte_budget", ExtractiveProjectionSelfTest::consolidation);
        test("hierarchy_preserves_omissions_and_rejects_conflicts", ExtractiveProjectionSelfTest::hierarchy);
        System.out.println("PASS groups=" + groups + " assertions=" + assertions);
    }

    private static void coordinates() throws Exception {
        expect(IllegalArgumentException.class, () -> new CellKey("n", "r", "f", "q", -1));
        expect(IllegalArgumentException.class, () -> new CellKey(" ", "r", "f", "q", 0));
        expect(IllegalArgumentException.class, () -> key("bad\0row", 1));
        expect(IllegalArgumentException.class, () -> key("\uD800", 1));
        expect(IllegalArgumentException.class, () -> key("a".repeat(513), 1));
        check(key("\uE000", 1).compareTo(key("\uD800\uDC00", 1)) < 0, "unsigned UTF8 order");
        check(key("r", Long.MAX_VALUE).compareTo(key("r", 0)) < 0, "descending versions without overflow");
        check(!new CellKey("ab", "c", "f", "q", 1).identity()
                .equals(new CellKey("a", "bc", "f", "q", 1).identity()), "framed identity");
    }

    private static void identity() throws Exception {
        TextInventoryCell a = cell("a", "source text");
        InventorySnapshot one = new InventorySnapshot(List.of(a, a));
        check(one.size() == 1 && one.get(a.key()).orElseThrow().equals(a), "idempotent cell");
        expect(IllegalArgumentException.class, () -> new InventorySnapshot(List.of(a, cell("a", "changed"))));
        expect(IllegalArgumentException.class, () -> new TextInventoryCell(a.key(), "text", "0".repeat(64)));
        check(a.contentSha256().equals(Sha256.digest(EntropyValues.utf8("source text"))), "exact hash");
        check(!a.contentSha256().equals(cell("b", "source text ").contentSha256()), "whitespace retained");
    }

    private static void scans() throws Exception {
        var old = new TextInventoryCell(key("ab", 1), "old");
        var latest = new TextInventoryCell(key("ab", 3), "new");
        var s = new InventorySnapshot(List.of(old, latest, cell("ac", "second"), cell("b", "stop"),
                new TextInventoryCell(new CellKey("other", "ab", "f", "q", 3), "secret"),
                new TextInventoryCell(new CellKey("n", "ab", "different", "q", 4), "other family")));
        check(s.scan("n", "a", "f", "q", 10, 10).equals(List.of(latest, cell("ac", "second"))), "prefix latest");
        check(s.scan("n", "ab", "f", "q", 2, 10).equals(List.of(old)), "as of");
        check(s.scan("n", "ab", "f", "q", 0, 10).isEmpty(), "before first version");
        check(s.scan("missing", "", "f", "q", 10, 10).isEmpty(), "namespace missing");
        check(s.scan("n", "", "f", "q", 10, 1).equals(List.of(latest)), "bounded scan");
        expect(IllegalArgumentException.class, () -> s.scan("", "", "f", "q", 10, 1));
        expect(IllegalArgumentException.class, () -> s.scan("n", "", "f", "q", -1, 1));
        expect(IllegalArgumentException.class, () -> s.scan("n", "", "f", "q", 10, 0));
    }

    private static void permutations() throws Exception {
        var values = new ArrayList<>(List.of(cell("c", "3"), cell("a", "1"), cell("b", "2")));
        var original = new InventorySnapshot(values);
        for (int i = 0; i < 6; i++) {
            Collections.rotate(values, 1);
            if (i % 2 == 0) Collections.reverse(values);
            check(original.root().equals(new InventorySnapshot(values).root()), "permutation root");
        }
        values.clear();
        check(original.size() == 3, "defensive input copy");
        expect(UnsupportedOperationException.class, () -> original.cells().clear());
    }

    private static void bloom() throws Exception {
        List<TextInventoryCell> cells = new ArrayList<>();
        for (int i = 0; i < 1000; i++) cells.add(cell("row-" + i, "value"));
        var snapshot = new InventorySnapshot(cells);
        var hint = new BloomHint(cells.stream().map(c -> c.key().rowToken()).toList());
        for (TextInventoryCell c : cells) {
            check(hint.mightContain(c.key().rowToken()) && snapshot.get(c.key()).isPresent(), "no false negative");
        }
        int positives = 0;
        for (int i = 1000; i < 10000; i++) {
            CellKey absent = key("row-" + i, 1);
            if (hint.mightContain(absent.rowToken())) {
                positives++;
                check(snapshot.get(absent).isEmpty(), "false positive is not presence");
            }
        }
        check(positives > 0, "fixture exercised false positives");
        check(!new BloomHint(List.of()).mightContain("anything"), "empty filter");
    }

    private static void roundtrip() throws Exception {
        for (InventorySnapshot s : List.of(new InventorySnapshot(List.of()),
                new InventorySnapshot(List.of(cell("unicode", "हिंदी 😀 exact\ntext"), cell("empty", ""))))) {
            Path p = SnapshotCodec.write(directory, s);
            byte[] before = Files.readAllBytes(p);
            check(SnapshotCodec.read(p).cells().equals(s.cells()), "round trip content");
            check(SnapshotCodec.read(p).root().equals(s.root()), "round trip root");
            check(SnapshotCodec.write(directory, s).equals(p), "idempotent path");
            check(Arrays.equals(before, Files.readAllBytes(p)), "idempotent bytes");
        }
    }

    private static void corruption() throws Exception {
        var s = new InventorySnapshot(List.of(cell("a", "source")));
        byte[] all = Files.readAllBytes(SnapshotCodec.write(directory, s));
        all[20] ^= 1;
        expect(IOException.class, () -> SnapshotCodec.read(fixture(all)));
        byte[] intact = Files.readAllBytes(SnapshotCodec.write(directory, s));
        for (int length : new int[] {0, 1, 43, intact.length - 1}) {
            byte[] cut = Arrays.copyOf(intact, length);
            expect(IOException.class, () -> SnapshotCodec.read(fixture(cut)));
        }
        expect(IOException.class, () -> SnapshotCodec.read(directory));
        expect(IOException.class, () -> SnapshotCodec.write(directory.resolve("not-created"), s));
    }

    private static void malformed() throws Exception {
        var a = cell("a", "text");
        var b = cell("b", "text");
        expect(IOException.class, () -> SnapshotCodec.read(sealed(raw(List.of(a, a)))));
        expect(IOException.class, () -> SnapshotCodec.read(sealed(raw(List.of(b, a)))));
        byte[] valid = raw(List.of(a));
        for (int[] patch : new int[][] {{0, 0}, {4, 2}, {8, -1}, {8, 100001}, {12, -1}, {12, 513}}) {
            byte[] bad = valid.clone();
            ByteBuffer.wrap(bad).putInt(patch[0], patch[1]);
            expect(IOException.class, () -> SnapshotCodec.read(sealed(bad)));
        }
        byte[] utf8 = valid.clone(); utf8[16] = (byte) 0xff;
        expect(IOException.class, () -> SnapshotCodec.read(sealed(utf8)));
        byte[] version = valid.clone(); ByteBuffer.wrap(version).putLong(32, -1L);
        expect(IOException.class, () -> SnapshotCodec.read(sealed(version)));
        byte[] hash = valid.clone(); hash[hash.length - 1] = (byte) 'z';
        expect(IOException.class, () -> SnapshotCodec.read(sealed(hash)));
        expect(IOException.class, () -> SnapshotCodec.read(sealed(Arrays.copyOf(valid, valid.length + 1))));
    }

    private static void bounds() throws Exception {
        expect(IllegalArgumentException.class, () -> new InventorySnapshot(Collections.nCopies(100001, cell("a", "x"))));
        expect(IllegalArgumentException.class, () -> cell("a", "x".repeat(1_048_577)));
        expect(IllegalArgumentException.class, () -> new BloomHint(Collections.nCopies(100001, "a")));
        List<TextInventoryCell> large = new ArrayList<>();
        String megabyte = "x".repeat(1_048_576);
        for (int i = 0; i < 65; i++) large.add(cell("large-" + i, megabyte));
        expect(IllegalArgumentException.class, () -> new InventorySnapshot(large));
    }

    private static void searchProjection() {
        var s = new InventorySnapshot(List.of(new TextInventoryCell(key("a", 1), "obsolete"),
                new TextInventoryCell(key("a", 2), "alpha beta"), cell("b", "alpha"),
                new TextInventoryCell(new CellKey("private", "secret", "f", "q", 1), "classified alpha")));
        var index = new Bm25Index(s, "n", "f", "q", Long.MAX_VALUE);
        check(index.documentCount() == 2, "namespace count");
        check(index.search("obsolete classified", 10).isEmpty(), "latest and namespace exclusion");
        check(index.search("alpha", 10).getFirst().cell().key().row().equals("b"), "length normalization");
        check(new Bm25Index(s, "n", "f", "q", 1).search("obsolete", 10).size() == 1, "as of index");
        check(index.sourceRoot().equals(s.root()), "index source binding");
    }

    private static void searchRules() throws Exception {
        var s = new InventorySnapshot(List.of(cell("z", "alpha"), cell("a", "alpha"), cell("unicode", "ＦＯＯ")));
        var index = new Bm25Index(s, "n", "f", "q", 1);
        check(index.search("alpha", 10).getFirst().cell().key().row().equals("a"), "stable tie");
        check(index.search("alpha alpha", 10).equals(index.search("alpha", 10)), "query term dedup");
        check(index.search("foo", 10).size() == 1, "NFKC token normalization");
        check(index.search("   ", 10).isEmpty(), "empty query");
        check(new Bm25Index(new InventorySnapshot(List.of()), "n", "f", "q", 1).search("alpha", 1).isEmpty(), "empty index");
        expect(IllegalArgumentException.class, () -> index.search("x", 0));
        expect(IllegalArgumentException.class, () -> index.search("x".repeat(8193), 1));
        expect(IllegalArgumentException.class, () -> index.search("alpha ".repeat(257), 1));
        expect(IllegalArgumentException.class, () -> new Bm25Index.Hit(cell("bad", "x"), Double.NaN));
    }

    private static void searchLimit() throws Exception {
        List<TextInventoryCell> many = new ArrayList<>();
        for (int i = 0; i <= Bm25Index.MAX_DOCUMENTS; i++) many.add(cell("doc-" + i, "word"));
        expect(IllegalArgumentException.class, () -> new Bm25Index(new InventorySnapshot(many), "n", "f", "q", 1));
    }

    private static void consolidation() throws Exception {
        var a = cell("a", "alpha"); var b = cell("b", "beta");
        var hits = hits(a, b);
        var p = ExtractiveConsolidator.consolidate(hits, 4, Set.of(b.key()));
        check(p.text().equals("beta") && p.selected().equals(List.of(b)), "mandatory first");
        check(p.omitted().equals(List.of(a)) && p.utf8Bytes() == 4, "omission retained");
        expect(IllegalArgumentException.class, () -> ExtractiveConsolidator.consolidate(hits, 3, Set.of(b.key())));
        expect(IllegalArgumentException.class, () -> ExtractiveConsolidator.consolidate(hits, 100, Set.of(key("missing", 1))));
        expect(IllegalArgumentException.class, () -> ExtractiveConsolidator.consolidate(hits(a, a), 100, Set.of()));
        var emoji = cell("emoji", "😀");
        check(ExtractiveConsolidator.consolidate(hits(emoji), 4, Set.of()).utf8Bytes() == 4, "UTF8 budget");
        check(ExtractiveConsolidator.consolidate(hits(emoji), 3, Set.of()).selected().isEmpty(), "no partial characters");
        check(ExtractiveConsolidator.consolidate(hits, 11, Set.of()).text().equals("alpha\n\nbeta"), "separator budget");
    }

    private static void hierarchy() throws Exception {
        var a = cell("a", "alpha"); var b = cell("b", "beta");
        var left = ExtractiveConsolidator.consolidate(hits(a), 0, Set.of());
        var right = ExtractiveConsolidator.consolidate(hits(b), 4, Set.of());
        var joined = ExtractiveConsolidator.combine(List.of(left, right), 100, Set.of(a.key()));
        check(joined.selected().equals(List.of(a, b)), "rehydrate omitted source");
        check(joined.root().equals(ExtractiveConsolidator.combine(List.of(right, left), 100, Set.of(a.key())).root()), "hierarchy replay");
        var conflict = ExtractiveConsolidator.consolidate(hits(cell("a", "changed")), 0, Set.of());
        expect(IllegalArgumentException.class, () -> ExtractiveConsolidator.combine(List.of(left, conflict), 100, Set.of()));
    }

    private static CellKey key(String row, long version) { return new CellKey("n", row, "f", "q", version); }
    private static TextInventoryCell cell(String row, String text) { return new TextInventoryCell(key(row, 1), text); }
    private static List<Bm25Index.Hit> hits(TextInventoryCell... cells) {
        return Arrays.stream(cells).map(c -> new Bm25Index.Hit(c, 1)).toList();
    }
    private static byte[] raw(List<TextInventoryCell> cells) throws IOException {
        var buffer = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(buffer)) {
            out.writeInt(0x53454931); out.writeInt(1); out.writeInt(cells.size());
            for (TextInventoryCell cell : cells) {
                for (String text : List.of(cell.key().namespace(), cell.key().row(), cell.key().family(), cell.key().qualifier())) string(out, text);
                out.writeLong(cell.key().version()); string(out, cell.text()); string(out, cell.contentSha256());
            }
        }
        return buffer.toByteArray();
    }
    private static void string(DataOutputStream out, String value) throws IOException {
        byte[] bytes = EntropyValues.utf8(value); out.writeInt(bytes.length); out.write(bytes);
    }
    private static Path sealed(byte[] body) throws IOException {
        byte[] result = Arrays.copyOf(body, body.length + 32);
        System.arraycopy(HexFormat.of().parseHex(Sha256.digest(body)), 0, result, body.length, 32);
        return fixture(result);
    }
    private static Path fixture(byte[] bytes) throws IOException {
        Path p = directory.resolve("malformed-" + (++fixtures) + ".sesi");
        return Files.write(p, bytes, StandardOpenOption.CREATE_NEW);
    }
    private static void test(String name, Checked action) throws Exception {
        action.run(); groups++; System.out.println("PASS " + name);
    }
    private static void check(boolean condition, String message) {
        assertions++; if (!condition) throw new AssertionError(message);
    }
    private static void expect(Class<? extends Throwable> type, Checked action) throws Exception {
        assertions++;
        try { action.run(); }
        catch (Throwable actual) {
            if (type.isInstance(actual)) return;
            throw new AssertionError("Expected " + type.getName() + " but got " + actual, actual);
        }
        throw new AssertionError("Expected " + type.getName());
    }
}
