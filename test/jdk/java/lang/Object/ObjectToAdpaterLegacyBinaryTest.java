/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: GPL-2.0-only
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details.
 */

/*
 * @test
 * @summary Precompiled Java 21 subclass of Object with pre-existing toAdpater dispatch
 * @run main/othervm ObjectToAdpaterLegacyBinaryTest
 * @run main/othervm -Xint ObjectToAdpaterLegacyBinaryTest
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

public final class ObjectToAdpaterLegacyBinaryTest {
    // Compiled with stock OpenJDK21 before java.lang.Object declared toAdpater.
    // Source: public class LegacyPreAliasSubclass {
    //   public <T> T toAdpater(Class<T> clazz) {
    //     if (clazz == String.class) return clazz.cast("LEGACY_ALIAS_CHILD");
    //     return null;
    //   }
    // }
    // The frozen classfile has JVM major version 65.
    private static final String LEGACY_B64 =
            "yv66vgAAAEEAHAoAAgADBwAEDAAFAAYBABBqYXZhL2xhbmcvT2JqZWN0AQAGPGluaXQ+AQADKClWBwAIAQAQamF2YS9s" +
            "YW5nL1N0cmluZwgACgEAEkxFR0FDWV9BTElBU19DSElMRAoADAANBwAODAAPABABAA9qYXZhL2xhbmcvQ2xhc3MBAARj" +
            "YXN0AQAmKExqYXZhL2xhbmcvT2JqZWN0OylMamF2YS9sYW5nL09iamVjdDsHABIBABZMZWdhY3lQcmVBbGlhc1N1YmNs" +
            "YXNzAQAEQ29kZQEAD0xpbmVOdW1iZXJUYWJsZQEACXRvQWRwYXRlcgEAJShMamF2YS9sYW5nL0NsYXNzOylMamF2YS9s" +
            "YW5nL09iamVjdDsBAA1TdGFja01hcFRhYmxlAQAJU2lnbmF0dXJlAQAxPFQ6TGphdmEvbGFuZy9PYmplY3Q7PihMamF2" +
            "YS9sYW5nL0NsYXNzPFRUOz47KVRUOwEAClNvdXJjZUZpbGUBABtMZWdhY3lQcmVBbGlhc1N1YmNsYXNzLmphdmEAIQAR" +
            "AAIAAAAAAAIAAQAFAAYAAQATAAAAHQABAAEAAAAFKrcAAbEAAAABABQAAAAGAAEAAAADAAEAFQAWAAIAEwAAADgAAgAC" +
            "AAAADysSB6YACisSCbYAC7ABsAAAAAIAFAAAAA4AAwAAAAUABgAGAA0ACAAXAAAAAwABDQAYAAAAAgAZAAEAGgAAAAIA" +
            "Gw==";
    private static final String CLASS_SHA256 =
            "4eff8d88c8154820bee15152e0f2cb4791b305cc67b5675b34276310aa568e36";
    private static int checks;

    private static final class PreAliasLoader extends ClassLoader {
        private PreAliasLoader() { super(null); }

        private Class<?> define(byte[] bytes) {
            return defineClass("LegacyPreAliasSubclass", bytes, 0, bytes.length);
        }
    }

    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }

    private static void same(Object left, Object right, String label) {
        check(left == right, label);
    }

    public static void main(String[] args) throws Throwable {
        byte[] bytes = Base64.getDecoder().decode(LEGACY_B64);
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        check(CLASS_SHA256.equals(actual), "the historical binary hash changed");
        check(bytes.length > 8 && bytes[0] == (byte) 0xca && bytes[1] == (byte) 0xfe
                && bytes[2] == (byte) 0xba && bytes[3] == (byte) 0xbe,
                "not a classfile");
        check(((bytes[6] & 0xff) << 8 | (bytes[7] & 0xff)) == 65,
                "not a Java 21 classfile");
        Class<?> legacyClass = new PreAliasLoader().define(bytes);
        same(Object.class, legacyClass.getSuperclass(), "original java.lang.Object superclass");
        Method historical = legacyClass.getDeclaredMethod("toAdpater", Class.class);
        check(Modifier.isPublic(historical.getModifiers()), "historical method not public");
        same(Object.class, historical.getReturnType(), "historical erased return");
        Method base = Object.class.getDeclaredMethod("toAdpater", Class.class);
        check(!Modifier.isFinal(base.getModifiers()), "new base alias is final");
        Object oldInstance = legacyClass.getConstructor().newInstance();
        String answer = oldInstance.toAdpater(String.class);
        check("LEGACY_ALIAS_CHILD".equals(answer), "old subclass did not override new method");
        check(oldInstance.toAdpater(Runnable.class) == null,
                "old subclass unsupported return changed");
        same(null, oldInstance.toAdapter(String.class),
                "the separate incumbent base toAdapter must be unaffected");
        check("LEGACY_ALIAS_CHILD".equals(base.invoke(oldInstance, String.class)),
                "reflective call ignored pre-existing override");
        MethodHandle virtual = MethodHandles.publicLookup().findVirtual(Object.class,
                "toAdpater", MethodType.methodType(Object.class, Class.class));
        check("LEGACY_ALIAS_CHILD".equals(virtual.invokeWithArguments(oldInstance, String.class)),
                "MethodHandle ignored pre-existing override");
        System.out.println("ObjectToAdpaterLegacyBinaryTest PASS checks=" + checks);
    }
}
