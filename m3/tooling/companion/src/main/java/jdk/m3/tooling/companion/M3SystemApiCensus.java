// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.companion;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Read-only, bounded JDK system-module API candidate census. This inventories
 * class-resource names in unconditionally exported packages, NOT public Java
 * declarations or admitted M3 APIs. Reflection/JPMS/ABI/ownership admission
 * remains the responsibility of M3CompanionPlan and the receiving JDK.
 * Resource scanning does not load classes. Optional review delegates to the
 * existing planner, which may resolve class metadata without initializing types.
 * No Java/JDK source is rewritten.
 */
public final class M3SystemApiCensus {
    /** Candidate binary identity; neither API visibility nor module resolution is asserted. */
    public record ApiType(String moduleName, String binaryName) {
        public ApiType {
            Objects.requireNonNull(moduleName, "moduleName");
            Objects.requireNonNull(binaryName, "binaryName");
            if (moduleName.isBlank() || binaryName.isBlank()) {
                throw new IllegalArgumentException("empty module or class identity");
            }
        }
    }

    private M3SystemApiCensus() { }

    /**
     * Enumerate each class resource in an unconditionally exported package of
     * one installed JDK module. Limits count ALL visited module resources, not
     * only matches; exhaustion refuses the entire result without truncation.
     */
    public static List<ApiType> scanModule(String moduleName, int maxResources) throws IOException {
        Objects.requireNonNull(moduleName, "moduleName");
        if (!moduleName.matches("[A-Za-z][A-Za-z0-9_.]*") || maxResources < 1) {
            throw new IllegalArgumentException("invalid module or positive resource budget");
        }
        ModuleReference ref = ModuleFinder.ofSystem().find(moduleName)
                .orElseThrow(() -> new IllegalArgumentException("system module not installed: " + moduleName));
        Set<String> exported = new HashSet<>();
        for (var specification : ref.descriptor().exports()) {
            if (!specification.isQualified()) {
                exported.add(specification.source());
            }
        }
        if (exported.isEmpty()) {
            return List.of();
        }
        TreeSet<String> classes = new TreeSet<>();
        int visited = 0;
        try (ModuleReader reader = ref.open(); Stream<String> resources = reader.list()) {
            Iterator<String> iterator = resources.iterator();
            while (iterator.hasNext()) {
                if (++visited > maxResources) {
                    throw new IllegalStateException("module resource budget exceeded: " + moduleName);
                }
                String path = iterator.next();
                if (!path.endsWith(".class")) {
                    continue;
                }
                int slash = path.lastIndexOf('/');
                if (slash <= 0) {
                    continue;
                }
                String pkg = path.substring(0, slash).replace('/', '.');
                if (!exported.contains(pkg)) {
                    continue;
                }
                String leaf = path.substring(slash + 1, path.length() - ".class".length());
                if (!leaf.matches("[A-Za-z_$][A-Za-z0-9_$]*") || leaf.matches(".*\\$[0-9]+.*")) {
                    continue;
                }
                classes.add(pkg + "." + leaf);
            }
        }
        return classes.stream().map(name -> new ApiType(moduleName, name)).toList();
    }

    /**
     * Discover candidates for every installed JDK system module, in stable
     * module/binary-name order. Bounds refuse the entire census, never return
     * a misleading partial 'all APIs' inventory.
     */
    public static List<ApiType> scanInstalled(int maxModules, int maxResourcesPerModule)
            throws IOException {
        if (maxModules < 1 || maxResourcesPerModule < 1) {
            throw new IllegalArgumentException("positive module and resource budgets required");
        }
        List<ModuleReference> modules = ModuleFinder.ofSystem().findAll().stream()
                .sorted(Comparator.comparing(reference -> reference.descriptor().name())).toList();
        if (modules.size() > maxModules) {
            throw new IllegalStateException("installed module budget exceeded");
        }
        List<ApiType> result = new ArrayList<>();
        for (ModuleReference module : modules) {
            result.addAll(scanModule(module.descriptor().name(), maxResourcesPerModule));
        }
        return List.copyOf(result);
    }
    /**
     * Review one module's resource candidates using the existing canonical
     * M3CompanionPlan visibility, export, collision, and reserved-owner gates.
     * REVIEW is not an accepted SDK surface; no class initialization occurs.
     * The resource scan refuses an incomplete budget before review begins.
     */
    public static List<M3CompanionPlan.Row> reviewModule(
            String moduleName, int maxResources, Set<String> reservedCompanions)
            throws IOException {
        Objects.requireNonNull(reservedCompanions, "reservedCompanions");
        return reviewResources(scanModule(moduleName, maxResources), reservedCompanions);
    }

    /**
     * Review all installed JDK system modules. Installed but unresolved
     * modules retain the planner's explicit HOLD_MODULE_NOT_RESOLVED status;
     * they are not resolved or loaded speculatively by this scan.
     */
    public static List<M3CompanionPlan.Row> reviewInstalled(
            int maxModules, int maxResourcesPerModule, Set<String> reservedCompanions)
            throws IOException {
        Objects.requireNonNull(reservedCompanions, "reservedCompanions");
        return reviewResources(scanInstalled(maxModules, maxResourcesPerModule),
                reservedCompanions);
    }

    private static List<M3CompanionPlan.Row> reviewResources(
            List<ApiType> resources, Set<String> reservedCompanions) {
        List<M3CompanionPlan.Request> requests = new ArrayList<>(resources.size());
        for (ApiType resource : resources) {
            requests.add(new M3CompanionPlan.Request(
                    resource.moduleName(), resource.binaryName()));
        }
        return M3CompanionPlan.inspect(requests, reservedCompanions);
    }

}
