// SPDX-License-Identifier: Apache-2.0
package com.m3.pack;

import java.lang.module.ModuleDescriptor;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Pure dependency Validator; jlink remains the authority for full JPMS resolution. */
public final class M3ModuleClosure {
    /**
     * Returns deterministic required module names. Static-only requirements are not forced into
     * runtime closure. Refuses collisions even for unused supplied artifacts; split-package checks
     * cover the reachable modules. Cycles and readability consistency are additionally checked by jlink.
     */
    public Set<String> resolve(Collection<ModuleDescriptor> system,
            Collection<M3ModuleArtifact> artifacts, Set<String> roots) {
        if (roots.isEmpty()) {
            throw new IllegalArgumentException("EMPTY_ROOTS");
        }
        Map<String, ModuleDescriptor> modules = new TreeMap<>();
        for (ModuleDescriptor descriptor : system) {
            put(modules, descriptor);
        }
        for (M3ModuleArtifact artifact : artifacts) {
            put(modules, artifact.descriptor());
        }
        TreeSet<String> queue = new TreeSet<>(roots);
        TreeSet<String> reached = new TreeSet<>();
        while (!queue.isEmpty()) {
            String name = queue.pollFirst();
            if (!reached.add(name)) {
                continue;
            }
            ModuleDescriptor descriptor = modules.get(name);
            if (descriptor == null) {
                throw new IllegalArgumentException("MISSING_MODULE: " + name);
            }
            for (ModuleDescriptor.Requires requirement : descriptor.requires()) {
                if (!requirement.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC)) {
                    queue.add(requirement.name());
                }
            }
        }
        Map<String, String> owners = new TreeMap<>();
        for (String name : reached) {
            for (String packageName : new TreeSet<>(modules.get(name).packages())) {
                String previous = owners.putIfAbsent(packageName, name);
                if (previous != null) {
                    throw new IllegalArgumentException("SPLIT_PACKAGE: " + packageName
                            + " in " + previous + " and " + name);
                }
            }
        }
        return java.util.Collections.unmodifiableSortedSet(reached);
    }

    private static void put(Map<String, ModuleDescriptor> modules, ModuleDescriptor descriptor) {
        if (descriptor.isAutomatic()) {
            throw new IllegalArgumentException("AUTOMATIC_MODULE: " + descriptor.name());
        }
        if (modules.putIfAbsent(descriptor.name(), descriptor) != null) {
            throw new IllegalArgumentException("DUPLICATE_MODULE: " + descriptor.name());
        }
    }
}
