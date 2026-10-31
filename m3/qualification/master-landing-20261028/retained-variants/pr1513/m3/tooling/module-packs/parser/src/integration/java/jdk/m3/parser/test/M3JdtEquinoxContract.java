/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.parser.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.osgi.launch.EquinoxFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

/** Test-only framework bootstrap; no Eclipse or framework runtime is shipped in jdk.m3.parser. */
public final class M3JdtEquinoxContract {
    private M3JdtEquinoxContract() { }
    public static void main(String[] args) throws Exception {
        Path area = Files.createTempDirectory("m3-jdt-working-copy-");
        Map<String, String> options = new HashMap<>();
        options.put("org.osgi.framework.storage", area.resolve("framework").toString());
        options.put("osgi.instance.area", area.resolve("workspace").toUri().toString());
        options.put("org.osgi.framework.system.packages.extra", "jdk.m3.parser");
        options.put("eclipse.ignoreApp", "true");
        Framework framework = new EquinoxFactory().newFramework(options);
        framework.init();
        try {
            List<Bundle> bundles = new ArrayList<>();
            for (int i = 1; i < args.length; i++) bundles.add(framework.getBundleContext().installBundle(Path.of(args[i]).toUri().toString()));
            Bundle probe = framework.getBundleContext().installBundle(Path.of(args[0]).toUri().toString());
            framework.start();
            List<String> unresolved = new ArrayList<>();
            for (Bundle bundle : bundles) {
                if (bundle.getHeaders().get("Fragment-Host") != null) continue;
                try { bundle.start(); }
                catch (BundleException failure) { unresolved.add(bundle.getSymbolicName() + ": " + failure); }
            }
            if (!unresolved.isEmpty()) throw new AssertionError("Unresolved real target platform: " + unresolved);
            probe.start();
            Object result = probe.loadClass("jdk.m3.parser.test.M3JdtWorkspaceProbe").getMethod("run").invoke(null);
            System.out.println(result);
        } finally {
            framework.stop();
            if (framework.waitForStop(20_000).getType() == org.osgi.framework.FrameworkEvent.WAIT_TIMEDOUT) {
                throw new AssertionError("Equinox did not stop");
            }
        }
    }
}
