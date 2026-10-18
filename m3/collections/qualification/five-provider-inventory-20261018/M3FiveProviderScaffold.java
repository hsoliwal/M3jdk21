/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import com.google.common.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Template-first (TFG) generator for the five-provider collection inventory and scaffold.
 *
 * <pre>
 * java -cp &lt;guava&gt;;&lt;commons-collections4&gt;;&lt;spring-core&gt;;&lt;spring-jcl&gt; M3FiveProviderScaffold.java inventory &lt;qualificationDir&gt;
 * java -cp ... M3FiveProviderScaffold.java scaffold &lt;qualificationDir&gt; &lt;implementationsDir&gt;
 * </pre>
 *
 * <p>{@code inventory} derives INVENTORY.tsv from the pinned #1278 candidate list, PROVIDERS.tsv,
 * STATUS_OVERLAY.tsv and FIRST_PARTY_ROWS.tsv. {@code scaffold} reads INVENTORY.tsv and, for every
 * OPEN JDK/Guava/Apache/Spring row, writes one module from scaffold-template/ whose class mirrors
 * the provider's public surface by reflection with bodies that throw UnsupportedOperationException.
 * Eclipse rows are PATTERN_ONLY and never generated. Output is deterministic (sorted, LF).
 */
public final class M3FiveProviderScaffold {
    private static final List<String> INVENTORY_COLUMNS = List.of("type", "provider", "version", "kind",
            "categories", "owned_status", "m3_owner", "evidence", "reuse_owner", "priority", "source", "module");
    private static final List<String> FAMILY_ORDER = List.of("18", "19", "01", "08", "09", "12", "15", "16",
            "02", "03", "05", "11", "06", "14", "07", "13", "04", "10", "17");
    private static final Set<String> P1 = Set.of("01", "08", "09", "12", "15", "16", "18", "19");
    private static final Set<String> LEGACY = Set.of("Hashtable", "Properties", "Stack", "Vector", "Attributes",
            "OrderedProperties", "SortedProperties", "ArrayStack", "MultiValueMap");
    private static final String DECORATOR = "^(Predicated|Transformed|Synchronized|Unmodifiable|FixedSize|Lazy|"
            + "Defaulted|Composite|Growth|Collection)[A-Z].*";

    private M3FiveProviderScaffold() { }

    public static void main(String[] args) throws Exception {
        Path q = Path.of(args[1]).toAbsolutePath().normalize();
        switch (args[0]) {
            case "inventory" -> inventory(q);
            case "scaffold" -> scaffold(q, Path.of(args[2]).toAbsolutePath().normalize());
            default -> throw new IllegalArgumentException("unknown command " + args[0]);
        }
    }

    // ---------------------------------------------------------------- TSV atoms
    static List<Map<String, String>> read(Path file) throws Exception {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String[] head = lines.get(0).split("\t", -1);
        List<Map<String, String>> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) { continue; }
            String[] cells = line.split("\t", -1);
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < head.length; i++) { row.put(head[i], i < cells.length ? cells[i] : ""); }
            rows.add(row);
        }
        return rows;
    }

    static Map<String, Map<String, String>> index(List<Map<String, String>> rows, String key) {
        Map<String, Map<String, String>> map = new HashMap<>();
        for (Map<String, String> row : rows) { map.put(row.get(key), row); }
        return map;
    }

    static void write(Path file, List<String> columns, List<Map<String, String>> rows) throws Exception {
        StringBuilder out = new StringBuilder(String.join("\t", columns)).append('\n');
        for (Map<String, String> row : rows) {
            out.append(columns.stream().map(c -> row.getOrDefault(c, "-")).collect(Collectors.joining("\t"))).append('\n');
        }
        text(file, out.toString());
    }

    static void text(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content.replace("\r\n", "\n"), StandardCharsets.UTF_8);
    }

    static String fill(String template, Map<String, String> values) {
        String out = template;
        for (Map.Entry<String, String> v : values.entrySet()) { out = out.replace("${" + v.getKey() + "}", v.getValue()); }
        return out;
    }

    // ---------------------------------------------------------------- inventory
    static String simple(String binary) {
        return binary.substring(binary.lastIndexOf('.') + 1).replace("$", "");
    }

    static String kebab(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1-$2").replaceAll("([A-Z])([A-Z][a-z])", "$1-$2").toLowerCase(Locale.ROOT);
    }

    static String family(String categories) {
        Set<String> codes = new TreeSet<>(Arrays.asList(categories.split(",")));
        return FAMILY_ORDER.stream().filter(codes::contains).findFirst().orElse("17");
    }

    static String priority(String type, String categories) {
        String name = simple(type);
        if (LEGACY.contains(name)) { return "P4"; }
        if (name.matches(DECORATOR)) { return "P3"; }
        return P1.contains(family(categories)) ? "P1" : "P2";
    }

    static String kind(String type) {
        try {
            Class<?> c = Class.forName(type, false, M3FiveProviderScaffold.class.getClassLoader());
            int m = c.getModifiers();
            String base = c.isInterface() ? "interface" : c.isEnum() ? "enum" : "class";
            return (Modifier.isAbstract(m) && !c.isInterface() ? "abstract " : Modifier.isFinal(m) ? "final " : "") + base;
        } catch (ClassNotFoundException | LinkageError e) {
            return "UNLOADABLE:" + e.getClass().getSimpleName();
        }
    }

    /**
     * Expands peer ledger groups such as {@code JDK:ConcurrentSkipListMap/Set} or {@code COMMONS:TransformedX family}
     * into "lib:SimpleName" keys that exist in the candidate universe.
     */
    static Map<String, List<String>> peerClaims(Path file, Set<String> known) throws Exception {
        Map<String, String> libs = Map.of("JDK", "jdk", "GUAVA", "guava", "COMMONS", "apache", "SPRING", "spring", "ECLIPSE", "eclipse");
        Map<String, List<String>> claims = new TreeMap<>();
        for (Map<String, String> row : read(file)) {
            String peer = row.get("peer_owner") + " (" + row.get("peer_ref") + ")";
            for (String group : row.get("provider_types").split(";")) {
                String[] parts = group.split(":", 2);
                String lib = libs.get(parts[0].trim());
                if (lib == null || parts.length < 2) { continue; }
                String[] names = parts[1].replace(" family", "").split("/");
                String first = names[0].trim();
                for (String raw : names) {
                    String n = raw.trim();
                    List<String> keys = new ArrayList<>();
                    if (n.endsWith("X")) {
                        String prefix = lib + ":" + n.substring(0, n.length() - 1);
                        known.stream().filter(k -> k.startsWith(prefix)).forEach(keys::add);
                    } else if (known.contains(lib + ":" + n)) {
                        keys.add(lib + ":" + n);
                    } else {
                        String stem = first.replaceAll("[A-Z][a-z0-9]*$", "");
                        if (known.contains(lib + ":" + stem + n)) { keys.add(lib + ":" + stem + n); }
                    }
                    for (String k : keys) { claims.computeIfAbsent(k, x -> new ArrayList<>()).add(peer); }
                }
            }
        }
        return claims;
    }

    static void inventory(Path q) throws Exception {
        Map<String, Map<String, String>> providers = index(read(q.resolve("PROVIDERS.tsv")), "code");
        Map<String, Map<String, String>> overlay = index(read(q.resolve("STATUS_OVERLAY.tsv")), "type");
        List<Map<String, String>> candidates = read(q.resolve("CANDIDATES_PR1278.tsv"));
        Set<String> known = new TreeSet<>();
        for (Map<String, String> c : candidates) {
            known.add(providers.get(c.get("artifact")).get("lib") + ":" + simple(c.get("binary_type")));
        }
        Map<String, List<String>> peers = peerClaims(q.resolve("PEER_SCAFFOLD_CLAIMS.tsv"), known);
        List<Map<String, String>> out = new ArrayList<>();
        for (Map<String, String> c : candidates) {
            String type = c.get("binary_type");
            Map<String, String> p = providers.get(c.get("artifact"));
            Map<String, String> o = overlay.getOrDefault(type, Map.of());
            boolean eclipse = p.get("lib").equals("eclipse");
            String status = o.getOrDefault("owned_status", eclipse ? "PATTERN_ONLY" : "OPEN");
            boolean open = status.equals("OPEN");
            Map<String, String> row = new LinkedHashMap<>();
            row.put("type", type);
            row.put("provider", p.get("lib"));
            row.put("version", p.get("version"));
            row.put("kind", eclipse ? "-" : kind(type));
            row.put("categories", c.get("categories"));
            row.put("owned_status", status);
            String owner = o.getOrDefault("m3_owner", "-");
            row.put("m3_owner", open && owner.equals("-") ? p.get("package") + ".M3" + simple(type) : owner);
            row.put("evidence", o.getOrDefault("evidence", eclipse ? "#1278 candidate; EPL pattern evidence only" : "#1278 candidate"));
            String reuse = o.getOrDefault("reuse_owner", "-");
            List<String> peer = peers.get(p.get("lib") + ":" + simple(type));
            if (peer != null) {
                String hint = "peer native skeleton " + String.join(", ", new TreeSet<>(peer));
                reuse = reuse.equals("-") ? hint : reuse + "; " + hint;
            }
            row.put("reuse_owner", reuse);
            row.put("priority", eclipse ? "-" : priority(type, c.get("categories")));
            row.put("source", eclipse ? "provider-pattern-only" : "provider");
            row.put("module", open ? p.get("lib") + "-" + kebab(simple(type)) : "-");
            out.add(row);
        }
        for (Map<String, String> f : read(q.resolve("FIRST_PARTY_ROWS.tsv"))) {
            Map<String, String> row = new LinkedHashMap<>(f);
            row.put("version", "-");
            row.put("kind", "-");
            row.put("module", "-");
            out.add(row);
        }
        List<String> order = List.of("jdk", "guava", "apache", "spring", "eclipse", "first-party");
        out.sort(Comparator.comparing((Map<String, String> r) -> order.indexOf(r.get("provider")))
                .thenComparing(r -> r.get("type")));
        write(q.resolve("INVENTORY.tsv"), INVENTORY_COLUMNS, out);
    }

    // ---------------------------------------------------------------- scaffold
    static void scaffold(Path q, Path impl) throws Exception {
        Map<String, Map<String, String>> providers = new HashMap<>();
        for (Map<String, String> p : read(q.resolve("PROVIDERS.tsv"))) { providers.putIfAbsent(p.get("lib"), p); }
        Map<String, Map<String, String>> plans = index(read(q.resolve("STORAGE_PLANS.tsv")), "family");
        Map<String, String> planByCode = new HashMap<>();
        for (Map<String, String> plan : plans.values()) {
            for (String code : plan.get("categories").split(",")) { planByCode.put(code, plan.get("family")); }
        }
        Path t = q.resolve("scaffold-template");
        String pom = Files.readString(t.resolve("module-pom.xml"));
        String readme = Files.readString(t.resolve("README.md"));
        String notice = Files.readString(t.resolve("NOTICE.txt"));
        String license = Files.readString(q.resolve("../../LICENSE"));
        List<Map<String, String>> inventory = read(q.resolve("INVENTORY.tsv"));
        List<Map<String, String>> surface = new ArrayList<>();
        Set<String> modules = new TreeSet<>();
        for (Map<String, String> row : inventory) {
            String status = row.get("owned_status");
            String lib = row.get("provider");
            if (!(status.equals("OPEN") || status.equals("SCAFFOLD")) || !Set.of("jdk", "guava", "apache", "spring").contains(lib)) {
                continue;
            }
            Map<String, String> p = providers.get(lib);
            String type = row.get("type");
            String familyName = simple(type).matches(DECORATOR) ? "decorator" : planByCode.get(family(row.get("categories")));
            Map<String, String> plan = plans.get(familyName);
            Class<?> donor = Class.forName(type, false, M3FiveProviderScaffold.class.getClassLoader());
            String className = "M3" + simple(type);
            Generator g = new Generator(donor, p.get("package"), className, type, plan);
            String source = g.source(p);
            Path module = impl.resolve(row.get("module"));
            Map<String, String> values = new LinkedHashMap<>();
            values.put("artifactId", "m3-scaffold-" + row.get("module"));
            values.put("type", type);
            values.put("className", className);
            values.put("package", p.get("package"));
            values.put("provider", p.get("provider"));
            values.put("version", p.get("version"));
            values.put("license", p.get("license"));
            values.put("family", familyName);
            values.put("plan", plan.get("plan"));
            values.put("lanes", Arrays.stream(plan.get("lanes").split(";")).map(l -> "`" + l + "`").collect(Collectors.joining(", ")));
            values.put("reuse", row.get("reuse_owner").equals("-") ? "existing M3SlotArrays/M3SlotMap lane mechanics" : row.get("reuse_owner"));
            values.put("dependencies", dependencies(lib, p));
            text(module.resolve("pom.xml"), fill(pom, values));
            text(module.resolve("README.md"), fill(readme, values));
            text(module.resolve("NOTICE.txt"), fill(notice, values));
            text(module.resolve("LICENSE.txt"), license);
            text(module.resolve("src/main/java/" + p.get("package").replace('.', '/') + "/" + className + ".java"), source);
            row.put("owned_status", "SCAFFOLD");
            row.put("evidence", "scaffold module m3/collections/implementations/" + row.get("module"));
            modules.add(row.get("module"));
            surface.add(g.surfaceRow(row.get("module")));
        }
        String aggregator = Files.readString(t.resolve("aggregator-pom.xml"));
        String moduleList = modules.stream().map(m -> "\n    <module>" + m + "</module>").collect(Collectors.joining());
        text(impl.resolve("pom-five-provider-scaffold.xml"),
                fill(aggregator, Map.of("modules", moduleList, "moduleCount", Integer.toString(modules.size()))));
        write(q.resolve("INVENTORY.tsv"), INVENTORY_COLUMNS, inventory);
        surface.sort(Comparator.comparing(r -> r.get("module")));
        write(q.resolve("SCAFFOLD_SURFACE.tsv"), Generator.SURFACE_COLUMNS, surface);
    }

    static String dependencies(String lib, Map<String, String> p) {
        if (lib.equals("jdk")) { return ""; }
        String[] gav = p.get("maven").split(":");
        String dep = "\n    <dependency><groupId>" + gav[0] + "</groupId><artifactId>" + gav[1] + "</artifactId><version>" + gav[2] + "</version></dependency>";
        if (lib.equals("spring")) {
            dep += "\n    <dependency><groupId>com.google.code.findbugs</groupId><artifactId>jsr305</artifactId><version>3.0.2</version><scope>provided</scope></dependency>";
        }
        return dep;
    }

    // ---------------------------------------------------------------- one class
    /** Renders one scaffold class from a provider type by reflection; no provider code is read or copied. */
    static final class Generator {
        static final List<String> SURFACE_COLUMNS = List.of("module", "type", "class", "interfaces", "constructors",
                "static_methods", "instance_methods", "skipped", "suppressed_lints", "deprecated_members");
        private final Class<?> donor;
        private final String pkg;
        private final String name;
        private final String type;
        private final Map<String, String> plan;
        private final TypeToken<?> token;
        private final Set<String> classVars = new TreeSet<>();
        private final List<String> skipped = new ArrayList<>();
        private final Set<String> lints = new TreeSet<>();
        private Map<String, String> rename = Map.of();
        private boolean raw;
        private boolean deprecatedUse;
        private boolean inaccessible;
        private int ctors;
        private int statics;
        private int instances;
        private int deprecated;
        private String interfaces = "";

        Generator(Class<?> donor, String pkg, String name, String type, Map<String, String> plan) {
            this.donor = donor;
            this.pkg = pkg;
            this.name = name;
            this.type = type;
            this.plan = plan;
            this.token = TypeToken.of(donor);
            for (TypeVariable<?> v : donor.getTypeParameters()) { classVars.add(v.getName()); }
        }

        Map<String, String> surfaceRow(String module) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("module", module);
            row.put("type", type);
            row.put("class", pkg + "." + name);
            row.put("interfaces", interfaces.isEmpty() ? "-" : interfaces);
            row.put("constructors", Integer.toString(ctors));
            row.put("static_methods", Integer.toString(statics));
            row.put("instance_methods", Integer.toString(instances));
            row.put("skipped", skipped.isEmpty() ? "-" : String.join(";", skipped));
            row.put("suppressed_lints", lints.isEmpty() ? "-" : String.join(",", lints));
            row.put("deprecated_members", Integer.toString(deprecated));
            return row;
        }

        String source(Map<String, String> provider) {
            StringBuilder body = new StringBuilder();
            body.append("    private static final String M3_SCAFFOLD_MESSAGE = \"M3 scaffold: ").append(type.replace('$', '.')).append(" not yet owned\";\n");
            boolean serializable = java.io.Serializable.class.isAssignableFrom(donor);
            if (serializable) { body.append("    @java.io.Serial\n    private static final long serialVersionUID = 0L;\n"); }
            body.append("\n    // Planned lanes (").append(plan.get("family")).append("): arrays, offsets and bit-mask state; no Entry/Node objects.\n");
            for (String lane : plan.get("lanes").split(";")) { body.append("    private transient ").append(lane).append(";\n"); }
            StringBuilder members = new StringBuilder();
            constructors(members);
            statics(members);
            instance(members);
            String header = header(serializable);
            StringBuilder out = new StringBuilder();
            out.append("/*\n * Copyright 2026 Hitesh Soliwal and contributors\n * SPDX-License-Identifier: Apache-2.0\n")
               .append(" * GENERATED by m3/collections/qualification/five-provider-inventory-20261018/M3FiveProviderScaffold.java;\n")
               .append(" * regenerate, do not hand-edit. Public surface mirrors ").append(type.replace('$', '.')).append(" from\n")
               .append(" * ").append(provider.get("provider")).append(' ').append(provider.get("version")).append(" (").append(provider.get("license"))
               .append(") by reflection; no provider implementation code is copied.\n */\n")
               .append("package ").append(pkg).append(";\n\n")
               .append(javadoc())
               .append(header).append(" {\n").append(body).append(members).append("}\n");
            return out.toString();
        }

        private String javadoc() {
            String lanes = Arrays.stream(plan.get("lanes").split(";")).map(l -> "{@code " + l + "}").collect(Collectors.joining(", "));
            return "/**\n * SCAFFOLD, not an owned implementation: declares the public surface of\n * {@code "
                    + type.replace('$', '.') + "} and every body throws {@link UnsupportedOperationException}.\n *\n"
                    + " * <p>Storage plan (" + plan.get("family") + "): " + plan.get("plan") + ".\n *\n"
                    + " * <p>Lanes: " + lanes + ". No Entry or Node object is stored per element; any\n"
                    + " * Entry-shaped value the provider API demands is a boundary flyweight created only at that call.\n *\n"
                    + " * <p>The class is final and has no provider implementation superclass or delegate; provider\n"
                    + " * interfaces are implemented directly. See INVENTORY.tsv and SCAFFOLD_SURFACE.tsv.\n"
                    + " */\n";
        }

        private String header(boolean serializable) {
            raw = false;
            deprecatedUse = false;
            rename = Map.of();
            List<String> types = new ArrayList<>();
            for (TypeToken<?> i : token.getTypes().interfaces()) {
                Class<?> r = i.getRawType();
                if (!accessible(r)) { continue; }
                types.add(ty(i.getType()));
            }
            types.sort(Comparator.naturalOrder());
            interfaces = String.join(";", types).replace(" ", "");
            String vars = vars(donor.getTypeParameters());
            StringBuilder h = new StringBuilder();
            Set<String> s = new TreeSet<>();
            if (raw) { s.add("rawtypes"); }
            if (deprecatedUse && !donor.isAnnotationPresent(Deprecated.class)) { s.add("deprecation"); }
            if (donor.isAnnotationPresent(Deprecated.class)) { h.append("@Deprecated\n"); }
            if (!s.isEmpty()) { h.append(suppress(s)).append('\n'); lints.addAll(s); }
            h.append("public final class ").append(name).append(vars);
            if (!types.isEmpty()) { h.append("\n        implements ").append(String.join(",\n                ", types)); }
            return h.toString();
        }

        private static String suppress(Set<String> s) {
            return "@SuppressWarnings({" + s.stream().map(x -> "\"" + x + "\"").collect(Collectors.joining(", ")) + "})";
        }

        // ------------------------------------------------------------ members
        private void constructors(StringBuilder out) {
            Constructor<?>[] cs = donor.getConstructors();
            Arrays.sort(cs, Comparator.comparing(Constructor::toGenericString));
            if (cs.length == 0) {
                out.append("\n    private ").append(name).append("() {\n        throw new UnsupportedOperationException(M3_SCAFFOLD_MESSAGE);\n    }\n");
                return;
            }
            for (Constructor<?> c : cs) {
                String sig = executable(c, null, true, false);
                if (sig == null) { continue; }
                out.append(sig);
                ctors++;
            }
        }

        private void statics(StringBuilder out) {
            List<Method> ms = Arrays.stream(donor.getDeclaredMethods())
                    .filter(m -> Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers()) && !m.isSynthetic())
                    .sorted(Comparator.comparing(Method::toGenericString)).toList();
            for (Method m : ms) {
                String sig = executable(m, m.getGenericReturnType(), false, true);
                if (sig == null) { continue; }
                out.append(sig);
                statics++;
            }
        }

        private void instance(StringBuilder out) {
            Map<String, Method> chosen = new TreeMap<>();
            for (Method m : donor.getMethods()) {
                int mod = m.getModifiers();
                if (Modifier.isStatic(mod) || m.isBridge() || m.isSynthetic()) { continue; }
                if (m.getDeclaringClass() == Object.class) { continue; }
                String key = m.getName() + Arrays.stream(m.getGenericParameterTypes())
                        .map(p -> TypeToken.of(resolve(p)).getRawType().getName()).collect(Collectors.joining(",", "(", ")"));
                chosen.merge(key, m, Generator::better);
            }
            boolean eq = chosen.containsKey("equals(java.lang.Object)");
            boolean hc = chosen.containsKey("hashCode()");
            if (eq != hc) { lints.add("overrides-pair"); }
            for (Method m : chosen.values()) {
                String sig = executable(m, resolve(m.getGenericReturnType()), false, false);
                if (sig == null) { continue; }
                out.append(sig);
                instances++;
            }
            if (eq && !hc) { out.append(objectMethod("public int hashCode()")); instances++; }
            if (hc && !eq) { out.append(objectMethod("public boolean equals(Object other)")); instances++; }
        }

        private static String objectMethod(String sig) {
            return "\n    " + sig + " {\n        throw new UnsupportedOperationException(M3_SCAFFOLD_MESSAGE);\n    }\n";
        }

        private static Method better(Method a, Method b) {
            Class<?> ra = a.getReturnType();
            Class<?> rb = b.getReturnType();
            if (ra != rb) { return ra.isAssignableFrom(rb) ? b : a; }
            boolean ia = a.getDeclaringClass().isInterface();
            boolean ib = b.getDeclaringClass().isInterface();
            if (ia != ib) { return ia ? b : a; }
            return a.getDeclaringClass().isAssignableFrom(b.getDeclaringClass()) ? b : a;
        }

        private Type resolve(Type t) {
            return token.resolveType(t).getType();
        }

        /** Renders one constructor or method; returns null (and records why) when a type is inaccessible. */
        private String executable(Executable e, Type returnType, boolean ctor, boolean isStatic) {
            raw = false;
            deprecatedUse = false;
            inaccessible = false;
            Map<String, String> names = new HashMap<>();
            for (TypeVariable<?> v : e.getTypeParameters()) {
                String n = v.getName();
                while (!isStatic && classVars.contains(n) || names.containsValue(n)) { n = n + "0"; }
                names.put(key(v), n);
            }
            rename = names;
            String vars = vars(e.getTypeParameters());
            Type[] params = e.getGenericParameterTypes();
            List<String> rendered = new ArrayList<>();
            boolean safeVarargs = false;
            for (int i = 0; i < params.length; i++) {
                Type p = isStatic || ctor ? params[i] : resolve(params[i]);
                String s = ty(p);
                if (e.isVarArgs() && i == params.length - 1) {
                    s = s.substring(0, s.length() - 2) + "...";
                    safeVarargs = !reifiable(p);
                }
                rendered.add(s + " arg" + i);
            }
            String ret = ctor ? null : ty(returnType);
            List<String> throwsList = new ArrayList<>();
            for (Type x : e.getGenericExceptionTypes()) { throwsList.add(ty(isStatic || ctor ? x : resolve(x))); }
            if (inaccessible) {
                skipped.add(e.getName() + "(" + params.length + "):inaccessible-type");
                rename = Map.of();
                return null;
            }
            boolean dep = e.isAnnotationPresent(Deprecated.class);
            if (!dep && !ctor && !isStatic && overridesDeprecated((Method) e)) { deprecatedUse = true; }
            Set<String> s = new TreeSet<>();
            if (raw) { s.add("rawtypes"); }
            if (deprecatedUse && !dep) { s.add("deprecation"); }
            lints.addAll(s);
            StringBuilder out = new StringBuilder("\n");
            if (dep) {
                Deprecated d = e.getAnnotation(Deprecated.class);
                out.append("    @Deprecated").append(d.forRemoval() ? "(forRemoval = true)" : "").append('\n');
                deprecated++;
            }
            if (safeVarargs) { out.append("    @SafeVarargs\n"); }
            if (!s.isEmpty()) { out.append("    ").append(suppress(s)).append('\n'); }
            out.append("    public ");
            if (isStatic) { out.append("static "); }
            if (safeVarargs && !ctor && !isStatic) { out.append("final "); }
            if (!vars.isEmpty()) { out.append(vars).append(' '); }
            out.append(ctor ? name : ret + " " + e.getName()).append('(').append(String.join(", ", rendered)).append(')');
            if (!throwsList.isEmpty()) { out.append(" throws ").append(String.join(", ", throwsList)); }
            out.append(" {\n        throw new UnsupportedOperationException(M3_SCAFFOLD_MESSAGE);\n    }\n");
            rename = Map.of();
            return out.toString();
        }

        /** JLS 4.7: a type is reifiable when no type argument is anything but an unbounded wildcard. */
        private static boolean reifiable(Type t) {
            if (t instanceof Class<?>) { return true; }
            if (t instanceof GenericArrayType g) { return reifiable(g.getGenericComponentType()); }
            if (t instanceof ParameterizedType p) {
                boolean args = Arrays.stream(p.getActualTypeArguments()).allMatch(a -> a instanceof WildcardType w
                        && w.getLowerBounds().length == 0 && w.getUpperBounds()[0] == Object.class);
                return args && (p.getOwnerType() == null || reifiable(p.getOwnerType()));
            }
            return false;
        }

        private boolean overridesDeprecated(Method m) {
            for (Class<?> i : token.getTypes().interfaces().rawTypes()) {
                try {
                    if (i.getMethod(m.getName(), m.getParameterTypes()).isAnnotationPresent(Deprecated.class)) { return true; }
                } catch (NoSuchMethodException ignored) {
                    // not declared by this interface
                }
            }
            return false;
        }

        private static String key(TypeVariable<?> v) {
            return v.getGenericDeclaration() + "#" + v.getName();
        }

        private String vars(TypeVariable<?>[] vs) {
            if (vs.length == 0) { return ""; }
            List<String> out = new ArrayList<>();
            for (TypeVariable<?> v : vs) {
                String n = rename.getOrDefault(key(v), v.getName());
                List<Type> bounds = Arrays.stream(v.getBounds()).filter(b -> b != Object.class).toList();
                out.add(bounds.isEmpty() ? n : n + " extends " + bounds.stream().map(this::ty).collect(Collectors.joining(" & ")));
            }
            return "<" + String.join(", ", out) + ">";
        }

        // ------------------------------------------------------------ type rendering
        private String ty(Type t) {
            if (t instanceof Class<?> c) {
                if (c.isArray()) { return ty(c.getComponentType()) + "[]"; }
                if (c.isPrimitive()) { return c.getName(); }
                if (c.getTypeParameters().length > 0) { raw = true; }
                return c == donor ? name : named(c);
            }
            if (t instanceof ParameterizedType p) {
                Class<?> r = (Class<?>) p.getRawType();
                String base = r == donor ? name
                        : p.getOwnerType() instanceof ParameterizedType o ? ty(o) + "." + r.getSimpleName() : named(r);
                if (r != donor && p.getOwnerType() instanceof ParameterizedType) { check(r); }
                return base + Arrays.stream(p.getActualTypeArguments()).map(this::ty).collect(Collectors.joining(", ", "<", ">"));
            }
            if (t instanceof WildcardType w) {
                if (w.getLowerBounds().length > 0) { return "? super " + ty(w.getLowerBounds()[0]); }
                Type u = w.getUpperBounds()[0];
                return u == Object.class ? "?" : "? extends " + ty(u);
            }
            if (t instanceof TypeVariable<?> v) { return rename.getOrDefault(key(v), v.getName()); }
            if (t instanceof GenericArrayType g) { return ty(g.getGenericComponentType()) + "[]"; }
            throw new IllegalStateException("unsupported type " + t);
        }

        private String named(Class<?> c) {
            check(c);
            return c.getPackageName().equals("java.lang") && c.getEnclosingClass() == null ? c.getSimpleName() : c.getCanonicalName();
        }

        private void check(Class<?> c) {
            if (!accessible(c)) { inaccessible = true; }
            for (Class<?> k = c; k != null; k = k.getEnclosingClass()) {
                if (k.isAnnotationPresent(Deprecated.class)) { deprecatedUse = true; }
            }
        }

        private static boolean accessible(Class<?> c) {
            for (Class<?> k = c; k != null; k = k.getEnclosingClass()) {
                if (!Modifier.isPublic(k.getModifiers())) { return false; }
            }
            return c.getCanonicalName() != null;
        }
    }
}
