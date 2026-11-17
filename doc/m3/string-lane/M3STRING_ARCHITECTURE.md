# M3String architecture of the string absorption

Status: design note, documentation before code. It records the owner decisions of 2026-11-17 and the plan that follows
from them. Each implementation PR names the section it implements.

## 1. Owner decisions

| # | Owner words (2026-11-17) | Decision |
|---|---|---|
| 28 | "That is exactly the reason why string needs to be so efficient" | Every family PR carries measured kernels and a timing table: against the donor, and against the naive version or the merged code. |
| 29 | "jul is the default logger" | java.util.logging is the default logger; jdk.m3.search ships the relocated slf4j-jdk14 JUL provider (#3219). |
| 30 | "apache ftl and the likes ... still need to be absorbed" | Engines: FreeMarker, Velocity, StringTemplate 4, Pebble; Mustache.java, Handlebars.java, JTE, Jinjava; Thymeleaf with attoparser and unbescape; Commons JEXL, MVEL, Jakarta EL, OGNL. |
| 31 | "M3String is so important to this" | Every kernel has an M3 path in the same PR: prepared facts answer in O(1) where they decide, otherwise bulk windows; never `charAt` per unit. |
| 32 | (dialog, all four options) | Results stay composite; kernels live in M3String; M3 paths in every kernel; a public M3String API through a `jdk.m3` facade. |
| 33 | "inline them in atomized way", "consolidate superset additively and optimize for m3string" | Atomize the donors first and inline only new atoms. Each kernel is one atom. The whole engines are inlined as atoms. Everything consolidates into one additive superset, optimized for M3String. |

## 2. Kernel placement

**Today** (families 1-3 and the leaf24 follow-up), each absorbed operation is a public `java.lang.String` method:

- A flat Latin-1 string is read on its `value` bytes.
- UTF-16 text is read in bulk windows (`getChars`, `SCAN_WINDOW` = 512 units, with a surrogate pair never split
  between two windows).
- M3 storage first asks its prepared `M3StringFacts`:
  - unit signals (`mayContainCodeUnit`);
  - exact ASCII regex classes (`regexAllClasses`, `regexAnyClasses`);
  - strip bounds (`stripStart`, `stripEnd`);
  - `ascii` and `latin1`.

  When the facts do not decide, M3 storage is read in the same windows.

**Target** (decision 32b):

- **Kernels in M3String.** The scan or transform of each family moves into `java.lang.M3String` and reads its owners
  segment by segment. It uses the leaf cursor `M3StringLeafCursor`, and atoms are read without a window copy.
- **String delegates.** `String` delegates to M3String when `m3() != null`. When M3 storage is on, a flat string is
  admitted first (`ensureM3()`), so its facts are computed once and kept, as the stored-metadata rule says.
- **Flat path stays.** The flat byte path remains for VMs that run without M3 storage.

**Order of migration.** One family per PR, in the order of the landed families. Each PR keeps the String-level code
as the oracle of the M3String kernel.

## 3. Composite results (decision 32a)

Methods that build a string return an M3 composite when M3 storage is on: escape, replace, pad, wrap, join, abbreviate,
remove, and template output.

- **No copy.** The result is made of slices of the input (`M3String.slice`) and the inserted pieces, joined through
  the pool (`M3StringPool.concat`).
- **Unchanged answers.** An unchanged result returns `this`. For example, an escape whose facts rule out every
  escapable unit (`<`, `>`, `&`, `"`, `'`) returns the string without reading it.
- **Fallback.** When M3 storage is off, or the pool refuses (budget), the flat builder path is used. The answer is the
  same either way.

## 4. Public facade (decision 32d)

**Location.** The existing package `jdk.m3.text` in java.base (26 classes today, among them `M3StringPiece` and
`M3BigString`). No new package.

**Contents.** One final class of static methods:

- build a composite from pieces and slices;
- walk the segments of a string (start and length of each leaf, no copying);
- read its prepared facts (ASCII, Latin-1, code-point count, strip bounds).

**Admission.** The class name and each member are proposed in the facade PR and admitted by the owner before they
land. No public member is added without that admission.

## 5. Donor atomization and the superset (decision 33)

### Donors

18 jars from Maven Central, binary and sources, SHA-1 checked; 3,466 Java files in total:

- freemarker 2.3.35
- velocity-engine-core 2.4.1, velocity-tools-generic 3.1
- ST4 4.3.4
- pebble 4.1.3
- mustache.java compiler 0.9.14
- handlebars 4.5.8
- jte-runtime 3.2.5
- jinjava 2.8.5
- thymeleaf 3.1.7, attoparser 2.0.10, unbescape 1.1.8
- commons-jexl3 3.7.1
- mvel2 2.6.0
- expressly 6.0.0, jakarta.el-api 6.0.1
- ognl 3.4.17
- spring-expression 7.0.10

### Pipeline

The pipeline is CPU only and read-only on the donors. It uses the atomize kit: `atomize.sh` with
`M3AtomizeInventory`, then `atom_planes.py`.

1. **Atomize.** Run over each donor and over the M3JDK string baseline: java.lang String, M3String* and the builders,
   plus `jdk.m3.text`, `jdk.m3.precompute`, the index-string package and `java.util.regex`; 161 files.
2. **Planes.** Compute the hash planes of every atom: exact, logic, structure, similarity, SimHash.
3. **Classify** each donor atom by the strongest plane that matches a baseline atom:
   - EXACT or LOGIC: already in M3JDK. It is reused and not inlined.
   - STRUCTURE or NEAR: a candidate variant. The donor jar is run as an oracle against the M3JDK owner. Equal answers
     merge into one atom; different answers become a named variant.
   - ABSENT: a new atom, inlined.
4. **Consolidate across donors.** Atoms that match each other merge into one owner, and the superset is additive: no
   donor behavior is dropped. Differing rules stay as named variants, for example HTML escaping by FreeMarker's rules
   versus unbescape HTML5.
5. **Ledger.** One row per atom: atom id, donor, file, line range, blob hash, license, plane, M3JDK owner, status
   (REUSED, MERGED, VARIANT, INLINED, HELD), proof.

### Placement

Where the whole-engine atoms land (module or packages) is decided with the owner once the atom counts are measured;
see section 7.

## 6. Proof of every kernel

- **Donor oracle.** The donor jar is the oracle, on exhaustive small alphabets plus every BMP unit in context, and
  every supplementary code point where it applies.
- **Three modes on a stock JDK 21.** The head method text is patched into `java.lang.String` together with a test
  stub of M3 storage (`M3StringStub.java.txt`):
  - flat strings;
  - stub M3 storage with prepared facts (`-Dm3.stub.force=true -Dm3.stub.facts=true`);
  - stub M3 storage without facts, read in windows (`-Dm3.stub.force=true`).

  Each mode runs with compact strings on and off, plus window-seam texts (deciding units at indexes 505-520 and
  1018-1030).
- **JUnit in CI.** Each storage form must answer as the flat string. CI runs the tests with
  `-XX:+UseM3StringStorage`, which covers real M3 storage.
- **Timing.** One JVM per kernel and side, the median of 3 forks; the donor range is given when donor timings are
  bimodal.

## 7. Open

- Placement of the whole-engine atoms (section 5), after the atom counts.
- Facade names (section 4).
- UTF-16 `isAlpha` is 0.71x the donor (`leaf23/BENCH_TABLE.tsv`); it is taken up in the M3String kernel pass.
