# JDK21 verbatim recipe law

Status: M3JDK21 execution contract.

## Immutable baseline

The immutable JDK21 baseline for this branch is commit:

    7a3f9c49d71a62c5ee2a398f4607d446dce518b8

This is the branch merge-base, not the moving master branch.

The baseline is recorded in:

    m3/tooling/verbatim/JDK21_BASELINE.tsv

A future baseline change is a separately reviewed migration. It must never occur implicitly because
master moved.

## Whole-tree byte comparison

Before promotion, every regular file below:

    src/
    test/
    make/
    doc/

is compared byte-for-byte by SHA-256 between the immutable baseline and the candidate tree.

The comparison emits one row per file:

    path
    status
    before_sha256
    after_sha256
    before_bytes
    after_bytes
    recipe_id
    approved

Possible status values are:

    SAME
    MODIFIED
    ADDED
    DELETED

Any changed row without an exact approval tuple fails the build.

Approval tuples live in:

    m3/tooling/verbatim/approved-product-delta.tsv

An approval is valid only for one exact:

    path + before_sha256 + after_sha256 + recipe_id

Changing even one byte invalidates the approval.

## Recipe generation

Java product changes are not maintained as hand-edited file deltas.

The workflow is:

    immutable JDK21 file
      -> byte-exact SHA-256
      -> reviewed candidate file
      -> byte-exact SHA-256
      -> M3HashPinnedCrateGenerator
      -> deterministic <=256-file recipe crate
      -> manifest.tsv
      -> exact postimage templates
      -> proposed approved-product-delta rows
      -> M3HashPinnedJavaSnapshotRecipe
      -> JUnit
      -> diff
      -> lint
      -> compile
      -> tests
      -> runtime when required
      -> promotion receipt

Each generated manifest row is:

    path<TAB>before_text_sha256<TAB>after_text_sha256<TAB>template

The recipe accepts only:

- the exact expected preimage;
- or the exact expected postimage, making reruns idempotent.

Any other source text is source drift and the recipe fails closed.

## Why both byte and text hashes exist

The repository gate hashes raw file bytes.

OpenRewrite operates on decoded Java source text and therefore pins the rendered UTF-8 source text.

For normal UTF-8 JDK Java sources the two identify the same visible content, but the distinction is
intentional:

- byte hash proves repository preimage/postimage identity;
- text hash proves OpenRewrite source-tree preimage/postimage identity.

The crate generator reads Java source as strict UTF-8 and refuses malformed input.

## Sharding

One hash-pinned crate contains at most 256 Java targets.

Large migrations are represented as deterministically ordered crates:

    <prefix>-0000
    <prefix>-0001
    ...

Each crate remains independently testable and scope-declared.

Repository size therefore does not force one enormous recipe.

## File-local law

A product-file recipe is FILE scope unless the actual transformation requires a broader contract.

A FILE recipe may be executed independently for every admitted file.

Broader authority is promoted only through:

    FILE
      -> VISIBILITY
      -> PACKAGE
      -> MODULE
      -> MULTI_MODULE
      -> LIBRARY_API

Verbatim comparison does not itself grant mutation authority.

## Deletion law

The generic hash-pinned Java snapshot recipe reconstructs Java files and may create an explicitly
ABSENT preimage.

It does not silently delete Java files.

A deletion requires its own explicit deletion-capable recipe, scope declaration, exact byte
approval, contract proof and tests.

## Semantic proof connection

After a hash-pinned source recipe applies, OpenRewrite regenerates the semantic graph:

    atom exact hash
    atom structural hash
    atom logic hash
    atom SimHash
      -> field/method
      -> type
      -> file/docs
      -> package
      -> module
      -> library
      -> project
      -> repository

The whole semantic root therefore provides an additional proof signal over the verbatim diff.

Neither structural hash, logic hash nor SimHash alone is allowed to assert behavior equivalence.

## Retention law

A generated recipe remains:

    CANDIDATE_UNVERIFIED

until its exact commit passes the configured executable proof.

Only then may the recipe and its approval rows be promoted to retained/verified state.

If a recipe fails, improve the recipe or its test/proof envelope. Do not bypass the recipe by
manually editing all target files.
