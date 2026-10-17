# CollectionViews in java.util — first file migration

Canonical recipe: Synexia PR #12015, commit 4541f77950157f9d75d95b24a5caf785cf4a0341,
`synexia-m3-recipe/recipes/m3-java-util-views-20261017`. Target baseline is PR #992 head
11dde1eac33cea2a8b90baba8c155eabec341144. The six files in targets.tsv are exact recipe outputs.

The sole implementation is now java.util.CollectionViews inside java.base, without public M3
prefixes. Existing com.m3.collections.M3CollectionViews is compatibility forwarding only; it
holds no storage, predicate or private wrapper classes. A client compiled before the move was
executed unchanged after it. This compatibility artifact now REQUIRES the matched java.base;
it is not a standalone stock-JDK library. Private implementation class names/layout do change.
CollectionViews is an M3JDK extension, not an official Java SE API.

The prior deferred, ordered predicate-array refinement is preserved. 20,000 nested filters
succeed with a 256 KiB stack. Existing dynamic ArrayList snapshot storage and backed selection
cursors remain authoritative. No per-entry retained wrappers or predicate-result cache is added;
no generic selection mask is cached without a validity/version contract. The plan retains O(depth)
metadata and cold preparation costs; no benchmark speedup or GC-pause reduction is established.

Local full source-recipe verifier: 472 recipe checks across 64 partial states; 14,674 baseline
checks; 14,700 generated checks in each of default/interpreter/C1/C2; 77 bootstrap checks;
unchanged precompiled client and named-module client; two compiled mutants rejected.
Real JNI with strict C17, fatal UBSan and -Xcheck:jni: 15,732 checks and 1,027 run queries.
The native kernel is unchanged verification-only code; no production native acceleration.

Run the independent target proof (Linux/JDK21/GCC/Python) with a fresh output directory:

```sh
python3 m3/runtime-integration/java-util-views/verify.py /absolute/new-output
# Maven target orchestration, when available:
mvn -B -ntp -f m3/runtime-integration/java-util-views/pom.xml verify -Dproof.output=/absolute/new-output
```

For source replay/refusal/full binary-client proof, use the canonical Synexia recipe README.
Its hydrate tool uses these installed files and the pinned git baseline, never a live mutable
remote branch. The target driver compiles only the generated CollectionViews into a patched
java.base on an installed Java21; it does NOT silently compile/qualify other modified JDK sources.

## DRAFT / HOLD

Maven/OpenRewrite SDK, full reactors, rebuilt matched JDK/jtreg/JCK, complete provider/API census,
other platforms, performance, and central aggregate naming-map admission remain pending. All
Apache notices are retained; this is NOT implicit relicensing. Core integration must satisfy the
existing explicit-relicense/target-owned policy before merge. The new public name may also
require review of source wildcard-import collisions; global source compatibility is not claimed.

M3String and its related owner/range/tuple/mask/native families stay intact. Each future file must
map toward the existing String or appropriate java.util contract, not flatten the representation
family or rename prefixes blindly. Full convergence to the JDK/Eclipse/Spring/Guava superset,
dynamic arrays and masks, compiler borrowing, GC and shared-address residency is still OPEN.
