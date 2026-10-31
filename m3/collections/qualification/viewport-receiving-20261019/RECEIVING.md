# M3JDK21 folded SWT/Nebula viewport receiving — 2026-10-19

Product baseline: 677437be295b23f7ccf7409be287008944d0e8b0, containing the PR1403
viewport fold. Modify the existing M3WindowPlan and M3RowTree, not an added toolkit
wrapper or runtime layer. Canonical recipe and source qualification:
https://github.com/hsoliwal/com.synexia/pull/12482
Commit caa565d6b8d5e09e533a4c862a163b8efb2ef094; docs-first
5502e89a3cc83adb6c245864cb360dd111842718.
Task: synexia-openrewrite-recipes/recipes/widget-backing-20261014/viewport-geometry-20261019/m3jdk21-receiving-20261019/.
Recipe: com.synexia.rewrite.M3Jdk21ViewportReceivingRecipe; unchanged canonical
M3GpuToolJoinRecipe/Manifest. Synexia is recipe custody, not a runtime dependency.

## Two original owners

M3WindowPlan receives SWT229 checked insertion/removal endpoints and resident
growth admission. Invalid overflowing edits fail before committed or planned state
changes; valid edits, native-index mapping, existing error text and declared API
remain. Existing touched()/covers() behavior is deliberately unchanged.

M3RowTree keeps its original dynamic array AVL slots, parent/child indexes, sums,
visible counts, free slots and payload identity. Endpoint queries can return cached
visible count only when visible(root)==size(root), proving positive weights (or an
empty tree). Unlike the Fenwick donor, custom height+1 may form negative int weights.
Weights10,-20,15 total5 with query5 distinguish the legacy descent from an unsafe
unconditional shortcut. Hidden/signed fallback, updates and rotations remain.

M3IndexMaskCompactor Java/C are exact KEEP dependencies, including the current
required/excluded masks and nine native exports. No native callback, JNI symbol,
new field, retained per-entry structural object or public signature is introduced.
The tests compose stable repeated-key display order, required/excluded Java/native
mask selection, hidden row retention, rank/pixel lookup and resident-window mapping
back to the same original objects. Payload fixtures implement the real item API;
they are not stand-ins for the product tree, heap policy, selector or widget.

## Executed source qualification

Seven complete Java owners compiled with Java21 --release21 -proc:none -Xlint:all
-Werror after source checks. Serial lanes: baseline, window-only, window/tree.
Ordinary window6758400 assertions; corrected boundaries6758426. Tree composition
269096 assertions, digest16622768763742859435, identical across all lanes and
real JNI execution, including32MiB repeat. Complete javap -p -s of the two owners
and iterator matches. C17 exact current selector: generated header, strict warnings,
UBSan/no-recover, nine exports and -Xcheck:jni. Two compiled wrong variants each
fail twice. Missing-native mode fails rather than silently falling back.

The manifest kernel passed2111 initial/2122 replay checks, all2048 mixed states,
refusal and fixed point; five generated files plus six KEEP inputs. It is NOT
OpenRewrite SDK execution. Maven was actually attempted and exited127: mvn absent.
SDK/JUnit fixture is authored NOT_RUN. Source seals and frozen tests are canonical;
no target output was manually repaired. Raw42 command logs are in the archive;
canonical EXECUTED.log and QUALIFICATION.json retain source-bound evidence.

## Performance and acceptance HOLD

Two fresh process pairs, second order reversed. CPU ns/query baseline->candidate:
positive endpoints58.11->5.29 and62.23->5.28; interior189.24->171.17 and177.86->190.11;
mixed108.31->60.24 and99.27->55.77; hidden endpoints60.04->67.45 and61.53->81.41.
Warm query allocations0 both. Process maximum RSS64012->73800KiB and62548->74940KiB.
Adverse hidden/interior timings and RSS remain recorded. No universal speedup,
retained-heap reduction, native GUI performance or installed-JDK acceptance claim.

Actual OpenRewrite SDK, default JUnit discovery, complete module, custom VM/JDK
image, java.base/java.desktop packaging, jtreg/JCK and original SWT/Nebula native
Display/events/pixels remain NOT_RUN. Skija/RWT/GWT/React and structural token-regex
reconciliation remain separate. Source receiving does not advance String-active
phase or prove design-only classes exist. See COVERAGE.tsv. No root/default-branch
write, merge, rebase, force push or old PR replacement is part of this receiver.

To reproduce source-bound Java/C17 tests, run verify.py with a fresh campaign name
in the pinned canonical packet or delivered archive. Maven needs the full canonical
checkout and approved custom SDK dependencies. The archive is not a JDK checkout.
