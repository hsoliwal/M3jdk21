# NATIVE_ORDER_MONITOR fix (2026-11-06)

## Defect

`NativeLineBatch.stableOrder(..., ProgressMonitor)` passes a `java.util.concurrent.ProgressMonitor`, the M3jdk21
java.base owner. #2498 relocated the Java side onto that type. It left the C upcall in `native/m3unixtext.c:282`
looking up the donor name `checkCanceled`, but java.base declares `checkCancelled`.

Effect: with any non-null monitor, `GetMethodID` fails and the call throws `NoSuchMethodError: checkCanceled`.

Fix: one string literal in `m3unixtext.c`, `"checkCanceled"` -> `"checkCancelled"`. The C field name is unchanged.

## Gates

Gates run on stock JDK 21.0.9 + MinGW gcc, against master e5f826ace24c:

| gate | live (master) | fix |
| --- | --- | --- |
| `order_gate.sh`: javac main + relocated test, java.base patched with master's Progress*/Monitored* sources | 0 errors | 0 errors |
| gcc -std=c11 -Wall -Wextra -Werror, all 5 natives | rc=0 | rc=0 |
| `NativeLineBatchOrderSelfTest` (native) | **FAIL**: NoSuchMethodError: checkCanceled | **PASS**: `M3_NATIVE_ORDER_PASS checks=25584 seed=4d334f5244455231` |
| `jni_upcall.py` (JNI_UPCALL v1) | **unbound=1** (`m3unixtext.c:282 checkCanceled()V`) | unbound=0 |

## Test received

The donor test is `synexia-unix4j-m3 NativeLineBatchOrderSelfTest`, blob c923e4871ddb.
- It was held at #2503 because it uses the donor-only `ProgressState` API.
- `ord_relocate.py` (PROGRESS_MONITOR_TEST_MAP v1, fail-closed, 10 rules) now relocates it into `jdk.m3.text`. The rules map its probe onto java.base `ProgressMonitor`:
  - `beginTask` -> `begin`
  - `isCanceled` -> `isCancelled`
  - `setCanceled` -> `cancel`
  - `setProgress`, `setTaskName`, `subTask` and `state()` are dropped; the kernel never calls them.
- `qualification/converge-20261106/run_list.tsv` gains the row, so `gate4.sh` runs it with the other harnesses.

## Sweep

On master, the natives contain one other donor-named upcall: `m3/search/src/main/native/jni_search.c:165`. It looks up `checkCanceled()V` on `jdk.m3.collections.M3ProgressMonitor`, which declares it, so that call is correct.
