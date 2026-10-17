// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Real SDK qualification; optional Results export happens only after ALL checks in this test. */
final class M3DeepFilterRecipeTest {
    private static final String JAVA_CRATE = "m3-deep-filter-20261017";
    private static final String TEXT_CRATE = "m3-deep-filter-native-20261017";
    private static final String NAME = "com.synexia.m3jdk.DeepFilterComposition20261017";
    private static final String JROOT = "/com/synexia/rewrite/hash-pinned-java/"+JAVA_CRATE+"/";
    private static final String TROOT = "/com/synexia/rewrite/hash-pinned-text/"+TEXT_CRATE+"/";

    private static String resource(String path) {
        try (InputStream in = M3DeepFilterRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(in,path);
            return new String(in.readAllBytes(),StandardCharsets.UTF_8);
        } catch (IOException failure) { throw new IllegalStateException(failure); }
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static List<String[]> rows(String root) {
        return resource(root+"manifest.tsv").lines().filter(s -> !s.isBlank())
                .map(s -> s.split("\t",-1)).toList();
    }
    private static List<String[]> allRows() {
        List<String[]> out = new ArrayList<>(rows(JROOT)); out.addAll(rows(TROOT));
        return out;
    }
    private static Map<String,String> expected() {
        Map<String,String> out = new TreeMap<>();
        for (String root : List.of(JROOT,TROOT)) {
            for (String[] row : rows(root)) assertNull(out.put(row[0],resource(root+row[3])));
        }
        return out;
    }
    private static Map<String,String> original() {
        Map<String,String> out = new TreeMap<>();
        for (String[] row : rows(JROOT)) if (!row[1].equals("ABSENT")) {
            out.put(row[0],resource(JROOT+"pre/"+row[3]));
        }
        return out;
    }
    private static List<SourceFile> parse(Map<String,String> input) {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        List<Parser.Input> java = new ArrayList<>();
        List<SourceFile> out = new ArrayList<>();
        new TreeMap<>(input).forEach((path,text) -> {
            if (path.endsWith(".java")) java.add(Parser.Input.fromString(Path.of(path),text));
            else out.add(PlainText.builder().sourcePath(Path.of(path)).text(text).build());
        });
        out.addAll(JavaParser.fromJavaVersion().build().parseInputs(java,null,context).toList());
        return out;
    }
    private static Recipe named() {
        return Environment.builder().scanRuntimeClasspath().build().activateRecipes(NAME);
    }
    private static RecipeRun apply(Recipe recipe, List<SourceFile> inputs) {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        return recipe.run(new InMemoryLargeSourceSet(inputs),context,1);
    }
    private static Map<String,String> changes(RecipeRun result) {
        Map<String,String> out = new TreeMap<>();
        result.getChangeset().getAllResults().forEach(change -> {
            assertNotNull(change.getAfter(),"no deletion");
            assertNull(out.put(change.getAfter().getSourcePath().toString().replace('\\','/'),
                    change.getAfter().printAll()),"duplicate output");
        });
        return out;
    }

    @Test
    void qualifyNamedRecipeAndOptionallyExportActualResults() throws Exception {
        Map<String,String> before = original(), after = expected();
        assertEquals(2,before.size()); assertEquals(6,after.size());
        assertEquals(5,rows(JROOT).size()); assertEquals(1,rows(TROOT).size());
        assertTrue(new M3HashPinnedJavaSnapshotRecipe(JAVA_CRATE).isLst());
        for (String root : List.of(JROOT,TROOT)) for (String[] row : rows(root)) {
            assertEquals(4,row.length);
            assertEquals(row[2],sha(resource(root+row[3])));
            if (!row[1].equals("ABSENT")) assertEquals(row[1],sha(before.get(row[0])));
        }
        Recipe recipe = named();
        Map<String,String> emitted = changes(apply(recipe,parse(before)));
        assertEquals(after,emitted,"actual Java/Text SDK Results differ from sealed templates");
        assertTrue(changes(apply(recipe,parse(emitted))).isEmpty(),"not a fixed point");
        // Every single-target partial delivery must preserve that installed target.
        for (String delivered : after.keySet()) {
            Map<String,String> partial = new LinkedHashMap<>(before);
            partial.put(delivered,after.get(delivered));
            partial.put("src/main/java/demo/Unrelated.java","package demo; class Unrelated {}\n");
            Map<String,String> delta = changes(apply(recipe,parse(partial)));
            Map<String,String> wanted = new TreeMap<>(after); wanted.remove(delivered);
            assertEquals(wanted,delta,"partial delivery/unrelated source corruption");
        }
        assertThrows(RuntimeException.class, () -> apply(named(),parse(Map.of())));
        for (String path : before.keySet()) {
            Map<String,String> drift = new LinkedHashMap<>(before);
            drift.put(path,drift.get(path)+"\n// foreign drift\n");
            assertThrows(RuntimeException.class, () -> apply(named(),parse(drift)));
        }
        for (String path : after.keySet()) if (!before.containsKey(path)) {
            Map<String,String> occupied = new LinkedHashMap<>(before);
            occupied.put(path,path.endsWith(".java") ? "class Occupied {}\n" : "occupied\n");
            assertThrows(RuntimeException.class, () -> apply(named(),parse(occupied)));
        }
        String javaPath = before.keySet().iterator().next();
        var wrongKind = PlainText.builder().sourcePath(Path.of(javaPath)).text(before.get(javaPath)).build();
        assertThrows(RuntimeException.class, () -> apply(new M3HashPinnedJavaSnapshotRecipe(JAVA_CRATE),List.of(wrongKind)));
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var javaRecipe = new M3HashPinnedJavaSnapshotRecipe(JAVA_CRATE);
        var inventory = javaRecipe.getInitialValue(context);
        var scanner = javaRecipe.getScanner(inventory);
        for (SourceFile source : parse(before)) { scanner.visit(source,context); scanner.visit(source,context); }
        assertThrows(IllegalStateException.class, () -> javaRecipe.generate(inventory,context));
        // No export can happen before the preceding assertions return successfully.
        String output = System.getProperty("m3.deep.sdk.output");
        if (output != null && !output.isBlank()) export(emitted,Path.of(output));
    }

    private static void export(Map<String,String> actualResults, Path requested) throws Exception {
        Path root = requested.toAbsolutePath().normalize();
        for (Path p=root;p!=null;p=p.getParent()) assertFalse(Files.isSymbolicLink(p),"symlinked output");
        assertFalse(Files.exists(root,LinkOption.NOFOLLOW_LINKS),"SDK output must be absent");
        Files.createDirectories(root);
        StringBuilder receipt = new StringBuilder("# actual OpenRewrite Results; qualified before export\n");
        for (Map.Entry<String,String> entry : new TreeMap<>(actualResults).entrySet()) {
            Path file = root.resolve(entry.getKey()).normalize();
            assertTrue(file.startsWith(root) && !file.equals(root),"path escape");
            Files.createDirectories(file.getParent());
            Files.writeString(file,entry.getValue(),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);
            receipt.append(entry.getKey()).append('\t').append(sha(entry.getValue())).append('\n');
        }
        Files.writeString(root.resolve("SDK_RECEIPT.tsv"),receipt.toString(),StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
    }
}
