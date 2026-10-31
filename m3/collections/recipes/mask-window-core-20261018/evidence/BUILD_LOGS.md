# Exact selected final-pass command logs

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0

Generated from the 105-command ordered run. Empty stdout is explicitly shown; exit code is authoritative. Full per-command raw outputs and event history are in the delivery archive identified by RECEIPT.json.

## 079 java-lint

Exit: 0; expected failure: false; stdout SHA-256: `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`

```text
["javac", "--release", "21", "-proc:none", "-Xlint:all", "-Werror", "-d", "/mnt/data/m3sdk_20261018/publication/proof/test/lint", "/mnt/data/m3sdk_20261018/publication/proof/candidate/m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java", "/mnt/data/m3sdk_20261018/publication/proof/candidate/m3/collections/src/main/java/com/m3/collections/M3LongAddressKernel.java", "/mnt/data/m3sdk_20261018/publication/proof/candidate/m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java", "/mnt/data/m3sdk_20261018/publication/proof/candidate/m3/collections/src/test/java/com/m3/collections/M3MaskWindowKernelSelfTest.java"]
[stdout empty]
```

## 080 native-lint

Exit: 0; expected failure: false; stdout SHA-256: `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`

```text
["gcc", "-std=c17", "-Wall", "-Wextra", "-Wconversion", "-Wshadow", "-Wpedantic", "-Werror", "-I/usr/lib/jvm/java-21-openjdk-amd64/include", "-I/usr/lib/jvm/java-21-openjdk-amd64/include/linux", "-fsyntax-only", "/mnt/data/m3sdk_20261018/publication/proof/candidate/m3/collections/src/main/native/collections/m3_index_mask_compactor.c"]
[stdout empty]
```

## 081 probe-lint

Exit: 0; expected failure: false; stdout SHA-256: `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`

```text
["gcc", "-std=c17", "-Wall", "-Wextra", "-Wconversion", "-Wshadow", "-Wpedantic", "-Werror", "-I/usr/lib/jvm/java-21-openjdk-amd64/include", "-I/usr/lib/jvm/java-21-openjdk-amd64/include/linux", "-DPRODUCT_SOURCE=\"/mnt/data/m3sdk_20261018/publication/proof/candidate/m3/collections/src/main/native/collections/m3_index_mask_compactor.c\"", "-fsyntax-only", "/mnt/data/m3sdk_20261018/publication/recipe/probes/native_probe.c"]
[stdout empty]
```

## 082 java-compile

Exit: 0; expected failure: false; stdout SHA-256: `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`

```text
["javac", "--release", "21", "-proc:none", "-h", "/mnt/data/m3sdk_20261018/publication/proof/test/headers", "-d", "/mnt/data/m3sdk_20261018/publication/proof/test/classes", "/mnt/data/m3sdk_20261018/publication/proof/candidate/m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java", "/mnt/data/m3sdk_20261018/publication/proof/candidate/m3/collections/src/main/java/com/m3/collections/M3LongAddressKernel.java", "/mnt/data/m3sdk_20261018/publication/proof/candidate/m3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java", "/mnt/data/m3sdk_20261018/publication/proof/candidate/m3/collections/src/test/java/com/m3/collections/M3MaskWindowKernelSelfTest.java"]
[stdout empty]
```

## 083 native-compile

Exit: 0; expected failure: false; stdout SHA-256: `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`

```text
["gcc", "-std=c17", "-Wall", "-Wextra", "-Wconversion", "-Wshadow", "-Wpedantic", "-Werror", "-I/usr/lib/jvm/java-21-openjdk-amd64/include", "-I/usr/lib/jvm/java-21-openjdk-amd64/include/linux", "-fsanitize=undefined", "-fno-sanitize-recover=all", "-O2", "-fPIC", "-shared", "-fstack-usage", "/mnt/data/m3sdk_20261018/publication/proof/candidate/m3/collections/src/main/native/collections/m3_index_mask_compactor.c", "-o", "/mnt/data/m3sdk_20261018/publication/proof/test/native/libm3_collections_mask.so"]
[stdout empty]
```

## 084 probe-compile

Exit: 0; expected failure: false; stdout SHA-256: `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`

```text
["gcc", "-std=c17", "-Wall", "-Wextra", "-Wconversion", "-Wshadow", "-Wpedantic", "-Werror", "-I/usr/lib/jvm/java-21-openjdk-amd64/include", "-I/usr/lib/jvm/java-21-openjdk-amd64/include/linux", "-fsanitize=undefined", "-fno-sanitize-recover=all", "-O2", "-DPRODUCT_SOURCE=\"/mnt/data/m3sdk_20261018/publication/proof/candidate/m3/collections/src/main/native/collections/m3_index_mask_compactor.c\"", "/mnt/data/m3sdk_20261018/publication/recipe/probes/native_probe.c", "-o", "/mnt/data/m3sdk_20261018/publication/proof/test/native-probe"]
[stdout empty]
```

## 085 api

Exit: 0; expected failure: false; stdout SHA-256: `b702a628e0cfce90a60bc9bf3872ab9463288bbcc9b71425f8a737d57d76dc90`

```text
["javap", "-public", "-s", "-cp", "/mnt/data/m3sdk_20261018/publication/proof/test/classes", "com.m3.collections.M3IndexMaskCompactor"]
Compiled from "M3IndexMaskCompactor.java"
public final class com.m3.collections.M3IndexMaskCompactor {
  public static boolean nativeAvailable();
    descriptor: ()Z

  public static int[] select(int[], byte[], int, com.m3.collections.M3IndexMaskCompactor$Backend);
    descriptor: ([I[BILcom/m3/collections/M3IndexMaskCompactor$Backend;)[I

  public static int[] selectPositions(byte[], int, com.m3.collections.M3IndexMaskCompactor$Backend);
    descriptor: ([BILcom/m3/collections/M3IndexMaskCompactor$Backend;)[I

  public static int selectedCount(byte[], int);
    descriptor: ([BI)I

  public static long[] selectLong(long[], byte[], int);
    descriptor: ([J[BI)[J

  public static int[] selectWindow(int[], int, byte[], int, int, int, com.m3.collections.M3IndexMaskCompactor$Backend);
    descriptor: ([II[BIIILcom/m3/collections/M3IndexMaskCompactor$Backend;)[I

  public static long[] selectLongWindow(long[], int, byte[], int, int, int, com.m3.collections.M3IndexMaskCompactor$Backend);
    descriptor: ([JI[BIIILcom/m3/collections/M3IndexMaskCompactor$Backend;)[J
}
```

## 086 api

Exit: 0; expected failure: false; stdout SHA-256: `eb9b5480c80efd01f44ba9eff3b750939e293516df51771903219dc36126c063`

```text
["javap", "-public", "-s", "-cp", "/mnt/data/m3sdk_20261018/publication/proof/test/classes", "com.m3.collections.M3IndexMaskCompactor$Backend"]
Compiled from "M3IndexMaskCompactor.java"
public final class com.m3.collections.M3IndexMaskCompactor$Backend extends java.lang.Enum<com.m3.collections.M3IndexMaskCompactor$Backend> {
  public static final com.m3.collections.M3IndexMaskCompactor$Backend JAVA;
    descriptor: Lcom/m3/collections/M3IndexMaskCompactor$Backend;
  public static final com.m3.collections.M3IndexMaskCompactor$Backend NATIVE_REQUIRED;
    descriptor: Lcom/m3/collections/M3IndexMaskCompactor$Backend;
  public static com.m3.collections.M3IndexMaskCompactor$Backend[] values();
    descriptor: ()[Lcom/m3/collections/M3IndexMaskCompactor$Backend;

  public static com.m3.collections.M3IndexMaskCompactor$Backend valueOf(java.lang.String);
    descriptor: (Ljava/lang/String;)Lcom/m3/collections/M3IndexMaskCompactor$Backend;
}
```

## 087 symbols

Exit: 0; expected failure: false; stdout SHA-256: `eab0e6e572008315a7c9e0e74d70f0d8aae72fec61652cf00f2ea9b04392aff1`

```text
["nm", "-D", "--defined-only", "/mnt/data/m3sdk_20261018/publication/proof/test/native/libm3_collections_mask.so"]
00000000000049d0 T Java_com_m3_collections_M3IndexMaskCompactor_positionsAbiNative
00000000000045a0 T Java_com_m3_collections_M3IndexMaskCompactor_rangeAbiNative
0000000000005140 T Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative
00000000000041b0 T Java_com_m3_collections_M3IndexMaskCompactor_selectNative
00000000000049e0 T Java_com_m3_collections_M3IndexMaskCompactor_selectPositionsNative
00000000000045b0 T Java_com_m3_collections_M3IndexMaskCompactor_selectRangeNative
00000000000050b0 T Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative
```

## 088 probe-test

Exit: 0; expected failure: false; stdout SHA-256: `27988b573cee7eb8fe4f47d123b7518c616947c79902e2a48f9d7f4d57e52d18`

```text
["/mnt/data/m3sdk_20261018/publication/proof/test/native-probe"]
WORK width=0 pattern=0 row_reads=0 row_elements=0 state_reads=8 writes=0
WORK width=0 pattern=1 row_reads=1 row_elements=256 state_reads=8 writes=1
WORK width=0 pattern=2 row_reads=4 row_elements=1024 state_reads=8 writes=4
WORK width=0 pattern=3 row_reads=4 row_elements=1024 state_reads=0 writes=4
WORK width=1 pattern=0 row_reads=0 row_elements=0 state_reads=8 writes=0
WORK width=1 pattern=1 row_reads=1 row_elements=256 state_reads=8 writes=1
WORK width=1 pattern=2 row_reads=4 row_elements=1024 state_reads=8 writes=4
WORK width=1 pattern=3 row_reads=4 row_elements=1024 state_reads=0 writes=4
NATIVE_PROBE_PASS assertions=7198
```

## 089 java-test

Exit: 0; expected failure: false; stdout SHA-256: `cf7bf2611b8965c4d30a6eabecfaa1947d4034fbb3cf1c2770d74d78c36f04c6`

```text
["java", "-Xmx32m", "-Xcheck:jni", "-Djava.library.path=/mnt/data/m3sdk_20261018/publication/proof/test/native", "-cp", "/mnt/data/m3sdk_20261018/publication/proof/test/classes", "com.m3.collections.M3MaskWindowKernelSelfTest"]
M3_MASK_WINDOW_PASS checks=18744 trace=1c6611ecf874e43ecc53e4d746badcb9bb3ad854da3b57a109c949987b61dd87
```

## 090 jni-runtime

Exit: 0; expected failure: false; stdout SHA-256: `7e68774530aaa7e738962f422dab2f8210735613b8b93e5f50b7c572afc04edc`

```text
["java", "-Xmx32m", "-Xcheck:jni", "-Djava.library.path=/mnt/data/m3sdk_20261018/publication/proof/test/native", "-cp", "/mnt/data/m3sdk_20261018/publication/proof/test/classes", "com.m3.collections.M3MaskWindowKernelSelfTest", "--native"]
M3_MASK_WINDOW_PASS checks=20549 trace=2c45448e20add12d774974ad7ab9ea38274e93b589c1e72bde25873fcb36944a
```

## 091 missing-runtime

Exit: 0; expected failure: false; stdout SHA-256: `e8ee77d6b6127aecc56d2b45b6519178f9c05e55b0526110efe8416264385001`

```text
["java", "-Djava.library.path=/mnt/data/m3sdk_20261018/publication/proof/test/empty", "-cp", "/mnt/data/m3sdk_20261018/publication/proof/test/classes", "com.m3.collections.M3MaskWindowKernelSelfTest", "--missing"]
M3_MASK_WINDOW_PASS checks=25 trace=04ae8e18032cc5a498b63324c102fed7ba99f27c3b0b3baa15c01e16c2eee8fa
```

## 092 binary-matrix

Exit: 0; expected failure: false; stdout SHA-256: `7e68774530aaa7e738962f422dab2f8210735613b8b93e5f50b7c572afc04edc`

```text
["java", "-Xmx32m", "-Xcheck:jni", "-Djava.library.path=/mnt/data/m3sdk_20261018/publication/proof/test/native", "-cp", "/mnt/data/m3sdk_20261018/publication/proof/before/classes", "com.m3.collections.M3MaskWindowKernelSelfTest", "--native"]
M3_MASK_WINDOW_PASS checks=20549 trace=2c45448e20add12d774974ad7ab9ea38274e93b589c1e72bde25873fcb36944a
```

## 093 binary-matrix

Exit: 0; expected failure: false; stdout SHA-256: `7e68774530aaa7e738962f422dab2f8210735613b8b93e5f50b7c572afc04edc`

```text
["java", "-Xmx32m", "-Xcheck:jni", "-Djava.library.path=/mnt/data/m3sdk_20261018/publication/proof/before/native", "-cp", "/mnt/data/m3sdk_20261018/publication/proof/test/classes", "com.m3.collections.M3MaskWindowKernelSelfTest", "--native"]
M3_MASK_WINDOW_PASS checks=20549 trace=2c45448e20add12d774974ad7ab9ea38274e93b589c1e72bde25873fcb36944a
```

## 094 legacy-lint

Exit: 0; expected failure: false; stdout SHA-256: `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`

```text
["gcc", "-std=c17", "-Wall", "-Wextra", "-Wconversion", "-Wshadow", "-Wpedantic", "-Werror", "-I/usr/lib/jvm/java-21-openjdk-amd64/include", "-I/usr/lib/jvm/java-21-openjdk-amd64/include/linux", "-fsyntax-only", "/mnt/data/m3sdk_20261018/publication/recipe/legacy/m3_index_mask_compactor.c"]
[stdout empty]
```

## 095 legacy-compile

Exit: 0; expected failure: false; stdout SHA-256: `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`

```text
["gcc", "-std=c17", "-Wall", "-Wextra", "-Wconversion", "-Wshadow", "-Wpedantic", "-Werror", "-I/usr/lib/jvm/java-21-openjdk-amd64/include", "-I/usr/lib/jvm/java-21-openjdk-amd64/include/linux", "-fsanitize=undefined", "-fno-sanitize-recover=all", "-O2", "-fPIC", "-shared", "/mnt/data/m3sdk_20261018/publication/recipe/legacy/m3_index_mask_compactor.c", "-o", "/mnt/data/m3sdk_20261018/publication/proof/legacy-native/libm3_collections_mask.so"]
[stdout empty]
```

## 096 legacy-runtime

Exit: 0; expected failure: false; stdout SHA-256: `7e68774530aaa7e738962f422dab2f8210735613b8b93e5f50b7c572afc04edc`

```text
["java", "-Xmx32m", "-Xcheck:jni", "-Djava.library.path=/mnt/data/m3sdk_20261018/publication/proof/legacy-native", "-cp", "/mnt/data/m3sdk_20261018/publication/proof/test/classes", "com.m3.collections.M3MaskWindowKernelSelfTest", "--native"]
M3_MASK_WINDOW_PASS checks=20549 trace=2c45448e20add12d774974ad7ab9ea38274e93b589c1e72bde25873fcb36944a
```
