<!-- SPDX-License-Identifier: Apache-2.0 -->

# Package and source-directory accounting

[Guide](../README.md) · [M3 semantic guide](../m3-packages.md) · [JDK modules](../jdk-modules.md) · [Coverage](../coverage.md)

The complete tracked-tree traversal found **13,793 physical directories containing Java source other than module-info.java**. All have a row below. This is a search/inventory denominator, not a claim of 13,793 distinct Java packages. Default-package tests, generated templates, vendored snapshots, duplicate namespaces and platform roots are deliberately retained. Resolve package declarations and source-set ownership before making namespace-level completion claims. An additional 559 directories contain module-info.java only; they are accounted for in the [descriptor ledger](../descriptors.md).

Evidence statuses: `PATH_ONLY` means path/type presence only; `DOC_LINKED` locates an existing package-info.java/package.html without claiming a full review; `OBSERVED_SAMPLE` identifies the 44 roots with a read source declaration and [partial semantic documentation](../m3-packages.md). No row means tests passed. A directory index is not substantive package documentation.

| Class | Directory rows |
| --- | ---: |
| jdk | 943 |
| m3 | 115 |
| custody | 1712 |
| test | 10910 |
| support | 113 |

| Shard | Rows | First path | Last path |
| --- | ---: | --- | --- |
| [jdk-01.md](jdk-01.md) | 400 | `src/java.base/aix/classes/sun/nio/ch` | `src/java.management/share/classes/javax/management/loading` |
| [jdk-02.md](jdk-02.md) | 400 | `src/java.management/share/classes/javax/management/modelmbean` | `src/jdk.internal.vm.ci/share/classes/jdk/vm/ci/code` |
| [jdk-03.md](jdk-03.md) | 143 | `src/jdk.internal.vm.ci/share/classes/jdk/vm/ci/code/site` | `src/jdk.zipfs/share/classes/jdk/nio/zipfs` |
| [m3-01.md](m3-01.md) | 115 | `m3/algorithms/src/com/m3/algorithm` | `m3/tooling/vi/src/test/java/com/m3/vi` |
| [custody-01.md](custody-01.md) | 400 | `m3/synexia-import/intakes/regex-precompute-v7-20261007/evidence/importer-replay-v3/drift-receiver/m3/synexia-import/src/main/java/com/m3/synexia/importer` | `m3/vendor/third_party/commonmark/src/main/java/org/commonmark/renderer/markdown` |
| [custody-02.md](custody-02.md) | 400 | `m3/vendor/third_party/commonmark/src/main/java/org/commonmark/renderer/text` | `m3/vendor/third_party/gizmo/src/main/java/io/quarkus/gizmo` |
| [custody-03.md](custody-03.md) | 400 | `m3/vendor/third_party/guava/src/main/java/com/google/common/annotations` | `m3/vendor/third_party/lucene-core/src/main/java/org/apache/lucene/util/packed` |
| [custody-04.md](custody-04.md) | 400 | `m3/vendor/third_party/lucene-core/src/main/java/org/apache/lucene/util/quantization` | `m3/vendor/third_party/snakeyaml-2.2/src/main/java/org/yaml/snakeyaml/external/biz/base64Coder` |
| [custody-05.md](custody-05.md) | 112 | `m3/vendor/third_party/snakeyaml-2.2/src/main/java/org/yaml/snakeyaml/external/com/google/gdata/util/common/base` | `m3/vendor/third_party/zero-allocation-hashing/upstream-excluded/java/lang` |
| [test-01.md](test-01.md) | 400 | `test/failure_handler/src/share/classes/jdk/test/failurehandler` | `test/hotspot/jtreg/runtime/cds/appcds/jigsaw/classpathtests/src/jdk/test` |
| [test-02.md](test-02.md) | 400 | `test/hotspot/jtreg/runtime/cds/appcds/jigsaw/classpathtests/src/sun/nio/cs/ext` | `test/hotspot/jtreg/vmTestbase/gc/g1/unloading/tests/unloading_keepRef_staticField_inMemoryCompilation_keep_obj` |
| [test-03.md](test-03.md) | 400 | `test/hotspot/jtreg/vmTestbase/gc/g1/unloading/tests/unloading_keepRef_staticField_keep_cl` | `test/hotspot/jtreg/vmTestbase/nsk/jdb/next/next001` |
| [test-04.md](test-04.md) | 400 | `test/hotspot/jtreg/vmTestbase/nsk/jdb/options/connect/connect001` | `test/hotspot/jtreg/vmTestbase/nsk/jdi/EventQueue/remove_l/remove_l001` |
| [test-05.md](test-05.md) | 400 | `test/hotspot/jtreg/vmTestbase/nsk/jdi/EventQueue/remove_l/remove_l002` | `test/hotspot/jtreg/vmTestbase/nsk/jdi/Method/locationOfCodeIndex` |
| [test-06.md](test-06.md) | 400 | `test/hotspot/jtreg/vmTestbase/nsk/jdi/Method/locationOfCodeIndex/locationofcodeindex001` | `test/hotspot/jtreg/vmTestbase/nsk/jdi/ReferenceType/nestedTypes/nestedtypes001` |
| [test-07.md](test-07.md) | 400 | `test/hotspot/jtreg/vmTestbase/nsk/jdi/ReferenceType/nestedTypes/nestedtypes002` | `test/hotspot/jtreg/vmTestbase/nsk/jdi/VirtualMachine/redefineClasses/redefineclasses003` |
| [test-08.md](test-08.md) | 400 | `test/hotspot/jtreg/vmTestbase/nsk/jdi/VirtualMachine/redefineClasses/redefineclasses003/newclass` | `test/hotspot/jtreg/vmTestbase/nsk/jdwp/VirtualMachine/CapabilitiesNew` |
| [test-09.md](test-09.md) | 400 | `test/hotspot/jtreg/vmTestbase/nsk/jdwp/VirtualMachine/CapabilitiesNew/capabilitiesnew001` | `test/hotspot/jtreg/vmTestbase/nsk/jvmti/IterateThroughHeap/filter-class-tagged` |
| [test-10.md](test-10.md) | 400 | `test/hotspot/jtreg/vmTestbase/nsk/jvmti/IterateThroughHeap/filter-class-untagged` | `test/hotspot/jtreg/vmTestbase/nsk/jvmti/scenarios/hotswap/HS302/hs302t007r` |
| [test-11.md](test-11.md) | 400 | `test/hotspot/jtreg/vmTestbase/nsk/jvmti/scenarios/hotswap/HS302/hs302t008` | `test/hotspot/jtreg/vmTestbase/nsk/monitoring/ThreadMXBean/ThreadInfo/NativeBlockedThread/NativeBlockedThread004` |
| [test-12.md](test-12.md) | 400 | `test/hotspot/jtreg/vmTestbase/nsk/monitoring/ThreadMXBean/ThreadInfo/NativeBlockedThread/NativeBlockedThread005` | `test/hotspot/jtreg/vmTestbase/vm/jit/LongTransitions/nativeFnc43` |
| [test-13.md](test-13.md) | 400 | `test/hotspot/jtreg/vmTestbase/vm/jit/LongTransitions/nativeFnc44` | `test/jdk/java/awt/Dialog/DialogOverflowSizeTest` |
| [test-14.md](test-14.md) | 400 | `test/jdk/java/awt/Dialog/DocumentModalSheetTest` | `test/jdk/java/awt/Toolkit/LoadAWTCrashTest` |
| [test-15.md](test-15.md) | 400 | `test/jdk/java/awt/Toolkit/LockingKeyStateTest` | `test/jdk/java/io/FileReader` |
| [test-16.md](test-16.md) | 400 | `test/jdk/java/io/FileWriter` | `test/jdk/java/lang/instrument/RedefinePermittedSubclassesAttr/HostAB` |
| [test-17.md](test-17.md) | 400 | `test/jdk/java/lang/instrument/RedefinePermittedSubclassesAttr/HostAB/redef` | `test/jdk/java/rmi/registry/readTest/testPkg` |
| [test-18.md](test-18.md) | 400 | `test/jdk/java/rmi/registry/reexport` | `test/jdk/java/util/logging/LogManager` |
| [test-19.md](test-19.md) | 400 | `test/jdk/java/util/logging/LogManager/Configuration` | `test/jdk/javax/swing/JComboBox/ShowPopupAfterHidePopupTest` |
| [test-20.md](test-20.md) | 400 | `test/jdk/javax/swing/JComboBox/WindowsComboBoxSize` | `test/jdk/javax/swing/ToolTipManager/7123767` |
| [test-21.md](test-21.md) | 400 | `test/jdk/javax/swing/ToolTipManager/JMenuItemToolTipKeyBindingsTest` | `test/jdk/sun/awt/dnd/8024061` |
| [test-22.md](test-22.md) | 400 | `test/jdk/sun/awt/font` | `test/jdk/tools/launcher/modules/limitmods` |
| [test-23.md](test-23.md) | 400 | `test/jdk/tools/launcher/modules/limitmods/src/test/jdk/test` | `test/langtools/jdk/javadoc/doclet/testReporterStreams` |
| [test-24.md](test-24.md) | 400 | `test/langtools/jdk/javadoc/doclet/testReturnTag` | `test/langtools/tools/javac/T6970173` |
| [test-25.md](test-25.md) | 400 | `test/langtools/tools/javac/T7008643` | `test/langtools/tools/javac/diags/examples/NotDefPublicCantAccessFragment` |
| [test-26.md](test-26.md) | 400 | `test/langtools/tools/javac/diags/examples/NotDefPublicCantAccessFragment/p` | `test/langtools/tools/javac/lambda/bytecode` |
| [test-27.md](test-27.md) | 400 | `test/langtools/tools/javac/lambda/considerExceptionTVarInStuckExprs` | `test/lib` |
| [test-28.md](test-28.md) | 110 | `test/lib-test` | `test/micro/org/openjdk/bench/vm/lang` |
| [support-01.md](support-01.md) | 113 | `.m3/openrewrite-recipes/src/main/java/com/synexia/m3/bootstrap` | `src/utils/src/build/tools/dirdiff` |
