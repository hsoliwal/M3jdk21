<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 compile-time String precompute

Status: design (D2C) + first vertical leaf proven in `m3/ports/compiletime` (constant regex).
No `src/**` change. Authority: stock JDK operations are the oracle for every lane.

Directive (2026-10-13): a Java program carries a large amount of compile-time-known String data
(regex, format strings, keys, needles, concat recipes, switch labels). M3 turns it into
build-time facts and plans so runtime String/regex work becomes lookups and arithmetic on ids.

## 1. Rules carried

- Lineage invariants 6 and 12 (`m3string-synexia-lineage.md`): precompute is never a second
  spelling store, and length-proportional lanes stay in bounded owners, never in `M3String`.
- Lineage invariant 8: absence or eviction of precompute cannot change String semantics.
- M3-SYNEXIA-RECEIVER-1 `STATIC_REGEX_PRECOMPUTE_IS_PRIORITY`: constant regex is the first lane.
- SYNEXIA-LLM-RECIPE-FIRST-2: delivery is a hash-pinned recipe crate with a fixed point and
  refusal on drift (`m3-compiletime-regex-precompute`).
- FOSS donor first: `m3/ports/compiletime/FOSS_REUSE_DECISION.tsv` (ASM direct import,
  OpenRewrite wrap, Synexia planner rules adapted, RE2/J evidence only, GraalVM/jlink pattern
  only, Error Prone pattern only).

## 2. Mechanical inventory (CPU only, exact)

Method: one JVM (`-Xmx2g`), `M3CompileTimeStringInventory` over

- JDK 21 modules: `jrt:/` of `C:\Program Files\Java\jdk-21` (21.0.9+7-LTS-338), 70 modules
  (4 have no classes: `java.se`, `jdk.jdwp.agent`, `jdk.internal.vm.compiler`,
  `jdk.internal.vm.compiler.management`);
- the vendored universe: all 121 `*-m3-source-1.jar` in `K:/m3-work/m2-build`
  (`module-info` and `META-INF/versions/**` classes skipped and counted).

Each class is read with ASM `ClassReader`; call-site operands are proven constant with ASM
`Analyzer<SourceValue>`/`SourceInterpreter` (an operand counts as constant only when its single
source instruction is an `ldc` String). Zero class-parse failures, zero analyzer failures, zero
methods over the analyzer budget. Wall time: about one minute for 65,506 classes.

Outputs (in `m3/ports/compiletime/inventory/`):

- `COMPILETIME_STRING_INVENTORY.tsv`: `corpus unit category metric value`, every metric per
  module/jar;
- `COMPILETIME_STRING_UNITS.tsv`: one wide row per module/jar with the headline metrics (awk
  projection `units_projection.awk` of the long table);
- `COMPILETIME_STRING_SUMMARY.tsv`: per-corpus and union totals, including cross-class
  duplicate counts from global content-keyed tables;
- `CONSTANT_REGEX_CORPUS.tsv`: every distinct constant (pattern, flags) key with its
  classification, sites per corpus and APIs.

| Item | Metric | JDK 21 modules | Vendored jars | Total |
|---|---|---:|---:|---:|
| classes scanned | `all.classes` | 27,630 | 37,876 | 65,506 |
| **a** ldc String sites | `a.ldc_string_sites` | 1,179,254 | 155,588 | 1,334,842 |
| a ldc UTF-16 units | `a.ldc_string_units` | 15,606,007 | 3,641,327 | 19,247,334 |
| a CONSTANT_String pool entries | `a.constant_pool_string_entries` | 957,289 | 131,499 | 1,088,788 |
| a (class, string) pairs | `a.ldc_class_string_pairs` | 945,224 | 126,437 | 1,071,661 |
| a distinct ldc strings | `a.ldc_distinct` | 307,533 | 70,750 | 372,562 |
| a distinct strings in 2+ classes | `a.ldc_in_2plus_classes` | 59,557 | 16,533 | 75,464 |
| a redundant per-class copies | `a.ldc_redundant_class_copies` | 637,691 | 55,687 | 699,099 |
| a redundant copy UTF-16 units | `a.ldc_redundant_class_copy_units` | 8,035,238 | 912,996 | 8,995,498 |
| **b** Pattern.compile(String) sites | `b.pattern_compile_sites` | 157 | 386 | 543 |
| b Pattern.compile const | `b.pattern_compile_const` | 105 | 304 | 409 |
| b Pattern.compile(String,int) const (const flags) | `b.pattern_compile_flags_const` | 7 | 37 | 44 |
| b String.matches const | `b.string_matches_const` | 38 | 25 | 63 |
| b String.replaceAll const | `b.string_replace_all_const` | 88 | 196 | 284 |
| b String.replaceFirst const | `b.string_replace_first_const` | 37 | 23 | 60 |
| b String.split const | `b.string_split_const` | 278 | 218 | 496 |
| b String.split(String,int) const | `b.string_split_limit_const` | 20 | 26 | 46 |
| b Pattern.matches const | `b.pattern_matches_const` | 0 | 6 | 6 |
| b const sites: split fastpath (no Pattern) | `b.regex_class_SPLIT_FASTPATH` | 228 | 196 | 424 |
| b const sites: pure literal | `b.regex_class_PURE_LITERAL` | 47 | 55 | 102 |
| b const sites: required literal | `b.regex_class_REQUIRED` | 52 | 131 | 183 |
| b const sites: real regex, no literal requirement | `b.regex_class_UNCONSTRAINED` | 246 | 466 | 712 |
| b distinct (pattern, flags) keys | `b.regex_distinct_keys` | 241 | 341 | 556 |
| b distinct keys pure literal | `b.regex_distinct_keys_PURE_LITERAL` | 31 | 31 | 54 |
| b distinct keys required literal | `b.regex_distinct_keys_REQUIRED` | 48 | 76 | 124 |
| b distinct keys unconstrained | `b.regex_distinct_keys_UNCONSTRAINED` | 162 | 234 | 378 |
| **c** String.format sites | `c.string_format_sites` | 778 | 1,988 | 2,766 |
| c String.format const | `c.string_format_const` | 637 | 1,833 | 2,470 |
| c String.format(Locale,..) const | `c.string_format_locale_const` | 1 | 147 | 148 |
| c formatted() const receiver | `c.string_formatted_const` | 35 | 0 | 35 |
| c printf const (PrintStream/PrintWriter) | `c.stream_printf_const` | 45 | 56 | 101 |
| c format const (PrintStream/PrintWriter) | `c.stream_format_const` | 100 | 9 | 109 |
| c Formatter.format const | `c.formatter_format_const` | 62 | 10 | 72 |
| c MessageFormat.format static const | `c.message_format_static_const` | 2 | 33 | 35 |
| c new MessageFormat(const, Locale) | `c.message_format_new_locale_const` | 62 | 0 | 62 |
| c Formatter plans SIMPLE (%s/%d only) | `c.formatter_plan_SIMPLE` | 631 | 1,750 | 2,381 |
| c Formatter plans GENERAL | `c.formatter_plan_GENERAL` | 224 | 283 | 507 |
| c Formatter plans NO_CONVERSION | `c.formatter_plan_NO_CONVERSION` | 26 | 35 | 61 |
| c distinct format strings | `c.format_string_distinct` | 719 | 1,114 | 1,805 |
| **d** indy makeConcatWithConstants sites | `d.indy_concat_sites` | 11,153 | 3,034 | 14,187 |
| d distinct recipes | `d.concat_recipe_distinct` | 6,884 | 2,180 | 8,983 |
| d dynamic arguments | `d.indy_concat_dynamic_args` | 16,692 | 5,189 | 21,881 |
| d recipe literal UTF-16 units | `d.indy_concat_recipe_literal_units` | 254,860 | 107,081 | 361,941 |
| **e** String switch sites (hashCode+switch) | `e.string_switch_sites` | 249 | 1,074 | 1,323 |
| e case labels (equals on ldc) | `e.string_switch_case_labels` | 1,358 | 3,153 | 4,511 |
| e hash keys | `e.string_switch_hash_keys` | 1,358 | 3,170 | 4,528 |
| e indy SwitchBootstraps sites | `e.indy_switch_bootstrap_sites` | 39 | 0 | 39 |
| **f** annotations | `f.annotations` | 14,079 | 42,968 | 57,047 |
| f annotation String values | `f.annotation_string_values` | 1,615 | 7,469 | 9,084 |
| f annotation enum values | `f.annotation_enum_values` | 345 | 6,415 | 6,760 |
| f enum classes | `f.enum_classes` | 768 | 2,091 | 2,859 |
| f enum constant names | `f.enum_constant_names` | 7,263 | 6,707 | 13,970 |
| f ResourceBundle key sites | `f.resource_bundle_key_sites` | 768 | 16 | 784 |
| f ResourceBundle key const | `f.resource_bundle_key_const` | 600 | 1 | 601 |
| f getProperty-family key sites | `f.property_key_sites` | 961 | 533 | 1,494 |
| f getProperty-family key const | `f.property_key_const` | 795 | 436 | 1,231 |
| f setProperty key const | `f.property_key_set_const` | 118 | 22 | 140 |
| f getenv key const | `f.env_key_const` | 33 | 38 | 71 |
| **g** StringBuilder/StringBuffer.append(const) | `g.string_builder_append_const` | 12,643 | 28,178 | 40,821 |
| g exception-message const | `g.exception_message_const` | 10,656 | 8,343 | 18,999 |
| g exception-message const units | `g.exception_message_const_units` | 332,253 | 311,785 | 644,038 |
| g ConstantValue String fields | `g.field_constant_value_strings` | 7,765 | 4,532 | 12,297 |
| g ConstantValue UTF-16 units | `g.field_constant_value_units` | 307,314 | 770,622 | 1,077,936 |
| g String.equals(const) | `g.string_equals_arg_const` | 4,032 | 1,067 | 5,099 |
| g const.equals(x) | `g.string_equals_receiver_const` | 1,164 | 1,452 | 2,616 |
| g equalsIgnoreCase(const) | `g.equals_ignore_case_arg_const` | 953 | 87 | 1,040 |
| g startsWith(const) | `g.search_starts_with_const` | 1,042 | 1,132 | 2,174 |
| g endsWith(const) | `g.search_ends_with_const` | 554 | 402 | 956 |
| g contains(const) | `g.search_contains_const` | 206 | 485 | 691 |
| g indexOf(const) | `g.search_index_of_const` | 211 | 104 | 315 |
| g lastIndexOf(const) | `g.search_last_index_of_const` | 69 | 36 | 105 |
| g replace(const, ...) | `g.search_replace_literal_const` | 123 | 319 | 442 |
| g Class.forName(const) | `g.class_for_name_const` | 77 | 225 | 302 |
| g reflection member name const | `g.reflection_member_name_const` | 235 | 355 | 590 |
| g MethodHandles.Lookup member name const | `g.method_handle_member_name_const` | 184 | 59 | 243 |
| g System.Logger template const | `g.log_template_system_logger_const` | 425 | 28 | 453 |
| g SLF4J template const | `g.log_template_slf4j_const` | 0 | 83 | 83 |
| g JUL template const | `g.log_template_jul_const` | 56 | 98 | 154 |

Reading the table:

- **a** `ldc` dominates by volume: 1,334,842 sites, 372,562 distinct strings, 699,099 redundant
  per-class copies. `jdk.localedata` alone holds 967,909 of the JDK's 1,179,254 `ldc` sites
  (locale tables as code): a facts image must exclude cold locale/resource tables and target
  strings that reach String operations (needles, keys, regex, formats).
- **b** constant regex: 1,421 constant regex operand sites (of 1,736 regex call sites); 424 are
  `split` fastpath sites the JDK already serves without a `Pattern`, the other 997 resolve to
  556 distinct (pattern, flags) keys (no site had a non-constant flags operand).
  54 keys are pure literals and 124 carry a required-literal formula: 178 of 556 keys (32.0%)
  are served by the first leaf; 378 are real regexes with no literal requirement (next lane:
  v7 execution image).
- **c** format: 3,050 constant format/message-format operand sites; 2,381 of the parsed
  `Formatter` sites are SIMPLE (`%s`/`%d` only), the first runtime plan candidates.
- **d** 14,187 indy concat sites over 8,983 distinct recipes; already linked once per call
  site by `StringConcatFactory`. The vendored universe also has 40,821 constant
  `StringBuilder.append` operands (pre-indy javac targets).
- **e** 1,323 string switches with 4,511 case labels.
- **g** constant needles (`equals`, `startsWith`, `endsWith`, `contains`, `indexOf`, `replace`,
  `equalsIgnoreCase`) are the largest hot-path lane after `ldc`; exception messages (18,999
  constant sites) and log templates are cold and get no precompute.

## 3. Category to precompute map

Every precompute below is a candidate or a mathematically complete plan with the stock JDK
operation as oracle; a miss, a refusal or an eviction leaves the stock path untouched
(lineage invariant 8).

| Cat | Compile-time site | Precompute | Image lane (ints) | Runtime receiver | Status |
|---|---|---|---|---|---|
| a | `ldc` String constants, `ConstantValue` fields | `M3StringFacts` fixed-size facts per distinct constant (length, Java hash, char-class bits, bigram/trigram presence, code-point count) | `facts[n][k]` keyed by content key | `M3StringPool` admission of an `ldc`-resolved constant | design |
| b | `Pattern.compile`, `String.matches/replaceAll/replaceFirst/split`, `Pattern.matches`, `Scanner` | requirement formula (literal atoms, AND/OR), M3TQ trigram keys per atom, pure-literal flag, matcher-state admission bit | `M3CTRX01` (this leaf) | `M3ConstantRegexGate` (port); later `Pattern.compile` → `m3Tq`/requirement slot | **leaf 1, proven** |
| b | same keys, regular subset | v7 `MIndexRegexProgram` execution image (Thompson NFA / ASCII DFA lanes) | program lanes of RECEIVER-1 | `Matcher` fast engine for admitted keys | next leaf |
| b | `split` with a non-fastpath pure literal (e.g. `", "`) | split plan: literal + limit semantics | 2 ints + literal coordinates | `String.split` | design |
| c | `String.format/formatted`, `PrintStream/PrintWriter.printf/format`, `Formatter.format` | `M3FormatPlan`: `[LITERAL begin end]` coordinates into the format string + `[CONVERSION conv flags width precision arg]` | `plan[]` per distinct format | `Formatter.parse()` cache by content key; SIMPLE plans (`%s`/`%d` only) take a concat path | plan parser in port; runtime design |
| c | `MessageFormat` constant patterns | argument-element count + parsed element image | per distinct pattern | `MessageFormat.applyPattern` | design |
| d | indy `makeConcatWithConstants` recipes | concat plan: literal-run coordinates in the recipe, argument slots, constant length for presizing | `recipe[]` per distinct recipe | `StringConcatFactory` linkage (already per call site) | low priority |
| d/g | `StringBuilder/StringBuffer.append(ldc)` chains in Java 8-targeted jars | the same concat plan recovered from append chains | as above | build-time rewrite (recipe lane) | design |
| e | `String.hashCode()` + `lookupswitch/tableswitch` + `equals(ldc)` | perfect-hash table over case labels keyed by M3 content key | `slots[]`, `labelOrdinal[]` | switch dispatch via M3 string id | design |
| f | annotation strings, enum constant names, `ResourceBundle` keys, `System/Properties.getProperty`, `getenv` keys | interned M3 key ids (TIL-bounded) + per-enum perfect hash for `valueOf` | `keys[]` | key lookup by id | design |
| g | `indexOf/contains/startsWith/endsWith/replace/equals/equalsIgnoreCase` constant needles | `M3StringSearchPrecompute` KMP/BMH/trigram facts per distinct needle | `needle[]` | the existing weak-owner search cache, prefilled | design |
| g | `Class.forName`, reflection/`MethodHandles.Lookup` member names, `Charset.forName` | resolved-name ids (cold path) | — | none (cold) | no precompute |
| g | exception messages, log templates | none: cold paths (HPCPOP) | — | — | no precompute |

## 4. Where precompute is produced

| Producer | Sees | Strength | Weakness | Role |
|---|---|---|---|---|
| ASM pass over class files (this leaf: `M3ClassScan` + `M3ConstantRegexHarvester`) | the exact compiled truth: javac constant folding, indy recipes, string-switch lowering, `ConstantValue` | works on any jar, including binary-only third-party code; one JVM; deterministic; minutes for the whole universe | no source positions; cannot rewrite call sites | **canonical producer** of every sidecar image |
| OpenRewrite/LST recipe at source level | source literals, `static final` fields, call-site intent | can rewrite call sites (e.g. hoist `s.matches(CONST)` into a `static final Pattern`), carries file provenance, recipe-first delivery | needs parseable sources; misses javac folding/lowering; vendored universe is mostly jars | call-site rewrite lane + hash-pinned delivery (`M3Jdk21HashPinnedSnapshotRecipe`) |
| jlink plugin at image level | the whole module closure of a runtime image | embeds the sidecar next to the CDS archive (`lib/m3/*.m3ct*`), one image per runtime, pattern of `--generate-cds-archive` and `--dedup-legal-notices`/`StringSharingPlugin` | GPL+CPE `src/jdk.jlink/**` code; only jlinked runtimes | later wrapper around the same Apache codec |

Decision: the ASM producer and the codec are Apache-2.0 tools under `m3/**`. The jlink plugin,
when it lands, is a thin GPL+CPE caller of the same documented format. The LST lane is used for
source rewrites, never for counting.

## 5. Keying and storage

- **Key** = `M3ContentKey(h1, h2, utf16Length, flags)`: FNV-1a-64 and a multiply-xorshift
  polynomial finished with MurmurHash3 `fmix64`, both over UTF-16 units and seeded with the
  flags. Class name, method, jar or call-site identity never enter a key, so one constant used in
  many classes resolves to one entry. The harvester refuses to write an image in which any two
  distinct keys share `h1` or `h2`.
- **Storage** = one little-endian int32 file per input set, memory-mapped read-only, no object
  graph, no Java serialization: `header[32] table[slots] entries[n][8] program[] atoms[m][4]
  units[] trigrams[t][2] trailer[8]` (layout in `M3ConstantRegexImageWriter`).
- **No second spelling store** (lineage invariant 6): an entry never stores its key pattern.
  The units lane holds only derived required literals; format plans are coordinates into the
  caller's own format string.
- **Bounded** (invariant 12): an image is bounded by its input set; the runtime gate materializes
  the atom needles once per `Pattern`, allocates nothing per call on the pruned path and adds no
  length-proportional state to `M3String`.

## 6. How the runtime finds it

| Moment | Lookup | Cost |
|---|---|---|
| `Pattern.compile(p, f)` | `M3ContentKey.of(p, f)` → open-addressed table → entry → needles + postfix program | one O(len) hash + O(1) probe; replaces the node-tree walk (`M3PatternQuery.derive`) |
| `Matcher.find/matches`, `String.replaceAll/split` on a `String` | evaluate the literal formula with `String.indexOf` (intrinsic); false ⇒ stock "no match" result without running the engine | O(n) SIMD scans of the input |
| pure literal key | `find` = `indexOf(lit) >= 0`, `matches` = `equals(lit)` | complete, no `Matcher` allocation |
| `ldc` resolution / `M3StringPool` admission | content key of the constant → facts entry | O(len) once per constant |
| `Formatter.parse` | content key of the format string → plan | O(len) once per distinct format |

The port consumer is enabled by the `m3.precompute.constantRegex.image` property (the
`m3.precompute.*` family used by the existing port); unset or refused means a passthrough gate.

## 7. Invalidation and refusal

1. Header magic `M3CT`/`RX01`, version and lane geometry must match the file length exactly.
2. Trailer SHA-256 over every preceding byte; any drift refuses the whole image.
3. Header carries the SHA-256 of the producing JDK's `java/util/regex/Pattern.class`; the reader
   recomputes it from its own runtime and refuses on any difference, so an image built against
   another parser (another JDK update, the M3 fork vs stock) is never trusted.
4. Header carries the SHA-256 of the input-set manifest (sorted jar SHA-256 + `jrt` runtime
   version); receipts and CI compare it to the jars actually shipped.
5. Lookup requires `utf16Length`, `flags`, `h1` and `h2` to agree; build-time proof of zero
   collisions inside the image.
6. Fail-open everywhere: miss, `UNCONSTRAINED`, `REFUSED`, `INVALID`, non-`String` input or a
   satisfied formula run the stock engine, which stays the oracle; differential tests compare
   every gated result with stock `java.util.regex`.

## 8. Owners extended

| Owner | Extension |
|---|---|
| `jdk.internal.mindex.M3TQ` | trigram key contract `c0<<32 | c1<<16 | c2` reused verbatim in the atoms' trigram lane |
| `java.util.regex.M3PatternQuery` | same walk rules; `matcherStateAdmitted` bit is exactly its admission (Start/BnM root, no `requireEnd` node), so a `src/**` consumer can take only those entries |
| `com.m3.precompute.M3RegexShape` | fail-closed literal decode cross-checks one-character literals |
| `com.m3.precompute.M3RegexOracleHarness` | the image gate is a `Candidate`; soundness proven on the frozen 640,000 pairs |
| `java.lang.M3StringSearchPrecompute` / `M3StringFacts` | targets of the `ldc`/needle lanes (design) |
| Synexia `MIndexRegexProgram` v7 (RECEIVER-1) | next regex lane: execution images for admitted keys |
| `M3Jdk21HashPinnedSnapshotRecipe` | delivery of this port as crate `m3-compiletime-regex-precompute` |

## 9. License boundary

- `m3/ports/compiletime/**`, `m3/docs/**`, the recipe crate and the image format are Apache-2.0
  (M3JDK first-party; Synexia rule lineage Apache-2.0).
- `src/**` postimages (a future `Pattern`/`Matcher` consumer, a jlink plugin) are GPLv2 with the
  Classpath Exception. No GPL+CPE text is copied into the Apache tools: `M3PatternQuery` is a rule
  twin, not a source donor.
- ASM (BSD-3) and OpenRewrite (Apache-2.0) are build-time dependencies only; RE2/J (BSD-3) is
  evidence only; GraalVM and the JDK jlink plugins are pattern-only. See
  `m3/ports/compiletime/FOSS_REUSE_DECISION.tsv`.

## 10. First vertical leaf: constant regex (proven)

Pipeline: `M3ConstantRegexHarvester` (ASM scan, regex lane) → `M3RegexRequirement` (stock JDK
node tree) → `M3ConstantRegexImageWriter` (`M3CTRX01`) → `M3ConstantRegexImage` (mmap, verify)
→ `M3ConstantRegexGate` (String fast paths, stock fallback). Host: Windows 11, Oracle JDK
21.0.9+7-LTS-338, shared with other agents (timings are medians, the host was not quiet).

### 10.1 Harvest (build time)

| Measure | Value |
|---|---|
| input set | `jrt:/` + 121 vendored jars, 65,506 classes; input-set SHA-256 `c3715cfd27bf93c281cf73d4d99db735411e96a18c08696be41dca1a5f759afd` |
| scan (ASM + operand proof, regex lane) | 14.05 s thread CPU, 15.66 s wall |
| distinct keys | 556: 54 PURE_LITERAL, 124 REQUIRED, 378 UNCONSTRAINED, 0 REFUSED, 0 INVALID; 367 matcher-state admitted |
| image | 39,524 bytes, SHA-256 `ad810b63c296433615e9d49f1fc46f101c18a1ae434dc909a0a51186a216a4b8`; self-check 556/556 keys |
| stock `Pattern.compile`, per key (median of 101 rounds, wall) | p50 601 ns, p90 1,900 ns, p99 4,900 ns, max 11,901 ns, mean 991 ns |
| compile + requirement walk, per key (median of 101 rounds, wall) | p50 2,200 ns, p90 10,100 ns, p99 23,199 ns, max 49,600 ns, mean 4,198 ns |
| walk only (difference of the two medians) | p50 1,499 ns, mean 3,206 ns per key |
| aggregate thread CPU per key (101 rounds × 556 keys) | stock compile 2,504 ns; compile + walk 5,008 ns (Windows thread-CPU clock ticks at 15.625 ms; totals 140.6 ms and 281.3 ms) |

The walk cost is what a runtime derivation would pay on every `Pattern.compile`; with the image
it is paid once at build time and the runtime pays one content-key hash plus one probe.

### 10.2 Differential proof (identical results)

| Corpus | Pairs | Mismatches | Pruned by the image |
|---|---:|---:|---:|
| 556 harvested keys × 2,965 sampled harvested `ldc` strings (every 128th by content hash, ≤ 256 units): `find`, `matches`, `replaceAll`, `split` | 1,648,540 | 0 / 0 / 0 / 0 | 524,084 (of 527,770 pairs on hit keys) |
| frozen oracle corpus 10,000 regexes × 64 strings (`M3RegexOracleHarness`, root `71d2f235…0adf`), image built from its expressions | 640,000 | 0 `find`/`matches`; 0 unsound | 49,672 (802 hit keys) |

### 10.3 Runtime cost (measured, stock vs gated)

Each pass sweeps every key × every input 8 times; values are medians of 5 passes after 2 warm-up
passes per side; stock = `pattern.matcher(s).<op>` on a precompiled `Pattern`.

| Workload | Calls / pass | Stock wall | Gated wall | Stock CPU | Gated CPU | Stock alloc | Gated alloc |
|---|---:|---:|---:|---:|---:|---:|---:|
| `find`, 178 hit keys | 4,222,160 | 2,852.0 ms | 185.5 ms | 2,765.6 ms | 187.5 ms | 908.2 MB | 2.7 MB |
| `matches`, 178 hit keys | 4,222,160 | 403.0 ms | 168.0 ms | 390.6 ms | 171.9 ms | 908.2 MB | 2.6 MB |
| `replaceAll`, 178 hit keys | 4,222,160 | 2,884.5 ms | 201.3 ms | 2,812.5 ms | 187.5 ms | 912.4 MB | 10.5 MB |
| `find`, all 556 keys (378 passthrough) | 13,188,320 | 7,434.0 ms | 6,231.1 ms | 7,265.6 ms | 5,890.6 ms | 2,888.1 MB | 1,982.5 MB |

(MB = 10^6 bytes, thread-allocated bytes.) Per call on hit keys: `find` 675.5 ns → 43.9 ns and
215.1 → 0.64 allocated bytes. An earlier run on the same host before the flags-key fix (176 hit
keys) measured `find` 1,906.0 → 126.6 ms wall and 898.4 → 2.7 MB: the ratio is stable, absolute
times move with host load.

Coverage: 178 of 556 distinct keys (32.0%) are served; 285 of the 997 `Pattern`-compiling
constant sites (28.6%); adding the 424 `split` fastpath sites, 709 of 1,421 constant regex sites
(49.9%) need no regex engine for absent inputs.

### 10.4 What did not run

- no `src/**` consumer: `Pattern.compile` does not yet read the image (the `matcherStateAdmitted`
  bit is in place for it);
- lanes a, c-g are design only; the format-plan parser runs in the inventory but no runtime
  consumer exists;
- no jlink plugin, no OpenRewrite/LST source-level inventory (class level only);
- the recipe-crate fixed point ran locally through a driver over the OpenRewrite 8.17.1
  `-m3-source-1` classpath, not through Maven surefire (the module needs the Synexia recipes
  jar); the new GitHub workflow had not run at the time of writing;
- the v7 `MIndexRegexProgram` execution image for the 378 UNCONSTRAINED keys.

## 11. Next leaves

1. `src/**` receiver (GPL+CPE): `Pattern` constructor looks up `(pattern, f)` when the image
   property is set and installs the requirement in place of `M3PatternQuery.derive` for
   matcher-state-admitted keys; the `Matcher` gate adds the `indexOf` formula next to M3TQ.
2. Needle lane (g): prefill `M3StringSearchPrecompute` for constant `indexOf/contains/startsWith/
   endsWith/replace` needles.
3. Format lane (c): SIMPLE plans as `Formatter` fast path; differential over all constant formats.
4. v7 regex execution image for UNCONSTRAINED keys (RECEIVER-1).
5. String-switch perfect hash (e) and key ids (f).

