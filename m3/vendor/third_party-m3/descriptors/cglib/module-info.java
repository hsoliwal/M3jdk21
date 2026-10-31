module cglib {
    requires transitive java.desktop;
    requires transitive org.objectweb.asm;

    exports net.sf.cglib.beans;
    exports net.sf.cglib.core;
    exports net.sf.cglib.core.internal;
    exports net.sf.cglib.proxy;
    exports net.sf.cglib.reflect;
    exports net.sf.cglib.transform;
    exports net.sf.cglib.transform.impl;
    exports net.sf.cglib.util;

}
