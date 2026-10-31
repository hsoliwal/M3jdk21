# Five-library collection candidate catalogue, 2026-10-18

This is a searchable source-only inventory for JDK21, Guava33.4.8-jre,
Commons Collections4.5.0, Eclipse Collections11.1.0 and Spring Core6.2.0.
It is a before-code catalogue, not a complete implementation or behavior result.
No collection source body, private ArrayMap body, artifact, runtime probe,
performance result or JNI collection-store claim is included.

The recorded passive inventory resolved 8,593 class entries in the exact selected
namespaces. It used Class.forName with initialize=false and read modifiers,
interfaces, superclass and assignability. It invoked no provider collection
constructor, factory, callback or collection operation. The separate category
pass follows recorded type relations. Its external ancestors were not all
enumerated; that is distinct from an unresolved class-load error. This packet
does not repeat either runtime pass.

`candidate-types.tsv` contains all 740 rows whose recorded declared modifier is
public, whose recorded role is CONCRETE_ROOT_IMPLEMENTOR and whose recorded
collectionRoot flag is true. This is a filter of modifiers and root relations.
It proves neither accessibility of enclosing classes nor constructibility,
supported operations, array storage, full source/returned-family closure, M3
ownership, contract preservation or implementations completed. Binary names
containing `$` retain their exact recorded spelling. NOT_EVALUATED is the frozen
passive inventory's ownership/contract evaluation state. Existing independently
qualified owners elsewhere in the repository have separate receipts; this
catalogue neither replaces those receipts nor transfers their proofs to a family.

The V2 taxonomy successor corrects the original multimap root-set omission:
category08 now includes Commons `org.apache.commons.collections4.MultiMap` and
Spring `org.springframework.util.MultiValueMap`, in addition to the original
Guava Multimap, Commons MultiValuedMap and Eclipse Multimap roots. CPU closure
was recomputed for every one of the 8,593 frozen type rows; the 740 candidate
identities and every noncategory field stay unchanged. Only additive08 tags
change. Its candidate count is42 instead of39; all unrelated category counts
stay unchanged. The original passive tables/source/hash and original review
remain private historical evidence, rather than being silently rewritten.

The five libraries use six artifact codes because Eclipse API and implementation
are separate artifacts. `artifacts.tsv` maps J/G/A/EA/EI/S to exact versions,
selected namespace prefixes, artifact byte counts and SHA256. J is the installed
Oracle21.0.9+7-LTS-338 runtime oracle, not a matched upstream source snapshot.
Spring covers core util only, not every Spring module. All raw artifact caches
remain read-only and private; no archive or full repository was copied here.

`categories.tsv` maps the candidate numeric codes to 19 problem families and
counts both all resolved namespace rows and the 740-candidate subset. Categories
overlap, so their counts must not be added as distinct implementations. Names
come from transitive recorded roots, not from guessing an algorithm from a class
name. support-or-other is the residual category; primitive-iteration is not a
claim that every generated primitive family is fully inventoried or implemented.

For example, search `rg 'FastList|UnifiedMap|UnifiedSet' *.tsv`, or look up a binary
name in candidate-types.tsv, its category codes in categories.tsv and its artifact
code in artifacts.tsv. The full private type table is 1,725,895 bytes, and the
problem-family table is 2,723,343 bytes. Their exact hashes remain in
input-provenance.tsv. This compact public filter omits noncandidate namespace
rows; it does not turn omission into proof that a type or capability is absent.

`eclipse-evaluation.tsv` retains the 18 source-only evaluation categories pinned
to eclipse-collections/eclipse-collections commit
`fb6abf7fe655ff04acf616158bce49725f2df5ad` (11.1.0). FastList's T[] items/size and
UnifiedMap's interleaved Object[] table/Object[] collision chains are strong
array source candidates, with runtime fit still OPEN. UnifiedSet retains linked
ChainedBucket objects in its Object[] table and is NO_FIT unmodified. Other rows
remain prospective source selections or whole-mechanism rejections. Provider
parents/delegates and reflective/service-loader/Proxy allocation do not qualify
as owned results. Temporary API entries/descriptors do not admit retained entry
nodes. Linear collision worst cases, capacity-dependent copying and allocation
sequencing remain tradeoffs; no speedup or all-allocation/resource parity was
measured.

D13 states the actual exact11 bag mechanism: AbstractHashBag retains
`protected MutableObjectIntMap<T> items`, initialized in HashBag through
ObjectIntMaps. A complete owned primitive count-map and rich returned-family
closure remain OPEN. The local Counter objects used for traversal are permitted
scalars, not retained per-key nodes. The reviewed HashBagCounter allegation was
rejected after primary-source inspection: that identifier is absent from these
exact11 donor bodies. No counting law or mechanism from another version/library
is transplanted into this catalogue. Original review output and root source
disposition are preserved with exact pins.

`m3-reuse-ledger.tsv` links the 28 existing first-party atoms examined at captured
base `07ba6e383343d32c83cfdaf641387bbe72bf325d`, with exact source SHA256 and Git
blob pins. This is historical candidate provenance, not an assertion that latest
master has the same bytes or that current behavior has been qualified for Eclipse.
Recheck later source changes before any admission. Dense object arrays, key-only
object maps/sets, checked capacity/hash and selected cursor/index/mask atoms are
reusable candidates. Whole callback/default policies, identity equality and
SDK catalogue names cannot be treated as completed rich owners. Primitive and
paged kernels are internal candidates and cannot substitute for full object APIs.

`closure-obligations.tsv` preserves the complete MutableBag/list/set/map rich
returned-family obligations: bimap/multimap, all primitive and sorted families,
immutable/fixed-size, wrappers/multi-reader, partition/chunk/reverse/lazy,
parallel and reached helpers/default/factory branches. The captured immediate
mutable ancestry and nine list/map/set selectors do not close that recursive
graph. G0 metadata and captured donor test source are not runtime admission.
No smaller interface, proxy, primitive replacement or silent provider factory
allocation is offered as a substitute. The full five-library goal remains open.

`notice-pins.tsv` records the exact embedded license/notice bytes read from the
existing archives. The installed JDK legal/LICENSE states Oracle NFTC; it is a
runtime oracle here and is not redistributed. Guava and Commons artifact metadata
retain Apache2; Spring's notice additionally calls out subcomponent terms. Exact
Eclipse11 source headers/licenses retain EPL1/EDL1; a prospective adapted body
route is EDL1 with BSD three-clause terms. Both exact license source links are in
artifacts.tsv. That prospective choice does not relicense donor bodies as Apache
or establish permissions for an unexamined subcomponent. Existing M3 atoms retain
their first-party Apache2 headers. No JDK reference body or other donor body ships.

`input-provenance.tsv` and `source-method-pins.tsv` bind the frozen private
receipts/tables/helper files and literal method/expression source spans used for
CPU filtering and serialization. These historical paths are custody identifiers,
not missing public download promises. Python spans use AST get_source_segment
on exact UTF8 source without body copying; no method body is published. The
before-output FOSS decision and all original 1.7/2.7-MB tables, Eclipse raw
sources, source-only catalogue and custody receipts stay private and unchanged.
The external public packet receipt binds every exact UTF8 publication byte and
raw Git blob. Source inventory counts, proof checks and file counts are distinct
from collections completed; model review and publication are separate steps.
