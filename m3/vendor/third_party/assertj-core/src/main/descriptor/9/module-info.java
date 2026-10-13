// NOTICE: declaration generated from the published META-INF/versions/9/module-info.class (class version 53); the published sources jar
// carries no module-info.java (upstream generates the descriptor in its build). No other content.
module org.assertj.core {
    requires static java.logging;
    requires static java.management;
    requires static java.xml;
    requires static junit;
    requires static net.bytebuddy;
    requires static org.hamcrest;
    requires static org.junit.jupiter.api;
    requires static org.opentest4j;
    exports org.assertj.core.annotations;
    exports org.assertj.core.api;
    exports org.assertj.core.api.exception;
    exports org.assertj.core.api.filter;
    exports org.assertj.core.api.iterable;
    exports org.assertj.core.api.junit.jupiter;
    exports org.assertj.core.api.recursive.comparison;
    exports org.assertj.core.condition;
    exports org.assertj.core.configuration;
    exports org.assertj.core.data;
    exports org.assertj.core.description;
    exports org.assertj.core.error;
    exports org.assertj.core.error.future;
    exports org.assertj.core.error.uri;
    exports org.assertj.core.extractor;
    exports org.assertj.core.groups;
    exports org.assertj.core.matcher;
    exports org.assertj.core.presentation;
    exports org.assertj.core.util;
    exports org.assertj.core.util.diff;
    exports org.assertj.core.util.diff.myers;
    exports org.assertj.core.util.introspection;
    exports org.assertj.core.util.xml;
    uses org.assertj.core.configuration.Configuration;
    uses org.assertj.core.presentation.Representation;
}
