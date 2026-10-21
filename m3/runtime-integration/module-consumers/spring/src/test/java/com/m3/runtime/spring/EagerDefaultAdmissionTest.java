/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.runtime.spring;

import com.m3.file.M3ModuleResources;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Refusal witness: the eager index contract cannot replace arbitrary lazy reader effects. */
class EagerDefaultAdmissionTest {
    private static ModuleReference failingReference(List<String> effects, IOException failure) {
        return new ModuleReference(ModuleDescriptor.newModule("failing.fixture").build(), URI.create("m3-test:/failing")) {
            @Override public ModuleReader open() {
                return new ModuleReader() {
                    @Override public Stream<String> list() {
                        return Stream.concat(Stream.of("first.class"), Stream.generate(() -> {
                            throw new UncheckedIOException(failure);
                        }));
                    }
                    @Override public Optional<URI> find(String name) {
                        effects.add("find:" + name);
                        return Optional.of(URI.create("file:/" + name));
                    }
                    @Override public void close() { effects.add("close:reader"); }
                };
            }
        };
    }

    @Test
    void lateEnumerationFailureRefusesEagerReplacementOfOriginalLazyPipeline() {
        List<String> originalEffects = new ArrayList<>();
        IOException originalFailure = new IOException("late list failure");
        ModuleReference original = failingReference(originalEffects, originalFailure);
        UncheckedIOException lazy = assertThrows(UncheckedIOException.class, () -> {
            try (ModuleReader reader = original.open(); Stream<String> names = reader.list()) {
                names.filter(name -> { originalEffects.add("match:" + name); return true; })
                        .map(name -> {
                            try { return reader.find(name); }
                            catch (IOException failure) { throw new UncheckedIOException(failure); }
                        }).forEach(uri -> originalEffects.add("add:" + uri.orElseThrow()));
            }
        });
        List<String> eagerEffects = new ArrayList<>();
        IOException eagerFailure = new IOException("late list failure");
        ModuleReference eager = failingReference(eagerEffects, eagerFailure);
        UncheckedIOException indexed = assertThrows(UncheckedIOException.class, () ->
                M3ModuleResources.open(eager, Integer.MAX_VALUE, Long.MAX_VALUE));
        assertSame(originalFailure, lazy.getCause());
        assertSame(eagerFailure, indexed.getCause());
        assertEquals(List.of("match:first.class", "find:first.class", "add:file:/first.class", "close:reader"), originalEffects);
        assertEquals(List.of("close:reader"), eagerEffects);
        assertNotEquals(originalEffects, eagerEffects,
                "do not admit eager default scanning by merely documenting callback reordering");
    }
}
