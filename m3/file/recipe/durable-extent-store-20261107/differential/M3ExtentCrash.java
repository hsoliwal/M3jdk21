// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package extent.differential;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Crash gate of recipe durable-extent-store-20261107: a writer process is killed (Runtime.halt, no finally block, no
 * shutdown hook) before each filesystem step of one store operation, and once in the middle of each write (half the
 * bytes written); a reopen in another process must see the previous or the new sealed index, never a torn one, read
 * every published key back, pass verifyAll and accept a further put. The steps are counted by a pass-through
 * FileSystemProvider over the default provider (paths wrap the default paths; nothing is buffered, so the bytes on disk
 * at the kill are exactly those a killed process leaves). A second lane kills the writer inside the package-private
 * failpoint (after the new index is written and forced, before its rename) and a third proves the writer lock across
 * processes. Reflection only, so one harness drives the donor and the relocated classes (store and extent class names
 * are arguments). Prints CRASH PASS|FAIL.
 *
 * <p>usage: M3ExtentCrash parent STORE_CLASS EXTENT_CLASS WORK_DIR [HALT_STATUS]
 */
//FLLDCIM: CRASH - kill-at-every-step gate over the atomic-rename publication; recipe m3/file/recipe/durable-extent-store-20261107
public final class M3ExtentCrash {
  static final int HALT = 77;
  static final String SOURCE = "3".repeat(64);
  static final byte[] OLD = "old payload of a".getBytes(StandardCharsets.UTF_8);
  static final byte[] NEW = "the superseding payload of a".getBytes(StandardCharsets.UTF_8);
  static final byte[] B = "a new key b".getBytes(StandardCharsets.UTF_8);
  static final byte[] POST = "after recovery".getBytes(StandardCharsets.UTF_8);

  private M3ExtentCrash() { }

  // ---- reflection over the store under test ---------------------------------------------------------------------
  static Class<?> store;
  static Class<?> extent;
  static Class<?> hook;
  static Method open;
  static Method openHook;
  static Method fromBytes;

  static void bind(String storeName, String extentName) throws ReflectiveOperationException {
    store = Class.forName(storeName);
    extent = Class.forName(extentName);
    hook = Class.forName(storeName + "$BeforePublish");
    open = store.getMethod("open", Path.class);
    openHook = store.getDeclaredMethod("open", Path.class, hook);
    openHook.setAccessible(true);
    fromBytes = extent.getMethod("fromBytes", String.class, String.class, long.class, byte[].class, String.class);
  }

  static Object invoke(Method method, Object target, Object... arguments) throws Exception {
    try {
      return method.invoke(target, arguments);
    } catch (InvocationTargetException wrapped) {
      Throwable cause = wrapped.getCause();
      if (cause instanceof Exception exception) throw exception;
      throw (Error) cause;
    }
  }

  static Object call(Object target, String name, Object... arguments) throws Exception {
    for (Method method : target.getClass().getMethods()) {
      if (method.getName().equals(name) && method.getParameterCount() == arguments.length) {
        return invoke(method, target, arguments);
      }
    }
    throw new NoSuchMethodException(name);
  }

  static Object extentOf(byte[] bytes) throws Exception {
    return invoke(fromBytes, null, "crash-source", SOURCE, 0L, bytes, "raw");
  }

  static Object openStore(Path root) throws Exception {
    return invoke(open, null, root);
  }

  // ---- step counting pass-through filesystem -----------------------------------------------------------------------
  static final class Steps {
    final long haltAt;
    final Path base;
    boolean armed;
    long count;
    final List<String> trace = new ArrayList<>();

    Steps(long haltAt, Path base) {
      this.haltAt = haltAt;
      this.base = base;
    }

    String name(Path path) {
      String text = base.relativize(path.toAbsolutePath()).toString().replace('\\', '/');
      text = text.replaceAll("\\.m3-staged-\\d+\\.blob", ".m3-staged-*.blob");
      text = text.replaceAll("\\.m3-index-\\d+\\.new", ".m3-index-*.new");
      text = text.replaceAll("([0-9a-f]{12})[0-9a-f]{52}\\.bin", "$1.bin");
      return text.isEmpty() ? "." : text;
    }

    /** Counts one step; halts the process before the step when it is the selected one. */
    void step(String what) {
      if (!armed) return;
      count++;
      trace.add(what);
      if (count == haltAt) {
        System.out.flush();
        Runtime.getRuntime().halt(HALT);
      }
    }

    /** A write is two steps: before it, and torn (half the remaining bytes reach the file, then the kill). */
    int write(String what, ByteBuffer source, WriteOp op) throws IOException {
      step("write-begin " + what);
      if (!armed) return op.write(source);
      count++;
      trace.add("write-torn " + what);
      if (count == haltAt) {
        ByteBuffer half = source.slice().limit(source.remaining() / 2);
        while (half.hasRemaining()) op.write(half);
        System.out.flush();
        Runtime.getRuntime().halt(HALT);
      }
      return op.write(source);
    }
  }

  @FunctionalInterface interface WriteOp {
    int write(ByteBuffer source) throws IOException;
  }

  static final class CrashPath implements Path {
    final CrashFs fs;
    final Path delegate;

    CrashPath(CrashFs fs, Path delegate) {
      this.fs = fs;
      this.delegate = delegate;
    }

    Path wrap(Path path) {
      return path == null ? null : new CrashPath(fs, path);
    }

    static Path unwrap(Path path) {
      if (path instanceof CrashPath crash) return crash.delegate;
      throw new ProviderMismatchException();
    }

    @Override public FileSystem getFileSystem() { return fs; }
    @Override public boolean isAbsolute() { return delegate.isAbsolute(); }
    @Override public Path getRoot() { return wrap(delegate.getRoot()); }
    @Override public Path getFileName() { return wrap(delegate.getFileName()); }
    @Override public Path getParent() { return wrap(delegate.getParent()); }
    @Override public int getNameCount() { return delegate.getNameCount(); }
    @Override public Path getName(int index) { return wrap(delegate.getName(index)); }
    @Override public Path subpath(int begin, int end) { return wrap(delegate.subpath(begin, end)); }
    @Override public boolean startsWith(Path other) { return delegate.startsWith(unwrap(other)); }
    @Override public boolean endsWith(Path other) { return delegate.endsWith(unwrap(other)); }
    @Override public Path normalize() { return wrap(delegate.normalize()); }
    @Override public Path resolve(Path other) { return wrap(delegate.resolve(unwrap(other))); }
    @Override public Path resolve(String other) { return wrap(delegate.resolve(other)); }
    @Override public Path relativize(Path other) { return wrap(delegate.relativize(unwrap(other))); }
    @Override public URI toUri() { return delegate.toUri(); }
    @Override public Path toAbsolutePath() { return wrap(delegate.toAbsolutePath()); }
    @Override public Path toRealPath(LinkOption... options) throws IOException {
      return wrap(delegate.toRealPath(options));
    }
    @Override public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events,
        WatchEvent.Modifier... modifiers) {
      throw new UnsupportedOperationException("watch");
    }
    @Override public int compareTo(Path other) { return delegate.compareTo(unwrap(other)); }
    @Override public boolean equals(Object other) {
      return other instanceof CrashPath crash && delegate.equals(crash.delegate);
    }
    @Override public int hashCode() { return delegate.hashCode(); }
    @Override public String toString() { return delegate.toString(); }
  }

  static final class CrashFs extends FileSystem {
    final CrashProvider provider;
    final FileSystem base;

    CrashFs(CrashProvider provider, FileSystem base) {
      this.provider = provider;
      this.base = base;
    }

    Path wrap(Path path) { return new CrashPath(this, path); }

    @Override public FileSystemProvider provider() { return provider; }
    @Override public void close() { throw new UnsupportedOperationException("close"); }
    @Override public boolean isOpen() { return true; }
    @Override public boolean isReadOnly() { return false; }
    @Override public String getSeparator() { return base.getSeparator(); }
    @Override public Iterable<Path> getRootDirectories() {
      List<Path> roots = new ArrayList<>();
      for (Path root : base.getRootDirectories()) roots.add(wrap(root));
      return roots;
    }
    @Override public Iterable<FileStore> getFileStores() { return base.getFileStores(); }
    @Override public Set<String> supportedFileAttributeViews() { return base.supportedFileAttributeViews(); }
    @Override public Path getPath(String first, String... more) { return wrap(base.getPath(first, more)); }
    @Override public PathMatcher getPathMatcher(String pattern) {
      PathMatcher matcher = base.getPathMatcher(pattern);
      return path -> matcher.matches(CrashPath.unwrap(path));
    }
    @Override public UserPrincipalLookupService getUserPrincipalLookupService() {
      return base.getUserPrincipalLookupService();
    }
    @Override public WatchService newWatchService() { throw new UnsupportedOperationException("watch"); }
  }

  static final class CrashProvider extends FileSystemProvider {
    final FileSystemProvider base;
    final Steps steps;
    final CrashFs fs;

    CrashProvider(FileSystem defaultFs, Steps steps) {
      this.base = defaultFs.provider();
      this.steps = steps;
      this.fs = new CrashFs(this, defaultFs);
    }

    Path in(Path path) { return CrashPath.unwrap(path); }

    @Override public String getScheme() { return "crash"; }
    @Override public FileSystem newFileSystem(URI uri, Map<String, ?> env) {
      throw new UnsupportedOperationException("newFileSystem");
    }
    @Override public FileSystem getFileSystem(URI uri) { return fs; }
    @Override public Path getPath(URI uri) { return fs.wrap(base.getPath(uri)); }
    @Override public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
        FileAttribute<?>... attrs) throws IOException {
      steps.step("open " + options.stream().map(Object::toString).sorted().toList() + " " + steps.name(in(path)));
      return base.newByteChannel(in(path), options, attrs);
    }
    @Override public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options,
        FileAttribute<?>... attrs) throws IOException {
      String name = steps.name(in(path));
      steps.step("channel " + options.stream().map(Object::toString).sorted().toList() + " " + name);
      return new CrashChannel(base.newFileChannel(in(path), options, attrs), steps, name);
    }
    @Override public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter)
        throws IOException {
      steps.step("list " + steps.name(in(dir)));
      DirectoryStream<Path> listing = base.newDirectoryStream(in(dir), entry -> filter.accept(fs.wrap(entry)));
      return new DirectoryStream<>() {
        @Override public Iterator<Path> iterator() {
          Iterator<Path> raw = listing.iterator();
          return new Iterator<>() {
            @Override public boolean hasNext() { return raw.hasNext(); }
            @Override public Path next() { return fs.wrap(raw.next()); }
          };
        }
        @Override public void close() throws IOException { listing.close(); }
      };
    }
    @Override public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
      steps.step("mkdir " + steps.name(in(dir)));
      base.createDirectory(in(dir), attrs);
    }
    @Override public void delete(Path path) throws IOException {
      steps.step("delete " + steps.name(in(path)));
      base.delete(in(path));
    }
    @Override public boolean deleteIfExists(Path path) throws IOException {
      steps.step("delete-if-exists " + steps.name(in(path)));
      return base.deleteIfExists(in(path));
    }
    @Override public void copy(Path source, Path target, CopyOption... options) throws IOException {
      steps.step("copy " + steps.name(in(source)) + " -> " + steps.name(in(target)));
      base.copy(in(source), in(target), options);
    }
    @Override public void move(Path source, Path target, CopyOption... options) throws IOException {
      steps.step("move " + steps.name(in(source)) + " -> " + steps.name(in(target)) + " " + List.of(options));
      base.move(in(source), in(target), options);
    }
    @Override public boolean isSameFile(Path path, Path other) throws IOException {
      return base.isSameFile(in(path), in(other));
    }
    @Override public boolean isHidden(Path path) throws IOException { return base.isHidden(in(path)); }
    @Override public FileStore getFileStore(Path path) throws IOException { return base.getFileStore(in(path)); }
    @Override public void checkAccess(Path path, AccessMode... modes) throws IOException {
      steps.step("access " + steps.name(in(path)));
      base.checkAccess(in(path), modes);
    }
    @Override public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type,
        LinkOption... options) {
      return base.getFileAttributeView(in(path), type, options);
    }
    @Override public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type,
        LinkOption... options) throws IOException {
      steps.step("stat " + steps.name(in(path)));
      return base.readAttributes(in(path), type, options);
    }
    @Override public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options)
        throws IOException {
      steps.step("stat " + steps.name(in(path)));
      return base.readAttributes(in(path), attributes, options);
    }
    @Override public void setAttribute(Path path, String attribute, Object value, LinkOption... options)
        throws IOException {
      steps.step("setattr " + steps.name(in(path)));
      base.setAttribute(in(path), attribute, value, options);
    }
  }

  static final class CrashChannel extends FileChannel {
    final FileChannel delegate;
    final Steps steps;
    final String name;

    CrashChannel(FileChannel delegate, Steps steps, String name) {
      this.delegate = delegate;
      this.steps = steps;
      this.name = name;
    }

    @Override public int read(ByteBuffer destination) throws IOException { return delegate.read(destination); }
    @Override public long read(ByteBuffer[] destinations, int offset, int length) throws IOException {
      return delegate.read(destinations, offset, length);
    }
    @Override public int write(ByteBuffer source) throws IOException {
      return steps.write(name, source, delegate::write);
    }
    @Override public long write(ByteBuffer[] sources, int offset, int length) throws IOException {
      steps.step("write-gather " + name);
      return delegate.write(sources, offset, length);
    }
    @Override public long position() throws IOException { return delegate.position(); }
    @Override public FileChannel position(long position) throws IOException {
      delegate.position(position);
      return this;
    }
    @Override public long size() throws IOException { return delegate.size(); }
    @Override public FileChannel truncate(long size) throws IOException {
      steps.step("truncate " + name);
      delegate.truncate(size);
      return this;
    }
    @Override public void force(boolean metaData) throws IOException {
      steps.step("force " + name);
      delegate.force(metaData);
    }
    @Override public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
      return delegate.transferTo(position, count, target);
    }
    @Override public long transferFrom(ReadableByteChannel source, long position, long count) throws IOException {
      steps.step("transfer-from " + name);
      return delegate.transferFrom(source, position, count);
    }
    @Override public int read(ByteBuffer destination, long position) throws IOException {
      return delegate.read(destination, position);
    }
    @Override public int write(ByteBuffer source, long position) throws IOException {
      return steps.write(name, source, buffer -> delegate.write(buffer, position));
    }
    @Override public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
      steps.step("map " + mode + " " + name);
      return delegate.map(mode, position, size);
    }
    @Override public FileLock lock(long position, long size, boolean shared) throws IOException {
      steps.step("lock " + name);
      return delegate.lock(position, size, shared);
    }
    @Override public FileLock tryLock(long position, long size, boolean shared) throws IOException {
      steps.step("try-lock " + name);
      return delegate.tryLock(position, size, shared);
    }
    @Override protected void implCloseChannel() throws IOException {
      steps.step("close " + name);
      delegate.close();
    }
  }

  // ---- scenarios ----------------------------------------------------------------------------------------------------
  /** Child: run one scenario over the counting filesystem, killed at step haltAt; prints the steps when it finishes. */
  static void child(String scenario, Path directory, long haltAt) throws Exception {
    Path base = directory.toAbsolutePath();
    Steps steps = new Steps(haltAt, base);
    CrashProvider provider = new CrashProvider(base.getFileSystem(), steps);
    Path root = provider.fs.wrap(base);
    switch (scenario) {
      case "INIT" -> {
        steps.armed = true;
        Object opened = openStore(root);
        steps.armed = false;
        call(opened, "close");
      }
      case "WRITE", "SUPERSEDE" -> {
        Object opened = openStore(root);
        steps.armed = true;
        if (scenario.equals("WRITE")) call(opened, "write", "b", extentOf(B), B);
        else call(opened, "supersede", "a", extentOf(NEW), NEW);
        steps.armed = false;
        call(opened, "close");
      }
      case "HOOK" -> {
        Object failpoint = Proxy.newProxyInstance(hook.getClassLoader(), new Class<?>[] {hook}, (proxy, method, args) -> {
          if (method.getName().equals("run")) {
            System.out.flush();
            Runtime.getRuntime().halt(HALT);
          }
          throw new UnsupportedOperationException(method.getName());
        });
        Object opened = invoke(openHook, null, base, failpoint);
        call(opened, "supersede", "a", extentOf(NEW), NEW);
        call(opened, "close");
      }
      case "LOCK" -> {
        try {
          call(openStore(base), "close");
          System.out.println("LOCK second writer admitted");
        } catch (IOException refused) {
          System.out.println("LOCK refused " + refused.getClass().getSimpleName() + ": " + refused.getMessage());
        }
        return;
      }
      default -> throw new IllegalArgumentException(scenario);
    }
    System.out.println("COMPLETED steps=" + steps.count);
    for (int i = 0; i < steps.trace.size(); i++) System.out.println("STEP " + (i + 1) + " " + steps.trace.get(i));
  }

  static String sha256(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  static String indexSha(Path root) throws Exception {
    Path index = root.resolve("index.m3");
    return Files.exists(index) ? sha256(Files.readAllBytes(index)) : "absent";
  }

  static void copyTree(Path from, Path to) throws IOException {
    try (Stream<Path> walk = Files.walk(from)) {
      for (Path path : walk.toList()) {
        Path target = to.resolve(from.relativize(path).toString());
        if (Files.isDirectory(path)) Files.createDirectories(target);
        else Files.copy(path, target);
      }
    }
  }

  static long count(Path root, String prefix) throws IOException {
    long found = 0;
    for (Path dir : List.of(root, root.resolve("blobs"))) {
      if (!Files.isDirectory(dir)) continue;
      try (Stream<Path> listing = Files.list(dir)) {
        found += listing.filter(path -> path.getFileName().toString().startsWith(prefix)).count();
      }
    }
    return found;
  }

  static long blobs(Path root) throws IOException {
    if (!Files.isDirectory(root.resolve("blobs"))) return 0;
    try (Stream<Path> listing = Files.list(root.resolve("blobs"))) {
      return listing.filter(path -> path.getFileName().toString().endsWith(".bin")).count();
    }
  }

  static final class Child {
    final List<String> lines;
    final int status;

    Child(List<String> lines, int status) {
      this.lines = lines;
      this.status = status;
    }
  }

  static Child launch(String... arguments) throws Exception {
    List<String> command = new ArrayList<>();
    command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    command.add("-Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir"));
    command.add("-cp");
    command.add(System.getProperty("java.class.path"));
    command.add(M3ExtentCrash.class.getName());
    command.addAll(List.of(arguments));
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    byte[] output = process.getInputStream().readAllBytes();
    int status = process.waitFor();
    return new Child(List.of(new String(output, StandardCharsets.UTF_8).split("\\R")), status);
  }

  /** Reopen a crashed directory in this process and classify it against the old and the new index. */
  static String classify(String scenario, Path dir, String oldSha, String newSha) throws Exception {
    String before = indexSha(dir);
    String outcome = before.equals(oldSha) ? "OLD" : before.equals(newSha) ? "NEW" : "TORN";
    long tmpIndex = count(dir, ".m3-index-");
    long tmpBlob = count(dir, ".m3-staged-");
    long blobCount = blobs(dir);
    StringBuilder line = new StringBuilder(" outcome=").append(outcome);
    Object reopened;
    try {
      reopened = openStore(dir);
    } catch (Exception refused) {
      return line.append(" reopen=REFUSED ").append(refused.getClass().getSimpleName()).append(": ")
          .append(refused.getMessage()).toString();
    }
    try {
      @SuppressWarnings("unchecked")
      List<String> keys = (List<String>) call(reopened, "keys", "", 0);
      line.append(" keys=").append(keys);
      String expected = switch (scenario) {
        case "INIT" -> "[]";
        case "WRITE" -> outcome.equals("NEW") ? "[a, b]" : "[a]";
        default -> "[a]";
      };
      if (!keys.toString().equals(expected)) line.append(" KEYS_MISMATCH");
      if (!scenario.equals("INIT")) {
        byte[] a = (byte[]) call(reopened, "read", "a");
        byte[] wantA = scenario.equals("SUPERSEDE") && outcome.equals("NEW") ? NEW : OLD;
        line.append(" a=").append(java.util.Arrays.equals(a, wantA) ? (wantA == NEW ? "new" : "old") : "WRONG");
        if (keys.contains("b")) {
          line.append(" b=").append(java.util.Arrays.equals((byte[]) call(reopened, "read", "b"), B) ? "ok" : "WRONG");
        }
      }
      call(reopened, "verifyAll");
      call(reopened, "write", "post", extentOf(POST), POST);
      line.append(" post=").append(java.util.Arrays.equals((byte[]) call(reopened, "read", "post"), POST) ? "ok" : "WRONG");
    } finally {
      call(reopened, "close");
    }
    return line.append(" tmpIndex=").append(tmpIndex).append(" tmpBlob=").append(tmpBlob).append(" blobs=")
        .append(blobCount).toString();
  }

  static void parent(Path work) throws Exception {
    Files.createDirectories(work);
    int kills = 0;
    int olds = 0;
    int news = 0;
    int bad = 0;
    for (String scenario : List.of("INIT", "WRITE", "SUPERSEDE")) {
      Path base = work.resolve(scenario.toLowerCase() + "-base");
      Files.createDirectories(base);
      if (!scenario.equals("INIT")) {
        Object seeded = openStore(base);
        call(seeded, "write", "a", extentOf(OLD), OLD);
        call(seeded, "close");
      }
      String oldSha = indexSha(base);
      Path dry = work.resolve(scenario.toLowerCase() + "-complete");
      copyTree(base, dry);
      Child complete = launch("child", store.getName(), extent.getName(), scenario, dry.toString(),
          Long.toString(Long.MAX_VALUE));
      List<String> stepNames = new ArrayList<>();
      long steps = -1;
      for (String line : complete.lines) {
        if (line.startsWith("COMPLETED steps=")) steps = Long.parseLong(line.substring("COMPLETED steps=".length()));
        else if (line.startsWith("STEP ")) stepNames.add(line.substring(line.indexOf(' ', 5) + 1));
      }
      if (complete.status != 0 || steps < 1 || stepNames.size() != steps) {
        System.out.println(scenario + " DRY_RUN_FAILED status=" + complete.status + " " + complete.lines);
        bad++;
        continue;
      }
      String newSha = indexSha(dry);
      System.out.println(scenario + " steps=" + steps + " old=" + oldSha.substring(0, Math.min(12, oldSha.length()))
          + " new=" + newSha.substring(0, 12));
      for (long k = 1; k <= steps; k++) {
        Path dir = work.resolve(scenario.toLowerCase() + "-k" + k);
        copyTree(base, dir);
        Child killed = launch("child", store.getName(), extent.getName(), scenario, dir.toString(), Long.toString(k));
        String step = stepNames.get((int) k - 1);
        if (killed.status != HALT) {
          System.out.println(scenario + " k=" + k + " NOT_KILLED status=" + killed.status + " " + killed.lines);
          bad++;
          continue;
        }
        kills++;
        String verdict = classify(scenario, dir, oldSha, newSha);
        if (verdict.contains("TORN") || verdict.contains("REFUSED") || verdict.contains("WRONG")
            || verdict.contains("MISMATCH")) bad++;
        else if (verdict.contains("outcome=OLD")) olds++;
        else news++;
        System.out.println(scenario + " k=" + k + " " + step + verdict);
      }
    }
    Path hookDir = work.resolve("hook");
    Files.createDirectories(hookDir);
    Object seeded = openStore(hookDir);
    call(seeded, "write", "a", extentOf(OLD), OLD);
    call(seeded, "close");
    String hookOld = indexSha(hookDir);
    Child hooked = launch("child", store.getName(), extent.getName(), "HOOK", hookDir.toString(), "0");
    if (hooked.status != HALT) bad++;
    else kills++;
    String hookVerdict = classify("SUPERSEDE", hookDir, hookOld, "-");
    if (!hookVerdict.contains("outcome=OLD") || hookVerdict.contains("WRONG")) bad++;
    else olds++;
    System.out.println("HOOK killed-between-forced-index-and-rename status=" + hooked.status + hookVerdict);
    Path lockDir = work.resolve("lock");
    Object holder = openStore(lockDir);
    Child locked = launch("child", store.getName(), extent.getName(), "LOCK", lockDir.toString(), "0");
    call(holder, "close");
    String lockLine = String.join(" ", locked.lines).trim();
    if (!lockLine.startsWith("LOCK refused")) bad++;
    System.out.println("LOCK cross-process " + lockLine);
    System.out.println("CRASH " + (bad == 0 ? "PASS" : "FAIL") + " kills=" + kills + " old=" + olds + " new=" + news
        + " bad=" + bad);
  }

  public static void main(String[] arguments) throws Exception {
    bind(arguments[1], arguments[2]);
    if (arguments[0].equals("child")) {
      child(arguments[3], Path.of(arguments[4]), Long.parseLong(arguments[5]));
    } else {
      parent(Path.of(arguments[3]));
    }
  }
}
