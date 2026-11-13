# Version admission repair

Continue the previously saved JDK module-pack class-version candidate on real upstream ancestry.
One existing private header recognizer changes. Preserve Java APIs, module descriptors, native ABI,
existing tests/POMs and archive ownership. Validity changes are explicit conformance repairs, not
behavior-identical refactoring. Reuse the existing M3HashPinnedJavaSnapshotRecipe; execute at
m3/tooling/module-packs/kernel with module-relative paths. Do not import the previous standalone
source-export Git history or its second snapshot engine.

The private ArchiveAdapter/ClassVersionAdmission atom enforces the Java21 non-preview header range.
Short test names VersionProbe and VersionTest replace only unpublished test-helper names. No JDK
runtime symbol or existing public M3 symbol is renamed. Existing corpus, compiler and VM remain
authorities; the header policy is not a full class verifier.

Plan: verify original blobs, seal three Java outputs, diff/lint/compile, reproduce original failure,
run real archive/JVM differential twice plus existing checks, author genuine recipe/JUnit proof,
save receipts and publish on a feature branch. Framework and whole-JDK admission remain separate.

Specification: JVMS Java SE 21 section 4.1. Major versions 45..55 admit any u2 minor; later supported
major versions require minor zero under this non-preview pack policy. No newer/preview class is
admitted by this change, and unused future multi-release overlays remain outside the Java21 view.
