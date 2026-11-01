<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others -->
# INTERFACE: M3JdkLombok plugin options, accepted annotations, diagnostics

The plugin has no exported Java API in v1. Its interface is the `-Xplugin` command line, the set of annotations and
config keys it accepts, and the diagnostics and report it emits. Semantics: `SPEC.md`; design: `LLD.md`.

## Command line

```
javac -Xplugin:"M3JdkLombok [config=auto|<file>] [strip=true|false] [coexist=refuse|skip] [report=<file.tsv>]" ...
```

| Option | Default | Meaning |
|---|---|---|
| `config=auto` | yes | Read `lombok.config` from each source file's directory and its parents, stopping at `config.stopBubbling = true`. |
| `config=<file>` | | Use this one file for every unit and do not look in directories. This is how a vendored module's `UPSTREAM_LOMBOK.config` is applied. |
| `strip=true` | yes | Remove the consumed Lombok annotations and the `lombok.*` imports only they use, so no Lombok type is needed at compile time. |
| `strip=false` | | Keep them; the Lombok annotation declarations must then be on the class path (for tools that read the annotations). |
| `coexist=refuse` | yes | If real Lombok is visible to the compilation (a `lombok.launch` annotation processor on the processor path), stop with `M3LOMBOK_CONFLICT`. |
| `coexist=skip` | | In that case disable this plugin for the compilation and print a note. |
| `report=<file.tsv>` | off | Write one row per generated member and per diagnostic: `unit`, `line`, `column`, `code`, `annotation`, `case`, `member`. `code` is `GENERATED` or a diagnostic code; `case` is the `PATTERNS.tsv` case. |

Unknown options and repeated options are errors (`M3LOMBOK_OPTION`). The plugin is not autostarted. With other
plugins (`-Xplugin:M3AST`, `-Xplugin:M3JdkStandard`) the order of `-Xplugin` options does not matter (`LLD.md`).

Examples:

```
javac -Xplugin:M3JdkLombok -d out @sources.txt
javac -Xplugin:"M3JdkLombok config=m3/vendor/third_party-images/rewrite-maven-8.17.1/UPSTREAM_LOMBOK.config report=out/lombok.tsv" \
      -Xplugin:M3AST -d out @sources.txt
```

## Accepted annotations

Names resolve through the unit's imports (single-type imports shadow `lombok.*` on-demand imports) or are written
fully qualified. Any other Lombok annotation, attribute or target is `M3LOMBOK_UNSUPPORTED`.

| Annotation | Targets | Accepted attributes | SPEC section |
|---|---|---|---|
| `lombok.Value` | class | none | `@Value` |
| `lombok.With` | class, field | `value` (`AccessLevel.PUBLIC`, `PROTECTED`, `PACKAGE`, `PRIVATE`, `NONE`) | `@With` |
| `lombok.EqualsAndHashCode` | class | `callSuper`, `onlyExplicitlyIncluded`, `exclude` | `@EqualsAndHashCode` |
| `lombok.EqualsAndHashCode.Include`, `.Exclude` | field | none | `@EqualsAndHashCode` |
| `lombok.Getter` | field, class, enum | `value` (`AccessLevel`) | `@Getter` |
| `lombok.Setter` | field, class | `value` (`AccessLevel`) | `@Setter` |
| `lombok.ToString` | class | `onlyExplicitlyIncluded` | `@ToString` |
| `lombok.ToString.Include` | field | none | `@ToString` |
| `lombok.RequiredArgsConstructor` | class | `access` | `@RequiredArgsConstructor` |
| `lombok.AllArgsConstructor` | class | `access`, `onConstructor_` (also written `onConstructor = @__({...})`) | `@AllArgsConstructor` |
| `lombok.NoArgsConstructor` | class | none | `@NoArgsConstructor` |
| `lombok.Data` | class | none | `@Data` |
| `lombok.Builder` | class, constructor | `builderClassName` | `@Builder` |
| `lombok.Builder.Default` | field | none | `@Builder` |
| `lombok.experimental.FieldDefaults` | class | `level`, `makeFinal` | `@FieldDefaults` |
| `lombok.experimental.NonFinal` | field | none | `@FieldDefaults` |
| `lombok.experimental.Accessors` | class, field | `fluent`, `chain`, `prefix` | `@Accessors` |
| `lombok.experimental.Delegate` | field | `excludes` | `@Delegate` |
| `lombok.SneakyThrows` | method | `value` | `@SneakyThrows` |
| `lombok.experimental.UtilityClass` | class | none | `@UtilityClass` |
| `lombok.extern.slf4j.Slf4j` | class | none | `@Slf4j` |

`AccessLevel` values are read from the source text (`AccessLevel.PRIVATE`, `lombok.AccessLevel.PRIVATE`); no
Lombok type is resolved.

## Accepted `lombok.config` keys

| Key | Operators | Values |
|---|---|---|
| `config.stopBubbling` | `=` | `true`, `false` |
| `lombok.addNullAnnotations` | `=` | `CUSTOM:<nonnull type>:<nullable type>` |
| `lombok.copyableAnnotations` | `+=`, `-=`, `clear` | fully qualified annotation type |
| `lombok.accessors.chain` | `=` | `true`, `false` |
| `lombok.accessors.prefix` | `+=`, `-=`, `clear` | prefix text |

A key that Lombok 1.18.48 defines (`LOMBOK_CONFIG_KEYS.tsv`) but that is outside this table changes Lombok's output,
so it is an error (`M3LOMBOK_CONFIG_UNSUPPORTED_KEY`), not ignored. A key Lombok does not define is a warning, as in
Lombok.

## Diagnostics

Every message is `[M3JdkLombok CODE] text`, reported at the annotation (config diagnostics: at the unit, naming the
file and line).

| Code | Kind | When |
|---|---|---|
| `M3LOMBOK_UNSUPPORTED` | error | Lombok annotation, attribute, target or other `lombok.*` name outside the accepted set |
| `M3LOMBOK_OPTION` | error | unknown or repeated plugin option, bad option value |
| `M3LOMBOK_CONFIG_IO` | error | config file unreadable, or a line that is not `key op value`, `clear key` or a comment |
| `M3LOMBOK_CONFIG_UNSUPPORTED_KEY` | error | Lombok config key outside the accepted keys |
| `M3LOMBOK_CONFIG_UNKNOWN_KEY` | warning | key Lombok does not define |
| `M3LOMBOK_CONFLICT` | error | real Lombok visible to the compilation and `coexist=refuse` |
| `M3LOMBOK_SKIPPED_EXISTING` | warning | the member is already declared (same name and parameter count; `equals`/`hashCode` as a pair); not generated |
| `M3LOMBOK_EQUALS_WITHOUT_SUPER` | warning | `@EqualsAndHashCode`/`@Data`/`@Value` on a class extending a type other than `Object` without `callSuper` |
| `M3LOMBOK_PREFIX_MISMATCH` | warning | field matches none of the accessor prefixes; no accessor generated for it |
| `M3LOMBOK_FINAL_FIELD_UNINITIALIZED` | error | `@NoArgsConstructor` with an uninitialized final field |
| `M3LOMBOK_UTILITY_CLASS_CONSTRUCTOR` | error | `@UtilityClass` on a class that declares a constructor |
| `M3LOMBOK_DELEGATE_UNRESOLVED` | error | the `@Delegate` field's type cannot be attributed |
| `M3LOMBOK_INTERNAL` | error | a handler failed; the unit is left unexpanded and unstripped |

There is no plugin diagnostic for a with-er or `build()` whose constructor call matches no constructor. Any
constructor that accepts the call serves, generated or written by hand; when none does, javac reports the call at
the annotation that generated it, as under Lombok (`SPEC.md` `@With` and rule 10).

## Guarantees

- No Lombok jar at compile time or run time (`strip=true`): the class files reference no `lombok/*` type.
- Class-file surface equal to compiling the delombok 1.18.48 expansion of the same source, without
  `@lombok.Generated` (`TEST_PLAN.md` T2, T3).
- Same input, same output: class files and report byte-identical across runs (`TEST_PLAN.md` T6).
