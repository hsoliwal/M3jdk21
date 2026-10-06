# JEP493 runtime-link additive leaf recovery

Current master lost the first reviewed product leaf from merged PR #109. The five GA-final JDK24
runtime-link classes were rechecked and confirmed absent on the exact current base.

This branch restores those five classes byte-for-byte from PR #109 together with the original
hash-pinned recipe, manifest, named recipe, JUnit proof, GA materialization ledger and scope/catalogue
registration. RuntimeImageLinkException remains excluded because final JDK24 GA removed it.

The leaf is MODULE-scoped and behavior/contract preserving because it adds internal implementation
classes without yet wiring JDK21 jlink behavior to them. The full JEP493 feature remains a
MULTI_MODULE candidate.

No recipe or OpenJDK compile success is claimed yet. The focused workflow must prove exact generation,
fixed point and scope registration, then configure the JDK and compile jdk.jlink before the next
existing-owner adaptation pass is admitted.
