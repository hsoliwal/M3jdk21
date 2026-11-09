<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Unified MFile streams: enhanced design

Status: BOUNDED_CORE_BRIDGE / ENHANCED_CONTRACTS_RUNTIME_PENDING
Product owner: M3JDK21
Reusable recipe owner: hsoliwal/com.synexia
Baseline: Java 21

## 0. Current bounded core and the richer proposal

The `m3fs-vfs-20261020` integration adds `com.m3.fs.MFiles` and the final class
`com.m3.fs.MFile` to the existing `jdk.m3fs` module. Apache Commons VFS 2.10.0 is
relocated to `com.m3.fs.vfs`; its `VFS` entry point is named `M3VFS`. The current
source surface is deliberately narrower than the future contracts below.

| Current source surface | Current boundary |
| --- | --- |
| `MFiles.from(Path)` / `MFiles.from(String)` | One local path or explicit provider address; construction performs no provider I/O |
| `Plan.filterName(...)`, `extensions(...)`, `filter(...)` | Cheap basename selection before full projection; selection does not prune traversal |
| `Plan.descendInto(...)`, `maxDepth(...)`, `order(...)` | Directory pruning, global depth and DFS/BFS policies; no implicit sorting |
| `Plan.enterArchives(predicate, maxArchiveLayers)` | Deferred opt-in archive traversal with a separate layer budget and root per layer |
| `Plan.withProvider(scheme, factory)` | A fresh relocated VFS `FileProvider` per opened stream; no automatic Git provider |
| `Plan.stream()` and `MFiles.stream(...)` | A new single-use stream with its own manager; replay reads fresh provider state |
| `Stream.map(...)` / filter-transform convenience overload | Arbitrary result projection; it does not rename or transform an `MFile` payload |
| `MFile` metadata and `MFileContent` | Stable encountered metadata; provider input/output and default operations remain owner-scoped |
| `MFileStreams.Input` / `.Output` | Borrowed progress and synchronous shadows, per-layer position and completion-qualified hash |
| `MFile.observation()` / `MFileObservation` | Explicit fresh provider facts; complete raw and qualified structure facts can reuse existing M3FS owners |
| `MFileDelta.between(...)` | Downstream delta for one source identity; metadata, stamp, content and structure evidence remain distinct |

The current `MFile` exposes `name`, `relativeName`, `uri`, `sourceAddress`,
`isFile`, `isDirectory`, `isSymbolicLink`, `size`, explicit `observation`, and
the inherited `MFileContent` input/output operations. It has no public `Kind`,
content snapshot, physical member ordinal or AST cache.
A directory root is not emitted; a file root is. `maxDepth(0)` therefore emits
only a selected file, and `maxDepth(1)` emits a directory's immediate children.
The descent predicate also sees the un-emitted directory root with an empty
relative name. Traversal defaults to depth first in provider encounter order;
`breadthFirst()` or `order(...)` selects the explicit frontier-based alternative.
It does not split for parallel traversal.

An archive `Path` is an ordinary file by default. Select its container explicitly,
for example `MFiles.from("zip:file:///tmp/data.zip!/")`, or opt into
`enterArchives(predicate, maxArchiveLayers)`. A nested archive can also be a root
such as `zip:zip:file:///tmp/outer.zip!/inner.zip!/`. Deferred traversal keeps a
distinct provider root and relative-name prefix for each archive layer. It does
not provide arbitrary content sniffing or physical-record enumeration.
Decoded `relativeName()` is a display/traversal path: literal `!` names can collide
with the rendered layer notation. Full `sourceAddress()` is the observation/delta
identity, not the display path.

The current archive view is the provider's logical hierarchy. Duplicate physical
archive records with the same normalized name can collapse; reverse/concurrent
access to distinct physical occurrences is not promised. The stronger duplicate
identity contracts below, particularly A15/A21, remain future requirements.

Metadata survives stream closure. Content does not: close readers promptly and
close the owning stream even after exhaustion or a short circuit. Normal
exhaustion leaves the owner open so collected entries may be read within its
scope. Replay and captured metadata do not freeze source bytes. Archive mounting
may replicate or index an archive, and provider metadata queries can perform I/O.
The facade does not eagerly buffer each payload, but this is not a zero-copy,
constant-memory, array-only or measured memory-gain claim.

A Git working tree can be scanned through its local file path. Commit-tree
selection and Git LFS hydration remain future provider work; an unhydrated LFS
pointer is read as the ordinary file bytes present on disk. Optional FTP/FTPS,
HTTP-client and SFTP transports are excluded from the bounded source profile.
The generic URL fallback is present and remains subject to installed URL handlers;
it does not establish qualification of those excluded transports.

See [the current API guide](../../fs/vfs/README.md), the
[checked image receipt](../../fs/verification/vfs-20261020/qualification.json), and
[the cross-gate report](../../fs/verification/vfs-20261020/RESULTS.json).
Receipts describe actual checks and outstanding gates. This design update records
no test pass. All 56 richer acceptance rows remain `NOT_RUN_RUNTIME`; a related
bounded-core test never makes a stronger full-specification row pass implicitly.

Sections 2-14 retain the richer proposed contract. Some signatures, including
the single-root `enterArchives` predicate, now have the bounded implementation
identified above. Other signatures and stronger guarantees remain proposals.
The current API guide and checked receipts define the delivered scope.

## 1. Intent and scope

Hitesh requested a complete directory as a stream, containing files and folders, accepting
filter and transformer lambdas, with MFile as the entry. Actual ZIPs and other supported
archives are input sources too. The approved API sketch is extended here while keeping
the common case short.

An application should be able to select Java sources from a directory or archive,
inspect their content, reuse the existing source/AST snapshot, transform selected views,
compare versions and write an explicit result. Traversal and repeated analyses must share
qualified existing work where source identity and semantic context permit it.

This document separates the bounded source surface above from the richer behavior
still proposed below. Documentation changes do not establish runtime qualification.

## 2. Proposed richer public shape

The earlier signatures remain design sketches. The options overload and multi-source
plan below are not part of the current bounded API:

~~~java
Stream<MFile> MFiles.stream(Path source) throws IOException;
Stream<MFile> MFiles.stream(Path source, MFileReadOptions options)
    throws IOException;
<R> Stream<R> MFiles.stream(
    Path source,
    Predicate<? super MFile> filter,
    Function<? super MFile, ? extends R> transformer) throws IOException;
~~~

Proposed multi-source extension of the existing reusable plan:

~~~java
MFiles.Plan MFiles.from(Path... sources);

var sources = MFiles.from(Path.of("src"), Path.of("dependencies.zip"))
    .descendInto(folder -> !folder.name().equals("target"))
    .enterArchives(entry -> entry.name().endsWith(".jar"), 2)
    .filter(entry -> entry.isFile() && entry.name().endsWith(".java"));

try (Stream<MFile> entries = sources.stream()) {
    entries.map(MFile::relativeName).forEach(System.out::println);
}
~~~

Plan construction opens no resources. Plans are immutable; each configuration method returns
a new plan. A plan retains its declared sources and stages, not an open traversal.
stream() opens a new single-use execution. Fresh execution ordinarily observes current
source state; repeating a plan does not freeze external files.

The entry-facing plan retains MFile values. Arbitrary result projection belongs to the
ordinary Stream.map or the generic convenience overload. Do not introduce a second
general-purpose query language, stream implementation, global scheduler or parser engine.

### Defaults

- Directory traversal is recursive; root itself is excluded; both files and directories
  are eligible for emission, including empty directories.
- Directory depth and archive nesting depth are separate concepts.
- Symbolic links are emitted as links and are not followed.
- Proposed reader selection may recognize an archive input as a container. The current
  bounded API requires an explicit layered URI; a `.zip` Path is not autodetected.
- Archives encountered inside a directory or another archive remain ordinary entries
  unless enterArchives explicitly selects them.
- Inclusion is unrestricted until a filter is added; descent is unrestricted except for
  links and configured depth limits.
- Encounter order is source argument order, then provider encounter order. There is no
  implicit lexical sort and no cross-run ordering promise for a live directory.
- Readers operate sequentially unless an execution explicitly selects admitted parallel
  work. Merely wrapping a decoder in a parallel Stream does not make it safely splittable.
- Failures are reported. Skipping failures requires an explicit policy with a report.
- Destination-name collisions fail by default. Writes require an explicit terminal.
- No network fetch, extraction, watch registration or write happens while building a plan.

## 3. Entry identity and storage

The following is the earlier proposed richer entry shape, not the declaration of the
current final `com.m3.fs.MFile` class. In particular, `kind()` remains unimplemented:

~~~java
public interface MFile {
    String name();
    String relativeName();
    Kind kind();
    OptionalLong size();
    boolean isFile();
    boolean isDirectory();
    boolean isSymbolicLink();
    InputStream openInputStream() throws IOException;
    enum Kind { FILE, DIRECTORY, SYMBOLIC_LINK, OTHER }
}
~~~

Entry descriptors are stable values. Never emit one mutable cursor and change its fields
for the next file. A collected descriptor retains its metadata and provenance; access
to content still follows the source-session or retained-snapshot lifetime.

Logical names, source identity and destination names are distinct:
- raw member name preserves archive evidence;
- relativeName is the validated logical name within its immediate container;
- display address includes the container chain for people;
- identity uses an owner/execution or immutable content generation plus the chain of
  member locators, including occurrence ordinals where names may repeat;
- output name is selected by an explicit output mapping.

A string such as app.zip!/lib/a.jar!/p/C.class is a display form, not a host Path or the
sole identity. Literal exclamation marks, duplicate names, case differences and Unicode
normalization must not collapse distinct entries.

Unknown size remains OptionalLong.empty(), never zero. DIRECTORY does not mean an empty
regular-file stream. Symlink targets are metadata, not automatically traversed content.
Missing archive directory records may be synthesized in an explicit logical-hierarchy mode;
synthetic parents expose unknown unavailable metadata rather than invented attributes.

The current VFS facade belongs to `com.m3.fs` and reads through the relocated provider
handles. The separate `com.m3.file.M3File` remains the original thin NIO/provider boundary;
do not introduce a second stream owner there. Future compact metadata integration should
reuse M3DirectoryTable and existing M3FS directory pages. Current donor caches and facade
collections retain objects; importing them does not implement an array-only redesign.

The compiletime M3File source-snapshot owner is a separate existing owner. A directory
descriptor must not claim to already contain its immutable text or AST.

## 4. Stream, session and snapshot lifetimes

The richer design distinguishes three modes. Only fresh execution is exposed by the
current Plan; its immutable metadata projections are not an explicit snapshot facility:

| Mode | Guarantee |
| --- | --- |
| Fresh plan execution | New traversal of currently visible sources |
| Metadata snapshot | Stable observed descriptors/index; no promise that file bytes are frozen |
| Content snapshot | Captured bytes or provider-proven immutable versions retained by the existing owner |

A pinned directory must preserve content or immutable provider versions. Matching size,
mtime and pathname cannot establish immutable content. Capturing files independently is
not a globally atomic directory snapshot; report per-entry capture or require an admitted
provider snapshot for a global consistency guarantee.

A future MFiles.Session is a closeable execution owner for callers that need several
content accesses or analyses before releasing archive readers and leases. Convenience
stream() owns its session and closes it through Stream.close(). In session.stream(),
the session is caller-owned; stream close releases only its traversal resources.
Closing a session invalidates unretained content leases but not copied descriptor facts.

Try-with-resources is required even after findFirst(), limit(), cancellation or exceptions.
Exhaustion may release traversal handles, but must not silently invalidate a retained
session that the caller still owns. Each openInputStream result must be individually
closed; session close also closes its outstanding children. Close is idempotent and
suppressed cleanup failures preserve the primary failure.

Initial open failures are IOException. Lazy traversal failures are UncheckedIOException
with source/entry context. User predicate failures propagate as their original runtime
exceptions. Checked-I/O helpers preserve the original IOException cause.

## 5. Directory and archive traversal

filter decides emission. descendInto decides whether to enter a directory. Archive
expansion is also a traversal decision and is evaluated independently of downstream
inclusion filters, allowing a pipeline to select only .java entries while entering .jar
containers.

The configured traversal stages inspect original container identity and names. Entry
renaming later in the pipeline cannot change which source subtree is traversed.

For nested traversal, keep the containing archive entry and yield children in a documented
container-before-children order. Preserve each level's member identity. A directory-depth
limit does not consume the archive-depth budget. Exceeding a requested safety ceiling
produces an explicit failure or partial disposition, never a silent complete result.

Directory subtree pruning can avoid opening the subtree. Archive prefix exclusion is
logical pruning: a sequential compressed reader may still need to decode/skip bytes
to reach later headers. Do not report avoided physical reads merely from rejected names.

ZIP readers should use index/local-header or equivalent ordinal-safe locators where
available. A provider that cannot reopen duplicate names unambiguously must refuse that
operation rather than silently reopen a different occurrence. Test retained duplicate
entries in reverse and concurrent access order, not only immediate forward reads.

Seekable ZIP, sequential ZIP, TAR and compressed TAR have different capabilities.
Compression and archive parsing compose: tar.gz is a gzip decoder followed by a TAR reader.
7z and other readers that require seekable sources must declare that requirement.
Unsupported compression, encryption, format extensions or entry features are explicit
reader outcomes. Format recognition is not proof that every member is readable.

Readers may inspect bounded magic/header data; a filename extension is a hint, not a
sufficient format proof. Preserve the peeked bytes when handing input to a decoder.

## 6. Efficient sequential content processing

A forward-only reader cannot simultaneously promise arbitrary retained payload access,
no storage and no rescanning. The full ordinary Stream contract must not conceal that
tradeoff.

Provide a scoped file-processing terminal in addition to entry streams:

~~~java
<R> MFiles.ScanReport MFiles.Plan.scanFiles(
    MFiles.EntryReader<R> reader,
    MFiles.ResultConsumer<? super R> consumer) throws IOException;
~~~

EntryReader receives (MFile entry, InputStream content) for regular files.
The framework owns and closes content after the callback, before advancing the decoder.
The result must not retain the borrowed stream or cursor. ResultConsumer is invoked before
the next entry unless an explicit, bounded, independently owned batch policy is selected.
Metadata-only directory processing remains available through stream().

Example:

~~~java
sources.scanFiles(
    (entry, input) -> parser.captureAndParse(entry, input),
    parsed -> index.accept(parsed));
~~~

parser and index are application-supplied existing facilities; these lines do not create
a new parser. Durable results refer to the existing immutable source owner.

Where content predicates, later transformations or repeated reads require replay,
use the existing retained-content/snapshot owner with an explicit retention policy.
A predicate that consumes the first bytes must not truncate the stream passed to a later
consumer. Do not rescan an archive once per file as an undocumented optimization.
An implementation may expose such an explicit fallback, with its work cost recorded.

## 7. Selection and transforms

Metadata selection includes type, qualified logical name/glob, known size, timestamps,
source, archive depth and explicit source facts. Unknown fields have a documented policy.
Regex flags, Unicode behavior and matching scope are part of a predicate's identity.

Content selection has explicit binary and text forms:
- filterContent consumes a callback-scoped InputStream and preserves replay requirements;
- filterText requires a charset and decoding-error policy;
- named byte/text search descriptors can reuse existing M3 String/regex facts;
- hashes and approximate signatures nominate candidates, never prove exact matches.

Plain predicates remain opaque. Preserve their observable stage order, short-circuiting,
exceptions and invocation behavior within the stated plan contract. Do not reorder or
memoize user callbacks by guessing they are pure. Standard Stream terminals may elide
unnecessary intermediate computation; required side effects belong in explicit terminals.

Entry transforms are immutable:
- rename changes a logical output name and preserves original provenance;
- metadata selection/preservation never rewrites source attributes implicitly;
- transformContent returns a lazy derived content reader;
- transformText has an explicit charset and encoding/error policy;
- fan-out is explicit and assigns distinct derived entry identities.

Changing bytes invalidates authoritative size, content digest, CRC, decoded text, token
ranges, AST associations and content-dependent archive metadata until recomputed.
Rename-only views may share content facts, but a parser whose semantics depend on filename,
URI, module or package context needs a new contextual parse key.

Content transforms can allocate and decompress. No generic zero-copy claim is made.
M3 String ranges/compositions should remain shared where the existing contract permits.
An ordinary String/byte[] return is an explicit materialization boundary. Text decoding
does not silently normalize newlines, Unicode or malformed input.

## 8. Reusable plan versus durable recipe

MFiles.Plan stores a sequence of operations for in-process replay. It can contain lambdas,
but a lambda's generated class name or serialization is not a durable operation identity.

An inspectable operation descriptor records operation ID, version, parameters, declared
effects and dependencies. Built-in descriptors may be exported into the existing M3 recipe
DAG. A registered custom descriptor requires the same explicit identity and parameter
contract. Opaque lambdas appear as opaque nodes in describe(); toRecipe() refuses them
unless an admitted registered descriptor replaces them.

Do not invent a new DAG owner. Recipe stages reuse the existing Synexia composition and
qualification machinery. Side effects are barriers; a filter cannot be pushed across a
content-changing transform just because it looks cheaper.

A saved plan records provider/operation versions and source selectors. An execution receipt
additionally records the source identities actually observed, result state, failures and
work performed. Reproducibility requires these contexts as well as operation order.

## 9. Multiple sources, joins, diff and deduplication

MFiles.from(a, b, c) preserves all entries; source union is not overlay and not deduplication.
Source provenance always remains available.

Join by an explicit key. Duplicate join keys produce groups or a documented rejection;
never silently select one occurrence. Overlays require explicit first/last/winner policy.

Diff has separate metadata and content modes. Report ADDED, REMOVED, CHANGED and UNCHANGED,
plus ERROR/UNKNOWN where reading or comparison failed. A changed pathname can be nominated
as a rename only under a declared identity policy. Arbitrary merge-join order requires a
qualified sorted index or sorting cost.

Content deduplication first groups by cheap facts and digest, then verifies exact byte
equality when claiming exact deduplication. It preserves a defined first winner and records
all origins. Equal bytes at different names are not automatically redundant for an output
package: storage deduplication and removing logical entries are separate operations.
The explicit distinctContent() operation removes later logical entries and must say so.

Global sorting, joins and deduplication can retain indexes. Reuse segmented index/mask
owners, spill approved cold state through the existing M3FS/SQLite mechanism, and report
materialization barriers. Never call these operations constant-memory or purely streaming.

## 10. Source, text, AST and build-result reuse

For a selected text file or archive member, bridge into the existing immutable source
snapshot, M3 String and M3AST/compiler owners. Traversal does not construct a second text
store, parser cache or compiler.

A reuse key covers content identity/generation, source locator, decoding and error policy,
parser identity/version/options, language level, URI/filename semantics and all applicable
module/classpath/compiler context. Immutable syntax facts may be reused separately from
context-dependent symbol resolution. A global compile is not implied by parsing one file.

Keep byte offsets, decoded UTF-16 offsets, code points and token/AST coordinates distinct.
Only publish a coordinate mapping after validation against the source generation.
A transformed view creates a new generation and source mapping; old facts remain attached
to the old immutable owner. Failed parses and provisional captures remain unpublished.
Share immutable facts; isolate mutable parser workspaces and compiler-session objects.

Repeated consumers should ask the existing owner for exact completed results before work.
Eviction may discard rebuildable derived facts, not canonical payload or promised retained
content. A valid weak or negative result is keyed and qualified just like a positive result.

## 11. Output terminals and previews

Add explicit writeDirectory and writeZip terminals on Plan. Other archive outputs are
reader/writer capabilities, not assumed from input support. Output construction is separate
from execution. The writer owns traversal and opened payload streams.

A preview resolves final destination names, collisions and capability requirements without
performing user filesystem writes. It may need metadata scans or content reads; it is not
necessarily free. Opaque or effectful callbacks may not be run as a harmless preview.
A preflight result is tied to source/output context and revalidated at commit.

Directory layout and empty directories are preserved where admitted. Expanded nested
members require an explicit flatten/preserve-container destination mapping; display
addresses are not extraction paths. Reject invalid escapes and collisions after the
destination provider's actual case/normalization rules. Link handling is explicit.
Do not traverse existing destination symlinks outside the selected output tree.

Default collision policy is FAIL. Overwrite and merge are opt-in policies with an operation
receipt. For a ZIP replacement, stage a complete sibling candidate and replace only using
the supported commit strategy. Atomic rename may be unavailable; do not silently weaken a
requested atomic guarantee. Ordinary multi-file directory output is per-file/partial unless
an admitted generation-root transaction provides stronger semantics.

A failure or cancellation returns/throws with committed, pending and failed entries
identified. Temporary outputs and leases are cleaned up. Never claim whole-tree rollback
where already committed writes remain. Signed archives and executable-package metadata
require explicit preservation/re-signing rules after mutation; stale verification marks
must not be asserted as valid.

Output ordering, timestamps, permissions, encoding and duplicate policy are explicit in
reproducible-output mode. Reproducibility is verified against fixed inputs; it is not
established by sorting filenames alone.

## 12. Incremental changes, progress and resources

Watch events are invalidation hints. Overflow, missing notifications, source replacement,
same-size changes and timestamp restoration require reconciliation. Events do not prove a
writer has finished. Debounce is a scheduling choice, not proof of stable content.

A watch/delta extension reuses the current file-generation and dependency owners. It emits
source identity and validated changes; consumers re-evaluate only facts whose keys changed.
An archive change invalidates affected member locators/index assumptions unless an admitted
format/provider supplies stronger version evidence. Watching a plan is an explicit operation;
this document creates no scheduled automation.

Reuse existing progress monitor/status/cancellation contracts. Counters distinguish listed
entries, skipped subtrees, compressed bytes read, decoded bytes, retained bytes, reused facts,
parse operations, outputs committed and unknown remaining work. Unknown totals are
indeterminate, not fabricated percentages.

Ordinary calls require no tuning of collection capacity. Residency and concurrency adapt
within existing memory-pressure policy. Explicit resource/security ceilings are separate
from capacity hints. Exceeding a ceiling yields a visible incomplete/failure disposition;
it never silently drops values. Pin active leases; reclaim only permitted optional residency.
Backpressure bounds decoded queues and pending writes. No global unbounded executor or
second cache framework is introduced.

Native/JNI acceleration is admitted only for measured compatible bulk work, through existing
owners. Pinning/GC lifetimes, cancellation and fallback remain explicit. No native symbol
is needed merely to expose a Java Stream. CPU, heap and native measurements are separate.

## 13. Reader and operation catalogue

CAPABILITIES.tsv is a searchable index of the specified operations and their primary
implementation tasks. ACCEPTANCE.tsv binds hostile and ordinary input classes to tasks.
All runtime gates start NOT_RUN.

Donor/category research is contract-driven: DFS/stack traversal, prefix pruning, indirect
ordering, merge join, exact hash verification, streaming pattern search, interval/range
mapping and bounded producer-consumer work. Existing M3 owners come first. Challenge sites
may supply problem categories; no editorial or submission body is copied into this packet.

OpenJDK NIO/ZIP and Apache Commons Compress documentation remain behavioral references.
The separate runtime integration now imports pinned Apache Commons VFS 2.10.0 source;
`m3/fs/vfs/SOURCE.json`, `SOURCE_PROFILE.tsv`, the dependency lock and retained Apache
notices describe that intake. This design packet itself contains documentation only.
Neither intake nor this proposal establishes support for every archive variant, provider
or platform. Git commit/LFS providers need a separate admitted implementation.

## 14. Delivery and verification

Implement contract groups as additive, source-sealed Synexia Maven/OpenRewrite recipes;
generate M3JDK21 product changes, then qualify the installed module/image and concrete
behavior. Preserve Inline -> Atomize -> Patternize -> Comment and the existing receiving
order; documentation is not a runtime-phase promotion.

Required checks include traversal/resource closure, duplicate-member retention, sequential
scan lifetime, transform invalidation, deterministic output, collision failures, exact diff,
snapshot/context reuse, partial failures and pressure. Compare cold preparation and warm
reuse separately. Finite correctness tests do not prove universal speedup or full JDK parity.

See IMPLEMENTATION_PLAN.md for the current-core bridge, future task interfaces and
release gates. Publication of this specification does not complete any richer acceptance
row or advance the receiving sequence.
