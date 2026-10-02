// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.index;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic structural/logic/SimHash fingerprint.
 *
 * <p>Hash mechanics are adapted from the Apache-2.0 Synexia MIndexAST reuse-key work
 * (MIndexASTReuseKeyFactory): framed domains, stable 64-bit mixing, child-first composition and
 * weighted SimHash voting. This class intentionally has no dependency on Synexia product modules.
 */
public record M3SemanticFingerprint(
        String exactSha256,
        String structuralSha256,
        String logicSha256,
        long structuralHash64,
        long logicHash64,
        long simHash64,
        String normalizedComposition) {

    public M3SemanticFingerprint {
        exactSha256 = requireSha(exactSha256, "exactSha256");
        structuralSha256 = requireSha(structuralSha256, "structuralSha256");
        logicSha256 = requireSha(logicSha256, "logicSha256");
        normalizedComposition = Objects.requireNonNull(normalizedComposition, "normalizedComposition");
    }

    /** Fingerprint one independently meaningful semantic leaf. */
    public static M3SemanticFingerprint leaf(
            String domain,
            List<String> structuralFeatures,
            List<String> logicFeatures,
            String exactSource) {
        String checkedDomain = token(domain, "domain");
        List<String> structure = List.copyOf(structuralFeatures);
        List<String> logic = List.copyOf(logicFeatures);

        String structuralSha = sha256Framed(join(checkedDomain, structure));
        String logicSha = sha256Framed(join(checkedDomain, logic));
        String exactSha = sha256Utf16(Objects.requireNonNull(exactSource, "exactSource"));

        long structural64 = domainHash("STRUCTURE:" + checkedDomain);
        for (String feature : structure) structural64 = combine(structural64, feature("S", feature));

        long logic64 = domainHash("LOGIC:" + checkedDomain);
        int[] votes = new int[Long.SIZE];
        for (String feature : logic) {
            long hashed = feature("L", feature);
            logic64 = combine(logic64, hashed);
            vote(votes, hashed, 1);
        }
        if (logic.isEmpty()) vote(votes, feature("L", checkedDomain), 1);

        String normalized = checkedDomain + "|" + String.join("|", logic);
        return new M3SemanticFingerprint(
                exactSha,
                structuralSha,
                logicSha,
                avalanche(structural64 ^ structure.size()),
                avalanche(logic64 ^ logic.size()),
                finishVotes(votes),
                normalized);
    }

    /** Child-first normalized composition used from METHOD through REPOSITORY scope. */
    public static M3SemanticFingerprint compose(
            String domain,
            List<M3SemanticFingerprint> orderedChildren) {
        String checkedDomain = token(domain, "domain");
        List<M3SemanticFingerprint> children = List.copyOf(orderedChildren);
        List<String> structural = new ArrayList<>(children.size());
        List<String> logic = new ArrayList<>(children.size());
        StringBuilder exact = new StringBuilder(checkedDomain);

        long structural64 = domainHash("STRUCTURE:" + checkedDomain);
        long logic64 = domainHash("LOGIC:" + checkedDomain);
        int[] votes = new int[Long.SIZE];

        for (int index = 0; index < children.size(); index++) {
            M3SemanticFingerprint child = Objects.requireNonNull(children.get(index), "child");
            String structuralFeature = index + ":" + child.structuralSha256();
            String logicFeature = index + ":" + child.logicSha256();
            structural.add(structuralFeature);
            logic.add(logicFeature);
            exact.append('|').append(index).append(':').append(child.exactSha256());

            structural64 = combine(structural64, child.structuralHash64());
            logic64 = combine(logic64, child.logicHash64());
            vote(votes, mix(child.simHash64() ^ feature("ORDINAL", Integer.toString(index))), 1);
        }
        if (children.isEmpty()) vote(votes, feature("EMPTY", checkedDomain), 1);

        return new M3SemanticFingerprint(
                sha256Utf16(exact),
                sha256Framed(join(checkedDomain, structural)),
                sha256Framed(join(checkedDomain, logic)),
                avalanche(structural64 ^ children.size()),
                avalanche(logic64 ^ children.size()),
                finishVotes(votes),
                checkedDomain + "|" + String.join("|", logic));
    }

    public String structuralHash64Hex() {
        return HexFormat.of().toHexDigits(structuralHash64);
    }

    public String logicHash64Hex() {
        return HexFormat.of().toHexDigits(logicHash64);
    }

    public String simHash64Hex() {
        return HexFormat.of().toHexDigits(simHash64);
    }

    private static List<String> join(String domain, List<String> features) {
        ArrayList<String> framed = new ArrayList<>(features.size() + 1);
        framed.add(domain);
        framed.addAll(features);
        return framed;
    }

    private static String sha256Framed(List<String> values) {
        MessageDigest digest = digest();
        for (String value : values) frame(digest, Objects.requireNonNull(value, "feature"));
        return HexFormat.of().formatHex(digest.digest());
    }

    public static String sha256Utf16(CharSequence value) {
        Objects.requireNonNull(value, "value");
        MessageDigest digest = digest();
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            digest.update((byte) (unit >>> 8));
            digest.update((byte) unit);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private static long domainHash(String value) {
        return avalanche(feature("DOMAIN", value) ^ 0x4d33494e44455821L);
    }

    private static long feature(String domain, String value) {
        long hash = 0xcbf29ce484222325L;
        hash = fnv(hash, domain);
        hash ^= 0xffL;
        hash *= 0x100000001b3L;
        return avalanche(fnv(hash, value));
    }

    private static long fnv(long seed, String text) {
        long hash = seed;
        for (byte value : text.getBytes(StandardCharsets.UTF_8)) {
            hash ^= Byte.toUnsignedInt(value);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private static long combine(long hash, long value) {
        return Long.rotateLeft(hash ^ avalanche(value + 0x9e3779b97f4a7c15L), 27)
                * 0x94d049bb133111ebL + 0x52dce729L;
    }

    private static long mix(long value) {
        return avalanche(value ^ 0x6a09e667f3bcc909L);
    }

    private static long avalanche(long value) {
        long result = value;
        result = (result ^ (result >>> 30)) * 0xbf58476d1ce4e5b9L;
        result = (result ^ (result >>> 27)) * 0x94d049bb133111ebL;
        return result ^ (result >>> 31);
    }

    private static void vote(int[] votes, long feature, int weight) {
        for (int bit = 0; bit < Long.SIZE; bit++) {
            votes[bit] += ((feature >>> bit) & 1L) == 0L ? -weight : weight;
        }
    }

    private static long finishVotes(int[] votes) {
        long result = 0L;
        for (int bit = 0; bit < votes.length; bit++) {
            if (votes[bit] >= 0) result |= 1L << bit;
        }
        return result;
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static String requireSha(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String token(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) throw new IllegalArgumentException(field);
        return checked;
    }
}
