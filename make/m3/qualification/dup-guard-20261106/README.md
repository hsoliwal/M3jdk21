# DUP_GUARD 2026-11-06: one definition twice after a 3-way merge

Base: master `39b0d9e2c6e90d48ffd0e54402752832133742a6` (2026-11-06). Workspace: a full, non-sparse checkout
scanned with CPU tools only (Python over the working tree; `git blame` only for the origin columns).

## The artifact class

A textual 3-way merge (merge, rebase, fold, superset landing) that receives the same member from both
parents can keep both copies. The result is one definition twice in one scope:

| language | shape | effect |
|---|---|---|
| Java method / constructor | same `name(parameter types)` twice directly inside one type body | javac: `method X is already defined` |
| Java field | same name twice in one type body | javac: `variable X is already defined` |
| Java import | identical import line twice | harmless (JLS 7.5.1 ignores the duplicate) but a merge leftover |
| Java top-level type | same `class X` in two files of one package directory | javac: `duplicate class` |
| C function | same function **definition** (body) twice in one file | cc: `redefinition of X` unless under different preprocessor branches |

Known instances that motivated the leaf: `M3BuildImpactPlan.requireSnapshot` (m3/file; lines 135 from
`2f708ccfd817` 2026-10-30 and 151 from `ce7051f77ca0` 2026-11-02; DIVERGENT bodies) was fixed by PR #2501 /
`8715237996e` before this leaf's base and is only listed here; `m3-fast-search`'s `jni_search.c`
duplicate function was fixed by its lane owner and master is clean (44 definitions, 0 duplicate names).

## The scanner: `dup_guard.py`

Deterministic, idempotent, no LLM, no network. Walks `m3/` and `src/`, skipping `target/`, `vendor/`,
`third_party/`, `retained-variants/` and `(main|test)/resources/`. Java files are masked (comments and
literal contents blanked, offsets preserved) and walked with a tolerant brace parser that keeps a scope
stack: FILE, TYPE (class/interface/enum/record/@interface, anonymous, enum-constant body, local class with a
unique ordinal), METHOD, INIT (expression-embedded braces: field initializers, lambda bodies, annotation
array values, enum-constant arguments), INITIALIZER, BLOCK. Members are collected only directly inside a
TYPE scope:

* methods / constructors: key `name<type parameters>(normalized parameter types)`; parameter names,
  annotations and `final` dropped, generics kept, whitespace removed; type-parameter bounds are part of the
  erasure, so `of<K extends Comparable<? super K>,V>(K,V)` and `of<K,V>(K,V)` are distinct overloads;
  constructors and same-named methods are distinct kinds (JDK's `EditorContainer()` has both)
* fields: key `name` (declarators split at depth 0; `->` never closes a generic)
* imports: normalized line (`import a . b` -> `import a.b`)
* top-level types: `(directory, name)` across files

C/C++ files are `*.c` and `*.h` under `**/native/**` only. Preprocessor lines are blanked (conditional
directives remembered by line), comments and literals masked; a function definition is a depth-0 brace
whose header ends with `identifier(params)` and has no `=`. `.c` keys are the bare name (no overloading);
`.h` keys are `qualified::name(params)`.

Verdicts per duplicate pair (first occurrence is the anchor; a third copy pairs with the first):

| verdict | meaning |
|---|---|
| `PURE_DUP` | member texts byte-identical from the first code token to the closing brace / semicolon |
| `DIVERGENT` | same key, texts differ: never decided here (NEC); both `sha256` blobs in `blob_a`/`blob_b` for the owner |
| `FALSE_POSITIVE` | a preprocessor conditional lies between the two C bodies, or a low-confidence header parse |

`DUP_CANDIDATES.tsv` columns: `path kind signature scope line_a line_b span_a span_b bodies_identical
diff_class blob_a blob_b origin_a origin_b verdict note` (`origin_* = <commit12>@<author date>` from
`git blame -L`; `diff_class` is IDENT / WS_ONLY / COMMENT_ONLY / CODE).

## Counts (pristine tree at the base)

| verdict | kind | rows |
|---|---|---|
| PURE_DUP | import | 30 |
| DIVERGENT | any | 0 |
| FALSE_POSITIVE | C function under `#if/#else` | 130 (60 src/java.desktop, 30 m3/ports, 28 src/java.base, 6 m3/fs, 4 jdk.hotspot.agent, 2 jdk.jdwp.agent) |

28,284 files scanned (Java + native C), 6 parser warnings (C files whose braces are unbalanced without the
preprocessor: launcher/main.c, zlib/trees.c, ParallelogramUtils.h, ftcalc.c, cmsio0.c, splashscreen_png.c).
No duplicate method, constructor, field or top-level type is left on master; the only remaining class is
the harmless duplicate import.

Scan v2 at the earlier base `48537c8e` additionally reported `M3BuildImpactPlan.requireSnapshot` DIVERGENT
(lines 135/151) -- that row disappeared when #2501 landed on master during this leaf.

## The fix: `apply.py` (recipe `DUP_GUARD_REMOVE_SECOND v1`)

Removes the SECOND byte-identical definition of each `PURE_DUP` row, re-scanning the file itself so line
shifts never matter, one adjacent blank line collapsed, leading javadoc/annotations included in the removed
span. Per file the preimage and postimage `sha256` are pinned in `RECIPE_LEDGER.tsv` together with the
removed member's `sha256` (== `blob_b` of the candidate). A second run finds the postimage and reports
`ALREADY_APPLIED` without rewriting the ledger (fixed point: ledger sha unchanged, `git diff` unchanged).
`DIVERGENT`, `FALSE_POSITIVE`, `--skip` keys and paths outside `--include` are never touched.

Applied (14 files, 14 deleted lines, all `import`):

* `m3/collections/src/test/java/jdk/m3/collections/M3StoredRepresentativeContract.java`
  (`import java.util.function.BiPredicate;` lines 34 + 36; origins `d1bd4e3447e5` / `747bcc185024`, 2026-10-19 additive restore)
* 13 files under `src/jdk.m3.graphics/share/classes/jdk/m3/graphics/{paragraph,shaper,skottie,sksg,svg}/`
  (`import jdk.m3.graphics.*;` twice; origin `da6112257f9b` 2026-10-31, the Skija package rename collapsed
  `io.github.humbleui.skija.*` and a sibling on-demand import into one line)

Listed, not fixed (16 rows, `RECIPE_LEDGER.tsv` status `SKIPPED:*`, also in `DUP_GUARD_ALLOW.tsv`):

* 15 identical imports in OpenJDK-upstream sources (origins are OpenJDK commits 2007-2023: AWTEventMulticaster,
  ShapeGraphicAttribute, BasicPanelUI, BasicViewportUI, MetalToggleButtonUI, OutlineTextRenderer,
  SunMinMaxPage, XChoicePeer, JdbcRowSet, dsig Policy, xsltc SAX2DOM, javac ClassFinder + JavaCompiler,
  SA SALauncher + VM). No JDK image build ran in this leaf, so no gate: `SKIPPED:OUT_OF_GATE_SCOPE`.
* `make/m3/tooling/module-packs/parser/verification/one-pass-20261020/qualification/parser-final/compiler-source/.../JavaCompiler.java`:
  a frozen qualification evidence copy mirroring the upstream duplicate: `SKIPPED:QUALIFICATION_EVIDENCE_COPY`.

## Gates

See `GATES.tsv`. Summary: `mvn -DskipTests package` of `m3/collections` GREEN before and after; `javac
--release 21 -Xlint:all` of `src/jdk.m3.graphics` (226 sources) GREEN before and after with the identical
63-warning set (pre-existing `missing-explicit-ctor`/`this-escape`/`fallthrough`/`module`; `-Werror` is red
on both sides for that reason); the 19 class files that differ on disk are `javap -p -c` identical
(LineNumberTable shift only); `jdk_naming.py plan --only <touched dirs>` stays at 0.

## Running the gate in CI

```
python -I make/m3/qualification/dup-guard-20261106/dup_guard.py check --root . \
    --allow make/m3/qualification/dup-guard-20261106/DUP_GUARD_ALLOW.tsv
```

exits 1 on any `PURE_DUP` or `DIVERGENT` row not in the allow list (one blocking line per row on stdout),
0 otherwise; ~10 s on 28k files with all cores, no git needed. Drop `--allow` to see the 16 listed rows.
`scan --blame --blame-verdicts PURE_DUP,DIVERGENT` regenerates `DUP_CANDIDATES.tsv` with origins;
`file <path>` prints one file's member table for debugging the parser.

## Limits

Textual parser, not javac: unusual formatting (code after `}` on the same line as the next member) is
refused by `apply.py` with `HOLD:*` rather than guessed; C++ (`.cpp/.hpp`) is out of scope (overloads,
initializer lists); erasure-equal methods with differently spelled parameter types
(`java.util.List<X>` vs `List<X>`) are not paired. Files: `dup_guard.py`, `apply.py`, `DUP_CANDIDATES.tsv`,
`RECIPE_LEDGER.tsv`, `DUP_GUARD_ALLOW.tsv`, `GATES.tsv`, `RECIPE_MEMORY.tsv`.
