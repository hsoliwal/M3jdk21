// NOTICE: declaration generated from the published META-INF/versions/9/module-info.class (class version 53); the published sources jar
// carries no module-info.java (upstream generates the descriptor in its build). No other content.
module com.fasterxml.jackson.dataformat.yaml {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.yaml.snakeyaml;
    exports com.fasterxml.jackson.dataformat.yaml;
    exports com.fasterxml.jackson.dataformat.yaml.snakeyaml.error;
    exports com.fasterxml.jackson.dataformat.yaml.util;
    provides com.fasterxml.jackson.core.JsonFactory with com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
    provides com.fasterxml.jackson.core.ObjectCodec with com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
}
