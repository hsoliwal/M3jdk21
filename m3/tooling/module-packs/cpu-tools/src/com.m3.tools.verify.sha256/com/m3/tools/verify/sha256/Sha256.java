// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK21 module name.
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.verify.sha256;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
/** Standard JDK SHA-256 content identity; no native dependency. */
public final class Sha256 {
    private Sha256() { }
    public static String hex(byte[] bytes) {
        Objects.requireNonNull(bytes);
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException impossible){throw new ExceptionInInitializerError(impossible);}
    }
    public static String utf8(String text){return hex(Objects.requireNonNull(text).getBytes(StandardCharsets.UTF_8));}
    public static void main(String[] args){
        if(args.length==1&&args[0].equals("--self-test")){
            if(!utf8("abc").equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"))
                throw new AssertionError("SHA256");
            System.out.println("M3_JMOD_TEST_PASS verify.sha256");
        }else System.out.println(utf8(String.join(" ",args)));
    }
}
