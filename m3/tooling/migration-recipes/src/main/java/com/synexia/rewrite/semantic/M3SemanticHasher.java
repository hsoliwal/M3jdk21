// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.semantic;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Canonical M3 semantic-source fingerprint primitives.
 *
 * <p>Fingerprints are deterministic convergence/search signals, never arbitrary equivalence proof
 * or mutation/promotion authority.</p>
 */
public final class M3SemanticHasher {
    private static final Pattern PURE_INT_ATOM = Pattern.compile(
            "\\{intm3\\$pureIntAtom=(.*);returnm3\\$pureIntAtom;\\}",
            Pattern.DOTALL);
    private static final Pattern ARCHITECTURE = Pattern.compile("M3-(?:IOP|ATOM):[^*\\r\\n]*");

    private M3SemanticHasher() {}

    public static Fingerprint method(String source) {
        String canonical = canonicalJava(source);
        int body = canonical.indexOf('{');
        String contract = body < 0 ? canonical : canonical.substring(0, body);
        String logic = body < 0 ? "" : normalizeAdmittedAtoms(canonical.substring(body));
        String architecture = architectureMaterial(source);
        String contractHash = sha256("M3-CONTRACT-v1", contract);
        String logicHash = sha256("M3-LOGIC-v1", logic);
        String architectureHash = sha256("M3-ARCH-v1", architecture);
        String behavioralHash = sha256("M3-BEHAVIORAL-v1", contractHash, logicHash);
        String wholeHash = sha256("M3-WHOLE-v1", behavioralHash, architectureHash);
        return new Fingerprint(contractHash, logicHash, architectureHash, behavioralHash, wholeHash);
    }

    public static Fingerprint file(String source) {
        String canonical = normalizeAdmittedAtoms(canonicalJava(source));
        String architecture = architectureMaterial(source);
        String contractHash = sha256("M3-FILE-CONTRACT-v1", canonicalTypeSurface(canonical));
        String logicHash = sha256("M3-FILE-LOGIC-v1", canonical);
        String architectureHash = sha256("M3-FILE-ARCH-v1", architecture);
        String behavioralHash = sha256("M3-FILE-BEHAVIORAL-v1", contractHash, logicHash);
        String wholeHash = sha256("M3-FILE-WHOLE-v1", behavioralHash, architectureHash);
        return new Fingerprint(contractHash, logicHash, architectureHash, behavioralHash, wholeHash);
    }

    static String canonicalJava(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
                i = skipLineComment(source, i + 2);
                continue;
            }
            if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
                i = skipBlockComment(source, i + 2);
                continue;
            }
            if (c == '"' && source.startsWith("\"\"\"", i)) {
                i = copyTextBlock(source, i, out);
                continue;
            }
            if (c == '"' || c == '\'') {
                i = copyQuoted(source, i, c, out);
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static int copyQuoted(String source, int start, char quote, StringBuilder out) {
        int i = start;
        boolean escaped = false;
        while (i < source.length()) {
            char c = source.charAt(i++);
            out.append(c);
            if (i == start + 1) continue;
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == quote) {
                break;
            }
        }
        return i;
    }

    private static int copyTextBlock(String source, int start, StringBuilder out) {
        int i = start;
        out.append("\"\"\"");
        i += 3;
        while (i < source.length()) {
            if (source.startsWith("\"\"\"", i)) {
                out.append("\"\"\"");
                return i + 3;
            }
            char c = source.charAt(i++);
            out.append(c);
            if (c == '\\' && i < source.length()) {
                out.append(source.charAt(i++));
            }
        }
        return i;
    }

    private static int skipLineComment(String source, int i) {
        while (i < source.length() && source.charAt(i) != '\n' && source.charAt(i) != '\r') i++;
        return i;
    }

    private static int skipBlockComment(String source, int i) {
        while (i + 1 < source.length()
                && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) {
            i++;
        }
        return Math.min(source.length(), i + 2);
    }

    private static String normalizeAdmittedAtoms(String source) {
        Matcher matcher = PURE_INT_ATOM.matcher(source);
        StringBuffer normalized = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(
                    normalized,
                    Matcher.quoteReplacement("{return" + matcher.group(1) + ";}"));
        }
        matcher.appendTail(normalized);
        return normalized.toString();
    }

    private static String architectureMaterial(String source) {
        String comments = commentMaterial(source);
        Matcher matcher = ARCHITECTURE.matcher(comments);
        StringBuilder material = new StringBuilder();
        while (matcher.find()) {
            if (!material.isEmpty()) material.append('|');
            material.append(matcher.group().trim());
        }
        return material.toString();
    }

    /** Extract only actual Java comments; string/char/text-block payloads are never marker authority. */
    private static String commentMaterial(String source) {
        StringBuilder comments = new StringBuilder();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '"' && source.startsWith("\"\"\"", i)) {
                i = skipTextBlock(source, i);
                continue;
            }
            if (c == '"' || c == '\'') {
                i = skipQuoted(source, i, c);
                continue;
            }
            if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
                int start = i + 2;
                i = skipLineComment(source, start);
                comments.append(source, start, i).append('\n');
                continue;
            }
            if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
                int start = i + 2;
                i = skipBlockComment(source, start);
                int end = Math.max(start, i - 2);
                comments.append(source, start, end).append('\n');
                continue;
            }
            i++;
        }
        return comments.toString();
    }

    private static int skipQuoted(String source, int start, char quote) {
        int i = start + 1;
        boolean escaped = false;
        while (i < source.length()) {
            char c = source.charAt(i++);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == quote) {
                break;
            }
        }
        return i;
    }

    private static int skipTextBlock(String source, int start) {
        int i = start + 3;
        while (i < source.length()) {
            if (source.startsWith("\"\"\"", i)) return i + 3;
            if (source.charAt(i) == '\\' && i + 1 < source.length()) i += 2;
            else i++;
        }
        return i;
    }

    private static String canonicalTypeSurface(String canonicalFile) {
        StringBuilder surface = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < canonicalFile.length(); i++) {
            char c = canonicalFile.charAt(i);
            if (c == '{') {
                depth++;
                if (depth <= 1) surface.append(c);
            } else if (c == '}') {
                if (depth <= 1) surface.append(c);
                depth--;
            } else if (depth <= 1) {
                surface.append(c);
            }
        }
        return surface.toString();
    }

    private static String sha256(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                digest.update((byte) 0);
                digest.update(part.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** Layered semantic-source fingerprint. */
    public record Fingerprint(
            String contractHash,
            String logicHash,
            String architectureHash,
            String behavioralHash,
            String wholeHash) {}
}
