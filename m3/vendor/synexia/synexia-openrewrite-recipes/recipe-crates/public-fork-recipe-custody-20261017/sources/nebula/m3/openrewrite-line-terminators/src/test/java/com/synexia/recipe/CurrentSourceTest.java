// SPDX-License-Identifier: Apache-2.0
package com.synexia.recipe;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.java.Java21Parser;
import org.openrewrite.java.tree.J;

/** The original pinned fixtures remain unchanged; also qualify the delivered sources. */
final class CurrentSourceTest {
    @Test void currentSnippetAndRoundScaleRemainLossless() throws Exception {
        var root=Path.of(System.getProperty("m3.nebula.currentRoot"));
        for(String name:List.of(
                "examples/org.eclipse.nebula.snippets/src/org/eclipse/nebula/snippets/gallery/SnippetTooltip.java",
                "widgets/visualization/org.eclipse.nebula.visualization.widgets/src/org/eclipse/nebula/visualization/widgets/figureparts/RoundScale.java")) {
            byte[] before=Files.readAllBytes(root.resolve(name));
            String text=new String(before,java.nio.charset.StandardCharsets.UTF_8);
            var errors=new ArrayList<Throwable>();
            var files=Java21Parser.builder().build().parseInputs(
                    List.of(Parser.Input.fromString(Path.of(name),text)),null,new InMemoryExecutionContext(errors::add)).toList();
            assertTrue(errors.isEmpty(),errors.toString()); assertEquals(1,files.size());
            assertInstanceOf(J.CompilationUnit.class,files.getFirst()); assertEquals(text,files.getFirst().printAll());
            assertArrayEquals(before,Files.readAllBytes(root.resolve(name)));
        }
    }
}
