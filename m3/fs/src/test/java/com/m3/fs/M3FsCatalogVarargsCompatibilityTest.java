// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for the M3JDK21 M3FS runtime from the pinned Synexia donor.
package com.m3.fs;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/**
 * The sealed annotation-warning fixture predates five profiles and two pipeline lookup methods
 * in the pinned donor. Preserve that baseline exactly and admit only those explicit additions.
 */
class M3FsCatalogVarargsCompatibilityTest {
  private static final String CATALOG = "com.m3.fs.M3FsPrecomputeCatalog";
  private static final String ROLE = "com.m3.fs.M3FsPrecomputeBundle$Role";
  @TempDir static Path temporary;
  private static Path beforeClasses;

  @BeforeAll static void compileSealedBaselineWithRealDependencyClasspath() throws Exception {
    byte[] source;
    try (var in = M3FsCatalogVarargsCompatibilityTest.class.getResourceAsStream("/m3-fs-safevarargs/M3FsPrecomputeCatalog.java")) {
      assertNotNull(in); source = in.readAllBytes();
    }
    assertEquals("4881ecb902097947162005442c3e35ee7a4a471c5e57d1957ea1db188befc70c",
        java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(source)));
    Path file = temporary.resolve("src/com/m3/fs/M3FsPrecomputeCatalog.java");
    Files.createDirectories(file.getParent());
    // The resource remains byte-for-byte sealed; translate only its package for the isolated compile.
    Files.writeString(file, new String(source, StandardCharsets.UTF_8)
        .replace("com.synexia.indexstring.fs", "com.m3.fs"), StandardCharsets.UTF_8);
    beforeClasses = temporary.resolve("classes"); Files.createDirectories(beforeClasses);
    var compiler = ToolProvider.getSystemJavaCompiler(); assertNotNull(compiler, "JDK compiler required");
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
      boolean success = compiler.getTask(null, manager, diagnostics,
          List.of("--release", "21", "-proc:none", "-Xlint:all", "-classpath", classpath(), "-d", beforeClasses.toString()),
          null, manager.getJavaFileObjects(file.toFile())).call();
      assertTrue(success, diagnostics.getDiagnostics().toString());
    }
    assertTrue(diagnostics.getDiagnostics().stream().anyMatch(d ->
        d.getKind() == Diagnostic.Kind.WARNING && d.getCode().contains("varargs")),
        "the genuine baseline must retain its known redundant annotation warning");
  }

  @Test void privateVarargsDescriptorAndValuesSurviveIntentionalAnnotationRemoval() throws Exception {
    try (URLClassLoader before = before(); URLClassLoader after = after()) {
      Method b = roles(before); Method a = roles(after);
      assertTrue(b.isVarArgs()); assertTrue(a.isVarArgs());
      assertTrue(Modifier.isPrivate(a.getModifiers())); assertTrue(Modifier.isStatic(a.getModifiers()));
      assertEquals(b.toGenericString(), a.toGenericString());
      assertTrue(b.isAnnotationPresent(SafeVarargs.class));
      assertFalse(a.isAnnotationPresent(SafeVarargs.class)); // Explicit intended private metadata change.
      assertEquals(values(before, 0), values(after, 0));
      assertEquals(values(before, 1), values(after, 1));
      assertEquals(values(before, 2), values(after, 2));
      assertEquals(Set.of(), values(after, 0));
    }
  }

  @Test void nullAndDuplicateArgumentsRetainExactExceptionTypeAndMessage() throws Exception {
    try (URLClassLoader before = before(); URLClassLoader after = after()) {
      for (String mode : List.of("null-array", "null-element", "duplicate")) {
        assertEquals(failure(before, mode), failure(after, mode), mode);
      }
      assertTrue(failure(after, "null-array").startsWith("java.lang.NullPointerException:"));
      assertTrue(failure(after, "duplicate").startsWith("java.lang.IllegalArgumentException:"));
    }
  }

  @Test void publicCataloguePreservesSealedBaselineAndExactPinnedDonorAdditions() throws Exception {
    try (URLClassLoader before = before(); URLClassLoader after = after()) {
      Class<?> b = before.loadClass(CATALOG); Class<?> a = after.loadClass(CATALOG);
      List<String> baselineApi = publicApi(b), currentApi = publicApi(a);
      assertTrue(currentApi.containsAll(baselineApi));
      Set<String> addedApi = new java.util.TreeSet<>(currentApi); addedApi.removeAll(baselineApi);
      assertEquals(Set.of(
          "public static com.m3.fs.M3FsPrecomputeCatalog$Pipeline com.m3.fs.M3FsPrecomputeCatalog.pipelineById(java.lang.String)",
          "public static java.util.List<com.m3.fs.M3FsPrecomputeCatalog$Pipeline> com.m3.fs.M3FsPrecomputeCatalog.pipelines()"), addedApi);
      List<String> original = profiles(b); List<String> candidate = profiles(a);
      assertFalse(original.isEmpty());
      Set<String> originalIds = original.stream().map(M3FsCatalogVarargsCompatibilityTest::profileId)
          .collect(java.util.stream.Collectors.toSet());
      assertEquals(original, candidate.stream().filter(p -> originalIds.contains(profileId(p))).toList());
      Set<String> additions = new java.util.TreeSet<>(candidate);
      additions.removeAll(original);
      assertEquals(Set.of(
          "gpu-prepared-byte-segments-v1|KEEP|[BYTE_PRESENCE_BITMAP_I64, CANDIDATE_BYTES, PAIR_NGRAM_BITMAP_I64, RECORD_LENGTHS_I32, RECORD_OFFSETS_I32, SEARCH_METADATA_I32, TRIGRAM_NGRAM_BITMAP_I64]|[]|[BYTE_SEGMENT_GREP, BITMAP_AND_I64]",
          "gpu-prepared-byte-machine-v1|KEEP|[AUTOMATON_METADATA_I32, AUTOMATON_OUTPUT_MASKS_I64, AUTOMATON_TRANSITIONS_I32, PATTERN_BYTES_U8, PATTERN_OFFSETS_I32]|[]|[BYTE_AUTOMATON_MATCH64_SEGMENTS]",
          "mindex-segmented-utf16-v1|KEEP|[SEGMENT_LENGTH_I32, SEGMENT_LOGICAL_END_I32, SEGMENT_PARENT_INDEX_I32, SEGMENT_PARENT_START_I32, UTF16_CODE_UNITS_I16]|[]|[UTF16_SCATTER_GATHER]",
          "gpu-grep-segments-v1|KEEP|[CANDIDATE_BYTES, RECORD_LENGTHS_I32, RECORD_OFFSETS_I64]|[BITMAP_WORDS_I64, HASH64_I64]|[BYTE_PATTERN_SCAN]",
          "compact-ascii-dfa-v1|KEEP|[AUTOMATON_ACCEPT_U8, AUTOMATON_ASCII_CLASS_MAP_U8, AUTOMATON_METADATA_I32, AUTOMATON_TRANSITIONS_I16]|[]|[ASCII_DFA_BATCH]"), additions);
      for (Object profile : (List<?>) b.getMethod("profiles").invoke(null)) {
        String id = (String) profile.getClass().getMethod("id").invoke(profile);
        assertEquals(profile(profile), profile(a.getMethod("byId", String.class).invoke(null, id)));
      }
      for (String operation : List.of("", "unknown", " ", "regex", "matmul")) {
        assertEquals(operation(b, operation), operation(a, operation));
      }
    }
  }

  private static String profileId(String profile) { return profile.substring(0, profile.indexOf('|')); }

  private static URLClassLoader before() throws Exception {
    List<URL> urls = new ArrayList<>(); urls.add(beforeClasses.toUri().toURL()); urls.addAll(classpathUrls());
    return new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
  }
  private static URLClassLoader after() throws Exception {
    return new URLClassLoader(classpathUrls().toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
  }
  private static String classpath() {
    return System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
  }
  private static List<URL> classpathUrls() throws Exception {
    List<URL> urls = new ArrayList<>();
    for (String entry : classpath().split(java.util.regex.Pattern.quote(java.io.File.pathSeparator)))
      urls.add(Path.of(entry).toAbsolutePath().toUri().toURL());
    return urls;
  }
  private static Method roles(ClassLoader loader) throws Exception {
    Class<?> role = loader.loadClass(ROLE); assertTrue(role.isEnum());
    Method method = loader.loadClass(CATALOG).getDeclaredMethod("roles", Array.newInstance(role, 0).getClass());
    method.setAccessible(true); return method;
  }
  private static Set<String> values(ClassLoader loader, int count) throws Exception {
    Class<?> role = loader.loadClass(ROLE); Object array = Array.newInstance(role, count);
    for (int i = 0; i < count; i++) Array.set(array, i, role.getEnumConstants()[i]);
    return Set.copyOf(names((Collection<?>) roles(loader).invoke(null, array)));
  }
  private static String failure(ClassLoader loader, String mode) throws Exception {
    Class<?> role = loader.loadClass(ROLE); Object array = null;
    if (!mode.equals("null-array")) {
      array = Array.newInstance(role, mode.equals("duplicate") ? 2 : 1);
      if (mode.equals("duplicate")) { Array.set(array,0,role.getEnumConstants()[0]); Array.set(array,1,role.getEnumConstants()[0]); }
    }
    Object argument = array;
    var error = assertThrows(InvocationTargetException.class, () -> roles(loader).invoke(null, argument));
    return error.getCause().getClass().getName() + ":" + error.getCause().getMessage();
  }
  private static List<String> publicApi(Class<?> type) {
    return java.util.Arrays.stream(type.getDeclaredMethods()).filter(m -> Modifier.isPublic(m.getModifiers()))
        .map(Method::toGenericString).sorted().toList();
  }
  private static List<String> profiles(Class<?> type) throws Exception {
    List<String> result = new ArrayList<>();
    for (Object profile : (List<?>) type.getMethod("profiles").invoke(null)) result.add(profile(profile));
    return result;
  }
  private static String profile(Object value) throws Exception {
    Class<?> type = value.getClass();
    return type.getMethod("id").invoke(value) + "|" + type.getMethod("retention").invoke(value) + "|"
        + names((Collection<?>) type.getMethod("requiredRoles").invoke(value)) + "|"
        + names((Collection<?>) type.getMethod("optionalRoles").invoke(value)) + "|"
        + type.getMethod("acceleratorOperations").invoke(value);
  }
  private static List<String> names(Collection<?> values) {
    return values.stream().map(value -> ((Enum<?>) value).name()).sorted().toList();
  }
  private static String operation(Class<?> type, String value) throws Exception {
    try {
      List<String> out = new ArrayList<>();
      for (Object p : (List<?>) type.getMethod("forOperation",String.class).invoke(null,value)) out.add(profile(p));
      return out.toString();
    } catch (InvocationTargetException error) {
      return error.getCause().getClass().getName()+":"+error.getCause().getMessage();
    }
  }
}
