/* SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors. */
package com.m3.collections.interop;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Discoverable SDK names, contract families and storage provenance; no dependency class loading on lookup. */
public final class M3LibraryBindings {
    private static final int MAX_BYTES = 1_048_576;
    private static final String HEADER = "family\tfactory\tparameters\tcontract\tadapter\tm3_owners\townership\tversion";
    private M3LibraryBindings() { }
    private static final class Loaded { private static final List<M3LibraryBinding> ALL = load(); }

    public static List<M3LibraryBinding> all() { return Loaded.ALL; }

    public static List<M3LibraryBinding> forFamily(M3LibraryBinding.Family family) {
        Objects.requireNonNull(family, "family");
        return all().stream().filter(binding -> binding.family() == family).toList();
    }

    /** Null monitor means no cancellation; adapt an existing monitor with monitor::isCanceled. */
    public static List<M3LibraryBinding> search(String text, BooleanSupplier cancelled) {
        String query = Objects.requireNonNull(text, "text").toLowerCase(Locale.ROOT);
        var matches = new ArrayList<M3LibraryBinding>();
        for (var binding : all()) {
            checkpoint(cancelled);
            if (binding.toString().toLowerCase(Locale.ROOT).contains(query)
                    || binding.family().displayName().toLowerCase(Locale.ROOT).contains(query)) matches.add(binding);
        }
        checkpoint(cancelled);
        return List.copyOf(matches);
    }

    /**
     * Verify exact public factory parameter/return names and owner type availability.
     * Does not instantiate collections, initialize donor classes, or prove behavior/storage internals.
     * Missing dependencies are failures, never silently dropped from the SDK inventory.
     */
    public static int verifyRuntime(ClassLoader loader, BooleanSupplier cancelled) throws ReflectiveOperationException {
        Objects.requireNonNull(loader, "loader");
        Class<?> api = Class.forName("com.m3.collections.interop.M3LibraryCollections", false, loader);
        int checked = 0;
        for (var binding : all()) {
            checkpoint(cancelled);
            Class<?>[] parameters = new Class<?>[binding.parameterTypes().size()];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = Class.forName(binding.parameterTypes().get(i), false, loader);
            }
            var method = api.getDeclaredMethod(binding.factoryName(), parameters);
            if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isStatic(method.getModifiers())
                    || !method.getReturnType().getName().equals(binding.contractType()))
                throw new IllegalStateException("factory signature drift: " + binding.factoryName());
            Class.forName(binding.adapterType(), false, loader);
            for (String owner : binding.m3Owners()) Class.forName(owner, false, loader);
            checked++;
        }
        // A new public factory cannot silently escape the name/owner inventory.
        long factories = java.util.Arrays.stream(api.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())).count();
        if (factories != checked) throw new IllegalStateException("unmapped SDK factory");
        checkpoint(cancelled);
        return checked;
    }

    private static void checkpoint(BooleanSupplier cancelled) {
        if (Thread.currentThread().isInterrupted() || cancelled != null && cancelled.getAsBoolean())
            throw new CancellationException("SDK binding query cancelled");
    }

    private static List<M3LibraryBinding> load() {
        try (InputStream input = M3LibraryBindings.class.getResourceAsStream("/META-INF/m3/sdk-family-bindings.tsv")) {
            if (input == null) throw new IllegalStateException("missing SDK binding resource");
            return read(input);
        } catch (IOException failure) { throw new IllegalStateException("cannot read SDK binding resource", failure); }
    }

    static List<M3LibraryBinding> read(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        byte[] bytes = input.readNBytes(MAX_BYTES + 1);
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("SDK binding resource too large");
        String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        var lines = text.lines().iterator();
        if (!lines.hasNext() || !lines.next().equals(HEADER)) throw new IllegalArgumentException("SDK binding header");
        var bindings = new ArrayList<M3LibraryBinding>();
        var names = new HashSet<String>();
        while (lines.hasNext()) {
            String[] fields = lines.next().split("\t", -1);
            if (fields.length != 8) throw new IllegalArgumentException("SDK binding row width");
            if (!names.add(fields[1] + "(" + fields[2] + ")")) throw new IllegalArgumentException("duplicate SDK factory descriptor");
            bindings.add(new M3LibraryBinding(M3LibraryBinding.Family.valueOf(fields[0]), fields[1],
                    fields[2].isEmpty() ? List.of() : List.of(fields[2].split(";", -1)), fields[3], fields[4],
                    List.of(fields[5].split(";", -1)), M3LibraryBinding.Ownership.valueOf(fields[6]), fields[7]));
        }
        if (bindings.isEmpty()) throw new IllegalArgumentException("empty SDK binding inventory");
        return List.copyOf(bindings);
    }
}
