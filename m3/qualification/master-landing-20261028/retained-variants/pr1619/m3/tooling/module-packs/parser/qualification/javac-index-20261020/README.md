<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original javac AST → canonical M3AST primitive lanes

**This modifies M3JDK21 itself, not an M3-over-JDK integration layer.**
The original MIndexAST/MIndexFile lineage and existing M3ASTIndex Builder are
the only detached syntax owner. `M3ASTIngress.javac` consumes the caller's
already-parsed `CompilationUnitTree`, original `Trees.getSourcePositions()`,
and exact `M3File` source generation. It never creates a compiler,
calls parse/analyze, invokes an OpenRewrite parser or reads the filesystem.

## Qualified source owners

- `M3ASTIndex`: additive JAVAC domain; optional 4-byte signed node-token
  plane (positive interned identifiers, negative interned literal values).
  New STATIC/FINAL/INTERFACE bit flags preserve the original flags word.
  Name/literal dictionary is scoped to the immutable source generation,
  not a global interner or copied source spelling.
- Lazy `line(node)`, `spanLines(node)` derive exact CR/LF/CRLF positions
  from the original UTF-16 M3File owner. Only the first demand prepares the
  int[] of line beginnings, not one Line object per node.
- `firstEnclosing(node,kind)` traverses original primitive parents.
  `exactSha256` computes UTF-16BE source-interval SHA-256 without slicing
  or storing an extra String; `structuralSha256` masks identifiers/literals
  and UTF-16 offsets and hashes ordered node kinds, arity and declaration
  modifiers for **candidate clustering only**, never semantic equivalence.
- `M3ASTIngress.javac` walks original javac nodes once in preorder using
  a reused direct-child scanner, primitive pending-parent/depth arrays and
  bounded limits. Compiler trees/TypeMirrors are not retained.
- `M3JavacParser.indexProjection()` gives all consumers the SAME projection
  identity per original task's parse event through `M3AST.Unit.project`.
  This avoids separate Java frontend conversions for compatible consumers.
  The existing M3JavacTree projection remains for compatibility.
- `M3JavacIndexContract` exercises real javac on Java 21: B.class produced
  first, A.java processed using the existing B binary; A's source read
  count does not increase during M3AST projection. Source drift, foreign URI
  and node budgets refuse. Prepared B.class bytes stay unchanged.
- Existing build.py parser lint/compile/test/runtime sequence now includes
  the original-javac indexed projection contract.

No public JDK `String`, M3File storage, JNI symbol, original parser object,
M3 AST class graph, or mandatory Synexia runtime owner is added.
Optional semantic lanes and line indexes are constructed only on demand.

## Scope and source-generation correctness

A source URI alone does not prove unchanged content. The five-argument
`javac(..., parsedSource, limits)` overload checks every original UTF-16
unit against the supplied M3File snapshot; the four-argument overload
requires caller custody of the same parsed generation. The M3AST event
projection uses `unit.source()`'s compiler seal and validates its digest.
Reuse of attributed bindings requires a separate classpath/module/toolchain,
annotation-processor and ABI/dependency-environment seal; a syntactically
unchanged AST cannot authorize stale type facts.

Many Maven plugins, Checkstyle/Error Prone-like linters, FindBugs/SpotBugs,
OpenRewrite, Eclipse JDT and NetBeans should consume this detached fact index
or the original compiler's actual tree through the owner/session. This PR does
NOT automatically intercept their separate invocation/plugin lifecycle or
make already-created independent parser tasks coalesce across JVMs.

OpenRewrite `J.CompilationUnit` depends on optional OpenRewrite 8.x: it must
be ingested from its original object inside the already existing
`M3RewriteFrontend` pack, not added as a mandatory classpath dependency to
the thin parser JMOD. The new compiler-side API supports that follow-on.
`M3FactsWriter` into `<module>/target/m3/facts.sqlite` is not yet
qualified; do not conflate the independent native/SQLite persistence
transaction with source parsing or create one SQLite DB per live node.

## Reproducible recipe and acceptance

Named existing-source class-backed Maven/OpenRewrite recipes:
`m3/tooling/shared-frontend/recipe-custody/datasets/m3-javac-index-20261020`.
Four hash-sealed Java targets, one hash-sealed parser build script, exact
before/after snapshots. Catalogue decisions and donor-license dispositions
are retained under `m3/tooling/module-packs/parser/qualification/javac-index-20261020`.

```sh
root="$PWD/m3/tooling/shared-frontend/recipe-custody"
mvn -B -ntp -f "$root/pom.xml" -Pcontract \
  -Dm3.dataset=m3-javac-index-20261020 \
  "-Drecipe.owner.root=$root/sdk-owner" verify
python3 "$root/receive.py" --dataset m3-javac-index-20261020 \
  --recipe-owner "$root/sdk-owner" --target "$PWD" --check

python3 m3/tooling/module-packs/parser/build.py --jdk-home "$JAVA_HOME" \
  --out /fresh/parser-proof --phase lint
python3 m3/tooling/module-packs/parser/build.py --jdk-home "$JAVA_HOME" \
  --out /fresh/parser-proof --phase compile
python3 m3/tooling/module-packs/parser/build.py --jdk-home "$JAVA_HOME" \
  --out /fresh/parser-proof --phase test
python3 m3/tooling/module-packs/parser/build.py --jdk-home "$JAVA_HOME" \
  --out /fresh/parser-proof --phase runtime
```

**Draft/hold** until the actual Maven/OpenRewrite SDK, parser linked-runtime,
M3JDK21 full source/HotSpot/jtreg and target Maven/Gradle/JDT consumers have
passed on exact resulting bytes. SHA-256/source custody alone is not such proof.
