# Regex spec tables

Owner decision 46: every regex carries a spec row with its id, pattern, flavour, language and
version, source, licence, positive and negative examples, and oracle. A regex without a row fails a
gate. These tables are the rows. The canon gate in `make/jdk/src/classes/build/tools/canongate`
reads them, and its command `regex_sites` generates the site table from the source tree.

| File | Kind | Content |
| --- | --- | --- |
| `REGEX_SPEC.tsv` | written by hand | one row per distinct pattern and flags, and one row per dynamic call text |
| `REGEX_SITES.tsv` | generated | one row per file, call and row id, with the number of sites |
| `REGEX_SCOPE.tsv` | written by hand | which Java sources the spec covers and which of them are gated |

## REGEX_SPEC.tsv

Tab-separated, UTF-8, ASCII only, with this header:

| Column | Content |
| --- | --- |
| `id` | the content key of the pattern and flags, `h1:h2:length:flags`: two 64-bit hashes in hex, the UTF-16 length and the flags. It is the key of `jdk.m3.compiletime.M3ContentKey`, so a row joins the compile-time regex image. A dynamic row is `dyn-` plus 16 hex digits of the SHA-256 of the call text with white space collapsed, for example `Pattern.compile(regex)` |
| `pattern` | the pattern as a Java string literal, with its quotes. Its value is the regex. A dynamic row has `TODO(dynamic)` followed by the call text |
| `flags` | `Pattern` flag names joined by `\|`, or `-` |
| `flavour` | `java.util.regex` |
| `language_version` | `Java SE 21` |
| `oracle` | `java.util.regex.Matcher#find` or `java.util.regex.Matcher#matches`: the method that decides the examples. A dynamic row has `TODO(dynamic)` |
| `positive_examples` | one or more Java string literals separated by `, `. The oracle accepts each one |
| `negative_examples` | the same, and the oracle rejects each one |
| `source` | the file where the row was first recorded |
| `licence` | the SPDX identifiers of the files that use the pattern, separated by `; `. `UNDETERMINED` means the file has no licence header. Licences are recorded and never gate |

A row is valid when every field is present, the `id` of a literal row is the content key of its
pattern and flags, and every example agrees with `java.util.regex` on the JDK that runs the gate.
`TODO` is allowed only in the pattern, oracle and example cells of a dynamic row, because such a
pattern is known only at run time. The oracle of a row is `#matches` when every site of the pattern
is a `matches` call, and `#find` otherwise.

## REGEX_SITES.tsv

`path`, `call`, `id`, `count`, `kind`, `scope`. A site is a call of `Pattern.compile` or
`Pattern.matches` with any first argument, or of `matches`, `replaceAll`, `replaceFirst` or
`split` whose first argument starts with a string literal. A one-argument `replaceAll` or
`replaceFirst` is the replacement method of `Matcher` and is not a site. Calls in comments and in
literals are not sites. Line numbers are not recorded, so an edit that moves a site does not change
the table.

The `kind` column is:
- `regex` when the pattern uses regex syntax;
- `literal-only` when no character of the pattern has a regex meaning beyond itself;
- `single-char-split` when it is a `split` that meets the `String.split` fastpath condition, so no
  `Pattern` is created (a one-character pattern that is not one of `.$|()[{^?*+\`, or a backslash
  followed by a character that is not an ASCII letter or digit);
- `dynamic` when the argument is not made only of string and character literals.

## REGEX_SCOPE.tsv

`glob`, `scope`, `owner`. The first glob that matches a path gives its scope. `*` matches within one
path segment, and `**` as a whole segment matches any number of segments.

| Scope | Meaning | Sources |
| --- | --- | --- |
| `gated` | M3-owned product code. The canon gate refuses a new site without a row | `String.java`, `jdk.m3.*` and `m3.*` modules, `m3` packages and `M3` types in JDK modules, `jdk.internal.mindex` |
| `baseline-provider` | vendored donor sources (Guava, Lucene and OpenNLP), kept as published upstream. Rows are recorded, and new sites are listed but do not fail | `src/*/providers/**` |
| `baseline-tooling` | the canon gate and the M3 build recipes. Rows are recorded, and new sites are listed but do not fail | `make/jdk/src/classes/build/tools/canongate/**`, `make/m3/**` |

A file that no glob matches is upstream OpenJDK source and is outside the spec. Test sources are
outside it too.

## Adding rows

1. Change the code.
2. Run `bash make/scripts/canon-gate.sh regex_sites . <out-dir>`. It reads the working tree; give a
   commit as a fourth argument to read that tree instead. It writes `REGEX_SITES.tsv`,
   `REGEX_CENSUS.tsv` (every site with its line), `REGEX_SPEC_TODO.tsv` (a skeleton row for each
   id without a row), `REGEX_SPEC_ORPHANS.tsv` (rows that no site uses) and
   `REGEX_SPEC_PROBLEMS.tsv` (rows that fail validation).
3. Copy each skeleton row into `REGEX_SPEC.tsv` and replace its `TODO` examples with real ones.
   Keep the rows sorted by `id`.
4. Copy the generated `REGEX_SITES.tsv` here.
5. Repeat until `regex_sites` exits 0. Exit status 0 means every site has a valid row and no row is
   orphaned, 1 means one of these conditions failed, and 2 means a wrong invocation or a read failure.

## Counts at generation

The tables were generated from M3jdk21 master `6b2c6e5d25e` with the leaf that changed six `\\t` and
`\\n` splits to `\t` and `\n` applied.

| | Sites | Files |
| --- | --- | --- |
| gated | 177 (127 regex, 2 literal-only, 36 single-char-split, 12 dynamic) | |
| baseline-provider | 172 (100 regex, 12 literal-only, 50 single-char-split, 10 dynamic) | |
| baseline-tooling | 73 (37 regex, 2 literal-only, 32 single-char-split, 2 dynamic) | |
| total | 422 | 195 |

These sites give 230 rows: 212 literal rows with 813 examples verified by `java.util.regex` on JDK
21.0.9, and 18 dynamic rows with `TODO`.

## Not covered

- The receiver type of a call is not resolved. A `matches`, `replaceAll`, `replaceFirst` or `split`
  call whose first argument is not a literal is not a site, and a method of the same name on
  another type with a literal argument is counted.
- Text blocks are dynamic. `splitWithDelimiters`, `Scanner` and `PathMatcher` patterns are not
  scanned.
- Regexes in Python, shell and workflow files are not covered.
