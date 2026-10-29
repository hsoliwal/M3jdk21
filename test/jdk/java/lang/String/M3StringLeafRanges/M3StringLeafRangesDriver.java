/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JavaLangAccess.stringM3LeafRanges reports M3 leaves as primitive lanes: native
 *          addresses decode to charAt at every index; stock-layout Strings are NOT_M3
 * @modules java.base/jdk.internal.access java.base/jdk.internal.misc
 * @build java.base/java.lang.M3StringLeafRangesOracle
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringLeafRangesDriver m3
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringLeafRangesDriver m3
 * @run main/othervm M3StringLeafRangesDriver stock
 */
public class M3StringLeafRangesDriver {
    public static void main(String[] args) {
        java.lang.M3StringLeafRangesOracle.main(args);
    }
}
