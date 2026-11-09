# Parser pack: Maven 4 effective-model projection (census2 P2-050), 2026-11-06

**Source.** com.synexia recipe-crates/m3-maven4-effective-20261021, hash-pinned datasets -001 to -008. find_pre.py (HASH_PINNED_3WAY v1) handled each target in one of four ways:
- pinned postimage when master equals the preimage (APPLY);
- NEW when the target is absent on master;
- a 3-way merge from the pinned preimage, located in M3jdk21 history by sha256, when master has drifted;
- held, for the workflow (see below).

| target | verdict |
| --- | --- |
| parser/src/main/java/jdk/m3/parser/M3ASTAuthoritativeModels.java | 3-way from base 33b6a40f138c. One add/add hunk (two provenance header comments) resolved by union. Adds maven4Effective(M3File, Object ModelBuilderResult, Limits) via reflection and the shared mavenEffectiveModel path. |
| parser/real-providers/src/test/java/jdk/m3/parser/tests/M3Maven4ProviderContract.java | NEW |
| parser/real-providers/maven4/{README.md, pom.xml, verify.py} | NEW |
| parser/real-providers/{CATALOGUE.csv, README.md} | APPLY (master == pinned preimage) |
| .github/workflows/m3-ast-real-providers-20261019.yml | HELD |

**Why the workflow job is held.** The maven4-fork job builds hsoliwal/maven at 2993571eedbe from source. It then runs verify.py, which requires the working M3ASTAuthoritativeModels to equal the crate's exact 2026-10-21 postimage. The merged owner keeps master's own provenance header, so the job would fail from the first run. It needs a re-sealed verify.py first.

## Gate

pg/parser_gate.sh, PARSER_PACK_GATE v3. It mirrors module-packs/parser/build.py on stock JDK 21.0.9, on master 4cdaa6073c08.

| step | live | leaf |
| --- | --- | --- |
| compiler-patch (5 M3 javac owners) | 0 errors | 0 errors |
| parser module, -Xlint:all,-module -Werror | 0 errors, 0 warnings | 0 errors, 0 warnings |
| all consumers, -Xlint:all -Werror | 0 errors, 0 warnings | 0 errors, 0 warnings |
| build.py CONTRACTS | 22/23 | 22/23, no regression |

The one failing contract on both sides is M3FactsTransactionContract, which fails on Windows because the SQLite file is readable through JNI.

The M3Maven4ProviderContract proof is NOT_RUN. Its compile and run need the Maven 4 API jars and 16 source-built Maven 4 fork jars (verify.py), and this host has only the rc-2 poms.
