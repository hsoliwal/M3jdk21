// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import com.m3.rewrite.scope.M3EditScope;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** One immutable source-changing recipe atom inside a backport packet. */
public record M3RecipeAtom(
        String id,
        M3EditScope scope,
        boolean scopePromotionApproved,
        String workRef,
        List<String> dependsOn) {

    public M3RecipeAtom {
        id = atomId(id);
        scope = Objects.requireNonNull(scope, "scope");
        workRef = required(workRef, "workRef");
        dependsOn = List.copyOf(Objects.requireNonNull(dependsOn, "dependsOn"));
        if (dependsOn.stream().anyMatch(id::equals)) {
            throw new IllegalArgumentException("self dependency: " + id);
        }
        if (dependsOn.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("blank atom dependency: " + id);
        }
        if (new HashSet<>(dependsOn).size() != dependsOn.size()) {
            throw new IllegalArgumentException("duplicate atom dependency: " + id);
        }
    }

    private static String atomId(String value) {
        String checked = required(value, "id");
        if (!checked.matches("[a-z0-9][a-z0-9-]{0,79}")) {
            throw new IllegalArgumentException("invalid atom id: " + checked);
        }
        return checked;
    }

    private static String required(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
