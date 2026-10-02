# Retained-admission and enhancement-porting increment

This is a partial implementation on the existing migration owner, not completion of the MIndex family or acceptance of a modified JDK.

## Pins and ownership

Target parent: draft #12 `d543255294ae85e4c8015812a3c8a97aaf498354`; observed master `8bb6215372e07712f1fdf5a0cb912af495007b19`. Source inspected: private `hsoliwal/com.synexia@8830a34a042d79e2d1b89b850d177c3038bf975e`. New source enhancement-porting owner: source draft #7634 `8a3915a194a2b0a2ce519469fad10a1ae74df170`. Keep `m3/migration/manifest.json` authoritative. #13 owns the separate exact-source recipe port; #14 owns the bounded filename census and broader documentation. Do not replace any of these with a parallel registry.

## Contract-preserving repair

The inspected Route A hashes a caller-controlled CharSequence before copying it. A changing sequence can therefore publish one spelling with another spelling's hash. Capture each UTF-16 unit once, then hash and admit the same private snapshot. Caller callbacks must not execute under the interner monitor.

Keep canonical payload lookup separate from weak facade lookup inside the existing owner. A live range retains the root LocalM3StringPiece, so collecting the original facade does not permit a second payload for the still-live atom. Unknown text remains local. This does not turn the foundation's copy-on-admission mapped reader into direct shared backing.

Preflight logical length before joined descriptor allocation. Normalize empty ranges and coalesce only truly adjacent coordinates over the same backing/root/encoding. Use a segment cursor for sequential content operations and validate any benign locality hint before use. Public Java declarations, UTF-16 indexing, independent mutable outputs and exact equality remain unchanged. Flat directories still cost O(S) per join; repeated appending can remain O(S squared). No blanket zero-allocation or speed claim.

## Compiler route

Implement a narrowly eligible compiler-backed recipe for explicit M3Text.fromString admissions whose concatenation operands are resolved immutable String constants. Do not rewrite arbitrary String +, evaluated receivers, dynamic values, unresolved symbols, identity-sensitive ordinary String operations or escaped boundaries. Preserve source text outside the eligible invocation and refuse transformations that lose comments or do not re-type-check. Bind the compiler to an immutable snapshot of the exact verified owner class closure. Compiler tooling stays outside java.base.

## JNI route boundary

JNI remains optional and cross-cutting. The new native probe tests ordinary String projections from Route A, including UTF-16, modified UTF-8, regions, null/error and critical release paths under -Xcheck:jni and UBSan. It is not a new storage authority and not proof of the modified-JDK route.

## Recipes and acceptance

The Maven/OpenRewrite safety recipe and dependency-free exact replay share pinned pre/postimages. Preflight every atom before mutation; refuse drift, missing owners and unapproved partial states. Idempotence and reverse replay must preserve later edits. Per-file atomic replacement is not a whole-change crash transaction; interrupted mixed state remains visible and requires explicit recovery. Existing foundation source pins are updated mechanically with their tests and runtime gates retained, not deleted or weakened.

The source-side three-way porting gate is an exact source/test port, preserving its package and Apache-2.0 notice in tooling. It plans only: no automatic overwrite, merge or reverse-port authority. Source/target changes, tombstones, missing observations and dependency closure remain distinct.

Mandatory open gates include complete semantic inventory, every-family ports, full shared/local owner convergence, general compiler lowering, matched exact-head JDK images, historical enabled StringJoiner OOME failures, JIT/intrinsics/CDS/GC/JVMTI/serviceability and full retained-memory/performance acceptance. Historical #6 receipts remain tied to 3776d6e674d6c9b04539ca24aca2504aa88d4a57.

Only explicitly requested migration source is published from the private repository. No unrelated source, datasets, credentials or challenge solution code is included. OpenJDK licensing and notices remain unchanged.
