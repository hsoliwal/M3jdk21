<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Integrated M3JDK21 utility coverage

This table is the human-readable projection of the existing naming map fragment
`m3/docs/name-mapping.d/m3-integrated-utility-families-20261018.json`. The existing `m3/docs/name-mapping.json` remains the registry authority.

M3JDK21 supplies these APIs in their owning JDK modules. Existing public JDK names remain
unchanged; new utility entry points are explicit additive candidates. No companion M3 utility
runtime is introduced. The source inventory is pinned to `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.

**Evidence boundary:** a public source declaration and module export establish baseline
ownership, not a complete API superset or executed behavior. New candidates have no invented
pass/implemented status. The existing receiving plan and all phase statuses are preserved.

| Family | Existing/public entry point or concrete gap | Owning module(s) | New candidate in this packet |
| --- | --- | --- | --- |
| Objects | java.util.Objects | `java.base` | None; retain existing owner / record follow-on gap |
| Strings | java.util.Strings | `java.base` | None; retain existing owner / record follow-on gap |
| Arrays | java.util.Arrays | `java.base` | None; retain existing owner / record follow-on gap |
| Collections | java.util.Collections | `java.base` | `java.util.Collections` |
| Iterators | Distributed protocols; no public java.util.Iterators at the baseline | `java.base` | None; retain existing owner / record follow-on gap |
| Spliterators | java.util.Spliterators | `java.base` | None; retain existing owner / record follow-on gap |
| Streams | Public Stream/primitive Stream factories and StreamSupport; existing Streams class is package-private | `java.base` | `java.util.stream.Streams` |
| Collectors | java.util.stream.Collectors | `java.base` | None; retain existing owner / record follow-on gap |
| Functions | Composition methods on functional interfaces; no public java.util.function.Functions at baseline | `java.base` | None; retain existing owner / record follow-on gap |
| Reflection | Array/Modifier/Proxy are existing focused utilities; general Reflection helper is a candidate | `java.base` | `java.lang.reflect.Reflection` |
| Classes | java.lang.Class and ClassLoader APIs | `java.base` | None; retain existing owner / record follow-on gap |
| Method and variable handles | java.lang.invoke.MethodHandles | `java.base` | None; retain existing owner / record follow-on gap |
| Input and output | No general public IOUtils at baseline; candidate java.io.IOUtils | `java.base` | `java.io.IOUtils` |
| Files | java.nio.file.Files | `java.base` | None; retain existing owner / record follow-on gap |
| Paths | java.nio.file.Paths and Path.of | `java.base` | None; retain existing owner / record follow-on gap |
| Buffers | Public buffer factories and methods; no general Buffers utility at baseline | `java.base` | None; retain existing owner / record follow-on gap |
| Channels | java.nio.channels.Channels | `java.base` | None; retain existing owner / record follow-on gap |
| Resources | ResourceBundle and ClassLoader discovery; AutoCloseable lifecycle protocol | `java.base` | None; retain existing owner / record follow-on gap |
| Charsets | java.nio.charset.Charset and StandardCharsets | `java.base` | None; retain existing owner / record follow-on gap |
| Regular expressions | java.util.regex.Pattern factories and Matcher execution | `java.base` | None; retain existing owner / record follow-on gap |
| Numbers | Numeric type factories and static helpers; no general Numbers utility at baseline | `java.base` | None; retain existing owner / record follow-on gap |
| Mathematics | java.lang.Math and StrictMath | `java.base` | None; retain existing owner / record follow-on gap |
| Bits and masks | BitSet plus Integer/Long bit utilities | `java.base` | None; retain existing owner / record follow-on gap |
| Date and time | Focused java.time factories, TemporalAdjusters and DateTimeFormatter | `java.base` | None; retain existing owner / record follow-on gap |
| Concurrency | Executors, CompletableFuture, TimeUnit and LockSupport | `java.base` | None; retain existing owner / record follow-on gap |
| Processes | ProcessBuilder and ProcessHandle factories | `java.base` | None; retain existing owner / record follow-on gap |
| Networking | Existing URI/URL/encoding/address APIs and java.net.http.HttpClient | `java.base`, `java.net.http` | None; retain existing owner / record follow-on gap |
| Archives | Existing ZIP/JAR owners; no general Archives utility at baseline | `java.base` | None; retain existing owner / record follow-on gap |
| Digests and message authentication | java.security.MessageDigest and javax.crypto.Mac factories | `java.base` | None; retain existing owner / record follow-on gap |
| JDBC | DriverManager and RowSetProvider factories with JDBC interfaces | `java.sql`, `java.sql.rowset` | None; retain existing owner / record follow-on gap |
| Modules and services | ModuleFinder/ModuleDescriptor/Configuration and ServiceLoader | `java.base` | None; retain existing owner / record follow-on gap |

## Complete source-module ownership index

The linked naming-map fragment `m3/docs/name-mapping.d/m3-jdk-module-owners-20261018.json`
is generated from every `src/<module>/<platform>/classes/module-info.java` in the pinned Git tree.
It covers **70 source module declarations and 227 unqualified exported packages**, with
**366 representative public source types** across the core and extended indexes. All modules
with public package exports have at least one authenticated existing owner reference. The
representative types do not enumerate every JDK class or method, and neither index establishes
universal utility/API completeness, Java SE conformance, or a complete runtime image.

The Windows-only `jdk.crypto.mscapi` declaration is recorded as conditional. `java.se` is an
aggregate of existing module requirements. Modules with no public exports retain their actual
tool, service-provider, runtime or internal owner disposition. In particular, jlink and jpackage
are reached through their declared `java.util.spi.ToolProvider` services; their internal packages
are not turned into public utilities. Preview foreign-memory, incubating vector and unsupported
module APIs retain those distinctions. `src/hotspot`, `src/demo` and `src/utils` are explicitly
classified as non-module source roots rather than silently omitted.

### Additional aspect owners

| Aspect | Existing public utility, factory or protocol owners | Modules |
| --- | --- | --- |
| Desktop graphics and event dispatch | `java.awt.EventQueue`, `java.awt.Desktop`, `java.awt.Toolkit`, `java.awt.GraphicsEnvironment` | `java.desktop` |
| Swing widgets and text | `javax.swing.SwingUtilities`, `javax.swing.UIManager`, `javax.swing.BorderFactory`, `javax.swing.text.Utilities` | `java.desktop` |
| Images and codec providers | `javax.imageio.ImageIO`, `javax.imageio.spi.IIORegistry`, `javax.imageio.ImageReader`, `javax.imageio.ImageWriter` | `java.desktop` |
| Beans and property metadata | `java.beans.Beans`, `java.beans.Introspector`, `java.beans.PropertyChangeSupport` | `java.desktop` |
| Clipboard and data transfer | `java.awt.datatransfer.DataFlavor`, `java.awt.datatransfer.SystemFlavorMap`, `java.awt.datatransfer.Clipboard` | `java.datatransfer` |
| Sound MIDI and printing | `javax.sound.sampled.AudioSystem`, `javax.sound.midi.MidiSystem`, `javax.print.PrintServiceLookup`, `java.awt.print.PrinterJob` | `java.desktop` |
| XML parsers validation and catalogues | `javax.xml.parsers.DocumentBuilderFactory`, `javax.xml.parsers.SAXParserFactory`, `javax.xml.stream.XMLInputFactory`, `javax.xml.validation.SchemaFactory`, `javax.xml.catalog.CatalogManager`, `javax.xml.datatype.DatatypeFactory` | `java.xml` |
| XPath XSLT and XML signatures | `javax.xml.xpath.XPathFactory`, `javax.xml.transform.TransformerFactory`, `javax.xml.crypto.dsig.XMLSignatureFactory`, `javax.xml.crypto.dsig.keyinfo.KeyInfoFactory` | `java.xml`, `java.xml.crypto` |
| Management and JMX | `java.lang.management.ManagementFactory`, `javax.management.JMX`, `javax.management.MBeanServerFactory`, `javax.management.remote.JMXConnectorFactory` | `java.management` |
| Directory naming and preferences | `javax.naming.spi.NamingManager`, `javax.naming.InitialContext`, `javax.naming.directory.InitialDirContext`, `javax.naming.ldap.InitialLdapContext`, `java.util.prefs.Preferences` | `java.naming`, `java.prefs` |
| Logging | `java.util.logging.Logger`, `java.util.logging.LogManager` | `java.logging` |
| RMI and distributed transactions | `java.rmi.Naming`, `java.rmi.registry.LocateRegistry`, `java.rmi.server.UnicastRemoteObject`, `javax.transaction.xa.XAResource` | `java.rmi`, `java.transaction.xa` |
| Scripting JShell and dynamic linking | `javax.script.ScriptEngineManager`, `jdk.jshell.JShell`, `jdk.jshell.tool.JavaShellToolBuilder`, `jdk.dynalink.DynamicLinkerFactory` | `java.scripting`, `jdk.dynalink`, `jdk.jshell` |
| Compiler language model and documentation | `javax.tools.ToolProvider`, `javax.tools.JavaCompiler`, `javax.lang.model.util.ElementFilter`, `javax.lang.model.util.Elements`, `javax.lang.model.util.Types`, `com.sun.source.util.Trees`, `com.sun.source.util.DocTrees`, `jdk.javadoc.doclet.Doclet` | `java.compiler`, `jdk.compiler`, `jdk.javadoc` |
| Jar jlink jpackage and command tools | `java.util.spi.ToolProvider`, `jdk.security.jarsigner.JarSigner` | `java.base`, `jdk.jartool` |
| JFR instrumentation attach and debugging | `jdk.jfr.FlightRecorder`, `jdk.jfr.Recording`, `jdk.jfr.consumer.RecordingFile`, `jdk.jfr.consumer.RecordingStream`, `java.lang.instrument.Instrumentation`, `com.sun.tools.attach.VirtualMachine`, `com.sun.jdi.Bootstrap` | `java.instrument`, `jdk.attach`, `jdk.jdi`, `jdk.jfr` |
| Security cryptography TLS and authentication | `java.security.Security`, `java.security.Provider`, `java.security.KeyStore`, `java.security.cert.CertificateFactory`, `java.security.cert.CertPathValidator`, `javax.crypto.Cipher`, `javax.net.ssl.SSLContext`, `javax.net.ssl.KeyManagerFactory`, `javax.net.ssl.TrustManagerFactory`, `javax.security.auth.Subject`, `javax.security.auth.login.LoginContext`, `javax.security.sasl.Sasl`, `org.ietf.jgss.GSSManager`, `javax.smartcardio.TerminalFactory` | `java.base`, `java.security.jgss`, `java.security.sasl`, `java.smartcardio` |
| Locale formatting and random generators | `java.util.Locale`, `java.text.Normalizer`, `java.text.NumberFormat`, `java.text.MessageFormat`, `java.text.BreakIterator`, `java.util.random.RandomGeneratorFactory` | `java.base` |
| Foreign memory and incubating vectors | `java.lang.foreign.Arena`, `java.lang.foreign.MemorySegment`, `java.lang.foreign.Linker`, `jdk.incubator.vector.VectorSpecies`, `jdk.incubator.vector.VectorOperators` | `java.base`, `jdk.incubator.vector` |

### Every observed module

The disposition below is source ownership only. Exact descriptor paths, Git blobs, SHA-256,
unqualified/qualified exports, requirements, services and owner references are in the linked
module index. No row advances the ordered receiving plan.

| Module | Source platform | Public packages | Existing owner disposition |
| --- | --- | ---: | --- |
| `java.base` | share | 54 | Retain existing exported API owners |
| `java.compiler` | share | 6 | Retain existing exported API owners |
| `java.datatransfer` | share | 1 | Retain existing exported API owners |
| `java.desktop` | share | 50 | Retain existing exported API owners |
| `java.instrument` | share | 1 | Retain existing exported API owners |
| `java.logging` | share | 1 | Retain existing exported API owners |
| `java.management.rmi` | share | 1 | Retain existing exported API owners |
| `java.management` | share | 9 | Retain existing exported API owners |
| `java.naming` | share | 6 | Retain existing exported API owners |
| `java.net.http` | share | 1 | Retain existing exported API owners |
| `java.prefs` | share | 1 | Retain existing exported API owners |
| `java.rmi` | share | 5 | Retain existing exported API owners |
| `java.scripting` | share | 1 | Retain existing exported API owners |
| `java.se` | share | 0 | Aggregate existing required modules |
| `java.security.jgss` | share | 2 | Retain existing exported API owners |
| `java.security.sasl` | share | 1 | Retain existing exported API owners |
| `java.smartcardio` | share | 1 | Retain existing exported API owners |
| `java.sql.rowset` | share | 3 | Retain existing exported API owners |
| `java.sql` | share | 2 | Retain existing exported API owners |
| `java.transaction.xa` | share | 1 | Retain existing exported API owners |
| `java.xml.crypto` | share | 6 | Retain existing exported API owners |
| `java.xml` | share | 25 | Retain existing exported API owners |
| `jdk.accessibility` | share | 1 | Retain existing exported API owners |
| `jdk.attach` | share | 2 | Retain existing exported API owners |
| `jdk.charsets` | share | 0 | Retain existing service/provider owners |
| `jdk.compiler` | share | 4 | Retain existing exported API owners |
| `jdk.crypto.cryptoki` | share | 0 | Use existing JCA/JCE provider entry points |
| `jdk.crypto.ec` | share | 0 | Use existing JCA/JCE provider entry points |
| `jdk.crypto.mscapi` | windows | 0 | Use existing JCA/JCE provider entry points |
| `jdk.dynalink` | share | 5 | Retain existing exported API owners |
| `jdk.editpad` | share | 0 | Retain existing service/provider owners |
| `jdk.hotspot.agent` | share | 0 | Internal or qualified exports; no general public utility |
| `jdk.httpserver` | share | 2 | Retain existing exported API owners |
| `jdk.incubator.vector` | share | 1 | Existing incubating APIs; no stability claim |
| `jdk.internal.ed` | share | 0 | Internal or qualified exports; no general public utility |
| `jdk.internal.jvmstat` | share | 0 | Internal or qualified exports; no general public utility |
| `jdk.internal.le` | share | 0 | Internal or qualified exports; no general public utility |
| `jdk.internal.opt` | share | 0 | Internal or qualified exports; no general public utility |
| `jdk.internal.vm.ci` | share | 0 | Internal or qualified exports; no general public utility |
| `jdk.internal.vm.compiler.management` | share | 0 | Internal or qualified exports; no general public utility |
| `jdk.internal.vm.compiler` | share | 0 | Internal or qualified exports; no general public utility |
| `jdk.jartool` | share | 1 | Retain existing exported API owners |
| `jdk.javadoc` | share | 1 | Retain existing exported API owners |
| `jdk.jcmd` | share | 0 | Command/runtime/internal owner; no public package export |
| `jdk.jconsole` | share | 1 | Retain existing exported API owners |
| `jdk.jdeps` | share | 0 | Use standard ToolProvider service |
| `jdk.jdi` | share | 5 | Retain existing exported API owners |
| `jdk.jdwp.agent` | share | 0 | Command/runtime/internal owner; no public package export |
| `jdk.jfr` | share | 2 | Retain existing exported API owners |
| `jdk.jlink` | share | 0 | Use standard ToolProvider service |
| `jdk.jpackage` | share | 0 | Use standard ToolProvider service |
| `jdk.jshell` | share | 4 | Retain existing exported API owners |
| `jdk.jsobject` | share | 1 | Retain existing exported API owners |
| `jdk.jstatd` | share | 0 | Retain existing service/provider owners |
| `jdk.localedata` | share | 0 | Retain existing service/provider owners |
| `jdk.management.agent` | share | 0 | Command/runtime/internal owner; no public package export |
| `jdk.management.jfr` | share | 1 | Retain existing exported API owners |
| `jdk.management` | share | 1 | Retain existing exported API owners |
| `jdk.naming.dns` | share | 0 | Retain existing service/provider owners |
| `jdk.naming.rmi` | share | 0 | Retain existing service/provider owners |
| `jdk.net` | share | 2 | Retain existing exported API owners |
| `jdk.nio.mapmode` | share | 1 | Retain existing exported API owners |
| `jdk.random` | share | 0 | Retain existing service/provider owners |
| `jdk.sctp` | share | 1 | Retain existing exported API owners |
| `jdk.security.auth` | share | 4 | Retain existing exported API owners |
| `jdk.security.jgss` | share | 1 | Retain existing exported API owners |
| `jdk.unsupported.desktop` | share | 1 | Existing unsupported APIs; no promotion |
| `jdk.unsupported` | share | 3 | Existing unsupported APIs; no promotion |
| `jdk.xml.dom` | share | 4 | Retain existing exported API owners |
| `jdk.zipfs` | share | 0 | Retain existing service/provider owners |

## Collection and I/O boundaries

The existing collection category catalogue is extended at `m3/collections/algorithm-catalog/integrated-utility-views-20261018.tsv`.
Live filters preserve backing identity, encounter order, duplicates and observable callback
behavior. Detached snapshots own captured visible `Object[]` values with offset/reverse windows.
Primitive masks and selected physical indexes prepare filtered captures; snapshots do not retain
a redundant index permutation. Live sorted projections remain a separate contract. A backing `ArrayList.set` can alter predicate/comparator outcomes
without structural modification, so `modCount` alone cannot validate a retained selection cache.

The existing `M3FilteredViews`, `M3FilteredLists`, `M3CollectionViews` and primitive Java/JNI
mask owners are source donors/proving grounds. Their existence is not direct `java.util`
integration. New runtime source is received through the qualified canonical recipe into the
existing JDK owners, with target-compatible source rights and exact target contracts.

I/O helpers must state byte versus character semantics, partial read/EOF behavior, charset
selection and resource ownership. Borrowed streams are not implicitly closed or flushed.
Reflection helpers must retain access checks; stream helpers must retain laziness, single-use
and close-handler behavior. The implementation receipts, not this table, establish
which candidate operations have passed the actual compiler/runtime gates.

### Detached snapshot sorter selection

Detached snapshot sorting now reuses `Arrays.sort` directly on its private captured visible
`Object[]`. The final snapshot keeps that array plus offset, length and reverse-window state.
Primitive masks and selected physical indexes remain transient capture inputs. The separate
indirect merge sorter and retained `int[]` ordering permutation are rejected as duplicated work
and storage; their exact source and pilot/full-call measurements remain in the receiving ledger.

The selection reuses the existing JDK stable sorter and removes redundant retained structure.
It does not establish a general CPU or allocation speedup against a straightforward captured
`ArrayList` / `removeIf` / `List.sort` baseline. Final and rejected-candidate measurements remain
separate, and the native selector stays diagnostic opt-in with Java as the default.

### Collection corrections retained in the category catalogue

Reverse list behavior reuses `ReverseOrderListView.of` so double reversal, index translation
and `RandomAccess` behavior stay with the existing JDK owner. Bounded navigation uses the
existing directional navigation owner: a probe outside an already bounded parent range must
not create an illegal child range as an implementation shortcut.

Identity-backed map views retain the backing key/value identity relation for entry equality and
key hashing. Filtered reads delegate to the backing `get` owner when visible so access-order
`LinkedHashMap` retains observable order changes. A filtered set addition that collides with a
hidden equivalent backing element must reject before mutation when returning false would leave
the view without the admitted value. These are source-backed contract decisions; candidate
review/runtime receipts establish the actual fixes independently and do not qualify a product image.

## Internal receiving candidates

The public collection entry point is `java.util.Collections`. Its candidate implementation owners
are package-private `java.util.M3CollectionViews` and `java.util.M3CollectionMasks` in `java.base`.
The mask JNI declaration is `static native int[] selected0(long[], int, int)` and the target symbol
is `Java_java_util_M3CollectionMasks_selected0` in
`src/java.base/share/native/libjava/M3CollectionMasks.c`. These internal source names do not
introduce a public M3 utility layer. The same naming-map fragment records these candidates.

Mask extraction clips a half-open physical range, counts selected positions, then emits an exact,
detached ascending `int[]`. Java remains the default; the startup property
`jdk.m3.collectionMasks.native=true` is a diagnostic opt-in with missing-symbol fallback.
Any patched-module and separately compiled native-library receipt must remain distinct from
full product `libjava`, JDK image, jtreg, platform and performance acceptance.

The current `Reflection` candidate covers type and annotation/parameter metadata. Caller-sensitive
member discovery and invocation stay with their existing owners. `IOUtils` covers bounded and
unbounded transfers, exact reads/skips, equality and explicit charset conversion; `Streams`
extends its existing owner with source adaptation and serial zip. The source verifier records
member declaration tokens only and does not infer overload signatures or runtime contracts.

## Token sequence APIs in the owning regex package

The additive candidates are `java.util.regex.TokenPattern<T>` and `TokenMatcher<T>`
in `java.base`. Existing `Pattern` and `Matcher` continue to own character regex.
The token owners reuse the JDK graph and match state with a predicate atom consuming one
whole input-list element. Tokens, including null, are never serialized, converted with
`toString`, or lexed to infer boundaries. A matcher takes a shallow defensive snapshot;
all regions, capture spans and search positions are absolute half-open token indices.

The finite builder contract covers literal/any/empty/predicate atoms, sequences, ordered
alternatives, greedy/reluctant/possessive repetition, and numbered or named captures.
Nested public owners are `TokenPattern.Quantifier` and `TokenMatcher.Span`. Text grammar,
lexing, backreference/lookaround builders and the full T-Rex language are outside this
packet. Backtracking and stack resource costs remain; this catalogue makes no general
linear-time, JNI acceleration or performance claim.

The same family fragment pins both product hashes and exact declaration tokens. The
contract catalogue and compiler-derived 68-method source seal live under
`m3/runtime-integration/token-regex/`. Those observations do not promote the baseline
source inventory into executed runtime or product-image acceptance. Fresh source-bound
contract receipts belong to the canonical recipe's token qualification packet.

The current follow-up was reconstructed after transient staging was lost. Its two
production source hashes match the prior recorded hashes. The test source and execution
receipts were recreated and rerun; unavailable historical receipt bytes are not claimed
as recovered. Existing character `Pattern` and `Matcher` sources remain unchanged.

The donor decision rows distinguish JDK graph reuse from reference-only T-Rex, NLTK and
Synexia algorithms. Named character groups do not make character regex token-aware.
Delimiter serialization is rejected because it can map different token sequences to the
same character string. No Apache donor implementation text is imported into JDK files.

## Source verification

Run `python3 m3/compatibility/check_integrated_utility_coverage.py --repository .`. The verifier checks the named baseline Git blobs,
SHA-256 values, public declarations, owning module exports, the complete source-module set (including conditional providers), representative owner references, coverage keys and preservation of
all previous naming-map and receiving-plan values. It never infers behavioral qualification
from an inventory count and does not advance a receiving phase. New source candidates need
their own recipe, compilation, contract and applicable native/JDK-image receipts.

Reusable refusal tests: `python3 m3/compatibility/test_integrated_utility_coverage.py --repository .`. They exercise real pinned Git source reads and refuse phase changes, source seal drift, missing families/modules, fabricated exports, misclassified conditional/internal/unsupported owners, omitted public provider routes and fabricated qualification.
