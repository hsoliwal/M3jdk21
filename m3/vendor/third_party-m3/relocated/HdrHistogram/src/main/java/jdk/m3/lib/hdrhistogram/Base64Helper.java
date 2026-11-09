/**
 * Written by Gil Tene of Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 * @author Gil Tene
 */

package jdk.m3.lib.hdrhistogram;

import java.util.Base64;

/**
 * Base64Helper exists to bridge inconsistencies in Java SE support of Base64 encoding and decoding.
 * Earlier Java SE platforms (up to and including Java SE 8) supported base64 encode/decode via the
 * javax.xml.bind.DatatypeConverter class, which was deprecated and eventually removed in Java SE 9.
 * Later Java SE platforms (Java SE 8 and later) support base64 encode/decode via the
 * java.util.Base64 class (first introduced in Java SE 8, and not available on e.g. Java SE 6 or 7).
 *
 * This makes it "hard" to write a single piece of source code that deals with base64 encodings and
 * will compile and run on e.g. Java SE 7 AND Java SE 9. And such common source is a common need for
 * libraries. This class is intended to encapsulate this "hard"-ness and hide the ugly pretzle-twising
 * needed under the covers.
 *
 * Base64Helper provides a common API that works across Java SE 6..9 (and beyond hopefully), and
 * uses late binding (Reflection) internally to avoid javac-compile-time dependencies on a specific
 * Java SE version (e.g. beyond 7 or before 9).
 *
 * <p>
 * M3JDK (BASE64_TO_JDK v1): M3JDK is Java SE 21, so this class calls {@code java.util.Base64} directly,
 * with no reflection and no javax.xml.bind fallback.
 * </p>
 */
class Base64Helper {

    //FLLDCIM: M3 - BASE64_TO_JDK v1: base64 is java.base's java.util.Base64, the JDK's one implementation

    /**
     * Converts an array of bytes into a Base64 string.
     *
     * @param binaryArray A binary encoded input array
     * @return a String containing the Base64 encoded equivalent of the binary input
     */
    static String printBase64Binary(byte [] binaryArray) {
        return Base64.getEncoder().encodeToString(binaryArray);
    }

    /**
     * Converts a Base64 encoded String to a byte array
     *
     * @param base64input A base64-encoded input String
     * @return a byte array containing the binary representation equivalent of the Base64 encoded input
     */
    static byte[] parseBase64Binary(String base64input) {
        return Base64.getDecoder().decode(base64input);
    }

}
