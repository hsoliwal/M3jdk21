// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Phase A NEC probe driver (scratch evidence, reflective so it names no donor package in source).
// args: <donor package> <corpus root> <out dir> <source dir>...
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.*;

public final class DonorPatternDump {
    static Object call(Object target, String name) throws Exception {
        Method m = target.getClass().getMethod(name); m.setAccessible(true); return m.invoke(target);
    }
    public static void main(String[] a) throws Exception {
        String pkg = a[0]; Path root = Path.of(a[1]); Path out = Path.of(a[2]);
        Class<?> atomizer = Class.forName(pkg + ".M3FileAtomizer");
        Class<?> index = Class.forName(pkg + ".M3AtomPatternIndex");
        Method analyze = atomizer.getMethod("analyze", String.class, String.class, byte[].class, long.class);
        Method classify = index.getMethod("classify", List.class);
        List<Path> files = new ArrayList<>();
        for (int i = 3; i < a.length; i++) try (Stream<Path> w = Files.walk(root.resolve(a[i]))) {
            w.filter(p -> p.toString().endsWith(".java")).forEach(files::add); }
        files.sort(Comparator.naturalOrder());
        List<Object> atoms = new ArrayList<>(); Map<Object,String> pathOf = new IdentityHashMap<>();
        int refused = 0; List<String> refusals = new ArrayList<>();
        for (Path f : files) {
            String rel = root.relativize(f).toString().replace(java.io.File.separatorChar, '/');
            String module = rel.split("/")[1];
            try {
                @SuppressWarnings("unchecked") List<Object> got = (List<Object>) analyze.invoke(null, module, rel, Files.readAllBytes(f), 16L << 20);
                for (Object at : got) { atoms.add(at); pathOf.put(at, rel); }
            } catch (java.lang.reflect.InvocationTargetException e) { refused++; refusals.add(rel + "\t" + e.getCause()); }
        }
        @SuppressWarnings("unchecked") Map<String,Object> entries = (Map<String,Object>) classify.invoke(null, atoms);
        List<String> atomRows = new ArrayList<>(); Map<String, List<String>> groups = new TreeMap<>();
        Map<String,Integer> states = new TreeMap<>();
        for (Object at : atoms) {
            String id = (String) call(at, "id"); Object e = entries.get(id);
            String pid = (String) call(e, "patternId"); String st = call(e, "state").toString();
            states.merge(st, 1, Integer::sum);
            String member = pathOf.get(at) + "#" + call(at, "owner") + "#" + call(at, "kind") + "#" + call(at, "name") + "#" + call(at, "startChar");
            atomRows.add(String.join("\t", pathOf.get(at), String.valueOf(call(at, "startChar")), String.valueOf(call(at, "endChar")),
                String.valueOf(call(at, "kind")), (String) call(at, "owner"), (String) call(at, "name"), id, (String) call(at, "exactRoot"),
                pid, st, String.valueOf(call(e, "members")), String.valueOf(call(e, "exactMembers"))));
            groups.computeIfAbsent(pid + "\t" + st, k -> new ArrayList<>()).add(member);
        }
        Collections.sort(atomRows);
        List<String> groupRows = new ArrayList<>();
        groups.forEach((k, v) -> { Collections.sort(v); groupRows.add(k + "\t" + v.size() + "\t" + String.join(",", v)); });
        Files.createDirectories(out);
        Files.write(out.resolve("DONOR_ATOMS.tsv"), atomRows, StandardCharsets.UTF_8);
        Files.write(out.resolve("DONOR_PATTERN_GROUPS.tsv"), groupRows, StandardCharsets.UTF_8);
        Files.write(out.resolve("DONOR_REFUSALS.tsv"), refusals, StandardCharsets.UTF_8);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] g = Files.readAllBytes(out.resolve("DONOR_PATTERN_GROUPS.tsv"));
        System.out.println("FILES=" + files.size() + " REFUSED=" + refused + " ATOMS=" + atoms.size() + " GROUPS=" + groups.size()
            + " STATES=" + states + " GROUPS_SHA256=" + HexFormat.of().formatHex(md.digest(g)));
    }
}
