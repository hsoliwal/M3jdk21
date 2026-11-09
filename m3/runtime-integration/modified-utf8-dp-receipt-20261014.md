# M3JDK21 composite JNI modified UTF-8 length DP — receiving receipt

**Exact target preimage:** `f9b3a0ce84dcdbd0cc75df956def7710ac5d04fd`.

**Source-authoring owner:** `hsoliwal/com.synexia` draft PR [#11288](https://github.com/hsoliwal/com.synexia/pull/11288), recipe `com.synexia.m3.M3Jdk21ModifiedUtf8Dp` with JDK Java/C++/JNI SHA-256 preimages, postimages and ABSENT test assertions.

## Change

The cached `M3StringFacts.utf8Length` remains the ordinary Java `StandardCharsets.UTF_8` byte count, including surrogate-pair seam adjustments and replacement for unpaired surrogates.

New `M3StringFacts.modifiedUtf8Length` is JNI's length: NUL two bytes, each UTF-16 surrogate unit three bytes, other UTF-16 code units one/two/three bytes. Its immutable atom summaries compose **by addition**, even at a high-surrogate/low-surrogate seam. The HotSpot `java_lang_M3String` offset table and `utf8_length_if_precomputed` accessor are updated together. A would-overflow positive `jint` is marked unavailable (`-1`), enabling the existing slow authoritative scan; this does not alter preexisting very-large-string JNI API limits.

JDK `M3String` instance layout remains exactly owner+packed coordinate. Characters remain native/mapped atoms and shared tuple DAGs, no new payload arrays or JNI exported methods.

## Status

The independent stock-JDK 21 C17/JNI modified UTF-8 oracle passed 16,122 cases and 199,254 checks under `-Xcheck:jni -Xmx32m`. SQLite authoring-only immutable fact ledger passed 10,003 checks including corruption/staleness refusal. The new jtreg source is committed but not yet tested against this target's matched M3JDK21 runtime.

**Hold promotion:** full OpenJDK build, HotSpot C++ compilation, jtreg native test, Java JNI M3 flag-on/flag-off, GC/JIT/CDS/JFR/serviceability, product native char-array/byte-array behavior and platform matrices remain NOT_RUN. No claim that all ordinary Java String admissions succeed or that process RSS stays within -Xmx; pool admission can still safely fall back under pressure.

Runtime remains JDK-owned; no dependency on Synexia Maven classes or SQLite.
