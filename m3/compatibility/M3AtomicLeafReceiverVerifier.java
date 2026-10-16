// SPDX-License-Identifier: Apache-2.0
// Target-only, source-independent verifier: does not load Synexia classes into M3JDK21.
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class M3AtomicLeafReceiverVerifier {
    private static final String LEAF_HEADER = "id\tproblem_category\tsource_fqn"
            + "\tcandidate_fqn\tsource_path\tcandidate_path\tsource_git_blob"
            + "\tcandidate_git_blob\tsource_sha256\tcandidate_sha256"
            + "\treceiving_plane\tsynexia_pr\tdecision\tjdk_runtime_admitted"
            + "\tcompatibility_gate";
    private static final String WRAPPER_HEADER = "id\tproblem_category"
            + "\tsynexia_source_pr\treceiving_plane\trequired_semantics\tdecision";
    private static final String SYNEXIA_PR =
            "https://github.com/hsoliwal/com.synexia/pull/11925";
    // SHA-1 Git blobs and SHA-256 exact file bytes on the committed Synexia candidate.
    private static final Map<String, List<String>> PINS = Map.of(
        "COMPLEXITY", List.of(
            "ea65df5741936c32a368b06bb502c1e4f554a067",
            "1a91af881d7e8c33c68a2be40d6526456727672f",
            "ec111da05f04c70fb3d5bd8ca03128df04c272e360481bc2aedaabc197f3463e",
            "fee1bf918b0f46a0ca7a51041314c6058964fb291f232a7b9cda3de15db9dc04"),
        "CAPABILITY", List.of(
            "536c53f1985759f9ff29376adc8c2173e0c2e04b",
            "e83918b83da20ed53c01b6a56bede6cf23312b67",
            "3fec840596457437c3f3fc8cd542a4f74f7c18d1ea3bc0f003cd92ac93c020f5",
            "74ab7e50597d288e3401379c6c307dacf5791aab685a9146fc4af672032bdc98"),
        "MEMBER_KIND", List.of(
            "9ba32db4528c74535a68c656b891d08924b75dc0",
            "e3cf6410aa8f17c9ce9351fe3e1aa28c31008a10",
            "76c8a68d9f1bafa36df848da02c1b00c4d738e7ff09c63d6761d6c8f892e20cb",
            "7cf7130665f9b4f5872d27c7e41580655f5490ba72fa4d4d48615d7217bf6b70"),
        "CLASS_FLAGS", List.of(
            "d8131ebce1c01a014a9320e2ec771673f2d06b5c",
            "e4f9dc223c4c0a424a42c5fedae0d54a6107a252",
            "bc3fcc4bc3f582e6565c1c8509038217e7a7360dbb0e290c66b03090c0f6961e",
            "43e85b0f935fc8e511b59f2d3a3aff79dfe54dc2a727b50178f178626ec7066d")
    );

    private static final Map<String, String> WRAPPER_PRS = Map.of(
            "FILTERED_ERASED_SET", "11913",
            "FILTERED_SORTED_MAP", "11920",
            "PRIMITIVE_SORTED_SELECTION", "11911",
            "PRIMITIVE_LIVE_FILTER", "11903");

    private M3AtomicLeafReceiverVerifier() { }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException("M3_RECEIVER_REFUSED: " + message);
        }
    }

    private static List<String[]> parse(List<String> lines, String header, int fields) {
        require(!lines.isEmpty() && lines.getFirst().equals(header),
                "ledger header changed");
        List<String[]> result = new ArrayList<>();
        for (int line = 1; line < lines.size(); line++) {
            String[] columns = lines.get(line).split("\\t", -1);
            require(columns.length == fields, "field count at line " + (line + 1));
            for (String column : columns) {
                require(!column.isBlank() && column.equals(column.strip()),
                        "blank or noncanonical field at line " + (line + 1));
            }
            result.add(columns);
        }
        return result;
    }

    private static void path(String value, String ownerName) {
        require(value.startsWith("synexia-mindex/") && value.endsWith(".java")
                && !value.contains("\\") && !value.contains("//")
                && !value.contains("/../") && !value.contains("/./")
                && !Path.of(value).isAbsolute(), "unapproved source path");
        require(value.endsWith("/" + ownerName + ".java"),
                "path/type mismatch: " + value);
    }

    public static int verifyLeaf(List<String> lines) {
        List<String[]> rows = parse(lines, LEAF_HEADER, 15);
        Set<String> seen = new HashSet<>();
        for (String[] row : rows) {
            String id = row[0];
            require(seen.add(id), "duplicate leaf id: " + id);
            List<String> pins = PINS.get(id);
            require(pins != null, "unknown leaf: " + id);
            String oldFqn = row[2], newFqn = row[3];
            require(oldFqn.startsWith("com.synexia.")
                    && oldFqn.substring(oldFqn.lastIndexOf('.') + 1).startsWith("MIndex"),
                    "non-MIndex source class");
            require(newFqn.startsWith("com.synexia.")
                    && newFqn.substring(newFqn.lastIndexOf('.') + 1).startsWith("M3")
                    && newFqn.substring(0, newFqn.lastIndexOf('.'))
                            .equals(oldFqn.substring(0, oldFqn.lastIndexOf('.'))),
                    "candidate family/package");
            path(row[4], oldFqn.substring(oldFqn.lastIndexOf('.') + 1));
            path(row[5], newFqn.substring(newFqn.lastIndexOf('.') + 1));
            for (int slot = 0; slot < 4; slot++) {
                require(row[6 + slot].equals(pins.get(slot)),
                        "content-addressed pin mismatch: " + id + " slot " + slot);
            }
            require(row[11].equals(SYNEXIA_PR), "unapproved source PR");
            require(row[12].equals("EVIDENCE_ONLY")
                    && row[13].equals("false"), "unproved JDK runtime admission");
            require(row[10].startsWith("M3_") && !row[10].contains("java.base")
                    && !row[10].contains("java.lang"), "invented target public owner");
            require(row[14].contains(";"), "missing contract gates");
        }
        require(seen.equals(PINS.keySet()), "four-leaf manifest is incomplete");
        return rows.size();
    }

    public static int verifyWrappers(List<String> lines) {
        List<String[]> rows = parse(lines, WRAPPER_HEADER, 6);
        Set<String> seen = new HashSet<>();
        for (String[] row : rows) {
            require(seen.add(row[0]), "duplicate wrapper id");
            String approved = WRAPPER_PRS.get(row[0]);
            require(approved != null && row[2].equals(
                    "https://github.com/hsoliwal/com.synexia/pull/" + approved),
                    "unverified wrapper PR");
            require(row[3].startsWith("M3_") && row[5].equals("EVIDENCE_ONLY"),
                    "wrapper promotion without proof");
            require(row[4].contains(";"), "missing wrapper semantic gates");
        }
        require(seen.equals(WRAPPER_PRS.keySet()), "wrapper catalogue incomplete");
        return rows.size();
    }

    private static void expectRefusal(Runnable corrupt) {
        try {
            corrupt.run();
            throw new AssertionError("negative control unexpectedly admitted");
        } catch (IllegalArgumentException expected) {
            // The valid oracle remains immutable; each variant is a private copy.
        }
    }

    private static void selfTest(List<String> leaves, List<String> wrappers) {
        List<String> duplicate = new ArrayList<>(leaves);
        duplicate.add(leaves.get(1));
        expectRefusal(() -> verifyLeaf(duplicate));

        List<String> forged = new ArrayList<>(leaves);
        forged.set(1, forged.get(1).replace("EVIDENCE_ONLY", "RUNTIME_ADMITTED"));
        expectRefusal(() -> verifyLeaf(forged));

        List<String> invalidBlob = new ArrayList<>(leaves);
        String[] cols = invalidBlob.get(1).split("\\t", -1);
        cols[7] = "0".repeat(40);
        invalidBlob.set(1, String.join("\t", cols));
        expectRefusal(() -> verifyLeaf(invalidBlob));

        List<String> wrappersPromoted = new ArrayList<>(wrappers);
        wrappersPromoted.set(1,
                wrappersPromoted.get(1).replace("EVIDENCE_ONLY", "RUNTIME_ADMITTED"));
        expectRefusal(() -> verifyWrappers(wrappersPromoted));
        System.out.println("M3_RECEIVER_REFUSAL_PASS cases=4");
    }

    public static void main(String[] args) throws IOException {
        boolean test = args.length == 3 && args[0].equals("--self-test");
        int offset = test ? 1 : 0;
        require(args.length == offset + 2, "expected optional --self-test plus two manifest paths");
        List<String> leaves = Files.readAllLines(Path.of(args[offset]), StandardCharsets.UTF_8);
        List<String> wrappers = Files.readAllLines(Path.of(args[offset + 1]),
                StandardCharsets.UTF_8);
        int leafCount = verifyLeaf(leaves);
        int wrapperCount = verifyWrappers(wrappers);
        if (test) selfTest(leaves, wrappers);
        System.out.println("M3_RECEIVER_CANDIDATES_PASS leaves=" + leafCount
                + " wrapper_workstreams=" + wrapperCount
                + " runtime_admitted=0");
    }
}
