// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.util.Base64;

/**
 * The pinned RFC 8259 grammar image {@code rfc8259-2017.m3grammar} as a constant.
 *
 * <p>java.base copies only the resource extensions listed in its build into the module, so
 * the image that {@link Rfc8259JsonGrammar#compileReference()} writes is carried here as
 * Base64 text instead of a resource file: 1295 bytes, SHA-256
 * {@code 63538ed114e8969d08ac3073d18b3329ad693cafc40aa90e5a5ad547cd8a57c1}.
 * Generated; do not edit. The SHA-256 that {@link Rfc8259JsonGrammar#prepared()} checks is
 * pinned in that class, not here.</p>
 */
final class Rfc8259JsonGrammarImage {
    private static final String BASE64 =
            "TTNHUgAAAAIAAAAEAEoAUwBPAE4AAAAMAFIARgBDADgAMgA1ADkALQAyADAAMQA3AAAACQAAAAEALAAA"
            + "AAUAAAAAAQA6AAAABgAAAAABAFsAAAADAAAAAAEAXQAAAAQAAAAABQBmAGEAbABzAGUAAAAIAAAAAAQA"
            + "bgB1AGwAbAAAAAkAAAAABAB0AHIAdQBlAAAABwAAAAABAHsAAAABAAAAAAEAfQAAAAIAAAAAAgAAAAoA"
            + "AAA8AFwAIgAoAD8AOgBbAF4AXAAiAFwAXABcAHgAMAAwAC0AXAB4ADEARgBdAHwAXABcACgAPwA6AFsA"
            + "XAAiAFwAXAAvAGIAZgBuAHIAdABdAHwAdQBbADAALQA5AGEALQBmAEEALQBGAF0AewA0AH0AKQApACoA"
            + "XAAiAAAAAAAAAAAAAAALAAAANAAtAD8AKAA/ADoAMAB8AFsAMQAtADkAXQBbADAALQA5AF0AKgApACgA"
            + "PwA6AFwALgBbADAALQA5AF0AKwApAD8AKAA/ADoAWwBlAEUAXQBbACsALQBdAD8AWwAwAC0AOQBdACsA"
            + "KQA/AAAAAAAAAAH////9AAAAFf////kAAAACAAAABf////3////6AAAAAv////n////6////+gAAAAD/"
            + "///7AAAAAv////3////6/////AAAAAH////7/////AAAAAD/////AAAAAwAAAAP////8AAAABP////UA"
            + "AAAEAAAABQAAAAoAAAAG/////f////YAAAAC////9f////b////2AAAAAP////cAAAAEAAAACgAAAAb/"
            + "///9////9v////gAAAAB////9/////gAAAAA/////gAAAAMAAAAB////+AAAAAL////9AAAAAf////7/"
            + "///9AAAAAf/////////9AAAAAQAAAAr////9AAAAAQAAAAv////9AAAAAQAAAAf////9AAAAAQAAAAj/"
            + "///9AAAAAQAAAAkAAAAhAAAAAAAAAAUAAAAHAAAAAQAAAAIAAAAJAAAAAQAAAAUAAAAIAAAAAgAAAAoA"
            + "AAAKAAAAAwAAAAIAAAAMAAAAAwAAAAoAAAALAAAABAAAAAUAAAAAAAAABQAAAAQAAAACAAAABQAAAAUA"
            + "AAABAAAABgAAAAEAAAADAAAABgAAAAMAAAADAAAABgAAAAcAAAADAAAABgAAAAgAAAADAAAABgAAAAkA"
            + "AAADAAAABgAAAAoAAAADAAAABgAAAAsAAAADAAAABwAAAAEAAAAEAAAABwAAAAMAAAAEAAAABwAAAAQA"
            + "AAAFAAAABwAAAAcAAAAEAAAABwAAAAgAAAAEAAAABwAAAAkAAAAEAAAABwAAAAoAAAAEAAAABwAAAAsA"
            + "AAAEAAAACAAAAAEAAAAOAAAACAAAAAMAAAAPAAAACAAAAAcAAAASAAAACAAAAAgAAAATAAAACAAAAAkA"
            + "AAAUAAAACAAAAAoAAAAQAAAACAAAAAsAAAARAAAACQAAAAEAAAANAAAACgAAAAMAAAAGAAAAQAA0ADQA"
            + "NQAxAGEAYgA0ADkAMAAyADQAZgA0ADgAMwA5ADIANwAzADEAOABhAGEAMgBiADMAMABjAGMAOABlAGUA"
            + "NABhADkAMwA2ADYAYQBkADgAZABhADEAZABmAGIAYQAyADkANQA5ADgANwAxAGIAMwBjADEAMwBlAGQA"
            + "MgA184lSIkEFvOJ5c+HD/4FTEhemq81g0CMmFSJDS5GV59E=";

    private Rfc8259JsonGrammarImage() { }

    /** Returns a new copy of the image bytes. */
    static byte[] bytes() {
        return Base64.getDecoder().decode(BASE64);
    }
}
