SLF4J API 2.0.12 as vendored in M3jdk21 at e3c8bb1d0e3 (m3/vendor/third_party/slf4j-api-2.0.12, 47 Java files; read from the git tree).
76 String and StringBuilder call sites in 7 files; org.slf4j.helpers.MessageFormatter holds 49 of them.
Maven fork 3b1854f5: 114 main files import org.slf4j, 605 logging call sites, 119 with {} placeholders on the same line.
"lp4j" was read as SLF4J (Maven logging, vendored in M3JDK) and as LSP4J; the owner has not said which. Log4j and LSP4J are used nowhere in the Maven fork or in M3JDK.
