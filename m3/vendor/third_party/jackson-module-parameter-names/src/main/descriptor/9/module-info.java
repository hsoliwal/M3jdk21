// NOTICE: declaration generated from the published META-INF/versions/9/module-info.class (class version 53); the published sources jar
// carries no module-info.java (upstream generates the descriptor in its build). No other content.
module com.fasterxml.jackson.module.paramnames {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    exports com.fasterxml.jackson.module.paramnames;
    provides com.fasterxml.jackson.databind.Module with com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
}
