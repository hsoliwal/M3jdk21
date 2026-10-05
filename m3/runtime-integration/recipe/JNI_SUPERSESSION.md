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
