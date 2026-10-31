module rewrite.xml {
    requires org.apache.commons.text;

    requires transitive com.fasterxml.jackson.annotation;
    requires transitive org.antlr.antlr4.runtime;
    requires transitive org.openrewrite;

    exports org.openrewrite.xml;
    exports org.openrewrite.xml.format;
    exports org.openrewrite.xml.internal;
    exports org.openrewrite.xml.internal.grammar;
    exports org.openrewrite.xml.search;
    exports org.openrewrite.xml.security;
    exports org.openrewrite.xml.style;
    exports org.openrewrite.xml.tree;

}
