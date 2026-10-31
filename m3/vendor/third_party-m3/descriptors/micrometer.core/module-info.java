module micrometer.core {
    requires HdrHistogram;
    requires LatencyUtils;
    requires java.logging;
    requires jdk.management;
    requires jsr305;
    requires org.slf4j;

    requires transitive com.github.benmanes.caffeine;
    requires transitive com.google.common;
    requires transitive java.management;
    requires transitive java.sql;

    exports io.micrometer.core.annotation;
    exports io.micrometer.core.aop;
    exports io.micrometer.core.instrument;
    exports io.micrometer.core.instrument.binder;
    exports io.micrometer.core.instrument.binder.cache;
    exports io.micrometer.core.instrument.binder.commonspool2;
    exports io.micrometer.core.instrument.binder.db;
    exports io.micrometer.core.instrument.binder.grpc;
    exports io.micrometer.core.instrument.binder.http;
    exports io.micrometer.core.instrument.binder.httpcomponents;
    exports io.micrometer.core.instrument.binder.hystrix;
    exports io.micrometer.core.instrument.binder.jersey.server;
    exports io.micrometer.core.instrument.binder.jetty;
    exports io.micrometer.core.instrument.binder.jpa;
    exports io.micrometer.core.instrument.binder.jvm;
    exports io.micrometer.core.instrument.binder.kafka;
    exports io.micrometer.core.instrument.binder.logging;
    exports io.micrometer.core.instrument.binder.mongodb;
    exports io.micrometer.core.instrument.binder.okhttp3;
    exports io.micrometer.core.instrument.binder.system;
    exports io.micrometer.core.instrument.binder.tomcat;
    exports io.micrometer.core.instrument.composite;
    exports io.micrometer.core.instrument.config;
    exports io.micrometer.core.instrument.config.validate;
    exports io.micrometer.core.instrument.cumulative;
    exports io.micrometer.core.instrument.distribution;
    exports io.micrometer.core.instrument.distribution.pause;
    exports io.micrometer.core.instrument.dropwizard;
    exports io.micrometer.core.instrument.internal;
    exports io.micrometer.core.instrument.logging;
    exports io.micrometer.core.instrument.noop;
    exports io.micrometer.core.instrument.push;
    exports io.micrometer.core.instrument.search;
    exports io.micrometer.core.instrument.simple;
    exports io.micrometer.core.instrument.step;
    exports io.micrometer.core.instrument.util;
    exports io.micrometer.core.ipc.http;
    exports io.micrometer.core.lang;
    exports io.micrometer.core.util.internal.logging;

}
