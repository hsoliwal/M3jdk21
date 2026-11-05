// Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
import static org.junit.jupiter.api.Assertions.*;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/** Existing-owner workflow maintenance only; does not execute hosted workloads locally. */
final class M3JoinedReferenceWorkflowTest {
    static final String REF = "1a79e669ec536ac45bf7a1566596aa05977bf18b";
    static final String LEGACY = ".github/workflows/m3-joined-range-directory.yml";
    static final String NLP = ".github/workflows/m3-current-nlp-search-jmods-20261018.yml";
    static final String MANIFEST = "m3/tooling/joined-range-directory/REFERENCE_REPLAY.json";
    static String legacyBefore, nlpBefore, legacyAfter, nlpAfter, reference;
    static int rejected, bashChecks, pythonChecks, commands;
    record Scalar(String tag, String value) { }
    @BeforeAll static void inputs() throws Exception {
        legacyBefore = resource("legacy-before.yml");
        nlpBefore = resource("nlp-before.yml");
        assertEquals("81e9f9ca6de6c06e547767ba8c87d42b7746825d", blob(legacyBefore));
        assertEquals("3c0fc67646b776b884a413ab6b62695ecac22aeb", blob(nlpBefore));
        Path root = Path.of(System.getProperty("m3.receiver.root")).toRealPath();
        legacyAfter = Files.readString(root.resolve(LEGACY));
        nlpAfter = Files.readString(root.resolve(NLP));
        reference = Files.readString(root.resolve(MANIFEST));
        Path parserJar = Path.of(Yaml.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        assertEquals(System.getProperty("m3.yaml.sha256"), sha(Files.readAllBytes(parserJar)));
    }
    static String resource(String name) throws Exception {
        try (var input = M3JoinedReferenceWorkflowTest.class.getResourceAsStream("/" + name)) {
            assertNotNull(input); return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    static String sha(byte[] b) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
    }
    static String blob(String text) throws Exception {
        byte[] b = text.getBytes(StandardCharsets.UTF_8);
        var digest = MessageDigest.getInstance("SHA-1");
        digest.update(("blob " + b.length + "\0").getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest(b));
    }
    static Object node(Node n) {
        if (n instanceof ScalarNode s) return new Scalar(s.getTag().getValue(), s.getValue());
        if (n instanceof SequenceNode s) {
            List<Object> result = new ArrayList<>();
            for (Node child : s.getValue()) result.add(node(child));
            return result;
        }
        if (n instanceof MappingNode m) {
            Map<String,Object> result = new TreeMap<>();
            for (var tuple : m.getValue()) {
                assertInstanceOf(ScalarNode.class, tuple.getKeyNode());
                String key = ((ScalarNode)tuple.getKeyNode()).getValue();
                assertNull(result.put(key, node(tuple.getValueNode())), "duplicate YAML key " + key);
            }
            return result;
        }
        throw new AssertionError("unsupported YAML node " + n);
    }
    static Map<String,Object> document(String text) {
        LoaderOptions options = new LoaderOptions(); options.setAllowDuplicateKeys(false);
        return map(node(new Yaml(new SafeConstructor(options)).compose(new StringReader(text))));
    }
    @SuppressWarnings("unchecked")
    static Map<String,Object> map(Object value) {
        assertInstanceOf(Map.class, value); return (Map<String,Object>)value;
    }
    @SuppressWarnings("unchecked")
    static List<Object> list(Object value) {
        assertInstanceOf(List.class, value); return (List<Object>)value;
    }
    static Object get(Object root, String... path) {
        Object value = root;
        for (String key : path) value = map(value).get(key);
        assertNotNull(value); return value;
    }
    static String scalar(Object value) { return assertInstanceOf(Scalar.class,value).value(); }
    static void mask(Map<String,Object> value, String key) {
        assertNotNull(value.get(key)); value.put(key, new Scalar("ignored", "approved-label-or-reference"));
    }
    static List<Object> legacySteps(Map<String,Object> doc) {
        return list(get(doc,"jobs","joined-range-directory","steps"));
    }
    static void admit(String historical, String current, String metadata) {
        var before = document(legacyBefore); var after = document(historical);
        var oldSteps = legacySteps(before); var steps = legacySteps(after);
        assertEquals(5, steps.size()); assertEquals(oldSteps.size(), steps.size());
        assertTrue(scalar(get(after,"name")).toLowerCase(Locale.ROOT).contains("historical"));
        assertTrue(scalar(get(after,"jobs","joined-range-directory","name")).toLowerCase(Locale.ROOT).contains("historical"));
        for (int index : new int[]{1,2})
            assertTrue(scalar(get(steps.get(index),"name")).toLowerCase(Locale.ROOT).contains("historical"));
        assertEquals(REF, scalar(get(steps.get(1),"with","ref")));
        assertEquals(REF, scalar(get(steps.get(2),"env","EXPECTED_SOURCE_SHA")));
        // Every executed legacy command, artifact fence, action pin and permission remains equal.
        mask(before,"name"); mask(after,"name");
        mask(map(get(before,"jobs","joined-range-directory")),"name");
        mask(map(get(after,"jobs","joined-range-directory")),"name");
        for (int index : new int[]{1,2}) {
            mask(map(oldSteps.get(index)),"name"); mask(map(steps.get(index)),"name");
        }
        mask(map(get(oldSteps.get(1),"with")),"ref"); mask(map(get(steps.get(1),"with")),"ref");
        mask(map(get(oldSteps.get(2),"env")),"EXPECTED_SOURCE_SHA");
        mask(map(get(steps.get(2),"env")),"EXPECTED_SOURCE_SHA");
        assertEquals(before,after,"all other historical workflow structure and command bytes");
        var oldCurrent = document(nlpBefore); var newCurrent = document(current);
        List<Object> oldPaths = list(get(oldCurrent,"on","pull_request","paths"));
        List<Object> newPaths = list(get(newCurrent,"on","pull_request","paths"));
        assertEquals(oldPaths.size()+2,newPaths.size());
        for (String name : List.of(LEGACY,MANIFEST))
            assertEquals(1,newPaths.stream().filter(x -> scalar(x).equals(name)).count());
        var retained = new ArrayList<>(newPaths);
        retained.removeIf(x -> List.of(LEGACY,MANIFEST).contains(scalar(x)));
        assertEquals(oldPaths,retained,"preserve all existing NLP triggers and their order");
        map(get(newCurrent,"on","pull_request")).put("paths",oldPaths);
        assertEquals(oldCurrent,newCurrent,"all current jobs, donor refs, V8 commands and checkout semantics");
        var ref = document(metadata);
        assertEquals(REF,scalar(get(ref,"historicalCommit")));
        assertEquals("false",scalar(get(ref,"historicalReplayIsCurrentAdmission")));
        assertEquals("false",scalar(get(ref,"currentHeadBoundInHistoricalReceipts")));
        assertEquals(NLP,scalar(get(ref,"currentValidationWorkflow")));
        assertEquals("5abff0ff5462a74439ceb13610913417ca5a1b2c4c8ddf0ba9d26a2428f1237d",
                scalar(get(ref,"unchangedV8DeclarationSha256")));
        assertEquals("145",scalar(get(ref,"unchangedNlpLockedJavaSources")));
    }
    @Test void exactReplayPreservesIndependentWorkflowContracts() {
        admit(legacyAfter,nlpAfter,reference);
    }
    @Test void identitiesAndCurrentTriggerPathsAreDistinct() {
        var old = document(legacyBefore); var after = document(legacyAfter); var current = document(nlpAfter);
        assertNotEquals(scalar(get(legacySteps(old).get(1),"with","ref")),
                scalar(get(legacySteps(after).get(1),"with","ref")));
        var currentSteps = list(get(current,"jobs","current-forks","steps"));
        assertEquals("Check out the M3 receiver",scalar(get(currentSteps.getFirst(),"name")));
        assertFalse(map(get(currentSteps.getFirst(),"with")).containsKey("ref"),
                "retain the existing dynamic GitHub merge checkout semantics");
        assertTrue(legacyAfter.contains("reserve,cap=8*1024**3,512*1024**2"));
        assertTrue(legacyAfter.contains("if-no-files-found: error"));
        assertTrue(nlpAfter.contains("--receiver-transition"));
    }
    static String replaceOnce(String source, String before, String after) {
        int at = source.indexOf(before); assertTrue(at>=0,before);
        return source.substring(0,at)+after+source.substring(at+before.length());
    }
    @Test void driftAndScopeConfusionAreRejected() {
        List<String[]> cases = new ArrayList<>();
        cases.add(new String[]{replaceOnce(legacyAfter,"ref: '"+REF+"'","ref: '0000000000000000000000000000000000000000'"),nlpAfter,reference});
        cases.add(new String[]{replaceOnce(legacyAfter,"EXPECTED_SOURCE_SHA: '"+REF+"'","EXPECTED_SOURCE_SHA: '"+ "1".repeat(40)+"'"),nlpAfter,reference});
        cases.add(new String[]{replaceOnce(legacyAfter,"reserve,cap=8*1024**3,512*1024**2","reserve,cap=0,1024*1024**2"),nlpAfter,reference});
        cases.add(new String[]{replaceOnce(legacyAfter,"contents: read","contents: write"),nlpAfter,reference});
        cases.add(new String[]{replaceOnce(legacyAfter,"actions/checkout@11d5960a326750d5838078e36cf38b85af677262","actions/checkout@"+"0".repeat(40)),nlpAfter,reference});
        cases.add(new String[]{replaceOnce(legacyAfter,"if-no-files-found: error","if-no-files-found: warn"),nlpAfter,reference});
        cases.add(new String[]{legacyAfter,replaceOnce(nlpAfter,"      - '"+LEGACY+"'\n",""),reference});
        cases.add(new String[]{legacyAfter,replaceOnce(nlpAfter,"mvn -B -ntp","mvn -B -ntp -DskipTests=true"),reference});
        cases.add(new String[]{legacyAfter,nlpAfter,replaceOnce(reference,"\"historicalReplayIsCurrentAdmission\": false","\"historicalReplayIsCurrentAdmission\": true")});
        for (String[] values : cases) {
            assertThrows(AssertionError.class,()->admit(values[0],values[1],values[2]));
            rejected++;
        }
        assertEquals(9,rejected);
    }
    static void command(List<String> argv, String stdin) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(argv).redirectErrorStream(true);
        builder.environment().remove("BASH_ENV"); builder.environment().remove("ENV");
        Process child = builder.start();
        try {
            child.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));child.getOutputStream().close();
            assertTrue(child.waitFor(30,TimeUnit.SECONDS),"bounded syntax command");
            byte[] output = child.getInputStream().readAllBytes();assertTrue(output.length<32768);
            assertEquals(0,child.exitValue(),new String(output,StandardCharsets.UTF_8));
            commands++;
        } finally {
            if (child.isAlive()) { child.destroyForcibly();assertTrue(child.waitFor(5,TimeUnit.SECONDS)); }
            assertFalse(child.isAlive());
        }
    }
    @Test void emittedPythonAndShellScriptsPassRealSyntaxChecks() throws Exception {
        for (String text : List.of(legacyAfter,nlpAfter)) {
            var jobs = map(get(document(text),"jobs"));
            for (Object job : jobs.values()) for (Object raw : list(get(job,"steps"))) {
                var step = map(raw);if (!step.containsKey("run")) continue;
                String script = scalar(step.get("run"));
                command(List.of(System.getProperty("m3.bash"),"--noprofile","--norc","-n"),script);
                bashChecks++;
                List<String> lines = script.lines().toList();
                for (int n=0;n<lines.size();n++) if (lines.get(n).contains("<<'PY'")) {
                    StringBuilder python = new StringBuilder();int at=n+1;
                    while (at<lines.size() && !lines.get(at).equals("PY")) python.append(lines.get(at++)).append('\n');
                    assertTrue(at<lines.size(),"closed Python heredoc");
                    command(List.of(System.getProperty("m3.python"),"-X","utf8","-I","-S","-B","-c",
                            "import ast,sys;ast.parse(sys.stdin.buffer.read().decode('utf-8'))"),python.toString());
                    pythonChecks++;n=at;
                }
            }
        }
        assertEquals(7,bashChecks);assertEquals(4,pythonChecks);
    }
    @AfterAll static void report() {
        assertEquals(9,rejected);assertEquals(4,pythonChecks);assertEquals(bashChecks+pythonChecks,commands);
        System.out.printf(Locale.ROOT,
                "{\"schema\":\"M3_REFERENCE_WORKFLOW_CPU_V1\",\"negativeCases\":%d,\"bashSyntax\":%d,\"pythonSyntax\":%d,\"terminalCommands\":%d,\"hostedExecuted\":false}%n",
                rejected,bashChecks,pythonChecks,commands);
    }
}
