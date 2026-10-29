<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0 -->

# Leaf 1b proposal: feed M3String leaves to the wc kernel

Status: PROPOSAL. Leaf 1 makes no java.base change. Everything below needs an M3 JDK
image build with `-XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage` to prove.

## What leaf 1 already provides

- `jdk.m3.text.Wc.feed(ByteBuffer window, int byteOffset, int units, int width,
  boolean bigEndian, long[] state)` and `Wc.finish(long[] state)`.
- The native entry `Java_jdk_m3_text_WcNative_count0` reads `units` code units at
  `byteOffset` of a direct buffer. Width 1 is Latin-1 bytes. Width 2 is UTF-16 in
  the byte order given by `bigEndian`. The `long[5]` state (lines, words, chars,
  flags, ABI magic) carries in-word, pending CR/LF and open-line facts across calls,
  so the kernel can take a String one leaf at a time in any split.
- `verify-wc-native.sh` proves every split point (gate `window-split-mid-word`) and
  both widths and byte orders against Unix4j.

Leaf 1b only has to turn an M3-backed `String` into a sequence of
`(direct window, units, width, bigEndian)` leaves without copying.

## M3String facts this relies on (origin/master 7f7accdd54e2)

| Fact | Source |
|---|---|
| `String.m3` is `private volatile M3String m3`; package-private `String.m3()` returns it, or null for a flat String | `src/java.base/share/classes/java/lang/String.java` |
| `M3String` = `owner` + `long value` (start<<32 \| length, UTF-16 units) | `java/lang/M3String.java` |
| Owners are sealed: `M3StringAtom`, `M3StringTuple`, `M3StringImmediate` | `java/lang/M3StringOwner.java` |
| `M3StringAtom`: `address`, `storageWidth` (1 or 2), `bigEndian`, `payloadOwner` (keeps mapped memory alive; VM-local blocks are managed by `M3StringPool`) | `java/lang/M3StringAtom.java` |
| `M3StringImmediate`: short Latin-1 text packed in a `long[]` page; no native address | `java/lang/M3StringImmediate.java` |
| `M3StringLeafCursor`: thread-confined walk of the tuple DAG yielding `(owner, start, length)` with optional coalescing; never copies text or exposes addresses | `java/lang/M3StringLeafCursor.java` |
| `JavaLangAccess` already has M3 hooks (`stringCodeUnitCursor`, `m3StringAsciiTokens`) wired in `System.java` | `jdk/internal/access/JavaLangAccess.java`, `java/lang/System.java` |
| `JavaNioAccess.newDirectByteBuffer(long addr, int cap, Object obj, MemorySegment segment)` makes a direct buffer whose attachment keeps `obj` reachable | `jdk/internal/access/JavaNioAccess.java` |

## Proposed java.base changes

1. **`JavaLangAccess`**: one method returning a thread-confined leaf iterator.

   ```java
   /** SoA leaf windows of an M3-backed String, or null for a flat String. */
   M3StringLeafWindows m3StringLeafWindows(String text);

   /** Thread-confined; one call fills up to lanes.length leaves; 0 means done. */
   interface M3StringLeafWindows {
       int fill(java.nio.ByteBuffer[] windows, int[] units, byte[] widthAndOrder);
   }
   ```

   Lanes are SoA, so there is no object per leaf beyond the buffer view:
   `windows[i]` is a read-only direct view over one atom range;
   `units[i]` is the UTF-16 length of the range;
   `widthAndOrder[i]` is `width | (bigEndian ? 0x80 : 0)`.

2. **Implementation in `System.java`**: use `M3StringLeafCursor(text.m3(), true)`.
   - **ATOM range** `(atom, start, length)`: call
     `JavaNioAccess.newDirectByteBuffer(atom.address + (long) start * atom.storageWidth,
     length * atom.storageWidth, atom, null).asReadOnlyBuffer()`. The attachment is the
     `M3StringAtom`, so the payload owner stays reachable while the view is reachable.
   - **IMMEDIATE range**: there is no address. Decode the packed Latin-1 units into one
     reusable direct scratch buffer owned by the iterator, then report width 1. The
     scratch is at most the immediate page limit; it is reused and never retained.
   - **Zero-length atoms** (address 0) are skipped.

3. **Qualified export**: `exports jdk.internal.access to ..., jdk.m3.text;` in
   `src/java.base/share/classes/module-info.java`. jdk.m3.text is an opt-in pack module
   today, not a module in the JDK build. A qualified export to a module that is not
   being compiled gives a `[module]` warning ("module not found"). Two options:
   (a) move jdk.m3.text sources into `src/jdk.m3.text` so it builds with the JDK;
   (b) export a dedicated small package (for example `jdk.internal.m3text`) that holds
   only the iterator type. Option (a) is cleaner and matches how the pack is mapped
   (`m3/docs/name-mapping.d`). The SDK owner decides.

4. **Facade (`jdk.m3.text.Wc`)**: add `count(String)` fast path:

   ```java
   var leaves = SharedSecrets.getJavaLangAccess().m3StringLeafWindows(text);
   if (leaves == null) return countNative(text, ...);   // flat String: today's path
   long[] state = WcNative.newState();
   try {
       for (int n; (n = leaves.fill(windows, units, flags)) > 0; )
           for (int i = 0; i < n; i++)
               WcNative.count0(windows[i], 0, units[i], flags[i] & 0x7f, flags[i] < 0, false, state);
       return finish(state);
   } finally {
       java.lang.ref.Reference.reachabilityFence(text);
       java.lang.ref.Reference.reachabilityFence(windows);
   }
   ```

## Lifetime rule

- The `String` (and the iterator plus its buffer views) must stay strongly reachable
  until the last `count0` call that reads those views has returned. Enforce this with
  `Reference.reachabilityFence(text)` in a `finally`, as above. The view attachment
  (`M3StringAtom`) keeps `payloadOwner` reachable; the fence covers the JIT liveness
  window for the String itself.
- Native code never retains an address. `count0` resolves
  `GetDirectBufferAddress`/`GetDirectBufferCapacity` per call, validates the range with
  `m3_jni_valid_window`, and reads only inside it (already in leaf 1).
- Views are read-only and thread-confined to the iterator. They must not escape the
  `Wc` call.
- M3StringPool reclamation of VM-local blocks must not run while a view is reachable.
  To confirm: the pool frees through the atom's reachability (Cleaner or reference
  queue). If it uses an explicit epoch instead, 1b must pin the epoch across the loop.

## How the C kernel receives atom ranges

No new JNI symbol is needed. Each leaf goes through the existing `count0`:

| count0 argument | Value for an ATOM leaf |
|---|---|
| `window` | read-only direct view at `atom.address + start * storageWidth` |
| `byteOffset` | 0 |
| `units` | leaf length (UTF-16 units) |
| `width` | `atom.storageWidth` (1 = Latin-1, 2 = UTF-16) |
| `bigEndian` | `atom.bigEndian` (local atoms are written high byte first) |
| `state` | the caller's `long[5]` carry state |

`M3String.length()` is in UTF-16 units for both widths, which matches the kernel's
`chars` count. A width-1 atom stores Latin-1 units, which the kernel's
whitespace table covers (gate `table-exhaustive`, width 1).

## Proof plan for leaf 1b

1. Build an M3 image with the accessor. Run with `-XX:+UnlockExperimentalVMOptions
   -XX:+UseM3StringStorage`.
2. Rerun `M3WcNativeSelfTest` and require that every case String is M3-backed
   (`m3StringLeafWindows != null`). Add tuple-concatenated Strings that split a word,
   a CRLF pair and a surrogate pair across leaves.
3. Discriminators: a String kept alive only by the view must still count correctly
   under `System.gc()` pressure, and a deliberately wrong `bigEndian` flag must fail
   the differential.
4. A jtreg test under `test/jdk/java/lang/String/` for the accessor itself.
