module univocity.parsers {
    requires transitive java.sql;

    exports com.univocity.parsers.annotations;
    exports com.univocity.parsers.annotations.helpers;
    exports com.univocity.parsers.common;
    exports com.univocity.parsers.common.beans;
    exports com.univocity.parsers.common.fields;
    exports com.univocity.parsers.common.input;
    exports com.univocity.parsers.common.input.concurrent;
    exports com.univocity.parsers.common.iterators;
    exports com.univocity.parsers.common.processor;
    exports com.univocity.parsers.common.processor.core;
    exports com.univocity.parsers.common.record;
    exports com.univocity.parsers.common.routine;
    exports com.univocity.parsers.conversions;
    exports com.univocity.parsers.csv;
    exports com.univocity.parsers.fixed;
    exports com.univocity.parsers.tsv;

}
