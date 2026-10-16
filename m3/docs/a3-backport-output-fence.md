# A3 backport evidence: no product writes through filesystem aliases

Status: candidate FILE-local A3 completion safety atom. No JDK product source is changed.

## Issue and invariant

A3 completion intends to write only beneath `m3/build/**`. Lexical `Path.normalize()`
does not prevent an existing symbolic link in `m3/build` from redirecting writes to
`src/**`, `test/**`, or another location. Truncating an existing output can also alter
a hard-linked product file. A backport inventory or recipe pass must not acquire product-write
authority through filesystem aliases.

## Small mechanical change

Keep `A3Fs.out` and its public/package contract. Refactor only the output operation:

`A3Fs.write -> A3OutputFence.write`.

The single-purpose helper verifies the checkout/output directory chain with
`NOFOLLOW_LINKS`, rejects symbolic links and non-regular output targets,
writes UTF-8 into a temporary sibling, and **replaces** the destination instead of
truncating its inode. The helper requests atomic rename, with an explicit same-directory
replace fallback where the provider cannot perform atomic rename.

A3 already denies paths outside `m3/build`; this atom strengthens the proof against
existing filesystem links. It is not an assertion of protection against a privileged,
malicious concurrent actor replacing parent directories between checks. Such adversarial
OS scenarios require a platform-specific secure directory handle.

## Contract and proof

- No changes to the A3 CLI, queue TSV schema, JDK classes or backport classification.
- Ordinary repeated writes remain valid and deterministic.
- Parent symbolic links and target symbolic links are rejected before a target write.
- A destination hard-linked to a product file is replaced; the product inode is not overwritten.
- Failed writes clean temporary files.
- Test under Java 21, including actual filesystem links on platforms supporting them.
- Source changes remain hash-pinned OpenRewrite recipe candidates, not direct
  automatic promotion. Maven/JUnit, fixed point and backport parity gates still apply.

## Scope and ownership

Only `m3/tooling/a3` implementation and tests change. The reusable source transformation
is owned by `hsoliwal/com.synexia` (OpenRewrite recipe crate); M3jdk21 retains only
the generated candidate plus its target-local JUnit proof. Camel/Airflow/Drools may
schedule this atom, but get no mutation or promotion authority.
