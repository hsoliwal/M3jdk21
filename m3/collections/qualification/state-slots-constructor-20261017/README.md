<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3ObjectStateSlots28 — preserve implicit public constructor contract

The existing dynamic 28-bit object state slot and byte-mask implementation
remains authoritative. The source warning observed in the inherited
four-library M3JDK21 CI was triggered solely because the exported class
used an implicit public no-argument constructor. This original constructor
is PUBLIC by Java language rules and `new M3ObjectStateSlots28()` works.

This receiver makes it explicit:
`public M3ObjectStateSlots28() { }`.
No state representation, bitwise operation, offsets, JNI symbol, Java object
ownership or public contract changes. The source is exactly generated from
the SHA-256-pinned first-party Synexia Maven/OpenRewrite recipe in
[Synexia canonical PR](https://github.com/hsoliwal/com.synexia/pulls).

Execute `sh m3/collections/qualification/state-slots-constructor-20261017/verify.sh`
to prove the exact original/new source mutation, compile original (with only
the specific old diagnostic disabled), compile candidate with strict
`-Xlint:all -Werror`, compare old/new public method descriptors using
`javap -public -s`, and run the original M3ObjectStateSlots28SelfTest.
This is a bounded real-JDK lint fix. It does not waive other CI gates or
replace strict installed-source manifest qualification.
