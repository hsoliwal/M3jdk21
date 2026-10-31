/* SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors. */
package com.m3.collections.interop;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;

/** Executes real binding/query code without pretending external donor runtimes are installed. */
public final class M3LibraryBindingsSelfTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("binding check " + checks); }
    private static void fails(Class<? extends Throwable> type, Action action) {
        try { action.run(); } catch (Throwable error) { check(type.isInstance(error)); return; }
        throw new AssertionError("expected " + type.getName());
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    public static void main(String[] args) throws Exception {
        var all = M3LibraryBindings.all();
        check(all.size() == 22);
        check(M3LibraryBindings.forFamily(M3LibraryBinding.Family.JDK).size() == 6);
        check(M3LibraryBindings.forFamily(M3LibraryBinding.Family.GUAVA).size() == 6);
        check(M3LibraryBindings.forFamily(M3LibraryBinding.Family.ECLIPSE_COLLECTIONS).size() == 5);
        check(M3LibraryBindings.forFamily(M3LibraryBinding.Family.SPRING_FRAMEWORK).size() == 3);
        check(M3LibraryBindings.forFamily(M3LibraryBinding.Family.APACHE_COMMONS_COLLECTIONS).size() == 2);
        check(M3LibraryBindings.all() == all);
        fails(UnsupportedOperationException.class, () -> all.clear());
        for (var binding : all) {
            check(!binding.m3Owners().isEmpty());
            check(binding.descriptor().startsWith(binding.factoryName() + "("));
            check(M3LibraryBindings.search(binding.factoryName(), null).contains(binding));
            check(M3LibraryBindings.search(binding.family().displayName(), null).contains(binding));
            fails(UnsupportedOperationException.class, () -> binding.m3Owners().clear());
        }
        check(M3LibraryBindings.search("unlikely-nonexistent-binding", null).isEmpty());
        check(M3LibraryBindings.search("", null).equals(all));
        fails(CancellationException.class, () -> M3LibraryBindings.search("", () -> true));
        Thread.currentThread().interrupt();
        try { fails(CancellationException.class, () -> M3LibraryBindings.search("", null)); }
        finally { check(Thread.interrupted()); }
        Locale old = Locale.getDefault();
        try { Locale.setDefault(Locale.forLanguageTag("tr-TR")); check(M3LibraryBindings.search("SPRING", null).size() == 3); }
        finally { Locale.setDefault(old); }
        var first = all.get(0);
        var owners = new ArrayList<>(first.m3Owners());
        var copy = new M3LibraryBinding(first.family(), first.factoryName(), first.parameterTypes(),
                first.contractType(), first.adapterType(), owners, first.ownership(), first.version());
        owners.clear(); check(!copy.m3Owners().isEmpty());
        String text = Files.readString(Path.of(args[0]));
        check(M3LibraryBindings.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))).equals(all));
        var line = text.lines().skip(1).findFirst().orElseThrow();
        String overload = line.replace("jdkMap\t\t", "jdkMap\tjava.util.Comparator\t");
        check(M3LibraryBindings.read(new ByteArrayInputStream((text + overload + "\n").getBytes(StandardCharsets.UTF_8))).size() == 23);
        for (String bad : List.of(text + line + "\n", text.replace("GUAVA", "GUVAVA"),
                text.replace("jdkMap", "apacheMap"), text.replace("com.m3.collections.M3LinkedHashMap", "java.util.HashMap"),
                text.replace("java.util.Map", "org.springframework.util.MultiValueMap"),
                text.replace("JDK\tjdkMap", "JDK\tjdkMap\textra"), "", text.lines().findFirst().orElseThrow() + "\n")) {
            fails(IllegalArgumentException.class, () -> M3LibraryBindings.read(new ByteArrayInputStream(bad.getBytes(StandardCharsets.UTF_8))));
        }
        fails(java.nio.charset.CharacterCodingException.class,
                () -> M3LibraryBindings.read(new ByteArrayInputStream(new byte[]{(byte)0xc3, 0x28})));
        fails(IllegalArgumentException.class, () -> M3LibraryBindings.read(new ByteArrayInputStream(new byte[1_048_577])));
        System.out.println("M3LibraryBindingsSelfTest checks=" + checks + " families=5 bindings=22");
    }
}
