module org.openrewrite.java {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.module.paramnames;
    requires io.github.classgraph;
    requires java.xml;
    requires micrometer.core;
    requires org.apache.commons.lang3;
    requires org.apache.commons.text;
    requires org.objectweb.asm;
    requires org.slf4j;
    requires snappy.java;

    requires transitive com.fasterxml.jackson.annotation;
    requires transitive java.compiler;
    requires transitive java.desktop;
    requires transitive jdk.compiler;
    requires transitive org.antlr.antlr4.runtime;
    requires transitive org.openrewrite;

    exports org.openrewrite.java;
    exports org.openrewrite.java.ai;
    exports org.openrewrite.java.cleanup;
    exports org.openrewrite.java.format;
    exports org.openrewrite.java.internal;
    exports org.openrewrite.java.internal.grammar;
    exports org.openrewrite.java.internal.template;
    exports org.openrewrite.java.isolated;
    exports org.openrewrite.java.marker;
    exports org.openrewrite.java.recipes;
    exports org.openrewrite.java.search;
    exports org.openrewrite.java.service;
    exports org.openrewrite.java.style;
    exports org.openrewrite.java.table;
    exports org.openrewrite.java.tree;

}
