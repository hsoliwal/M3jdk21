# Canon gate

The canon gate is the hard, executable form of the M3-SCALE v3.0 run contract (task packet, verify order, scope locks,
idempotence, output contract) plus the mandatory atomization and patternization proof. It has no warning mode: any
violation fails the run, and the final report is rendered from recorded evidence only.

## Run it

```sh
bash make/scripts/canon-gate.sh check <packet.tsv>
bash make/scripts/canon-gate.sh run <packet.tsv> <run-dir> [<root>]
bash make/scripts/canon-gate.sh selftest
bash make/scripts/canon-gate.sh <stamp|extension_points|text_refs|regex_spec> <root> <base-ref> <out-dir> [<name>...]
bash make/scripts/canon-gate.sh regex_sites <root> <out-dir> [<rev>]
```

`JAVA_HOME` selects the JDK (17 or later); `<root>` defaults to this repository. Exit status: 0 pass, 1 stage, scope, atomization or contract failure, 2 invalid packet or invocation. The run directory
must be outside the repository or git-ignored. A run stopped by a crash resumes from its last completed stage when the
same command is repeated; a sealed run directory is immutable.

## Packet

UTF-8 `key<TAB>value` lines; `#` comments and blank lines are ignored; unknown or repeated keys are refused. Lists are
comma-separated. Relative file paths resolve from the packet's directory; target paths are repository-relative.

| Key | Rule |
| --- | --- |
| `task_packet` | `active` |
| `source_doc`, `source_doc_sha256` | readable file; optional pinned SHA-256 must match |
| `invariant_id` | required |
| `run.mode` | `dry-run` (no file may change during the run) or `apply` |
| `run.profile` | `fast`, `full` (must include `test`) or `custom` |
| `run.kind` | `change` (default) or `inventory` (dry-run only; adds the inventory artifacts) |
| `atom.coverage` | `changed` (default): require every changed/untracked file; `selected`: also require atom rows for all tracked files in the chosen shard |
| `checks`, `checks.renamed` | optional; `checks` lists [built-in checks](#built-in-checks) (`stamp`, `extension_points`, `text_refs`, `regex_spec`), `checks.renamed` lists the retired names and is given exactly when `text_refs` is listed |
| `run.workers`, `run.shard_count`, `run.shard_index` | integers; more than one worker or shard requires `compute_resources` |
| `targets.modules`, `targets.files` | at least one; `.` names the whole repository |
| `invariant_locks` | must contain `PUBLIC_API:LOCKED,EXTERNAL_BEHAVIOR:LOCKED,DISABLED_PATHS:FORBIDDEN,EXCLUDED_PATHS:FORBIDDEN,BUILD_SCRIPTS:LOCKED` |
| `allowed_delta`, `forbidden`, `stop_conditions` | required |
| `verify_order` | ordered subset of `diff, atom_pattern, lint, compile, test, runtime`; `diff, atom_pattern, lint, compile` are mandatory |
| `verify.<stage>` | shell command for each declared stage; written to `logs/<n>-<stage>.sh` and run by `sh` in the repository root |
| `output_contract` | must list `STATUS.tsv, FINAL_REPORT.md, RUN_CONTEXT.tsv, PROVENANCE.tsv, VERIFY_CONTRACT.tsv, OUTPUT_CONTRACT.tsv, ATOM_PATTERN.tsv` |
| `pattern_catalog` | readable file of admitted pattern ids (column 1); [make/conf/canon-patterns.tsv](../make/conf/canon-patterns.tsv) is the seed |
| `excluded_paths`, `disabled_paths` | optional; any change below them fails |
| `base_ref` | optional, default `HEAD`; scope and atom checks apply to every path that differs from it |
| `baseline_run` | optional earlier run directory; a repeat with a different post-hash digest fails |

Stage commands see `CANON_RUN_DIR`, `CANON_ROOT`, `CANON_BASE` (the commit `base_ref` resolved to), `CANON_STAGE`, `CANON_MODE`, `CANON_WORKERS`, `CANON_SHARD_INDEX`,
`CANON_SHARD_COUNT` and `CANON_SHARD_MANIFEST`; `CANON_SHELL` replaces `sh`.

## Atomization and patternization

The `atom_pattern` stage must write `$CANON_RUN_DIR/ATOM_PATTERN.tsv`, tab-separated, with the header
`path sha256 atom pattern role exact_hash logic_hash structure_hash sim_hash`.

- Every row must hash-match the file as it is at the seal (`sha256`).
- Every pattern must be admitted by `pattern_catalog` or be a typed residue written `RESIDUE:<reason>`; `UNKNOWN` is
  never admitted. A residue that keeps recurring is the candidate for a new catalog row.
- Every file that differs from `base_ref` needs at least one row. With `atom.coverage=selected`,
  the gate additionally requires a row for **every tracked file in the packet-selected shard**,
  including unchanged files. All changed/untracked files remain mandatory. Legacy task packets
  default to `changed` to preserve their existing behavior.
- Atom evidence is validated **immediately after `atom_pattern`** before lint, compile, tests
  or runtime can execute. It is validated again at the final seal to catch later source drift.
- Only canonical repository-relative paths may appear in atom rows: reject absolute paths, `.`/`..`,
  empty path components, backslashes, colons and control characters. A symlink cannot be
  treated as an ordinary source file merely because its target has a matching digest.
  Check every component including intermediate directories without following symbolic links:
  a row through an ancestor link fails with `ATOM_NONREGULAR_ROW`, even if the target is Java.
- The current table has no deletion or nonregular-path evidence schema. **Changed deleted
  files, symlinks, gitlinks and directories fail closed** with `ATOM_NONREGULAR_CHANGE`
  until a versioned and tested deletion-proof representation is explicitly added.
- Hash planes: `exact_hash`, `logic_hash` and `structure_hash` are 64 hex digits and `sim_hash` (SimHash-64) is 16.
  Every atom of a `.java` file must carry all four; other files may write `-`.
- Atoms that share a plane value are listed in `ATOM_CLUSTERS.tsv` as proposals ("These atoms may be similar. Please
  confirm via hash or diff."); the gate never decides equivalence.

## Built-in checks

Checks that are part of the gate itself: no interpreter, no second process, the same result on every platform. A
packet turns them on with the key `checks`; a packet without the key behaves exactly as before. The gate runs them
right after the `lint` command passes, so a finding fails `lint` and `compile` never starts, and again at the seal, so
a later stage cannot introduce what `lint` refused and every finding is in `FINAL_REPORT.md` under its own code.

| Check | Refuses | Violation codes | Evidence |
| --- | --- | --- | --- |
| `stamp` | a change that introduces a product stamp: a new path with a stamped segment the base does not have, or an added line with a stamped name that no other line of the tree contains. A stamped name the tree already has is listed as `CARRIED` and does not fail | `STAMP_NEW_PATH <path> <segment>`, `STAMP_NEW_NAME <path>:<line> <name>` | `STAMP_GATE.tsv` |
| `extension_points` | a new abstract method on an interface or abstract class that exists on both sides of a modified Java file. Default methods and methods with a body are allowed and listed. New types, annotation types, concrete classes, static and private methods are not compared; comments and literals are blanked first | `EXTENSION_ABSTRACT_ADDED <path> <type> <signature>` | `EXTENSION_POINTS.tsv` |
| `text_refs` | a workflow, make file, build descriptor, script or test list (tracked or untracked, at most 2 MiB) that still contains a name of `checks.renamed` as a whole word; the first such line of each file is reported. Java sources are left to the compiler | `TEXT_REF_STALE <path>:<line> <name>` | `TEXT_REFS.tsv` |
| `regex_spec` | owner decision 46: a regex call site on an added line of a gated Java file (tracked or untracked) whose pattern has no row in `make/data/m3regex/REGEX_SPEC.tsv`, or that is not listed in `REGEX_SITES.tsv` (the file now has more sites of that call and row than the table lists), and any spec row that fails validation. A site whose call text is on the removed side of the patch is `MOVED` and passes with its row; comments and literals are not sites; sites added to baseline files are listed as `BASELINE` and do not fail | `REGEX_SPEC_NO_ROW <path>:<line> <id>`, `REGEX_SPEC_NO_SITE <path>:<line> <id>`, `REGEX_SPEC_INCOMPLETE`, `REGEX_SPEC_KEY`, `REGEX_SPEC_ORACLE`, `REGEX_SPEC_DUPLICATE` `<table>:<line> <id> <detail>` | `REGEX_SPEC_GATE.tsv` |

`VERIFY_CONTRACT.tsv` has one `check` row per listed check and `PROVENANCE.tsv` a `checks` row with their summaries.
A finding at the lint stage is also appended to the lint log as `path:line: error: CODE detail`, so it is a row of
`BUILD_LOG.tsv` and counts as a lint error.
Each check also runs on its own, outside a run:

```sh
bash make/scripts/canon-gate.sh stamp <root> <base-ref> <out-dir>
bash make/scripts/canon-gate.sh extension_points <root> <base-ref> <out-dir>
bash make/scripts/canon-gate.sh text_refs <root> <base-ref> <out-dir> <name>...
bash make/scripts/canon-gate.sh regex_spec <root> <base-ref> <out-dir>
```

Exit status 0 no finding, 1 at least one finding (printed, and written to the evidence table in `<out-dir>`), 2 a
wrong invocation or a git failure. The checks only read: git is asked for the base tree, the patch and line counts;
the index is not touched and nothing is fetched.

## Regex spec tables

`make/data/m3regex` holds the regex spec of owner decision 46: one row per distinct pattern, the generated site table
and the scope ([README](../make/data/m3regex/README.md)). `regex_sites <root> <out-dir> [<rev>]` reads every Java file
that `REGEX_SCOPE.tsv` covers, from the working tree or from the tree of `<rev>` (one `git cat-file --batch` process;
nothing is checked out or fetched), validates every spec row (every field present, the id is the content key of the
pattern and flags, every example agrees with `java.util.regex`) and writes `REGEX_SITES.tsv`, `REGEX_CENSUS.tsv`,
`REGEX_SPEC_TODO.tsv`, `REGEX_SPEC_ORPHANS.tsv` and `REGEX_SPEC_PROBLEMS.tsv` to `<out-dir>`. Exit status 0 when every
site has a valid row and no row is orphaned, 1 otherwise, 2 for a wrong invocation or a read failure.

## Violations

`STAGE_FAILED`, `HEAD_MOVED`, `DRY_RUN_MUTATION`, `OUT_OF_SCOPE`, `EXCLUDED_PATH`, `DISABLED_PATH`, `BUILD_SCRIPT_LOCKED`
(a build script changes without being named in `targets.files`), `ATOM_PATTERN_SCHEMA`, `ATOM_STALE`, `PATTERN_UNKNOWN`,
`PATTERN_NOT_ADMITTED`, `ATOM_PLANES_MISSING`, `ATOM_UNCOVERED`, `ATOM_PATTERN_MISSING`, `ATOM_NONREGULAR_ROW`, `ATOM_NONREGULAR_CHANGE`,
`NON_IDEMPOTENT`, `BASELINE_UNREADABLE`, `OUTPUT_CONTRACT_MISSING`, and from the built-in checks `STAMP_NEW_PATH`,
`STAMP_NEW_NAME`, `EXTENSION_ABSTRACT_ADDED`, `TEXT_REF_STALE`, `REGEX_SPEC_NO_ROW`, `REGEX_SPEC_NO_SITE`,
`REGEX_SPEC_INCOMPLETE`, `REGEX_SPEC_KEY`, `REGEX_SPEC_ORACLE`, `REGEX_SPEC_DUPLICATE`.

## Artifacts

Always: `STATUS.tsv` (append-only stage ledger), `RUN_CONTEXT.tsv`, `PROVENANCE.tsv`, `VERIFY_CONTRACT.tsv` (stages,
locks and the atom check), `OUTPUT_CONTRACT.tsv`, `FINAL_REPORT.md` (files changed, exact delta summary, exact
verification executed, exact blockers/failures, artifact paths), `BUILD_LOG.tsv` (file, line, column, severity from every
stage log), `PRE_HASH.tsv`/`POST_HASH.tsv`, `PRE_DIRTY.tsv`/`POST_DIRTY.tsv`, `SHARD_MANIFEST.txt`, `ATOM_CLUSTERS.tsv` and `logs/`.
Built-in checks add `STAMP_GATE.tsv`, `EXTENSION_POINTS.tsv`, `TEXT_REFS.tsv` and `REGEX_SPEC_GATE.tsv`. Inventory runs add `INVENTORY.tsv`, `CLUSTERS_EXACT.tsv`, `CLUSTERS_NORMALIZED.tsv`, `ACTION_QUEUE.tsv` and
`SUMMARY.tsv`; every action is proposal-only and carries "These files may be similar. Please confirm via hash or diff."

## Not enforced by the gate

`PUBLIC_API` and `EXTERNAL_BEHAVIOR` are delegated to the declared stages (for example an API-compatibility check in
`lint` and the test suite); `VERIFY_CONTRACT.tsv` records that delegation. The built-in check `extension_points`
covers one part of `PUBLIC_API`, additions to types others implement, when a packet lists it. NUMA binding is not used.

The same sources ship in the M3 Maven fork as `tools/m3-smart-builder/src/main/java/io/synexia/maven/smart/canon`;
the two copies differ only in their package line.

The gate reads `ATOM_PATTERN.tsv` one UTF-8 row at a time instead of retaining every input line at once. Source coverage, source digests and hash-plane clusters are still retained for deterministic results, so this is not a claim of constant memory for the whole run.

### Complete selected-file inventory dispositions

The optional inventory emits additive `INVENTORY_DISPOSITIONS.tsv` with
`path`, `status`, `sha256` columns and one row per selected path, alongside
the unmodified seven-column `INVENTORY.tsv`. Ordinary files report
`INVENTORIED` and SHA-256. Missing, directory, symlink/linked-parent,
nonregular or noncanonical entries report `MISSING`,
`UNSUPPORTED_DIRECTORY`, `UNSUPPORTED_SYMLINK`,
`UNSUPPORTED_NONREGULAR` or `UNSAFE_PATH` with `-` rather than being
silently skipped. These dispositions do not count as verified atoms.
The current inventory still materializes bytes for normalized clustering;
bounded-memory source indexing and race-free file opening need separate work.
The disposition `path` column encodes a literal backslash and ISO control
characters as four-digit `\\uXXXX` sequences. This preserves tabs,
newlines and carriage returns as printable data rather than creating extra
TSV fields or records, while the underlying source still remains unsafe
and unadmitted. The original `INVENTORY.tsv` and atom schema are unchanged.
