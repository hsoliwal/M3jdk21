// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Content-addressed identity of one complete recipe-atom evidence manifest. */
public final class M3AtomEvidenceRoot {
    private static final String SCHEMA = "M3_RECIPE_ATOM_EVIDENCE_V1";

    private M3AtomEvidenceRoot() {}

    public static String of(
            M3BackportPacket packet,
            M3BackportPacketEvidence evidence) {
        M3BackportPacket checkedPacket = Objects.requireNonNull(packet, "packet");
        M3BackportPacketEvidence checkedEvidence =
                Objects.requireNonNull(evidence, "evidence");
        checkedEvidence.requireComplete(checkedPacket);

        MessageDigest digest = sha256();
        update(digest, SCHEMA);
        update(digest, checkedPacket.packetId());
        for (M3RecipeAtom atom : checkedPacket.atoms()) {
            M3RecipeAtomEvidence row = checkedEvidence.require(atom.id());
            update(digest, row.atomId());
            update(digest, row.contractRef());
            update(digest, row.documentationRef());
            update(digest, row.pattern());
            update(digest, row.iopRole());
            update(digest, row.junitProofRef());
            update(digest, Boolean.toString(row.fixedPointRequired()));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
