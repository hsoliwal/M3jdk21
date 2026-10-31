<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others -->
# LLD: the M3JdkLombok javac plugin

Inputs: `SPEC.md` (semantics), `PATTERNS.tsv` (generated members per case), `INTERFACE.md` (options, accepted
annotations, diagnostics). Status: design v1; every file listed under "File plan" is HOLD until Chrome local AI is
available (one generation call per file).

## Placement

| Decision | Choice | Reason |
|---|---|---|
| Where the plugin lives | Builtin plugin of module `jdk.compiler`, registered like M3AST: `provides com.sun.source.util.Plugin with jdk.internal.m3.javac.lombok.M3JdkLombokPlugin;` in `module-info.java` | No `-processorpath`, no jar. Inside `jdk.compiler` the plugin may use `TreeMaker`, `Names`, `Enter`/`MemberEnter` and `Flow` directly; outside it would need `--add-exports` for each. |
| Name | `-Xplugin:M3JdkLombok` | Product type prefix `M3Jdk*` (user naming decision); "Lombok" stays because it is the donor's name. |
| Package | `jdk.internal.m3.javac.lombok` (not exported) | Naming rule `jdk.internal.m3.<area>`. M3AST's `com.sun.tools.javac.m3` predates the rule and is not moved. |
| Lombok annotation types | Not needed at compile time: after a unit is expanded the plugin removes the annotations it consumed and the `lombok.*` imports that only those annotations used | "No Lombok jar" holds for compile and run. delombok removes the same annotations; all in-scope ones are `SOURCE` retention, so the class files are unchanged. A `lombok.*` name used anywhere else keeps its import and is reported (`M3LOMBOK_UNSUPPORTED`). |
| Autostart | No (`autoStart() == false`) | A build that still runs real Lombok as an annotation processor must not expand twice; `INTERFACE.md` defines the conflict check. |

Staging route before it becomes builtin (optional, same sources): build the plugin as a module on
`--processor-module-path`, like M3JdkStandard (#2188), with `--add-exports jdk.compiler/com.sun.tools.javac.{tree,util,code,comp}=jdk.internal.m3.javac.lombok`.
The tests in `TEST_PLAN.md` are written against `-Xplugin:M3JdkLombok` and run unchanged in both forms.

## Where in javac: three task events, one tree

javac fires `PARSE` finished per unit, then `ENTER` started for every unit before `Enter.main(roots)`, then
`ENTER` finished per unit, then `ANALYZE` per class. The plugin registers one `TaskListener` in `init`.

| Stage | Event | Work | Why there |
|---|---|---|---|
| S0 | `Plugin.init` | Parse options (`INTERFACE.md`), build the config resolver, register the listener. | |
| S1 | `ENTER` started, per unit | Syntactic expansion: resolve Lombok names, resolve config, run handlers H-MOD to H-SNEAKY (below) on every class of the unit, innermost class first, strip consumed annotations and imports. | Every unit is parsed and no symbol exists yet. Members added now are entered by javac itself, so other classes, annotation processors and later rounds see them as if written. Units created by annotation processors pass through the same event in their round. |
| S2 | `ENTER` finished, per unit | Attribution-dependent expansion: `@Delegate` only. Attribute the field's type (`Attr.attribType`), list the delegate methods with `Types`/`Elements`, add the methods to the class tree and enter them with `MemberEnter`. | Needs the type's members; must finish before any class is attributed against this class. |
| - | `ANALYZE` | Nothing. | javac attributes, flows and lowers the expanded tree. |

M3JDK `JavaCompiler.enterTrees` (lines 1075-1088 at `52fb81c4`) fires `ENTER` started for all roots, calls
`Enter.main(roots)`, then fires `ENTER` finished for all roots, so S1 always runs before any symbol exists.

Idempotence: when annotation processing needs another round, javac cleans the symbols off every root and enters it
again, firing `ENTER` again for trees the plugin already expanded. S1 and S2 keep an identity set of expanded units
(S2: of expanded `@Delegate` fields) and skip them; by then the generated members are ordinary members of the tree
and javac enters them itself.

One-AST law: there is no second tree and no reparse. The plugin edits javac's own `JCCompilationUnit` in place
with `TreeMaker`. Generated nodes carry the start position of the annotation that caused them (diagnostics land on
the annotation) and no end position. A per-task identity map `generated: JCTree -> Origin(annotation, pattern id)`
records which nodes are generated.

### Interaction with the M3JDK plugins that exist

| Plugin | Event it uses | What it sees | Contract |
|---|---|---|---|
| M3AST (builtin, `com.sun.tools.javac.m3.M3ASTPC`) | `PARSE` finished (PARSED consumers), `ANALYZE` finished (ANALYZED consumers) | PARSED: the source as written (S1 runs later). ANALYZED: the expanded classes. | M3AST projections that read source ranges skip nodes in `generated` (no end position, no sealed source text). The plugin exposes `generated` to M3AST through the task `Context`, never as a second tree. |
| M3JdkStandard (#2188, `-processorpath`) | `PARSE` finished, `COMPILATION` | The source as written. | Naming and layout rules never apply to generated members; `@Delegate` parameter names (`arg0` when the delegate type has no parameter names) cannot trip its `arg0` rule. |
| Annotation processors | after `Enter` | The expanded classes. | Same view as with real Lombok, without its processor-ordering dependence. |

## Handler pipeline (stage S1)

Each handler is a typed behaviour atom: input = the class tree, its field model, the resolved annotations and the
config view; output = tree edits recorded as (kind, member, pattern id). Order is fixed so that later handlers see
earlier output, matching delombok's member order (`PATTERNS.tsv` rows of one case appear in that order in the
delombok output).

| # | Handler | Annotations | Output (PATTERNS cases) | Depends on |
|---|---|---|---|---|
| 1 | H-CONFIG | (every unit with a Lombok name) | config view for the unit's directory chain | |
| 2 | H-MOD | `@UtilityClass`, `@FieldDefaults`, `@Value` (finality and access part), `@NonFinal` | modifier edits, private constructor of `@UtilityClass` (C35-C38, C45, C01 MODIFIERS rows) | 1 |
| 3 | H-NAMES | `@Accessors`, `lombok.accessors.*` | accessor name and return-type plan per field (C39-C42, C49, C50) | 1, 2 |
| 4 | H-CTOR | `@NoArgsConstructor`, `@RequiredArgsConstructor`, `@AllArgsConstructor`, implied by `@Value`, `@Data`, `@Builder` | constructors (C22-C29, C01-01, C30, C31-01) | 2 |
| 5 | H-ACCESS | `@Getter`, `@Setter`, implied by `@Value`, `@Data` | getters and setters (C14-C19, C51) | 3 |
| 6 | H-WITH | `@With` | with-ers (C02-C05) | 4 (all-args constructor) |
| 7 | H-EQUALS | `@EqualsAndHashCode` (+ `.Include`/`.Exclude`), implied by `@Value`, `@Data` | `equals`, `canEqual`, `hashCode` (C06-C13, C47) | 5 (getters when present) |
| 8 | H-TOSTRING | `@ToString` (+ `.Include`), implied by `@Value`, `@Data` | `toString` (C20, C21) | 5 |
| 9 | H-BUILDER | `@Builder`, `@Builder.Default` | `builder()`, nested builder class, `$default$` methods (C31-C34) | 4 |
| 10 | H-LOG | `@Slf4j` | logger field (C46) | |
| 11 | H-SNEAKY | `@SneakyThrows` | body rewrite with a sneaky `throw` (C44) | |
| 12 | H-STRIP | every consumed annotation | removes the annotations and the `lombok.*` imports only they used | 2-11 succeeded for the unit |
| S2 | H-DELEGATE | `@Delegate` | delegate methods (C43) | attributed field type |

Shared atoms used by the handlers:

| Atom | Input | Output | Failure |
|---|---|---|---|
| ResolveLombokNames | unit imports and annotation trees | (annotation tree, fully qualified Lombok name) pairs; single-type imports shadow `lombok.*` on-demand imports (JLS 6.4.1); the census tool `tools/LombokCensus.java` implements the same rule and its fixture is the discriminator | name outside LOMBOK-API v1: `M3LOMBOK_UNSUPPORTED` |
| ResolveConfig | source file path, `config=` option | immutable view of the in-scope keys; bubbling stops at `config.stopBubbling = true` | unreadable file: `M3LOMBOK_CONFIG_IO`; unknown key: `M3LOMBOK_CONFIG_UNKNOWN_KEY` (warning) |
| FieldModel | class tree | non-static fields in declaration order: name, type tree, primitive kind, array depth, final, initializer, field annotations, copyable annotations | |
| MemberIndex | class tree | names and arities of declared methods and constructors (for SPEC rule 6) | |
| Make | pattern id, field binding | the `JCTree` of one `PATTERNS.tsv` row, `final` parameters, nullness and copyable annotations applied (SPEC rules 2, 7, 8) | |

`Make` is template-first: every generated member is one `PATTERNS.tsv` row (signature + body with `${Type}`,
`${f}`, `${F}`, `${T}` bound). The body text is not reparsed at compile time; the generator for each `Make` case is
produced from the row (TmOP), and `TEST_PLAN.md` T1 proves the produced tree prints back to the row.

## javac hooks (edits to existing jdk.compiler files)

| Hook | File | Edit | Needed by |
|---|---|---|---|
| H1 | `src/jdk.compiler/share/classes/module-info.java` | add the `provides ... Plugin with jdk.internal.m3.javac.lombok.M3JdkLombokPlugin` line | builtin registration |
| H2 | `src/jdk.compiler/share/classes/com/sun/tools/javac/comp/Flow.java` | `FlowAnalyzer.visitThrow` (and the try/catch rethrow analysis) skips the unreported-exception check for a `JCThrow` the plugin registered as sneaky in a per-task identity set | `@SneakyThrows` without `lombok.Lombok.sneakyThrow` |
| H3 | none (internal API call) | stage S2 calls `MemberEnter.memberEnter(tree, env)` for each delegate method it adds | `@Delegate` |

H2 keeps the class file identical to real Lombok's (Lombok rewrites `invokestatic lombok/Lombok.sneakyThrow`
followed by `athrow` into a bare `athrow` after compilation): the bytecode is a plain `athrow` of the caught
exception. Rejected alternative: a private generic `sneakyThrow` helper method per class, because it adds a private
member to the class surface.

## Diagnostics and determinism

- All messages go through `Trees.printMessage(kind, message, tree, unit)` with the codes of `INTERFACE.md`; the
  message text starts with `[M3JdkLombok CODE]`, like M3JdkStandard's `[M3JdkStandard RULE]`, so the BLLSE
  extractors parse both.
- A handler failure (exception) is reported as `M3LOMBOK_INTERNAL` on the annotation and the unit is left
  unexpanded and unstripped: javac then also fails on the unresolved Lombok annotation, so no half-expanded class
  is ever compiled.
- Output depends only on (unit text, config files, plugin version): no clock, no hash order (all maps are
  insertion-ordered), no environment.

## Cost

- Units without a `lombok.*` import or a `@lombok.` name are skipped after a scan of their import list.
- No annotation-processing round, no Lombok jar loading, no javac patching: the work is the tree edits of the
  generated members only.
- The `report=` option (`INTERFACE.md`) writes one TSV row per generated member (unit, line, code, pattern id) for
  the census tools; off by default.

## File plan (HOLD: Chrome local AI, one call per file)

| File (module `jdk.compiler`, package `jdk.internal.m3.javac.lombok` unless noted) | Purpose |
|---|---|
| `M3JdkLombokPlugin.java` | `Plugin`: name, options, listener for S1/S2, `autoStart() == false` |
| `M3JdkLombokNames.java` | ResolveLombokNames |
| `M3JdkLombokConfig.java` | ResolveConfig (config chain, the five keys plus `lombok.accessors.*`) |
| `M3JdkLombokFields.java` | FieldModel and MemberIndex |
| `M3JdkLombokMake.java` | Make: the `PATTERNS.tsv` templates as tree builders |
| `M3JdkLombokModifiers.java` | H-MOD |
| `M3JdkLombokAccessors.java` | H-NAMES and H-ACCESS |
| `M3JdkLombokConstructors.java` | H-CTOR |
| `M3JdkLombokWith.java` | H-WITH |
| `M3JdkLombokEquality.java` | H-EQUALS |
| `M3JdkLombokToString.java` | H-TOSTRING |
| `M3JdkLombokBuilder.java` | H-BUILDER |
| `M3JdkLombokLog.java` | H-LOG |
| `M3JdkLombokSneaky.java` | H-SNEAKY and the identity set H2 reads |
| `M3JdkLombokDelegate.java` | H-DELEGATE (S2) |
| `M3JdkLombokDiagnostics.java` | codes and message formatting |
| `module-info.java`, `Flow.java` (existing) | H1, H2 (in-file maintenance) |
| `test/langtools/tools/javac/m3/lombok/*` | jtreg tests of `TEST_PLAN.md` |

Every new file carries the Synexia SPDX header and a `//FLLDCIM:` anchor naming its handler and PATTERNS cases.
