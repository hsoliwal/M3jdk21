package jdk.internal.search.slf4j.jul;

import jdk.internal.search.slf4j.ILoggerFactory;
import jdk.internal.search.slf4j.IMarkerFactory;
import jdk.internal.search.slf4j.helpers.BasicMDCAdapter;
import jdk.internal.search.slf4j.helpers.BasicMarkerFactory;
import jdk.internal.search.slf4j.spi.MDCAdapter;
import jdk.internal.search.slf4j.spi.SLF4JServiceProvider;

public class JULServiceProvider implements SLF4JServiceProvider {

    /**
     * Declare the version of the SLF4J API this implementation is compiled
     * against. The value of this field is modified with each major release.
     */
    // to avoid constant folding by the compiler, this field must *not* be final
    public static String REQUESTED_API_VERSION = "2.0.99"; // !final

    private ILoggerFactory loggerFactory;
    private IMarkerFactory markerFactory;
    private MDCAdapter mdcAdapter;

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    @Override
    public String getRequestedApiVersion() {
        return REQUESTED_API_VERSION;
    }

    @Override
    public void initialize() {
        loggerFactory = new JDK14LoggerFactory();
        markerFactory = new BasicMarkerFactory();
        mdcAdapter = new BasicMDCAdapter();
    }
}
