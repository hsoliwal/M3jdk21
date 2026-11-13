# Restore of PR #1381 (RX7 lexical plan, transfer JNI guard)

PR #1381, "M3JDK21: receive lexical RX7, transfer JNI and filtered sorted refinement" (head `07e5e24c7539`,
base `6e5035c1aa0b`), was marked merged, but its java.base work never reached master: the fold merges kept its
ancestry and dropped its tree. The fold-drop scan found it as case 29 of the lane-4d java.base triage
(`m3/qualification/foldaudit2-jbdrop-4d-20261107/DECISION.tsv`, WHOLE_PR).

Its head is an ancestor of master, so the merge base of master and the head is the head itself. The replay
therefore uses the PR's own base: `union_merge_api.py ... --base=6e5035c1aa0b`.

A 3-way text merge of `M3RX7.java` was not usable. Master had received `VocabularyFilter` by another route
(2026-10-21), so the merge duplicated it and dropped 81 master lines. The restore is structural instead:
`members.py` lists the members of each side, and the 39 members #1381 adds that master lacks are inserted
verbatim into master's file. Those members are the `MAX_LEXICAL_*` constants, `fromLexicalNfa`, the
`LexicalPlan` NFA plan, `packRange`/`rangeStart`/`rangeEnd` and `utf16Range`.

In `M3RX7.c`, everything #1381 changed is already on master except one guard: `buildPacked0` now refuses an
output array that aliases its DFA input.

Five of the six #1381 jtreg contracts are restored byte-exact. They pass on the restored owner, and none of them
compiles against master's owner. The sixth, `M3RX7TransitionCompositionContract`, is held: it enforces a
`TokenIndex` without String fields, which #1381 introduced and master does not have. That change goes to the
owner.

`DECISION.tsv` covers all 41 files of #1381 and `GATES.tsv` has the gates. To replay the gate, run
`gate-rx7.sh` (stock JDK 21 with `jdk.internal.mindex` patched, MinGW `-Werror` for the native fixture) and
`gate-wsl.sh`.
