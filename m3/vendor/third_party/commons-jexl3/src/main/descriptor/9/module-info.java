// NOTICE: declaration generated from the published META-INF/versions/9/module-info.class (class version 53); the published sources jar
// carries no module-info.java (upstream generates the descriptor in its build). No other content.
module org.apache.commons.jexl3 {
    requires java.desktop;
    requires java.scripting;
    requires org.apache.commons.logging;
    exports org.apache.commons.jexl3;
    exports org.apache.commons.jexl3.annotations;
    exports org.apache.commons.jexl3.internal;
    exports org.apache.commons.jexl3.internal.introspection;
    exports org.apache.commons.jexl3.introspection;
    exports org.apache.commons.jexl3.parser;
    exports org.apache.commons.jexl3.scripting;
    provides javax.script.ScriptEngineFactory with org.apache.commons.jexl3.scripting.JexlScriptEngineFactory;
}
