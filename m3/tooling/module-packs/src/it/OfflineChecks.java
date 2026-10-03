// SPDX-License-Identifier: Apache-2.0
package com.m3.pack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/** Additional JDK-only smoke; this is not a substitute for JUnit/JaCoCo. */
public final class OfflineChecks {
    private OfflineChecks() {}
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        Files.createDirectories(root);
        var classes = M3ArchiveFixtures.classes(root, "example.offline");
        var contents = new LinkedHashMap<>(classes);
        contents.put("META-INF/versions/23/sample/Leaf.class",
                M3ArchiveFixtures.version(classes.get("sample/Leaf.class"), 67, 0));
        Path jar = M3ArchiveFixtures.jar(root.resolve("offline.jar"), contents, true);
        var fact = new M3ModuleInspector().inspect(jar);
        require(fact.maximumClassVersion() == 65, "multi-release Java21 view");
        require(fact.descriptor().name().equals("example.offline"), "explicit descriptor");
        var system = java.lang.module.ModuleFinder.ofSystem().findAll().stream().map(java.lang.module.ModuleReference::descriptor).toList();
        require(new M3ModuleClosure().resolve(system, List.of(fact), Set.of("example.offline")).contains("java.base"), "requires closure");
        Path lock = M3ArchiveFixtures.lock(root, M3ArchiveFixtures.row(jar, "example.offline"));
        Path staged = new M3PackTool().stage(lock, root.resolve("staged"));
        require(new M3PackTool().verify(staged).getFirst().sha256().equals(fact.sha256()), "staged identity");
        classes.remove("module-info.class");
        Path automatic = M3ArchiveFixtures.jar(root.resolve("automatic.jar"), classes, false);
        try { new M3ModuleInspector().inspect(automatic); throw new AssertionError("automatic module admitted"); }
        catch (IllegalArgumentException expected) { require(expected.getMessage().contains("EXPLICIT_DESCRIPTOR_REQUIRED"), "automatic module refusal"); }
        try { new M3ModuleClosure().resolve(system, List.of(fact, fact), Set.of("example.offline")); throw new AssertionError("duplicate admitted"); }
        catch (IllegalArgumentException expected) { require(expected.getMessage().contains("DUPLICATE_MODULE"), "duplicate refusal"); }
        System.out.println("PASS: offline archive/closure/staging checks (not JUnit)");
    }
    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
