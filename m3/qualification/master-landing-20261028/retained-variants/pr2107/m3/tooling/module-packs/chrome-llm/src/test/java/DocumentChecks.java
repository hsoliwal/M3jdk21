/* SPDX-License-Identifier: Apache-2.0 */
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import jdk.m3.llm.document.Desm;
import jdk.m3.llm.document.Lexical;
import jdk.m3.llm.document.TreeNode;

/** Independent exact-source and isolation checks; labels remain candidates. */
public final class DocumentChecks {
    private static int checks;
    private DocumentChecks() {}
    private static void yes(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static String sha(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
    public static void main(String[] arguments) throws Exception {
        String source = "# Root\r\nMUST retain sources because evidence matters.\n"
                + "- first\n  - nested TODO\n## Next\n"
                + "```java\n// NEVER becomes a prose constraint here\n```\n"
                + "See https://example.invalid/result on 2026-10-29.\n";
        List<TreeNode> nodes = Desm.extract("source.md", source);
        int previous = 0;
        for (TreeNode node : nodes) {
            yes(node.start() == previous, "contiguous exact source ranges");
            String exact = source.substring(node.start(), node.end());
            yes(node.label().equals(exact.stripTrailing()), "label uses exact line spelling");
            yes(node.payloadHash().equals(sha(exact)), "hash binds full line including newline");
            previous = node.end();
        }
        yes(previous == source.length(), "full source addressability");
        yes(nodes.get(0).kind().equals("HEADING"), "heading recognized");
        yes(nodes.get(1).kind().contains("CONSTRAINT_CANDIDATE"), "constraint remains candidate");
        yes(nodes.get(1).kind().contains("CAUSAL_CANDIDATE"), "causality remains candidate");
        yes(nodes.get(3).parent().equals(nodes.get(2).id()), "nested list parent retained");
        yes(nodes.get(3).kind().contains("OBLIGATION"), "pending obligation retained");
        yes(nodes.get(6).kind().equals("EMBEDDED_SOURCE"), "code fence isolated from prose tags");
        yes(nodes.get(8).kind().contains("PROVENANCE_CANDIDATE"), "source link retained");
        yes(nodes.get(8).kind().contains("TEMPORAL_CANDIDATE"), "time retained");
        yes(nodes.equals(Desm.extract("source.md", source)), "DESM replay deterministic");
        List<TreeNode> open = Desm.extract("open.md", "~~~\nMUST remain code\n");
        yes(open.getLast().kind().equals("TYPED_RESIDUE"), "unclosed fence explicit residue");
        yes(open.get(1).kind().equals("EMBEDDED_SOURCE"), "unclosed fence still isolated");

        String java = "class X {\r\n String s = \"fake class Y {}\"; // TODO fake\r\n"
                + " /** Javadoc MUST survive */ int n = 1;\n}\n";
        Lexical.View view = Lexical.scan(java);
        yes(view.safe(), "ordinary lexical input accepted");
        yes(view.masked().length() == java.length(), "UTF-16 offsets preserved");
        for (int i = 0; i < java.length(); i++) {
            if (java.charAt(i) == '\r' || java.charAt(i) == '\n')
                yes(view.masked().charAt(i) == java.charAt(i), "line ending preserved");
        }
        for (Lexical.Span span : view.protectedSpans()) {
            for (int i = span.start(); i < span.end(); i++) {
                char c = java.charAt(i);
                yes(view.masked().charAt(i) == (c == '\r' || c == '\n' ? c : ' '),
                        "protected span is offset-stable mask");
            }
        }
        yes(!view.masked().contains("fake"), "code-looking literal/comment cannot become structure");
        yes(view.tokens().stream().anyMatch(t -> t.text().equals("\"fake class Y {}\"")),
                "literal raw spelling retained for fingerprints");
        yes(Lexical.scan("class X { /* unfinished").residues().contains("UNCLOSED_COMMENT"),
                "unclosed comment explicit residue");
        String escaped = "class X { " + '\\' + "u0061; }";
        yes(!Lexical.scan(escaped).safe(), "Unicode escape requires explicit mapping");
        System.out.println("DOCUMENT_SOURCE_PARITY_PASS checks=" + checks);
    }
}
