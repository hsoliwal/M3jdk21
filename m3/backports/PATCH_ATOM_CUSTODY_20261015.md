# JDK backport commit-patch atom custody (2026-10-15)

This is a target-side verification and candidate-output safety refinement. It does **not**
backport a JEP, edit the OpenJDK product tree, accept semantic equivalence, or promote
any upstream patch.

## Where it fits

```text
released GA / JBS donor commit
 -> exact Git NUL-delimited status inventory
 -> sorted single-FILE candidate selection
 -> reviewed donor patch
 -> target preimage custody
 -> scratch-only, source-disjoint receipt writing
 -> A3 atomize / patternize / document
 -> Synexia Maven/OpenRewrite recipe + JUnit
 -> Java 21 compile / jtreg / runtime + JNI if relevant
 -> full DAG fixed point
 -> serial promotion
```

`commit_patch_atoms.py` now reuses the small `patch_atom_io.py` contract atom.
The helper rejects absolute, traversing, ambiguous, nonportable or control-character
Git paths; reads only regular single-link target files; refuses symlinks in path
components; and prevents output directories inside or above the donor or target
source tree. Generated patches, candidates and the applicability ledger are written
once or accepted on a byte-identical replay. Stale output, symlink aliases and
hardlink aliases cause refusal instead of overwriting an existing inode.

Git status uses `git diff --name-status -z` so unusual filenames cannot be
silently reinterpreted by textual line splitting. Unsafe names remain typed
refusals, not partially rewritten candidates.

## Verification

The backport Maven control plane already runs `test_*.py` during `verify`:

```bash
python3 -m unittest discover -s m3/backports -p 'test_*.py' -v
mvn -B -ntp -f m3/backports/pom.xml verify
```

`test_patch_atom_io.py` covers traversal, output/source disjointness,
symlink/hardlink refusal, byte-exact replay and drift. Existing
`test_commit_patch_atoms.py` now additionally proves complete repeated
materialization and NUL-delimited hostile-path refusal.

These checks do not replace Git tree/object verification, source-declared Maven/
OpenRewrite recipes or Java 21 compiler/runtime tests. This helper is designed for
repository-controlled workspaces, not to be a security boundary against an
adversarial process racing filesystem changes concurrently.
