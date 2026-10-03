// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.modulepack;

import java.lang.module.ModuleDescriptor;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Deterministic selected-root graph validator. Pattern/IOP role: Specification.
 * Required edges expand closure; static requires do not. Service providers are
 * explicit roots, not guessed. jlink and application execution remain mandatory.
 */
public final class M3ModuleGraph {
    /** Stateless graph validator. */
    public M3ModuleGraph() {}

    /**
     * Validate one closed candidate graph and return module-name-sorted artifacts.
     * Duplicate module names are always refused. Split packages and cycles are
     * checked only inside the selected runtime closure.
     *
     * @param artifacts inspected archives, including the selected JDK's JMODs
     * @param roots explicitly selected module names
     * @return immutable, deterministic selected closure
     */
    public List<M3ModuleArtifact> resolve(Collection<M3ModuleArtifact> artifacts,
            Collection<String> roots) {
        TreeMap<String, M3ModuleArtifact> all = index(artifacts);
        if (roots.isEmpty()) throw new IllegalArgumentException("EMPTY_ROOTS");
        TreeSet<String> selected = new TreeSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>(new TreeSet<>(roots));
        while (!pending.isEmpty()) {
            String name = pending.removeFirst();
            M3ModuleArtifact artifact = all.get(name);
            if (artifact == null) throw new IllegalArgumentException("MISSING_MODULE: " + name);
            if (!selected.add(name)) continue;
            required(artifact.descriptor()).forEach(pending::addLast);
        }
        checkPackages(all, selected);
        checkCycles(all, selected);
        return selected.stream().map(all::get).toList();
    }

    private static TreeMap<String, M3ModuleArtifact> index(Collection<M3ModuleArtifact> artifacts) {
        TreeMap<String, M3ModuleArtifact> result = new TreeMap<>();
        for (M3ModuleArtifact artifact : artifacts) {
            ModuleDescriptor descriptor = artifact.descriptor();
            if (descriptor.isAutomatic()) throw new IllegalArgumentException("AUTOMATIC_MODULE");
            if (result.putIfAbsent(descriptor.name(), artifact) != null) {
                throw new IllegalArgumentException("DUPLICATE_MODULE: " + descriptor.name());
            }
        }
        return result;
    }

    private static List<String> required(ModuleDescriptor descriptor) {
        return descriptor.requires().stream()
                .filter(edge -> !edge.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC))
                .map(ModuleDescriptor.Requires::name).sorted().toList();
    }

    private static void checkPackages(Map<String, M3ModuleArtifact> all, Collection<String> selected) {
        TreeMap<String, String> owners = new TreeMap<>();
        for (String name : selected) {
            for (String pkg : new TreeSet<>(all.get(name).descriptor().packages())) {
                String owner = owners.putIfAbsent(pkg, name);
                if (owner != null) {
                    throw new IllegalArgumentException("SPLIT_PACKAGE: " + pkg + ": " + owner + "," + name);
                }
            }
        }
    }

    private static void checkCycles(Map<String, M3ModuleArtifact> all, Collection<String> selected) {
        TreeSet<String> remaining = new TreeSet<>(selected);
        while (!remaining.isEmpty()) {
            List<String> leaves = remaining.stream()
                    .filter(name -> required(all.get(name).descriptor()).stream()
                            .noneMatch(remaining::contains)).toList();
            if (leaves.isEmpty()) throw new IllegalArgumentException("MODULE_CYCLE: " + remaining);
            remaining.removeAll(leaves);
        }
    }
}
