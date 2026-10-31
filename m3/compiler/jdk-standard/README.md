<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JdkStandard javac plugin

M3JDK21 keeps the source standard of the JDK itself in naming, code quality and comments. This
module holds M3-owned Java to that standard inside the compile that already runs, using only
`jdk.compiler`: no external linter and no second parse.

| Concern | Owner | Rule |
|---|---|---|
| Doc comments | `javac -Xdoclint:all/protected` (the JDK's own) | public and protected API documented; tags complete and well formed |
| Naming | `M3JdkStandard` | conventional casing plus the deterministic type/method quality rules in [MEANINGFUL_NAMES.md](MEANINGFUL_NAMES.md); no reflection parameter names (`arg0`, `arg1`, ...) |
| Imports | `M3JdkStandard` | every type imported by name, no `*` |
| Layout | `M3JdkStandard` | 100 columns (package/import lines and URLs exempt) |
| Whitespace | `M3JdkStandard`, same rules as `.jcheck/conf` | no tab, no trailing whitespace, no carriage return |
| Naming scheme (opt-in `scheme=m3`) | `M3JdkStandard` | packages `jdk.m3.<area>` or `jdk.internal.m3.<area>`; no name carried over from Synexia (`Synexia*`, `SYNEXIA_*` become `M3*`, `M3_*`) |

`serialVersionUID` and `serialPersistentFields` keep their Java serialization names.

## Use

```
javac -Xdoclint:all/protected -processorpath <plugin classes or jar> \
      -Xplugin:"M3JdkStandard [mode=warning] [scheme=m3] [report=<file.tsv>]" ...
```

Findings are compiler errors tagged `[M3JdkStandard RULE] line L:C message` (or warnings with
`mode=warning`, for a census of existing code). `report=` also writes them as TSV rows
(`file`, `line`, `column`, `rule`, `message`) for the BLLSE extractors.

## Proof

`verify.sh` builds the plugin with plain javac (`-Xlint:all -Werror`), compiles the plugin's own
sources again with the plugin and `-Xdoclint:all/protected` (it meets the standard it enforces),
and runs `M3JdkStandardPluginSelfTest`. The clean fixture must have no finding. Independent
convention and semantic-quality fixtures must break all 12 rules exactly as expected (25 findings).
Discriminators prove that `arg` and `argument` pass while `arg0` is refused, inherited overrides
retain their contract, and concise domain names pass while placeholders, promotional terms,
versions, dates, repeated words and excessive names fail without confusing `Hyperlink` or `IPv6`
for policy terms. The opt-in scheme fixture adds exactly three `NAME_SCHEME` findings for legacy
Synexia ownership and none for valid M3 names. Maven runs the same test through
`M3JdkStandardPluginJUnitTest`.
