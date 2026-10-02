# MIndex-backed java.lang.String — shadow phase

## Purpose

This stage makes selected ordinary `java.lang.String` operations consume the same canonical MIndex
mapped backing proven by the backing-kernel stage.

It is intentionally a shadow migration.

Every experimental MIndex-backed String still carries the historical `byte[] value` and
`coder`. That duplicate is the correctness oracle for all JDK/HotSpot paths that have not yet
been converted.

The additional String field is a logical MIndex ID, not a native or mmap address.

## Internal construction

No public String API is added.

The internal flow is:

`MIndexStrings.install(backing) -> MIndexStrings.fromId(id) -> JavaLangAccess -> String.newMIndexString(id)`

The factory:

1. resolves the canonical content from the installed backing;
2. creates the ordinary array-backed fallback;
3. verifies backing length/hash parity;
4. attaches the non-zero MIndex ID.

`new String(mindexBackedString)` preserves the backing ID.

## Operations routed through MIndex

For a String with a non-zero MIndex ID, this stage routes:

- `length()`;
- `isEmpty()`;
- `charAt()`;
- `codePointAt()`;
- `codePointBefore()`;
- `codePointCount()`;
- `hashCode()`;
- `equals()` when either operand is MIndex-backed;
- `compareTo()` when either operand is MIndex-backed;
- UTF-8 `getBytes(Charset)`;
- default `getBytes()` through the charset entry point;
- substring source reads/range materialization.

All other String and HotSpot paths continue to operate on the fallback `value[]`.

## Equality symmetry

The equality branch is selected when either String is MIndex-backed. This is required so:

`ordinary.equals(backed) == backed.equals(ordinary)`

Canonical MIndex ID equality is a fast proof only when both Strings belong to the one installed
backing namespace. Otherwise exact UTF-16 character equality is performed.

## Substring

String substring coordinates remain UTF-16 code-unit coordinates.

The MIndex backing materializes only the requested range. A range may split a surrogate pair exactly
as ordinary String substring does.

The returned substring is ordinary array-backed in this phase. A later range-backed String stage may
retain MIndex coordinates after the VM layout contract is extended.

## Backing lifetime

The installed backing must outlive every String carrying one of its IDs.

The current experimental facade intentionally permits one backing namespace per JVM and rejects
replacement with a different backing instance.

A production VM integration should move this lifetime/namespace authority into VM-managed state.

## Why the duplicate byte[] remains

HotSpot and java.base still have many direct String.value assumptions, including:

- java_lang_String VM helpers;
- StringTable/interning;
- CDS archived strings;
- GC string deduplication;
- JNI GetString*/NewString* paths;
- compiler/JVMCI intrinsics;
- many StringLatin1/StringUTF16 helper calls.

Removing `value[]` before those owners understand MIndex backing would turn a performance
experiment into a correctness hazard.

The removal gate is therefore mechanical: inventory and convert every direct layout consumer, prove
differential parity, then allow MIndex-backed Strings to omit the duplicate payload.

## No JNI requirement

The tested backing is `MIndexMappedStringBacking`, which reads the same OS-mapped MIndex files
directly from Java.

JNI/native backing can later implement the same `MIndexStringBacking` interface.
