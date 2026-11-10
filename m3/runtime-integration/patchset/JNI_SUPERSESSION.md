# JNI runtime-integration supersession

Status: custody repair only. No JDK source is modified by this packet.

The original runtime-integration recipe predates later M3 JNI/String work. Its manifest already
allows explicitly reviewed superseded owners so a newer canonical implementation is not mistaken
for source drift.

Current owner:

- path: `src/hotspot/share/prims/jni.cpp`
- Git blob: `81ace3e98b92a1b6c0cf1acdc7440484368e3ae4`
- SHA-256: `95adc8ea2a9035e65b613a7fb4131f8629bc90f81b70008895a77c1e396b05f9`
- last content-changing commit:
  `f74592e363466a59b391fbbe47500c7985c9a691`
- commit subject:
  `feat(m3): inline Class counterparts and verify Java/JNI on the built JDK`

That successor adds the JNI String null-handle guard and migrates String creation to the newer
`java_lang_String::create_from_unicode` / `create_from_str` owners while retaining M3 String
admission. The commit records a matching Linux fastdebug build plus C1/C2 Class probes, JNI semantic
oracles and recipe/installer tests.

The runtime-integration recipe therefore classifies the current file as `superseded`. It does not
rewrite it back to the older patch image. Unknown future hashes remain refused.

Verification obligations remain separate:

- `test_recipe.py` pins this exact current SHA-256 and requires it in the superseded set;
- every other runtime-integration target must still be exact `after` or explicit `superseded`;
- the original runtime patch bytes remain content-addressed;
- arbitrary drift and symlink targets remain refused.

## libjava `jni_util.c` and SA `OopUtilities.java` (2026-10-07)

The consolidate commit `26320a320f` replaced `java.lang.MIndexString` with `java.lang.M3String`
(`String.m3`, owner + packed coordinate) but left two readers on the old layout:

- `src/java.base/share/native/libjava/jni_util.c` (master blob `3552e9ade564428dabb07dc17768909d0bcd838b`,
  SHA-256 `f19521f723ecf9a4cdf639a04b93c0dece27e37f5877d756f007adbdc16d6a16`): `InitializeEncoding`
  cached `GetFieldID(String, "mindex", "Ljava/lang/MIndexString;")` with `CHECK_NULL`, leaving a pending
  `NoSuchFieldError` on every VM start.
- `src/jdk.hotspot.agent/share/classes/sun/jvm/hotspot/oops/OopUtilities.java` (master blob
  `996cd6f7133d4d0f7e301e51b75f511d321146b9`, SHA-256
  `5c76caf1c717e4f05c0f2f8adf87265422c0a0e3571dc5e852f8ceaa476c90ba`): decoded `mindex` segment arrays.

Both are superseded by the hash-pinned crate `m3-jni-sa-m3-field`
(`com.m3.rewrite.backport.M3JniSaM3FieldRecipe`, Java lane `jdk21-hash-pinned/m3-jni-sa-m3-field`,
text lane `jdk21-hash-pinned-text/m3-jni-sa-m3-field`). Postimage SHA-256:
`jni_util.c` `4669c033c1b820ee90eb7fbe9f0c3ead9681312853e5675d360bb8db81e697f7`,
`OopUtilities.java` `f964925e59e6b1ef233d3508a6ba901afc9c4bf1430b60fc6ea0f71d6999cc86`.
The older `runtime.patch` image (MIndexString era) remains stale for these two paths and for the
`M3String` runtime files; regenerating it against current master is a separate leaf (R1).
