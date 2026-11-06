// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Javadoc;

/** Lossless Java LST witnesses for the existing JDK21 Runtime documentation. */
public final class M3RuntimeJavadocParserTest {
    private static J.CompilationUnit parse(String source) {
        List<Throwable> errors = new ArrayList<>();
        List<SourceFile> files = JavaParser.fromJavaVersion().build().parseInputs(
            List.of(Parser.Input.fromString(Path.of("Probe.java"), source)), null,
            new InMemoryExecutionContext(errors::add)).toList();
        assertTrue(errors.isEmpty(), () -> "parser callbacks: " + errors);
        assertEquals(1, files.size());
        J.CompilationUnit result = assertInstanceOf(J.CompilationUnit.class, files.get(0));
        assertEquals(source, result.printAll(), "all original text must round trip");
        return result;
    }

    private static String source(String documentation) {
        return documentation + "\npublic class Probe { public int field; public void work() {} }\n";
    }

    @Test void specTagsRetainUrlsWhitespaceAndInlineTitleMarkup() {
        for (String doc : List.of(
                "/** Documentation.\n * @spec jni/index.html Java Native Interface Specification\n */",
                "/** Documentation.\n * @spec https://example.org/spec?q=1#chapter A {@code title} &amp; text\n */",
                "/** Documentation.\n * @spec jni/index.html\n *     Multiline {@link String} title\n * @since 21\n */",
                "/** Documentation.\n * @spec first.html First\n * @spec second.html Second\n */")) {
            parse(source(doc));
            parse(source(doc).replace("\n", "\r\n"));
        }
    }

    @Test void localFragmentsRetainTextAndDoNotInventJavaMembers() {
        for (String doc : List.of(
                "/** {@linkplain ##shutdown Shutdown} reference. */",
                "/** {@link ##termination} reference. */",
                "/** {@linkplain ##shutdown Multi\n * line {@code label}}. */",
                "/** {@link ##alpha Alpha} and {@linkplain ##beta Beta}. */")) {
            J.CompilationUnit unit = parse(source(doc));
            AtomicInteger fragments = new AtomicInteger();
            AtomicInteger members = new AtomicInteger();
            new JavaVisitor<Void>() {
                @Override protected JavadocVisitor<Void> getJavadocVisitor() {
                    return new JavadocVisitor<Void>(this) {
                        @Override public Javadoc visitUnknownInline(Javadoc.UnknownInline tag, Void p) {
                            if (tag.getName().equals("link") || tag.getName().equals("linkplain")) fragments.incrementAndGet();
                            return super.visitUnknownInline(tag, p);
                        }
                        @Override public Javadoc visitReference(Javadoc.Reference reference, Void p) {
                            members.incrementAndGet();
                            return super.visitReference(reference, p);
                        }
                    };
                }
            }.visit(unit, null);
            assertTrue(fragments.get() > 0);
            assertEquals(0, members.get(), "URI fragments must not become Java member references");
            parse(source(doc).replace("\n", "\r\n"));
        }
    }

    @Test void existingTypedLinksAndUnknownTagsKeepTheirRepresentation() {
        J.CompilationUnit unit = parse(source("/** {@link String} and {@link #work()} and {@link #field}.\n * @apiNote Existing note.\n */"));
        AtomicInteger references = new AtomicInteger();
        AtomicInteger fragments = new AtomicInteger();
        new JavaVisitor<Void>() {
            @Override protected JavadocVisitor<Void> getJavadocVisitor() {
                return new JavadocVisitor<Void>(this) {
                    @Override public Javadoc visitReference(Javadoc.Reference reference, Void p) {
                        references.incrementAndGet();
                        return super.visitReference(reference, p);
                    }
                    @Override public Javadoc visitUnknownInline(Javadoc.UnknownInline inline, Void p) {
                        fragments.incrementAndGet();
                        return super.visitUnknownInline(inline, p);
                    }
                };
            }
        }.visit(unit, null);
        assertEquals(3, references.get());
        assertEquals(0, fragments.get());
    }

    @Test void exactRuntimePreimageAndPostimageRoundTrip() throws IOException {
        for (String name : List.of("Runtime-before.java.txt", "Runtime-after.java.txt")) {
            try (var stream = getClass().getResourceAsStream("/runtime/" + name)) {
                assertNotNull(stream, name);
                String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                parse(source);
                parse(parse(source).printAll());
            }
        }
    }
}
