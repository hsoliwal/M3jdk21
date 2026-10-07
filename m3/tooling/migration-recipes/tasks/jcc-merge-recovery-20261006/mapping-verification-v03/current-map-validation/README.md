# Exact current mapping validator fixture

**The unchanged canonical validator fails on the current receiving baseline with
two diagnostics, both for `synexia.counterpart.MIndexRegexTrigramQuery`.** The
current map, source owners, recipe, statuses, and tests have not been repaired or
changed by this audit.

The audited receiving commit is `d1b9162cd568108f4c8d82f6b6a03cccfdb91bd2`, root
`0812573f02908d8078d13f5f49346865cdf1c0e0`. Its mapping contains 52 ordered records,
50 non-JCC records, 25 current mapping objects, and 20 gates.

## Canonical result

`execution/baseline-v02/receipt.json` records exit code **1** and the exact diagnostic
stream in `execution/baseline-v02/stderr.log`:

```text
synexia.counterpart.MIndexRegexTrigramQuery: target hash drift: src/java.base/share/classes/jdk/internal/mindex/M3TQ.java
synexia.counterpart.MIndexRegexTrigramQuery: recipe hash drift
```

| Current artifact | SHA-256 declared by the map | Actual SHA-256 at the pinned commit |
| --- | --- | --- |
| `src/java.base/share/classes/jdk/internal/mindex/M3TQ.java` | `6ba1883a307c0c8c4b6254c1a531e98b805c00a6cb943135a6cc13b47522b187` | `d7c3f9fdff0b5f1dcc5272ac4b57962d3f405ec53003b79344361e5515992155` |
| `m3/tooling/tq/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-tq/manifest.tsv` | `245e252da59d310ea7e4229e5cafa5d25e322fee82d349337b37eab8a1f161a1` | `30b9aee3598aa556a9de57e612a1b3e3b045586b6d7382d53dc92798177338fb` |

The current target Git blob is `ca23741ecd8cd24593ba5c260aa0d939c53d6766`; the
current recipe-manifest blob is `b050a6ef7e2c2ab114c7d78395ff38782125818d`.
All other required mapping and selected-receipt SHA comparisons agree with their
current bodies. This is a finite metadata/artifact gate, not a test or rebuilt-JDK
acceptance result.

The initial run under the default Python failed before validation because
`jsonschema` was absent. `execution/baseline-v01/` preserves that raw failure and
the preparation script snapshot. Its post-run environment-metadata probe also
failed; the receipt explicitly distinguishes that launcher issue from validation.

The actual canonical run used an existing local Python 3.12 environment with
`jsonschema` **4.26.0**. No package was installed, updated, or downloaded.
`execution/baseline-v02/runtime.json` records the selected interpreter identity,
installed dependency versions, and package-file hashes. The unchanged validator is
SHA-256 `fd54e5fade1fc3050816265f2317967dd4176e7bb5e0bc55e6f620878026e88e`;
the unchanged schema is
`c29f34e5b5d858b60cddd43b2e7f273d9c01ad553cb0e3a5dcc568698646f70b`.

## Bounded acquisition and fixture contents

`REQUIREMENTS.json` inventories the 30 unique implemented target/recipe paths and
one receipt read by the canonical validator. Only the `m3.prefix-z` record claims
`implemented-tested`; it selects `prefix-z-normal` and `prefix-z-interpreter` from
`m3/migration/evidence/local-20261002-final/receipt.json`.

`RECEIPT_CLOSURE.json` records those two matched runs' 12 shared input paths and two
stdout paths. One input already belongs to the original 30-path set, so the receipt
adds **13** paths. The total acquired artifact set is exactly **44 paths**.
Unselected runs in that receipt were not expanded, and none of its historical
commands were executed.

All 44 immutable GitHub readback envelopes are retained in `calls/`. Each request
names the exact commit and complete path; each body is base64-decoded and its Git
blob identity recomputed against the returned blob SHA. Every `m3/` body is also
checked against the fully reconstructed recursive `m3` tree connected to the
captured commit/root. `src/` membership is supported by the exact immutable
commit/path contents response, with its body Git identity verified independently.

`root-evidence/` retains the commit, root, and recursive `m3` captures plus the
frozen current input-epoch receipt. The recursive proof reconstructs all 589 `m3`
tree objects. No broader repository acquisition occurred.

The reusable `fixture/` contains exactly **50 files**:

- 44 acquired target, recipe, receipt, selected input, and stdout artifacts;
- the unchanged canonical validator and schema;
- the four frozen current mapping images.

`ACQUISITION.json` records identities, envelopes, membership evidence, and every
declared-hash comparison. `FIXTURE_MANIFEST.json` seals all 50 paths, byte counts,
SHA-256 values, and Git blob identities. Before/after snapshots confirmed that the
canonical validator did not change the fixture.

## Reuse for current or actual future afterimages

Use `run_validator.py` with an existing interpreter that has the canonical
`jsonschema` dependency. For the current baseline:

```sh
python3 -B run_validator.py \
  --python /absolute/path/to/existing/python-with-jsonschema \
  --output /absolute/path/to/new-current-validation-evidence
```

The already verified local interpreter is:

`/workspace/scratch/5809f5dce3dd/continuation-20261006/base-refresh-20261006-0235/native/current-python-consumer-v2/host-venv/bin/python`

For the future **actual** Maven result directory and actual-mode V02 binder bundle:

```sh
python3 -B run_validator.py \
  --python /absolute/path/to/existing/python-with-jsonschema \
  --materialized /absolute/path/to/actual-four-Maven-Result-images \
  --mapping-resources /absolute/path/to/actual-v02-binder-output \
  --output /absolute/path/to/new-afterimage-validation-evidence
```

This mode requires exactly the four expected result files, hashes matching the
actual-mode binder receipt, the exact current receiving epoch, and a source packet
marked as actual GitHub-readback verified. It makes a fresh workspace from the
sealed current fixture, replaces only those four files, and runs the same unchanged
validator/schema. The original current map is passed as `--previous` to check
lineage as well. No future workspace or afterimage execution has been created yet.

The receipt reports the canonical exit code and diagnostics, then compares future
diagnostics with the actual baseline failure. An inherited failure remains a
failure; this wrapper never converts it into a pass. Any new or removed diagnostic
is listed explicitly. The source publication's authenticity and its upstream
readback proof remain the responsibility of the separately verified source packet.

Keep execution evidence outside `fixture/`, the actual result directory, and the
binder input directory. The wrapper rejects output paths inside those inputs and
requires a new output directory. Frozen Java templates, source-publication inputs,
mapping metadata, canonical owners, statuses, and tests remain unchanged.
