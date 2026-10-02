// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3LibraryApiSurfaceRecipeTest {
    @Test
    void computesDeterministicModuleAndLibraryApiRoots() {
        SourceFile base = parse(
                "src/java.base/share/classes/p/PublicApi.java",
                """
                package p;
                public class PublicApi {
                    public int x;
                    protected int y;
                    private int z;

                    public void a() {}
                    protected void b() {}
                    private void c() {}
                }

                class Hidden {
                    public int leaked;
                    public void hidden() {}
                }
                """);
        SourceFile contract = parse(
                "src/java.base/share/classes/p/Contract.java",
                """
                package p;
                public interface Contract {
                    int VALUE = 1;
                    int apply(int value);
                    private int helper() { return 1; }
                }
                """);
        SourceFile logging = parse(
                "src/java.logging/share/classes/p/Other.java",
                """
                package p;
                public final class Other {
                    public void log() {}
                }
                """);

        List<M3LibraryApiSurfaceRecipe.Row> left =
                run(List.of(base, contract, logging));
        List<M3LibraryApiSurfaceRecipe.Row> right =
                run(List.of(logging, contract, base));

        assertEquals(left.size(), right.size());
        for (int index = 0; index < left.size(); index++) {
            assertEquals(left.get(index).scopeKind(), right.get(index).scopeKind());
            assertEquals(left.get(index).scopeName(), right.get(index).scopeName());
            assertEquals(left.get(index).symbolCount(), right.get(index).symbolCount());
            assertEquals(left.get(index).apiRoot(), right.get(index).apiRoot());
            assertEquals(left.get(index).patternRole(), right.get(index).patternRole());
        }
        assertEquals(3, left.size());

        var javaBase = left.stream()
                .filter(row -> row.scopeKind().equals("MODULE")
                        && row.scopeName().equals("java.base"))
                .findFirst()
                .orElseThrow();
        var javaLogging = left.stream()
                .filter(row -> row.scopeKind().equals("MODULE")
                        && row.scopeName().equals("java.logging"))
                .findFirst()
                .orElseThrow();
        var library = left.stream()
                .filter(row -> row.scopeKind().equals("LIBRARY"))
                .findFirst()
                .orElseThrow();

        assertEquals(8, javaBase.symbolCount());
        assertEquals(2, javaLogging.symbolCount());
        assertEquals(10, library.symbolCount());
        assertEquals(64, javaBase.apiRoot().length());
        assertEquals(64, javaLogging.apiRoot().length());
        assertEquals(64, library.apiRoot().length());
        assertEquals("M3:MODULE_API_SURFACE", javaBase.patternRole());
        assertEquals("M3:LIBRARY_API_SURFACE", library.patternRole());
    }

    @Test
    void packagePrivateOwnerDoesNotLeakPublicMembersIntoLibrarySurface() {
        SourceFile hidden = parse(
                "src/java.base/share/classes/p/Hidden.java",
                """
                package p;
                class Hidden {
                    public int field;
                    protected int protectedField;
                    public void method() {}
                    protected void protectedMethod() {}

                    public static class NestedPublic {
                        public int nestedField;
                        public void nestedMethod() {}
                    }
                }
                """);

        List<M3LibraryApiSurfaceRecipe.Row> rows = run(List.of(hidden));
        var module = rows.stream()
                .filter(row -> row.scopeKind().equals("MODULE"))
                .findFirst()
                .orElseThrow();
        var library = rows.stream()
                .filter(row -> row.scopeKind().equals("LIBRARY"))
                .findFirst()
                .orElseThrow();

        assertEquals(0, module.symbolCount());
        assertEquals(0, library.symbolCount());
    }

    @Test
    void metadataDeclaresLibraryApiAdmissionInventory() {
        var recipe = new M3LibraryApiSurfaceRecipe();
        assertTrue(recipe.getDisplayName().contains("library API"));
        assertTrue(recipe.getDescription().contains("without modifying source"));
        assertTrue(recipe.getTags().contains("library-api"));
        assertTrue(recipe.getTags().contains("admission"));
        assertTrue(recipe.getTags().contains("non-mutating"));
    }

    private static List<M3LibraryApiSurfaceRecipe.Row> run(List<SourceFile> sources) {
        var execution = new M3LibraryApiSurfaceRecipe()
                .run(new InMemoryLargeSourceSet(sources), context(), 1);
        assertTrue(execution.getChangeset().getAllResults().isEmpty());
        ArrayList<M3LibraryApiSurfaceRecipe.Row> rows =
                new ArrayList<>(execution.getDataTableRows(
                        M3LibraryApiSurfaceRecipe.ApiSurfaceTable.class));
        rows.sort(Comparator.comparing(M3LibraryApiSurfaceRecipe.Row::scopeKind)
                .thenComparing(M3LibraryApiSurfaceRecipe.Row::scopeName));
        return List.copyOf(rows);
    }

    private static SourceFile parse(String path, String source) {
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), source)),
                        null,
                        context())
                .findFirst()
                .orElseThrow();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}
