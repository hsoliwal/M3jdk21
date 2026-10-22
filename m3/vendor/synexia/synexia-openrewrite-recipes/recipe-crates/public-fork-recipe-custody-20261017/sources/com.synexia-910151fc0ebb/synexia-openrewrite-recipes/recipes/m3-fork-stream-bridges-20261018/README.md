# Fork-wide M3JDK21 stream operator convergence

Source-first extension of canonical [object-stream recipe PR #12322](https://github.com/hsoliwal/com.synexia/pull/12322). One SHA-256 sealed recipe crate per fork, one shared M3JDK21 operator implementation, no duplicated stream execution engines. The four fork artifacts must install `m3-collections` PR #1313 and their original provider artifacts before isolated Maven testing. License the new adapters Apache-2.0; retain every fork's original notices. No source code from donor libraries was copied. Changes are additive to a new opt-in module, not invasive donor API modifications.

Gates: exact recipe replay/fixed point and preimage drift refusal, javac Java21 lint, provider behavioral tests, PR-level hosted CI; do not merge while any are unverified.
