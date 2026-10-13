module snappy.java {
    requires java.logging;

    requires transitive org.osgi.core;

    exports org.xerial.snappy;
    exports org.xerial.snappy.buffer;
    exports org.xerial.snappy.pool;

}
