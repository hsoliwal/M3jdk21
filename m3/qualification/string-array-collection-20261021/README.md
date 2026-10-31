# Integrated String and collection qualification

This receipt identifies 23 source changes emitted by the existing canonical Synexia recipes. M3JDK21 owns the Java, HotSpot, JNI and collection implementation; the qualification crate is not a product runtime dependency.

Canonical implementation, catalogue, donor attribution, append-only correction history and complete test evidence: [Synexia review](https://github.com/hsoliwal/com.synexia/pull/12748), commit `fb936abe6e3c643f5198b0aaefe5589359a0ec98`.

## Result

The original JDK image build passed. The matching-image String gate passed five tests, 13 VM actions and 24,499 assertions. Owning collection suites passed 551 Java tests (two optional native skips) and 551 required-native tests (zero skips), with zero failures/errors. Installed CollectionViews passed 77 bootstrap checks, 14,700 Java checks, and 15,732 native-mode checks including 1,027 JNI queries.

The canonical recipe qualified all 23 serial phases, fixed points and negative guards. Actual application wrote 16 + 2 + 2 + 1 + 2 files; final replay wrote zero. Exact preimages and postimages are in TARGET_FILES.tsv. RECEIPT.json pins the authoritative proof and its review holds.

## Contract and ownership

Bulk char-array admission reuses the exact historical internal owner methods and existing canonical array/segment owners. It validates all lanes before admission and adds no retained flat payload cache or public String method. Public/JNI output arrays remain independent materialized results. Native range validation occurs before allocation. Filtered endpoint and cursor repairs preserve the current direct reversed/COW owners. Java/JNI radix skips redundant uniform-digit work without adding per-element nodes.

Compilation recoveries retain original compiler gates: nine comment dashes, 29 Javadoc tags and an existing one-argument scaffold exception constructor. Numeric token-text behavior remains unreceived. Exact historical Java/C test companions restore the original installed-view contract tests, which are otherwise unchanged.

## Review limits

Invalid private native inputs, endpoint refusal and cursor exception propagation have explicit observable repairs. The facade adds two declared overrides, requiring reflection-surface review. Original source licenses are retained; existing Apache-licensed core additions require integration review. Runtime evidence is pinned to the recorded base. A later M3StringPool leaf-cursor change was checked for clean three-way merge, not runtime-qualified as part of this image.

Observed platform is Linux x86_64. Test JNI libraries use fatal UBSan and -Xcheck:jni; the full JDK image is not sanitizer-instrumented. Full JDK/JCK, alternate-platform and GC/CDS/JVMTI qualification remain outside this packet. No blanket performance or memory claim is made. The global receiving phase remains STRING and completion remains false.
