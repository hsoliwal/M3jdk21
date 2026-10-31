// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

/**
 * Build-time reference metadata for M3 item residency (m3/docs/m3-item-residency-metadata.md).
 * For every call site that obtains a transient item handle (an {@code INVOKE*} returning a
 * transient type or an array of it, or {@code NEW} of one) it records whether the reference
 * escapes, using forward def-use over ASM {@link SourceInterpreter} frames. Reuses PR #814's
 * corpus walk order ({@code jrt:/} then sorted jars, sorted entries, module-info and versioned
 * entries skipped), its argument and log helpers and its 4,000,000-cell frame budget.
 *
 * <pre>
 *   com.m3.compiletime.M3ItemReferenceScan --out DIR [--jdk] [--jars LIST] [--label NAME=LIST]...
 * </pre>
 */
public final class M3ItemReferenceScan {
    static final int FIELD = 1, ARRAY = 1 << 1, COLLECTION = 1 << 2, RETURN = 1 << 3, LAMBDA = 1 << 4,
            IDENTITY_COMPARE = 1 << 5, IDENTITY_HASH = 1 << 6, MONITOR = 1 << 7, ARG_UNKNOWN = 1 << 8,
            ANALYSIS_FAILED = 1 << 9, RETAINS_CALLEE = 1 << 10;
    static final int RETAINING = FIELD | ARRAY | COLLECTION | RETURN | LAMBDA | IDENTITY_COMPARE | IDENTITY_HASH
            | MONITOR | RETAINS_CALLEE;
    static final int SITE_UNKNOWN = 0, SITE_BEHAVIOUR_ONLY = 1, SITE_RETAINS = 2;
    static final String[] KIND_NAMES = {"FIELD", "ARRAY", "COLLECTION", "RETURN", "LAMBDA", "IDENTITY_COMPARE",
        "IDENTITY_HASH", "MONITOR", "ARG_UNKNOWN", "ANALYSIS_FAILED", "RETAINS_CALLEE"};
    static final String[] CLASS_NAMES = {"UNKNOWN", "BEHAVIOUR_ONLY", "RETAINS"};
    /** Same frame budget as {@code M3ClassScan} (PR #814). */
    static final long MAX_FRAME_CELLS = 4_000_000L;
    static final String TRANSIENT = "Lcom/m3/residency/M3Transient;";
    static final String RETAINS = "Lcom/m3/residency/M3Retains;";
    static final String BEHAVIOUR_ONLY = "Lcom/m3/residency/M3BehaviourOnly;";

    /** Seeds for libraries that cannot carry {@code @M3Transient}. */
    static final String[] SEED_TYPES = {
        "org/eclipse/swt/widgets/Item", "org/eclipse/swt/widgets/TableItem", "org/eclipse/swt/widgets/TreeItem",
        "org/eclipse/swt/widgets/TableColumn", "org/eclipse/swt/widgets/TreeColumn", "org/eclipse/swt/widgets/TabItem",
        "org/eclipse/swt/widgets/ToolItem", "org/eclipse/swt/widgets/MenuItem", "org/eclipse/swt/widgets/CoolItem",
        "org/eclipse/swt/widgets/ExpandItem", "org/eclipse/swt/widgets/TaskItem", "org/eclipse/swt/widgets/TrayItem",
        "org/eclipse/swt/custom/CTabItem", "org/eclipse/swt/custom/TableTreeItem",
        "org/eclipse/nebula/widgets/grid/GridItem", "org/eclipse/nebula/widgets/grid/GridColumn",
        "javax/swing/tree/TreePath"};
    /** Callee (owner.name) that only uses its transient argument during the call. */
    static final Set<String> BEHAVIOUR_CALLEES = Set.of("java/lang/String.valueOf", "java/lang/StringBuilder.append",
        "java/lang/StringBuffer.append", "java/util/Objects.requireNonNull", "java/io/PrintStream.println",
        "java/io/PrintStream.print");
    static final Set<String> BEHAVIOUR_NAMES_ON_SWT = Set.of("indexOf", "showItem", "setTopItem", "setInsertMark",
        "select", "deselect", "setSelection", "showColumn", "isSelected");
    static final Set<String> RETAINING_CALLEES = Set.of("org/eclipse/swt/custom/TableEditor.setEditor",
        "org/eclipse/swt/custom/TreeEditor.setEditor", "org/eclipse/swt/custom/TableTreeEditor.setEditor",
        "org/eclipse/swt/widgets/Widget.setData");
    static final Set<String> COLLECTION_NAMES = Set.of("add", "addAll", "put", "putAll", "putIfAbsent", "offer",
        "offerFirst", "offerLast", "push", "addFirst", "addLast", "set", "addElement", "insertElementAt",
        "setElementAt", "compute", "computeIfAbsent", "merge", "replace");
    static final Set<String> KEY_LOOKUP_NAMES = Set.of("get", "containsKey", "remove", "getOrDefault", "contains",
        "indexOf", "lastIndexOf");

    final Map<String, String> superOf = new HashMap<>();
    final Set<String> annotatedTransient = new HashSet<>();
    final Set<String> retainingMethods = new HashSet<>(), behaviourMethods = new HashSet<>();
    Set<String> transientTypes = Set.of();
    final List<String[]> rows = new ArrayList<>();
    final Map<String, long[]> summary = new TreeMap<>();
    final Map<Integer, int[]> sidecar = new TreeMap<>();
    long classes, methods, methodsFailed, methodsOverBudget, offsetMismatches;

    public static void main(String[] args) throws IOException {
        Path out = Path.of(M3CompileTimeStringInventory.arg(args, "--out", "target/item-reference"));
        Files.createDirectories(out);
        List<String[]> units = units(args);
        M3ItemReferenceScan scan = new M3ItemReferenceScan();
        scan.run(units, true);
        scan.closeTransientSet();
        scan.run(units, false);
        scan.write(out);
        M3CompileTimeStringInventory.log("DONE " + out + " sites=" + scan.rows.size());
    }

    /** (label, path) units: {@code --jdk} (all of jrt:/), {@code --jars LIST}, {@code --label NAME=LIST}. */
    static List<String[]> units(String[] args) throws IOException {
        List<String[]> units = new ArrayList<>();
        if (List.of(args).contains("--jdk")) units.add(new String[] {"jdk", "jrt:/"});
        for (int i = 0; i + 1 < args.length; i++) {
            if (args[i].equals("--jars")) addJars(units, "jars", args[i + 1]);
            if (args[i].equals("--label")) {
                String[] kv = args[i + 1].split("=", 2);
                addJars(units, kv[0], kv[1]);
            }
        }
        return units;
    }

    private static void addJars(List<String[]> units, String label, String list) throws IOException {
        List<String> jars = new ArrayList<>(Files.readAllLines(Path.of(list)).stream().map(String::trim)
                .filter(s -> !s.isEmpty() && !s.startsWith("#")).toList());
        Collections.sort(jars);
        jars.forEach(j -> units.add(new String[] {label, j}));
    }

    interface ClassSink { void accept(String corpus, String unit, byte[] bytes); }

    void run(List<String[]> units, boolean hierarchy) throws IOException {
        ClassSink sink = hierarchy ? (c, u, b) -> hierarchy(b) : this::sites;
        for (String[] unit : units) {
            if (unit[1].equals("jrt:/")) walkJdk(unit[0], sink);
            else walkJar(unit[0], Path.of(unit[1]), sink);
        }
    }

    private static void walkJdk(String corpus, ClassSink sink) throws IOException {
        FileSystem jrt = FileSystems.getFileSystem(URI.create("jrt:/"));
        List<Path> modules;
        try (Stream<Path> s = Files.list(jrt.getPath("/modules"))) {
            modules = s.sorted().toList();
        }
        for (Path module : modules) {
            List<Path> files;
            try (Stream<Path> s = Files.walk(module)) {
                files = s.filter(p -> p.toString().endsWith(".class"))
                        .filter(p -> !p.getFileName().toString().equals("module-info.class")).sorted().toList();
            }
            for (Path c : files) sink.accept(corpus, module.getFileName().toString(), Files.readAllBytes(c));
        }
    }

    private static void walkJar(String corpus, Path jar, ClassSink sink) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            List<? extends ZipEntry> entries = Collections.list(zip.entries()).stream()
                    .filter(e -> e.getName().endsWith(".class") && !e.getName().endsWith("module-info.class")
                            && !e.getName().startsWith("META-INF/versions/"))
                    .sorted((a, b) -> a.getName().compareTo(b.getName())).toList();
            for (ZipEntry e : entries) {
                try (InputStream in = zip.getInputStream(e)) {
                    sink.accept(corpus, jar.getFileName().toString(), in.readAllBytes());
                }
            }
        }
    }

    // ---- pass 1: hierarchy and annotations ------------------------------------------------------

    void hierarchy(byte[] bytes) {
        try {
            ClassNode cn = new ClassNode();
            new ClassReader(bytes).accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            if (cn.superName != null) superOf.putIfAbsent(cn.name, cn.superName);
            if (has(cn.invisibleAnnotations, TRANSIENT) || has(cn.visibleAnnotations, TRANSIENT)) annotatedTransient.add(cn.name);
            for (MethodNode m : cn.methods) {
                String key = cn.name + "." + m.name + m.desc;
                if (has(m.invisibleAnnotations, RETAINS) || has(m.visibleAnnotations, RETAINS)) retainingMethods.add(key);
                if (has(m.invisibleAnnotations, BEHAVIOUR_ONLY) || has(m.visibleAnnotations, BEHAVIOUR_ONLY)) behaviourMethods.add(key);
            }
        } catch (RuntimeException unreadable) {
            M3CompileTimeStringInventory.log("HIERARCHY_PARSE_FAIL " + unreadable);
        }
    }

    private static boolean has(List<AnnotationNode> list, String desc) {
        if (list == null) return false;
        for (AnnotationNode a : list) if (a.desc.equals(desc)) return true;
        return false;
    }

    /** Transient = seeds, annotated types and every class whose super chain reaches one. */
    void closeTransientSet() {
        Set<String> roots = new HashSet<>(List.of(SEED_TYPES));
        roots.addAll(annotatedTransient);
        Set<String> all = new HashSet<>(roots);
        for (String c : superOf.keySet()) {
            for (String s = c, guard = ""; s != null && guard.length() < 64; s = superOf.get(s), guard += ".") {
                if (roots.contains(s)) {
                    all.add(c);
                    break;
                }
            }
        }
        transientTypes = all;
        M3CompileTimeStringInventory.log("TRANSIENT_TYPES " + all.size() + " annotated=" + annotatedTransient.size());
    }

    private String transientOf(String desc) {
        Type t = Type.getType(desc);
        if (t.getSort() == Type.ARRAY && t.getDimensions() == 1) t = t.getElementType();
        return t.getSort() == Type.OBJECT && transientTypes.contains(t.getInternalName()) ? t.getInternalName() : null;
    }

    // ---- pass 2: sites -------------------------------------------------------------------------

    void sites(String corpus, String unit, byte[] bytes) {
        ClassNode cn = new ClassNode();
        ClassReader reader;
        try {
            reader = new ClassReader(bytes);
            reader.accept(cn, ClassReader.SKIP_FRAMES);
        } catch (RuntimeException unreadable) {
            M3CompileTimeStringInventory.log("SITE_PARSE_FAIL " + unit + " " + unreadable);
            return;
        }
        classes++;
        Map<String, int[]> offsets = M3BytecodeOffsets.of(reader);
        for (MethodNode m : cn.methods) {
            if (m.instructions.size() == 0) continue;
            List<AbstractInsnNode> obtaining = new ArrayList<>();
            for (AbstractInsnNode insn : m.instructions) {
                String type = obtained(insn);
                if (type != null) obtaining.add(insn);
            }
            if (obtaining.isEmpty()) continue;
            methods++;
            method(corpus, unit, cn, m, obtaining, offsets.get(m.name + m.desc));
        }
    }

    private String obtained(AbstractInsnNode insn) {
        if (insn instanceof MethodInsnNode call && !call.name.equals("<init>")) {
            return transientOf(Type.getReturnType(call.desc).getDescriptor());
        }
        if (insn instanceof TypeInsnNode t && t.getOpcode() == Opcodes.NEW && transientTypes.contains(t.desc)) return t.desc;
        return null;
    }

    private void method(String corpus, String unit, ClassNode cn, MethodNode m, List<AbstractInsnNode> sites, int[] bci) {
        Frame<SourceValue>[] frames = null;
        long cells = (long) m.instructions.size() * (m.maxLocals + m.maxStack + 1);
        if (cells > MAX_FRAME_CELLS) {
            methodsOverBudget++;
        } else {
            try {
                frames = new Analyzer<>(new SourceInterpreter()).analyze(cn.name, m);
            } catch (AnalyzerException | RuntimeException failed) {
                methodsFailed++;
            }
        }
        int[] ordinal = ordinals(m);
        if (bci != null && bci.length != ordinal[ordinal.length - 1]) {
            offsetMismatches++;
            bci = null;
        }
        for (AbstractInsnNode site : sites) {
            int mask = frames == null ? ANALYSIS_FAILED : M3EscapeFlow.escape(this, m, frames, site);
            int index = m.instructions.indexOf(site);
            int offset = bci == null ? -1 - ordinal[index] : bci[ordinal[index]];
            record(corpus, unit, cn, m, site, offset, mask);
        }
    }

    /** ordinal[i] = count of real instructions before index i; last slot = total real instructions. */
    private static int[] ordinals(MethodNode m) {
        int[] ordinal = new int[m.instructions.size() + 1];
        int n = 0, i = 0;
        for (AbstractInsnNode insn : m.instructions) {
            ordinal[i++] = n;
            if (insn.getOpcode() >= 0) n++;
        }
        ordinal[i] = n;
        return ordinal;
    }

    static int classOf(int mask) {
        if ((mask & RETAINING) != 0) return SITE_RETAINS;
        return mask == 0 ? SITE_BEHAVIOUR_ONLY : SITE_UNKNOWN;
    }

    private void record(String corpus, String unit, ClassNode cn, MethodNode m, AbstractInsnNode site, int bci, int mask) {
        String type = obtained(site);
        String obtain = site.getOpcode() == Opcodes.NEW ? "new" : "invoke";
        String callee = site instanceof MethodInsnNode call ? call.owner + "." + call.name : type + ".<new>";
        int cls = classOf(mask);
        int key = siteKey(cn.name, m.name, m.desc, bci);
        boolean internal = cn.name.startsWith(type.substring(0, type.lastIndexOf('/') + 1));
        rows.add(new String[] {corpus, unit, cn.name, m.name, m.desc, String.valueOf(bci), obtain, type, callee,
            CLASS_NAMES[cls], kinds(mask), String.valueOf(mask), Integer.toHexString(key), internal ? "internal" : "caller"});
        for (String bucket : new String[] {"ALL", corpus, corpus + "\t" + type, "SCOPE\t" + (internal ? "internal" : "caller"),
            corpus + "\tSCOPE\t" + (internal ? "internal" : "caller"), "OBTAIN\t" + obtain}) {
            long[] c = summary.computeIfAbsent(bucket, k -> new long[3]);
            c[cls]++;
        }
        int[] merged = sidecar.get(key);
        if (merged == null) sidecar.put(key, new int[] {cls, mask});
        else {
            merged[1] |= mask;
            merged[0] = merged[0] == SITE_RETAINS || cls == SITE_RETAINS ? SITE_RETAINS
                    : merged[0] == SITE_UNKNOWN || cls == SITE_UNKNOWN ? SITE_UNKNOWN : SITE_BEHAVIOUR_ONLY;
        }
    }

    static String kinds(int mask) {
        if (mask == 0) return "-";
        StringBuilder s = new StringBuilder();
        for (int b = 0; b < KIND_NAMES.length; b++) {
            if ((mask & (1 << b)) != 0) s.append(s.length() == 0 ? "" : ",").append(KIND_NAMES[b]);
        }
        return s.toString();
    }

    /** FNV-1a 32 over the UTF-16 units of {@code owner.name+desc@bci}; identical to SWT ItemResidency.siteKey. */
    static int siteKey(String owner, String name, String desc, int bci) {
        String s = owner + '.' + name + desc + '@' + bci;
        int h = 0x811C9DC5;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 16777619;
        }
        return h;
    }

    // ---- outputs --------------------------------------------------------------------------------

    void write(Path out) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(out.resolve("ITEM_REFERENCE_SITES.tsv"))) {
            w.write("corpus\tunit\tclass\tmethod\tdesc\tbci\tobtain\ttransient_type\tcallee\tsite_class\tkinds\tmask\tsite_key\tscope\n");
            for (String[] r : rows) w.write(String.join("\t", r) + "\n");
        }
        try (BufferedWriter w = Files.newBufferedWriter(out.resolve("ITEM_REFERENCE_SUMMARY.tsv"))) {
            w.write("bucket\tbehaviour_only\tretains\tunknown\ttotal\n");
            for (Map.Entry<String, long[]> e : summary.entrySet()) {
                long[] c = e.getValue();
                w.write(e.getKey() + "\t" + c[SITE_BEHAVIOUR_ONLY] + "\t" + c[SITE_RETAINS] + "\t" + c[SITE_UNKNOWN] + "\t"
                        + (c[0] + c[1] + c[2]) + "\n");
            }
            w.write("META\tclasses_with_sites_scanned=" + classes + "\tmethods_with_sites=" + methods + "\tanalysis_failed="
                    + methodsFailed + "\tover_budget=" + methodsOverBudget + "\toffset_mismatch_methods=" + offsetMismatches
                    + "\ttransient_types=" + transientTypes.size() + "\n");
        }
        try (OutputStream raw = Files.newOutputStream(out.resolve("item-reference-sites.m3irs"));
                DataOutputStream d = new DataOutputStream(raw)) {
            d.writeInt(0x4D334952);
            d.writeInt(1);
            d.writeInt(sidecar.size());
            for (Map.Entry<Integer, int[]> e : new TreeMap<>(sidecar).entrySet()) {
                d.writeInt(e.getKey());
                d.writeInt(e.getValue()[1] << 2 | e.getValue()[0]);
            }
        }
    }
}
