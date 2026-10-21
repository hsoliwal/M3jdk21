# Qualified mask-window Java/JNI receiver

Canonical source: [Synexia PR #12120](https://github.com/hsoliwal/com.synexia/pull/12120), exact commit `eaba5869004e320845edb652a73b8f9f2085d305`.
Recipe: `com.synexia.m3jdk.MaskWindowSparse20261017`. `RECEIVER.json` binds four actual SDK outputs to their exact preimages; `PROOF_MIRROR.tsv` binds the tooling mirror to source Git blobs. Synexia remains canonical; the copied authoring tooling creates no target runtime dependency.

The implementation skips state scanning for constant mask expressions and JNI row reads for rejected chunks. Public JDK and library names, primitive cursor/output contracts, the existing native loader and ABI remain unchanged. No retained per-row nodes, new object-state owner or predicate callback bridge is introduced.

Review the [full qualification](tooling/synexia-openrewrite-recipes/recipe-crates/m3-mask-window-sparse-20261017/README.md), [catalogue](../../algorithm-catalog/mask-window-sparse-20261017.tsv), and `NAME_MAPPING.tsv`. Existing filtered/sorted wrapper behavior remains intact. This is a bounded incremental SDK refinement, not completion of all Guava/Eclipse Collections/Spring/Apache/JDK families.

Reproduce the actual Maven/OpenRewrite, Java21 and checked JNI/UBSan proof using the approved offline cache:

```sh
export JAVA_HOME=/path/to/jdk21
python3 m3/collections/qualification/mask-window-sparse-20261017/tooling/synexia-openrewrite-recipes/recipe-crates/m3-mask-window-sparse-20261017/proof/verify.py --output /new/proof-directory --maven /path/to/mvn --m2 /path/to/offline-m2
```

Keep draft: full owning reactor/JUnit, built M3JDK image/jtreg/JCK, GC/JIT matrix, other native platforms, statistical performance and retained/native live-memory attribution remain pending. This does not advance the ordered STRING receiving phase. All existing OpenJDK/third-party licenses remain authoritative; first-party Apache-2.0 LICENSE/NOTICE are retained in the mirror.
