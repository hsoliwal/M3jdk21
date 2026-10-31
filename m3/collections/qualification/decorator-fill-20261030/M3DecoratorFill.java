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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Pattern DECORATOR_FILL v1.3 (TmOP generator): turns one five-provider decorator scaffold into an
 * owned decorator.
 * Every scaffold method is one atom. The atom is classified against Apache Commons Collections
 * 4.5.0, which must be
 * on the class path, together with its unpacked sources:
 * <ul>
 * <li>{@code OBJECT}: the declaring class is {@code Object}, so the method is dropped and the
 * inherited one is kept.
 * <li>{@code DEFAULT}: the declaring class is an interface, so {@code Iface.super.m(..)} is
 * emitted. The same default chain runs.
 * <li>{@code THROW}, {@code DELEGATE}, {@code EQUALS}, {@code WRAP} or {@code FACTORY}: the method
 * body is read from the
 * declaring Commons class and classified by its shape. A template is emitted; no provider body is
 * copied.
 * <li>v1.3 LANES (policy decorators such as Predicated*): {@code LANES_CONSTRUCTOR}, {@code
 * GUARD_DELEGATE},
 * {@code LANE_VIEW}, {@code SELF_CALL}, {@code INNER_VIEW}, {@code MAP_ITERATOR_ADAPTER}, {@code
 * CHECKED_ENTRY_SET} and
 * the lane members {@code VALIDATE}, {@code CHECK_SET_VALUE}, {@code SERIAL_HOOK}; see PATTERNS.tsv
 * for each side condition.
 * <li>{@code MANUAL}: any other shape. The class is not written and the atom is reported, so a gap
 * is never guessed.
 * </ul>
 * Usage: {@code java -cp commons-collections4-4.5.0.jar M3DecoratorFill.java <sources dir>
 * <scaffold.java> <commons fqn>
 * <out.java> <atoms.tsv>}. Exit 0 = written, 3 = MANUAL atoms reported. The atoms ledger is
 * appended.
 * {@code --rule-check <sources dir> <out.tsv>} runs the classifier side-condition checks (exit 0 =
 * all PASS, 4 = FAIL).
 */
public final class M3DecoratorFill {
    private static final Logger LOG = Logger.getLogger(M3DecoratorFill.class.getName());
    private static final String UNMOD = "org.apache.commons.collections4.Unmodifiable";
    private static final String GENERATOR =
            "m3/collections/qualification/decorator-fill-20261030/M3DecoratorFill.java";
    private static final Pattern METHOD = Pattern.compile(
            "(?m)^    (public|private) (.+) \\{\\R        throw new " +
                    "UnsupportedOperationException\\(M3_SCAFFOLD_MESSAGE\\);\\R    }\\R");
    /**
     * WRAP table: Commons wrapper factory -> owner expression; %s is the delegated call, %b the
     * backing.
     */
    private static final Map<String, String> WRAP = Map.of(
            "UnmodifiableIterator.unmodifiableIterator:iterator",
                    "java.util.Collections.unmodifiableCollection(%b).iterator()",
            "UnmodifiableIterator.unmodifiableIterator:descendingIterator",
            "java.util.Collections.unmodifiableNavigableSet(%b).descendingIterator()",
            "UnmodifiableListIterator.unmodifiableListIterator:listIterator",
                    "java.util.Collections.unmodifiableList(%b).%c",
            "UnmodifiableSortedSet.unmodifiableSortedSet",
                    "java.util.Collections.unmodifiableSortedSet(%s)",
            "UnmodifiableNavigableSet.unmodifiableNavigableSet",
                    "java.util.Collections.unmodifiableNavigableSet(%s)",
            "UnmodifiableSet.unmodifiableSet", "java.util.Collections.unmodifiableSet(%s)",
            "UnmodifiableList.unmodifiableList", "java.util.Collections.unmodifiableList(%s)");
    /** SFA: a Java string literal (data, never rewritten). */
    private static final String LIT = "\"(?:[^\"\\\\]|\\\\.)*\"";
    private static final Pattern LITERAL = Pattern.compile(LIT);
    /**
     * LANES_CONSTRUCTOR statement shapes s1..s4 (normalized Commons constructor text, super(..)
     * chain flattened).
     */
    private static final Pattern CTOR_STMT = Pattern.compile(
            "this\\.(\\w+) = Objects\\.requireNonNull\\((\\w+), (" + LIT + ")\\);"
            + "|this\\.(\\w+) = (\\w+);|for \\(final (\\w+) (\\w+) : (\\w+)\\) \\{ " +
                    "validate\\(\\7\\); \\}|(\\w+)\\.forEach\\(this::validate\\);");
    /**
     * GUARD_DELEGATE guard forms: elem, entry, each, each-entry; group 9 is the rest of the body.
     */
    private static final Pattern VALIDATE_GUARD = Pattern.compile(
            "^(?:validate\\((\\w+)\\);|validate\\((\\w+), (\\w+)\\);"
            + "|for \\(final (\\w+) (\\w+) : (\\w+)\\) \\{ validate\\(\\5\\); \\}"
            + "|for \\(final Map\\.Entry<\\? extends K, \\? extends V> (\\w+) : " +
                    "(\\w+)\\.entrySet\\(\\)\\) \\{ "
            + "validate\\(\\7\\.getKey\\(\\), \\7\\.getValue\\(\\)\\); \\}) (.+)$");
    /**
     * LANE_CHECK statement of a lane member: if ([L != null && ]!L.test(p)) { throw new
     * IllegalArgumentException(MSG); }.
     */
    private static final Pattern LANE_CHECK = Pattern.compile(
            "if \\((?:(\\w+) != null && )?!(\\w+)\\.test\\((\\w+)\\)\\) \\{ "
            + "throw new IllegalArgumentException\\(((?:" + LIT + "|\\w+)(?: \\+ (?:" + LIT
                    + "|\\w+))*)\\); \\}");
    private static final Pattern CHECKED_ENTRY_SET = Pattern.compile(
            "^if \\(isSetValueChecking\\(\\)\\) \\{ return new (\\w+)"
            + "\\(decorated\\(\\)\\.entrySet\\(\\), this\\); \\} return " +
                    "decorated\\(\\)\\.entrySet\\(\\);$");

    record Atom(String visibility, String signature, String name, List<String[]> params,
            boolean isStatic, String returnType) {
    }

    record Emit(String category, String body) {
    }

    private final Path sources;
    private final Class<?> commons;
    private final String m3Simple;
    private final List<String> directInterfaces;
    private final List<String> ledger = new ArrayList<>();
    private String factoryParamType;
    /**
     * SYNC lane: monitor expression; "this" for the owned class (Commons lock == this), "lock"
     * inside the view helper.
     */
    private final String lockExpr;
    /**
     * VIEW: the Commons lock-sharing Set view class (SynchronizedBag$SynchronizedBagSet,
     * SynchronizedMultiSet$SynchronizedSet).
     */
    private Class<?> syncView;
    private final java.util.Set<String> accessors = new java.util.HashSet<>();
    /**
     * v1.3 ALIAS: Commons backing fields (collection, map, iterator) that decorated()-style
     * accessors return.
     */
    private final Set<String> aliases = new LinkedHashSet<>();
    /**
     * v1.3 LANES: enclosing generator of an inner helper (its lane members live there), else null.
     */
    private final M3DecoratorFill outer;
    /**
     * v1.3 LANES: arity of the self factory {@code new Self<>(p0, p1..)}; 0 = single-backing
     * constructor (v1.1/v1.2).
     */
    private int laneArity;
    private final List<String> ctorStatements = new ArrayList<>();
    /**
     * Policy lane field -> M3 type, in constructor order; laneOrder[i - 1] is the lane fed by
     * constructor parameter i.
     */
    private final Map<String, String> laneTypes = new LinkedHashMap<>();
    private final List<String> laneOrder = new ArrayList<>();
    private String ctorParams = "";
    private String backingDecl = "";
    private String ctorError;
    private boolean serialHook;
    private Set<String> surface = Set.of();
    private final Map<String, String> members = new LinkedHashMap<>();
    private final Map<String, String> helpers = new LinkedHashMap<>();
    /**
     * v1.4 JDK_STANDARD: each class this generator emits (simple name) -> its Commons donor of
     * parameter names.
     */
    private final Map<String, Class<?>> donors = new java.util.TreeMap<>();
    private Class<?> declaringNow;
    private final Map<String, String> sourceCache = new HashMap<>();

    private M3DecoratorFill(Path sources, Class<?> commons, String scaffold) throws IOException {
        this(sources, commons, scaffold, "this");
    }

    private M3DecoratorFill(Path sources, Class<?> commons, String scaffold,
            String lockExpr) throws IOException {
        this(sources, commons, scaffold, lockExpr, null);
    }

    private M3DecoratorFill(Path sources, Class<?> commons, String scaffold, String lockExpr,
            M3DecoratorFill outer) throws IOException {
        this.lockExpr = lockExpr;
        this.sources = sources;
        this.commons = commons;
        this.outer = outer;
        accessors();
        this.m3Simple = find(scaffold, "public final class (\\w+)").orElseThrow();
        donors.put(m3Simple, commons);
        this.directInterfaces = Arrays.stream(
                find(scaffold, "(?s)implements(.+?)\\{").orElseThrow().split(",\\s*(?![^<]*>)"))
                .map(s -> s.trim().replaceAll("<.*", "")).filter(s -> !s.isEmpty()).toList();
    }

    /**
     * Fills one owned decorator class from its scaffold, or runs the classifier rule checks.
     *
     * @param args {@code <commons sources> <scaffold> <commons class> <owned output> <atoms
     * ledger>}, or
     *        {@code --rule-check <commons sources> <rules ledger>}
     * @throws IOException if a source or ledger cannot be read or written
     * @throws ReflectiveOperationException if a Commons class or member cannot be loaded
     */
    public static void main(String[] args) throws IOException, ReflectiveOperationException {
        if (args[0].equals("--rule-check")) {
            System.exit(RuleCheck.run(Path.of(args[1]), Path.of(args[2])));
        }
        String scaffold = Files.readString(Path.of(args[1]), StandardCharsets.UTF_8);
        M3DecoratorFill fill =
                new M3DecoratorFill(Path.of(args[0]), Class.forName(args[2]), scaffold);
        String owned = fill.fill(scaffold);
        Files.write(Path.of(args[4]), fill.ledger, StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
        long manual = fill.ledger.stream().filter(r -> r.contains("\tMANUAL\t")).count();
        LOG.info(() -> "M3_DECORATOR_FILL class=" + fill.m3Simple + " atoms=" + fill.ledger.size()
                + " manual=" + manual);
        if (manual > 0) {
            System.exit(3);
        }
        JdkStandard standard = new JdkStandard(Path.of(args[0]));
        String text = standard.standardize(owned, fill.m3Simple, fill.commons, fill.donors);
        Path ledgers = Path.of(args[4]).toAbsolutePath().getParent();
        Files.write(ledgers.resolve("param-donors.tsv"),
                standard.donorRows.stream().map(r -> fill.m3Simple + "\t" + r).toList(),
                StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
        Files.write(ledgers.resolve("jdk-standard-residue.tsv"),
                standard.residue.stream().map(r -> fill.m3Simple + "\t" + r).toList(),
                StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
        Files.writeString(Path.of(args[3]), text, StandardCharsets.UTF_8);
        LOG.info(() -> "M3_JDK_STANDARD class=" + fill.m3Simple + " renamed="
                + standard.donorRows.size()
                + " residue=" + standard.residue.size());
        if (!standard.residue.isEmpty()) {
            System.exit(5);
        }
    }

    private List<Atom> atoms(String scaffold) throws IOException {
        List<Atom> atoms = new ArrayList<>();
        METHOD.matcher(scaffold).results().forEach(r -> atoms.add(atom(r.group(1), r.group(2))));
        atoms.stream().filter(a -> a.isStatic && a.params.size() == 1).findFirst()
                .ifPresent(a -> factoryParamType = a.params.get(0)[0]);
        surface = atoms.stream().map(Atom::name).collect(Collectors.toSet());
        lanes(atoms);
        return atoms;
    }

    private String fill(String scaffold) throws IOException {
        List<Atom> atoms = atoms(scaffold);
        StringBuilder out = new StringBuilder();
        Matcher m = METHOD.matcher(scaffold);
        int i = 0;
        while (m.find()) {
            Atom a = atoms.get(i++);
            Emit e = a.name.equals(m3Simple) ? constructor() : classify(a);
            ledger.add(
                    String.join("\t", m3Simple, a.signature, e.category,
                    e.body.replace('\n', ' ').replace('\u0000', ' ')));
            m.appendReplacement(out,
                    Matcher.quoteReplacement(e.category.equals("OBJECT") ? "" : render(a, e)));
        }
        m.appendTail(out);
        String text = syncView == null ? out.toString()
                : out.toString().replaceFirst("}\\s*$", Matcher.quoteReplacement(helper() + "}\n"));
        if (!members.isEmpty() || !helpers.isEmpty()) {
            String extra = String.join("", members.values()) + String.join("", helpers.values());
            text = text.replaceFirst("}\\s*$", Matcher.quoteReplacement(extra + "}\n"));
        }
        return ledger.stream().anyMatch(r -> r.contains("\tMANUAL\t")) ? text : header(text);
    }

    /**
     * VIEW helper: a lock-sharing Set view. No JDK owner fits
     * (java.util.Collections.synchronizedSet(Set, Object) is
     * package-private, and the public one locks on itself and synchronizes forEach), so a private
     * static nested class is
     * emitted. Its atoms are the public methods declared by the Commons lock owner
     * (SynchronizedCollection), synthesized
     * into a scaffold and filled by this same generator with the lock lane explicit; defaults stay
     * inherited.
     */
    private String helper() throws IOException {
        StringBuilder s =
                new StringBuilder(
                "public final class SyncSet<E>\n        implements java.util.Set<E>, " +
                "java.io.Serializable {\n"
                + "    @java.io.Serial\n    private static final long serialVersionUID = 0L;\n\n"
                + "    // Planned lanes (decorator)\n    private transient Object backing;\n    " +
                        "private transient int state;\n\n"
                + "    private SyncSet() {\n        throw new " +
                        "UnsupportedOperationException(M3_SCAFFOLD_MESSAGE);\n    }\n");
        Arrays.stream(syncView.getSuperclass().getDeclaredMethods())
                .filter(x -> java.lang.reflect.Modifier.isPublic(x.getModifiers())
                        && !java.lang.reflect.Modifier.isStatic(x.getModifiers()))
                .filter(x -> !x.isSynthetic() && !x.isBridge()).sorted(
                        java.util.Comparator.comparing(Method::toGenericString))
                .forEach(
                        x -> s.append("\n    public ").append(signature(x)).append(
                        " {\n        throw new " +
                        "UnsupportedOperationException(M3_SCAFFOLD_MESSAGE);\n    }\n"));
        String scaffold = s.append("}\n").toString();
        M3DecoratorFill view = new M3DecoratorFill(sources, syncView, scaffold, "lock");
        view.factoryParamType = "java.util.Set<E>";
        String text = view.fill(scaffold).replace("public final class SyncSet<E>",
                "private static final class SyncSet<E>");
        donors.putAll(view.donors);
        view.ledger.forEach(r -> ledger.add(m3Simple + "$" + r));
        return "\n    /** Lock-sharing Set view (VIEW helper): no JDK owner shares the caller's " +
                "lock; see DECORATOR_FILL SYNC. */\n"
                + text.lines().map(l -> l.isEmpty() ? l : "    " + l).collect(
                        Collectors.joining("\n")) + "\n";
    }

    private static String signature(Method x) {
        String typeParams = x.getTypeParameters().length == 0 ? ""
                : Arrays.stream(x.getTypeParameters()).map(
                        java.lang.reflect.TypeVariable::getName).collect(
                        Collectors.joining(", ", "<", "> "));
        java.lang.reflect.Type[] ps = x.getGenericParameterTypes();
        return typeParams + x.getGenericReturnType().getTypeName() + " " + x.getName() + "("
                + java.util.stream.IntStream.range(0, ps.length)
                .mapToObj(i -> ps[i].getTypeName() + " arg" + i).collect(Collectors.joining(", "))
                        + ")";
    }

    private static Atom atom(String visibility, String signature) {
        int open = signature.indexOf('(');
        String head = signature.substring(0, open);
        String name = head.substring(head.lastIndexOf(' ') + 1);
        String paramText = signature.substring(open + 1, signature.lastIndexOf(')'));
        List<String[]> params = paramText.isBlank() ? List.of()
                : Arrays.stream(paramText.split(",\\s*(?![^<]*>)"))
                .map(p -> new String[] {p.substring(0, p.lastIndexOf(' ')),
                        p.substring(p.lastIndexOf(' ') + 1)}).toList();
        boolean isStatic = head.startsWith("static ");
        String noStatic = isStatic ? head.substring(7) : head;
        String noTypeParams = noStatic.startsWith("<") ? noStatic.substring(closing(noStatic) + 2)
                : noStatic;
        String returnType = noTypeParams.equals(name) ? ""
                : noTypeParams.substring(0, noTypeParams.length() - name.length() - 1);
        return new Atom(visibility, signature, name, params, isStatic, returnType);
    }

    private static int closing(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            depth += s.charAt(i) == '<' ? 1 : s.charAt(i) == '>' ? -1 : 0;
            if (depth == 0) {
                return i;
            }
        }
        return s.length() - 1;
    }

    private Emit classify(Atom a) throws IOException {
        Optional<Method> resolved = resolve(a);
        if (resolved.isEmpty()) {
            return new Emit("MANUAL", "unresolved in " + commons.getName());
        }
        Class<?> declaring = resolved.get().getDeclaringClass();
        if (declaring == Object.class) {
            return new Emit("OBJECT", "inherited from Object");
        }
        if (declaring.isInterface()) {
            return defaultCall(a, resolved.get());
        }
        declaringNow = declaring;
        Optional<String> b = body(a, declaring);
        return b.isPresent() ? shape(a, b.get()) :
                new Emit("MANUAL", "no source body in " + declaring.getName());
    }

    /**
     * RESOLVE: the public method of the Commons class with the atom's erased parameters. v1.3: a
     * public method that only
     * a visibility bridge exposes (declared public in a package-private superclass, e.g.
     * AbstractInputCheckedMapDecorator
     * .entrySet) is found by walking the superclass chain.
     */
    private Optional<Method> resolve(Atom a) {
        List<String> erased = a.params.stream().map(p -> erase(p[0])).toList();
        Optional<Method> direct = Arrays.stream(commons.getMethods()).filter(m -> !m.isBridge()
                && m.getName().equals(a.name))
                .filter(m -> Arrays.stream(m.getParameterTypes()).map(
                        Class::getTypeName).toList().equals(erased))
                .min((x, y) -> x.getReturnType().isAssignableFrom(y.getReturnType()) ? 1 : -1);
        if (direct.isPresent()) {
            return direct;
        }
        for (Class<?> c = commons; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.isBridge() && Modifier.isPublic(m.getModifiers())
                        && m.getName().equals(a.name)
                        && Arrays.stream(m.getParameterTypes()).map(
                                Class::getTypeName).toList().equals(erased)) {
                    return Optional.of(m);
                }
            }
        }
        return Optional.empty();
    }

    private static String erase(String type) {
        String raw = type.replaceAll("<.*>", "").replace("...", "[]");
        String base = raw.replace("[]", "");
        String dims = raw.substring(base.length());
        String qualified = base.matches("[A-Z][A-Z0-9]*") ? "java.lang.Object"
                : base.contains(".")
                        || base.matches("int|long|short|byte|char|boolean|float|double") ? base
                        : "java.lang." + base;
        return qualified + dims;
    }

    private Emit defaultCall(Atom a, Method m) {
        Class<?> declaring = m.getDeclaringClass();
        String iface = directInterfaces.stream().map(M3DecoratorFill::load).flatMap(
                Optional::stream)
                .filter(declaring::isAssignableFrom).filter(c -> declares(c, m, declaring))
                .reduce((x, y) -> x.isAssignableFrom(y) ? y : x).map(Class::getName).orElse(
                        declaring.getName());
        return new Emit("DEFAULT", ret(a) + iface + ".super." + a.name + "(" + args(a) + ");");
    }

    private static boolean declares(Class<?> c, Method m, Class<?> declaring) {
        try {
            return c.getMethod(m.getName(), m.getParameterTypes()).getDeclaringClass() == declaring;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Optional<Class<?>> load(String name) {
        try {
            return Optional.of(Class.forName(name));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * Comment-stripped source of the top-level class that encloses c (cached); empty when the
     * sources lack it.
     */
    private Optional<String> source(Class<?> c) throws IOException {
        Class<?> top = c;
        while (top.getEnclosingClass() != null) {
            top = top.getEnclosingClass();
        }
        String key = top.getName();
        if (!sourceCache.containsKey(key)) {
            Path src = sources.resolve(key.replace('.', '/') + ".java");
            sourceCache.put(key, Files.exists(src) ? Files.readString(src, StandardCharsets.UTF_8)
                    .replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "") : null);
        }
        return Optional.ofNullable(sourceCache.get(key));
    }

    /**
     * v1.3 G1 scope: {open brace, end} of the body of c in its top-level source; nested classes are
     * found level by level.
     */
    private static int[] scope(String text, Class<?> c) {
        List<Class<?>> chain = new ArrayList<>();
        for (Class<?> k = c; k != null; k = k.getEnclosingClass()) {
            chain.add(0, k);
        }
        int open = -1;
        int end = text.length();
        for (Class<?> k : chain) {
            Matcher m = Pattern.compile("\\b(?:class|interface|enum) " + k.getSimpleName()
                    + "\\b[^{]*\\{").matcher(text).region(open + 1, end);
            if (!m.find()) {
                return new int[0];
            }
            open = m.end() - 1;
            end = close(text, open);
        }
        return new int[] {open, end};
    }

    private static int close(String text, int brace) {
        int depth = 0;
        for (int i = brace; i < text.length(); i++) {
            depth += text.charAt(i) == '{' ? 1 : text.charAt(i) == '}' ? -1 : 0;
            if (depth == 0) {
                return i;
            }
        }
        return text.length();
    }

    private static int depth(String text, int from, int to) {
        int d = 0;
        for (int i = from; i < to; i++) {
            d += text.charAt(i) == '{' ? 1 : text.charAt(i) == '}' ? -1 : 0;
        }
        return d;
    }

    /**
     * v1.3 G1: members named {@code name} declared directly in the body of c (depth 1), as
     * {parameter text, normalized body}.
     * A nested class (PredicatedCollection.Builder.add) can no longer shadow the member of its
     * enclosing class.
     */
    private List<String[]> declared(Class<?> c, String name) throws IOException {
        Optional<String> text = source(c);
        if (text.isEmpty()) {
            return List.of();
        }
        int[] scope = scope(text.get(), c);
        if (scope.length == 0) {
            return List.of();
        }
        List<String[]> out = new ArrayList<>();
        Matcher m = Pattern.compile("[\\s>]" + Pattern.quote(name)
                + "\\(([^)]*)\\)[^;{]*\\{").matcher(text.get()).region(scope[0] + 1, scope[1]);
        while (m.find()) {
            if (depth(text.get(), scope[0], m.start()) == 1) {
                out.add(new String[] {m.group(1), block(text.get(), m.end() - 1)});
            }
        }
        return out;
    }

    private Optional<String> body(Atom a, Class<?> declaring) throws IOException {
        for (String[] d : declared(declaring, a.name)) {
            String[] ps = d[0].isBlank() ? new String[0] : d[0].split(",(?![^<]*>)");
            if (ps.length == a.params.size() && sameSimpleTypes(ps, a)) {
                return Optional.of(d[1] + "\u0000" + names(ps));
            }
        }
        return Optional.empty();
    }

    private static boolean sameSimpleTypes(String[] ps, Atom a) {
        for (int i = 0; i < ps.length; i++) {
            String p = ps[i].trim().replace("final ", "");
            String type = simple(erase(p.substring(0, p.lastIndexOf(' ')).trim()));
            if (!type.equals(simple(erase(a.params.get(i)[0])))) {
                return false;
            }
        }
        return true;
    }

    private static String simple(String erased) {
        return erased.substring(erased.lastIndexOf('.') + 1).replace("Object[]", "Object[]");
    }

    private static String names(String[] ps) {
        return Arrays.stream(ps).map(
                p -> p.trim().substring(p.trim().lastIndexOf(' ') + 1)).collect(
                Collectors.joining(","));
    }

    private static String block(String text, int brace) {
        int end = close(text, brace);
        return end < text.length() ? collapse(text.substring(brace + 1, end)) : "";
    }

    /**
     * SFA: whitespace runs collapse in code only; string literals are data and keep their bytes.
     */
    private static String collapse(String s) {
        StringBuilder out = new StringBuilder();
        Matcher m = LITERAL.matcher(s);
        int pos = 0;
        while (m.find()) {
            out.append(s.substring(pos, m.start()).replaceAll("\\s+", " ")).append(m.group());
            pos = m.end();
        }
        return out.append(s.substring(pos).replaceAll("\\s+", " ")).toString().trim();
    }

    /**
     * ACCESSOR rule: a no-argument, non-public method of the Commons class chain whose normalized
     * body is
     * {@code return [(T) ][super.]decorated();} or {@code return [(T) ]F;} only re-types the
     * decorated owner
     * (getBag, getSortedBag, getSortedMap, getListIterator, decorated overrides). Calls to it are
     * rewritten to
     * {@code decorated()} before shape matching. v1.3 ALIAS: F is a backing field, i.e. a field
     * such an accessor returns and
     * a constructor of its class assigns with {@code this.F = Objects.requireNonNull(..)}.
     */
    private void accessors() throws IOException {
        Pattern field = Pattern.compile("return (\\([\\w.<>, ?]+\\) )?(\\w+);");
        for (Class<?> c = commons; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                Matcher f = field.matcher(accessorBody(c, m).orElse(""));
                if (f.matches() && backingField(f.group(2))) {
                    aliases.add(f.group(2));
                }
            }
        }
        for (Class<?> c = commons; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                Optional<String> b = accessorBody(c, m);
                if (b.filter(
                        s -> s.matches(
                        "return (\\([\\w.<>, ?]+\\) " +
                        ")?(super\\.)?(decorated\\(\\)|collection);")).isPresent()
                        || b.filter(s -> s.matches("return (\\([\\w.<>, ?]+\\) )?(\\w+);")
                                && aliases.contains(
                                        s.replaceAll("return (\\([\\w.<>, ?]+\\) )?(\\w+);",
                                        "$2"))).isPresent()) {
                    accessors.add(m.getName());
                }
            }
        }
    }

    private Optional<String> accessorBody(Class<?> c, Method m) throws IOException {
        if (m.getParameterCount() != 0 || java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
            return Optional.empty();
        }
        Atom probe = new Atom("protected", m.getName() + "()", m.getName(), List.of(), false, "");
        return body(probe, c).map(s -> s.substring(0, s.indexOf('\u0000')));
    }

    /**
     * ALIAS side condition: F is declared in the chain and assigned through Objects.requireNonNull
     * in its declaring class.
     */
    private boolean backingField(String f) throws IOException {
        for (Class<?> c = commons; c != null && c != Object.class; c = c.getSuperclass()) {
            if (Arrays.stream(c.getDeclaredFields()).anyMatch(x -> x.getName().equals(f)
                    && !Modifier.isStatic(x.getModifiers()))) {
                Optional<String> text = source(c);
                int[] scope = text.isPresent() ? scope(text.get(), c) : new int[0];
                return scope.length == 2
                        && Pattern.compile("this\\." + f + " = Objects\\.requireNonNull\\(")
                        .matcher(text.get().substring(scope[0], scope[1])).find();
            }
        }
        return false;
    }

    private Emit shape(Atom a, String bodyAndNames) {
        String body = bodyAndNames.substring(0, bodyAndNames.indexOf('\u0000')).replaceAll(
                "\\.<[\\w, ]+>(\\w+)\\(", ".$1(");
        for (String accessor : accessors) {
            body = body.replaceAll("\\b" + accessor + "\\(\\)", "decorated()");
        }
        List<String> names = bodyAndNames.endsWith("\u0000") ? List.of()
                : List.of(bodyAndNames.substring(bodyAndNames.indexOf('\u0000') + 1).split(","));
        for (String f : aliases) {
            if (unshadowed(body, names, f)) {
                body = body.replaceAll("(?<![\\w.])" + f + "\\.", "decorated().");
            }
        }
        return shape(a, body, names);
    }

    /**
     * R6(2) guard: a backing field name is the backing only when no parameter or local variable of
     * that name shadows it.
     */
    private static boolean unshadowed(String body, List<String> names, String f) {
        return !names.contains(f)
                && !Pattern.compile("(^|[;{}] )(final )?[\\w.]+(<[^=;]*>)? " + f + " = ").matcher(
                body).find();
    }

    private Emit shape(Atom a, String body, List<String> names) {
        String call = String.join(", ", names);
        String self = commons.getSimpleName();
        boolean lanesNew = laneArity >= 2 && names.size() == laneArity && ctorError == null;
        if (a.isStatic && (names.size() == 1 || lanesNew)
                && body.equals("return new " + self + "<>(" + call + ");")) {
            return new Emit("FACTORY_NEW", "return new " + m3Simple + "<>(" + args(a) + ");");
        }
        Matcher guard = Pattern.compile(
                "^if \\((\\w+) == this\\) \\{ return true; \\} (.+)$").matcher(body);
        if (guard.matches() && names.contains(guard.group(1)) && !isEqualsShape(body, names)) {
            Emit rest = shape(a, guard.group(2), names);
            return rest.category.equals("MANUAL") ? new Emit("MANUAL", body)
                    : new Emit("GUARD+" + rest.category, "if (arg" + names.indexOf(guard.group(1))
                            + " == this) {\n            return true;\n"
                            + "        }\n        " + rest.body);
        }
        Matcher sync = Pattern.compile("^synchronized \\(lock\\) \\{ (.+) \\}$").matcher(body);
        if (sync.matches()) {
            Emit inner = shape(a, sync.group(1), names);
            return inner.category.equals("MANUAL") ? new Emit("MANUAL", body)
                    : new Emit("SYNC(" + inner.category + ")", "synchronized (" + lockExpr
                            + ") {\n            "
                            + inner.body.replace("\n", "\n    ") + "\n        }");
        }
        Matcher view = Pattern.compile(
                "^final [\\w.]+(<.*>)? (\\w+) = decorated\\(\\)\\.(\\w+)\\(\\); return new " +
                "(\\w+)(<>)?\\(\\2, lock\\);$")
                .matcher(body);
        if (view.matches() && names.isEmpty() && syncView(view.group(4)).isPresent()) {
            syncView = syncView(view.group(4)).get();
            return new Emit("VIEW", "return new SyncSet<>(backing." + view.group(3) + "(), "
                    + lockExpr + ");");
        }
        Matcher validated = VALIDATE_GUARD.matcher(body);
        if (validated.matches()) {
            return guardDelegate(a, validated, names);
        }
        if (body.equals("throw new UnsupportedOperationException();")) {
            return new Emit("THROW", "throw new UnsupportedOperationException();");
        }
        if (body.matches("(return )?decorated\\(\\)\\." + a.name + "\\(" + Pattern.quote(call)
                + "\\);")) {
            return new Emit("DELEGATE", ret(a) + "backing." + a.name + "(" + args(a) + ");");
        }
        String n0 = names.isEmpty() ? "" : names.get(0);
        if (names.size() == 1
                && (body.equals("return " + n0 + " == this || decorated().equals(" + n0 + ");")
                || body.equals("if (" + n0 + " == this) { return true; } return decorated().equals("
                        + n0 + ");"))) {
            return new Emit("EQUALS", "return arg0 == this || backing.equals(arg0);");
        }
        if (a.isStatic && body.contains("instanceof Unmodifiable")
                && body.contains("return new " + self + "<>(" + call + ");")) {
            return factory(a);
        }
        return lanesShape(a, body, names).orElseGet(() -> wrap(a, body, names));
    }

    private static boolean isEqualsShape(String body, List<String> names) {
        String n0 = names.isEmpty() ? "" : names.get(0);
        return names.size() == 1
                && body.equals("if (" + n0 + " == this) { return true; } return decorated().equals("
                + n0 + ");");
    }

    /**
     * VIEW rule guard: the named Commons member class must implement Set, extend the class that
     * declares the lock lane
     * and declare no method of its own, so its behaviour is exactly the synchronized-collection
     * atoms over (set, lock).
     */
    private Optional<Class<?>> syncView(String simple) {
        for (Class<?> c = commons; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Class<?> member : c.getDeclaredClasses()) {
                if (member.getSimpleName().equals(simple)
                        && java.util.Set.class.isAssignableFrom(member)
                        && declaresLock(member.getSuperclass())
                                && member.getDeclaredMethods().length == 0) {
                    return Optional.of(member);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean declaresLock(Class<?> c) {
        return c != null
                && Arrays.stream(c.getDeclaredFields()).anyMatch(f -> f.getName().equals("lock")
                && f.getType() == Object.class);
    }

    private Emit wrap(Atom a, String body, List<String> names) {
        String wrapper = "(new \\w+<>|\\w+\\.\\w+|\\w+)";
        String inlined = body.replaceAll(
                "^final [\\w.]+(<[^=]*?>)? (\\w+) = (decorated\\(\\)\\.\\w+\\([^;]*\\)); return "
                + wrapper
                + "\\(\\2((?:, \\w+)*)\\);$", "return $4($3$5);");
        Matcher m = Pattern.compile("^return " + wrapper
                + "\\(decorated\\(\\)\\.(\\w+)\\(([^()]*)\\)((?:, \\w+)*)\\);$").matcher(inlined);
        if (!m.matches() || !m.group(3).equals(String.join(", ", names))) {
            return new Emit("MANUAL", body);
        }
        if (!m.group(4).isEmpty()) {
            return laneView(a, m.group(1), m.group(2), List.of(m.group(4).substring(2).split(", ")),
                    body);
        }
        String delegated = "backing." + m.group(2) + "(" + args(a) + ")";
        String self = commons.getSimpleName();
        boolean own = m.group(1).equals(self + "." + m3Factory()) || m.group(1).equals(m3Factory());
        String template = own ? m3Simple + "." + m3Factory() + "(%s)"
                : m.group(1).equals("new " + self + "<>") ? "new " + m3Simple + "<>(%s)"
                : WRAP.getOrDefault(m.group(1) + ":" + m.group(2), WRAP.get(m.group(1)));
        if (template == null) {
            return new Emit("MANUAL", body);
        }
        String expr = template.replace("%s", delegated).replace("%b", "backing").replace("%c",
                a.name + "(" + args(a) + ")");
        return new Emit("WRAP", "return " + expr + ";");
    }

    private String m3Factory() {
        String s = commons.getSimpleName();
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private Emit factory(Atom a) {
        String param = a.params.get(0)[0];
        String cast = param.equals(a.returnType) ? "" : "(" + a.returnType + ") ";
        return new Emit("FACTORY", "if (arg0 instanceof " + UNMOD + ") {\n            return "
                + cast + "arg0;\n        }\n"
                + "        return new " + m3Simple + "<>(arg0);");
    }

    private Emit constructor() {
        if (laneArity > 0) {
            return ctorError != null ? new Emit("MANUAL", ctorError)
                    : new Emit("LANES_CONSTRUCTOR", String.join("\n        ", ctorStatements)
                            + "\u0000" + ctorParams);
        }
        if (lockExpr.equals("lock")) {
            return new Emit("CONSTRUCTOR_LOCK",
                    "this.backing = java.util.Objects.requireNonNull(arg0, \"collection\");\n"
                    + "        this.lock = java.util.Objects.requireNonNull(arg1, \"lock\");\u0000"
                            + factoryParamType);
        }
        if (factoryParamType == null) {
            return new Emit("MANUAL",
                    "no single-argument static factory: constructor shape outside DECORATOR_FILL " +
                    "v1");
        }
        String typeVar = factoryParamType.replaceAll(".*<(\\? extends )?([A-Z]\\w*)>.*", "$2");
        String param = factoryParamType.replace("<"
                + (factoryParamType.contains("? extends") ? "? extends " : "") + typeVar + ">",
                factoryParamType.contains("? extends") ? "<? extends E>" : "<E>");
        String backing = backingType();
        String cast = param.equals(backing) ? "" : "(" + backing + ") ";
        return new Emit("CONSTRUCTOR", "this.backing = " + cast
                + "java.util.Objects.requireNonNull(arg0, \"collection\");\u0000" + param);
    }

    private String backingType() {
        return factoryParamType.replaceAll("<.*>", "") + "<E>";
    }

    private String render(Atom a, Emit e) {
        if (e.category.equals("LANES_CONSTRUCTOR")) {
            String[] bp = e.body.split("\u0000");
            return "    private " + m3Simple + "(" + bp[1] + ") {\n        " + bp[0] + "\n    }\n";
        }
        if (e.category.startsWith("CONSTRUCTOR")) {
            String[] bp = e.body.split("\u0000");
            String suppress = bp[0].contains(") java.util")
                    ? "    @SuppressWarnings(\"unchecked\")\n" : "";
            String lockParam = e.category.equals("CONSTRUCTOR_LOCK") ? ", final Object arg1" : "";
            return suppress + "    private " + m3Simple + "(final " + bp[1] + " arg0" + lockParam
                    + ") {\n        " + bp[0] + "\n    }\n";
        }
        boolean override = !a.isStatic && overrides(a);
        boolean unchecked = e.category.equals("FACTORY") && e.body.contains("return (");
        // MODIFIER rule: the Commons method's ACC_SYNCHRONIZED flag (SynchronizedSortedBag
        // comparator/first/last) is carried over.
        boolean monitor = !a.isStatic
                && resolve(a).filter(
                m -> java.lang.reflect.Modifier.isSynchronized(m.getModifiers())).isPresent();
        return (override ? "    @Override\n" : "")
                + (unchecked ? "    @SuppressWarnings(\"unchecked\")\n" : "") + "    "
                + a.visibility + " "
                + (monitor ? "synchronized " : "") + a.signature + " {\n        " + e.body
                        + "\n    }\n";
    }

    private boolean overrides(Atom a) {
        return resolve(a).map(
                m -> java.util.stream.Stream.concat(
                directInterfaces.stream().map(M3DecoratorFill::load)
                .flatMap(Optional::stream), java.util.stream.Stream.of(Object.class)).anyMatch(
                        c -> has(c, m))).orElse(false);
    }

    private static boolean has(Class<?> c, Method m) {
        try {
            c.getMethod(m.getName(), m.getParameterTypes());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /** The GENERATED block comment the scaffold carries; the fill replaces it. */
    private static final String GENERATED_BLOCK = "(?s)/\\*\n \\* GENERATED by .*? \\*/";

    /**
     * The GENERATED block comment of an owned class: provenance, how its atoms are emitted and an
     * optional extra
     * line. Long lines are wrapped later by {@link JdkStandard#layout(String)}.
     */
    private String generated(String how, String extra) {
        return "/*\n * GENERATED by " + GENERATOR
                + " (pattern DECORATOR_FILL v1.4) from the five-provider scaffold;\n"
                + " * regenerate, do not hand-edit. Behaviour mirrors " + commons.getName() + "\n"
                + " * from Apache Commons Collections 4.5.0 (Apache-2.0): " + how + "\n"
                + (extra.isEmpty() ? "" : " * " + extra + "\n") + " */";
    }

    private String header(String text) {
        if (laneArity > 0) {
            return lanesHeader(text);
        }
        String generated = generated(
                "each method atom is classified as THROW, DELEGATE, EQUALS, WRAP, DEFAULT, FACTORY,"
                + " FACTORY_NEW, SYNC, GUARD or VIEW and emitted from a template; no provider " +
                        "implementation code is copied.", "");
        return text.replaceFirst(GENERATED_BLOCK, Matcher.quoteReplacement(generated))
                .replaceFirst(
                        "(?s) \\* SCAFFOLD, not an owned implementation: declares the public " +
                        "surface of\n \\* (\\{@code [^}]+})"
                        + " and every body throws \\{@link UnsupportedOperationException}\\.",
                        Matcher.quoteReplacement(" * Owned decorator with the public contract of ")
                                + "$1.")
                .replace("{@code Object backing}, {@code int state}", "{@code backing}")
                .replaceFirst("    private static final String M3_SCAFFOLD_MESSAGE = .*\\R", "")
                .replaceFirst(
                        "(?s)    // Planned lanes \\(decorator\\).*?private transient int " +
                        "state;\\R",
                        Matcher.quoteReplacement(
                                "    /** The decorated owner: one reference, elements are never " +
                                "copied. */\n"
                                + "    @SuppressWarnings(\"serial\")\n    private final "
                                        + backingType() + " backing;\n"
                                + (lockExpr.equals("lock")
                                        ? "    /** The monitor shared with the owning decorator. " +
                                        "*/\n"
                                        + "    @SuppressWarnings(\"serial\")\n    private final " +
                                                "Object lock;\n" : "")))
                .replaceAll("\n{3,}", "\n\n");
    }

    /**
     * v1.3 header and lane block of a LANES class: the categories line lists exactly the categories
     * of its atoms.
     */
    private String lanesHeader(String text) {
        List<String> used = ledger.stream().map(
                r -> r.split("\t")[2].replaceAll("[/(+].*", "")).distinct().sorted().toList();
        String generated = generated(
                "each method atom is classified and emitted from a template; no provider " +
                "implementation"
                + " code is copied. Exception messages are string data extracted from the " +
                        "Commons source (SFA).",
                "Categories: " + String.join(", ", used) + ".");
        StringBuilder lanes = new StringBuilder(serialHook
                ? "    /** The decorated owner, written after the policy lanes by writeObject " +
                        "(the Commons stream order). */\n"
                        + "    private transient " + backingDecl + " backing;\n"
                : "    /** The decorated owner: one reference, elements are never copied. */\n"
                        + "    @SuppressWarnings(\"serial\")\n    private final " + backingDecl
                                + " backing;\n");
        laneTypes.forEach(
                (f, t) -> lanes.append("    /** Policy lane {@code ").append(f).append(
                "} (Commons field of the same name). */\n")
                .append("    @SuppressWarnings(\"serial\")\n    private final ").append(t).append(
                        ' ').append(f).append(";\n"));
        String laneDoc = laneTypes.keySet().stream().map(f -> "{@code " + f + "}").collect(
                Collectors.joining(", ", "{@code backing}"
                + (laneTypes.isEmpty() ? "" : ", "), ""));
        return text.replaceFirst("(?s)/\\*\n \\* GENERATED by .*? \\*/",
                Matcher.quoteReplacement(generated))
                .replaceFirst(
                        "(?s) \\* SCAFFOLD, not an owned implementation: declares the public " +
                        "surface of\n \\* (\\{@code [^}]+})"
                        + " and every body throws \\{@link UnsupportedOperationException}\\.",
                        Matcher.quoteReplacement(" * Owned decorator with the public contract of ")
                                + "$1.")
                .replace("{@code Object backing}, {@code int state}", laneDoc)
                .replaceFirst("    private static final String M3_SCAFFOLD_MESSAGE = .*\\R", "")
                .replaceFirst(
                        "(?s)    // Planned lanes \\(decorator\\).*?private transient int " +
                        "state;\\R", Matcher.quoteReplacement(lanes.toString()))
                .replaceAll("\n{3,}", "\n\n");
    }

    private static String ret(Atom a) {
        return a.returnType.equals("void") ? "" : "return ";
    }

    private static String args(Atom a) {
        return a.params.stream().map(p -> p[1]).collect(Collectors.joining(", "));
    }

    private static Optional<String> find(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    // ---------------------------------------------------------------------------------------------
    // v1.3 LANES family (Predicated*): policy lanes beside the backing, all derived from the
    // Commons class chain.
    // ---------------------------------------------------------------------------------------------

    /**
     * LANES pre-pass: a static atom whose Commons body is {@code return new Self<>(p0, p1[, p2]);}
     * (FACTORY_NEW of arity
     * n >= 2, or the arity a helper is created with) makes the class a LANES class; its constructor
     * is then derived from
     * the Commons constructor of arity n before any atom is classified, because views and guards
     * need the lane order.
     */
    private void lanes(List<Atom> atoms) throws IOException {
        for (Atom a : atoms) {
            Optional<String> b = laneArity > 0 || !a.isStatic || a.params.size() < 2
                    ? Optional.empty() : body(a, commons);
            if (b.isPresent() && b.get().equals("return new " + commons.getSimpleName() + "<>("
                    + b.get().substring(b.get().indexOf('\u0000') + 1).replace(",", ", ")
                            + ");\u0000"
                    + b.get().substring(b.get().indexOf('\u0000') + 1))) {
                laneArity = a.params.size();
            }
        }
        if (laneArity > 0) {
            lanesConstructor();
        }
    }

    /**
     * LANES_CONSTRUCTOR: the flattened Commons constructor must consist only of the statement
     * shapes s1 {@code this.F =
     * Objects.requireNonNull(p, "msg");}, s2 {@code this.F = p;}, s3 {@code for (final E i : p) {
     * validate(i); }} and s4
     * {@code p.forEach(this::validate);}, where every p is a constructor parameter (R5). The
     * backing field (ALIAS) becomes
     * {@code backing}; every other field becomes a policy lane typed by its Commons declaration
     * (deprecated Commons
     * functional interfaces map to their single java.util.function super interface). Statement
     * order is kept, so the
     * null-check order (backing first, then policy) is the Commons order.
     */
    private void lanesConstructor() throws IOException {
        Optional<String[]> flat = flatten(commons, laneArity);
        Optional<List<Type>> types = ctorTypes(commons, laneArity);
        if (flat.isEmpty() || types.isEmpty()) {
            ctorError = "no unique Commons constructor of arity " + laneArity
                    + " with a pass-through super chain";
            return;
        }
        List<String> names = List.of(flat.get()[1].split(","));
        List<String[]> parsed = new ArrayList<>();
        String rest = flat.get()[0];
        while (!rest.isEmpty()) {
            Matcher m = CTOR_STMT.matcher(rest);
            if (!m.lookingAt()) {
                ctorError = "constructor statement outside LANES_CONSTRUCTOR: " + rest;
                return;
            }
            String field = m.group(1) != null ? m.group(1) : m.group(4);
            String param = m.group(2) != null ? m.group(2) : m.group(5) != null ? m.group(5)
                    : m.group(8) != null ? m.group(8) : m.group(9);
            if (!ctorArgIsParam(names, param)) {
                ctorError = "constructor assigns a non-parameter: " + m.group();
                return;
            }
            parsed.add(new String[] {m.group(1) != null ? "s1" : m.group(4) != null ? "s2"
                    : m.group(6) != null ? "s3" : "s4", field, param,
                    m.group(3), m.group(6)});
            rest = rest.substring(m.end()).trim();
        }
        String[] slots = new String[laneArity];
        for (String[] s : parsed) {
            if (s[1] != null && !aliases.contains(s[1])) {
                laneTypes.put(s[1],
                        laneType(field(s[1]).map(Field::getGenericType).orElse(Object.class)));
            }
            if (s[1] != null && names.contains(s[2])) {
                slots[names.indexOf(s[2])] = aliases.contains(s[1]) ? "backing" : s[1];
            }
        }
        for (String[] s : parsed) {
            Optional<String> stmt = ctorStatement(s, names, types.get());
            if (stmt.isEmpty()) {
                ctorError = "constructor validation needs a LANE_CHECK validate member: "
                        + String.join(" ", s);
                return;
            }
            ctorStatements.add(stmt.get());
        }
        if (!"backing".equals(slots[0])) {
            ctorError = "constructor parameter 0 is not the backing field";
            return;
        }
        Arrays.stream(slots).skip(1).forEach(laneOrder::add);
        backingDecl = laneType(types.get().get(0));
        ctorParams = java.util.stream.IntStream.range(0, laneArity).mapToObj(i -> "final "
                + laneType(types.get().get(i)) + " arg" + i)
                .collect(Collectors.joining(", "));
        serialHook();
    }

    /**
     * R5 guard: in s1/s2/s3/s4 the assigned or iterated value must be a constructor parameter
     * ({@code this.lock = this} is not).
     */
    private static boolean ctorArgIsParam(List<String> names, String x) {
        return names.contains(x);
    }

    private Optional<String> ctorStatement(String[] s, List<String> names,
            List<Type> types) throws IOException {
        String arg = "arg" + names.indexOf(s[2]);
        String target = s[1] == null ? "" : aliases.contains(s[1]) ? "this.backing" : "this."
                + s[1];
        return switch (s[0]) {
            case "s1" -> Optional.of(target + " = java.util.Objects.requireNonNull(" + arg + ", "
                    + s[3] + ");");
            case "s2" -> Optional.of(target + " = " + arg + ";");
            case "s3" -> laneMember("validate", 1)
                    ? Optional.of("for (final " + s[4] + " item : " + arg
                    + ") {\n            validate(item);\n"
                    + "        }") : Optional.empty();
            default -> laneMember("validate",
                    rawClass(types.get(names.indexOf(s[2]))).filter(
                    Map.class::isAssignableFrom).isPresent() ? 2 : 1)
                    ? Optional.of(arg + ".forEach(this::validate);") : Optional.empty();
        };
    }

    private static Optional<Class<?>> rawClass(Type t) {
        return t instanceof Class<?> c ? Optional.of(c) : t instanceof ParameterizedType p
                ? rawClass(p.getRawType()) : Optional.empty();
    }

    private Optional<Field> field(String name) {
        for (Class<?> c = commons; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getName().equals(name)) {
                    return Optional.of(f);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * DEPRECATED_LANE_TYPE: a deprecated Commons functional interface becomes its single
     * java.util.function super interface.
     */
    private static String laneType(Type t) {
        Optional<Class<?>> raw = rawClass(t);
        if (raw.isPresent() && raw.get().isAnnotationPresent(Deprecated.class)
                && raw.get().getGenericInterfaces().length == 1
                && raw.get().getGenericInterfaces()[0] instanceof ParameterizedType sup
                && ((Class<?>) sup.getRawType()).getPackageName().equals("java.util.function")
                && Arrays.equals(sup.getActualTypeArguments(), raw.get().getTypeParameters())) {
            return ((Class<?>) sup.getRawType()).getName()
                    + t.getTypeName().substring(raw.get().getName().length());
        }
        return t.getTypeName();
    }

    /**
     * Commons constructor of the given arity, with each pass-through {@code super(p..);} replaced
     * by the parent body.
     */
    private Optional<String[]> flatten(Class<?> c, int arity) throws IOException {
        List<String[]> found = declared(c, c.getSimpleName()).stream()
                .filter(d -> (d[0].isBlank() ? 0
                        : d[0].split(",(?![^<]*>)").length) == arity).toList();
        if (found.size() != 1) {
            return Optional.empty();
        }
        String[] ps = found.get(0)[0].isBlank() ? new String[0]
                : found.get(0)[0].split(",(?![^<]*>)");
        List<String> names = List.of(names(ps).split(","));
        String body = found.get(0)[1];
        Matcher sup = Pattern.compile("^super\\(([\\w, ]*)\\);\\s*").matcher(body);
        if (sup.lookingAt()) {
            List<String> passed = sup.group(1).isBlank() ? List.of()
                    : List.of(sup.group(1).split(", "));
            Optional<String[]> parent = names.containsAll(passed)
                    ? flatten(c.getSuperclass(), passed.size()) : Optional.empty();
            if (parent.isEmpty()) {
                return Optional.empty();
            }
            List<String> parentNames = List.of(parent.get()[1].split(","));
            Map<String, String> rename = new HashMap<>();
            java.util.stream.IntStream.range(0, passed.size()).forEach(
                    i -> rename.put(parentNames.get(i), passed.get(i)));
            body = (renameCode(parent.get()[0], rename) + " " + body.substring(sup.end())).trim();
        }
        return Optional.of(new String[] {body, String.join(",", names)});
    }

    /**
     * SFA rename: identifiers are renamed in code only; string literals and field selections
     * (this.x, a.x) keep their text.
     */
    private static String renameCode(String text, Map<String, String> rename) {
        if (rename.isEmpty()) {
            return text;
        }
        Pattern ids = Pattern.compile("(?<![.\\w])(" + String.join("|", rename.keySet()) + ")\\b");
        StringBuilder out = new StringBuilder();
        Matcher lit = LITERAL.matcher(text);
        int pos = 0;
        while (lit.find()) {
            out.append(
                    ids.matcher(text.substring(pos, lit.start())).replaceAll(
                    r -> Matcher.quoteReplacement(rename.get(r.group(1)))));
            out.append(lit.group());
            pos = lit.end();
        }
        return out.append(
                ids.matcher(text.substring(pos)).replaceAll(
                r -> Matcher.quoteReplacement(rename.get(r.group(1))))).toString();
    }

    /**
     * Generic parameter types of the unique Commons constructor of that arity (an inner class's
     * outer instance is dropped).
     */
    private static Optional<List<Type>> ctorTypes(Class<?> c, int arity) {
        boolean inner = c.getEnclosingClass() != null && !Modifier.isStatic(c.getModifiers());
        List<Constructor<?>> found = Arrays.stream(c.getDeclaredConstructors())
                .filter(k -> k.getParameterCount() - (inner ? 1 : 0) == arity).toList();
        if (found.size() != 1) {
            return Optional.empty();
        }
        Type[] g = found.get(0).getGenericParameterTypes();
        return Optional.of(Arrays.asList(g).subList(g.length - arity, g.length));
    }

    /**
     * SERIAL_HOOK: when the Commons backing field is transient, the Commons chain must write it
     * with private hooks of the
     * shape {@code out.defaultWriteObject(); out.writeObject(F);} / {@code in.defaultReadObject();
     * F = (T) in.readObject();}.
     * The owned class then declares a transient backing and the same hooks, so policy lanes precede
     * the backing in the
     * stream (R7: the NotSerializableException names the same object as in Commons).
     */
    private void serialHook() throws IOException {
        String backing = aliases.stream().filter(f -> field(f).isPresent()).findFirst().orElse("");
        if (field(backing).filter(f -> Modifier.isTransient(f.getModifiers())).isEmpty()) {
            return;
        }
        Optional<Method> write = declaredInChain(commons, "writeObject", 1);
        Optional<Method> read = declaredInChain(commons, "readObject", 1);
        Optional<String> w = (write.isPresent() ? memberBody(write.get())
                : Optional.<String>empty()).map(s -> s.substring(0, s.indexOf('\u0000')));
        Optional<String> r = (read.isPresent() ? memberBody(read.get())
                : Optional.<String>empty()).map(s -> s.substring(0, s.indexOf('\u0000')));
        String f = "(?:" + Pattern.quote(backing) + "|decorated\\(\\))";
        if (w.filter(
                s -> s.matches("(\\w+)\\.defaultWriteObject\\(\\); \\1\\.writeObject\\(" + f
                + "\\);")).isEmpty()
                || r.filter(
                        s -> s.matches("(\\w+)\\.defaultReadObject\\(\\); " + Pattern.quote(backing)
                        + " = (\\([\\w.<>, ?]+\\) )?\\1\\.readObject\\(\\);")).isEmpty()) {
            ctorError = "transient Commons backing without a SERIAL_HOOK shape: "
                    + w.orElse("no writeObject") + " / " + r.orElse("no readObject");
            return;
        }
        serialHook = true;
        members.put("writeObject",
                "\n    @java.io.Serial\n    private void writeObject(final " +
                "java.io.ObjectOutputStream arg0) throws java.io.IOException {\n"
                + "        arg0.defaultWriteObject();\n        arg0.writeObject(backing);\n    " +
                        "}\n");
        members.put("readObject",
                "\n    @java.io.Serial\n    @SuppressWarnings(\"unchecked\")\n    private void " +
                "readObject(final java.io.ObjectInputStream arg0)"
                + " throws java.io.IOException, ClassNotFoundException {\n        " +
                        "arg0.defaultReadObject();\n        backing = (" + backingDecl
                + ") arg0.readObject();\n    }\n");
        ledger.add(
                String.join("\t", m3Simple,
                "private void writeObject(java.io.ObjectOutputStream arg0)", "SERIAL_HOOK",
                "arg0.defaultWriteObject(); arg0.writeObject(backing);"));
        ledger.add(
                String.join("\t", m3Simple,
                "private void readObject(java.io.ObjectInputStream arg0)", "SERIAL_HOOK",
                "arg0.defaultReadObject(); backing = (" + backingDecl + ") arg0.readObject();"));
    }

    private static Optional<Method> declaredInChain(Class<?> start, String name, int arity) {
        for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == arity && !m.isBridge()
                        && !m.isSynthetic()
                        && !Modifier.isPublic(m.getModifiers())
                                && !Modifier.isStatic(m.getModifiers())) {
                    return Optional.of(m);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> memberBody(Method m) throws IOException {
        Type[] ps = m.getGenericParameterTypes();
        List<String[]> params = java.util.stream.IntStream.range(0, ps.length)
                .mapToObj(i -> new String[] {ps[i].getTypeName(), "arg" + i}).toList();
        return body(
                new Atom("private", m.getName(), m.getName(), params, false,
                m.getGenericReturnType().getTypeName()), m.getDeclaringClass());
    }

    /**
     * Lane members VALIDATE ({@code void validate(..)}) and CHECK_SET_VALUE ({@code V
     * checkSetValue(V)}): the most derived
     * non-public Commons declaration must be a sequence of LANE_CHECK statements over this class's
     * lanes and parameters,
     * optionally followed by {@code return p;}. The message is a concatenation of string literals,
     * parameters and lanes
     * only. The member is emitted once; an inner helper's members are emitted into its enclosing
     * class.
     */
    private boolean laneMember(String name, int arity) throws IOException {
        String key = name + "/" + arity;
        if (members.containsKey(key)) {
            return !members.get(key).isEmpty();
        }
        Optional<Method> m = declaredInChain(commons, name, arity);
        if (m.isEmpty()) {
            return outer != null && outer.laneMember(name, arity);
        }
        Optional<String> b = memberBody(m.get());
        Type[] ps = m.get().getGenericParameterTypes();
        List<String> names = b.map(
                s -> List.of(s.substring(s.indexOf('\u0000') + 1).split(","))).orElse(List.of());
        Optional<String> emitted = b.flatMap(
                s -> laneChecks(s.substring(0, s.indexOf('\u0000')), names,
                m.get().getGenericReturnType().getTypeName(), ps));
        String sig = "private " + m.get().getGenericReturnType().getTypeName() + " " + name + "("
                + java.util.stream.IntStream.range(0, ps.length)
                .mapToObj(i -> ps[i].getTypeName() + " arg" + i).collect(Collectors.joining(", "))
                        + ")";
        String category = m.get().getGenericReturnType() == void.class ? "VALIDATE"
                : "CHECK_SET_VALUE";
        ledger.add(String.join("\t", m3Simple, sig, emitted.isPresent() ? category : "MANUAL",
                emitted.orElse(
                        b.map(s -> s.substring(0, s.indexOf('\u0000'))).orElse(
                        "no source body")).replace('\n', ' ')));
        String sigFinal = sig.replaceAll("\\((.*)\\)", "("
                + java.util.stream.IntStream.range(0, ps.length)
                .mapToObj(i -> "final " + ps[i].getTypeName() + " arg" + i).collect(
                        Collectors.joining(", ")) + ")");
        members.put(key,
                emitted.map(e -> "\n    " + sigFinal + " {\n        " + e + "\n    }\n").orElse(
                ""));
        return emitted.isPresent();
    }

    private Optional<String> laneChecks(String body, List<String> names, String returnType,
            Type[] ps) {
        List<String> out = new ArrayList<>();
        String rest = body;
        Map<String, String> rename = new HashMap<>();
        java.util.stream.IntStream.range(0, names.size()).forEach(
                i -> rename.put(names.get(i), "arg" + i));
        Matcher m = LANE_CHECK.matcher(rest);
        while (m.lookingAt()) {
            boolean lanesOk = laneTypes.containsKey(m.group(2))
                    && (m.group(1) == null || m.group(1).equals(m.group(2)))
                    && names.contains(m.group(3))
                            && Arrays.stream(
                            LITERAL.matcher(m.group(4)).replaceAll("\"\"").split(" \\+ "))
                            .filter(t -> !t.equals("\"\"")).allMatch(t -> names.contains(t)
                                    || laneTypes.containsKey(t));
            if (!lanesOk) {
                return Optional.empty();
            }
            out.add("if (" + (m.group(1) == null ? "" : m.group(1) + " != null && ") + "!"
                    + m.group(2) + ".test(" + rename.get(m.group(3))
                    + ")) {\n            throw new IllegalArgumentException("
                            + renameCode(m.group(4), rename) + ");\n        }");
            rest = rest.substring(m.end()).trim();
            m = LANE_CHECK.matcher(rest);
        }
        Matcher r = Pattern.compile("return (\\w+);").matcher(rest);
        boolean voidOk = rest.isEmpty() && returnType.equals("void");
        boolean returnOk = r.matches() && names.contains(r.group(1))
                && ps[names.indexOf(r.group(1))].getTypeName().equals(returnType);
        if (out.isEmpty() || !(voidOk || returnOk)) {
            return Optional.empty();
        }
        return Optional.of(String.join("\n        ", out)
                + (returnOk ? "\n        return " + rename.get(r.group(1)) + ";" : ""));
    }

    /**
     * GUARD_DELEGATE: one guard (elem {@code validate(p);}, entry {@code validate(k, v);},
     * each/each-entry: a for-each that
     * validates every element or entry of a parameter) followed by a DELEGATE, or by {@code
     * super.m(params);} when the
     * superclass body of m is itself a DELEGATE (R6(1)). Validation runs before the single
     * delegated call.
     */
    private Emit guardDelegate(Atom a, Matcher g, List<String> names) {
        String form = g.group(1) != null ? "elem" : g.group(2) != null ? "entry"
                : g.group(4) != null ? "each" : "each-entry";
        List<String> guarded = java.util.stream.Stream.of(g.group(1), g.group(2), g.group(3),
                g.group(6), g.group(8))
                .filter(java.util.Objects::nonNull).toList();
        String body = g.group(0);
        try {
            if (!names.containsAll(guarded)
                    || !laneMember("validate", form.endsWith("entry") ? 2 : 1)) {
                return new Emit("MANUAL", body);
            }
            Emit rest = restDelegate(a, g.group(9), names);
            if (!rest.category.equals("DELEGATE")) {
                return new Emit("MANUAL", body);
            }
            String guard = switch (form) {
                case "elem" -> "validate(" + arg(names, g.group(1)) + ");";
                case "entry" -> "validate(" + arg(names, g.group(2)) + ", " + arg(names, g.group(3))
                        + ");";
                case "each" -> "for (final " + g.group(4) + " item : " + arg(names, g.group(6))
                        + ") {\n            validate(item);\n        }";
                default -> "for (final java.util.Map.Entry<? extends K, ? extends V> entry : "
                        + arg(names, g.group(8)) + ".entrySet()) {\n"
                        + "            validate(entry.getKey(), entry.getValue());\n        }";
            };
            return new Emit("GUARD_DELEGATE/" + form, guard + "\n        " + rest.body);
        } catch (IOException e) {
            return new Emit("MANUAL", body + " (" + e + ")");
        }
    }

    private static String arg(List<String> names, String n) {
        return "arg" + names.indexOf(n);
    }

    private Emit restDelegate(Atom a, String rest, List<String> names) throws IOException {
        Emit direct = shape(a, rest, names);
        Matcher sup = Pattern.compile("^(return )?super\\.(\\w+)\\((.*)\\);$").matcher(rest);
        if (direct.category.equals("DELEGATE") || !sup.matches() || !sup.group(2).equals(a.name)
                || !sup.group(3).equals(String.join(", ", names))) {
            return direct;
        }
        Class<?> here = declaringNow;
        Optional<Emit> superShape = Optional.empty();
        for (Class<?> c = here.getSuperclass(); c != null && c != Object.class
                && superShape.isEmpty(); c = c.getSuperclass()) {
            Optional<String> b = body(a, c);
            if (b.isPresent()) {
                declaringNow = c;
                superShape = Optional.of(shape(a, b.get()));
            }
        }
        declaringNow = here;
        return superShape.filter(this::superIsDelegate).isPresent()
                ? new Emit("DELEGATE", ret(a) + "backing." + a.name + "(" + args(a) + ");") :
                        new Emit("MANUAL", rest);
    }

    /**
     * R6(1) guard: super.m(..) counts as the delegate only when the superclass body of m is a
     * DELEGATE.
     */
    private boolean superIsDelegate(Emit superShape) {
        return superShape.category.equals("DELEGATE");
    }

    /**
     * v1.3 shapes tried after the v1.1/v1.2 ones and before WRAP: SELF_CALL, INNER_VIEW,
     * MAP_ITERATOR_ADAPTER, CHECKED_ENTRY_SET.
     */
    private Optional<Emit> lanesShape(Atom a, String body, List<String> names) {
        try {
            Matcher self = Pattern.compile(
                    "^(return )?(\\w+)\\(((?:\\w+|\\d+)(?:, (?:\\w+|\\d+))*)?\\);$").matcher(body);
            if (self.matches() && surface.contains(self.group(2))
                    && !self.group(2).equals("validate") && !a.isStatic) {
                List<String> passed = self.group(3) == null ? List.of()
                        : List.of(self.group(3).split(", "));
                if (passed.stream().allMatch(x -> names.contains(x) || x.matches("\\d+"))
                        && (self.group(1) != null) == !ret(a).isEmpty()) {
                    return Optional.of(
                            new Emit("SELF_CALL", ret(a) + self.group(2) + "(" + passed.stream()
                            .map(x -> x.matches("\\d+") ? x : arg(names, x)).collect(
                                    Collectors.joining(", ")) + ");"));
                }
            }
            Matcher inner = Pattern.compile(
                    "^return new (\\w+)\\(decorated\\(\\)\\.(\\w+)\\(([^()]*)\\)\\);$").matcher(
                    body);
            if (inner.matches() && inner.group(2).equals(a.name)
                    && inner.group(3).equals(String.join(", ", names))) {
                return Optional.of(
                        innerHelper(inner.group(1)).map(h ->
                        new Emit("INNER_VIEW", "return new " + h + "(backing." + a.name
                        + "(" + args(a) + "));")).orElse(new Emit("MANUAL", body)));
            }
            if (body.equals("return new EntrySetToMapIteratorAdapter<>(entrySet());")
                    && surface.contains("entrySet") && adapterContract()) {
                helpers.put("EntrySetMapIterator", MAP_ITERATOR_HELPER);
                return Optional.of(
                        new Emit("MAP_ITERATOR_ADAPTER",
                        "return new EntrySetMapIterator<>(entrySet());"));
            }
            Matcher checked = CHECKED_ENTRY_SET.matcher(body);
            if (checked.matches()) {
                return Optional.of(
                        checkedEntrySet(a, checked.group(1)).orElse(new Emit("MANUAL", body)));
            }
        } catch (IOException e) {
            return Optional.of(new Emit("MANUAL", body + " (" + e + ")"));
        }
        return Optional.empty();
    }

    /**
     * CHECKED_ENTRY_SET side conditions (R4): {@code isSetValueChecking()} is {@code return L !=
     * null;} over a policy lane L,
     * {@code checkSetValue} is a validating lane member (CHECK_SET_VALUE, not a transforming hook),
     * the class is a
     * {@code <K, V>} map and the Commons nested EntrySet / EntrySetIterator / MapEntry override
     * exactly what the H3 template
     * overrides. Emitted: {@code L != null ? new CheckedEntrySet<..>(backing.entrySet()) :
     * backing.entrySet()}.
     */
    private Optional<Emit> checkedEntrySet(Atom a, String entrySetClass) throws IOException {
        Optional<Method> checking = declaredInChain(commons, "isSetValueChecking", 0);
        Optional<String> lane = (checking.isPresent() ? memberBody(checking.get())
                : Optional.<String>empty())
                .map(s -> s.substring(0, s.indexOf('\u0000'))).filter(
                        s -> s.matches("return \\w+ != null;"))
                .map(s -> s.replaceAll("return (\\w+) != null;", "$1")).filter(
                        laneTypes::containsKey);
        Class<?> owner = declaredInChain(commons, "checkSetValue", 1).map(
                Method::getDeclaringClass).orElse(Object.class);
        Class<?> decl = resolve(a).map(Method::getDeclaringClass).orElse(Object.class);
        boolean contract = contract(decl, entrySetClass, "iterator", "toArray", "toArray")
                && contract(decl, "EntrySetIterator", "next")
                && contract(decl, "MapEntry", "setValue")
                        && Arrays.stream(commons.getTypeParameters()).map(Object::toString).toList()
                        .equals(List.of("K", "V"));
        if (lane.isEmpty() || owner == Object.class || !contract || !checkSetValueValidates()) {
            return Optional.empty();
        }
        helpers.put("CheckedEntrySet", CHECKED_ENTRY_SET_HELPER);
        String element = a.returnType.substring(a.returnType.indexOf('<') + 1,
                a.returnType.lastIndexOf('>'));
        return Optional.of(
                new Emit("CHECKED_ENTRY_SET", "return " + lane.get()
                + " != null ? new CheckedEntrySet<" + element
                + ">(backing.entrySet()) : backing.entrySet();"));
    }

    /**
     * R4 guard: checkSetValue must be a validating lane member (LANE_CHECK statements + return
     * value).
     */
    private boolean checkSetValueValidates() throws IOException {
        return laneMember("checkSetValue", 1);
    }

    private static boolean contract(Class<?> outerClass, String nested, String... declared) {
        return Arrays.stream(outerClass.getDeclaredClasses()).filter(
                c -> c.getSimpleName().equals(nested)).findFirst()
                .map(c -> Arrays.stream(c.getDeclaredMethods()).filter(m -> !m.isSynthetic()).map(
                        Method::getName).sorted().toList()
                        .equals(Arrays.stream(declared).sorted().toList())).orElse(false);
    }

    /**
     * MAP_ITERATOR_ADAPTER guard: EntrySetToMapIteratorAdapter still has exactly the surface the H2
     * template implements.
     */
    private static boolean adapterContract() {
        return load("org.apache.commons.collections4.map.EntrySetToMapIteratorAdapter").filter(
                c -> Set.of(c.getInterfaces()).equals(Set.of(
                load("org.apache.commons.collections4.MapIterator").orElseThrow(),
                        load("org.apache.commons.collections4.ResettableIterator")
                        .orElseThrow()))
                                && Arrays.stream(c.getDeclaredMethods()).filter(
                                m -> Modifier.isPublic(m.getModifiers()))
                                .map(Method::getName).collect(Collectors.toSet())
                                .equals(Set.of("getKey", "getValue", "hasNext", "next", "remove",
                                        "reset", "setValue"))).isPresent();
    }

    /**
     * LANE_VIEW: {@code return W(decorated().m(params), lanes..)} (one local inlined). The lane
     * arguments must be this
     * class's lanes in constructor order (R6(3)). W is the class itself ({@code new Self<>}, or its
     * own factory when that
     * factory is FACTORY_NEW) -> {@code new M3Self<>(..)}; or a Commons superclass of the decorator
     * (sibling, e.g.
     * PredicatedSortedSet inside PredicatedNavigableSet) -> a private static nested helper
     * generated from that class.
     */
    private Emit laneView(Atom a, String w, String m, List<String> lanes, String body) {
        if (laneArity == 0 || !lanesInOrder(lanes)) {
            return new Emit("MANUAL", body);
        }
        String self = commons.getSimpleName();
        String delegated = "backing." + m + "(" + args(a) + "), " + String.join(", ", lanes);
        try {
            if (w.equals("new " + self + "<>")
                    || (w.equals(m3Factory()) || w.equals(self + "." + m3Factory()))
                    && ownFactoryIsNew()) {
                return new Emit("LANE_VIEW/self", "return new " + m3Simple + "<>(" + delegated
                        + ");");
            }
            Optional<Class<?>> sibling = w.matches("new \\w+<>")
                    ? siblingSuper(w.substring(4, w.length() - 2)) : Optional.empty();
            Optional<String> helper = sibling.isPresent() ? siblingHelper(sibling.get(), lanes)
                    : Optional.empty();
            return helper.map(h ->
                    new Emit("LANE_VIEW/sibling", "return new " + h + "<>(" + delegated
                    + ");")).orElse(new Emit("MANUAL", body));
        } catch (IOException e) {
            return new Emit("MANUAL", body + " (" + e + ")");
        }
    }

    /**
     * R6(3) guard: the lane arguments of a view are this class's lanes, in the order the
     * constructor takes them.
     */
    private boolean lanesInOrder(List<String> lanes) {
        return lanes.equals(laneOrder);
    }

    private boolean ownFactoryIsNew() throws IOException {
        for (Method f : commons.getMethods()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getName().equals(m3Factory())
                    && f.getParameterCount() == laneArity
                    && f.getDeclaringClass() == commons) {
                Optional<String> b = memberBody(f);
                String call = b.map(
                        s -> s.substring(s.indexOf('\u0000') + 1).replace(",", ", ")).orElse("?");
                return b.filter(
                        s -> s.startsWith("return new " + commons.getSimpleName() + "<>(" + call
                        + ");\u0000")).isPresent();
            }
        }
        return false;
    }

    /**
     * Sibling wrapper {@code new S<>(..)}: S is resolved in the decorator's package and must pass
     * the superclass guard.
     */
    private Optional<Class<?>> siblingSuper(String simple) {
        return load(commons.getPackageName() + "." + simple).filter(this::siblingIsSuper);
    }

    /**
     * R6(3) guard: a sibling wrapper must be a concrete Commons superclass of this decorator (not
     * any same-package class).
     */
    private boolean siblingIsSuper(Class<?> c) {
        return c.isAssignableFrom(commons) && !Modifier.isAbstract(c.getModifiers());
    }

    /**
     * Synthesized scaffold of a Commons class: its public instance methods of the class chain (no
     * interface defaults).
     */
    private static String synth(Class<?> c, String name, boolean statics) {
        String typeParams = c.getTypeParameters().length == 0 ? ""
                : Arrays.stream(c.getTypeParameters()).map(Object::toString).collect(
                        Collectors.joining(", ", "<", ">"));
        Set<Type> ifaces = new LinkedHashSet<>();
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            Arrays.stream(k.getGenericInterfaces()).filter(
                    t -> rawClass(t).filter(r -> Modifier.isPublic(r.getModifiers())).isPresent())
                    .forEach(ifaces::add);
        }
        List<String> minimal = ifaces.stream().filter(
                t -> ifaces.stream().noneMatch(u -> u != t && rawClass(t).get() != rawClass(u).get()
                && rawClass(t).get().isAssignableFrom(rawClass(u).get()))).map(
                        Type::getTypeName).distinct().sorted().toList();
        boolean serial = java.io.Serializable.class.isAssignableFrom(c);
        StringBuilder s =
                new StringBuilder("public final class " + name + typeParams
                + "\n        implements " + String.join(", ", minimal)
                + " {\n"
                        + (serial
                        ? "    @java.io.Serial\n    private static final long serialVersionUID = " +
                        "0L;\n\n" : "")
                + "    // Planned lanes (decorator)\n    private transient Object backing;\n    " +
                        "private transient int state;\n\n"
                + "    private " + name
                        + "() {\n        throw new " +
                        "UnsupportedOperationException(M3_SCAFFOLD_MESSAGE);\n    }\n");
        Arrays.stream(c.getMethods()).filter(x -> statics == Modifier.isStatic(x.getModifiers())
                || !Modifier.isStatic(x.getModifiers()))
                .filter(x -> !x.isSynthetic() && !x.isBridge()
                        && x.getDeclaringClass() != Object.class
                        && !x.getDeclaringClass().isInterface())
                .sorted(java.util.Comparator.comparing(Method::getName).thenComparing(
                        Method::toGenericString))
                .forEach(
                        x -> s.append(Modifier.isStatic(x.getModifiers())
                        ? "\n    @SuppressWarnings({\"deprecation\"})\n    public static "
                        : "\n    public ")
                        .append(signature(x)).append(
                                " {\n        throw new " +
                                "UnsupportedOperationException(M3_SCAFFOLD_MESSAGE);\n    }\n"));
        return s.append("}\n").toString();
    }

    /**
     * LANE_VIEW sibling helper: the sibling Commons class filled by this generator as a private
     * static nested class.
     */
    private Optional<String> siblingHelper(Class<?> sibling,
            List<String> lanes) throws IOException {
        String name = sibling.getSimpleName() + "View";
        if (!helpers.containsKey(name)) {
            String scaffold = synth(sibling, name, false);
            M3DecoratorFill view = new M3DecoratorFill(sources, sibling, scaffold, "this", null);
            view.laneArity = lanes.size() + 1;
            String text = view.fill(scaffold);
            donors.putAll(view.donors);
            view.ledger.forEach(r -> ledger.add(m3Simple + "$" + r));
            boolean ok = view.ledger.stream().noneMatch(r -> r.contains("\tMANUAL\t"))
                    && view.laneOrder.equals(lanes);
            helpers.put(name, ok
                    ? nest(text, "public final class " + name, "private static final class " + name,
                    "LANE_VIEW sibling helper: the " + sibling.getSimpleName()
                            + " contract, filled by DECORATOR_FILL (no cross-module reference)")
                    : "");
        }
        return helpers.get(name).isEmpty() ? Optional.empty() : Optional.of(name);
    }

    /**
     * INNER_VIEW helper: a non-static member class of the Commons chain, filled by this generator
     * as an inner class.
     */
    private Optional<String> innerHelper(String simple) throws IOException {
        Optional<Class<?>> member = Optional.empty();
        for (Class<?> c = commons; c != null && c != Object.class
                && member.isEmpty(); c = c.getSuperclass()) {
            member = Arrays.stream(c.getDeclaredClasses()).filter(
                    k -> k.getSimpleName().equals(simple) && !Modifier.isStatic(k.getModifiers())
                    && !k.isInterface() && k.getTypeParameters().length == 0).findFirst();
        }
        if (member.isEmpty()) {
            return Optional.empty();
        }
        if (!helpers.containsKey(simple)) {
            String scaffold = synth(member.get(), simple, false);
            M3DecoratorFill view =
                    new M3DecoratorFill(sources, member.get(), scaffold, "this", this);
            view.laneArity = 1;
            String text = view.fill(scaffold);
            donors.putAll(view.donors);
            view.ledger.forEach(r -> ledger.add(m3Simple + "$" + r));
            boolean ok = view.ledger.stream().noneMatch(r -> r.contains("\tMANUAL\t"));
            helpers.put(simple, ok
                    ? nest(text, "public final class " + simple, "private final class " + simple,
                    "INNER_VIEW helper: the " + member.get().getName().replaceAll(".*\\.", "")
                            + " contract, filled by DECORATOR_FILL") : "");
        }
        return helpers.get(simple).isEmpty() ? Optional.empty() : Optional.of(simple);
    }

    private static String nest(String text, String from, String to, String doc) {
        return "\n    /** " + doc + ". */\n"
                + text.replace(from, to).lines().map(l -> l.isEmpty() ? l : "    " + l)
                .collect(Collectors.joining("\n")) + "\n";
    }

    /**
     * H2 template (MAP_ITERATOR_ADAPTER): the EntrySetToMapIteratorAdapter contract over this map's
     * entrySet().
     */
    private static final String MAP_ITERATOR_HELPER = """

                /** MAP_ITERATOR_ADAPTER helper (no JDK owner): a MapIterator over an entry set, \
            the EntrySetToMapIteratorAdapter contract. */
                private static final class EntrySetMapIterator<K, V>
                        implements org.apache.commons.collections4.MapIterator<K, V>, \
            org.apache.commons.collections4.ResettableIterator<K> {
                    private final java.util.Set<java.util.Map.Entry<K, V>> entrySet;
                    private java.util.Iterator<java.util.Map.Entry<K, V>> iterator;
                    private java.util.Map.Entry<K, V> entry;

                    EntrySetMapIterator(final java.util.Set<java.util.Map.Entry<K, V>> entrySet) {
                        this.entrySet = entrySet;
                        reset();
                    }

                    private synchronized java.util.Map.Entry<K, V> current() {
                        if (entry == null) {
                            throw new IllegalStateException();
                        }
                        return entry;
                    }

                    @Override
                    public K getKey() {
                        return current().getKey();
                    }

                    @Override
                    public V getValue() {
                        return current().getValue();
                    }

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public K next() {
                        entry = iterator.next();
                        return getKey();
                    }

                    @Override
                    public void remove() {
                        iterator.remove();
                        entry = null;
                    }

                    @Override
                    public synchronized void reset() {
                        iterator = entrySet.iterator();
                    }

                    @Override
                    public V setValue(final V value) {
                        return current().setValue(value);
                    }
                }
            """;

    /**
     * H3 template (CHECKED_ENTRY_SET): entry set view whose entries pass setValue through
     * checkSetValue.
     */
    private static final String CHECKED_ENTRY_SET_HELPER = """

                /**
                 * CHECKED_ENTRY_SET helper (no JDK owner): entry set view whose entries check \
            setValue, the
                 * AbstractInputCheckedMapDecorator.EntrySet contract. The element type is an \
            unbounded type variable so
                 * add(Object) erases without a checkcast bridge, as in Commons.
                 */
                private final class CheckedEntrySet<X> implements java.util.Set<X>, \
            java.io.Serializable {
                    @java.io.Serial
                    private static final long serialVersionUID = 1L;
                    @SuppressWarnings("serial")
                    private final java.util.Set<X> entries;

                    CheckedEntrySet(final java.util.Set<X> entries) {
                        this.entries = java.util.Objects.requireNonNull(entries, "collection");
                    }

                    @Override
                    public boolean add(final X object) {
                        return entries.add(object);
                    }

                    @Override
                    public boolean addAll(final java.util.Collection<? extends X> coll) {
                        return entries.addAll(coll);
                    }

                    @Override
                    public void clear() {
                        entries.clear();
                    }

                    @Override
                    public boolean contains(final Object object) {
                        return entries.contains(object);
                    }

                    @Override
                    public boolean containsAll(final java.util.Collection<?> coll) {
                        return entries.containsAll(coll);
                    }

                    @Override
                    public boolean equals(final Object object) {
                        return object == this || entries.equals(object);
                    }

                    @Override
                    public int hashCode() {
                        return entries.hashCode();
                    }

                    @Override
                    public boolean isEmpty() {
                        return entries.isEmpty();
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public java.util.Iterator<X> iterator() {
                        return (java.util.Iterator<X>) new \
            CheckedEntryIterator((java.util.Iterator<java.util.Map.Entry<K, V>>) \
            entries.iterator());
                    }

                    @Override
                    public boolean remove(final Object object) {
                        return entries.remove(object);
                    }

                    @Override
                    public boolean removeAll(final java.util.Collection<?> coll) {
                        return entries.removeAll(coll);
                    }

                    @Override
                    public boolean removeIf(final java.util.function.Predicate<? super X> filter) {
                        return entries.removeIf(filter);
                    }

                    @Override
                    public boolean retainAll(final java.util.Collection<?> coll) {
                        return entries.retainAll(coll);
                    }

                    @Override
                    public int size() {
                        return entries.size();
                    }

                    @Override
                    public Object[] toArray() {
                        final Object[] array = entries.toArray();
                        java.util.Arrays.setAll(array, i -> wrap(array[i]));
                        return array;
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> T[] toArray(final T[] array) {
                        final Object[] result = entries.toArray(array.length > 0
                                ? (Object[]) \
            java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), 0) : array);
                        java.util.Arrays.setAll(result, i -> wrap(result[i]));
                        if (result.length > array.length) {
                            return (T[]) result;
                        }
                        System.arraycopy(result, 0, array, 0, result.length);
                        if (array.length > result.length) {
                            array[result.length] = null;
                        }
                        return array;
                    }

                    @Override
                    public String toString() {
                        return entries.toString();
                    }

                    @SuppressWarnings("unchecked")
                    private Object wrap(final Object entry) {
                        return new CheckedEntry((java.util.Map.Entry<K, V>) entry);
                    }
                }

                /** CHECKED_ENTRY_SET helper: iterator of {@link CheckedEntrySet}; every returned \
            entry checks setValue. */
                private final class CheckedEntryIterator implements \
            java.util.Iterator<java.util.Map.Entry<K, V>> {
                    private final java.util.Iterator<java.util.Map.Entry<K, V>> iterator;

                    CheckedEntryIterator(final java.util.Iterator<java.util.Map.Entry<K, V>> \
            iterator) {
                        this.iterator = java.util.Objects.requireNonNull(iterator, "iterator");
                    }

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public java.util.Map.Entry<K, V> next() {
                        return new CheckedEntry(iterator.next());
                    }

                    @Override
                    public void remove() {
                        iterator.remove();
                    }
                }

                /** CHECKED_ENTRY_SET helper: boundary flyweight entry; setValue passes the value \
            lane first. */
                private final class CheckedEntry implements java.util.Map.Entry<K, V>, \
            org.apache.commons.collections4.KeyValue<K, V> {
                    private final java.util.Map.Entry<K, V> entry;

                    CheckedEntry(final java.util.Map.Entry<K, V> entry) {
                        this.entry = java.util.Objects.requireNonNull(entry, "entry");
                    }

                    @Override
                    public boolean equals(final Object object) {
                        return object == this || entry.equals(object);
                    }

                    @Override
                    public K getKey() {
                        return entry.getKey();
                    }

                    @Override
                    public V getValue() {
                        return entry.getValue();
                    }

                    @Override
                    public int hashCode() {
                        return entry.hashCode();
                    }

                    @Override
                    public V setValue(final V value) {
                        return entry.setValue(checkSetValue(value));
                    }

                    @Override
                    public String toString() {
                        return entry.toString();
                    }
                }
            """;

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
    static final class JdkStandard {
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

        JdkStandard(Path commonsSources) {
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
                    int k = chain.map(JdkStandard::typeIndex).orElse(-1);
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
                    t -> load(t).ifPresent(
                    c -> Arrays.stream(c.getClasses()).forEach(
                    m -> members.add(m.getSimpleName()))));
            Map<String, String> chosen = new java.util.TreeMap<>();
            counts.keySet().stream().collect(
                    Collectors.groupingBy(JdkStandard::simpleName, java.util.TreeMap::new,
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
                    && load("java.lang." + simple).isPresent();
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
                    Optional<List<String>> doc = factoryDoc(p, cls, m, noun);
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
        private Optional<List<String>> factoryDoc(Parsed p, com.sun.source.tree.ClassTree cls,
                com.sun.source.tree.MethodTree m, Noun noun) {
            String name = m.getName().toString();
            String kind = name.replaceFirst("^(predicated|synchronized|unmodifiable).*", "$1");
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
                if (!typeText.contains("Predicate") || lane.isEmpty()) {
                    return Optional.empty();
                }
                String what = lane.startsWith("key") ? "key" : lane.startsWith("value") ? "value"
                        : "element";
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

    /**
     * Classifier side-condition checks (refutations R4, R5, R6 of the PREDICATED skeptic): each
     * case runs one real or
     * synthetic atom through the same classification code and compares the category with the
     * expected one. A planted
     * defect in any guard method (ctorArgIsParam, superIsDelegate, unshadowed, lanesInOrder,
     * siblingIsSuper,
     * checkSetValueValidates) turns its case into FAIL.
     */
    private static final class RuleCheck {
        private static final String PKG = "org.apache.commons.collections4.";

        private RuleCheck() {
        }

        static int run(Path sources, Path out) throws IOException, ReflectiveOperationException {
            List<String> rows =
                    new ArrayList<>(
                    List.of("check\trefutation\tcommons\tinput\texpected\tactual\tresult"));
            String entrySet = "java.util.Set<java.util.Map.Entry<K, V>> entrySet()";
            rows.add(
                    real(sources, "R4_TRANSFORMING_HOOK", "R4", "map.TransformedMap", entrySet,
                    "MANUAL"));
            rows.add(
                    real(sources, "R4_VALIDATING_HOOK", "R4", "map.PredicatedMap", entrySet,
                    "CHECKED_ENTRY_SET"));
            rows.add(
                    ctor(sources, "R5_SELF_LOCK", "collection.SynchronizedCollection", 1,
                    "MANUAL"));
            rows.add(
                    ctor(sources, "R5_LOCK_PARAMETER", "collection.SynchronizedCollection", 2,
                    "LANES_CONSTRUCTOR"));
            rows.add(
                    synthetic(sources, "R6_SUPER_NOT_DELEGATE", "list.PredicatedList",
                    "boolean add(E arg0)",
                    "validate(object); return super.add(object);", "object", "MANUAL"));
            rows.add(
                    real(sources, "R6_SUPER_DELEGATE", "R6", "map.PredicatedMap",
                    "void putAll(java.util.Map<? extends K, ? extends V> arg0)",
                    "GUARD_DELEGATE/each-entry"));
            rows.add(
                    synthetic(sources, "R6_SHADOWED_ALIAS", "map.PredicatedMap",
                    "boolean containsKey(Object arg0)",
                    "return map.containsKey(map);", "map", "MANUAL"));
            rows.add(
                    real(sources, "R6_FIELD_ALIAS", "R6", "map.PredicatedMap",
                    "V put(K arg0, V arg1)", "GUARD_DELEGATE/entry"));
            rows.add(
                    synthetic(sources, "R6_LANES_SWAPPED", "map.PredicatedSortedMap",
                    "java.util.SortedMap<K, V> headMap(K arg0)",
                    "final SortedMap<K, V> map = getSortedMap().headMap(toKey); return new " +
                            "PredicatedSortedMap<>(map, valuePredicate, keyPredicate);",
                    "toKey", "MANUAL"));
            rows.add(
                    real(sources, "R6_LANES_IN_ORDER", "R6", "map.PredicatedSortedMap",
                    "java.util.SortedMap<K, V> headMap(K arg0)", "LANE_VIEW/self"));
            rows.add(
                    synthetic(sources, "R6_SIBLING_NOT_SUPER", "set.PredicatedSortedSet",
                    "java.util.SortedSet<E> headSet(E arg0)",
                    "final SortedSet<E> head = decorated().headSet(toElement); return new " +
                            "PredicatedNavigableSet<>(head, predicate);",
                    "toElement", "MANUAL"));
            rows.add(
                    real(sources, "R6_SIBLING_SUPER", "R6", "set.PredicatedNavigableSet",
                    "java.util.SortedSet<E> headSet(E arg0)", "LANE_VIEW/sibling"));
            Files.write(out, rows, StandardCharsets.UTF_8);
            long fail = rows.stream().filter(r -> r.endsWith("\tFAIL")).count();
            LOG.info(() -> "M3_RULE_CHECK checks=" + (rows.size() - 1) + " fail=" + fail);
            return fail == 0 ? 0 : 4;
        }

        private static M3DecoratorFill generator(Path sources, String fqn) throws IOException,
                ReflectiveOperationException {
            Class<?> c = Class.forName(PKG + fqn);
            String scaffold = synth(c, "M3" + c.getSimpleName(), true);
            M3DecoratorFill g = new M3DecoratorFill(sources, c, scaffold);
            g.atoms(scaffold);
            return g;
        }

        /**
         * One case: the classifier's category, or ERROR:<exception> when a (planted) defect makes
         * it throw.
         */
        interface Case {
            String category() throws IOException, ReflectiveOperationException;
        }

        private static String guarded(Case c) {
            try {
                return c.category();
            } catch (IOException | ReflectiveOperationException | RuntimeException e) {
                return "ERROR:" + e.getClass().getSimpleName();
            }
        }

        private static String real(Path sources, String id, String refutation, String fqn,
                String sig, String expected) {
            return row(id, refutation, fqn, sig, expected,
                    guarded(() -> generator(sources, fqn).classify(atom("public", sig)).category));
        }

        private static String synthetic(Path sources, String id, String fqn, String sig,
                String body, String names, String expected) {
            return row(id, "R6", fqn, body, expected, guarded(() -> {
                M3DecoratorFill g = generator(sources, fqn);
                g.declaringNow = g.commons;
                return g.shape(atom("public", sig), body + "\u0000" + names).category;
            }));
        }

        private static String ctor(Path sources, String id, String fqn, int arity,
                String expected) {
            return row(id, "R5", fqn, fqn + " constructor arity " + arity, expected, guarded(() -> {
                Class<?> c = Class.forName(PKG + fqn);
                M3DecoratorFill g =
                        new M3DecoratorFill(sources, c, synth(c, "M3" + c.getSimpleName(), false));
                g.laneArity = arity;
                g.lanesConstructor();
                return g.ctorError == null ? "LANES_CONSTRUCTOR" : "MANUAL";
            }));
        }

        private static String row(String id, String refutation, String fqn, String input,
                String expected, String actual) {
            return String.join("\t", id, refutation, fqn, input.replace('\t', ' '), expected,
                    actual, expected.equals(actual) ? "PASS" : "FAIL");
        }
    }
}
