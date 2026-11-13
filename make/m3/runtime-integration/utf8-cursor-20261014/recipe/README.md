<!-- SPDX-License-Identifier: Apache-2.0 -->
# UTF8 receiving SDK proof

This is an exact related qualification mirror of canonical Synexia PR11964,
not a second recipe owner. canonical-source.json binds the private canonical
commit and the exact 13-file canonical module tree, checked before Maven.
The mirrored providers already exist publicly in this repository. The only
newly received canonical-source test is the directly relevant unchanged UTF8
five-test SDK class. No unrelated private code or private dependency is required.

Run:
python3 -I -S -B canonical/verify.py --repository <repository-root>
mvn -B -ntp -f canonical/pom.xml test
python3 -I -S -B canonical/verify.py --reports --repository <repository-root>

Maven validate executes26 bounded input-closure refusal cases, then compiles
the three pinned receiving providers and runs all five unchanged SDK tests.
Post-verification requires exact testcase identities and zero failures/errors/
skips. It emits canonical/target/sdk-receipt.json. SOURCE_README.md and the
source catalogue/borrow decisions preserve original context: their sdk/
paths refer to this mirror's canonical/ directory. GPL fixture licenses live
under canonical/licenses/; Java source headers and Apache tooling notices
remain intact.

Existing destination CI runs this module in its precompute job before its
unchanged Java/JNI precompute commands. It adds no host or executor and changes
no runtime Java/JNI source. A successful receipt qualifies receiving provider
2530 with these five SDK tests only. Canonical39c, YAML activation, matched-JDK
build/jtreg, native M3String lifetime and all performance/retention gates remain
open. The original UTF8 HOLD and phase chronology stay unchanged. Full migration
and continual optimization remain active.
