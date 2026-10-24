// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package com.synexia.proof;

import com.m3.arrays.M3ArrayNative;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
* Test-only configured-startup contract; this source is not an executed proof.
* Run EACH mode in a fresh, bounded Java 21 JVM with -ea -Xcheck:jni and
* -Djava.security.manager=allow. SecurityManager is used only by this fixture.
* Arguments: mode, actual-provider-directory, valid-production-DLL-path,
* expected-module-name (or UNNAMED), optional mode-specific argument.
* The first-* modes use the DLL path verbatim: supply absolute, relative,
* actual space-containing and actual Unicode paths in separate fresh JVMs.
* legacy requires the existing legacy DLL as argument 5. nonexistent-retry
* optionally accepts a known nonexistent path as argument 5.
* Compile against the actual recipe output and complete arrays provider.
* This fixture never creates a loader, supplies backing arrays, resets loaded,
* compiles a class, writes a file, or calls System.load itself.
*/
@SuppressWarnings("removal")
public final class M3ConfiguredArrayStartupContract {
  private static final String PROPERTY = "m3.arrays.native.path";
  private static final String UNLOADED = "M3 array JNI library is not loaded";
  private static final long WAIT_NANOS = TimeUnit.SECONDS.toNanos(10);
  private static final AtomicInteger CHECKS = new AtomicInteger();
  private static final Class<?>[] TYPES =
      {Object[].class, byte[].class, int[].class, long[].class};

  private M3ConfiguredArrayStartupContract() {}

  @FunctionalInterface
  private interface Action {
    void run() throws Exception;
  }

  private static void check(boolean condition, String message) {
    CHECKS.incrementAndGet();
    if (!condition) throw new AssertionError(message);
  }

  private static Object allocate(int type, int length) {
    return switch (type) {
      case 0 -> M3ArrayNative.allocateObjectsRequired(length);
      case 1 -> M3ArrayNative.allocateBytesRequired(length);
      case 2 -> M3ArrayNative.allocateIntsRequired(length);
      case 3 -> M3ArrayNative.allocateLongsRequired(length);
      default -> throw new AssertionError("unknown array type: " + type);
    };
  }

  private static Throwable failure(int type, int length,
      Class<? extends Throwable> expected) {
    Throwable observed = null;
    try {
      allocate(type, length);
    } catch (Throwable error) {
      observed = error;
    }
    check(observed != null && observed.getClass() == expected,
        "failure type=" + type + " length=" + length
            + " expected=" + expected.getName() + " actual=" + observed);
    if (expected == NegativeArraySizeException.class) {
      check(Integer.toString(length).equals(observed.getMessage()),
          "exact negative-size message");
    }
    return observed;
  }

  private static void negatives(int type) {
    failure(type, -1, NegativeArraySizeException.class);
    failure(type, Integer.MIN_VALUE, NegativeArraySizeException.class);
  }

  private static void arrays(int type, int length) {
    Object first = allocate(type, length);
    Object second = allocate(type, length);
    check(first != second, "detached allocation identities, including zero");
    check(first.getClass() == TYPES[type] && second.getClass() == TYPES[type],
        "exact array component types");
    check(Array.getLength(first) == length && Array.getLength(second) == length,
        "exact allocation lengths");
    for (int i = 0; i < length; i++) {
      Object value = Array.get(first, i);
      check(type == 0 ? value == null : ((Number) value).longValue() == 0L,
          "zero-initialized allocation");
    }
    if (length != 0) {
      if (type == 0) ((Object[]) first)[length - 1] = new Object();
      else if (type == 1) ((byte[]) first)[length - 1] = Byte.MIN_VALUE;
      else if (type == 2) ((int[]) first)[length - 1] = Integer.MIN_VALUE;
      else ((long[]) first)[length - 1] = Long.MIN_VALUE;
      Object untouched = Array.get(second, length - 1);
      check(type == 0 ? untouched == null : ((Number) untouched).longValue() == 0L,
          "detached writes");
    }
  }

  private static void allArrays() {
    for (int type = 0; type < 4; type++) {
      negatives(type);
      arrays(type, 0);
      arrays(type, 17);
    }
    check(M3ArrayNative.isLoaded(), "successful Required allocation sets existing loaded flag");
  }

  private static void utf16(boolean loaded) {
    check(M3ArrayNative.isLoaded() == loaded, "expected optional UTF16 provider state");
    char[] left = {'a', 0, (char) 0xd800, 'z'};
    char[] right = {'a', 0, (char) 0xd800, 'y'};
    check(M3ArrayNative.compareUtf16(left, 0, right, 0, 3) == 0,
        "existing UTF16 equal region");
    check(M3ArrayNative.compareUtf16(left, 3, right, 3, 1) == 1,
        "existing UTF16 comparison");
    char[] joined = M3ArrayNative.concatenateUtf16(left, new char[0], right);
    check(joined.length == 8
            && Arrays.equals(Arrays.copyOfRange(joined, 0, 4), left)
            && Arrays.equals(Arrays.copyOfRange(joined, 4, 8), right),
        "existing UTF16 concatenation");
    joined[0] = 'x';
    check(left[0] == 'a', "existing UTF16 detached result");
    check(M3ArrayNative.isLoaded() == loaded, "optional UTF16 does not initiate load");
  }

  /** Permit unrelated harness work; intercept only this property and this exact library. */
  private static final class AccessGate extends SecurityManager implements AutoCloseable {
    private final String expectedLibrary;
    private final AtomicInteger reads = new AtomicInteger();
    private final AtomicInteger links = new AtomicInteger();
    private final SecurityException propertyFailure =
        new SecurityException("configured-array property marker");
    private final SecurityException libraryFailure =
        new SecurityException("configured-array library marker");
    private volatile boolean denyProperty;
    private volatile boolean denyLibrary;

    private AccessGate(String library) {
      expectedLibrary = Path.of(library).toAbsolutePath().normalize().toString();
      check(System.getSecurityManager() == null, "fresh JVM has no inherited SecurityManager");
      System.setSecurityManager(this);
    }

    @Override public void checkPermission(Permission permission) {}
    @Override public void checkPermission(Permission permission, Object context) {}

    @Override public void checkPropertyAccess(String key) {
      if (PROPERTY.equals(key)) {
        reads.incrementAndGet();
        if (denyProperty) throw propertyFailure;
      }
    }

    @Override public void checkLink(String library) {
      if (expectedLibrary.equals(library)) {
        links.incrementAndGet();
        if (denyLibrary) throw libraryFailure;
      }
    }

    @Override public void close() {
      System.setSecurityManager(null);
    }
  }

  private static Thread start(String name, Action action,
      AtomicReference<Throwable> failures) {
    Thread worker = new Thread(() -> {
      try {
        action.run();
      } catch (Throwable error) {
        failures.compareAndSet(null, error);
      }
    }, name);
    worker.setDaemon(true);
    worker.start();
    return worker;
  }

  private static void healthy(AtomicReference<Throwable> failures) {
    Throwable error = failures.get();
    if (error != null) throw new AssertionError("startup worker failed", error);
  }

  private static void join(Thread[] workers, AtomicReference<Throwable> failures)
      throws InterruptedException {
    long deadline = System.nanoTime() + WAIT_NANOS;
    for (Thread worker : workers) {
      while (worker.isAlive()) {
        healthy(failures);
        long remaining = deadline - System.nanoTime();
        check(remaining > 0, "bounded worker completion: " + worker.getName());
        worker.join(Math.max(1L, Math.min(100L,
            TimeUnit.NANOSECONDS.toMillis(remaining))));
      }
    }
    healthy(failures);
  }

  private static Thread[] fourWorkers(String label, Action[] actions,
      AtomicReference<Throwable> failures) {
    check(actions.length == 4, "four array-kind actions");
    Thread[] workers = new Thread[4];
    for (int type = 0; type < 4; type++) {
      workers[type] = start(label + "-" + type, actions[type], failures);
    }
    return workers;
  }

  private static Action[] allocationActions(boolean negative) {
    Action[] actions = new Action[4];
    for (int type = 0; type < 4; type++) {
      final int kind = type;
      actions[type] = negative ? () -> negatives(kind) : () -> arrays(kind, 17);
    }
    return actions;
  }

  private static void blockedOnOwner(Thread[] workers,
      AtomicReference<Throwable> failures) {
    var threads = ManagementFactory.getThreadMXBean();
    long owner = Thread.currentThread().threadId();
    int monitor = System.identityHashCode(M3ArrayNative.class);
    long deadline = System.nanoTime() + WAIT_NANOS;
    for (Thread worker : workers) {
      for (;;) {
        healthy(failures);
        ThreadInfo info = threads.getThreadInfo(worker.threadId(), 12);
        boolean inHelper = info != null && Arrays.stream(info.getStackTrace())
            .anyMatch(frame -> frame.getClassName().equals(M3ArrayNative.class.getName())
                && frame.getMethodName().equals("requireAllocationLengthAndLibrary"));
        if (info != null && info.getThreadState() == Thread.State.BLOCKED
            && info.getLockInfo() != null
            && info.getLockInfo().getClassName().equals("java.lang.Class")
            && info.getLockInfo().getIdentityHashCode() == monitor
            && info.getLockOwnerId() == owner && inHelper) break;
        check(worker.isAlive(), "worker must wait for existing owner monitor");
        check(deadline - System.nanoTime() > 0,
            "bounded exact owner-monitor observation: " + worker.getName());
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
      }
    }
  }

  private static void unconfigured(String mode, String library) {
    if (mode.equals("absent")) System.clearProperty(PROPERTY);
    else System.setProperty(PROPERTY, mode.equals("empty") ? "" : " \t\r\n");
    try (AccessGate gate = new AccessGate(library)) {
      for (int type = 0; type < 4; type++) {
        negatives(type);
        for (int length : new int[] {0, 1, 257}) {
          Throwable error = failure(type, length, UnsatisfiedLinkError.class);
          check(UNLOADED.equals(error.getMessage()), "exact missing/blank ULE message");
        }
      }
      check(gate.reads.get() == 12, "each unconfigured positive call reads configuration");
      check(gate.links.get() == 0, "missing/blank never attempts library load");
      check(!M3ArrayNative.isLoaded(), "missing/blank remains unloaded");
      utf16(false);
    }
  }

  private static void first(int type, String library) {
    System.setProperty(PROPERTY, library);
    utf16(false); // Merely setting the property does not load optional UTF16.
    try (AccessGate gate = new AccessGate(library)) {
      for (int kind = 0; kind < 4; kind++) negatives(kind);
      check(gate.reads.get() == 0 && gate.links.get() == 0,
          "negatives precede configured startup");
      arrays(type, 0);
      allArrays();
      check(gate.reads.get() == 1 && gate.links.get() == 1,
          "one successful configured startup then loaded hot path");
      utf16(true);
    }
  }

  private static void negativeConfiguration(String library) throws Exception {
    String malformed = "bad" + (char) 0 + "path";
    System.setProperty(PROPERTY, malformed);
    try (AccessGate gate = new AccessGate(library)) {
      for (boolean denied : new boolean[] {false, true}) {
        gate.denyProperty = denied;
        for (int type = 0; type < 4; type++) negatives(type);
        check(gate.reads.get() == 0 && gate.links.get() == 0,
            "negative-first before malformed or denied configuration");
      }
      AtomicReference<Throwable> failures = new AtomicReference<>();
      synchronized (M3ArrayNative.class) {
        join(fourWorkers("negative-with-owner-held", allocationActions(true), failures),
            failures);
      }
      check(gate.reads.get() == 0 && gate.links.get() == 0,
          "negative calls avoid existing class monitor and configuration");
      Throwable error = failure(0, 0, SecurityException.class);
      check(error == gate.propertyFailure, "exact denied property throwable identity");
      check(!M3ArrayNative.isLoaded(), "property denial does not mark loaded");
    }
  }

  private static void deniedPropertyRetry(String library) throws Exception {
    System.setProperty(PROPERTY, library);
    try (AccessGate gate = new AccessGate(library)) {
      gate.denyProperty = true;
      for (int type = 0; type < 4; type++) {
        check(failure(type, 0, SecurityException.class) == gate.propertyFailure,
            "property failure propagated unchanged for every type");
      }
      check(gate.reads.get() == 4 && gate.links.get() == 0,
          "denied configuration never reaches existing loader");
      check(!M3ArrayNative.isLoaded(), "property denial leaves unloaded");
      gate.denyProperty = false;
      allArrays();
      check(gate.reads.get() == 5 && gate.links.get() == 1,
          "direct Required retry succeeds without touching collections");
      loadedWithoutConfiguration(gate);
    }
  }

  private static void deniedLibraryRetry(String library) throws Exception {
    System.setProperty(PROPERTY, library);
    try (AccessGate gate = new AccessGate(library)) {
      gate.denyLibrary = true;
      check(failure(0, 0, SecurityException.class) == gate.libraryFailure,
          "exact existing-loader access failure identity");
      check(!M3ArrayNative.isLoaded(), "failed System.load leaves loaded false");
      check(gate.reads.get() == 1 && gate.links.get() == 1, "one denied load attempt");
      gate.denyLibrary = false;
      allArrays();
      check(gate.reads.get() == 2 && gate.links.get() == 2,
          "library failure does not cache unavailability");
      loadedWithoutConfiguration(gate);
    }
  }

  private static void retry(String mode, String library, String extra) {
    String bad = mode.equals("malformed-retry")
        ? "bad" + (char) 0 + "path"
        : extra == null ? library + ".configured-startup-nonexistent" : extra;
    if (mode.equals("nonexistent-retry")) {
      check(!Files.exists(Path.of(bad)), "fixture path must actually be nonexistent");
    }
    System.setProperty(PROPERTY, bad);
    for (int type = 0; type < 4; type++) {
      negatives(type);
      failure(type, 0, mode.equals("malformed-retry")
          ? InvalidPathException.class : UnsatisfiedLinkError.class);
      check(!M3ArrayNative.isLoaded(), "failed path startup leaves unloaded");
    }
    System.setProperty(PROPERTY, library);
    allArrays();
    utf16(true);
  }

  private static void untrimmed(String library) {
    // With an absolute Windows DLL path, the leading space also invalidates the drive colon.
    String raw = " " + library;
    Class<? extends Throwable> expected = UnsatisfiedLinkError.class;
    try {
      check(!Files.exists(Path.of(raw)), "untrimmed fixture must not name another real file");
    } catch (InvalidPathException error) {
      expected = InvalidPathException.class;
    }
    System.setProperty(PROPERTY, raw);
    for (int type = 0; type < 4; type++) failure(type, 0, expected);
    check(!M3ArrayNative.isLoaded(), "a nonblank configured path is not trimmed");
    System.setProperty(PROPERTY, library);
    allArrays();
  }

  private static void loadedWithoutConfiguration(AccessGate gate) throws Exception {
    int reads = gate.reads.get();
    int links = gate.links.get();
    gate.denyProperty = true;
    gate.denyLibrary = true;
    System.setProperty(PROPERTY, "bad" + (char) 0 + "path");
    AtomicReference<Throwable> failures = new AtomicReference<>();
    synchronized (M3ArrayNative.class) {
      join(fourWorkers("loaded-with-owner-held", allocationActions(false), failures),
          failures);
    }
    allArrays();
    check(gate.reads.get() == reads && gate.links.get() == links,
        "loaded positives bypass class monitor, configuration and loader");
    utf16(true);
  }

  private static void loaded(String library) throws Exception {
    M3ArrayNative.load(Path.of(library));
    try (AccessGate gate = new AccessGate(library)) {
      loadedWithoutConfiguration(gate);
    }
  }

  private static void concurrent(String library) throws Exception {
    System.setProperty(PROPERTY, library);
    try (AccessGate gate = new AccessGate(library)) {
      int count = 12;
      Object[] results = new Object[count];
      CountDownLatch ready = new CountDownLatch(count);
      CountDownLatch begin = new CountDownLatch(1);
      AtomicReference<Throwable> failures = new AtomicReference<>();
      Thread[] workers = new Thread[count];
      for (int index = 0; index < count; index++) {
        final int slot = index;
        workers[index] = start("configured-first-" + index, () -> {
          ready.countDown();
          check(begin.await(10, TimeUnit.SECONDS), "bounded first-use release");
          results[slot] = allocate(slot % 4, 17);
        }, failures);
      }
      try {
        check(ready.await(10, TimeUnit.SECONDS), "all first-use callers ready");
      } finally {
        begin.countDown();
      }
      join(workers, failures);
      IdentityHashMap<Object, Boolean> identities = new IdentityHashMap<>();
      for (int index = 0; index < count; index++) {
        Object array = results[index];
        check(array != null && array.getClass() == TYPES[index % 4],
            "concurrent exact component type");
        check(Array.getLength(array) == 17, "concurrent exact length");
        check(identities.put(array, Boolean.TRUE) == null,
            "concurrent detached identities");
        for (int offset = 0; offset < 17; offset++) {
          Object value = Array.get(array, offset);
          check(index % 4 == 0 ? value == null : ((Number) value).longValue() == 0L,
              "concurrent zero initialization");
        }
      }
      check(gate.reads.get() == 1 && gate.links.get() == 1,
          "class-monitor recheck permits exactly one successful startup");
      check(M3ArrayNative.isLoaded(), "concurrent first-use provider loaded");
    }
  }

  private static void priority(String library) throws Exception {
    System.setProperty(PROPERTY, "bad" + (char) 0 + "path");
    try (AccessGate gate = new AccessGate(library)) {
      gate.denyProperty = true;
      AtomicReference<Throwable> failures = new AtomicReference<>();
      Thread[] workers;
      synchronized (M3ArrayNative.class) {
          workers = fourWorkers("required-awaiting-explicit-load",
              allocationActions(false), failures);
          blockedOnOwner(workers, failures);
          check(gate.reads.get() == 0 && gate.links.get() == 0,
              "Required startup has not read config before acquiring class monitor");
          M3ArrayNative.load(Path.of(library)); // Existing reentrant synchronized owner.
          check(M3ArrayNative.isLoaded(), "explicit load succeeds while monitor held");
          gate.denyLibrary = true;
      }
      join(workers, failures);
      check(gate.reads.get() == 0 && gate.links.get() == 1,
          "waiting Required callers recheck successful explicit load first");
      loadedWithoutConfiguration(gate);
    }
  }

  private static void legacy(String library, String legacyLibrary) {
    check(legacyLibrary != null, "legacy mode requires existing legacy DLL argument");
    check(Files.isRegularFile(Path.of(legacyLibrary)), "existing legacy DLL must exist");
    System.setProperty(PROPERTY, legacyLibrary);
    try (AccessGate gate = new AccessGate(legacyLibrary)) {
      for (int type = 0; type < 4; type++) {
        negatives(type);
        Throwable error = failure(type, 0, UnsatisfiedLinkError.class);
        check(!UNLOADED.equals(error.getMessage()), "ordinary missing JNI symbol failure");
        check(M3ArrayNative.isLoaded(), "legacy library loaded despite missing allocator ABI");
      }
      check(gate.reads.get() == 1 && gate.links.get() == 1,
          "legacy DLL selected once by existing owner");
      gate.denyProperty = true;
      gate.denyLibrary = true;
      System.setProperty(PROPERTY, library);
      M3ArrayNative.load(Path.of(library)); // Loaded owner keeps its original provider.
      for (int type = 0; type < 4; type++) {
        Throwable error = failure(type, 1, UnsatisfiedLinkError.class);
        check(!UNLOADED.equals(error.getMessage()), "later config cannot repair selected ABI");
      }
      check(gate.reads.get() == 1 && gate.links.get() == 1,
          "no second library selection after legacy success");
      utf16(true);
    }
  }

  public static void main(String[] args) throws Exception {
    check(args.length >= 4 && args.length <= 5,
        "mode providerDirectory productionDll expectedModule [extra]");
    String mode = args[0];
    String library = args[2];
    String extra = args.length == 5 ? args[4] : null;
    Path expectedProvider = Path.of(args[1]).toRealPath();
    check(Files.isDirectory(expectedProvider), "actual complete-provider directory");
    check(Files.isRegularFile(Path.of(library)), "existing production DLL required");
    check(M3ArrayNative.class.getProtectionDomain().getCodeSource() != null,
        "actual bridge source provider available");
    Path provider = Path.of(M3ArrayNative.class.getProtectionDomain().getCodeSource()
        .getLocation().toURI()).toRealPath();
    check(provider.equals(expectedProvider), "exact actual-recipe complete-provider directory");
    Module module = M3ArrayNative.class.getModule();
    check((module.isNamed() ? module.getName() : "UNNAMED").equals(args[3]),
        "exact expected provider module identity");
    check(module.isExported("com.m3.arrays", M3ConfiguredArrayStartupContract.class.getModule()),
        "actual provider exports public array API to probe");
    check(!M3ArrayNative.isLoaded(), "fresh JVM begins with unloaded bridge");
    switch (mode) {
      case "absent", "empty", "blank" -> unconfigured(mode, library);
      case "first-objects" -> first(0, library);
      case "first-bytes" -> first(1, library);
      case "first-ints" -> first(2, library);
      case "first-longs" -> first(3, library);
      case "negative-config" -> negativeConfiguration(library);
      case "denied-property-retry" -> deniedPropertyRetry(library);
      case "denied-library-retry" -> deniedLibraryRetry(library);
      case "malformed-retry", "nonexistent-retry" -> retry(mode, library, extra);
      case "untrimmed" -> untrimmed(library);
      case "loaded-ignores" -> loaded(library);
      case "concurrent-first" -> concurrent(library);
      case "explicit-priority" -> priority(library);
      case "legacy" -> legacy(library, extra);
      default -> throw new AssertionError("unknown fresh-JVM scenario: " + mode);
    }
    System.out.println("M3_CONFIGURED_ARRAY_STARTUP_CONTRACT_PASS mode="
        + mode + " checks=" + CHECKS.get());
  }
}
