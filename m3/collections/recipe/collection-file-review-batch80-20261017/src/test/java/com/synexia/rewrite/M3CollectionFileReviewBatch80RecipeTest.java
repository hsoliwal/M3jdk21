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
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

/** Static catalogue custody and declared review boundaries, without behavioral admission. */
final class M3CollectionFileReviewBatch80RecipeTest {
    private static final String ROOT="/com/synexia/rewrite/hash-pinned-text/collection-file-review-batch80/";
    private static final String PATH_ROOT="m3/collections/algorithm-catalog/";
    private static final String YAML="/META-INF/rewrite/m3-collection-file-review-batch80.yml";
    private static final String NAME="com.synexia.m3.CollectionFileReviewBatch8020261017";
    private static final String UNRELATED="unrelated.txt";
    private static final String UNTOUCHED="preserve exactly\n";
    private static final String COMPILE_SHA="7fe7626117be708f6b357756bb94325a20417dbb442a3d7d5315d4e36ad7bad5";
    private record Target(String path,String hash,int bytes,String text) { }
    private static String resource(String path) throws Exception {
        try(var stream=M3CollectionFileReviewBatch80RecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream,path);return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    private static List<Target> targets() throws Exception {
        var expected=List.of(
                new Target("m3/collections/algorithm-catalog/README-collection-file-review-20261017.md","f598ec356cd140e71a7d2c085c61ac857b8a7d8e8228723a85df7cfff60db4d9",5039,resource(ROOT+"README-collection-file-review-20261017.md")),
                new Target("m3/collections/algorithm-catalog/SOURCE_INVARIANT_CONTRACT-20261017.md","e80c95e72cdff72e8f14be43e70a768a503eaeea0b168a1ce6393696d51c1fe5",2044,resource(ROOT+"SOURCE_INVARIANT_CONTRACT-20261017.md")),
                new Target("m3/collections/algorithm-catalog/TASKS-collection-batch80-20261017.md","86062da35ac684503c468118cf925b286be92cfd470e9bc10748f8e712659cae",2311,resource(ROOT+"TASKS-collection-batch80-20261017.md")),
                new Target("m3/collections/algorithm-catalog/collection-candidate-comparison-20261017.tsv","e9d721fd062fe291f695a9dc39de2f4d90a725a49432cde5017ccac644c910cd",15104,resource(ROOT+"collection-candidate-comparison-20261017.tsv")),
                new Target("m3/collections/algorithm-catalog/collection-catalogue-input-provenance-20261017.tsv","e50ed945f9d64519744945895af73b0df57039d3079d603d59f4929c963997cf",5392,resource(ROOT+"collection-catalogue-input-provenance-20261017.tsv")),
                new Target("m3/collections/algorithm-catalog/collection-file-review-20261017.tsv","fba70d976e0521bbcaffb7474499430454cffef323b0c2b6d606b856069f2be1",63289,resource(ROOT+"collection-file-review-20261017.tsv")));
        var lines=resource(ROOT+"manifest.tsv").lines()
                .filter(line->!line.isBlank()&&!line.startsWith("#")).toList();
        assertEquals(6,lines.size());String previous="";
        for(int i=0;i<lines.size();i++) {
            String[] cells=lines.get(i).split("\t",-1);assertEquals(4,cells.length);
            var target=expected.get(i);assertEquals(target.path,cells[0]);assertEquals("ABSENT",cells[1]);
            assertEquals(target.hash,cells[2]);assertTrue(previous.compareTo(cells[0])<0);previous=cells[0];
            assertEquals(target.text,resource(ROOT+cells[3]));assertEquals(target.hash,sha(target.text));
            assertEquals(target.bytes,target.text.getBytes(StandardCharsets.UTF_8).length);
        }
        return expected;
    }
    private static String path(SourceFile file) {
        return file.getSourcePath().normalize().toString().replace('\\','/');
    }
    private static Recipe activate() throws Exception {
        var location=M3CollectionFileReviewBatch80RecipeTest.class.getResource(YAML);assertNotNull(location,YAML);
        try(var stream=M3CollectionFileReviewBatch80RecipeTest.class.getResourceAsStream(YAML)) {
            assertNotNull(stream,YAML);
            Recipe activated=Environment.builder().load(new YamlResourceLoader(stream,location.toURI(),
                    new Properties(),M3CollectionFileReviewBatch80RecipeTest.class.getClassLoader()))
                    .build().activateRecipes(NAME);
            assertEquals(1,activated.getRecipeList().size());var delivered=activated.getRecipeList().getFirst();
            assertEquals(NAME,delivered.getName());assertEquals(1,delivered.getRecipeList().size());
            var engine=assertInstanceOf(M3HashPinnedTextSnapshotRecipe.class,delivered.getRecipeList().getFirst());
            assertEquals("collection-file-review-batch80",engine.getCrateName());
            assertTrue(activated.validateAll().stream().allMatch(validation->validation.isValid()));return activated;
        }
    }
    private static List<SourceFile> freshSources(Map<String,String> source) {
        var errors=new ArrayList<Throwable>();
        var inputs=source.entrySet().stream()
                .map(entry->Parser.Input.fromString(Path.of(entry.getKey()),entry.getValue())).toList();
        var parsed=PlainTextParser.builder().build().parseInputs(inputs,null,
                new InMemoryExecutionContext(errors::add)).toList();
        assertTrue(errors.isEmpty(),()->errors.toString());assertEquals(source.size(),parsed.size());
        for(var file:parsed) {
            assertInstanceOf(PlainText.class,file);assertTrue(file.getMarkers().getMarkers().isEmpty());
            assertEquals(source.get(path(file)),file.printAll());
        }
        return parsed;
    }
    private static List<Result> run(List<SourceFile> source) throws Exception {
        var errors=new ArrayList<Throwable>();
        var changes=activate().run(new InMemoryLargeSourceSet(source),
                new InMemoryExecutionContext(errors::add),1).getChangeset().getAllResults();
        assertTrue(errors.isEmpty(),()->errors.toString());return changes;
    }
    private static Map<String,String> renderAll(List<SourceFile> source,List<Result> changes) {
        var files=new TreeMap<String,SourceFile>();
        for(var file:source)assertNull(files.put(path(file),file));
        for(var change:changes) {
            assertNull(change.getBefore());assertNotNull(change.getAfter());
            assertInstanceOf(PlainText.class,change.getAfter());
            assertNull(files.put(path(change.getAfter()),change.getAfter()));
        }
        var rendered=new TreeMap<String,String>();
        files.forEach((name,file)->rendered.put(name,file.printAll()));return rendered;
    }
    private static LinkedHashMap<String,String> state(List<Target> targets,int mask,boolean reverse) {
        var input=new LinkedHashMap<String,String>();
        for(int i=0;i<targets.size();i++)if((mask & 1<<i)!=0)input.put(targets.get(i).path,targets.get(i).text);
        input.put(UNRELATED,UNTOUCHED);if(!reverse)return input;
        var inverted=new LinkedHashMap<String,String>();
        var keys=new ArrayList<>(input.keySet());java.util.Collections.reverse(keys);
        for(String key:keys)inverted.put(key,input.get(key));return inverted;
    }
    private static void refuses(List<SourceFile> source,String reason,String target) throws Exception {
        Recipe recipe=activate();var errors=new ArrayList<Throwable>();
        var refusal=assertThrows(IllegalStateException.class,()->recipe.run(
                new InMemoryLargeSourceSet(source),new InMemoryExecutionContext(errors::add),1));
        assertTrue(refusal.getMessage().contains(reason+": "+target),refusal::getMessage);
        assertTrue(errors.isEmpty(),()->errors.toString());
    }
    /** The source-sealed TSVs contain plain tab-separated cells, without embedded/quoted tabs. */
    private static List<String[]> table(String text,String header) {
        var lines=text.lines().toList();assertEquals(header,lines.getFirst());
        int width=header.split("\t",-1).length;var rows=new ArrayList<String[]>();
        for(String line:lines.subList(1,lines.size())) {
            String[] cells=line.split("\t",-1);assertEquals(width,cells.length);
            for(String cell:cells)assertFalse(cell.isBlank());rows.add(cells);
        }
        return rows;
    }
    private static void checkCatalogue(Map<String,String> received) throws Exception {
        var comparisons=table(received.get(PATH_ROOT+"collection-candidate-comparison-20261017.tsv"),
                "donor_link_id\tcategory\tcandidate\tsource\trevision\trights\tcontract\tcomplexity\tevidence\tdecision\trationale");
        assertEquals(23,comparisons.size());var byId=new TreeMap<String,String[]>();
        var allowed=Set.of("REFERENCE_ONLY_REVIEW_REQUIRED","REJECT_DIRECT_STORAGE_DONOR","PRESERVE_API_OWNER",
                "REFERENCE_ONLY_VERSION_DIFFERENCE","REUSE_EXISTING_COMPONENTS","TAXONOMY_ONLY",
                "CORPUS_DISCOVERY_ONLY","REUSE_EXISTING_RECIPE");
        for(var row:comparisons) {
            assertTrue(row[0].matches("[A-Z][A-Z0-9_]*"));assertNull(byId.put(row[0],row));
            assertTrue(allowed.contains(row[9]));String rights=row[5];
            assertTrue(rights.startsWith("MIT")||rights.startsWith("GPL-2.0-only WITH Classpath-exception-2.0")
                    ||rights.startsWith("Apache-2.0")||rights.startsWith("EPL-1.0 AND EDL-1.0")
                    ||rights.startsWith("problem reference only")||rights.startsWith("article reference only"));
        }
        for(String id:List.of("ALG_HEAP","ALG_AVL","ALG_CURSOR","APACHE_INDEX"))
            assertEquals("REJECT_DIRECT_STORAGE_DONOR",byId.get(id)[9]);
        assertTrue(byId.get("ALG_CURSOR")[6].contains("Node<T>[]"));
        assertEquals("REFERENCE_ONLY_VERSION_DIFFERENCE",byId.get("GUAVA_FILTER")[9]);
        assertTrue(byId.get("GUAVA_FILTER")[4].contains("candidate 33.7.2"));
        assertTrue(byId.get("GUAVA_FILTER")[6].contains("33.4.8-jre"));
        assertTrue(byId.get("EC_LIST")[5].startsWith("EPL-1.0 AND EDL-1.0"));
        assertTrue(byId.get("JDK_CONTRACT")[5].contains("no source copied"));
        for(String id:List.of("LC_REMOVE","LC_DEDUP","LC_BOUND","HR_DYNAMIC_ARRAY","HR_BITSET","GFG_BITMASK"))
            assertEquals("TAXONOMY_ONLY",byId.get(id)[9]);
        for(String id:List.of("LC_LICENSED_CORPUS","GFG_LICENSED_CORPUS"))
            assertEquals("CORPUS_DISCOVERY_ONLY",byId.get(id)[9]);
        assertEquals("REUSE_EXISTING_RECIPE",byId.get("M3_SERIAL_RECIPE")[9]);
        assertTrue(byId.get("M3_SERIAL_RECIPE")[8].contains("authority methods all false"));
        var review=table(received.get(PATH_ROOT+"collection-file-review-20261017.tsv"),
                "schema\tordinal\tsource_path\tsha256\trole\tcategory\toperation\tstorage_fields_observed\tdonor_link_ids\tsource_review_scope\tcompile_proof\tbehavior_proof\tinterface_proof\trecipe_link_id\tdecision\tcontract_risk");
        assertEquals(80,review.size());var paths=new HashSet<String>();var hashes=new HashSet<String>();
        var categories=new TreeMap<String,Integer>();var sourceSet=new StringBuilder();int ordinal=0;
        for(var row:review) {
            assertEquals("M3_COLLECTION_FILE_REVIEW_V1",row[0]);assertEquals(++ordinal,Integer.parseInt(row[1]));
            assertTrue(row[2].matches("m3/collections/src/main/java/.*[.]java"));assertTrue(paths.add(row[2]));
            assertTrue(row[3].matches("[0-9a-f]{64}"));assertTrue(hashes.add(row[3]));
            categories.merge(row[5],1,Integer::sum);
            for(String id:row[8].split(";"))assertTrue(byId.containsKey(id),id);
            assertEquals("SERIAL_FULL_BYTE_READ;CURATED_DECLARATION_DOC_ROLE;LEXICAL_EXTRACTION_NOT_EXHAUSTIVE",row[9]);
            assertEquals("SHARED_JAVA21_WARNINGS_AS_ERRORS_PASS;core80-02.json@sha256:"+COMPILE_SHA,row[10]);
            assertEquals("REVIEW_REQUIRED;NOT_ESTABLISHED_BY_SHARED_COMPILE",row[11]);
            assertEquals("REVIEW_REQUIRED;NO_PER_FILE_ABI_ATTESTATION",row[12]);
            assertEquals("M3_SERIAL_RECIPE",row[13]);assertEquals("INVENTORY_KEEP_INCUMBENT_REVIEW_REQUIRED",row[14]);
            sourceSet.append(row[2]).append('\t').append(row[3]).append('\n');
        }
        assertTrue(paths.stream().anyMatch(path->path.endsWith("/package-info.java")));
        assertTrue(paths.stream().anyMatch(path->path.endsWith("/module-info.java")));
        assertEquals(Map.ofEntries(Map.entry("API_CONTRACT",18),Map.entry("ARRAY_GROWTH",1),Map.entry("ARRAY_OFFSETS",3),Map.entry("BINARY_HEAP",2),Map.entry("BIT_STATE",2),Map.entry("COLLECTION_FACTORY",1),Map.entry("ENUM_ORDINAL",3),Map.entry("HASH_PROBE",5),Map.entry("IDENTITY_HASH",2),Map.entry("IMMUTABLE_CAS",2),Map.entry("INDEXED_LINKS",2),Map.entry("LAZY_POLICY",5),Map.entry("LAZY_STATE",1),Map.entry("LOCKED_CONCURRENCY",5),Map.entry("MODULE_BOUNDARY",2),Map.entry("ORDERED_HASH",3),Map.entry("PRIMITIVE_LANES",4),Map.entry("PROGRESS",7),Map.entry("RING_DEQUE",1),Map.entry("SLOT_VIEW",4),Map.entry("STABLE_SLOT_LIFECYCLE",2),Map.entry("TRAVERSAL",2),Map.entry("TREE_ORDER",2),Map.entry("WEAK_KEYS",1)),categories);
        assertEquals("fe4a87b11ba635110b41f3168196b1e3fb5a30b9814e2fda05d525984b688acd",sha(sourceSet.toString()));
        var provenance=table(received.get(PATH_ROOT+"collection-catalogue-input-provenance-20261017.tsv"),
                "input_id\trepository\trevision\tpath\tgit_blob\tobservation");
        assertEquals(22,provenance.size());var inputIds=new HashSet<String>();
        for(var row:provenance) {
            assertTrue(inputIds.add(row[0]));assertTrue(row[2].matches("[0-9a-f]{40}|BLOB_PIN_ONLY"));
            assertTrue(row[4].matches("[0-9a-f]{40}"));
        }
        assertTrue(received.get(PATH_ROOT+"TASKS-collection-batch80-20261017.md")
                .contains("0 of 80 per-type equivalence admissions"));
        assertTrue(received.get(PATH_ROOT+"README-collection-file-review-20261017.md")
                .contains("admit no replacement or performance winner"));
    }
    @Test void allSixtyFourInstallationStatesBothOrdersAndFreshParseFixedPoint() throws Exception {
        var rows=targets();int states=0;
        for(int mask=0;mask<64;mask++)for(boolean reverse:List.of(false,true)) {
            var source=freshSources(state(rows,mask,reverse));var changes=run(source);
            assertEquals(6-Integer.bitCount(mask),changes.size());
            var rendered=renderAll(source,changes);assertEquals(7,rendered.size());
            for(var target:rows) {
                assertEquals(target.text,rendered.get(target.path));assertEquals(target.hash,sha(rendered.get(target.path)));
            }
            assertEquals(UNTOUCHED,rendered.get(UNRELATED));checkCatalogue(rendered);
            var reparsed=freshSources(rendered);assertTrue(run(reparsed).isEmpty());
            assertEquals(rendered,renderAll(reparsed,List.of()));states++;
        }
        assertEquals(128,states);
    }
    @Test void compositeRefusesDriftAndDuplicatesForAllSixTargets() throws Exception {
        var rows=targets();var baseline=state(rows,63,false);
        for(var target:rows) {
            var drift=new LinkedHashMap<>(baseline);drift.put(target.path,"drift\n");
            refuses(freshSources(drift),"source drift",target.path);
            var duplicate=new ArrayList<>(freshSources(baseline));
            duplicate.addAll(freshSources(Map.of(target.path,target.text)));
            refuses(duplicate,"duplicate target",target.path);
        }
    }
}
