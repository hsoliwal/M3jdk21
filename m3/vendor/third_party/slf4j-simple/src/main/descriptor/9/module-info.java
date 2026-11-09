// NOTICE: declaration generated from the published META-INF/versions/9/module-info.class (class version 53); the published sources jar
// carries no module-info.java (upstream generates the descriptor in its build). No other content.
module org.slf4j.simple {
    requires org.slf4j;
    exports org.slf4j.simple;
    opens org.slf4j.simple to org.slf4j;
    provides org.slf4j.spi.SLF4JServiceProvider with org.slf4j.simple.SimpleServiceProvider;
}
