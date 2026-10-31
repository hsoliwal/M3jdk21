<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JdkStandard javac plugin

M3JDK21 keeps the source standard of the JDK itself in naming, code quality and comments. This
module holds M3-owned Java to that standard inside the compile that already runs, using only
`jdk.compiler`: no external linter and no second parse.

| Concern | Owner | Rule |
|---|---|---|
| Doc comments | `javac -Xdoclint:all/protected` (the JDK's own) | public and protected API documented; tags complete and well formed |
| Naming | `M3JdkStandard` | packages lowercase; types UpperCamel; methods, fields, parameters and locals lowerCamel; constants (static final, interface fields, enum constants) UPPER_SNAKE; type parameters one capital letter and an optional digit; no reflection parameter names (`arg0`, `arg1`, ...) |
| Imports | `M3JdkStandard` | every type imported by name, no `*` |
| Layout | `M3JdkStandard` | 100 columns (package/import lines and URLs exempt) |
| Whitespace | `M3JdkStandard`, same rules as `.jcheck/conf` | no tab, no trailing whitespace, no carriage return |

`serialVersionUID` and `serialPersistentFields` keep their Java serialization names.

## Use

```
javac -Xdoclint:all/protected -processorpath <plugin classes or jar> \
      -Xplugin:"M3JdkStandard [mode=warning] [report=<file.tsv>]" ...
```

Findings are compiler errors tagged `[M3JdkStandard RULE] line L:C message` (or warnings with
`mode=warning`, for a census of existing code). `report=` also writes them as TSV rows
(`file`, `line`, `column`, `rule`, `message`) for the BLLSE extractors.

## Proof

`verify.sh` builds the plugin with plain javac (`-Xlint:all -Werror`), compiles the plugin's own
sources again with the plugin and `-Xdoclint:all/protected` (it meets the standard it enforces),
and runs `M3JdkStandardPluginSelfTest`. The self-test compiles a clean fixture, which must have no
finding, and a dirty fixture, which must break each of the 11 rules exactly as expected (13
findings). As a discriminator, the parameters `arg` and `argument` must pass while `arg0` is
refused. Error mode must fail only the dirty compile. The Maven build runs the same self-test
through `M3JdkStandardPluginJUnitTest`.
