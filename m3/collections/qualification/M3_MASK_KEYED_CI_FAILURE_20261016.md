<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 keyed mask Java21 CI — duplicate anchor repair

Original [Set/Map CI run 38889447549](https://github.com/hsoliwal/M3jdk21/actions/runs/38889447549)
failed in Java compilation, after all six existing and four keyed source digests
had been validated. Source-generated postimages doubled two method declarations
in the same line:

- `M3LongHashSet.contains(long)`: duplicate `@Override`.
- `M3LongLongHashMap.getOrDefault(long,long)`: duplicate signature.

The source-recipe correction was committed FIRST to canonical
[Synexia #11870](https://github.com/hsoliwal/com.synexia/pull/11870)
as `4db5dec1b424bd2d1b6c1d4d60705b687f9ef127`. This receiving
commit installs only those two corrected exact template bytes plus their
SHA256 runtime receipt. No public descriptors, input ownership, arrays,
mask semantics, tests, runtime dependency or JNI code are changed.

The earlier failed output is preserved in Git history, and a new strict
Java21 run must pass before this draft is promoted. Historical failure
receipts are retained rather than silently called successful.
