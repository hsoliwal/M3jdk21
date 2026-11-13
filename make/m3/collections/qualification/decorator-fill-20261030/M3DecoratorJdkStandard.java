/*
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
 */

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * v1.4 JDK_STANDARD pass over one owned compilation unit, run after classification. Steps, each
 * on a fresh
 * javac parse (com.sun.source; no Java declaration is parsed by regex): PARAMETER_NAMES
 * (reflection names argN
 * become the names the API declares, taken from donor sources), IMPORTS (qualified type names
 * become explicit
 * single-type imports where the simple name is unambiguous), JAVADOC (type-parameter tags,
 * undocumented public
 * factories) and LAYOUT (100 columns, 8-space continuation). Only names, imports, comments and
 * line breaks
 * change, so the class files compiled with -g:none stay byte-identical to the previous version
 * (BCP).
 */
final class M3DecoratorJdkStandard {
    private static final Pattern REFLECTION_NAME = Pattern.compile("arg\\d+");
    private static final Set<String> ROOTS = Set.of("java", "javax", "jdk", "org", "com");
    private static final int WIDTH = 100;
    private static final javax.tools.JavaCompiler JAVAC =
            javax.tools.ToolProvider.getSystemJavaCompiler();
    private static final javax.tools.StandardJavaFileManager FILES =
            JAVAC.getStandardFileManager(
            null, null,
            StandardCharsets.UTF_8);
    private final Path commonsSources;
    private final Path jdkSources = Path.of(System.getProperty("java.home"), "lib", "src.zip");
    private final Map<String, Optional<com.sun.source.tree.CompilationUnitTree>> units =
            new HashMap<>();
    /**
     * Rows kind, class, member, detail: anything the pass could not bring to the standard
     * (fails the run).
     */
    final List<String> residue = new ArrayList<>();
    /** Rows class, member, renames, donor, tier: the evidence of every parameter rename. */
    final List<String> donorRows = new ArrayList<>();

    M3DecoratorJdkStandard(Path commonsSources) {
        this.commonsSources = commonsSources;
    }

    record Parsed(String text, com.sun.source.tree.CompilationUnitTree unit,
            com.sun.source.util.SourcePositions positions) {
        int start(com.sun.source.tree.Tree t) {
            return (int) positions.getStartPosition(unit, t);
        }

        int end(com.sun.source.tree.Tree t) {
            return (int) positions.getEndPosition(unit, t);
        }
    }

    record Edit(int start, int end, String text) {
    }

    record Match(List<String> names, String donor, String tier) {
    }

    static Parsed parse(String name, String text) {
        javax.tools.JavaFileObject file =
                new javax.tools.SimpleJavaFileObject(java.net.URI.create("string:///"
                + name.replace('.', '/') + ".java"), javax.tools.JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return text;
            }
        };
        List<String> errors = new ArrayList<>();
        com.sun.source.util.JavacTask task = (com.sun.source.util.JavacTask) JAVAC.getTask(null,
                FILES, d -> {
            if (d.getKind() == javax.tools.Diagnostic.Kind.ERROR) {
                errors.add(d.getLineNumber() + ": " + d.getMessage(java.util.Locale.ROOT));
            }
        }, List.of("-proc:none"), null, List.of(file));
        try {
            com.sun.source.tree.CompilationUnitTree unit = task.parse().iterator().next();
            if (!errors.isEmpty()) {
                throw new IllegalStateException("javac parse of " + name + ": "
                        + errors.get(0));
            }
            return new Parsed(text, unit,
                    com.sun.source.util.Trees.instance(task).getSourcePositions());
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    static String apply(String text, List<Edit> edits) {
        StringBuilder out = new StringBuilder(text);
        edits.stream().sorted(java.util.Comparator.comparingInt(Edit::start).reversed())
                .forEach(e -> out.replace(e.start, e.end, e.text));
        return out.toString();
    }

    /**
     * The whole pass; donors maps each class declared in the unit (top level and helpers) to
     * its Commons donor.
     */
    String standardize(String text, String top, Class<?> commons,
            Map<String, Class<?>> donors) {
        String named = parameters(text, top, donors);
        String imported = imports(named, top);
        String documented = javadoc(imported, top, commons);
        String laid = layout(documented);
        parse(top, laid);
        laid.lines().filter(l -> l.length() > WIDTH
                && !l.matches("(import|package) .*|.*https?://.*"))
                .forEach(l -> residue.add(String.join("\t", "LAYOUT", top, "-", l.strip())));
        parse(top, laid).unit().accept(new com.sun.source.util.TreeScanner<Void, Void>() {
            @Override
            public Void visitVariable(com.sun.source.tree.VariableTree v, Void unused) {
                if (REFLECTION_NAME.matcher(v.getName()).matches()) {
                    residue.add(
                            String.join("\t", "PARAMETER_NAME", top, v.getName().toString(),
                            "reflection name left"));
                }
                return super.visitVariable(v, unused);
            }
        }, null);
        return laid;
    }

    // ---- PARAMETER_NAMES
    // -------------------------------------------------------------------------------------

    private String parameters(String text, String top, Map<String, Class<?>> donors) {
        Parsed p = parse(top, text);
        List<Edit> edits = new ArrayList<>();
        java.util.Deque<com.sun.source.tree.ClassTree> classes = new java.util.ArrayDeque<>();
        p.unit().accept(new com.sun.source.util.TreeScanner<Void, Void>() {
            @Override
            public Void visitClass(com.sun.source.tree.ClassTree c, Void unused) {
                classes.push(c);
                super.visitClass(c, unused);
                classes.pop();
                return null;
            }

            @Override
            public Void visitMethod(com.sun.source.tree.MethodTree m, Void unused) {
                if (m.getParameters().stream().anyMatch(
                        v -> REFLECTION_NAME.matcher(v.getName()).matches())) {
                    rename(p, m, classes, donors, edits);
                }
                return super.visitMethod(m, unused);
            }
        }, null);
        return apply(text, edits);
    }

    /**
     * A donor parameter name in JDK form: donors are not always lowerCamel (Commons
     * PredicatedQueue declares
     * {@code Queue<E> Queue}), so a leading capital becomes lower case ({@code queue}).
     */
    static String jdkName(String donor) {
        return donor.isEmpty() || Character.isLowerCase(donor.charAt(0)) ? donor
                : Character.toLowerCase(donor.charAt(0)) + donor.substring(1);
    }

    private void rename(Parsed p, com.sun.source.tree.MethodTree m,
            java.util.Deque<com.sun.source.tree.ClassTree> classes,
            Map<String, Class<?>> donors, List<Edit> edits) {
        String owner = classes.peek().getSimpleName().toString();
        Set<String> typeVars = new java.util.HashSet<>();
        classes.forEach(
                c -> c.getTypeParameters().forEach(t -> typeVars.add(t.getName().toString())));
        m.getTypeParameters().forEach(t -> typeVars.add(t.getName().toString()));
        List<String> types = m.getParameters().stream().map(
                v -> erased(v.getType(), typeVars)).toList();
        boolean ctor = m.getName().contentEquals("<init>");
        String member = (ctor ? owner : m.getName().toString()) + "(" + String.join(", ", types)
                + ")";
        Optional<Match> match = Optional.ofNullable(donors.get(owner)).flatMap(
                d -> find(d, m.getName().toString(), types, ctor));
        if (match.isEmpty()) {
            residue.add(
                    String.join("\t", "PARAMETER_NAME", owner, member,
                    "no donor declares this member"));
            return;
        }
        Map<String, String> rename = new LinkedHashMap<>();
        List<String> names = match.get().names();
        java.util.stream.IntStream.range(0, names.size()).filter(
                i -> REFLECTION_NAME.matcher(m.getParameters().get(i).getName()).matches())
                .forEach(
                        i -> rename.put(m.getParameters().get(i).getName().toString(),
                        jdkName(names.get(i))));
        Set<String> kept = m.getParameters().stream().map(v -> v.getName().toString()).filter(
                n -> !rename.containsKey(n))
                .collect(Collectors.toSet());
        Set<String> used = bodyNames(m.getBody());
        Optional<String> clash = rename.values().stream().filter(n -> used.contains(n)
                || kept.contains(n)
                || java.util.Collections.frequency(rename.values(), n) > 1).findFirst();
        if (clash.isPresent()) {
            residue.add(
                    String.join("\t", "PARAMETER_NAME", owner, member, "donor name "
                    + clash.get()
                    + " would shadow or repeat a name the body reads"));
            return;
        }
        for (com.sun.source.tree.VariableTree v : m.getParameters()) {
            String old = v.getName().toString();
            int end = p.end(v);
            if (rename.containsKey(old) && p.text().startsWith(old, end - old.length())) {
                edits.add(new Edit(end - old.length(), end, rename.get(old)));
            }
        }
        if (m.getBody() != null) {
            m.getBody().accept(new com.sun.source.util.TreeScanner<Void, Void>() {
                @Override
                public Void visitIdentifier(com.sun.source.tree.IdentifierTree i, Void unused) {
                    String n = i.getName().toString();
                    if (rename.containsKey(n)) {
                        edits.add(new Edit(p.start(i), p.end(i), rename.get(n)));
                    }
                    return null;
                }
            }, null);
        }
        donorRows.add(
                String.join("\t", owner, member,
                rename.entrySet().stream().map(e -> e.getKey() + "->" + e.getValue())
                .collect(Collectors.joining(" ")), match.get().donor(), match.get().tier()));
    }

    /**
     * Every simple name the body reads or declares (identifiers, locals, lambda parameters).
     */
    private static Set<String> bodyNames(com.sun.source.tree.Tree body) {
        Set<String> names = new java.util.HashSet<>();
        if (body != null) {
            body.accept(new com.sun.source.util.TreeScanner<Void, Void>() {
                @Override
                public Void visitIdentifier(com.sun.source.tree.IdentifierTree i, Void unused) {
                    names.add(i.getName().toString());
                    return null;
                }

                @Override
                public Void visitVariable(com.sun.source.tree.VariableTree v, Void unused) {
                    names.add(v.getName().toString());
                    return super.visitVariable(v, unused);
                }
            }, null);
        }
        return names;
    }

    /** Erased simple type name; a type variable is {@code #}, an array adds {@code []}. */
    private static String erased(com.sun.source.tree.Tree t, Set<String> typeVars) {
        return switch (t) {
            case com.sun.source.tree.ArrayTypeTree a -> erased(a.getType(), typeVars) + "[]";
            case com.sun.source.tree.ParameterizedTypeTree pt -> erased(pt.getType(), typeVars);
            case com.sun.source.tree.AnnotatedTypeTree at -> erased(at.getUnderlyingType(),
                    typeVars);
            case com.sun.source.tree.MemberSelectTree ms -> ms.getIdentifier().toString();
            case com.sun.source.tree.IdentifierTree i -> typeVars.contains(
                    i.getName().toString()) ? "#" : i.getName().toString();
            default -> t.toString();
        };
    }

    /**
     * DONOR lookup: the decorated Commons class and its Commons supertypes first, then the JDK
     * types of the same
     * hierarchy (src.zip); a constructor only matches the donor class's own constructors. Tier
     * EXACT compares erased
     * simple parameter types (type variables equal), LOOSE also equates a type variable with
     * Object, ARITY (constructors
     * only) takes the unique constructor of that arity.
     */
    private Optional<Match> find(Class<?> donor, String name, List<String> types,
            boolean ctor) {
        List<Class<?>> order = ctor ? List.of(donor) : hierarchy(donor);
        for (String tier : List.of("EXACT", "LOOSE")) {
            for (Class<?> c : order) {
                Optional<List<String>> names = donorTree(c).flatMap(ct -> members(ct).stream()
                        .filter(m -> m.getName().contentEquals(name)
                                && m.getParameters().size() == types.size())
                        .filter(m -> same(types, donorTypes(ct, m), tier.equals("LOOSE")))
                        .map(m -> m.getParameters().stream().map(
                                v -> v.getName().toString()).toList()).findFirst());
                if (names.isPresent()) {
                    return Optional.of(new Match(names.get(), c.getName(), tier));
                }
            }
        }
        if (ctor) {
            List<com.sun.source.tree.MethodTree> arity = donorTree(donor).map(
                    ct -> members(ct).stream()
                    .filter(m -> m.getName().contentEquals("<init>")
                            && m.getParameters().size() == types.size()).toList())
                    .orElse(List.of());
            if (arity.size() == 1) {
                return Optional.of(
                        new Match(
                        arity.get(0).getParameters().stream().map(
                        v -> v.getName().toString()).toList(),
                        donor.getName(), "ARITY"));
            }
        }
        return Optional.empty();
    }

    private static List<com.sun.source.tree.MethodTree> members(
            com.sun.source.tree.ClassTree ct) {
        return ct.getMembers().stream().filter(com.sun.source.tree.MethodTree.class::isInstance)
                .map(com.sun.source.tree.MethodTree.class::cast).toList();
    }

    private static List<String> donorTypes(com.sun.source.tree.ClassTree ct,
            com.sun.source.tree.MethodTree m) {
        Set<String> typeVars = new java.util.HashSet<>();
        ct.getTypeParameters().forEach(t -> typeVars.add(t.getName().toString()));
        m.getTypeParameters().forEach(t -> typeVars.add(t.getName().toString()));
        return m.getParameters().stream().map(v -> erased(v.getType(), typeVars)).toList();
    }

    private static boolean same(List<String> a, List<String> b, boolean loose) {
        java.util.function.UnaryOperator<String> norm = s -> loose ? s.replace("#", "Object")
                : s;
        return a.stream().map(norm).toList().equals(b.stream().map(norm).toList());
    }

    /**
     * Superclass chain, then interfaces breadth first, then Object; Commons types before JDK
     * types.
     */
    private static List<Class<?>> hierarchy(Class<?> owner) {
        List<Class<?>> chain = new ArrayList<>();
        for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
            chain.add(c);
        }
        Set<Class<?>> seen = new LinkedHashSet<>(chain);
        java.util.Deque<Class<?>> queue = new java.util.ArrayDeque<>(chain);
        while (!queue.isEmpty()) {
            Arrays.stream(queue.poll().getInterfaces()).filter(seen::add).forEach(queue::add);
        }
        seen.add(Object.class);
        return java.util.stream.Stream.concat(
                seen.stream().filter(c -> c.getName().startsWith("org.apache.commons.")),
                seen.stream().filter(c -> c.getName().startsWith("java."))).toList();
    }

    private Optional<com.sun.source.tree.ClassTree> donorTree(Class<?> c) {
        List<String> path = new ArrayList<>();
        Class<?> top = c;
        for (Class<?> k = c; k != null; k = k.getEnclosingClass()) {
            path.add(0, k.getSimpleName());
            top = k;
        }
        Optional<com.sun.source.tree.CompilationUnitTree> unit = units.computeIfAbsent(
                top.getName(), this::donorUnit);
        Optional<com.sun.source.tree.ClassTree> found = unit.flatMap(
                u -> u.getTypeDecls().stream()
                .filter(com.sun.source.tree.ClassTree.class::isInstance).map(
                        com.sun.source.tree.ClassTree.class::cast)
                .filter(t -> t.getSimpleName().contentEquals(path.get(0))).findFirst());
        for (String nested : path.subList(1, path.size())) {
            found = found.flatMap(
                    t -> t.getMembers().stream().filter(
                    com.sun.source.tree.ClassTree.class::isInstance)
                    .map(com.sun.source.tree.ClassTree.class::cast).filter(
                            k -> k.getSimpleName().contentEquals(nested)).findFirst());
        }
        return found;
    }

    private Optional<com.sun.source.tree.CompilationUnitTree> donorUnit(String fqn) {
        String rel = fqn.replace('.', '/') + ".java";
        try {
            if (fqn.startsWith("org.apache.commons.")
                    && Files.exists(commonsSources.resolve(rel))) {
                return Optional.of(
                        parse(fqn,
                        Files.readString(commonsSources.resolve(rel),
                        StandardCharsets.UTF_8)).unit());
            }
            if (fqn.startsWith("java.") && Files.exists(jdkSources)) {
                try (java.util.zip.ZipFile zip =
                        new java.util.zip.ZipFile(jdkSources.toFile())) {
                    java.util.zip.ZipEntry entry = zip.getEntry("java.base/" + rel);
                    if (entry != null) {
                        return Optional.of(
                                parse(fqn,
                                new String(zip.getInputStream(entry).readAllBytes(),
                                StandardCharsets.UTF_8)).unit());
                    }
                }
            }
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        return Optional.empty();
    }

    // ---- IMPORTS
    // -----------------------------------------------------------------------------------------

    record Use(String type, int start, int end) {
    }

    private String imports(String text, String top) {
        Parsed p = parse(top, text);
        List<Use> uses = new ArrayList<>();
        Set<String> bare = new java.util.HashSet<>();
        Set<String> declared = new java.util.HashSet<>();
        p.unit().getTypeDecls().forEach(
                t -> t.accept(new com.sun.source.util.TreeScanner<Void, Void>() {
            @Override
            public Void visitMemberSelect(com.sun.source.tree.MemberSelectTree ms,
                    Void unused) {
                Optional<List<String>> chain = chain(ms);
                int k = chain.map(M3DecoratorJdkStandard::typeIndex).orElse(-1);
                if (k < 0) {
                    return super.visitMemberSelect(ms, unused);
                }
                String type = String.join(".", chain.get().subList(0, k + 1));
                int start = p.start(ms);
                if (text.startsWith(type, start)) {
                    uses.add(new Use(type, start, start + type.length()));
                }
                return null;
            }

            @Override
            public Void visitIdentifier(com.sun.source.tree.IdentifierTree i, Void unused) {
                bare.add(i.getName().toString());
                return null;
            }

            @Override
            public Void visitClass(com.sun.source.tree.ClassTree c, Void unused) {
                declared.add(c.getSimpleName().toString());
                c.getTypeParameters().forEach(tp -> declared.add(tp.getName().toString()));
                return super.visitClass(c, unused);
            }

            @Override
            public Void visitMethod(com.sun.source.tree.MethodTree m, Void unused) {
                m.getTypeParameters().forEach(tp -> declared.add(tp.getName().toString()));
                return super.visitMethod(m, unused);
            }
        }, null));
        String pkg = p.unit().getPackageName() == null ? ""
                : p.unit().getPackageName().toString();
        Map<String, Long> counts = uses.stream().collect(
                Collectors.groupingBy(Use::type, java.util.TreeMap::new,
                Collectors.counting()));
        Set<String> members = new java.util.HashSet<>();
        counts.keySet().forEach(
                t -> M3DecoratorFill.load(t).ifPresent(
                c -> Arrays.stream(c.getClasses()).forEach(
                m -> members.add(m.getSimpleName()))));
        Map<String, String> chosen = new java.util.TreeMap<>();
        counts.keySet().stream().collect(
                Collectors.groupingBy(M3DecoratorJdkStandard::simpleName, java.util.TreeMap::new,
                Collectors.toList()))
                .forEach(
                        (simple, types) -> types.stream().filter(
                        t -> usable(t, simple, pkg, bare, declared, members))
                        .max(java.util.Comparator.comparingLong((String t) -> counts.get(t))
                                .thenComparing(java.util.Comparator.<String>reverseOrder()))
                        .ifPresent(t -> chosen.put(simple, t)));
        List<Edit> edits = uses.stream().filter(
                u -> u.type.equals(chosen.get(simpleName(u.type))))
                .map(u -> new Edit(u.start, u.end, simpleName(u.type))).collect(
                        Collectors.toCollection(ArrayList::new));
        List<String> imports = chosen.values().stream().filter(
                t -> !packageOf(t).equals("java.lang") && !packageOf(t).equals(pkg))
                .sorted().map(t -> "import " + t + ";").toList();
        if (!imports.isEmpty()) {
            int semi = text.indexOf(';', p.start(p.unit().getPackage()));
            int eol = text.indexOf('\n', semi) + 1;
            edits.add(new Edit(eol, eol, "\n" + String.join("\n", imports) + "\n"));
        }
        return apply(text, edits);
    }

    private static Optional<List<String>> chain(com.sun.source.tree.ExpressionTree t) {
        if (t instanceof com.sun.source.tree.IdentifierTree i) {
            return Optional.of(new ArrayList<>(List.of(i.getName().toString())));
        }
        if (t instanceof com.sun.source.tree.MemberSelectTree ms) {
            return chain(ms.getExpression()).map(l -> {
                l.add(ms.getIdentifier().toString());
                return l;
            });
        }
        return Optional.empty();
    }

    /**
     * Index of the top-level type in a qualified chain: lowercase package segments, then an
     * UpperCamel name.
     */
    private static int typeIndex(List<String> chain) {
        if (!ROOTS.contains(chain.get(0))) {
            return -1;
        }
        for (int i = 1; i < chain.size(); i++) {
            if (Character.isUpperCase(chain.get(i).charAt(0))) {
                return i;
            }
            if (!chain.get(i).matches("[a-z][a-z0-9]*")) {
                return -1;
            }
        }
        return -1;
    }

    private static String simpleName(String type) {
        return type.substring(type.lastIndexOf('.') + 1);
    }

    private static String packageOf(String type) {
        return type.substring(0, type.lastIndexOf('.'));
    }

    /**
     * IMPORTS guard: a simple name is usable when nothing else in the unit already answers to
     * it (a declared type or
     * type variable, a bare identifier, a member type of a referenced type, a java.lang type)
     * unless it is that type.
     */
    private static boolean usable(String type, String simple, String pkg, Set<String> bare,
            Set<String> declared, Set<String> members) {
        boolean implicit = packageOf(type).equals("java.lang") || packageOf(type).equals(pkg);
        boolean javaLang = !packageOf(type).equals("java.lang")
                && M3DecoratorFill.load("java.lang." + simple).isPresent();
        return !declared.contains(simple) && !members.contains(simple) && !javaLang
                && (!bare.contains(simple) || implicit);
    }

    // ---- JAVADOC
    // -----------------------------------------------------------------------------------------

    private String javadoc(String text, String top, Class<?> commons) {
        Parsed p = parse(top, text);
        com.sun.source.tree.ClassTree cls = p.unit().getTypeDecls().stream().filter(
                com.sun.source.tree.ClassTree.class::isInstance)
                .map(com.sun.source.tree.ClassTree.class::cast).filter(
                        t -> t.getSimpleName().contentEquals(top)).findFirst().orElseThrow();
        Noun noun = Noun.of(commons);
        List<Edit> edits = new ArrayList<>();
        int start = p.start(cls);
        int close = text.lastIndexOf("*/", start);
        int open = close < 0 ? -1 : text.lastIndexOf("/**", close);
        if (open < 0 || !text.substring(close + 2, start).isBlank()) {
            residue.add(
                    String.join("\t", "JAVADOC", top, top,
                    "class has no documentation comment"));
        } else if (!cls.getTypeParameters().isEmpty()
                && !text.substring(open, close).contains("@param <")) {
            int lineStart = text.lastIndexOf('\n', close) + 1;
            List<String> tags = typeParamTags(
                    cls.getTypeParameters().stream().map(t -> t.getName().toString()).toList(),
                    noun, "this");
            edits.add(
                    new Edit(lineStart, lineStart, " *\n"
                    + tags.stream().map(t -> " * " + t + "\n").collect(Collectors.joining())));
        }
        for (com.sun.source.tree.MethodTree m : members(cls)) {
            Set<javax.lang.model.element.Modifier> flags = m.getModifiers().getFlags();
            boolean exported = flags.contains(javax.lang.model.element.Modifier.PUBLIC)
                    || flags.contains(javax.lang.model.element.Modifier.PROTECTED);
            boolean override = m.getModifiers().getAnnotations().stream().anyMatch(
                    a -> a.getAnnotationType().toString().equals("Override"));
            int at = p.start(m);
            boolean documented = text.substring(0, at).stripTrailing().endsWith("*/");
            if (exported && !override && !documented) {
                Optional<List<String>> doc = factoryDoc(p, cls, m, noun).or(
                        () -> inverseDoc(m, commons));
                if (doc.isEmpty()) {
                    residue.add(
                            String.join("\t", "JAVADOC", top, m.getName().toString(),
                            "no Javadoc template for this public member"));
                    continue;
                }
                int lineStart = text.lastIndexOf('\n', at) + 1;
                String indent = text.substring(lineStart, at);
                edits.add(new Edit(lineStart, lineStart, indent + "/**\n" + doc.get().stream()
                        .map(l -> l.isEmpty() ? indent + " *\n" : indent + " * " + l
                                + "\n").collect(Collectors.joining()) + indent + " */\n"));
            }
        }
        return apply(text, edits);
    }

    /**
     * Collection family of the decorated Commons class: wording of type parameters and factory
     * documentation.
     */
    record Noun(String name, String family) {
        static Noun of(Class<?> commons) {
            String rest = commons.getSimpleName().replaceFirst(
                    "^(Predicated|Synchronized|Unmodifiable|Transformed|FixedSize|Lazy)", "");
            String name = rest.equals("MultiSet") ? "multiset"
                    : rest.replaceAll("(?<=[a-z])(?=[A-Z])", " ").toLowerCase(
                    java.util.Locale.ROOT)
                    .replace("multi set", "multiset");
            return new Noun(name, name.substring(name.lastIndexOf(' ') + 1));
        }
    }

    /** java.util wording: owner "this" for the class, "the" for a factory. */
    private static List<String> typeParamTags(List<String> names, Noun noun, String owner) {
        if (noun.family().equals("map") && names.size() == 2) {
            return List.of("@param <" + names.get(0) + "> the type of keys maintained by "
                    + owner + " map",
                    "@param <" + names.get(1) + "> the type of mapped values");
        }
        String where = switch (noun.family()) {
            case "set" -> "maintained by";
            case "queue" -> "held in";
            default -> "in";
        };
        String what = owner.equals("this") ? noun.family() : noun.name();
        return names.stream().map(n -> "@param <" + n + "> the type of elements " + where + " "
                + owner + " " + what).toList();
    }

    /**
     * FACTORY documentation: summary, type parameter tags, one tag per parameter, the return
     * value and only the
     * exceptions the emitted constructor throws (requireNonNull -> NullPointerException,
     * validation of the decorated
     * elements -> IllegalArgumentException).
     */
    /**
     * v1.6 INVERSE_CACHE documentation: a public no-argument {@code inverse*} instance method
     * that is not an override (UnmodifiableOrderedBidiMap.inverseOrderedBidiMap).
     */
    private static Optional<List<String>> inverseDoc(com.sun.source.tree.MethodTree m,
            Class<?> commons) {
        if (!m.getName().toString().startsWith("inverse") || !m.getParameters().isEmpty()
                || m.getModifiers().getFlags().contains(
                        javax.lang.model.element.Modifier.STATIC)) {
            return Optional.empty();
        }
        String unmodifiable = org.apache.commons.collections4.Unmodifiable.class
                .isAssignableFrom(commons) ? " unmodifiable" : "";
        return Optional.of(List.of(
                "Returns the" + unmodifiable + " view of this map with its keys and values",
                "reversed. The view is created on the first call and kept.", "",
                "@return the inverse view of this map"));
    }

    private Optional<List<String>> factoryDoc(Parsed p, com.sun.source.tree.ClassTree cls,
            com.sun.source.tree.MethodTree m, Noun noun) {
        String name = m.getName().toString();
        String kind = name.replaceFirst(
                "^(predicated|synchronized|unmodifiable|transformed|transforming).*", "$1");
        List<String> params = m.getParameters().stream().map(
                v -> v.getName().toString()).toList();
        if (!m.getModifiers().getFlags().contains(javax.lang.model.element.Modifier.STATIC)
                || kind.equals(name) || params.isEmpty()) {
            return Optional.empty();
        }
        List<com.sun.source.tree.NewClassTree> created = new ArrayList<>();
        boolean[] passThrough = {false};
        m.getBody().accept(new com.sun.source.util.TreeScanner<Void, Void>() {
            @Override
            public Void visitNewClass(com.sun.source.tree.NewClassTree n, Void unused) {
                created.add(n);
                return super.visitNewClass(n, unused);
            }

            @Override
            public Void visitInstanceOf(com.sun.source.tree.InstanceOfTree i, Void unused) {
                passThrough[0] = true;
                return super.visitInstanceOf(i, unused);
            }
        }, null);
        if (created.size() != 1) {
            return Optional.empty();
        }
        List<String> passed = created.get(0).getArguments().stream().map(
                Object::toString).toList();
        Optional<com.sun.source.tree.MethodTree> ctor = members(cls).stream()
                .filter(c -> c.getName().contentEquals("<init>")
                        && c.getParameters().size() == passed.size()).findFirst();
        if (ctor.isEmpty() || !params.containsAll(passed)) {
            return Optional.empty();
        }
        Ctor facts = Ctor.of(ctor.get());
        List<String> ctorParams = ctor.get().getParameters().stream().map(
                v -> v.getName().toString()).toList();
        java.util.function.Function<String, String> toCtor = f -> ctorParams.get(
                passed.indexOf(f));
        String first = params.get(0);
        List<String> doc = new ArrayList<>();
        boolean map = noun.family().equals("map");
        switch (kind) {
            case "predicated" -> doc.add("Returns a predicated " + noun.name()
                    + " backed by the specified " + noun.name() + "; "
                    + (map ? "keys and values put" : "elements added")
                            + " through the returned " + noun.name()
                    + " are validated first.");
            case "synchronized" -> doc.add("Returns a synchronized (thread-safe) " + noun.name()
                    + " backed by the specified "
                    + noun.name() + ".");
            case "transformed" -> doc.add("Returns a transformed " + noun.name()
                    + " backed by the specified " + noun.name() + "; the "
                    + (map ? "keys and values" : "elements") + " it already contains are"
                    + " transformed in place, and those " + (map ? "put" : "added")
                    + " through the returned " + noun.name() + " are transformed first.");
            case "transforming" -> doc.add("Returns a transforming " + noun.name()
                    + " backed by the specified " + noun.name() + "; "
                    + (map ? "keys and values put" : "elements added") + " through the"
                    + " returned " + noun.name() + " are transformed first, and those it"
                    + " already contains are left as they are.");
            default -> doc.add("Returns an unmodifiable view of the specified " + noun.name()
                    + ".");
        }
        doc.add("");
        doc.addAll(
                typeParamTags(
                m.getTypeParameters().stream().map(t -> t.getName().toString()).toList(), noun,
                "the"));
        for (String param : params) {
            if (param.equals(first)) {
                doc.add("@param " + param + " the " + noun.name()
                        + (kind.equals("unmodifiable") ? " to wrap in an unmodifiable view"
                        : " to decorate"));
                continue;
            }
            String lane = facts.lanes().getOrDefault(toCtor.apply(param), "");
            String typeText = m.getParameters().get(params.indexOf(param)).getType().toString();
            String what = lane.startsWith("key") ? "key" : lane.startsWith("value") ? "value"
                    : "element";
            if (typeText.contains("Transformer") && !lane.isEmpty()) {
                // a transformer lane may be null only where the constructor does not check it
                boolean nullable = !facts.nonNull().contains(toCtor.apply(param));
                doc.add("@param " + param + " the transformer applied to each " + what
                        + (nullable ? ", or {@code null} to leave the " + what + "s as they are"
                        : ""));
                continue;
            }
            if (!typeText.contains("Predicate") || lane.isEmpty()) {
                return Optional.empty();
            }
            boolean nullable = !facts.nonNull().contains(toCtor.apply(param))
                    && p.text().contains(lane + " != null && !" + lane + ".test(");
            doc.add("@param " + param + " the predicate that each " + what + " must satisfy"
                    + (nullable ? ", or {@code null} to accept every " + what : ""));
        }
        doc.add(switch (kind) {
            case "predicated" -> "@return a new predicated " + noun.name()
                    + " backed by {@code " + first + "}";
            case "synchronized" -> "@return a new synchronized " + noun.name()
                    + " backed by {@code " + first + "}";
            case "transformed" -> "@return a new transformed " + noun.name()
                    + " backed by {@code " + first + "}";
            case "transforming" -> "@return a new transforming " + noun.name()
                    + " backed by {@code " + first + "}";
            default -> passThrough[0] ? "@return an unmodifiable view of {@code " + first
                    + "}, or {@code " + first
                    + "} itself if it is already unmodifiable"
                            : "@return a new unmodifiable view of {@code " + first + "}";
        });
        List<String> nulls = params.stream().filter(x -> passed.contains(x)
                && facts.nonNull().contains(toCtor.apply(x)))
                .map(x -> "{@code " + x + "}").toList();
        if (!nulls.isEmpty()) {
            doc.add("@throws NullPointerException if "
                    + (nulls.size() == 1 ? nulls.get(0) + " is null"
                    : nulls.size() == 2 ? nulls.get(0) + " or " + nulls.get(1) + " is null"
                    : "any of " + String.join(", ", nulls.subList(0, nulls.size() - 1)) + " or "
                            + nulls.get(nulls.size() - 1) + " is null"));
        }
        params.stream().filter(x -> passed.contains(x)
                && facts.validated().contains(toCtor.apply(x))).findFirst()
                .ifPresent(
                        x -> doc.add("@throws IllegalArgumentException if {@code " + x
                        + "} already contains "
                        + (map ? "a key or value that a predicate rejects"
                                : "an element that the predicate rejects")));
        return Optional.of(doc);
    }

    /**
     * Constructor facts read from its statements: null-checked parameters, validated
     * parameters, parameter -> lane.
     */
    record Ctor(Set<String> nonNull, Set<String> validated, Map<String, String> lanes) {
        static Ctor of(com.sun.source.tree.MethodTree ctor) {
            Set<String> nonNull = new java.util.HashSet<>();
            Set<String> validated = new java.util.HashSet<>();
            Map<String, String> lanes = new HashMap<>();
            for (com.sun.source.tree.StatementTree s : ctor.getBody().getStatements()) {
                if (s instanceof com.sun.source.tree.EnhancedForLoopTree f
                        && f.getExpression() instanceof com.sun.source.tree.IdentifierTree id) {
                    validated.add(id.getName().toString());
                }
                if (!(s instanceof com.sun.source.tree.ExpressionStatementTree es)) {
                    continue;
                }
                if (es.getExpression() instanceof com.sun.source.tree.MethodInvocationTree mi
                        && mi.getMethodSelect()
                                instanceof com.sun.source.tree.MemberSelectTree ms
                                && ms.getIdentifier().contentEquals("forEach")
                        && ms.getExpression()
                                instanceof com.sun.source.tree.IdentifierTree id) {
                    validated.add(id.getName().toString());
                }
                if (es.getExpression() instanceof com.sun.source.tree.AssignmentTree as
                        && as.getVariable()
                                instanceof com.sun.source.tree.MemberSelectTree field) {
                    com.sun.source.tree.ExpressionTree rhs = as.getExpression()
                            instanceof com.sun.source.tree.TypeCastTree tc
                            ? tc.getExpression() : as.getExpression();
                    if (rhs instanceof com.sun.source.tree.MethodInvocationTree mi
                            && mi.getMethodSelect().toString().endsWith("requireNonNull")
                            && mi.getArguments().get(
                                    0) instanceof com.sun.source.tree.IdentifierTree id) {
                        nonNull.add(id.getName().toString());
                        lanes.put(id.getName().toString(), field.getIdentifier().toString());
                    } else if (rhs instanceof com.sun.source.tree.IdentifierTree id) {
                        lanes.put(id.getName().toString(), field.getIdentifier().toString());
                    }
                }
            }
            return new Ctor(nonNull, validated, lanes);
        }
    }

    // ---- LAYOUT
    // -----------------------------------------------------------------------------------------

    /**
     * LAYOUT: the Eclipse-style banner becomes a plain block comment (javac reads any comment
     * opening with two
     * stars as a documentation comment, and one before {@code package} is not expected there);
     * comment lines are
     * word-wrapped with their own prefix; code lines are broken after a comma or before a
     * binary operator with an
     * 8-space continuation indent. Breaks are never placed inside a string or character
     * literal.
     */
    static String layout(String text) {
        String unbanner = text.replaceFirst("\\A/\\*{3,}\\n", "/*\n").replaceFirst(
                "(?m)^ \\*{3,}/$", " */");
        List<String> out = new ArrayList<>();
        boolean inBlock = false;
        boolean inTextBlock = false;
        for (String line : unbanner.split("\n", -1)) {
            // a text block is a string literal: its lines pass through byte for byte
            boolean opensOrCloses = (line.split("\"\"\"", -1).length - 1) % 2 == 1;
            if (inTextBlock || opensOrCloses) {
                out.add(line);
                inTextBlock = inTextBlock != opensOrCloses;
                continue;
            }
            String l = line.stripTrailing();
            String trimmed = l.strip();
            int indent = l.length() - l.stripLeading().length();
            String pad = " ".repeat(indent);
            if (!inBlock && trimmed.startsWith("/**") && trimmed.endsWith("*/")
                    && l.length() > WIDTH) {
                out.add(pad + "/**");
                out.addAll(
                        words(pad + " * ", trimmed.substring(3, trimmed.length() - 2).strip()));
                out.add(pad + " */");
            } else if (inBlock && trimmed.startsWith("*") && !trimmed.startsWith("*/")
                    && l.length() > WIDTH) {
                String body = trimmed.substring(1).strip();
                out.addAll(words(pad + "* ", body));
            } else if (!inBlock && trimmed.startsWith("//") && l.length() > WIDTH) {
                out.addAll(words(pad + "// ", trimmed.substring(2).strip()));
            } else if (!inBlock && l.length() > WIDTH && !l.matches("(import|package) .*")) {
                out.addAll(code(l, indent));
            } else {
                out.add(l);
            }
            if (!inBlock && trimmed.startsWith("/*") && !trimmed.substring(2).contains("*/")) {
                inBlock = true;
            } else if (inBlock && trimmed.contains("*/")) {
                inBlock = false;
            }
        }
        return String.join("\n", out);
    }

    static List<String> words(String prefix, String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder(prefix);
        for (String word : text.split(" +")) {
            if (line.length() > prefix.length() && line.length() + 1 + word.length() > WIDTH) {
                lines.add(line.toString());
                line = new StringBuilder(prefix);
            }
            line.append(line.length() > prefix.length() ? " " : "").append(word);
        }
        lines.add(line.toString());
        return lines;
    }

    private static List<String> code(String line, int indent) {
        List<String> lines = new ArrayList<>();
        String pad = " ".repeat(indent + 8);
        String rest = line;
        while (rest.length() > WIDTH) {
            String current = rest;
            int floor = current.length() - current.stripLeading().length() + 8;
            List<int[]> breaks = breaks(current).stream().filter(b -> b[0] > floor).toList();
            // JDK style: break at the outermost nesting level that fits, then as far right as
            // possible
            Optional<int[]> fit = breaks.stream()
                    .filter(b -> current.substring(0, b[0]).stripTrailing().length() <= WIDTH)
                    .min(java.util.Comparator.<int[]>comparingInt(b -> b[1]).thenComparingInt(
                            b -> -b[0]));
            if (fit.isPresent()) {
                lines.add(current.substring(0, fit.get()[0]).stripTrailing());
                rest = pad + current.substring(fit.get()[0]).strip();
                continue;
            }
            // a long string literal splits after a space: "abc " + "def" is the same
            // compile-time constant
            Optional<Integer> split = literalBreaks(current).stream()
                    .filter(k -> k > floor && k + 3 <= WIDTH).max(Integer::compare);
            Optional<Integer> cut = breaks.stream().map(b -> b[0]).min(Integer::compare);
            if (split.isPresent()) {
                lines.add(current.substring(0, split.get()) + "\" +");
                rest = pad + "\"" + current.substring(split.get());
            } else if (cut.isPresent()) {
                lines.add(current.substring(0, cut.get()).stripTrailing());
                rest = pad + current.substring(cut.get()).strip();
            } else {
                break;
            }
        }
        lines.add(rest);
        return lines;
    }

    /**
     * Offsets just after a space inside a double-quoted string literal of one code line (never
     * after an escape).
     */
    private static List<Integer> literalBreaks(String line) {
        List<Integer> splits = new ArrayList<>();
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                } else if (quote == '"' && c == ' ') {
                    splits.add(i + 1);
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                break;
            }
        }
        return splits;
    }

    /**
     * Break offsets of one code line with their parenthesis depth: after ", ", before " ? ", "
     * : ", " && ", " || ",
     * " + ", " new ", and after the "(" of a call or constructor argument list; never inside a
     * literal, a type
     * argument list or a trailing line comment.
     */
    private static List<int[]> breaks(String line) {
        List<int[]> breaks = new ArrayList<>();
        int angle = 0;
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                break;
            } else if (c == '<' && i + 1 < line.length() && line.charAt(i + 1) != ' '
                    && line.charAt(i + 1) != '=') {
                angle++;
            } else if (c == '>' && i > 0 && line.charAt(i - 1) != ' '
                    && line.charAt(i - 1) != '-' && angle > 0) {
                angle--;
            } else if (c == '(') {
                if (angle == 0 && i > 0 && i + 1 < line.length() && line.charAt(i + 1) != ')'
                        && (Character.isJavaIdentifierPart(line.charAt(i - 1))
                                || line.charAt(i - 1) == '>')) {
                    breaks.add(new int[] {i + 1, depth + 1});
                }
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (angle == 0 && c == ',' && i + 1 < line.length()
                    && line.charAt(i + 1) == ' ') {
                breaks.add(new int[] {i + 2, depth});
            } else if (angle == 0 && c == ' '
                    && (line.startsWith(" ? ", i) || line.startsWith(" : ", i)
                    || line.startsWith(" && ", i) || line.startsWith(" || ", i)
                    || line.startsWith(" + ", i) || line.startsWith(" new ", i)
                    || line.startsWith(" instanceof ", i))) {
                breaks.add(new int[] {i + 1, depth});
            } else if (angle == 0 && c == ' ' && line.startsWith(" = ", i)) {
                // after the "=" of an assignment or initializer
                breaks.add(new int[] {i + 3, depth});
            }
        }
        return breaks;
    }
}
