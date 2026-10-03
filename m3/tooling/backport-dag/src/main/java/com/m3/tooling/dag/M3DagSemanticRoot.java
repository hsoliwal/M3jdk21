// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** Content-addressed semantic identity of one validated M3 recipe DAG. */
public final class M3DagSemanticRoot {
    private static final String SCHEMA = "M3_RECIPE_DAG_V1";

    private M3DagSemanticRoot() {}

    public static String of(M3RecipeDag dag) {
        MessageDigest digest = sha256();
        update(digest, SCHEMA);
        for (M3DagNode node : Objects.requireNonNull(dag, "dag").topologicalOrder()) {
            update(digest, node.id());
            update(digest, node.kind().name());
            update(digest, node.scope().name());
            update(digest, Boolean.toString(node.mutating()));
            update(digest, Boolean.toString(node.serialPromotion()));
            update(digest, Boolean.toString(node.scopePromotionApproved()));
            update(digest, node.workRef());
            List<String> dependencies = node.dependsOn().stream().sorted().toList();
            update(digest, Integer.toString(dependencies.size()));
            dependencies.forEach(value -> update(digest, value));
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
