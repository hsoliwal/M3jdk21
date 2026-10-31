// Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.filesystem.launcher;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.wiring.FrameworkWiring;
import static org.junit.jupiter.api.Assertions.*;

/** Starts real Equinox; the scenario and backend are loaded by OSGi, never by this launcher. */
final class M3ResourcesIT {
    @TempDir Path temporary;

    @Test void eclipseResourcesConsumesTheRegisteredM3Filesystem() throws Exception {
        boolean integrated = ModuleLayer.boot().findModule("jdk.m3fs").isPresent();
        Path dependencies = Path.of(required("m3.eclipse.dependencies"));
        Path provider = Path.of(required("m3.eclipse.provider"));
        Path scenarios = Path.of(required("m3.eclipse.scenarios"));
        Map<String, String> configuration = new HashMap<>();
        configuration.put(Constants.FRAMEWORK_STORAGE, temporary.resolve("framework").toString());
        configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        configuration.put("osgi.instance.area", temporary.resolve("workspace").toUri().toASCIIString());
        configuration.put("eclipse.ignoreApp", "true");
        configuration.put("eclipse.consoleLog", "true");
        if (integrated) {
            configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "com.m3.fs;version=\"0.1.0\"");
            configuration.put(Constants.FRAMEWORK_BOOTDELEGATION, "com.m3.fs");
        }
        FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).findFirst().orElseThrow();
        Framework framework = factory.newFramework(configuration);
        List<Bundle> installed = new ArrayList<>();
        try {
            framework.init();
            BundleContext context = framework.getBundleContext();
            try (var files = Files.list(dependencies)) {
                for (Path jar : files.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().toList()) {
                    String symbolicName = symbolicName(jar);
                    if (symbolicName == null || symbolicName.equals("org.eclipse.osgi")) continue;
                    if (integrated && symbolicName.equals("com.m3.fs")) continue;
                    if (!runtimeBundle(symbolicName)) continue;
                    installed.add(context.installBundle(jar.toUri().toASCIIString()));
                }
            }
            installed.add(context.installBundle(provider.toUri().toASCIIString()));
            Bundle scenarioBundle = context.installBundle(scenarios.toUri().toASCIIString());
            installed.add(scenarioBundle);
            framework.start();
            assertTrue(framework.adapt(FrameworkWiring.class).resolveBundles(installed), () -> unresolved(installed));
            List<Bundle> starts = new ArrayList<>(installed);
            starts.sort(Comparator.comparingInt(M3ResourcesIT::startPriority));
            for (Bundle bundle : starts) {
                if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) bundle.start();
            }
            Class<?> scenario = scenarioBundle.loadClass("com.m3.eclipse.filesystem.it.M3WorkspaceScenario");
            String[] completed;
            try {
                completed = (String[]) scenario.getMethod("run", String.class, boolean.class)
                        .invoke(null, temporary.resolve("workspace.m3fs").toString(), integrated);
            } catch (InvocationTargetException problem) {
                Throwable cause = problem.getCause();
                if (cause instanceof Exception exception) throw exception;
                if (cause instanceof Error error) throw error;
                throw problem;
            }
            assertArrayEquals(new String[] {
                "extension-discovery", "efs-uri-round-trip", "workspace-linked-folders",
                "workspace-create-read", "workspace-append-replace", "m3fs-to-m3fs-copy",
                "m3fs-to-local-copy", "local-to-m3fs-copy", "workspace-move",
                "workspace-refresh-external-change", "workspace-delete-refresh",
                "workspace-native-timestamp", "snapshot-view-isolation",
                "workspace-project-reopen", "durable-image-reopen"
            }, completed);
            System.out.println("M3FS_WORKSPACE_MODE=" + (integrated ? "jdk.m3fs" : "backend-osgi-bundle"));
            System.out.println("M3FS_WORKSPACE_JAVA_HOME=" + System.getProperty("java.home"));
            System.out.println("M3FS_WORKSPACE_CHECKS=" + completed.length + " " + String.join(",", completed));
        } finally {
            framework.stop();
            FrameworkEvent stopped = framework.waitForStop(10_000);
            assertNotEquals(FrameworkEvent.WAIT_TIMEDOUT, stopped.getType(), "Equinox must stop cleanly");
        }
    }

    private static String required(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing test property " + key);
        return value;
    }

    private static String symbolicName(Path jar) throws IOException {
        try (JarFile file = new JarFile(jar.toFile())) {
            if (file.getManifest() == null) return null;
            String value = file.getManifest().getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
            if (value == null) return null;
            return value.split(";", 2)[0].strip();
        }
    }

    private static boolean runtimeBundle(String name) {
        return name.startsWith("org.eclipse.") || name.startsWith("org.osgi.")
                || name.startsWith("com.sun.jna") || name.equals("org.apache.felix.scr") || name.equals("com.m3.fs");
    }

    private static int startPriority(Bundle bundle) {
        return switch (bundle.getSymbolicName()) {
            case "org.eclipse.equinox.common" -> 10;
            case "org.eclipse.core.jobs" -> 20;
            case "org.eclipse.equinox.registry" -> 30;
            case "org.eclipse.equinox.preferences" -> 40;
            case "org.eclipse.core.contenttype" -> 50;
            case "org.eclipse.core.expressions" -> 60;
            case "org.eclipse.equinox.app" -> 70;
            case "org.eclipse.core.runtime" -> 80;
            case "org.apache.felix.scr" -> 90;
            case "org.eclipse.core.resources" -> 100;
            case "com.m3.eclipse.filesystem" -> 110;
            case "com.m3.eclipse.filesystem.scenarios" -> 120;
            default -> 0;
        };
    }

    private static String unresolved(List<Bundle> bundles) {
        StringBuilder result = new StringBuilder("Unresolved Equinox bundles:");
        for (Bundle bundle : bundles) {
            if (bundle.getState() == Bundle.INSTALLED) {
                result.append('\n').append(bundle.getSymbolicName())
                        .append(" imports=").append(bundle.getHeaders().get(Constants.IMPORT_PACKAGE))
                        .append(" requires=").append(bundle.getHeaders().get(Constants.REQUIRE_BUNDLE));
            }
        }
        return result.toString();
    }
}

