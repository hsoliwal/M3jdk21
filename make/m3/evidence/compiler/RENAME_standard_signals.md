# jdk.m3.compiler.standard and jdk.m3.compiler.signals: product prefix dropped (2026-11-08)

`rename_types.py` v1.7 renamed 15 types: JdkStandardPlugin, JdkStandardCensus, ChangedNameGate, NameQuality,
MethodSignals and SourceSignalPlugin, with their tests (`RENAME_standard_signals.tsv`). It ran with `--string-words
--comment-words --doc m3/compiler/`. What follows the rename:
- the Plugin descriptors;
- four workflows (paths, compiled sources, mains, `-Dtest`);
- verify.sh, census.sh and check-changed.sh;
- the module READMEs and MEANINGFUL_NAMES.md.
The javac plugin names are strings and stay: `-Xplugin:M3JdkStandard`, `-Xplugin:M3SourceSignals`.

These two modules stay separate from m3/compiler, the owner module of #2841, for two reasons:
- each depends only on JUnit, and seven workflows build them standalone from a sparse checkout; m3/compiler depends
  on m3-collections;
- the standard's self-check (`-Xplugin:M3JdkStandard -Xdoclint:all/protected -Werror` over its module) refuses the
  folded #2841 sources (100+ findings: line length, Javadoc).

## Seal carried (JDK_NAMING_RESEAL by hand)

`m3/compiler/source-signals/qualification/method-signals-20261022/SOURCE_MANIFEST.tsv` pins 5 sources by path,
sha256 and git blob, and its verify.sh is step 1 of m3-method-source-signals-20261022.yml. The rows now carry the
renamed paths and bytes. The 15 carried values (PATH, SHA256, BLOB) are appended to
make/m3/tooling/jdk-naming/RENAME_RESEAL.tsv. The preimage column is unchanged.

The seal had already failed on master: M3MethodSignalsPluginTest.java changed in c5bd05277774 (2026-11-06, Windows
code-source URI fix) without a reseal. The carried row seals the current bytes, which include that fix.
- Seal verifier: master FAIL (SHA256 drift on that row); after PASS ("five exact ... postimages").

## Gate (WSL OpenJDK 21), before vs after

| | Before | After |
|---|---|---|
| jdk-standard verify.sh (plugin over its own sources, doclint, self-tests) | rc=0 | rc=0 |
| source-signals verify.sh | rc=0 | rc=0 |
| javac main/test + JUnit (make/m3/evidence/compiler/gate_fold_compiler.sh) | standard 1/1, signals 3/3 | same |
| rename fixed point | - | second run: no change |
