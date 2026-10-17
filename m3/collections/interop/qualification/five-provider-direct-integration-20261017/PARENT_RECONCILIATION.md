# Product parent reconciliation (2026-10-17)

Canonical first-party donor source was authored and source-sealed against M3JDK21 #1174 head `58a72d0b2b8ffab220e3dc6e53ff7cc5d136bfb5`. Before publication, #1174 advanced one additive CI/receipt-only commit to **`6c00eef6fd67ed6c34fd86560a4ffe8343ec17d4`**. GitHub compare confirms *no existing production Java source, Maven POM or `sdk-family-bindings.tsv` changed*. The sealed `M3LibraryCollections.java` preimage stays Git blob `f65660191bfbfa5e10d54f7fe1365ce9bc8c9ce4`; family bindings stays `821ed89f52bce25afa787d8aa02b48a43dc030c7`.

The product receiving PR MUST stack on exact current #1174 head `6c00eef6fd67ed6c34fd86560a4ffe8343ec17d4` and retain the parent's workflow/receipt correction. Nothing in this crate authorizes reverting any older implementation or rebuilding the five-provider owner hierarchy. No develop rebase or force update.
