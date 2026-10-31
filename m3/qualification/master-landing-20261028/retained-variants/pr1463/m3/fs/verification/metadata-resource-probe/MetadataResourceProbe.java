// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/** Finite construction probe; measurements are evidence, never performance assertions. */
public final class MetadataResourceProbe {
  private static volatile Object retainedOwner;
  private static volatile Map<String, Object> retainedInput;
  private static final List<MemoryPoolMXBean> HEAP_POOLS = ManagementFactory.getMemoryPoolMXBeans()
      .stream().filter(pool -> pool.getType() == MemoryType.HEAP).toList();

  private MetadataResourceProbe() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 4) throw new IllegalArgumentException("package rows warmups repetitions");
    String ownerPackage = args[0];
    int rows = Integer.parseInt(args[1]);
    int warmups = Integer.parseInt(args[2]);
    int repetitions = Integer.parseInt(args[3]);
    if (rows < 1 || rows > 100_000 || warmups < 1 || repetitions < 1 || repetitions > 10)
      throw new IllegalArgumentException("probe bounds exceeded");

    ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    if (!bean.isCurrentThreadCpuTimeSupported() || !bean.isThreadAllocatedMemorySupported())
      throw new IllegalStateException("current-thread CPU and allocation counters are required");
    bean.setThreadCpuTimeEnabled(true);
    bean.setThreadAllocatedMemoryEnabled(true);
    long thread = Thread.currentThread().threadId();
    Class<?> idClass = Class.forName(ownerPackage + ".M3ContentId");
    Class<?> rootClass = Class.forName(ownerPackage + ".M3GenerationRoot");
    Constructor<?> constructor = rootClass.getConstructor(long.class, idClass, Map.class);
    Method of = idClass.getMethod("of", byte[].class);
    Method canonicalBytes = rootClass.getMethod("canonicalBytes");
    Method rootId = rootClass.getMethod("id");

    int[] order = new int[rows];
    for (int row = 0; row < rows; row++) order[row] = row;
    SplittableRandom random = new SplittableRandom(0x4d33465320261019L);
    for (int row = rows - 1; row > 0; row--) {
      int swap = random.nextInt(row + 1);
      int value = order[row]; order[row] = order[swap]; order[swap] = value;
    }
    MessageDigest ids = MessageDigest.getInstance("SHA-256");
    MessageDigest inputDigest = MessageDigest.getInstance("SHA-256");
    Map<String, Object> input = new LinkedHashMap<>();
    for (int row : order) {
      String path = "/project/p" + (row % 64) + "/src/Unit" + (1_000_000 + row)
          + (row % 16 == 0 ? "_\u00c9.java" : ".java");
      byte[] id = ids.digest(("manifest-" + row).getBytes(StandardCharsets.UTF_8));
      input.put(path, of.invoke(null, (Object) id));
      inputDigest.update(path.getBytes(StandardCharsets.UTF_8));
      inputDigest.update((byte) 0);
      inputDigest.update(id);
    }
    retainedInput = input;
    System.out.println("ENV\tpackage\t" + ownerPackage);
    System.out.println("ENV\tjava_runtime_version\t" + System.getProperty("java.runtime.version"));
    System.out.println("ENV\tjava_vm_name\t" + System.getProperty("java.vm.name"));
    System.out.println("ENV\tjvm_arguments\t" + ManagementFactory.getRuntimeMXBean().getInputArguments());
    System.out.println("ENV\theap_used_source\tSum of heap MemoryPoolMXBean collection-usage snapshots after GC");
    System.out.println("INPUT\t" + rows + "\t" + HexFormat.of().formatHex(inputDigest.digest()));
    System.out.println("COLUMNS\trepetition\trows\tconstructor_cpu_ns\tconstructor_allocated_bytes"
        + "\tconstructor_wall_ns\theap_baseline_bytes\theap_retained_bytes\theap_delta_bytes"
        + "\tvm_rss_kib\tvm_hwm_kib\trss_anon_kib\trss_file_kib"
        + "\theap_live_baseline_bytes\theap_live_retained_bytes");

    for (int run = 0; run < warmups; run++) retainedOwner = constructor.newInstance(0L, null, input);
    for (int run = 1; run <= repetitions; run++) {
      retainedOwner = null;
      collect();
      long baseline = heapUsed();
      long liveBaseline = heapLive();
      long cpuStart = bean.getCurrentThreadCpuTime();
      long allocationStart = bean.getThreadAllocatedBytes(thread);
      long wallStart = System.nanoTime();
      retainedOwner = constructor.newInstance(0L, null, input);
      long wall = System.nanoTime() - wallStart;
      long allocation = bean.getThreadAllocatedBytes(thread) - allocationStart;
      long cpu = bean.getCurrentThreadCpuTime() - cpuStart;
      collect();
      long retained = heapUsed();
      long liveRetained = heapLive();
      long[] rss = processRss();
      System.out.println("SAMPLE\t" + run + "\t" + rows + "\t" + cpu + "\t" + allocation
          + "\t" + wall + "\t" + baseline + "\t" + retained + "\t" + (retained - baseline)
          + "\t" + rss[0] + "\t" + rss[1] + "\t" + rss[2] + "\t" + rss[3]
          + "\t" + liveBaseline + "\t" + liveRetained);
      verifyCanonical(canonicalBytes, rootId, retainedOwner, run);
    }
    if (retainedInput.size() != rows || retainedOwner == null) throw new AssertionError("retention failed");
  }

  private static void verifyCanonical(Method canonicalBytes, Method rootId, Object owner, int run)
      throws Exception {
    byte[] bytes = (byte[]) canonicalBytes.invoke(owner);
    String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    String id = rootId.invoke(owner).toString();
    if (!id.equals(digest)) throw new AssertionError("root ID disagrees with canonical bytes");
    System.out.println("CANONICAL\t" + run + "\t" + bytes.length + "\t" + digest);
  }

  private static long heapUsed() {
    long sum = 0;
    for (MemoryPoolMXBean pool : HEAP_POOLS) {
      MemoryUsage usage = pool.getCollectionUsage();
      if (usage == null) throw new IllegalStateException("heap collection-usage counter unavailable: " + pool.getName());
      sum += usage.getUsed();
    }
    return sum;
  }

  private static long heapLive() {
    Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }

  private static void collect() throws InterruptedException {
    for (int attempt = 0; attempt < 3; attempt++) {
      System.gc();
      Thread.sleep(80);
    }
  }

  private static long[] processRss() throws Exception {
    long[] values = {-1, -1, -1, -1};
    List<String> names = List.of("VmRSS:", "VmHWM:", "RssAnon:", "RssFile:");
    for (String line : Files.readAllLines(Path.of("/proc/self/status"), StandardCharsets.US_ASCII)) {
      for (int index = 0; index < names.size(); index++) {
        if (line.startsWith(names.get(index))) values[index] = Long.parseLong(line.trim().split("\\s+")[1]);
      }
    }
    for (long value : values) if (value < 0) throw new IllegalStateException("Linux RSS counter unavailable");
    return values;
  }
}
