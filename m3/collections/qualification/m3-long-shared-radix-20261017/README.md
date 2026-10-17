# M3 signed-long shared radix product receiver — 2026-10-17

Source-first donor/recipe [Synexia #12102](https://github.com/hsoliwal/com.synexia/pull/12102). Stacks on M3JDK21 #1098 (physical state-mask projections) and #1092 (direct-address kernel), not directly on bare master.

The existing `M3LongIndexProjection` owns an immutable canonical `long[]` and a physical-row `int[]`. New opt-in overloads `sortedAscending(M3SignalOrder.Backend)` and descending sort physical row IDs using the existing stable `M3SignalOrder` primitive LSD radix engine. Signed natural order is normalized by `value ^ Long.MIN_VALUE`; descending reverses unsigned key order with complement. Stable equal-key passes preserve distinct physical row identity and the encounter order of ties. Slices, prior filtered/32-bit masks, duplicate long values and immutable payload backing stay intact.

One package-private `M3SignalOrder.sortUnsignedKeyRows` bridge calls the original sorter; no second engine or JNI ABI. Explicit `Backend.NATIVE_REQUIRED` delegates to original `M3SignalOrderNative.sort` and `m3_signal_order_jni.c`. It fails closed if unavailable. No-argument sorting uses the fast Java radix for eligible large batches and automatic heap headroom, otherwise retains the original stable merge code. Candidate threshold (4096) is conservative and must be performance-qualified.

**Exact canonical source image SHA-256**
- `M3LongIndexProjection.java` pre: `c6159ed8f93b8ed364cd67bd2aeb82e246a7eaf93506cf4b38ac5a0ec09ae2ec`; post: `57acf6174c2e2f15931fb1e43cdc6026501a8bbb8789d57a5e143a2ec24714b3`.
- `M3SignalOrder.java` pre: `da761a6b4f10d9a3c9a61979a4ee238fd6d26492d1ba0e6daacdf371f6561058`; post: `01a27d87732081d6c0ada76e1ed4258b25fec0a558caa6826167918cc956a60f`.
- Standalone Java21/JNI oracle: `6d2e63f078f6f0a6b268144dcdcbc15c574694db330aa3ac2c4b58aebd262959`.

CI compiles actual product owners using Java 21 `-Xlint:all -Werror`, runs the differential proof, builds unchanged existing C17 JNI radix with UBSan and executes `-Xcheck:jni`. The canonical recipe has its own Maven/OpenRewrite LST fixed-point, refusal and source stamp tests. The local independent algorithm proof passed 1,075,100 signed-long stability checks; that is not a full patched JDK build or universal speed claim.

M3 phase ordering String -> arrays -> collections is unchanged. No automatic native route, retained per-row Entry, new object store or `java.util` API replacement. Full reactor/JCK/JTReg/HotSpot, memory pressure and A/B throughput gates remain unqualified until separately executed.
