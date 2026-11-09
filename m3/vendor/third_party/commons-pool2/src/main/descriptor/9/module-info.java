// NOTICE: declaration generated from the published META-INF/versions/9/module-info.class (class version 53); the published sources jar
// carries no module-info.java (upstream generates the descriptor in its build). No other content.
module org.apache.commons.pool2 {
    requires static cglib;
    requires java.management;
    exports org.apache.commons.pool2;
    exports org.apache.commons.pool2.impl;
    exports org.apache.commons.pool2.proxy;
}
