# M3 Spring case-insensitive Map: source-first array and index receiver

A source-level Map<String,V> implementation shaped by real Spring Framework 6.2.0 LinkedCaseInsensitiveMap semantics, but NOT a substitute for that concrete Spring class, serialization, clone or subclass hooks.

M3SpringCaseInsensitiveMap<V> reuses two original M3LinkedHashMap owners. One M3 map stores canonical key spelling, values and insertion-order slots; the other indexes locale-folded strings to resident key representatives. No per-element storage Entry or Node. Folded strings and the extra index are real memory costs; no universal speedup. Existing JDK, Guava, Eclipse Collections, Spring MultiValueMap and Apache SDK types retain correct independent upstream API names, tracked in PROVIDER_COVERAGE.tsv.

Actual local OpenJDK21.0.12.1 strict compilation and real spring-core6.2.0/spring-jcl6.2.0 differential passed 947,349 checks over 22,500 seeded operations in default and -Xint interpreter JVM modes. Tested Turkish, ROOT, US locale casefold, null values, explicit put case adoption, putIfAbsent/computeIfAbsent stored representative, Spring keySet.contains aliases, null-valued keySet.remove false result despite removal, entrySet mutation and iteration, map equality/hash/default methods.

Recipe uses existing M3HashPinnedJavaSnapshotRecipe in Java AST/LST mode, with 3 source/test targets ABSENT -> exact SHA256; Maven JUnit replay, source drift/duplicate/refusal and second-pass fixed point are required. Container lacks Maven: do not claim local recipe execution or integrated JNI result. The focused PR CI supplies those gates and independent unchanged C17 M3IndexMaskCompactor -Xcheck:jni / UBSan. No new JNI symbols or Java callback bridge. No source-built JDK, JCK/jtreg, cross-version Spring API parity, JMM/serialization/peak memory/JMH claims.

First-party text Apache-2.0 copyright 2026 Hitesh Soliwal and contributors. Real Spring vendor binary is a referenced oracle and retains its own Apache-2.0 rights; original code is not copied. LeetCode/HackerRank/GeeksforGeeks are algorithm problem categories only. Keep STRING -> arrays -> collections -> AST/compiler promotion gate.
