package jdk.m3.graphics.impl;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class M3Library {
    public static volatile boolean _loaded = false;
    public static final String LIBRARY_NAME = "skija";
    private static final String NATIVE_LIBRARY_NAME = "m3graphics";
    private static boolean loading;

    public static void staticLoad() {
        if (!_loaded && !"false".equals(property("staticLoad"))) load();
    }

    public static String readResource(URL url) {
        try (Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
            StringBuilder builder = new StringBuilder();
            int ch;
            while ((ch = reader.read()) >= 0) {
                builder.append((char) ch);
            }
            return builder.toString().trim();
        } catch (IOException e) {
            return null;
        }
    }

    public static synchronized void load() {
        // FindClass during native initialization can initialize another graphics class.
        // Preserve the original reentrant bootstrap behavior.
        if (_loaded || loading) return;
        String loadFromLibraryPath = property("loadFromLibraryPath");
        if ("true".equals(loadFromLibraryPath)) {
            _loadFromLibraryPath();
            return;
        }
        String libraryPath = property("library.path");
        if (libraryPath != null) {
            _loadFromDir(new File(libraryPath));
            return;
        }
        File runtimeDir = new File(System.getProperty("java.home"),
                M3OperatingSystem.CURRENT == M3OperatingSystem.WINDOWS ? "bin" : "lib");
        File installed = new File(runtimeDir, System.mapLibraryName(NATIVE_LIBRARY_NAME));
        if (installed.isFile()) {
            _loadFromFile(installed);
        } else if (!"false".equals(loadFromLibraryPath)) {
            loadNative(() -> System.loadLibrary(NATIVE_LIBRARY_NAME));
        } else {
            throw new UnsatisfiedLinkError("jdk.m3.graphics native library is not installed: " + installed);
        }
    }

    public static void _loadFromLibraryPath() {
        loadNative(() -> {
            M3Log.debug("Loading jdk.m3.graphics native library from library path");
            try {
                System.loadLibrary(NATIVE_LIBRARY_NAME);
            } catch (UnsatisfiedLinkError missing) {
                // Preserve explicitly requested installations using the historic filename.
                // Their JNI exports must still implement this module's descriptors.
                try {
                    System.loadLibrary(LIBRARY_NAME);
                } catch (UnsatisfiedLinkError legacyMissing) {
                    missing.addSuppressed(legacyMissing);
                    throw missing;
                }
            }
        });
    }

    public static void _loadFromDir(File dir) {
        File library = new File(dir, System.mapLibraryName(NATIVE_LIBRARY_NAME));
        if (!library.isFile()) {
            File legacy = new File(dir, System.mapLibraryName(LIBRARY_NAME));
            if (legacy.isFile()) library = legacy;
        }
        _loadFromFile(library);
    }

    public static void _loadFromFile(File file) {
        String libPath = file.getAbsolutePath();
        loadNative(() -> {
            M3Log.debug("Loading " + libPath);
            System.load(libPath);
        });
    }

    private static String property(String suffix) {
        String value = System.getProperty("m3.graphics." + suffix);
        return value != null ? value : System.getProperty("skija." + suffix);
    }

    private static synchronized void loadNative(Runnable loader) {
        if (_loaded || loading) return;
        loading = true;
        boolean initialized = false;
        try {
            loader.run();
            // JNI resolves graphics classes whose initializers call staticLoad().
            // Publish the loaded library as before, then roll back on init failure.
            // Waiting here in another class initializer can deadlock FindClass.
            _loaded = true;
            _nAfterLoad();
            initialized = true;
        } finally {
            if (!initialized) _loaded = false;
            loading = false;
        }
    }

    public static File _extract(M3ResourceFinder finder, String resourcePath, String fileName, File tempDir) {
        try {
            File file;
            URL url = finder.findResource(resourcePath + fileName);
            if (url == null) {
                file = new File(fileName);
                if (!file.exists()) throw new UnsatisfiedLinkError("Library file " + fileName + " not found in " + resourcePath);
            } else if ("file".equals(url.getProtocol())) {
                file = new File(url.toURI());
            } else {
                file = new File(tempDir, fileName);
                try (InputStream is = url.openStream()) {
                    if (file.exists() && file.length() != is.available()) {
                        file.delete();
                    }
                    if (!file.exists()) {
                        if (!tempDir.exists()) {
                            tempDir.mkdirs();
                        }
                        File fileTmp = File.createTempFile(fileName, "tmp", tempDir);
                        M3Log.debug("Extracting " + fileName + " to " + file + " via " + fileTmp);
                        Files.copy(is, fileTmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        Files.move(fileTmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            return file;
        } catch (final java.lang.Throwable $ex) {
            throw M3SneakyThrow.sneakyThrow($ex);
        }
    }

    public static native void _nAfterLoad();
}
