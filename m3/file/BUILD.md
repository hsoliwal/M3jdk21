# m3/file build

Module `org.m3jdk.file` (packages `jdk.m3.file`, `jdk.m3.file.json`), Maven coordinates
`com.m3:m3-file:0.1.0-SNAPSHOT`, Java 21; `com.m3:m3-collections` (the progress-monitor owner
`jdk.m3.collections.M3ProgressMonitor`, which `jdk.m3.file.M3ProgressMonitor` extends since
`m3/qualification/progress-monitor-unify-20261106`) is the only dependency outside `java.base`.

| Step | Command |
|---|---|
| compile + jar | `mvn -f m3/file/pom.xml package` (JDK 21; `-Xlint:all,-module -Werror`, the lint set of `qualification/m3fmt-shared-lexicon-20261021/verify.sh`) |
| from the control reactor | `mvn -f m3/pom.xml -pl file package` (`file` is a default module of `m3/pom.xml`) |
| natives | `cmake -S m3/file/src/main/native -B <build> && cmake --build <build>`: `m3_file_jni.c`, `m3_json_signal_jni.c`, `m3_text_metrics_jni.c`, `m3fmt_varints_jni.c`; loaded at run time by `jdk.m3.file.M3JniLibraryLoader` from an `M3NativeLibrarySpec` |
| qualification | `bash m3/file/qualification/m3fmt-shared-lexicon-20261021/verify.sh` (javac gate, JNI header check, contracts, gcc build of the varints library) |

Consumers depend on the module with

```xml
<dependency>
  <groupId>com.m3</groupId>
  <artifactId>m3-file</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

and `requires org.m3jdk.file;` in their module descriptor. The sources are not changed by this build; they keep
the 2026-10-31 JDK naming (`M3*` types, `M3JDK_*` constants). The first consumer is the native search engine
(`m3/search`, `jdk.m3.search`), whose loader owner is `jdk.m3.file.M3JniLibraryLoader`.
