<!-- SPDX-License-Identifier: Apache-2.0 -->
# V5 real Surefire execution census: 173 JUnit methods across 50 source files

The earlier V4 JUnit verifier is source-sealed to exactly 48 Java test
classes and 167 test identities. Do not modify its original receipts,
historical XML verifier, method whitelist or negative tests.

The real V4-approved product commit is
`b123804aff3ac6930e420cfb82b5ce07040049fe`.
The V5 source verifier delegates to V4's original `expected_methods`
**on that real checkout**, then to source V4's 2-file SHA256/Git-object
admission for six independent new JUnit methods. The product must have
50 source classes and 173 methods; it cannot count a declared method
as executed without corresponding Maven Surefire XML.

Actual report evaluation reuses the pinned original historical
`runtime-junit-census-20261022/verify_surefire.py`. Source V4, JUnit V4,
and original V2 history all remain unchanged. This successor refuses
skipped/failing/missing/extra/duplicate/rerun/flaky tests and expects
39 actual JUnit suites (the old 37 plus two licensed provider tests).
The separate V4 successor source gate independently refuses any
non-source-pinned changes or additions.

Per-atom test evidence:
- Flat3Map: null/growth, collision/deletion, seeded eight-method parity,
  callback observation — four distinct @Test identities.
- FrozenLongSet: Guava sorted semantics and Eclipse primitive set
  semantics — two distinct @Test identities.

Run after original interop Maven verify has produced Surefire XML:
```sh
python3 m3/collections/interop/qualification/current-junit-census-v5-20261031/verify_surefire_v5.py \
 --product . --approved-product approved-interop-20261029 \
 --before-product before-interop-20261026 \
 --historical-product historical-product \
 --reports m3/collections/interop/target/surefire-reports
```
All original six vendor tests must actually execute; source/regex
recognition alone is not promotion authority. This versioned task
depends on original Java21/JNI and licensing gates.
