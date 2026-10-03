// SPDX-License-Identifier: Apache-2.0
package com.m3.pack;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class M3ModuleClosureTest {
    private static final List<ModuleDescriptor> SYSTEM = List.of(
            ModuleDescriptor.newModule("java.base").packages(Set.of("java.lang")).build());

    private static M3ModuleArtifact artifact(ModuleDescriptor descriptor) {
        return new M3ModuleArtifact(Path.of(descriptor.name() + ".jar"), "hash", descriptor, 65, List.of());
    }

    @Test void computesSortedImmutableClosureAndDoesNotRequireStaticOnlyDependency() {
        var leaf = ModuleDescriptor.newModule("example.leaf").packages(Set.of("leaf")).build();
        var top = ModuleDescriptor.newModule("example.top").requires("example.leaf")
                .requires(Set.of(ModuleDescriptor.Requires.Modifier.STATIC), "example.optional")
                .packages(Set.of("top")).build();
        var result = new M3ModuleClosure().resolve(SYSTEM, List.of(artifact(top), artifact(leaf)), Set.of("example.top"));
        assertEquals(List.of("example.leaf", "example.top", "java.base"), result.stream().toList());
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test void refusesMissingDependencyEmptyRootAutomaticModuleCollisionAndSplitPackage() {
        var leaf = ModuleDescriptor.newModule("example.leaf").packages(Set.of("shared")).build();
        var other = ModuleDescriptor.newModule("example.other").packages(Set.of("shared")).build();
        var resolver = new M3ModuleClosure();
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(SYSTEM, List.of(), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(SYSTEM, List.of(), Set.of("missing")));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(SYSTEM,
                List.of(artifact(leaf), artifact(leaf)), Set.of("example.leaf")));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(SYSTEM,
                List.of(artifact(SYSTEM.getFirst())), Set.of("java.base")));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(SYSTEM,
                List.of(artifact(ModuleDescriptor.newAutomaticModule("example.auto").build())), Set.of("example.auto")));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(SYSTEM,
                List.of(artifact(leaf), artifact(other)), Set.of("example.leaf", "example.other")));
        var absent = ModuleDescriptor.newModule("example.absent").requires("example.required").build();
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(SYSTEM,
                List.of(artifact(absent)), Set.of("example.absent")));
    }
}
