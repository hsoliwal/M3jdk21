// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NameNode;
import jdk.m3.jackson.M3AST;
import jdk.m3.jackson.M3ASTAtom;

/** Execute against actual JDK XML parser plus synthetic JDT/Jackson API signatures. */
public final class M3CrossFrontendContract {
    private static final AtomicInteger CHECKS = new AtomicInteger();
    private M3CrossFrontendContract() { }

    private static void ok(boolean condition, String message) {
        CHECKS.incrementAndGet();
        if (!condition) throw new AssertionError(message);
    }
    private static void eq(Object expected, Object actual, String reason) {
        CHECKS.incrementAndGet();
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(reason + " expected=" + expected + " got=" + actual);
        }
    }
    private static M3File source(String name, String body) {
        return M3File.of(URI.create("memory:/" + name), body);
    }
    private static M3ASTIngress.Limits room() {
        return new M3ASTIngress.Limits(200_000, 100_000, 100);
    }
    private static void rejects(Class<? extends Throwable> type, Checked op, String reason) {
        CHECKS.incrementAndGet();
        try {
            op.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError(reason + " wrong failure " + error, error);
        }
        throw new AssertionError(reason + " unexpectedly succeeded");
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }

    private static void primitiveIndex() {
        M3File input = source("source.java", "class A {}");
        M3ASTIndex.Builder b = M3ASTIndex.builder(input, M3ASTIndex.Domain.ECLIPSE_JDT, 4);
        int root = b.add(-1, "COMPILATION_UNIT", 0, input.length(), M3ASTIndex.EXACT_RANGE);
        int a = b.add(root, "CLASS", -1, -1, M3ASTIndex.SYNTAX_ONLY);
        int n = b.add(a, "NAME", 6, 7, M3ASTIndex.EXACT_RANGE);
        int right = b.add(root, "BLOCK", 8, 10, M3ASTIndex.EXACT_RANGE);
        b.completeRange(a, 0, 8, true);
        M3ASTIndex index = b.seal();
        eq(4, index.nodeCount(), "node count");
        eq("A", index.sourceText(n), "exact UTF-16 source slice");
        eq(2, index.childCount(root), "root child count");
        eq(a, index.firstChild(root), "first child coordinate");
        eq(right, index.nextSibling(a), "sibling chain");
        eq(-1, index.nextSibling(right), "last sibling sentinel");
        eq(a, index.parent(n), "direct child belongs to class");
    }

    private static void mavenPom() throws IOException {
        String pom = "<?xml version=\"1.0\"?><project xmlns=\"http://maven.apache.org/POM/4.0.0\">"
                + "<!-- preserve -->"
                + "<modelVersion>4.0.0</modelVersion><groupId>g</groupId>"
                + "<dependencies><dependency><groupId>x</groupId><artifactId>y</artifactId>"
                + "</dependency><dependency><groupId>a</groupId></dependency></dependencies>"
                + "<properties><hello><![CDATA[<literal>]]></hello></properties></project>";
        M3ASTIndex index = M3ASTIngress.mavenPom(source("pom.xml", pom), room());
        eq(M3ASTIndex.Domain.MAVEN_POM, index.domain(), "POM domain");
        ok(index.nodeCount() > 12, "POM XML elements, text, attrs, comments and CDATA");
        eq("XML_DOCUMENT", index.kind(0), "original XML document owner");
        ok(index.hasExactRange(0), "entire source envelope exact");
        eq(pom, index.sourceText(0), "preserve complete original source spelling");
        boolean comment = false, cdata = false, xmlns = false;
        int groupIdCount = 0;
        for (int i = 0; i < index.nodeCount(); i++) {
            if (index.kind(i).equals("XML_COMMENT")) comment = true;
            if (index.kind(i).equals("XML_CDATA")) cdata = true;
            if (index.kind(i).equals("XML_ELEMENT:groupId")) groupIdCount++;
            if (index.kind(i).equals("XML_ATTRIBUTE:xmlns")) xmlns = true;
            if (i != 0) ok(!index.hasExactRange(i), "DOM element source spans are unknown");
        }
        ok(comment && cdata && xmlns && groupIdCount == 3, "full XML occurrence preservation");
        rejects(IOException.class, () -> M3ASTIngress.mavenPom(
                source("pom.xml", "<!DOCTYPE x [<!ENTITY external SYSTEM 'file:///etc/passwd'>]><x>&external;</x>"),
                room()), "XXE and DTD must fail closed");
        rejects(IOException.class, () -> M3ASTIngress.mavenPom(
                source("pom.xml", "<project><a></project>"), room()), "invalid XML must fail");
        rejects(IOException.class, () -> M3ASTIngress.mavenPom(
                source("pom.xml", "<notProject/>"), room()), "foreign XML root is not a POM");
        rejects(IOException.class, () -> M3ASTIngress.mavenPom(
                source("pom.xml", "<project xmlns=\"urn:foreign\"/>"), room()), "foreign POM namespace");
        rejects(IllegalArgumentException.class, () -> M3ASTIngress.mavenPom(
                source("pom.xml", pom), new M3ASTIngress.Limits(8, 100, 5)), "source budget");
    }

    private static void eclipseJdt() {
        String code = "class A { int n; }";
        M3File file = source("A.java", code);
        CompilationUnit root = new CompilationUnit(0, code.length());
        NameNode classNode = new NameNode(6, 1);
        NameNode recovered = new NameNode(14, 1);
        recovered.setFlags(ASTNode.RECOVERED);
        root.add("type", classNode);
        root.add("field", recovered);
        M3ASTIndex index = M3ASTIngress.eclipseJdt(file, root, room());
        eq(M3ASTIndex.Domain.ECLIPSE_JDT, index.domain(), "JDT domain");
        eq(3, index.nodeCount(), "JDT nodes and parent indexes");
        eq("JDT_CompilationUnit", index.kind(0), "JDT root kind");
        eq("A", index.sourceText(1), "JDT exact source offsets");
        eq("n", index.sourceText(2), "JDT second source offsets");
        ok((index.flags(2) & M3ASTIndex.RECOVERED) != 0, "recovery provenance retained");
        eq(2, index.childCount(0), "JDT structural descriptors preserved");
        rejects(IllegalArgumentException.class, () -> M3ASTIngress.eclipseJdt(
                file, new NameNode(1234, 5), room()), "JDT outside source fail closed");
    }

    private static void gradle() {
        String groovy = "plugins { id 'java' }\n"
                + "dependencies { implementation('g:a:1'); // implementation('evil')\n"
                + "  ext.msg = \"literal {not-a-block}\"; /* } */ }\n";
        M3ASTIndex index = M3ASTIngress.gradleScript(source("build.gradle", groovy), false, room());
        eq(M3ASTIndex.Domain.GRADLE_GROOVY, index.domain(), "Groovy indexing only");
        eq(groovy, index.sourceText(0), "full source preserved");
        int blocks = 0, comments = 0, strings = 0;
        for (int i = 0; i < index.nodeCount(); i++) {
            if (index.kind(i).equals("BRACE_BLOCK")) {
                blocks++;
                ok(index.hasExactRange(i), "balanced block has exact range");
                ok(index.sourceText(i).startsWith("{"), "block starts at opener");
            }
            if (index.kind(i).endsWith("COMMENT")) comments++;
            if (index.kind(i).equals("STRING")) strings++;
        }
        eq(2, blocks, "comments/literals do not create fake blocks");
        eq(2, comments, "comment trivia indexed without code matches");
        eq(3, strings, "string literals indexed");
        String kotlin = "plugins { kotlin(\"jvm\") version \"2.2.0\" }\n/* one /* two */ one */\n"
                + "val json = \"\"\"{\"build\":true}\"\"\"\n";
        M3ASTIndex kts = M3ASTIngress.gradleScript(source("build.gradle.kts", kotlin), true, room());
        eq(M3ASTIndex.Domain.GRADLE_KOTLIN, kts.domain(), "Kotlin syntax domain");
        ok(kts.nodeCount() > 5, "Kotlin nested comments and raw strings");
        rejects(IllegalArgumentException.class, () -> M3ASTIngress.gradleScript(
                source("build.gradle", "plugins { id 'x'"), false, room()), "unclosed block");
        rejects(IllegalArgumentException.class, () -> M3ASTIngress.gradleScript(
                source("build.gradle.kts", "plugins { foo() ]"), true, room()), "unmatched group");
        rejects(IllegalArgumentException.class, () -> M3ASTIngress.gradleScript(
                source("build.gradle", "'incomplete"), false, room()), "unclosed string");
    }

    private static void jacksonJson() {
        M3ASTAtom value = new M3ASTAtom("NUMBER_INT");
        M3ASTAtom first = new M3ASTAtom("FIELD", value);
        M3ASTAtom array = new M3ASTAtom("ARRAY", first, first);
        M3AST original = new M3AST(array);
        M3ASTIndex index = M3ASTIngress.jacksonJson(
                source("doc.json", "[1,1]"), original, room());
        eq(M3ASTIndex.Domain.JACKSON_JSON, index.domain(), "JSON original owner");
        eq(5, index.nodeCount(), "distinct DAG occurrences, not unique pool atoms");
        eq("JSON_ARRAY", index.kind(0), "JSON root");
        eq("JSON_FIELD", index.kind(1), "JSON first field");
        eq("JSON_FIELD", index.kind(3), "JSON repeated occurrence");
        ok(!index.hasExactRange(2), "do not forge JSON source coordinates");
        ok((index.flags(0) & M3ASTIndex.SOURCE_UNVERIFIED) != 0,
                "external Jackson AST lacks a provenance tie to supplied M3File");
        rejects(IllegalStateException.class, () -> index.sourceText(1),
                "JSON unknown source range must refuse");
        rejects(IllegalArgumentException.class, () -> M3ASTIngress.jacksonJson(
                source("doc.json", "[1,1]"), new Object(), room()), "wrong Jackson source owner");
    }

    private static void concurrency() throws InterruptedException {
        M3ASTIndex indexed = M3ASTIngress.gradleScript(
                source("build.gradle", "plugins { id 'java' }"), false, room());
        AtomicReference<Throwable> failure = new AtomicReference<>();
        ArrayList<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 3; t++) {
            Thread worker = new Thread(() -> {
                try {
                    for (int i = 0; i < 2000; i++) {
                        eq("GRADLE_SOURCE", indexed.kind(0), "immutable shared read");
                        ok(indexed.primitiveBytes() > 0, "nonzero indexed geometry");
                    }
                } catch (Throwable e) {
                    failure.compareAndSet(null, e);
                }
            });
            threads.add(worker);
            worker.start();
        }
        for (Thread thread : threads) thread.join();
        if (failure.get() != null) throw new AssertionError("shared immutable view", failure.get());
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        primitiveIndex();
        mavenPom();
        eclipseJdt();
        gradle();
        jacksonJson();
        concurrency();
        System.out.println("M3_AST_MULTIFRONTEND_PASS checks=" + CHECKS.get());
    }
}
