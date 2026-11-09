module rewrite.yaml {
    requires org.yaml.snakeyaml;

    requires transitive org.antlr.antlr4.runtime;
    requires transitive org.openrewrite;

    exports org.openrewrite.yaml;
    exports org.openrewrite.yaml.cleanup;
    exports org.openrewrite.yaml.format;
    exports org.openrewrite.yaml.internal;
    exports org.openrewrite.yaml.internal.grammar;
    exports org.openrewrite.yaml.search;
    exports org.openrewrite.yaml.style;
    exports org.openrewrite.yaml.tree;

}
