// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.semantic;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Canonical M3 semantic fingerprint primitives. Fingerprints are proof signals, not equivalence proofs. */
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
                i += 2;
                while (i < source.length() && source.charAt(i) != '\n' && source.charAt(i) != '\r') i++;
                continue;
            }
            if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < source.length()
                        && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) i++;
                i = Math.min(source.length(), i + 2);
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
            if (escaped) escaped = false;
            else if (c == '\\') escaped = true;
            else if (c == quote) break;
        }
        return i;
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
        Matcher matcher = ARCHITECTURE.matcher(source);
        StringBuilder material = new StringBuilder();
        while (matcher.find()) {
            if (!material.isEmpty()) material.append('|');
            material.append(matcher.group().trim());
        }
        return material.toString();
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

    /** Layered semantic fingerprint. */
    public record Fingerprint(
            String contractHash,
            String logicHash,
            String architectureHash,
            String behavioralHash,
            String wholeHash) {}
}
