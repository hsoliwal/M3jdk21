module com.google.common {
    requires j2objc.annotations;
    requires java.logging;
    requires jdk.unsupported;

    requires transitive com.google.common.util.concurrent.internal;
    requires transitive com.google.errorprone.annotations;
    requires transitive jsr305;

    exports com.google.common.annotations;
    exports com.google.common.base;
    exports com.google.common.base.internal;
    exports com.google.common.cache;
    exports com.google.common.collect;
    exports com.google.common.escape;
    exports com.google.common.eventbus;
    exports com.google.common.graph;
    exports com.google.common.hash;
    exports com.google.common.html;
    exports com.google.common.io;
    exports com.google.common.math;
    exports com.google.common.net;
    exports com.google.common.primitives;
    exports com.google.common.reflect;
    exports com.google.common.util.concurrent;
    exports com.google.common.xml;
    exports com.google.thirdparty.publicsuffix;

}
