# PR 140 registry compatibility admission

This is a new bounded integration task. The published candidate, original 35-output packet, 39-case Java consumer proof, 88-method host proof, installer resources and historical receipts remain immutable.

The exact inputs are master `8131f535300c005afd27443d0281ede11198522f` (tree `8a87d42532c93117f88f8e49abc728f3e49a8118`), PR head `a73202a02577d3e3242a390b962fd3e08b691b60` (tree `a3ddbb9adba0023fea585d46bfc0bb74eb58a199`) and common declared base `ccb4ace7f7d79f97ae2d9600de55960504cc3c21`. Complete tree comparison found one overlap among the 544 published paths: `m3/docs/name-mapping.json`. No AGENTS.md or CLAUDE.md appears in the exact master tree.

The registry union retains all 46 master records and every non-record field exactly as parsed JSON values, including the richer unverified descriptor reconciliation and two blocked JCC obligations. It appends the 35 disjoint intake records from the published head unchanged. The resulting 81 IDs must be unique. Deterministic derivation binds exact base/master/head bytes and refuses changed inputs; it does not infer a new source or target qualification epoch.

Reuse `m3/migration/recipe.py` and its `m3.sealed-install/1` format. The integration recipe replaces the published 79-record registry with the 81-record union, replaces only the setup of the newer JCC packet test with an exact two-profile selector, and adds one JCC compatibility plan plus the profile regression fixture at `m3/migration/test/test_jcc_handoff_context_profiles.py`. These are exactly four installer outputs: two replacements and two additions. That new plan retains the old five images/manifest and changes only its context identity and exact migration-tool guard. The six JCC test bodies and all helpers remain unchanged. Unknown code or plan hashes refuse; no existing guard or historical plan is weakened.

New control files live under `m3/migration/intake-20261005/reconcile-8131`, with a Maven task entry using the existing Python host-test pattern and already acquired exec-maven-plugin 3.6.4. The additional JCC plan lives beside its existing plan. The only existing paths changed by this reconciliation are the canonical registry and the JCC test setup.

The original packet remains valid for its exact historical 79-record authority and must refuse against the 81-record authority. A separately named derived packet changes only `authority.sha256` to the union hash. All source/target qualification pins, owners, source-root data, five producer protocols, 35 output identities and original proof references remain byte-equivalent as JSON values. A separate integration receipt records the authenticated master/head/union identities. Neither packet upgrades JDK runtime, dependency, source-tree or native acceptance.

The bounded proof must retain ordered lint, compilation, Maven tests and runtime checks. It covers unchanged core intake tests, the six retained JCC tests under both exact permitted profiles, new profile refusals, union provenance, actual installer replay/fixed point/rollback/refusals, old-packet authority refusal, exact historical replay, and actual read-only derived-packet admission/replay. No broad Java/native proof rerun is indicated because their source bytes and original evidence remain unchanged.

Construct the full proposed merge tree from the exact master tree, the original 544-path overlay with the registry replaced by the union, the setup-only JCC test postimage and new compatibility files. Check that every unrelated master entry survives and all other 543 original publication entries remain exact. Any Git index used must be isolated; no checkout, original index/ref change, protected-master update, or remote write is authorized to this agent. Root reviews the frozen proposal and owns the additive merge commit with parents PR head and exact master.

Run the bounded host tests from the repository root with the already configured Python environment (jsonschema 4.26.0):

```sh
mvn -f m3/migration/intake-20261005/reconcile-8131/pom.xml -Dm3.python=python3 test
```

After integration, the separate 81-record-authority packet is admitted read-only with:

```sh
python3 m3/migration/migration.py intake . --intake-packet m3/migration/intake-20261005/reconcile-8131/packet.json --expected-sha256 3b556b885af4b5afeb0b9f22a4bcceea93b6c836125019bde1534652d77c7639
```

The original portable packet and its receipt remain under `intake-20261005/portable`. Their original 79-record authority is preserved in `inputs/head.json`; passing the old packet against the union registry intentionally refuses. The new packet retains all original producer and consumer proofs and their source/target snapshot pins. It grants review intake only.
