// SPDX-License-Identifier: Apache-2.0
// DEPENDENCY-SOURCE-INLINE, PROVE_FIT gate: API-surface equivalence of a source-built jar against the
// published binary jar. For every class (module-info excluded; synthetic classes skipped except package-info,
// which carries package annotations) it emits a canonical line per class header, field and method: access
// flags (minus ACC_SYNTHETIC/ACC_SUPER), name, descriptor, generic signature, thrown exceptions, constant value,
// and the sorted visible+invisible annotation descriptors (including parameter annotations). Synthetic members
// (lambda bodies, accessors, enum $values) are skipped; class-file versions are reported separately.
// View: "base" reads the root entries; "mrN" reads a multi-release jar as a JDK N runtime does (for every class
// the highest META-INF/versions/k <= N entry wins, else the root entry).
// Usage (JDK source launcher, ASM on the classpath):
//   java -cp asm.jar ApiSurface.java <published.jar> <built.jar> <diff.tsv> [base|mrN]
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class ApiSurface {
    record Side(Map<String, TreeSet<String>> classes, TreeMap<Integer, Integer> versions, boolean multiRelease) {}

    public static void main(String[] args) throws Exception {
        int release = args.length > 3 && args[3].startsWith("mr") ? Integer.parseInt(args[3].substring(2)) : 0;
        Side pub = read(Path.of(args[0]), release);
        Side built = read(Path.of(args[1]), release);
        List<String> out = new ArrayList<>();
        out.add("side\tclass\tline");
        int missing = 0, extra = 0, memberPub = 0, memberBuilt = 0, same = 0;
        TreeSet<String> all = new TreeSet<>(pub.classes().keySet());
        all.addAll(built.classes().keySet());
        for (String c : all) {
            TreeSet<String> a = pub.classes().get(c), b = built.classes().get(c);
            if (b == null) { missing++; out.add("PUBLISHED_ONLY_CLASS\t" + c + "\t" + String.join(" | ", a)); continue; }
            if (a == null) { extra++; out.add("BUILT_ONLY_CLASS\t" + c + "\t" + String.join(" | ", b)); continue; }
            boolean equal = true;
            for (String l : a) if (!b.contains(l)) { memberPub++; equal = false; out.add("PUBLISHED_ONLY\t" + c + "\t" + l); }
            for (String l : b) if (!a.contains(l)) { memberBuilt++; equal = false; out.add("BUILT_ONLY\t" + c + "\t" + l); }
            if (equal) same++;
        }
        Map<String, String> ra = resources(Path.of(args[0]), false), rb = resources(Path.of(args[1]), true);
        int resPub = 0, resBuilt = 0, resDiff = 0;
        TreeSet<String> rall = new TreeSet<>(ra.keySet());
        rall.addAll(rb.keySet());
        for (String r : rall) {
            String x = ra.get(r), y = rb.get(r);
            if (y == null) { resPub++; out.add("RESOURCE_PUBLISHED_ONLY\t" + r + "\t" + x); }
            else if (x == null) { resBuilt++; out.add("RESOURCE_BUILT_ONLY\t" + r + "\t" + y); }
            else if (!x.equals(y)) { resDiff++; out.add("RESOURCE_DIFFERS\t" + r + "\t" + x + " " + y); }
        }
        Files.write(Path.of(args[2]), out, StandardCharsets.UTF_8);
        System.out.println("view=" + (release == 0 ? "base" : "mr" + release) + " published_multi_release=" + pub.multiRelease()
                + " published_classes=" + pub.classes().size() + " built_classes=" + built.classes().size()
                + " identical_classes=" + same + " published_only_classes=" + missing + " built_only_classes=" + extra
                + " published_only_lines=" + memberPub + " built_only_lines=" + memberBuilt
                + " resources_published=" + ra.size() + " resources_published_only=" + resPub + " resources_built_only=" + resBuilt + " resources_differ=" + resDiff
                + " published_versions=" + pub.versions().toString().replace(" ", "") + " built_versions=" + built.versions().toString().replace(" ", ""));
    }

    static Map<String, String> resources(Path jar, boolean built) throws Exception {
        Map<String, String> res = new TreeMap<>();
        try (ZipFile z = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> en = z.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String n = e.getName();
                if (e.isDirectory() || n.endsWith(".class") || n.equals("META-INF/MANIFEST.MF") || n.equals("META-INF/INDEX.LIST")
                        || n.startsWith("META-INF/maven/") || n.startsWith("META-INF/versions/")
                        || (n.startsWith("META-INF/") && (n.endsWith(".SF") || n.endsWith(".RSA") || n.endsWith(".DSA") || n.endsWith(".EC")))
                        || (built && n.startsWith("META-INF/third-party/"))) continue;
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                try (InputStream in = z.getInputStream(e)) { md.update(in.readAllBytes()); }
                res.put(n, HexFormat.of().formatHex(md.digest()));
            }
        }
        return res;
    }

    static Side read(Path jar, int release) throws IOException {
        Map<String, TreeSet<String>> classes = new TreeMap<>();
        TreeMap<Integer, Integer> versions = new TreeMap<>();
        boolean mr = false;
        try (ZipFile z = new ZipFile(jar.toFile())) {
            Map<String, ZipEntry> chosen = new TreeMap<>();
            Map<String, Integer> chosenVersion = new HashMap<>();
            Enumeration<? extends ZipEntry> en = z.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String n = e.getName();
                if (!n.endsWith(".class") || n.endsWith("module-info.class")) continue;
                int v = 0;
                String key = n;
                if (n.startsWith("META-INF/versions/")) {
                    mr = true;
                    String rest = n.substring("META-INF/versions/".length());
                    int slash = rest.indexOf('/');
                    if (slash < 0) continue;
                    try { v = Integer.parseInt(rest.substring(0, slash)); } catch (NumberFormatException ex) { continue; }
                    if (release == 0 || v > release) continue;
                    key = rest.substring(slash + 1);
                } else if (n.startsWith("META-INF/")) {
                    continue;
                }
                Integer prev = chosenVersion.get(key);
                if (prev == null || v > prev) { chosen.put(key, e); chosenVersion.put(key, v); }
            }
            for (ZipEntry e : chosen.values()) {
                byte[] bytes;
                try (InputStream in = z.getInputStream(e)) { bytes = in.readAllBytes(); }
                ClassReader cr = new ClassReader(bytes);
                boolean pkgInfo = cr.getClassName().endsWith("/package-info");
                if ((cr.getAccess() & Opcodes.ACC_SYNTHETIC) != 0 && !pkgInfo) continue;
                versions.merge(((bytes[6] & 0xff) << 8) | (bytes[7] & 0xff), 1, Integer::sum);
                TreeSet<String> lines = new TreeSet<>();
                cr.accept(new Collector(lines, pkgInfo), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                classes.put(cr.getClassName(), lines);
            }
        }
        return new Side(classes, versions, mr);
    }

    static String flags(int access) {
        return Integer.toHexString(access & ~(Opcodes.ACC_SYNTHETIC | Opcodes.ACC_SUPER));
    }

    static final class Collector extends ClassVisitor {
        final TreeSet<String> lines;
        final boolean pkgInfo;
        final TreeSet<String> classAnn = new TreeSet<>();
        String header;

        Collector(TreeSet<String> lines, boolean pkgInfo) { super(Opcodes.ASM9); this.lines = lines; this.pkgInfo = pkgInfo; }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            TreeSet<String> ifs = new TreeSet<>();
            if (interfaces != null) for (String i : interfaces) ifs.add(i);
            header = pkgInfo ? "PACKAGE-INFO" : "CLASS " + flags(access) + " super=" + superName + " ifs=" + ifs + " sig=" + signature;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) { classAnn.add(desc); return null; }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            if ((access & Opcodes.ACC_SYNTHETIC) != 0) return null;
            TreeSet<String> ann = new TreeSet<>();
            String base = "FIELD " + name + " " + flags(access) + " " + desc + " sig=" + signature + " const=" + value;
            return new FieldVisitor(Opcodes.ASM9) {
                @Override public AnnotationVisitor visitAnnotation(String d, boolean v) { ann.add(d); return null; }
                @Override public void visitEnd() { lines.add(base + " ann=" + ann); }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if ((access & Opcodes.ACC_SYNTHETIC) != 0 || name.startsWith("lambda$")) return null;
            TreeSet<String> ex = new TreeSet<>();
            if (exceptions != null) for (String x : exceptions) ex.add(x);
            TreeSet<String> ann = new TreeSet<>();
            String base = "METHOD " + name + desc + " " + flags(access) + " sig=" + signature + " throws=" + ex;
            return new MethodVisitor(Opcodes.ASM9) {
                @Override public AnnotationVisitor visitAnnotation(String d, boolean v) { ann.add(d); return null; }
                @Override public AnnotationVisitor visitParameterAnnotation(int p, String d, boolean v) { ann.add(p + ":" + d); return null; }
                @Override public void visitEnd() { lines.add(base + " ann=" + ann); }
            };
        }

        @Override
        public void visitEnd() { lines.add(header + " ann=" + classAnn); }
    }
}
