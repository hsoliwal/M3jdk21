module com.sun.jna.platform {
    requires java.logging;

    requires transitive com.sun.jna;
    requires transitive java.datatransfer;
    requires transitive java.desktop;

    exports com.sun.jna.platform;
    exports com.sun.jna.platform.bsd;
    exports com.sun.jna.platform.dnd;
    exports com.sun.jna.platform.linux;
    exports com.sun.jna.platform.mac;
    exports com.sun.jna.platform.unix;
    exports com.sun.jna.platform.unix.aix;
    exports com.sun.jna.platform.unix.solaris;
    exports com.sun.jna.platform.win32;
    exports com.sun.jna.platform.win32.COM;
    exports com.sun.jna.platform.win32.COM.tlb;
    exports com.sun.jna.platform.win32.COM.tlb.imp;
    exports com.sun.jna.platform.win32.COM.util;
    exports com.sun.jna.platform.win32.COM.util.annotation;
    exports com.sun.jna.platform.wince;

}
