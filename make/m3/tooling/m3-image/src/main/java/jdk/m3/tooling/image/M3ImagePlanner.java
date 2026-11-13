/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Ordered inventory, analysis, and plan construction. */
public final class M3ImagePlanner {
    private M3ImagePlanner() {
    }

    /** Constructs and seals a plan. */
    public static M3ImagePlan plan(M3JdkTools tools, List<Path> classPath, String mainClass,
                                   M3Mode mode, M3Profile profile, Set<String> dynamicRoots,
                                   Set<M3FeatureKind> allowedFeatures, Path output)
            throws IOException, InterruptedException {
        M3ClassPathScanner.Result scan = M3ClassPathScanner.scan(classPath);
        if (!scan.classes().containsKey(mainClass)) {
            throw new IOException("Main class not found in application classpath: " + mainClass);
        }
        Set<String> effectiveDynamicRoots = new TreeSet<>(dynamicRoots);
        effectiveDynamicRoots.addAll(scan.serviceProviders());
        M3ReachabilityAnalyzer.Result reachability = M3ReachabilityAnalyzer.analyze(
                scan.classes(), mainClass, effectiveDynamicRoots, allowedFeatures, profile, mode);
        if (mode == M3Mode.CLOSED_WORLD) {
            if (!reachability.missingRoots().isEmpty()) {
                throw new IOException("Closed-world roots are missing: " + reachability.missingRoots());
            }
            List<M3ReachabilityAnalyzer.Feature> unresolved = reachability.features().stream()
                    .filter(feature -> !feature.resolved()).toList();
            if (!unresolved.isEmpty()) {
                throw new IOException("Closed-world dynamic constraints are unresolved: " + unresolved);
            }
        }
        Path work = output.toAbsolutePath().normalize().getParent();
        if (work == null) {
            work = Path.of(".").toAbsolutePath().normalize();
        }
        M3ModuleResolver.Result modules = M3ModuleResolver.resolve(tools, classPath, mode, work);
        return M3PlanIO.write(output, mainClass, mode, tools, scan, reachability, modules, profile);
    }
}
