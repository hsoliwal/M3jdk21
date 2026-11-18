<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3SDK: machine-oriented programming and conditional Apache/Eclipse convergence

**State: design proposal, NOT production admission.** The future
M3SDK is an Apache-2.0-only compatible **SDK superset of the JDK**,
not just a renamed OpenJDK fork. The present M3JDK21 tree retains
its actual GPLv2/Classpath and other source license obligations.

## Namespace and distribution contracts

- Every standard `java.*`, `javax.*` and supported `jdk.*` API
  preserves its Java 21 fully qualified name, descriptors, module
  exports, public/protected surface, reflection, serialization,
  JNI/JVMTI and runtime semantics. This is true even if a future
  independent implementation itself has Apache-2.0 rights.
- Existing `jdk.m3.*` extensions retain their approved owners. A
  future independent project may adopt `org.apache.<approved>.*`
  package names only after the required Apache Software Foundation
  project/naming approval. Prior to admission there is no implied
  ASF affiliation or license-by-rename authority.
- Eclipse compatibility retains `org.eclipse.*` packages, SWT
  `Bundle-SymbolicName` and `Automatic-Module-Name`, UI/OSGi
  extension points and native/Display-thread behavior. SWT,
  Eclipse Platform/Platform UI, JDT Core and Nebula inspected
  roots identify EPL-2.0; an EPL source file does not become
  Apache-2.0 when moved into `org.apache.*`.
- A separately licensed Eclipse-compatible bridge and a *strict*
  Apache-2.0-only implementation are **different products**.
  Conditional ASF inclusion of EPL binary dependencies does not
  prove that a strict Apache-only source distribution was built.

## MOP: atoms, composition and semantic skeleton

Every appropriately splittable feature is modeled as:
`public/protected API and module owner (locked) -> typed
composed method -> small semantic private/instance/static
atoms -> verified reusable recipe/pattern`. Prefer private
static atoms for pure, explicit-state work. Preserve virtual
dispatch and instance/effect ownership where required.
The 15-line leaf target is a review heuristic, not permission
to fragment native critical sections, change stack/caller
semantics, class initialization, exception ordering, memory
fences, resource lifetimes or JVM compiler intrinsics.

Interface defaults, existing abstract classes and additive
overloads are **compatibility mechanisms, not universal
mandatory scaffolding**. No existing superclass, abstract
obligation, public signature or virtual hook may change
without an explicit compatibility unlock and old-client tests.
New overloads can make `null` calls ambiguous; unrelated
interface defaults can form diamond conflicts. Test these
negative cases, old binaries without recompilation, old
source recompilation, new sources and reflection separately.

The canonical knowledge lifecycle is **Atomize ->
Patternize -> Annotate -> Document**, with source-pinned,
fixed-point, refusal-tested, versioned Maven/OpenRewrite
recipes and append-only correction memory only in Synexia.
This public JDK receives qualified outputs and its own tests,
never a second recipe writer, donor mirror or memory store.
See the existing
[MOP recipe-owned-memory design](../m3/papers/MOP_RECIPE_OWNED_IMPROVEMENT_MEMORY.md).

## SDK pack and Eclipse integration priorities

| Lane | Existing owner and mandatory proof |
| --- | --- |
| Maven / Gradle | Keep actual fork tool distributions, launchers, worker realms and plugin behavior; use the existing optional SDK tool-pack owner |
| Lucene / OpenNLP | Use the existing NLP/search JMOD lane; qualify source versions, model resources, API parity and Java 21/25 compatibility separately |
| Felix / Atomos | Preserve OSGi Connect, original bundle IDs, service loading and JPMS classloader distinctions; Atomos is not the unresolved JMO/Flex/Flexi family |
| JMOD / jlink | Reuse existing module inspector/graph/descriptor/image assembly; test split packages, native assets, services, signed inputs, LICENSE/NOTICE and installed SDK tools |
| Eclipse / SWT / Nebula / JDT | Preserve org.eclipse APIs, SWT widgets, Display thread, native handles, OSGi extensions and real plugin regression evidence; review EPL rights before code reuse |

JMO, Flex and Flexi names remain `HOLD_OWNER_UNRESOLVED`
until source paths, signatures, rights and owners are found.
The String -> arrays -> collections -> compiler -> further SDK
receiving order remains unchanged.

## Build-first ASF proposal gate

The first reproducibly built SDK must contain its declared
Java 21 development tools, module image and qualified native
closure. It must pass old/new API and ABI, jtreg, JNI/JVMTI,
GC/CDS, package/JMOD tools, runtime and platform tests, plus
file-level source and binary Apache-2.0 LICENSE/NOTICE/SBOM
checks. One platform passing does not qualify all OS targets.

An ASF Incubator proposal comes **after a credible first
build**, with independent governance, community/champion
and mentors, source provenance, trademarks, IP clearance
and project naming. A build alone does not mean the ASF
will accept or sponsor the project. No ASF proposal has
been submitted or approved.

**Supporting proof:** an original Java21 test fixture
preserved all prior public descriptors and executed an
unrecompiled old client against its atomized implementation.
It passed 60,000 UTF-16 comparisons in JIT and interpreter
modes, and `javac` rejected two unsafe overload/default
changes. These synthetic results are **not** M3JDK21 or
Eclipse product acceptance.

**NOT_EXECUTED:** canonical Synexia documentation/source
recipe, source-built M3JDK21/jtreg/JNI/GC, actual Eclipse
native integration, SDK-wide Apache-only legal closure and
ASF project admission. No runtime source changes here.

[Existing SDK roadmap](apache-only-jdk-convergence.md) ·
[ASF Incubator guidance](https://incubator.apache.org/guides/proposal.html) ·
[ASF branding](https://incubator.apache.org/guides/branding.html) ·
[ASF third-party licensing](https://www.apache.org/legal/resolved.html) ·
[Eclipse legal FAQ](https://www.eclipse.org/legal/faq/).
