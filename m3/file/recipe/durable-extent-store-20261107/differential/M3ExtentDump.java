// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package extent.differential;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Differential dump of recipe durable-extent-store-20261107: drives an extent class and a store class (names are
 * arguments, reflection only, so the donor and the relocated classes run the same driver) over seeded identity cases,
 * seeded append / supersede / read / metadata / keys / manifest / verifyAll / close-reopen sequences, and the refusals
 * (corrupt and truncated index, sealed but noncanonical index rows, truncated, extended, altered and missing blobs,
 * exact-length and digest mismatches on write, a failed publication, a second writer, a closed store). Every line is
 * either an OP line (results, exception simple names and messages, index and blob digests) or an ID line (values in
 * the identity domain: extent ids, source ids, manifest roots, receipts). Paths are printed relative to the work
 * directory. A tampered blob is always a blob this JVM never mapped (Windows refuses to rewrite a mapped file).
 *
 * <p>usage: M3ExtentDump STORE_CLASS EXTENT_CLASS WORK_DIR [SEQUENCES OPS IDENTITIES]
 */
//FLLDCIM: DIFFERENTIAL - donor vs relocated dump over append/publish/reopen/read and refusals; recipe m3/file/recipe/durable-extent-store-20261107
public final class M3ExtentDump {
  static final HexFormat HEX = HexFormat.of();
  static final String[] KEYS = {"a", "b", "ns/a", "ns/b", "ns/z", "k\tt", "k\nn", "\u00e9t\u00e9", "\ud83d\ude00",
      "a/b/c", "zz", "\u0001ctl", "q\"uote", "long-" + "x".repeat(200), "ns/", "n", "nsa", "A", "Z", "0", "~",
      "ns/\u00e9", "dup", "x", "y", "key with space", "k=v", "\\", "/", "ns/a/b"};
  static final String[] PREFIXES = {"", "ns/", "a", "k", "ns/a", "zz", "\u00e9", "none", "~"};
  static final String[] SOURCES = {"src", "with\nnewline", "q\"uote", "back\\slash", "tab\tbed", "\u0001\u001f",
      "\u00e9", "\ud83d\ude00 emoji", "x".repeat(300)};
  static final String[] KINDS = {"raw", "text", "bytes", "a b", "\u00e9", "k\"q"};

  static Class<?> store;
  static Class<?> extent;
  static Class<?> hook;
  static Path work;
  static final StringBuilder out = new StringBuilder();

  private M3ExtentDump() { }

  static void op(String line) { out.append("OP ").append(line).append('\n'); }
  static void id(String line) { out.append("ID ").append(line).append('\n'); }

  static Object invoke(Method method, Object target, Object... arguments) throws Throwable {
    try {
      return method.invoke(target, arguments);
    } catch (InvocationTargetException wrapped) {
      throw wrapped.getCause();
    }
  }

  static Method method(Class<?> owner, String name, int count) throws NoSuchMethodException {
    for (Method method : owner.getMethods()) {
      if (method.getName().equals(name) && method.getParameterCount() == count) return method;
    }
    throw new NoSuchMethodException(owner.getSimpleName() + "." + name + "/" + count);
  }

  static Object construct(Object... arguments) throws Throwable {
    try {
      return extent.getConstructor(String.class, String.class, long.class, long.class, String.class, String.class)
          .newInstance(arguments);
    } catch (InvocationTargetException wrapped) {
      throw wrapped.getCause();
    }
  }

  static Object call(Object target, String name, Object... arguments) throws Throwable {
    return invoke(method(target.getClass(), name, arguments.length), target, arguments);
  }

  static Object stat(Class<?> owner, String name, Object... arguments) throws Throwable {
    return invoke(method(owner, name, arguments.length), null, arguments);
  }

  static String clean(String text) {
    if (text == null) return "null";
    String base = work.toAbsolutePath().toString();
    return text.replace(base, "<work>").replace('\\', '/').replace("\n", "\\n").replace("\t", "\\t");
  }

  static String failure(Throwable thrown) {
    StringBuilder line = new StringBuilder("THROWS ");
    for (Throwable at = thrown; at != null; at = at.getCause()) {
      if (at != thrown) line.append(" <- ");
      line.append(at.getClass().getSimpleName()).append(": ").append(clean(at.getMessage()));
    }
    return line.toString();
  }

  static String sha(byte[] bytes) throws Exception {
    return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  static String esc(String text) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
  }

  static String fields(Object e) throws Throwable {
    if (e == null) return "null";
    return esc((String) call(e, "sourceId")) + "|" + call(e, "sourceSha256") + "|" + call(e, "byteStart") + ".."
        + call(e, "byteEnd") + "|" + call(e, "byteLength") + "|" + call(e, "contentSha256") + "|"
        + esc((String) call(e, "logicalKind")) + "|h=" + e.hashCode();
  }

  static String sorted(Object map) {
    TreeMap<String, String> sorted = new TreeMap<>();
    ((Map<?, ?>) map).forEach((k, v) -> sorted.put(String.valueOf(k), clean(String.valueOf(v))));
    return sorted.toString();
  }

  static Object fromBytes(String source, String sourceSha, long start, byte[] content, String kind) throws Throwable {
    return stat(extent, "fromBytes", source, sourceSha, start, content, kind);
  }

  static String hex64(Random random) {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return HEX.formatHex(bytes);
  }

  static byte[] bytes(Random random, int max) {
    byte[] bytes = new byte[random.nextInt(max + 1)];
    random.nextBytes(bytes);
    return bytes;
  }

  // ---- part 1: identity ------------------------------------------------------------------------------------------
  static void identity(int count) throws Throwable {
    Random random = new Random(0x5EED1D);
    List<Object> made = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String source = SOURCES[random.nextInt(SOURCES.length)];
      String sourceSha = hex64(random);
      long start = random.nextInt(4) == 0 ? random.nextInt(16) : (random.nextLong() >>> 24);
      byte[] content = bytes(random, 64);
      String kind = KINDS[random.nextInt(KINDS.length)];
      Object e = fromBytes(source, sourceSha, start, content, kind);
      made.add(e);
      op("E " + i + " " + fields(e));
      id("E " + i + " extentId=" + call(e, "extentId") + " schema=" + call(e, "schema"));
      id("E " + i + " receipt=" + sorted(call(e, "receipt")));
      op("E " + i + " evidence=" + sorted(call(e, "evidenceRecord")).replaceAll("unit_id=[0-9a-f]{64}", "unit_id=<id>"));
      id("E " + i + " identity=" + sorted(call(e, "identityPayload")));
      Object copy = construct(call(e, "sourceId"), call(e, "sourceSha256"), call(e, "byteStart"), call(e, "byteEnd"),
          call(e, "contentSha256"), call(e, "logicalKind"));
      op("E " + i + " equalsCopy=" + e.equals(copy) + " sameHash=" + (e.hashCode() == copy.hashCode())
          + " sameId=" + call(e, "extentId").equals(call(copy, "extentId")));
      id("E " + i + " validate=" + sorted(stat(extent, "validateContent", e, content)));
      byte[] shorter = Arrays.copyOf(content, Math.max(0, content.length - 1));
      byte[] longer = Arrays.copyOf(content, content.length + 1);
      byte[] altered = content.clone();
      if (altered.length > 0) altered[random.nextInt(altered.length)] ^= 1;
      for (byte[] probe : List.of(shorter, longer, altered)) {
        if (Arrays.equals(probe, content)) continue;
        try {
          stat(extent, "validateContent", e, probe);
          op("E " + i + " validate-probe ACCEPTED");
        } catch (Throwable refused) {
          op("E " + i + " validate-probe " + failure(refused));
        }
      }
      id("E " + i + " sourceId=" + stat(extent, "contentSourceId", sourceSha, (long) content.length));
    }
    for (int m = 0; m < count / 4; m++) {
      List<Object> chosen = new ArrayList<>();
      int size = random.nextInt(8);
      for (int j = 0; j < size; j++) chosen.add(made.get(random.nextInt(made.size())));
      List<Object> reversed = new ArrayList<>(chosen);
      java.util.Collections.reverse(reversed);
      try {
        Object manifest = stat(extent, "buildManifest", chosen);
        Object back = stat(extent, "buildManifest", reversed);
        id("M " + m + " root=" + call(manifest, "manifestRoot") + " ids=" + call(manifest, "orderedExtentIds"));
        id("M " + m + " receipt=" + sorted(call(manifest, "receipt")));
        op("M " + m + " size=" + ((Collection<?>) call(manifest, "extents")).size() + " orderFree="
            + call(manifest, "root").equals(call(back, "manifestRoot")));
      } catch (Throwable refused) {
        op("M " + m + " " + failure(refused));
      }
    }
    String zero = "0".repeat(64);
    Object[][] invalid = {
        {"", zero, 0L, 0L, zero, "raw"}, {null, zero, 0L, 0L, zero, "raw"}, {"s\0", zero, 0L, 0L, zero, "raw"},
        {"s", zero.toUpperCase().replace('0', 'A'), 0L, 0L, zero, "raw"}, {"s", "0".repeat(63), 0L, 0L, zero, "raw"},
        {"s", zero, -1L, 0L, zero, "raw"}, {"s", zero, 4L, 3L, zero, "raw"}, {"s", zero, 0L, -1L, zero, "raw"},
        {"s", zero, 0L, 0L, "g".repeat(64), "raw"}, {"s", zero, 0L, 0L, zero, ""}, {"s", zero, 0L, 0L, zero, null},
        {"s", zero, 0L, 0L, zero, "k\0"}, {"s", null, 0L, 0L, zero, "raw"}, {"s", zero, 0L, 0L, null, "raw"}};
    for (int i = 0; i < invalid.length; i++) {
      try {
        construct(invalid[i]);
        op("INVALID " + i + " ACCEPTED");
      } catch (Throwable refused) {
        op("INVALID " + i + " " + failure(refused));
      }
    }
    for (Object[] args : new Object[][] {{"s", zero, Long.MAX_VALUE, new byte[1], "raw"},
        {"s", zero, -5L, new byte[0], "raw"}, {"s", zero, 0L, null, "raw"}}) {
      try {
        stat(extent, "fromBytes", args);
        op("FROMBYTES ACCEPTED");
      } catch (Throwable refused) {
        op("FROMBYTES " + failure(refused));
      }
    }
    for (Object[] args : new Object[][] {{zero, -1L}, {"Z".repeat(64), 1L}}) {
      try {
        stat(extent, "contentSourceId", args);
        op("SOURCEID ACCEPTED");
      } catch (Throwable refused) {
        op("SOURCEID " + failure(refused));
      }
    }
    Object a = made.get(0);
    try {
      stat(extent, "buildManifest", List.of(a, a));
      op("DUPLICATE ACCEPTED");
    } catch (Throwable refused) {
      op("DUPLICATE " + failure(refused));
    }
  }

  // ---- part 2: store sequences -----------------------------------------------------------------------------------
  static Object open(Path root) throws Throwable {
    return stat(store, "open", root);
  }

  static Object openFailing(Path root) throws Throwable {
    Method failpoint = store.getDeclaredMethod("open", Path.class, hook);
    failpoint.setAccessible(true);
    Object injected = Proxy.newProxyInstance(hook.getClassLoader(), new Class<?>[] {hook}, (proxy, method, args) -> {
      if (method.getName().equals("run")) throw new IOException("injected failure before the index rename");
      throw new UnsupportedOperationException(method.getName());
    });
    return invoke(failpoint, null, root, injected);
  }

  static String indexSha(Path root) throws Exception {
    Path index = root.resolve("index.m3");
    return Files.exists(index) ? sha(Files.readAllBytes(index)) : "absent";
  }

  static String blobListing(Path root) throws IOException {
    try (Stream<Path> listing = Files.list(root.resolve("blobs"))) {
      return listing.map(path -> path.getFileName() + ":" + size(path)).sorted().toList().toString();
    }
  }

  static long size(Path path) {
    try {
      return Files.size(path);
    } catch (IOException failure) {
      return -1;
    }
  }

  static String attempt(Throwing action) {
    try {
      Object result = action.run();
      return result == null ? "null" : result instanceof byte[] bytes ? "bytes:" + bytes.length + ":" + sha(bytes)
          : result instanceof List<?> list
              ? list.stream().map(item -> item instanceof String text ? esc(text) : String.valueOf(item)).toList().toString()
          : extent.isInstance(result) ? fields(result) : String.valueOf(result);
    } catch (Throwable refused) {
      return failure(refused);
    }
  }

  @FunctionalInterface interface Throwing {
    Object run() throws Throwable;
  }

  static void sequences(int count, int ops) throws Throwable {
    for (int s = 0; s < count; s++) {
      Random random = new Random(0xE7E7L * (s + 1));
      Path root = work.resolve("seq-" + s);
      String sourceSha = hex64(random);
      List<byte[]> contents = new ArrayList<>();
      for (int c = 0; c < 12; c++) contents.add(bytes(random, c == 0 ? 0 : 2048));
      Object[] holder = {open(root)};
      op("S " + s + " open index=" + indexSha(root));
      for (int o = 0; o < ops; o++) {
        int choice = random.nextInt(100);
        String key = KEYS[random.nextInt(KEYS.length)];
        String tag = "S " + s + " " + o + " ";
        Object st = holder[0];
        if (choice < 45) {
          String which = choice < 30 ? "write" : "supersede";
          byte[] content = random.nextInt(3) == 0 ? contents.get(random.nextInt(contents.size())) : bytes(random, 512);
          long start = random.nextInt(1000);
          Object e = fromBytes(SOURCES[random.nextInt(3)], sourceSha, start, content, KINDS[random.nextInt(3)]);
          byte[] given = content;
          int tamper = random.nextInt(10);
          if (tamper == 0) given = Arrays.copyOf(content, content.length + 1);
          else if (tamper == 1 && content.length > 0) {
            given = content.clone();
            given[0] ^= 0x10;
          }
          byte[] payload = given;
          op(tag + which + " " + esc(key) + " tamper=" + tamper + " -> " + attempt(() -> call(st, which, key, e, payload)));
        } else if (choice < 65) {
          op(tag + "read " + esc(key) + " -> " + attempt(() -> call(st, "read", key)));
        } else if (choice < 72) {
          op(tag + "metadata " + esc(key) + " -> " + attempt(() -> call(st, "metadata", key)));
        } else if (choice < 80) {
          String prefix = PREFIXES[random.nextInt(PREFIXES.length)];
          int limit = random.nextInt(6) - (random.nextInt(20) == 0 ? 7 : 0);
          op(tag + "keys " + esc(prefix) + " " + limit + " -> " + attempt(() -> call(st, "keys", prefix, limit)));
        } else if (choice < 85) {
          try {
            Object manifest = call(st, "manifest");
            op(tag + "manifest size=" + ((Collection<?>) call(manifest, "extents")).size());
            id(tag + "manifest root=" + call(manifest, "manifestRoot"));
          } catch (Throwable refused) {
            op(tag + "manifest " + failure(refused));
          }
        } else if (choice < 89) {
          op(tag + "verifyAll -> " + attempt(() -> { call(st, "verifyAll"); return "OK"; }));
        } else if (choice < 95) {
          call(st, "close");
          holder[0] = open(root);
          op(tag + "reopen index=" + indexSha(root) + " keys=" + attempt(() -> call(holder[0], "keys", "", 0)));
        } else {
          String bad = switch (random.nextInt(4)) {
            case 0 -> "";
            case 1 -> "nul\0key";
            case 2 -> "k".repeat(4097);
            default -> "\u00e9".repeat(2049);
          };
          Object e = fromBytes("s", sourceSha, 0, new byte[0], "raw");
          op(tag + "badkey " + bad.length() + " -> " + attempt(() -> call(st, "write", bad, e, new byte[0])));
        }
      }
      call(holder[0], "close");
      op("S " + s + " closed -> " + attempt(() -> call(holder[0], "read", "a")));
      op("S " + s + " final index=" + indexSha(root) + " blobs=" + blobListing(root));
      Object reopened = open(root);
      op("S " + s + " final keys=" + attempt(() -> call(reopened, "keys", "", 0)));
      op("S " + s + " final verifyAll -> " + attempt(() -> { call(reopened, "verifyAll"); return "OK"; }));
      call(reopened, "close");
    }
  }

  // ---- part 3: refusals ------------------------------------------------------------------------------------------
  static Path seeded(String name, int keys) throws Throwable {
    Path root = work.resolve(name);
    Object st = open(root);
    for (int k = 0; k < keys; k++) {
      byte[] content = ("payload number " + k + " of " + name).getBytes(StandardCharsets.UTF_8);
      call(st, "write", "key-" + k, fromBytes("src", "4".repeat(64), k * 100L, content, "text"), content);
    }
    call(st, "close");
    return root;
  }

  static void openAttempt(String tag, Path root) {
    op(tag + " open -> " + attempt(() -> {
      Object st = open(root);
      Object keys = call(st, "keys", "", 0);
      call(st, "close");
      return keys;
    }));
  }

  static String seal(String body) throws Exception {
    return body + "#sha256:" + sha(body.getBytes(StandardCharsets.UTF_8)) + "\n";
  }

  static String row(String key, String source, String sourceSha, String start, String end, String contentSha,
      String kind) {
    return String.join("\t", key, source, sourceSha, start, end, contentSha, kind) + "\n";
  }

  static void refusals() throws Throwable {
    Path index = seeded("corrupt-index", 3).resolve("index.m3");
    byte[] good = Files.readAllBytes(index);
    op("R index bytes=" + good.length + " sha=" + sha(good));
    int[] positions = {0, 3, 9, 10, 11, 40, good.length / 2, good.length - 70, good.length - 65, good.length - 64,
        good.length - 2, good.length - 1};
    for (int position : positions) {
      byte[] flipped = good.clone();
      flipped[position] ^= 0x01;
      Files.write(index, flipped);
      openAttempt("R flip@" + position, index.getParent());
    }
    for (int length : new int[] {0, 1, 9, 10, 11, good.length / 2, good.length - 66, good.length - 1}) {
      Files.write(index, Arrays.copyOf(good, length));
      openAttempt("R truncate@" + length, index.getParent());
    }
    Files.write(index, Arrays.copyOf(good, good.length + 1));
    openAttempt("R extend+1", index.getParent());
    Files.write(index, good);
    openAttempt("R restored", index.getParent());

    String magic = "M3EXTIDX1\n";
    String zero = "5".repeat(64);
    String content = "6".repeat(64);
    String ok = row(esc("k"), esc("s"), zero, "0", "3", content, esc("raw"));
    Map<String, String> crafted = new TreeMap<>();
    crafted.put("01-empty", seal(magic));
    crafted.put("02-one-row", seal(magic + ok));
    crafted.put("03-six-fields", seal(magic + String.join("\t", esc("k"), esc("s"), zero, "0", "3", content) + "\n"));
    crafted.put("04-eight-fields", seal(magic + ok.replace("\n", "\textra\n")));
    crafted.put("05-padded-base64", seal(magic + row(esc("k") + "==", esc("s"), zero, "0", "3", content, esc("raw"))));
    crafted.put("06-leading-zero", seal(magic + row(esc("k"), esc("s"), zero, "00", "3", content, esc("raw"))));
    crafted.put("07-plus-sign", seal(magic + row(esc("k"), esc("s"), zero, "+0", "3", content, esc("raw"))));
    crafted.put("08-out-of-order", seal(magic + row(esc("b"), esc("s"), zero, "0", "3", content, esc("raw"))
        + row(esc("a"), esc("s"), zero, "0", "3", content, esc("raw"))));
    crafted.put("09-duplicate", seal(magic + ok + ok));
    crafted.put("10-upper-digest", seal(magic + row(esc("k"), esc("s"), zero.toUpperCase().replace('5', 'A'), "0",
        "3", content, esc("raw"))));
    crafted.put("11-end-before-start", seal(magic + row(esc("k"), esc("s"), zero, "4", "3", content, esc("raw"))));
    crafted.put("12-empty-key", seal(magic + row("", esc("s"), zero, "0", "3", content, esc("raw"))));
    crafted.put("13-nul-key", seal(magic + row(esc("k\0"), esc("s"), zero, "0", "3", content, esc("raw"))));
    crafted.put("14-too-large", seal(magic + row(esc("k"), esc("s"), zero, "0", "2147483648", content, esc("raw"))));
    crafted.put("15-negative", seal(magic + row(esc("k"), esc("s"), zero, "-1", "3", content, esc("raw"))));
    crafted.put("16-unterminated", magic + ok.trim() + "#sha256:" + sha((magic + ok.trim()).getBytes(StandardCharsets.UTF_8)) + "\n");
    crafted.put("17-no-magic", seal(ok));
    crafted.put("18-trailing", seal(magic + ok) + "x");
    crafted.put("19-bad-base64", seal(magic + row("*", esc("s"), zero, "0", "3", content, esc("raw"))));
    crafted.put("20-overflow", seal(magic + row(esc("k"), esc("s"), zero, "0", "99999999999999999999", content,
        esc("raw"))));
    crafted.put("21-empty-kind", seal(magic + row(esc("k"), esc("s"), zero, "0", "3", content, "")));
    crafted.put("22-long-key", seal(magic + row(esc("k".repeat(4097)), esc("s"), zero, "0", "3", content, esc("raw"))));
    crafted.put("23-bad-seal", magic + ok + "#sha256:" + "0".repeat(64) + "\n");
    crafted.put("24-short-seal", magic + ok + "#sha256:" + "0".repeat(63) + "\n");
    for (Map.Entry<String, String> entry : crafted.entrySet()) {
      Path root = work.resolve("crafted-" + entry.getKey());
      Files.createDirectories(root.resolve("blobs"));
      Files.write(root.resolve("index.m3"), entry.getValue().getBytes(StandardCharsets.UTF_8));
      openAttempt("R crafted " + entry.getKey(), root);
    }
    Path invalidUtf8 = work.resolve("crafted-25-invalid-utf8");
    Files.createDirectories(invalidUtf8.resolve("blobs"));
    byte[] body = (magic + ok).getBytes(StandardCharsets.UTF_8);
    body[body.length - 3] = (byte) 0xC3;
    Files.write(invalidUtf8.resolve("index.m3"), body);
    openAttempt("R crafted 25-invalid-utf8", invalidUtf8);

    String[] tampers = {"truncate", "extend", "alter-first", "alter-last", "delete", "empty", "directory"};
    for (String tamper : tampers) {
      Path root = seeded("blob-" + tamper, 2);
      Object e;
      Object st = open(root);
      e = call(st, "metadata", "key-0");
      call(st, "close");
      Path blob = root.resolve("blobs").resolve(call(e, "contentSha256") + ".bin");
      byte[] original = Files.readAllBytes(blob);
      switch (tamper) {
        case "truncate" -> Files.write(blob, Arrays.copyOf(original, original.length - 1));
        case "extend" -> Files.write(blob, Arrays.copyOf(original, original.length + 1));
        case "alter-first" -> {
          byte[] altered = original.clone();
          altered[0] ^= 0x20;
          Files.write(blob, altered);
        }
        case "alter-last" -> {
          byte[] altered = original.clone();
          altered[altered.length - 1] ^= 0x01;
          Files.write(blob, altered);
        }
        case "delete" -> Files.delete(blob);
        case "empty" -> Files.write(blob, new byte[0]);
        default -> {
          Files.delete(blob);
          Files.createDirectory(blob);
        }
      }
      Object reopened = open(root);
      String tag = "R blob " + tamper;
      op(tag + " read key-0 -> " + attempt(() -> call(reopened, "read", "key-0")));
      op(tag + " read key-1 -> " + attempt(() -> call(reopened, "read", "key-1")));
      op(tag + " verifyAll -> " + attempt(() -> { call(reopened, "verifyAll"); return "OK"; }));
      byte[] again = ("payload number 0 of blob-" + tamper).getBytes(StandardCharsets.UTF_8);
      op(tag + " supersede-same -> " + attempt(() -> call(reopened, "supersede", "key-0", e, again)));
      op(tag + " write-same-new-key -> " + attempt(() -> call(reopened, "write", "key-9", e, again)));
      call(reopened, "close");
      op(tag + " after index=" + indexSha(root) + " blobs=" + blobListing(root));
    }

    Path lengths = work.resolve("exact-length");
    Object st = open(lengths);
    byte[] payload = "exact length payload".getBytes(StandardCharsets.UTF_8);
    Object e = fromBytes("src", "7".repeat(64), 10, payload, "raw");
    for (int delta : new int[] {-payload.length, -1, 1, 7}) {
      byte[] given = Arrays.copyOf(payload, payload.length + delta);
      op("R length " + delta + " write -> " + attempt(() -> call(st, "write", "k", e, given)));
    }
    byte[] altered = payload.clone();
    altered[5] ^= 0x04;
    op("R digest write -> " + attempt(() -> call(st, "write", "k", e, altered)));
    op("R null extent -> " + attempt(() -> call(st, "write", "k", null, payload)));
    op("R null key -> " + attempt(() -> call(st, "write", null, e, payload)));
    op("R good write -> " + attempt(() -> call(st, "write", "k", e, payload)));
    op("R second write -> " + attempt(() -> call(st, "write", "k", e, payload)));
    op("R supersede missing -> " + attempt(() -> call(st, "supersede", "missing", e, payload)));
    op("R keys negative -> " + attempt(() -> call(st, "keys", "", -1)));
    op("R keys null -> " + attempt(() -> call(st, "keys", null, 0)));
    op("R second writer -> " + attempt(() -> open(lengths)));
    call(st, "close");
    op("R closed read -> " + attempt(() -> call(st, "read", "k")));
    op("R closed write -> " + attempt(() -> call(st, "write", "z", e, payload)));
    op("R closed keys -> " + attempt(() -> call(st, "keys", "", 0)));
    op("R close twice -> " + attempt(() -> { call(st, "close"); return "OK"; }));

    Path failing = seeded("failed-publish", 2);
    String before = indexSha(failing);
    Object failed = openFailing(failing);
    byte[] next = "a replacement that never publishes".getBytes(StandardCharsets.UTF_8);
    Object nextExtent = fromBytes("src", "4".repeat(64), 0, next, "text");
    op("R failed supersede -> " + attempt(() -> call(failed, "supersede", "key-0", nextExtent, next)));
    op("R failed write -> " + attempt(() -> call(failed, "write", "key-new", nextExtent, next)));
    op("R failed metadata -> " + attempt(() -> call(failed, "metadata", "key-0")));
    op("R failed keys -> " + attempt(() -> call(failed, "keys", "", 0)));
    call(failed, "close");
    op("R failed index unchanged=" + before.equals(indexSha(failing)) + " blobs=" + blobListing(failing));
    Object recovered = open(failing);
    op("R recovered read -> " + attempt(() -> call(recovered, "read", "key-0")));
    op("R recovered supersede -> " + attempt(() -> call(recovered, "supersede", "key-0", nextExtent, next)));
    op("R recovered read -> " + attempt(() -> call(recovered, "read", "key-0")));
    call(recovered, "close");
    op("R recovered index=" + indexSha(failing));
  }

  public static void main(String[] arguments) throws Throwable {
    store = Class.forName(arguments[0]);
    extent = Class.forName(arguments[1]);
    hook = Class.forName(arguments[0] + "$BeforePublish");
    work = Path.of(arguments[2]).toAbsolutePath();
    Files.createDirectories(work);
    int sequences = arguments.length > 3 ? Integer.parseInt(arguments[3]) : 40;
    int ops = arguments.length > 4 ? Integer.parseInt(arguments[4]) : 200;
    int identities = arguments.length > 5 ? Integer.parseInt(arguments[5]) : 2000;
    identity(identities);
    sequences(sequences, ops);
    refusals();
    System.out.write(out.toString().getBytes(StandardCharsets.UTF_8));
    System.out.flush();
  }
}
