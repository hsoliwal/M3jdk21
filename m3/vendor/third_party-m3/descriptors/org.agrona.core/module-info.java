module org.agrona.core {
    requires java.management;

    requires transitive java.compiler;
    requires transitive jdk.unsupported;

    exports org.agrona;
    exports org.agrona.collections;
    exports org.agrona.concurrent;
    exports org.agrona.concurrent.broadcast;
    exports org.agrona.concurrent.errors;
    exports org.agrona.concurrent.ringbuffer;
    exports org.agrona.concurrent.status;
    exports org.agrona.console;
    exports org.agrona.generation;
    exports org.agrona.hints;
    exports org.agrona.io;
    exports org.agrona.nio;
    exports org.agrona.sbe;

}
