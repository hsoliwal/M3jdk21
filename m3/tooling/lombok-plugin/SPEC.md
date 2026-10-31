<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others -->
# SPEC: Lombok-equivalent expansion in the M3JDK compiler

Status: specification v1 (2026-10-31). Implementation is on HOLD: new production classes are generated through Chrome
local AI, one call per file, and Chrome is not available. This file, `LLD.md`, `INTERFACE.md` and `TEST_PLAN.md` are
the inputs to that generation.

## Goal

Sources that use Lombok annotations compile with M3JDK's `javac` and no Lombok jar. An M3JDK javac plugin expands
the annotations into javac's own tree before symbol entry. The result has the same class-file surface as compiling
the `lombok delombok` expansion of the same source.

Scope: the annotation kinds, attributes and `lombok.config` keys that M3JDK and com.synexia actually use
(`USAGE_CENSUS.tsv`), plus `@Accessors(prefix)` / `@Accessors(chain)` and their `lombok.accessors.*` config keys.
Anything else Lombok offers is out of scope for v1 and is refused with a diagnostic (`INTERFACE.md`), never silently
ignored.

## Evidence

| File | What it is | How it is made |
|---|---|---|
| `LOMBOK_API.tsv`, `LOMBOK_CONFIG_KEYS.tsv` | The public Lombok API and config keys of lombok 1.18.48 (sha1 `6858f13541bab505384f07053c5a7b539bbfd3e3`): 71 types, 67 annotations with @Target, @Retention, elements and defaults; 81 config keys | `tools/lombok_api.py` (javap) |
| `USAGE_CENSUS.tsv` | Every Lombok annotation use (annotation, target, normalized attributes), import, config key and module | `tools/lombok_census.py` + `tools/LombokCensus.java` (javac parser) |
| `PATTERNS.tsv` | The members each annotation group generates (signature, annotations, body), per config profile; cases C68-C86 discriminate the equality rank, the accessor prefix order and every class-level annotation combination of `CLASS_COMBINATIONS.tsv` (rules 9 and 10, `@With`) | `tools/lombok_patterns.py` + `tools/PatternDiff.java` over delombok 1.18.48 |
| `CLASS_COMBINATIONS.tsv` | Class-level annotation combinations that decide constructors, with-ers and builder members: 35 classes mix `@Value`/`@Data` with an explicit constructor annotation or `@Builder` (10 shapes); 12 of the 206 classes with type-level `@With` have neither `@Value` nor `@AllArgsConstructor`; 4 classes have `@Builder.Default` fields | `tools/lombok_combinations.py` over the census stage (`OCCURRENCES.tsv`) |
| D0 differential | delombok 1.18.48 on the 269 vendored upstream Lombok sources vs M3JDK's committed 1.18.30 expansions | `tools/delombok_differential.py` |

Census inputs: M3JDK master `52fb81c4` (all 39,147 `m3/**/*.java` blobs, 771 mention "lombok") and the com.synexia
checkout at `K:/m3-work/com.synexia` (`caf7c1a72dd4`, files on disk by ripgrep, 95 mention "lombok").

| Origin | Files with Lombok annotations | Annotation uses | lombok.config files |
|---|---|---|---|
| M3JDK | 490 (269 `upstream-lombok/` copies of 6 rewrite modules, 219 in 8 `third_party-images/rewrite-*` modules, 2 in `runtime-integration`) | 3,409 | 14, all one OpenRewrite config |
| com.synexia | 83 (test resources, donor corpora, recipe crates) | 149 | 0 |

The 6 vendored `third_party/rewrite-*` modules compile today from committed delombok expansions (`src/`, Lombok
1.18.30, recorded in each `UPSTREAM_LOMBOK.json` and NOTICE line). The 8 `third_party-images/rewrite-*` modules and
`runtime-integration/.../parser-source/rewrite-java-21` still carry Lombok annotations: they are the first consumers
of the plugin. checker-qual and org.eclipse.jdt.core only mention Lombok in comments (0 uses).

Markers below: **[P-Cnn]** = observed in `PATTERNS.tsv` case Cnn; **[D0]** = observed in the D0 differential;
**[L]** = Lombok's documented behaviour that no case exercises yet (`TEST_PLAN.md` adds the case before it is
implemented).

## Annotations in use

| Annotation | Uses | M3JDK | com.synexia | Targets and attributes in use |
|---|---|---|---|---|
| `@With` | 885 | 885 | 0 | type; field; field `AccessLevel.PRIVATE`, `AccessLevel.NONE` |
| `@Value` | 520 | 520 | 0 | class |
| `@EqualsAndHashCode` | 490 | 490 | 0 | class: none, `callSuper=false`, `callSuper=true`, `onlyExplicitlyIncluded=true`, both combined, `exclude` |
| `@Getter` | 463 | 453 | 10 | field; class; enum |
| `@EqualsAndHashCode.Include` | 250 | 250 | 0 | field |
| `@RequiredArgsConstructor` | 208 | 208 | 0 | class: none, `access=AccessLevel.PRIVATE` |
| `@FieldDefaults` (experimental) | 179 | 179 | 0 | class: `level=PRIVATE`, `level=PRIVATE, makeFinal=true`, `level=PROTECTED, makeFinal=true` |
| `@NonFinal` (experimental) | 147 | 147 | 0 | field |
| `@AllArgsConstructor` | 118 | 108 | 10 | class: none, `access=PRIVATE`, `access=PUBLIC`, `onConstructor_={@JsonCreator}` |
| `@Data` | 109 | 82 | 27 | class |
| `@Slf4j` (extern) | 59 | 0 | 59 | class |
| `@UtilityClass` (experimental) | 25 | 0 | 25 | class |
| `@Builder.Default` | 24 | 24 | 0 | field |
| `@ToString` | 22 | 21 | 1 | class: none, `onlyExplicitlyIncluded=true` |
| `@Setter` | 15 | 12 | 3 | field; field `AccessLevel.PACKAGE`; class |
| `@Builder` | 12 | 6 | 6 | class; constructor; class `builderClassName="TestEntityBuilder"` |
| `@EqualsAndHashCode.Exclude` | 11 | 11 | 0 | field |
| `@NoArgsConstructor` | 10 | 6 | 4 | class |
| `@SneakyThrows` | 4 | 0 | 4 | method, with exception classes |
| `@ToString.Include` | 4 | 4 | 0 | field |
| `@Accessors` (experimental) | 2 | 2 | 0 | class: `fluent=true`, `fluent=true, chain=true` |
| `@Delegate` (experimental) | 1 | 1 | 0 | field, `excludes=<class>` |

Not used anywhere: `@NonNull`, `val`/`var`, `@Cleanup`, `@Synchronized`, `@Locked`, `@SuperBuilder`, `@Singular`,
`@Jacksonized`, `@StandardException`, `@FieldNameConstants`, `@ExtensionMethod`, `@Helper`, `@Tolerate`,
`@PackagePrivate`, `@WithBy`, `@Wither`, `@CustomLog`, the other loggers, `@Getter(lazy=true)`, `onMethod`/`onParam`.

## Rules shared by every annotation

1. Expansion is syntactic and happens once, before symbol entry, on javac's own tree (`LLD.md`). `@Delegate` is
   the single exception: it needs the attributed type of its field [D0: the one CLASSPATH difference].
2. Generated method and constructor parameters are `final` [P-C01-01].
3. Generated members carry no `@lombok.Generated` (that type would need the Lombok jar) and no
   `@SuppressWarnings` (source retention, no class-file effect). delombok 1.18.48 adds both; the M3JDK committed
   expansions have neither [D0].
4. Fields are visited in declaration order; static fields never take part [P-C15]. `equals` and `hashCode` visit
   fields by rank, higher rank first, equal ranks in declaration order [P-C47-04, P-C47-05, P-C68-04, P-C68-05].
   The rank comes from the field's type as written: 1000 for `boolean`, `byte`, `short`, `int`, `long`, `float`,
   `double`, `char`; 800 for `Boolean`, `Byte`, `Short`, `Integer`, `Long`, `Float`, `Double`, `Character`, plain
   or `java.lang.`-qualified; 0 for every other type, arrays included (Lombok
   `HandlerUtil.defaultEqualsAndHashcodeIncludeRank`). Lombok's explicit `@EqualsAndHashCode.Include(rank = n)` is
   unused and out of scope (`M3LOMBOK_UNSUPPORTED`). `toString` keeps declaration order [P-C68-06].
5. Accessor names: `get` + capitalized name; `is` + capitalized name for primitive `boolean` only [P-C14-02];
   a `boolean` field already named `isX` keeps `isX()` and gets `setX` [L].
6. A member that the source already declares (same name, same parameter count; for `equals`/`hashCode`, either
   one) is not generated; the plugin reports `M3LOMBOK_SKIPPED_EXISTING` as a warning [L].
7. Copyable annotations (`lombok.copyableAnnotations`) on a field are copied to the generated getter, to the
   setter's, with-er's and constructor's parameter for that field [D0: e.g. rewrite-core `Maintainer(final String
   maintainer, @Nullable final URI logo)`].
8. `lombok.addNullAnnotations = CUSTOM:<nonnull>:<nullable>` adds `<nullable>` to the `equals` parameter and
   `<nonnull>` to the return of `toString`, with-ers, fluent/chained setters, `builder()`, builder setters and
   `build()` [P-C01-05, P-C01-10, P-C02-02, P-C31-03, P-C31-08, P-C31-10, P-C40-02].
9. `@Builder.Default` initializers are removed before any constructor, with-er or builder property is planned
   (Lombok runs `HandleBuilder` at priority -1024, before `HandleValue` at -512 and the constructor and `@With`
   handlers at 0). An *initialized final* is a final field, made final by the source, `@FieldDefaults` or `@Value`,
   that still has its initializer after that removal: it is no constructor parameter and gets no with-er and no
   builder property. A `@Builder.Default` field therefore is a parameter of the all-args constructor and gets a
   with-er. In case C69, `text` (`@Builder.Default`) is a constructor parameter with a with-er and a builder
   property; `kind` (initialized, made final by `@Value`) is none of these [P-C69-01 to P-C69-08, P-C69-19 to
   P-C69-21]. A field-level `@With` on a `@Builder.Default` field works the same way (rewrite-core `PlainText`
   `withId`) [P-C86-01, P-C86-05].
10. Constructors implied by `@Value`, `@Data` and class-level `@Builder` give way to explicit ones (Lombok's
    `SkipIfConstructorExists.YES` and `I_AM_BUILDER`). `@Value` and `@Data` generate no constructor when the class
    carries `@NoArgsConstructor`, `@RequiredArgsConstructor`, `@AllArgsConstructor` or `@Builder` [P-C69-01,
    P-C70-01, P-C70-02, P-C73-01, P-C74-01, P-C75-01, P-C76-01, P-C80-01, P-C82-01, P-C83-01, P-C83-02, P-C84-01,
    P-C84-02, P-C85-01] or declares a constructor [L]. Class-level `@Builder` generates its package-private
    all-args constructor only when the class carries none of `@NoArgsConstructor`, `@RequiredArgsConstructor`,
    `@AllArgsConstructor` [P-C69-01, P-C70-02, P-C82-01, P-C86-01] and declares no constructor [L]. Each explicit
    constructor annotation generates its own constructor, so `@RequiredArgsConstructor` with
    `@AllArgsConstructor(access = PRIVATE)` gives two [P-C79-01, P-C79-02, P-C84-01, P-C84-02]. The corpus has 10
    such shapes in 35 classes (`CLASS_COMBINATIONS.tsv`), each with a case. `build()` and with-ers call whatever
    constructor the class then has; the plugin does not check that one accepts the call, and javac reports a call
    that matches none, as under Lombok [L].

## Per annotation

### `@Value` (class) [P-C01]
- Class becomes `final` [P-C01-11]. Fields without an access modifier become `private`; all non-static fields
  become `final` unless marked `@NonFinal` [P-C01-02, P-C38].
- Public all-args constructor over the non-static fields that are not initialized finals (rule 9), in declaration
  order [P-C01-01]. None is generated when the class carries an explicit constructor annotation or `@Builder`, or
  declares a constructor (rule 10) [P-C73-01, P-C75-01, P-C76-01].
- Getters for every non-static field [P-C01-04, P-C01-07].
- `equals`/`hashCode`/`toString` as below, through the getters (`this.getCount()`), without `canEqual` because the
  class is final [P-C01-05, P-C01-08, P-C01-09].

### `@With` (type, field; `AccessLevel`) [P-C02 to P-C05]
- One with-er per non-static field that is not an initialized final (rule 9) at type level, or for the annotated
  field: `public T withF(final FT f) { return this.f == f ? this : new T(..., f, ...); }`, the constructor called
  with those fields in declaration order [P-C02-01, P-C69-05 to P-C69-08]. The comparison is reference/primitive
  `==` for every type [P-C02-05].
- Any constructor that accepts that call serves: one generated by `@Value`, `@AllArgsConstructor` (also
  `access = PRIVATE`), `@Data` or `@RequiredArgsConstructor` (when the with-er fields are all final), or one
  written by hand [P-C02, P-C71, P-C72, P-C74, P-C76, P-C78 to P-C81, P-C84, P-C86]. The 12 classes with
  type-level `@With` and neither `@Value` nor `@AllArgsConstructor` (`CLASS_COMBINATIONS.tsv`) rely on this. The
  plugin does not look for the constructor and has no diagnostic for its absence: javac reports a with-er call
  that matches no constructor (rule 10), as under Lombok, whose documentation accepts a hand-written constructor
  [L].
- `@With(AccessLevel.PRIVATE)` makes the with-er private [P-C04]; `@With(AccessLevel.NONE)` on a field suppresses
  the type-level with-er for that field [P-C05].

### `@EqualsAndHashCode` (class) with `.Include` / `.Exclude` [P-C06 to P-C13, P-C47]
- `equals(final java.lang.Object o)`: identity test; `instanceof T`; cast; `other.canEqual((java.lang.Object) this)`
  unless the class is final; `super.equals(o)` when `callSuper=true` [P-C09-04]; then per field, in rule 4 order:
  primitives other than float/double `!=`; `float` `Float.compare(...) != 0`; `double` `Double.compare(...) != 0`;
  primitive arrays `java.util.Arrays.equals`; `Object[]` `java.util.Arrays.deepEquals`; other references null-safe
  `this$f == null ? other$f != null : !this$f.equals(other$f)` [P-C47-04].
- Fields are read through getters when the class has them (`@Value`, `@Data`, `@Getter`), otherwise directly
  [P-C01-05 vs P-C06].
- `protected boolean canEqual(final java.lang.Object other) { return other instanceof T; }` for non-final classes
  [P-C08-02].
- `hashCode()`: `final int PRIME = 59; int result = 1;` (`super.hashCode()` when `callSuper=true` [P-C09-05]); per
  field `result = result * PRIME + h`: `int`/`char`/`short`/`byte` the value; `boolean` `(f ? 79 : 97)`; `long`
  `(int)($f >>> 32 ^ $f)`; `float` `Float.floatToIntBits`; `double` via `Double.doubleToLongBits` then the long
  rule; arrays `Arrays.hashCode` / `Arrays.deepHashCode`; references `($f == null ? 43 : $f.hashCode())`
  [P-C47-05].
- `callSuper=false` given explicitly: no super call and no warning [P-C07]. Unspecified on a class that extends a
  type other than `Object`: no super call and warning `M3LOMBOK_EQUALS_WITHOUT_SUPER` [L].
- `onlyExplicitlyIncluded=true`: only `@EqualsAndHashCode.Include` fields [P-C08, P-C12]; `@EqualsAndHashCode.Exclude`
  removes a field [P-C13]; `exclude = "name"` (legacy) removes the named fields [P-C11]. Static and transient
  fields never take part [L].

### `@Getter` (field, class, enum; `AccessLevel`) [P-C14 to P-C16]
- `public FT getF() { return this.f; }`, `isF()` for `boolean` [P-C14]. Class or enum level: every non-static
  field [P-C15, P-C16].

### `@Setter` (field, class; `AccessLevel`) [P-C17 to P-C19]
- `public void setF(final FT f) { this.f = f; }` [P-C17]; final fields are skipped [P-C19];
  `AccessLevel.PACKAGE` gives no access modifier [P-C18].

### `@ToString` (class) with `.Include` [P-C20, P-C21]
- `public java.lang.String toString() { return "T(a=" + this.getA() + ", b=" + ... + ")"; }` through getters when
  present; arrays through `java.util.Arrays.toString` / `deepToString` [P-C01-09, D0: rewrite-core `Checksum`].
- `onlyExplicitlyIncluded=true`: only `@ToString.Include` fields [P-C21].

### `@RequiredArgsConstructor` (class; `access`) [P-C22, P-C23]
- Constructor over final fields without initializer after rule 9 (and `@NonNull` fields, unused), in declaration
  order; public, or the given access [P-C23, P-C73-01].

### `@AllArgsConstructor` (class; `access`, `onConstructor_`) [P-C24 to P-C28]
- Constructor over every non-static field except initialized finals (rule 9) [P-C24, P-C69-01]. `access` sets its
  modifier [P-C26, P-C27]; `onConstructor_ = {@JsonCreator}` copies the listed annotations onto it [P-C28,
  P-C76-01].

### `@NoArgsConstructor` (class) [P-C29]
- Public no-argument constructor; an uninitialized final field is an error `M3LOMBOK_FINAL_FIELD_UNINITIALIZED` [L].
  It assigns `this.f = T.$default$f()` for every `@Builder.Default` field [P-C70-01; D0: rewrite-test
  `TypeValidation`].

### `@Data` (class) [P-C30]
- `@Getter` + `@Setter` (non-final fields) + `@RequiredArgsConstructor` + `@ToString` + `@EqualsAndHashCode` with
  `canEqual`. The constructor follows rule 10: none when the class carries an explicit constructor annotation or
  `@Builder`, or declares a constructor [P-C70-01, P-C70-02, P-C74-01].

### `@Builder` (class, constructor; `builderClassName`) and `@Builder.Default` [P-C31 to P-C34]
- Class level: package-private all-args constructor over the fields of rule 9, only as rule 10 allows [P-C31-01,
  P-C75-01]; `public static T.TBuilder builder()` [P-C31-02]; nested `public static class TBuilder` [P-C31-15]
  with one private field per property [P-C31-05], a package-private no-arg constructor [P-C31-04], fluent setters
  returning the builder [P-C31-09, P-C31-11], `public T build()` [P-C31-07] and `toString()`
  `"T.TBuilder(a=..., ...)"` [P-C31-13].
- `builderClassName = "X"` names the nested class `X` [P-C34]. Constructor level: the builder's properties are the
  constructor's parameters and `build()` calls it [P-C33].
- `@Builder.Default` field: the initializer moves into `private static FT $default$f()`, the field loses it, the
  builder keeps `f$value` and `f$set` and `build()` calls `$default$f()` when unset [P-C32]. This happens before
  constructors and with-ers are planned (rule 9) [P-C69-02, P-C69-04]; a generated no-argument constructor assigns
  `this.f = T.$default$f()` [P-C70-01].

### `@FieldDefaults` (class; `level`, `makeFinal`) and `@NonFinal` (field) [P-C35 to P-C38]
- Fields without an access modifier get `level` [P-C35]; `makeFinal=true` makes every non-static field final
  except `@NonFinal` fields [P-C36, P-C37]. `@NonFinal` also exempts a field from `@Value` [P-C38].

### `@Accessors` (class; `fluent`, `chain`, `prefix`) and `lombok.accessors.*` [P-C39 to P-C42, P-C49, P-C50]
- `fluent=true`: the getter is `f()` and the setter `f(final FT f)`; `fluent` implies `chain` unless `chain=false`
  is given, so the setter returns `this` with type `T` [P-C39, P-C40].
- `chain=true`: setters return `this` with type `T`, names unchanged [P-C41]. Config key
  `lombok.accessors.chain = true` has the same effect for every class below the config file [P-C49].
- `prefix = {"f", ...}`: a field whose name starts with a listed prefix followed by an upper-case letter (or a
  prefix ending in a non-letter, such as `m_`) gets accessors named after the rest (`fCount` gives `getCount()` /
  `setCount(...)`, `m_active` gives `isActive()`) [P-C42, P-C50]. A field that matches no prefix gets no accessors
  and the warning `M3LOMBOK_PREFIX_MISMATCH` (delombok: "Not generating getter for this field: It does not fit your
  @Accessors prefix list.") [P-C42-07, P-C50-05]. Config key `lombok.accessors.prefix += f` adds to the list
  [P-C50]. Prefixes are tried in list order and the first that matches wins, not the longest: with
  `prefix = {"_", "__", ""}`, `__count` gives `get_count()` and `_size` gives `getSize()`; an empty prefix matches
  any field and leaves its name unchanged (`plain` gives `getPlain()`, no warning) [P-C77-01 to P-C77-03].
- Precedence: `@Accessors` on the field, then on the class, then on enclosing classes, then config [L].
  Prefix stripping also names with-ers (`withCount`) [L].

### `@Delegate` (experimental; field; `excludes`) [P-C43]
- For every public instance method of the field's declared type, including default methods, except the methods of
  `java.lang.Object` and of the `excludes` types, a public method with the same signature that calls the field
  [P-C43]. Needs the attributed field type (`LLD.md`, stage 2).

### `@SneakyThrows` (method; exception classes) [P-C44]
- The body becomes `try { body } catch (final X $ex) { throw $ex; }` for each listed class (all `Throwable` when
  none is listed) with javac's checked-exception analysis suppressed for that `throw`. delombok writes
  `throw lombok.Lombok.sneakyThrow($ex)` [P-C44-01]; the plugin must not reference `lombok.Lombok` (`LLD.md`, hook
  H2).

### `@UtilityClass` (experimental; class) [P-C45]
- Class `final`; `private T() { throw new java.lang.UnsupportedOperationException("This is a utility class and
  cannot be instantiated"); }`; every field, method and member class `static` [P-C45-01 to P-C45-04]. A declared
  constructor is an error `M3LOMBOK_UTILITY_CLASS_CONSTRUCTOR` [L].

### `@Slf4j` (extern; class) [P-C46]
- `private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(T.class);` [P-C46-01]. The slf4j
  API stays the user's own compile-time dependency.

## `lombok.config` keys in use

All 14 config files in M3JDK are one file (sha256 `7a7eb5e3...`, OpenRewrite v8.17.1 `lombok.config`, kept as
`UPSTREAM_LOMBOK.config` so that real Lombok never reads it); com.synexia has none.

| Key | Value | Semantics |
|---|---|---|
| `config.stopBubbling` | `true` | Do not read `lombok.config` files from parent directories. |
| `lombok.addNullAnnotations` | `CUSTOM:org.openrewrite.internal.lang.NonNull:org.openrewrite.internal.lang.Nullable` | Rule 8. |
| `lombok.copyableAnnotations +=` | `org.intellij.lang.annotations.Language`, `org.openrewrite.internal.lang.Nullable`, `org.openrewrite.internal.lang.NonNull` | Rule 7. |
| `lombok.accessors.chain` | (not used; in scope) | `@Accessors(chain=true)` for every class below the file. |
| `lombok.accessors.prefix +=` | (not used; in scope) | Adds a prefix to the `@Accessors(prefix)` list. |

Config files are read the way Lombok reads them: the directory of the source file, then each parent, stopping at
`config.stopBubbling = true`; `+=` appends, `-=` removes, `clear` empties, a later line wins. The plugin also
accepts an explicit file (`config=<path>` option), which is how `UPSTREAM_LOMBOK.config` is used.

## Out of scope for v1

Every unused annotation and attribute listed above, Lombok's `onX` forms other than `onConstructor_`, Eclipse/ECJ
support, IDE support, `@lombok.Generated` output and Lombok's `lombok.config` keys other than the five above. An
annotation or attribute outside scope is an error `M3LOMBOK_UNSUPPORTED` naming it.
