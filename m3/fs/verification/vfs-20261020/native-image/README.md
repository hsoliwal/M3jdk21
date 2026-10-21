# Combined M3FS Java, VFS and native runtime image

The unchanged `m3/fs/tools/package_runtime.py` and existing shared JMOD packer
built this image from the same generated-source M3FS JAR already admitted by
`SOURCE_LOCK.capture.tsv`, the newly built JNI library, the pinned SQLite runtime,
and the five pinned provider dependency modules. The supplied JDK is Temurin
21.0.12.1+1. This is not a source-built OpenJDK/M3JDK21 qualification.

`command-receipt.json` preserves the invocation, actual exit 0, and unchanged
source, JAR, helper and native input hashes. `runtime-proof.json` and `probe.log`
show the mandatory native-required `-Xcheck:jni` probe passed. The probe verifies
M3FS class loading from jrt, full feature bits 2097151, actual filesystem behavior,
and `/proc/self/maps` origins for JNI and SQLite within the linked image. It
uses no explicit native path or Java library path override.

The existing independently compiled named VFS/capture consumer also passed on
this same image using fresh fixtures. `combined-image-proof.json` binds both
probes to the image and preserves the named consumer class hashes. The VFS
consumer's own `NATIVE_SQLITE_PROBE=NOT_RUN` line describes that consumer's local
scope; native behavior is established by the separate passing runtime probe.

All 132 legal comparisons across six vendored JMOD/image modules passed. Original
Apache VFS, dependency and SQLite distribution legal bytes are retained. The
original legal texts and their hashes, JMOD manifests and linked-image release
metadata are included here. The mandatory text-only publication inventory is
`PUBLICATION_FILES.tsv`; compiled binaries, probe classes, databases and fixtures
are excluded from that inventory.
