<!-- SPDX-License-Identifier: EPL-2.0 -->
# Donor custody

OpenRewrite 8.90.4 is an unchanged external Apache-2.0 dependency. The reviewed reset API
and Java21 delegate are pinned to openrewrite/rewrite@398a6349648ca802aa050bdb6fe45a99395dce47.
JavaParser blob fe826b1503214fde49f9569f751642b834f26901;
Java21Parser blob 79b73eaa4d7359091799f1fbcbb40f4310df8564;
ReloadableJava21Parser blob d9423230fa36e8fef820707f5ddf991aa6b0dc20;
LICENSE blob 261eeb9e9f8b2b4b0d119366dda99c6fd7d35c64.
https://github.com/openrewrite/rewrite/blob/398a6349648ca802aa050bdb6fe45a99395dce47/LICENSE
No donor implementation is copied. The wrapper calls the existing full reset API.
The exact Java snapshot owner is retained from Nebula e78e926 unchanged (EPL-2.0).
The new source-sealed composition upgrades only the historical A3 proving laboratory.
Canonical atomization/patternization remains owned by hsoliwal/com.synexia.
