# String layout invariant

Owner decision 47 (2026-11-17), an invariant of the entire JDK:

```java
private final byte[] value;   // Java 21 compatibility is the interned bytes
private final char[] chValue; // Java 21 compatibility is the interned chars: JNI array, protected internally
```

Every `java.lang.String` holds its text twice in Java 21 form, and fills both arrays when it is made:

- `value` holds the bytes exactly as Java 21 lays them out: Latin-1 or UTF-16 by `coder`.
- `chValue` holds the UTF-16 units.

Neither array is ever written again. `chValue` never leaves `String`: a caller only ever receives a copy.

An M3-backed String keeps its canonical owner and coordinate in `m3` as well. Its `value` is then the JNI byte shadow of that text. The zero-length layout sentinel of the earlier M3 design is retired.

## Sites that make a String

| Site | Owner | Fill |
|---|---|---|
| 17 constructor paths that assign `m3` | `String.java` | `layoutChars(value, coder, m3)`: the JNI char shadow when M3-backed, else `StringLatin1.toChars` or `StringUTF16.toChars` |
| `String(String)` | `String.java` | shares the original's `chValue`, as it shares `value` |
| `create_from_unicode`, `create_from_str`, `create_from_symbol` | `javaClasses.cpp` | `fill_char_value` once the bytes are final |
| C2 `PhaseStringOpts` | `c2_globals.hpp` | `OptimizeStringConcat` defaults to false |

**VM creation paths.** The three `create_from_*` functions are the only callers of `basic_create`, and `basic_create` is the VM's only String allocator; a code search of the jdk21u sources confirms it. Every VM-made String comes through one of them:

- JNI `NewString` and `NewStringUTF`;
- StringTable literals;
- symbols, class names and platform strings;
- CDS dump-time strings.

**C2 string concatenation.** `PhaseStringOpts` allocates a String and stores only `value` and `coder`, so its Strings would lack `chValue`. The gate test catches this: under `-Xcomp` on a VM that still optimizes, `StringBuilder.toString` fails.

The other concatenation paths need no special handling because they build through String constructors:

- javac's `invokedynamic` concatenation goes through `StringConcatHelper` to `new String(byte[], byte)`;
- `StringBuilder.toString` goes through `new String(byte[], byte)`.

**What proves every path is covered.** `chValue` is `final`, so javac's definite-assignment rule proves that no constructor path skips it.

## Proof (this PR)

**Compile.** `compile_closure.sh` compiles master's `String.java` with its 45-file M3 closure against the stock JDK 21 `java.base`. It runs `javac -Xlint:all,-this-escape -Werror` and gets exit 0 both before and after the recipe.

**Run.** `StringLayoutInvariantTest` checked 40,039 Strings on the stock VM with the edited `java.base` and `-Dlayout.vmSites=false`. It covers:

- 31 Java construction sites;
- 20,000 C1/C2-compiled builder chains;
- 20,000 compiled concatenations.

| Run | Result |
|---|---|
| default | PASS |
| `-XX:-CompactStrings` | PASS |
| `-Xcomp -XX:-TieredCompilation` (stock C2 optimizes) | FAIL: `StringBuilder.toString` has a null `chValue` |
| `-Xcomp -XX:-TieredCompilation -XX:-OptimizeStringConcat` | PASS |
| default, VM sites included (stock VM) | 8 VM sites null, as expected without the VM part: literal, empty literal, `Class.getName`, system property, `Thread.getName`, `StackTraceElement`, `intern`, `new String(literal)` |

The JDK build of this PR fills those 8 sites. CI runs the test with all four `@run` lines and the VM sites strict.

## Cost

Every String carries its chars. That is 2 bytes per char per distinct text, plus a 16-byte array header. With compressed oops, `chValue` takes the alignment gap left by the `m3` field, so a String object keeps its 32 bytes. VM-made Strings fill the array with one extra allocation.

## Next stages

1. **Readers.** Done for Java (recipe `record/layout-invariant/string_readers_chvalue.py`, proof `READERS_PROOF.txt`): `getChars` is one `System.arraycopy` from `chValue` after its unchanged bounds checks, and `toCharArray` returns `chValue.clone()`. M3-backed Strings no longer walk their owners per call, and the families' bulk windows inherit this, since they read through `getChars`. A String made outside the invariant keeps the previous paths. Still owed: `JNI GetStringChars`/`GetStringCritical` read `chValue` without inflating; the critical path pins it.
2. **Interning.** Equal texts share one interned `value` and one `chValue`. This goes through the M3 pool for M3-backed Strings and through string deduplication for flat ones. Today, sharing happens only through `String(String)` and `intern()` identity.
3. **Gate.** A naming and census gate rejects a new `String` allocation site that does not fill `chValue`.
