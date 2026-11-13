# jdk.internal.m3.compression: product prefix dropped (2026-11-08)

The user's 2026-11-08 rule: no naming sprawl in the JDK. Inside an M3 package the package already names the product,
so a type carries no `M3` prefix. `make/m3/tooling/jdk-naming/rename_types.py` (DROP_PRODUCT_PREFIX v1) renamed every type
of the package (`RENAME.tsv`: 43 types, 10 product and 33 test). Files, references, qualified names in strings, JNI
symbols (`Java_jdk_internal_m3_compression_LzNative_*`) and the launcher script follow. Other string literals and the
evidence records keep their bytes. A second run reports no change.

Gate: `make/m3/compression/test-java-base.sh` (WSL, OpenJDK 21, GCC) before and after the rename gives identical
results, with class names normalised: JUnit 14/14 without and with the JNI library, all 10 self-test mains with the
same check counts, and the same LZ4 and Snappy output SHA-256.
