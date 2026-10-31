package jdk.m3.graphics.impl;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class M3Library {
    public static volatile boolean _loaded = false;
    public static final String LIBRARY_NAME = "skija";

    public static void staticLoad() {
        if (!_loaded && !"false".equals(System.getProperty("skija.staticLoad"))) load();
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
        try {
            if (_loaded) return;
            // If `skija.loadFromLibraryPath` is set to true, use System.loadLibrary to load the native library
            String loadFromLibraryPath = System.getProperty("skija.loadFromLibraryPath");
            if ("true".equals(loadFromLibraryPath)) {
                _loadFromLibraryPath();
                return;
            }
            // If `skija.library.path` is set, load the native library from this path
            String libraryPath = System.getProperty("skija.library.path");
            if (libraryPath != null) {
                _loadFromDir(new File(libraryPath));
                return;
            }
            boolean failedLoadFromLibraryPath = false;
            // If skija is bundled in JRE, try to load the bundled native library
            // User can disable this behavior by set `skija.loadFromLibraryPath` to false
            if (loadFromLibraryPath == null) {
                URL theClassFile = M3Library.class.getResource("M3Library.class");
                if (theClassFile != null && "jrt".equals(theClassFile.getProtocol())) {
                    try {
                        _loadFromLibraryPath();
                        return;
                    } catch (UnsatisfiedLinkError e) {
                        failedLoadFromLibraryPath = true;
                        M3Log.warn("Please use skija platform jmod when using jlink");
                    }
                }
            }
            String osName = M3OperatingSystem.CURRENT.name().toLowerCase(Locale.ROOT);
            String archName = M3Architecture.CURRENT.name().toLowerCase(Locale.ROOT);
            String moduleName = "io.github.humbleui.skija." + osName + "." + archName;
            String basePath = moduleName.replace('.', '/') + "/";
            try (M3ResourceFinder finder = new M3ResourceFinder(moduleName)) {
                URL versionFile = finder.findResource(basePath + "skija.version");
                // The platform library is bundled in the JRE, but the skija shared is not bundled.
                // Platforms without official support can provide native libraries in this way.
                if (loadFromLibraryPath == null && !failedLoadFromLibraryPath && versionFile != null && "jrt".equals(versionFile.getProtocol())) {
                    try {
                        _loadFromLibraryPath();
                        return;
                    } catch (UnsatisfiedLinkError e) {
                        failedLoadFromLibraryPath = true;
                        M3Log.warn("Please use skija platform jmod when using jlink");
                    }
                }
                // Finally, try to load the bundled native library
                String version = versionFile != null ? readResource(versionFile) : null;
                boolean isSnapshot = version == null || version.endsWith("SNAPSHOT");
                File tempDir = new File(System.getProperty("java.io.tmpdir"), "skija_" + (isSnapshot ? String.valueOf(System.nanoTime()) : version + "_" + archName));
                File libFile = _extract(finder, basePath, System.mapLibraryName(LIBRARY_NAME), tempDir);
                if (M3OperatingSystem.CURRENT == M3OperatingSystem.WINDOWS) {
                    _extract(finder, basePath, "icudtl.dat", tempDir);
                }
                if (isSnapshot && tempDir.exists()) {
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            Files.walk(tempDir.toPath()).map(Path::toFile).sorted(Comparator.reverseOrder()).forEach(f -> {
                                M3Log.debug("Deleting " + f);
                                f.delete();
                            });
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }));
                }
                _loadFromFile(libFile);
            }
        } catch (final java.lang.Throwable $ex) {
            throw M3SneakyThrow.sneakyThrow($ex);
        }
    }

    public static void _loadFromLibraryPath() {
        M3Log.debug("Loading skija native library from library path");
        System.loadLibrary(LIBRARY_NAME);
        _loaded = true;
        _nAfterLoad();
    }

    public static void _loadFromDir(File dir) {
        String libPath = new File(dir, System.mapLibraryName(LIBRARY_NAME)).getAbsolutePath();
        M3Log.debug("Loading " + libPath);
        System.load(libPath);
        _loaded = true;
        _nAfterLoad();
    }

    public static void _loadFromFile(File file) {
        String libPath = file.getAbsolutePath();
        M3Log.debug("Loading " + libPath);
        System.load(libPath);
        _loaded = true;
        _nAfterLoad();
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
