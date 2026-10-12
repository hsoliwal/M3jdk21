// NOTICE: declaration generated from the published META-INF/versions/9/module-info.class (class version 53); the published sources jar
// carries no module-info.java (upstream generates the descriptor in its build). No other content.
module com.fasterxml.jackson.datatype.jsr310 {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    exports com.fasterxml.jackson.datatype.jsr310;
    exports com.fasterxml.jackson.datatype.jsr310.deser;
    exports com.fasterxml.jackson.datatype.jsr310.deser.key;
    exports com.fasterxml.jackson.datatype.jsr310.ser;
    exports com.fasterxml.jackson.datatype.jsr310.ser.key;
    opens com.fasterxml.jackson.datatype.jsr310.deser;
    opens com.fasterxml.jackson.datatype.jsr310.deser.key;
    opens com.fasterxml.jackson.datatype.jsr310.ser;
    opens com.fasterxml.jackson.datatype.jsr310.ser.key;
    provides com.fasterxml.jackson.databind.Module with com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
}
