// SPDX-License-Identifier: Apache-2.0
package com.synexia.donor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Precomputed donor index by mechanical/algorithm shape.
 *
 * <p>This is discovery and provenance metadata only. A catalogue match does not grant source-copy
 * permission, prove semantic compatibility, or promote donor code into Synexia runtime authority.
 * Exact attribution/license handling remains owned by {@link DonorAttribution} and the normal donor
 * intake pipeline.</p>
 */
public final class DonorMechanicalShapeCatalog {

  public enum SourceClass {
    CONTEST_ALGORITHM,
    KNOWLEDGE_REASONING,
    NATIVE_SYSTEMS
  }

  public enum Family {
    ALGORITHM,
    STORAGE_PAGER,
    ALLOCATOR_ARENA,
    POINTER_CONTAINER,
    COMPRESSED_INDEX,
    HASHING,
    COMPRESSION,
    EVENT_RUNTIME,
    KNOWLEDGE_REASONING
  }

  public enum Decision {
    WRAP_ENGINE,
    ADAPT_PATTERN,
    REFERENCE_ONLY
  }

  public record Entry(
      String repository,
      String revision,
      String license,
      String licensePath,
      String licenseBlob,
      SourceClass sourceClass,
      Family family,
      Decision decision,
      List<String> shapes,
      String note) {

    public Entry {
      repository = requireRepository(repository);
      revision = requireSha(revision, "revision");
      license = requireText(license, "license");
      licensePath = requireText(licensePath, "licensePath");
      licenseBlob = requireSha(licenseBlob, "licenseBlob");
      sourceClass = Objects.requireNonNull(sourceClass, "sourceClass");
      family = Objects.requireNonNull(family, "family");
      decision = Objects.requireNonNull(decision, "decision");

      TreeSet<String> normalizedShapes = new TreeSet<>();
      for (String shape : Objects.requireNonNull(shapes, "shapes")) {
        normalizedShapes.add(normalizeShape(shape));
      }
      if (normalizedShapes.isEmpty()) {
        throw new IllegalArgumentException("at least one shape required");
      }
      shapes = List.copyOf(normalizedShapes);
      note = requireText(note, "note");
    }
  }

  private static final List<Entry> ENTRIES;
  private static final Map<String, Entry> BY_REPOSITORY;
  private static final Map<String, List<Entry>> BY_SHAPE;
  private static final Map<SourceClass, List<Entry>> BY_SOURCE_CLASS;
  private static final Map<Family, List<Entry>> BY_FAMILY;
  private static final Map<Decision, List<Entry>> BY_DECISION;
  private static final List<String> SHAPES;

  static {
    ArrayList<Entry> entries = new ArrayList<>();

    entries.add(
        entry(
            "neetcode-gh/leetcode",
            "3186ede2ea4c4788e87be4b509bf2b66d5eba0e9",
            "MIT",
            "LICENSE",
            "144c6f237c2c9a19404bd1def098172a9fc1c7c3",
            SourceClass.CONTEST_ALGORITHM,
            Family.ALGORITHM,
            Decision.REFERENCE_ONLY,
            List.of("BINARY_SEARCH", "SLIDING_WINDOW", "LRU", "LFU", "TOP_K", "HEAP", "HASH_TABLE", "GRAPH", "TRIE"),
            "Problem/category evidence only; challenge solution bodies are not runtime authority."));

    entries.add(
        entry(
            "RyanFehr/HackerRank",
            "bdec63d3accaed7dbb16f62d389b79fad6e04fa9",
            "MIT",
            "license.md",
            "0b85e3420b649ccf273a60e3e7bb656bf2690a89",
            SourceClass.CONTEST_ALGORITHM,
            Family.ALGORITHM,
            Decision.REFERENCE_ONLY,
            List.of("BINARY_SEARCH", "SLIDING_WINDOW", "BFS", "GRAPH", "SEARCH", "QUEUE", "SHORTEST_PATH"),
            "Problem-shape evidence for bounded traversal and search."));

    entries.add(
        entry(
            "kishanrajput23/GFG-Problem-Solutions",
            "39d6a47408cd7a25796e314945258da9aa546227",
            "MIT",
            "LICENSE",
            "d4a14690a511cc9878f9c3221b310cd35002e2c6",
            SourceClass.CONTEST_ALGORITHM,
            Family.ALGORITHM,
            Decision.REFERENCE_ONLY,
            List.of("BINARY_SEARCH", "SLIDING_WINDOW", "LRU", "CACHE", "GRAPH", "BITMAP", "DYNAMIC_PROGRAMMING"),
            "GeeksforGeeks-oriented category evidence only."));

    entries.add(
        entry(
            "TweetyProjectTeam/TweetyProject",
            "2abd5bab530f84dec43947a33bc68d4b1cfa1157",
            "LGPL-3.0-or-later file headers; GPL-3.0 license material also present",
            "lgpl_license.txt",
            "65c5ca88a67c30becee01c5a8816d964b03862f9",
            SourceClass.KNOWLEDGE_REASONING,
            Family.KNOWLEDGE_REASONING,
            Decision.REFERENCE_ONLY,
            List.of(
                "ARGUMENTATION",
                "BELIEF_DYNAMICS",
                "INCONSISTENCY",
                "KNOWLEDGE_REPRESENTATION",
                "LOGIC_REASONING",
                "PROBABILISTIC_REASONING",
                "REASONER",
                "SAT_SOLVING",
                "SET_ITERATION"),
            "Pinned architecture/algorithm-shape evidence only. Repository contains GPL/LGPL "
                + "licensing surfaces; path-level license review is required before any source reuse."));

    entries.add(
        entry(
            "sqlite/sqlite",
            "2acb2ea9089c604d5786f56ee3e50a0479a268dd",
            "Public-Domain",
            "LICENSE.md",
            "5f59bb8fefd54767e2eb08a17d0dc19007958d90",
            SourceClass.NATIVE_SYSTEMS,
            Family.STORAGE_PAGER,
            Decision.WRAP_ENGINE,
            List.of(
                "PAGE_CACHE",
                "PAGER",
                "WAL",
                "BTREE_PAGE",
                "PREPARED_STATEMENT",
                "MMAP"),
            "Explicit wrapped engine where selected; Synexia retains identity/policy authority."));

    entries.add(
        entry(
            "google/leveldb",
            "7ee830d02b623e8ffe0b95d59a74db1e58da04c5",
            "BSD-3-Clause",
            "LICENSE",
            "8e80208cd72b3225c87d9111c4d7cab13af1c2ac",
            SourceClass.NATIVE_SYSTEMS,
            Family.STORAGE_PAGER,
            Decision.REFERENCE_ONLY,
            List.of("BLOCK_CACHE", "LSM", "BLOOM", "COMPACTION", "LOG_STRUCTURED"),
            "Reference for immutable block/table/log mechanics."));

    entries.add(
        entry(
            "facebook/rocksdb",
            "4052fccde9d9533fa9c57749b63ba3cda2c9b134",
            "Apache-2.0",
            "LICENSE.Apache",
            "d645695673349e3947e8e5ae42332d0ac3164cd7",
            SourceClass.NATIVE_SYSTEMS,
            Family.STORAGE_PAGER,
            Decision.REFERENCE_ONLY,
            List.of(
                "PAGE_CACHE",
                "SHARDED_CACHE",
                "PINNED_BLOCK",
                "STRICT_CAPACITY",
                "SECONDARY_CACHE",
                "LSM"),
            "Reference for sharding, pins, secondary cache and strict-capacity behavior."));

    entries.add(
        entry(
            "jemalloc/jemalloc",
            "cf35c74da0b6bf10c4efa8e8c67b026187768b68",
            "BSD-2-Clause",
            "COPYING",
            "3b7fd3585d2b7c6997ba499253cbd773e5f2efb6",
            SourceClass.NATIVE_SYSTEMS,
            Family.ALLOCATOR_ARENA,
            Decision.ADAPT_PATTERN,
            List.of("ARENA", "SLAB", "SIZE_CLASS", "THREAD_CACHE"),
            "Allocator topology reference; no allocator dependency is implied."));

    entries.add(
        entry(
            "microsoft/mimalloc",
            "31d034d94cdb8e22f7d7ed55967f581a2d6e831d",
            "MIT",
            "LICENSE",
            "53315ebee557ac22f3396ea1781df0440c26bb15",
            SourceClass.NATIVE_SYSTEMS,
            Family.ALLOCATOR_ARENA,
            Decision.ADAPT_PATTERN,
            List.of("ARENA", "PAGE", "SEGMENT", "FREE_LIST", "LOCAL_SHARD"),
            "Reference for page/segment ownership, free lists and locality."));

    entries.add(
        entry(
            "google/tcmalloc",
            "0ee717eb71245ca7cc60b4d6d0f6b260de7a945a",
            "Apache-2.0",
            "LICENSE",
            "62589edd12a37dd28b6b6fed1e2d728ac9f05c8d",
            SourceClass.NATIVE_SYSTEMS,
            Family.ALLOCATOR_ARENA,
            Decision.REFERENCE_ONLY,
            List.of("THREAD_CACHE", "SIZE_CLASS", "CENTRAL_FREE_LIST", "PAGE_HEAP"),
            "Reference for split local/central allocation ownership."));

    entries.add(
        entry(
            "troydhanson/uthash",
            "a49bed0b4abb7dff16c73906dcdc8a9718d582d2",
            "BSD-1-Clause",
            "LICENSE",
            "039b7b4f24cf9bba559f5d2c5816ccf79e8366d4",
            SourceClass.NATIVE_SYSTEMS,
            Family.POINTER_CONTAINER,
            Decision.ADAPT_PATTERN,
            List.of("INTRUSIVE_HASH", "INTRUSIVE_LIST", "NO_WRAPPER_NODE"),
            "Pointer-era container shape translated to coordinates/primitive lanes."));

    entries.add(
        entry(
            "attractivechaos/klib",
            "97a0fcb790b43b9e5da8994f4671021fec036f19",
            "MIT",
            "LICENSE.txt",
            "0a996fae3360fb8445ac2e050c6fa267eba97451",
            SourceClass.NATIVE_SYSTEMS,
            Family.POINTER_CONTAINER,
            Decision.ADAPT_PATTERN,
            List.of("ARENA", "HASH_TABLE", "DYNAMIC_ARRAY", "BINARY_HEAP", "SORT"),
            "Compact C container/arena mechanics reference."));

    entries.add(
        entry(
            "RoaringBitmap/CRoaring",
            "736200862589ef3cca72007dbcdaf1537c49dcbc",
            "Apache-2.0 OR MIT",
            "LICENSE",
            "8b0ad80d743d242082d7c6e0bca54be0c26a6507",
            SourceClass.NATIVE_SYSTEMS,
            Family.COMPRESSED_INDEX,
            Decision.ADAPT_PATTERN,
            List.of(
                "ADAPTIVE_BITMAP",
                "ARRAY_CONTAINER",
                "BITMAP_CONTAINER",
                "RUN_CONTAINER",
                "RANK_SELECT"),
            "Reference for density-driven representation changes."));

    entries.add(
        entry(
            "OffchainLabs/hashtree",
            "76c54d6dcf3596c03788ea5bc0ef030e9f1b43f4",
            "MIT",
            "LICENSE",
            "c3c5f10f345a19b90f7aa840b14d82cedce0801b",
            SourceClass.NATIVE_SYSTEMS,
            Family.HASHING,
            Decision.ADAPT_PATTERN,
            List.of("SHA256_FIXED_64B", "SHA256_MULTI_BUFFER", "SIMD_HASH"),
            "Pinned Merkle/fixed-block SHA-256 SIMD pattern evidence; no source-copy authority."));
    entries.add(
        entry(
            "intel/intel-ipsec-mb",
            "75c6991063d8c4726c6a95ca84987b1ec88d11f8",
            "BSD-3-Clause AND Apache-2.0",
            "LICENSE",
            "da02730e5ece64e0e0b5b735a1b5a43e287fbca0",
            SourceClass.NATIVE_SYSTEMS,
            Family.HASHING,
            Decision.WRAP_ENGINE,
            List.of("SHA256_HARDWARE_DISPATCH", "SHA256_MULTI_BUFFER", "SIMD_HASH"),
            "Pinned optional linked SHA-256 multi-buffer engine; public job API only."));
    entries.add(
        entry(
            "kste/sha256_avx",
            "c7d86de978d6ffa81563e119a244ec29ec78bca2",
            "MIT",
            "LICENSE",
            "8864d4a39633c1bcb33a8983d146d0b6390ed3d0",
            SourceClass.NATIVE_SYSTEMS,
            Family.HASHING,
            Decision.REFERENCE_ONLY,
            List.of("SHA256_MULTI_BUFFER", "SIMD_HASH"),
            "Pinned compact eight-lane AVX SHA-256 evidence; no source copied."));
    entries.add(
        entry(
            "minio/sha256-simd",
            "6096f891a77bfe490cbea7a424c821b5fdb92849",
            "Apache-2.0",
            "LICENSE",
            "d645695673349e3947e8e5ae42332d0ac3164cd7",
            SourceClass.NATIVE_SYSTEMS,
            Family.HASHING,
            Decision.REFERENCE_ONLY,
            List.of("SHA256_HARDWARE_DISPATCH", "SHA256_MULTI_BUFFER", "SIMD_HASH"),
            "Pinned independent-stream scheduling/reference evidence; Go/assembly source not imported."));
    entries.add(
        entry(
            "Cyan4973/xxHash",
            "680bf463fa1ca0461b9a7c2dab7556e1f54cf4cf",
            "BSD-2-Clause",
            "LICENSE",
            "e4c5da7234eca7baad9a46f0b144e49ccad2e500",
            SourceClass.NATIVE_SYSTEMS,
            Family.HASHING,
            Decision.REFERENCE_ONLY,
            List.of("BULK_HASH", "STREAM_HASH", "FINGERPRINT"),
            "Candidate hashing reference; exact identity remains stronger than a fast hash."));

    entries.add(
        entry(
            "lz4/lz4",
            "0774d05537f9762f838f7ab541b7765f1a729cb5",
            "BSD-2-Clause(lib)",
            "LICENSE",
            "ba8a2900f8b180fefc0c16f8a35076ef290c9492",
            SourceClass.NATIVE_SYSTEMS,
            Family.COMPRESSION,
            Decision.REFERENCE_ONLY,
            List.of("BLOCK_COMPRESSION", "FRAME", "BOUNDED_DECODE"),
            "Library block/frame mechanics only; benchmark gate required."));

    entries.add(
        entry(
            "facebook/zstd",
            "01b7154f1172432f8abe9b3bb9909e14a1176b7d",
            "BSD",
            "LICENSE",
            "75800288cc243f164b9d130f125e2ffae28f8e39",
            SourceClass.NATIVE_SYSTEMS,
            Family.COMPRESSION,
            Decision.REFERENCE_ONLY,
            List.of("BLOCK_COMPRESSION", "DICTIONARY_COMPRESSION", "STREAM_COMPRESSION"),
            "Compression/dictionary mechanics reference; benchmark gate required."));

    entries.add(
        entry(
            "libuv/libuv",
            "abe835d41317b55b16260990821f89b4ee9e437b",
            "MIT",
            "LICENSE",
            "6566365d4f238005a3358710e20b2acde266b594",
            SourceClass.NATIVE_SYSTEMS,
            Family.EVENT_RUNTIME,
            Decision.REFERENCE_ONLY,
            List.of("EVENT_LOOP", "ASYNC_IO", "HANDLE_LIFECYCLE"),
            "Reference for explicit handle/event ownership and asynchronous I/O lifecycle."));

    entries.add(
        entry(
            "memcached/memcached",
            "2d51e364799bc9698bd4b11728ea978cea12da6e",
            "BSD",
            "LICENSE",
            "4746b00c326b9a010ffe0265d4facd1c1d6ee644",
            SourceClass.NATIVE_SYSTEMS,
            Family.EVENT_RUNTIME,
            Decision.REFERENCE_ONLY,
            List.of("SLAB", "LRU", "HASH_TABLE", "EVENT_LOOP", "ITEM_LIFECYCLE"),
            "Reference for slab-backed LRU server object lifecycles."));

    entries.sort((left, right) -> left.repository().compareTo(right.repository()));
    ENTRIES = List.copyOf(entries);

    TreeMap<String, Entry> byRepository = new TreeMap<>();
    TreeMap<String, ArrayList<Entry>> byShape = new TreeMap<>();
    EnumMap<SourceClass, ArrayList<Entry>> bySource = new EnumMap<>(SourceClass.class);
    EnumMap<Family, ArrayList<Entry>> byFamily = new EnumMap<>(Family.class);
    EnumMap<Decision, ArrayList<Entry>> byDecision = new EnumMap<>(Decision.class);

    for (Entry entry : ENTRIES) {
      if (byRepository.putIfAbsent(entry.repository(), entry) != null) {
        throw new ExceptionInInitializerError("duplicate donor repository " + entry.repository());
      }
      for (String shape : entry.shapes()) {
        byShape.computeIfAbsent(shape, ignored -> new ArrayList<>()).add(entry);
      }
      bySource.computeIfAbsent(entry.sourceClass(), ignored -> new ArrayList<>()).add(entry);
      byFamily.computeIfAbsent(entry.family(), ignored -> new ArrayList<>()).add(entry);
      byDecision.computeIfAbsent(entry.decision(), ignored -> new ArrayList<>()).add(entry);
    }

    BY_REPOSITORY = Collections.unmodifiableMap(byRepository);
    BY_SHAPE = freezeStringIndex(byShape);
    BY_SOURCE_CLASS = freezeEnumIndex(bySource);
    BY_FAMILY = freezeEnumIndex(byFamily);
    BY_DECISION = freezeEnumIndex(byDecision);
    SHAPES = List.copyOf(BY_SHAPE.keySet());
  }

  private DonorMechanicalShapeCatalog() {}

  public static List<Entry> all() {
    return ENTRIES;
  }

  public static Optional<Entry> byRepository(String repository) {
    return Optional.ofNullable(BY_REPOSITORY.get(requireRepository(repository)));
  }

  public static List<Entry> byShape(String shape) {
    return BY_SHAPE.getOrDefault(normalizeShape(shape), List.of());
  }

  public static List<Entry> bySourceClass(SourceClass sourceClass) {
    return BY_SOURCE_CLASS.getOrDefault(
        Objects.requireNonNull(sourceClass, "sourceClass"),
        List.of());
  }

  public static List<Entry> byFamily(Family family) {
    return BY_FAMILY.getOrDefault(Objects.requireNonNull(family, "family"), List.of());
  }

  public static List<Entry> byDecision(Decision decision) {
    return BY_DECISION.getOrDefault(
        Objects.requireNonNull(decision, "decision"),
        List.of());
  }

  public static List<String> shapes() {
    return SHAPES;
  }

  public static String manifest() {
    StringBuilder out =
        new StringBuilder(
            "repository\trevision\tlicense\tsourceClass\tfamily\tdecision\tshape\tnote\n");
    for (Entry entry : ENTRIES) {
      for (String shape : entry.shapes()) {
        out.append(entry.repository())
            .append('\t')
            .append(entry.revision())
            .append('\t')
            .append(entry.license())
            .append('\t')
            .append(entry.sourceClass())
            .append('\t')
            .append(entry.family())
            .append('\t')
            .append(entry.decision())
            .append('\t')
            .append(shape)
            .append('\t')
            .append(entry.note())
            .append('\n');
      }
    }
    return out.toString();
  }


  private static Entry entry(
      String repository,
      String revision,
      String license,
      String licensePath,
      String licenseBlob,
      SourceClass sourceClass,
      Family family,
      Decision decision,
      List<String> shapes,
      String note) {
    return new Entry(
        repository,
        revision,
        license,
        licensePath,
        licenseBlob,
        sourceClass,
        family,
        decision,
        shapes,
        note);
  }

  private static String normalizeShape(String value) {
    String normalized = requireText(value, "shape")
        .toUpperCase(Locale.ROOT)
        .replace('-', '_')
        .replace(' ', '_');
    if (!normalized.matches("[A-Z0-9_]{1,80}")) {
      throw new IllegalArgumentException("invalid shape");
    }
    return normalized;
  }

  private static String requireRepository(String value) {
    String repository = requireText(value, "repository");
    if (!repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) {
      throw new IllegalArgumentException("repository must be owner/name");
    }
    return repository;
  }

  private static String requireSha(String value, String field) {
    String sha = requireText(value, field);
    if (!sha.matches("[0-9a-f]{40}")) {
      throw new IllegalArgumentException(field + " must be a pinned Git SHA");
    }
    return sha;
  }

  private static String requireText(String value, String field) {
    String normalized = Objects.requireNonNull(value, field).trim();
    if (normalized.isEmpty()) throw new IllegalArgumentException(field + " required");
    return normalized;
  }

  private static Map<String, List<Entry>> freezeStringIndex(
      Map<String, ? extends List<Entry>> source) {
    TreeMap<String, List<Entry>> result = new TreeMap<>();
    source.forEach((key, value) -> result.put(key, List.copyOf(value)));
    return Collections.unmodifiableMap(result);
  }

  private static <E extends Enum<E>> Map<E, List<Entry>> freezeEnumIndex(
      Map<E, ? extends List<Entry>> source) {
    if (source.isEmpty()) return Map.of();
    E first = source.keySet().iterator().next();
    Class<E> type = first.getDeclaringClass();
    EnumMap<E, List<Entry>> result = new EnumMap<>(type);
    source.forEach((key, value) -> result.put(key, List.copyOf(value)));
    return Collections.unmodifiableMap(result);
  }
}
