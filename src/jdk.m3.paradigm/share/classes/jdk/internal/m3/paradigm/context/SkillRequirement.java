/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * One named capability requirement inside a composite skill/DAG template.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/cognition/SkillRequirement.java}
 * blob {@code 53534ba244037411ab98f00b5a517fde4c510c8f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record SkillRequirement(
        String id,
        String capability,
        SortedSet<String> tags,
        int taskScalePermille,
        int budgetSharePermille,
        Set<String> dependsOn) {

    public SkillRequirement {
        id = SemanticHash.requireId(id, "requirement id");
        capability = SemanticHash.requireId(capability, "capability");
        tags = SemanticHash.canonicalTokens(Objects.requireNonNull(tags, "tags"));
        if (taskScalePermille < 1 || taskScalePermille > 1_000) {
            throw new IllegalArgumentException("taskScalePermille must be 1..1000");
        }
        if (budgetSharePermille < 1 || budgetSharePermille > 1_000) {
            throw new IllegalArgumentException("budgetSharePermille must be 1..1000");
        }
        TreeSet<String> dependencies = new TreeSet<>(Objects.requireNonNull(dependsOn, "dependsOn"));
        dependencies.forEach(value -> SemanticHash.requireId(value, "dependency id"));
        if (dependencies.contains(id)) throw new IllegalArgumentException("requirement cannot depend on itself");
        dependsOn = Collections.unmodifiableSet(dependencies);
    }

    public static SkillRequirement of(String id, String capability, Collection<String> tags,
            int taskScalePermille, int budgetSharePermille, Collection<String> dependsOn) {
        return new SkillRequirement(id, capability, SemanticHash.canonicalTokens(tags),
                taskScalePermille, budgetSharePermille, Set.copyOf(dependsOn));
    }

    public static SkillRequirement leaf(String id, String capability) {
        return of(id, capability, java.util.List.of(), 1_000, 1_000, java.util.List.of());
    }

    /** Root of the planning demand independent of local sibling naming/dependency wiring. */
    public String semanticRoot() {
        java.util.ArrayList<String> fields = new java.util.ArrayList<>(java.util.List.of(
                capability, Integer.toString(taskScalePermille), Integer.toString(budgetSharePermille)));
        fields.addAll(tags);
        return SemanticHash.sha256("COP-SKILL-DEMAND/1", fields);
    }

    public String root() {
        java.util.ArrayList<String> fields = new java.util.ArrayList<>(java.util.List.of(
                id, capability, Integer.toString(taskScalePermille), Integer.toString(budgetSharePermille)));
        fields.addAll(tags);
        fields.add("--deps--");
        fields.addAll(dependsOn.stream().sorted().toList());
        return SemanticHash.sha256("COP-SKILL-REQUIREMENT/1", fields);
    }
}
