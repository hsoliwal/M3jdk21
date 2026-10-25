<!--
/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
-->
The Eclipse Collections source comes from `eclipse-collections/eclipse-collections` commit
`00a668d624c870652bf746d10f250a4d37a777f8` (14.0.0-SNAPSHOT). It is received through the fork `hsoliwal/eclipse-collections`,
commit `d7e0b3970e611dd6be1aeca56fe6d94cf8d34fa7`, tree `97cd9b660586845b0015e4882d28af6834ed5527`.

Donor files are licensed under the Eclipse Public License 1.0 or the Eclipse Distribution License 1.0. `NOTICE.md` gives this
as `SPDX-License-Identifier: EPL-1.0 OR BSD-3-Clause`. `LICENSE-EPL-1.0.txt`, `LICENSE-EDL-1.0.txt`, `NOTICE.md`, `about.html`
and every donor source header are retained unchanged in the implementation directory, and they remain authoritative.
Eclipse Collections is a trademark of the Eclipse Foundation.

The namespace changes from `org.eclipse.collections` to `com.m3.collections.eclipse`. A package rename does not relicense
source. Five upstream files edited in the fork keep their Goldman Sachs EPL-1.0/EDL-1.0 header and carry a
`Modified 2026 by Hitesh Soliwal and Synexia contributors` line: `ArrayAdapter.java`, `ImmutableBooleanArrayList.java`,
`FastList.java`, `TreeSortedMap.java` and `immutablePrimitiveArrayList.stg`.

The 21 first-party `.java` files that the fork added carry the canonical Synexia header and are Apache-2.0 OR EPL-2.0. The
other first-party fork files keep the notices they carry, for example `m3/indexed-rb/LICENSE-APACHE-2.0.txt` and
`m3/indexed-rb/NOTICE-APACHE.txt`. `IndexedRedBlackTree.java` adapts the rotations and deletion repair of Apache Commons
Collections `TreeBidiMap` (Apache-2.0, The Apache Software Foundation). Its provenance and the retained Apache licence and
NOTICE are in `m3/indexed-rb/` of the implementation directory.

The donor Maven coordinates `org.eclipse.collections:*:14.0.0-SNAPSHOT` and the donor provenance URLs are retained for
custody. This tree is not approved for release under those coordinates. Qualified class, module, OSGi bundle, resource,
reflection and serialized identities change under the package-first exception in `NAMESPACE_EXCEPTION.json`.

Receiving metadata: Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others, Apache-2.0 OR EPL-2.0. This
covers only the authored metadata and does not relicense donor material.
