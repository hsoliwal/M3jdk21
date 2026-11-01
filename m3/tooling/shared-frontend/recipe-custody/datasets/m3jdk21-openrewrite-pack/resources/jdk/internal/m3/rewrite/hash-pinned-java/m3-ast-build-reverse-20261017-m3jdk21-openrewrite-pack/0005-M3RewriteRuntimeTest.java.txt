/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package org.m3jdk.tooling.rewrite;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.hcl.HclParser;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.json.JsonParser;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.properties.ChangePropertyValue;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.protobuf.ProtoParser;
import org.openrewrite.tree.ParseError;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.yaml.YamlParser;
import static org.junit.jupiter.api.Assertions.*;

/** Full framework API qualification, independent from the Java shared-compiler CLI input route. */
class M3RewriteRuntimeTest {
    static Stream<Arguments> languages() {
        return Stream.of(
                Arguments.of("json", JsonParser.builder().build(), "{\"name\": \"α\", \"items\": [1, 2]}\n"),
                Arguments.of("yaml", YamlParser.builder().build(), "# keep\nname: α\nitems:\n  - one\n"),
                Arguments.of("xml", XmlParser.builder().build(), "<?xml version=\"1.0\"?>\n<root><!-- keep --><item>α</item></root>\n"),
                Arguments.of("properties", PropertiesParser.builder().build(), "# keep\nsample.key=old\n"),
                Arguments.of("hcl", HclParser.builder().build(), "resource \"example\" \"value\" {\n  name = \"item\"\n}\n"),
                Arguments.of("protobuf", ProtoParser.builder().build(), "syntax = \"proto2\";\nmessage Greeting { optional string name = 1; }\n"),
                Arguments.of("groovy", GroovyParser.builder().build(), "class Greeting { String hello(String name) { return name.trim() } }\n"),
                Arguments.of("gradle", GradleParser.builder().build(), "plugins { id 'java' }\nrepositories { mavenCentral() }\n"),
                Arguments.of("maven", MavenParser.builder().build(), "<project>\n  <modelVersion>4.0.0</modelVersion>\n  <groupId>example</groupId>\n  <artifactId>sample</artifactId>\n  <version>1</version>\n</project>\n"));
    }

    @ParameterizedTest(name = "{0} retains its actual upstream parser and lossless source")
    @MethodSource("languages")
    void publishedLanguageParsersRoundTripActualInputs(String language, Parser parser, String input) {
        List<Throwable> errors = new ArrayList<>();
        var context = new InMemoryExecutionContext(errors::add);
        List<SourceFile> files = parser.parse(context, input).toList();
        assertTrue(errors.isEmpty(), () -> language + ": " + errors);
        assertEquals(1, files.size(), language);
        assertFalse(files.getFirst() instanceof ParseError, () -> files.getFirst().printAll());
        assertEquals(input, files.getFirst().printAll(), language);
    }

    @Test void nonJavaFrameworkRecipeExecutesOnItsActualLanguageTree() {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        SourceFile source = PropertiesParser.builder().build().parse(context, "# keep\nsample.key=old\n")
                .findFirst().orElseThrow().withSourcePath(Path.of("application.properties"));
        var recipe = new ChangePropertyValue("sample.key", "new", "old", false, false);
        var results = recipe.run(new InMemoryLargeSourceSet(List.of(source)), context)
                .getChangeset().getAllResults();
        assertEquals(1, results.size());
        assertNotNull(results.getFirst().getAfter());
        assertEquals("# keep\nsample.key=new\n", results.getFirst().getAfter().printAll());
        assertEquals("# keep\nsample.key=old\n", source.printAll());
    }

    @Test void realFrameworkDiscoveryFindsRecipesAcrossBuildAndDataLanguages() {
        var environment = Environment.builder().scanRuntimeClasspath("org.openrewrite").build();
        Set<String> names = environment.listRecipeDescriptors().stream()
                .map(descriptor -> descriptor.getName()).collect(Collectors.toSet());
        Set<String> expected = Set.of("org.openrewrite.java.ChangeMethodName",
                "org.openrewrite.gradle.ChangeDependency", "org.openrewrite.maven.ChangePropertyValue",
                "org.openrewrite.properties.ChangePropertyValue", "org.openrewrite.json.ChangeValue",
                "org.openrewrite.xml.ChangeTagValue", "org.openrewrite.yaml.ChangePropertyValue");
        assertTrue(names.containsAll(expected), () -> "Missing framework recipes: "
                + expected.stream().filter(name -> !names.contains(name)).toList());
    }
}
