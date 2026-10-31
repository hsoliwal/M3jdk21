// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import com.m3.rewrite.scope.M3EditScope;
import java.util.List;
import java.util.Objects;

/** One immutable recipe/work atom in a backport DAG. */
public record M3DagNode(
        String id,
        M3DagKind kind,
        M3EditScope scope,
        boolean mutating,
        boolean serialPromotion,
        boolean scopePromotionApproved,
        String workRef,
        List<String> dependsOn) {

    public M3DagNode {
        id = required(id, "id");
        kind = Objects.requireNonNull(kind, "kind");
        scope = Objects.requireNonNull(scope, "scope");
        workRef = Objects.toString(workRef, "").strip();
        dependsOn = List.copyOf(Objects.requireNonNull(dependsOn, "dependsOn"));
        if (dependsOn.stream().anyMatch(id::equals)) {
            throw new IllegalArgumentException("self dependency: " + id);
        }
        if (dependsOn.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("blank dependency: " + id);
        }
        if (kind == M3DagKind.RECIPE && workRef.isEmpty()) {
            throw new IllegalArgumentException("recipe node requires workRef: " + id);
        }
        if (kind == M3DagKind.PROMOTION && !serialPromotion) {
            throw new IllegalArgumentException("promotion must be serial: " + id);
        }
    }

    private static String required(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
