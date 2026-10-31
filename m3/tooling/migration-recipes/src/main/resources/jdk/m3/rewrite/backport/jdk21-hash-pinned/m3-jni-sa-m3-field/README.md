<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3-jni-sa-m3-field (Java lane)

Serviceability Agent decoder `sun.jvm.hotspot.oops.OopUtilities.stringOopToString` still read the
removed `mindex`/`MIndexString` layout (segment arrays, `storageKind`, `localValue`). Postimage decodes
the current layout exactly as `java.lang.M3StringOwner.charAt` does, without executing Java in the
target VM:

- `String.m3` (`Ljava/lang/M3String;`) null -> stock `value`/`coder` path;
- `M3String.owner` + packed `value` (start = high 32 bits, length = low 32 bits);
- owner `kind` 1 = `M3StringAtom`: `address`, `storageWidth` (1 = unsigned byte, 2 = UTF-16 unit),
  `bigEndian`; read through the debugger address space;
- owner `kind` 2 = `M3StringTuple`: `left`/`right` child M3Strings, index split on the left length.

The template keeps the OpenJDK GPLv2+CPE header. `00-OopUtilities.java.txt.before` is the exact master
preimage for the fixed-point/drift test. Lint note: `-Xlint:all` reports two pre-existing `[static]`
warnings in the untouched `initThreadFields` region; the SA module is not `-Werror` clean upstream.
