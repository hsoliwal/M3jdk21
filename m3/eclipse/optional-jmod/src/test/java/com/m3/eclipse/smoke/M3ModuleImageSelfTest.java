// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
package com.m3.eclipse.smoke;

import com.m3.collections.M3IndexMaskCompactor;
import com.m3.file.M3ModuleResources;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

/** Executed by the linked image, requiring the real JRT owner and optional JNI. */
public final class M3ModuleImageSelfTest {
    private M3ModuleImageSelfTest() { }

    public static void main(String[] args) throws Exception {
        boolean nativeRequired = args.length == 1 && args[0].equals("native");
        var backend = nativeRequired ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED
                : M3IndexMaskCompactor.Backend.JAVA;
        var module = ModuleLayer.boot().findModule("com.m3.file").orElseThrow();
        require(M3ModuleResources.class.getModule() == module, "M3 resource class module origin");
        var reference = ModuleFinder.ofSystem().find("com.m3.file").orElseThrow();
        require(reference.location().orElseThrow().getScheme().equals("jrt"), "linked image module origin");
        String entry = "com/m3/file/M3ModuleResources.class";
        M3ModuleResources resources = M3ModuleResources.open(reference);
        try (resources) {
            require(resources.reference() == reference, "reference ownership");
            require(resources.descriptor().equals(reference.descriptor()), "descriptor identity");
            require(resources.contains(entry), "resource lookup");
            int[] javaRows = resources.select("com/m3/file/", M3ModuleResources.CLASS,
                    M3ModuleResources.MODULE_DESCRIPTOR, M3IndexMaskCompactor.Backend.JAVA);
            int[] selected = resources.select("com/m3/file/", M3ModuleResources.CLASS,
                    M3ModuleResources.MODULE_DESCRIPTOR, backend);
            require(javaRows.length > 0 && Arrays.equals(javaRows, selected), "Java/JNI image row parity");
            require(Arrays.equals(selected, resources.select("com/m3/file/", M3ModuleResources.CLASS,
                    M3ModuleResources.MODULE_DESCRIPTOR, backend)), "repeated physical row selection");
            for (int row : selected) require(resources.nameAt(row).startsWith("com/m3/file/"), "prefix boundary");
            try (var indexed = resources.openStream(entry);
                 var loaded = M3ModuleResources.class.getResourceAsStream("M3ModuleResources.class")) {
                require(loaded != null, "loaded class bytes available");
                require(Arrays.equals(indexed.readAllBytes(), loaded.readAllBytes()), "index/loaded class byte origin");
            }
            require(resources.find(entry).orElseThrow().getScheme().equals("jrt"), "JRT resource URI");
        }
        require(!resources.isOpen(), "owner close");
        try {
            resources.open(entry);
            throw new AssertionError("closed owner accepted a read");
        } catch (IOException expected) {
            // Closure is a resource-lifetime boundary, independent of class-loader identity.
        }
        if (nativeRequired) {
            require(M3IndexMaskCompactor.nativeAvailable(), "native execution required");
            Path library = Path.of(System.getProperty("java.home"), "lib", "libm3_collections_mask.so");
            require(Files.isRegularFile(library), "image owns the native library");
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(library)));
            System.out.println("M3_LINKED_NATIVE_SHA256=" + digest);
        }
        System.out.println("M3_MODULE_IMAGE_PASS backend=" + backend + " origin=jrt module=" + module.getName());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
