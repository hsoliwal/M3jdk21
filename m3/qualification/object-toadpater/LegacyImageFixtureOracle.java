// SPDX-License-Identifier: GPL-2.0-only
// This fixture validates the historic JVM bytes, without claiming M3JDK runtime dispatch.
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class LegacyImageFixtureOracle {
    private static final class Loader extends ClassLoader {
        Loader() { super(null); }
        Class<?> define(byte[] bytes) {
            return defineClass("LegacyPreAliasSubclass", bytes, 0, bytes.length);
        }
    }
    public static void main(String[] args) throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of(args[0]));
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if (!sha.equals("4eff8d88c8154820bee15152e0f2cb4791b305cc67b5675b34276310aa568e36")) {
            throw new AssertionError("historical bytes changed");
        }
        Class<?> old = new Loader().define(bytes);
        Method alias = old.getDeclaredMethod("toAdpater", Class.class);
        Object receiver = old.getConstructor().newInstance();
        if (!"LEGACY_ALIAS_CHILD".equals(alias.invoke(receiver, String.class))) {
            throw new AssertionError("old virtual method returned unexpected value");
        }
        if (alias.invoke(receiver, Runnable.class) != null) {
            throw new AssertionError("unsupported type should return null");
        }
        System.out.println("PASS historical-subclass-binary sha256=" + sha);
    }
}
