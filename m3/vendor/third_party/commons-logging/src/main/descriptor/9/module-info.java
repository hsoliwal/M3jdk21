// NOTICE: declaration generated from the published META-INF/versions/9/module-info.class (class version 53); the published sources jar
// carries no module-info.java (upstream generates the descriptor in its build). No other content.
module org.apache.commons.logging {
    requires static org.apache.logging.log4j;
    requires static org.slf4j;
    requires static avalon.framework;
    requires static java.logging;
    requires static javax.servlet.api;
    requires static logkit;
    requires static org.apache.log4j;
    exports org.apache.commons.logging;
    exports org.apache.commons.logging.impl;
    uses org.apache.commons.logging.LogFactory;
}
