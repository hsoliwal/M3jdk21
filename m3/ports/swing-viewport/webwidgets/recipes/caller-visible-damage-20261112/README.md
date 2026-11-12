# Selected producer recipe: visible caller damage

This Maven recipe reuses the existing `M3WebGen.emit` producer and the two qualified generator templates for `M3WebLanes` and `M3WebChecks`. The generator templates are the owning implementation sources. Generated Java is produced by this recipe and must not be patched separately.

The caller repair ignores unresident item rectangles, clips each damage input before accumulation, calculates padded intersections with wide intermediates, and preserves the existing virtual damage hook and backend exception/output effects. It adds no retained fields to the production owner and changes no public or protected API.

## Reuse and custody

Producer: `hsoliwal/M3jdk21@b98bf8c065818aac3447be165909e2a013325d9a`, `m3/ports/swing-viewport/webwidgets/gen/M3WebGen.java`, SHA-256 `0ea3cf99fb09e346774715f69f1b47565ade39c2121a5e470565b2d55401a150`.

The template after-images come unchanged from the previously qualified Synexia caller repair. Its generated Java after-images receive only the mechanical `com.m3.` to `jdk.m3.` namespace substitution. Original Apache-2.0 OR EPL-2.0 notices are retained. The producer differs from the historical qualified producer only in its two namespace strings.

`bindings.tsv` closes the scope to one producer, two templates, two generated Java files, fifteen unchanged compilation dependencies and one unchanged approved oracle. Template sources must already equal the pinned after-images before apply; this controller does not bypass a snapshot engine to edit templates.

## Running

Use JDK 21 and Maven. All three roots must be absolute paths. Use a fresh output root for each invocation and keep its temporary files on that same admitted output lane.

```sh
mvn -B -ntp -f /absolute/recipe/pom.xml verify \
  -Dm3.caller.receiverRoot=/absolute/M3jdk21 \
  -Dm3.caller.recipeRoot=/absolute/recipe \
  -Dm3.caller.outputRoot=/admitted/fresh/qualification \
  -Dm3.caller.mode=qualify
```

Run again with another fresh output root and `-Dm3.caller.mode=apply` after qualification. Commit only the exact two generator outputs, together with the owning template edits and this recipe. Repeating apply must leave their bytes and modification times unchanged.

Qualification emits before and after sources in isolation, compares exact hashes, verifies same-instance and fresh-instance zero-write replay, compiles each seventeen-source closure with `--release 21 -Xlint:all -Werror`, and compares the protected API of `M3WebLanes`. A failed preflight must not modify receiving Java.

## Qualification limits

Historical evidence is not fresh namespace qualification. The previous generator proof established exact two-file emission and zero-write replay; the previous API proof found identical protected surfaces. Before this new recipe runs, its current-namespace status is NOT_RUN.

The approved eighteen-method `M3ViewportCoreApiTest` is retained byte-for-byte under `fixtures/` and is outside the two generated targets. It is compiled into a separate test output, ahead of the ordinary four-method test of the same name, and run in a separate CI job. All failures and the nonzero process status remain visible. No test is removed or weakened.

This caller change does not implement the separate full-domain accumulator repair. That repair still requires exact wide unions, checked legacy projection with retained state, and `takeWide(long[])`. Passing the caller/public suite cannot qualify those outstanding contracts or complete the wider Synexia UI goal.
