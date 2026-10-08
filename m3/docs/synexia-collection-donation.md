<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Synexia collection receiving repair

Synexia owns reusable source, Maven/OpenRewrite recipes, fixtures and convergence evidence.
M3JDK21 receives qualified source into its existing separately licensed com.m3.collections
module. Apache-2.0 reuse includes the recipes; original copyright, NOTICE and modification
notices remain attached. OpenJDK and third-party rights remain per file.

The 2026-10-08 successor preserves all current public APIs and 37 of 39 Java files byte-for-byte,
adding 49 files and evolving only the existing M3LongList interface and factory. The
result is 65 main Java and 23 test Java sources. Current primitive list/deque, identity,
enum, hash/heap implementations and tests are retained. Generic ordered/sorted collections,
weak maps, blocking queues, lazy state and progress are restored from qualified canonical
postimages with their original notices. No Synexia runtime dependency is introduced.

Use m3/collections/verify_donation.py for current product qualification. The frozen lane
recipe has a separate Maven fixture. Do not call the historical installer against current
expanded sources. The dedicated workflow exercises both independent gates.

The actual application and qualification receipts are under m3/collections/qualification.
Historical 20261007 receipts remain revision-scoped in Synexia. Current destination blob
and mode readback, not merge status or ancestry, establishes that content landed.

The existing global map remains byte-identical, with STRING active and later phases
pending. Full reactor, OpenJDK/jtreg/HotSpot, provider/platform, retained heap and exhaustive
convergence remain separate gates. Finite correctness and cost samples do not prove that
all collections are universally fastest or that the 4,770-path family catalogue is complete.
