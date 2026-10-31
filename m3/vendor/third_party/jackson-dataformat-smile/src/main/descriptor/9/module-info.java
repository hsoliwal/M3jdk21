// NOTICE: declaration generated from the published META-INF/versions/9/module-info.class (class version 53); the published sources jar
// carries no module-info.java (upstream generates the descriptor in its build). No other content.
module com.fasterxml.jackson.dataformat.smile {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    exports com.fasterxml.jackson.dataformat.smile;
    exports com.fasterxml.jackson.dataformat.smile.async;
    exports com.fasterxml.jackson.dataformat.smile.databind;
    provides com.fasterxml.jackson.core.JsonFactory with com.fasterxml.jackson.dataformat.smile.SmileFactory;
}
