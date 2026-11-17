/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary jdk.m3.search logs through java.util.logging by default
 * @modules java.logging jdk.m3.search/jdk.internal.search.slf4j
 * @run main/othervm --add-modules jdk.m3.search SearchJulDefaultTest
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import jdk.internal.search.slf4j.Logger;
import jdk.internal.search.slf4j.LoggerFactory;

/**
 * The relocated SLF4J of jdk.m3.search finds its relocated JUL provider: a logger is the JUL
 * adapter, a call reaches java.util.logging with the formatted message, the level and the caller,
 * and SLF4J prints no "no providers" warning.
 */
public class SearchJulDefaultTest {
    public static void main(String[] args) {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream original = System.err;
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        List<LogRecord> records = new ArrayList<>();
        java.util.logging.Logger jul = java.util.logging.Logger.getLogger("m3.search.test");
        jul.setUseParentHandlers(false);
        jul.setLevel(Level.ALL);
        jul.addHandler(new Handler() {
            @Override public void publish(LogRecord record) { records.add(record); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        Logger logger = LoggerFactory.getLogger("m3.search.test");
        logger.info("hello {}", "jul");
        logger.debug("detail {} {}", 1, 2);
        System.setErr(original);
        expect("jdk.internal.search.slf4j.jul.JDK14LoggerAdapter", logger.getClass().getName());
        expect(2, records.size());
        expect("hello jul", records.get(0).getMessage());
        expect(Level.INFO, records.get(0).getLevel());
        expect(SearchJulDefaultTest.class.getName(), records.get(0).getSourceClassName());
        expect(Level.FINE, records.get(1).getLevel());
        expect("detail 1 2", records.get(1).getMessage());
        expect("", err.toString(StandardCharsets.UTF_8).strip());
    }

    private static void expect(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }
}
