# Acceptance matrix — MIndex -> M3

Evidence states used here:
- **tested**: executed in this pass or tied to an exact checked-in historical candidate
- **implemented/unverified**: code exists but this pass did not execute the required gate
- **partial**: only a bounded contract slice is implemented
- **blocked**: execution/publication unavailable in the current tool path
- **pending**: not implemented in the M3 target yet

| Capability | Route A | Route B | Route C |
| --- | --- | --- | --- |
| Exact UTF-16 length/charAt | tested locally | source owner inventoried | historical enabled tests pass |
| Arbitrary UTF-16 ranges | tested locally | pending lowering proof | historical substring test passes |
| Unpaired surrogates | tested locally | pending | historical supplementary/API evidence only |
| Surrogate pair across segment seam | tested locally | pending | focused historical candidate evidence |
| Java hashCode independent of segmentation | tested locally | pending | historical HashCode + focused evidence |
| Immutable concat payload reuse | partial/tested for local payloads | pending | historical allocation evidence |
| Shared mapped lexicon payload | pending | source candidates inventoried | partial historical candidate |
| Weak/local miss interning | tested for wrapper/content behavior | source owner inventoried | partial historical candidate |
| toCharArray mutation isolation | tested locally | boundary proof pending | historical upstream test passes |
| indexOf across seams | tested locally with exact KMP | lowering pending | focused candidate evidence |
| lastIndexOf | implemented; full differential pending | pending | upstream surface evidence incomplete |
| java.util.regex CharSequence use | tested simple seam case | lowering pending | historical Regex test passes |
| Full regex flags/regions/captures/lookarounds/replacement matrix | pending | pending | not fully evidenced |
| Case-insensitive compare/equality | implemented via JDK materialization; differential pending | pending | historical focused tests pass |
| Encoding/decoding policy matrix | pending | pending | historical Encodings/nativeEncoding pass with feature caveats |
| String.intern identity | not claimed | boundary rule pending | historical Intern test passes; compiled/serviceability gates open |
| invokedynamic concatenation | explicit API only | pending exact lowering proof | historical concat tests pass in checked candidate modes |
| JNI ownership/release/modified UTF-8 | optional backend inventoried | cross-cutting pending | partial historical JNI evidence |
| JVMTI | not applicable to explicit view | not applicable | historical callbacks only; broad gate open |
| GC/dedup | ordinary Java reachability | not applicable | historical focused evidence; complete compiled gate open |
| CDS/reflection/serviceability | not claimed | boundary only | pending comprehensive acceptance |
| Full Java 21 String surface | partial | pending | partial; not full jtreg/JCK |
| Complete JDK image | stock JDK view only | stock/compiler route | historical candidate build only; must rebuild exact eventual head |

## Route A local receipt

Environment: OpenJDK 21.0.11.  
Compilation: `javac --release 21 -Xlint:all -Werror` over the exact M3 text source closure used by the test.  
Execution modes: default, `-Xint`, `-XX:-CompactStrings`, and `-Xbatch -XX:-TieredCompilation`.  
Result in each mode: `ROUTE_A_PASS checks=15536`.

This receipt proves only the bounded Route A source closure. It is not a full `m3/build.sh` run, a Maven reactor run, a modified-JDK image build, or PR-head CI.

## Route C historical receipt

Candidate: `hsoliwal/M3jdk21#6@3776d6e674d6c9b04539ca24aca2504aa88d4a57`.

Checked-in historical evidence:
- flag off: 86 passed, 0 failed
- enabled focused upstream set: 16 passed, 2 failed
- failing tests: `java/util/StringJoiner/StringJoinerOomUtf16Test` and `java/util/StringJoiner/StringJoinerTest`
- enabled segmented execution was interpreter-only
- full jtreg/JCK, compiled segmented execution and live SA attach were not completed

No later source changes inherit this evidence automatically.
