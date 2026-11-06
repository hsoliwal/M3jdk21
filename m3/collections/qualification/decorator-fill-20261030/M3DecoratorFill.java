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
 * Pattern DECORATOR_FILL v1.6 (TmOP generator): turns one five-provider decorator scaffold into an
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
    static final Logger LOG = Logger.getLogger(M3DecoratorFill.class.getName());
    private static final String UNMOD = "org.apache.commons.collections4.Unmodifiable";
    private static final String GENERATOR =
            "m3/collections/qualification/decorator-fill-20261030/M3DecoratorFill.java";
    /** The pattern version the GENERATED block of every owned class names. */
    private static final String VERSION = "1.6";
    /** SFA: a Java string literal (data, never rewritten). */
    /** v1.6 INVERSE_CACHE body: a bidi map's inverse view, created once and linked back. */

    record Atom(String visibility, String signature, String name, List<String[]> params,
            boolean isStatic, String returnType) {
    }

    record Emit(String category, String body) {
    }

    private final Path sources;
    final Class<?> commons;
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
    /** v1.6: extra instance fields of the owned class (INVERSE_CACHE), emitted after backing. */
    private final List<String> fields = new ArrayList<>();
    /**
     * v1.3 LANES: enclosing generator of an inner helper (its lane members live there), else null.
     */
    private final M3DecoratorFill outer;
    /**
     * v1.3 LANES: arity of the self factory {@code new Self<>(p0, p1..)}; 0 = single-backing
     * constructor (v1.1/v1.2).
     */
    int laneArity;
    private final List<String> ctorStatements = new ArrayList<>();
    /**
     * Policy lane field -> M3 type, in constructor order; laneOrder[i - 1] is the lane fed by
     * constructor parameter i.
     */
    private final Map<String, String> laneTypes = new LinkedHashMap<>();
    private final List<String> laneOrder = new ArrayList<>();
    private String ctorParams = "";
    private String backingDecl = "";
    String ctorError;
    private boolean serialHook;
    private Set<String> surface = Set.of();
    final Map<String, String> members = new LinkedHashMap<>();
    private final Map<String, String> helpers = new LinkedHashMap<>();
    /**
     * v1.4 JDK_STANDARD: each class this generator emits (simple name) -> its Commons donor of
     * parameter names.
     */
    private final Map<String, Class<?>> donors = new java.util.TreeMap<>();
    Class<?> declaringNow;
    private final Map<String, String> sourceCache = new HashMap<>();

    M3DecoratorFill(Path sources, Class<?> commons, String scaffold) throws IOException {
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
            System.exit(M3DecoratorRuleCheck.run(Path.of(args[1]), Path.of(args[2])));
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
        M3DecoratorJdkStandard standard = new M3DecoratorJdkStandard(Path.of(args[0]));
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

    List<Atom> atoms(String scaffold) throws IOException {
        List<Atom> atoms = new ArrayList<>();
        M3DecoratorPatterns.METHOD.matcher(scaffold).results()
                .forEach(r -> atoms.add(atom(r.group(1), r.group(2))));
        atoms.stream().filter(a -> a.isStatic && a.params.size() == 1).findFirst()
                .ifPresent(a -> factoryParamType = a.params.get(0)[0]);
        surface = atoms.stream().map(Atom::name).collect(Collectors.toSet());
        lanes(atoms);
        return atoms;
    }

    private String fill(String scaffold) throws IOException {
        List<Atom> atoms = atoms(scaffold);
        StringBuilder out = new StringBuilder();
        Matcher m = M3DecoratorPatterns.METHOD.matcher(scaffold);
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

    static Atom atom(String visibility, String signature) {
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

    Emit classify(Atom a) throws IOException {
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

    static Optional<Class<?>> load(String name) {
        try {
            return Optional.of(Class.forName(name));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * v1.5: a simple class name as the Commons source that declares the current atom resolves it:
     * the decorator's own package first, then a single-type import of that source (for example
     * {@code set.TransformedSet} in {@code bag.TransformedBag}, which also declares the
     * {@code uniqueSet()} of its subclass {@code TransformedSortedBag}).
     */
    private Optional<Class<?>> imported(String simple) throws IOException {
        Optional<Class<?>> local = load(commons.getPackageName() + "." + simple);
        if (local.isPresent()) {
            return local;
        }
        Class<?> declaring = declaringNow == null ? commons : declaringNow;
        Matcher m = Pattern.compile("(?m)^import\\s+(org\\.apache\\.commons\\.collections4"
                + "\\.[\\w.]+\\." + Pattern.quote(simple) + ")\\s*;")
                .matcher(source(declaring).orElse(""));
        return m.find() ? load(m.group(1)) : Optional.empty();
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
        Matcher m = M3DecoratorPatterns.LITERAL.matcher(s);
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
        // v1.6 ALIAS without an accessor: a class outside the decorator chain whose only instance
        // field is a backing field (UnmodifiableTrie.delegate)
        List<java.lang.reflect.Field> own = Arrays.stream(commons.getDeclaredFields()).filter(
                x -> !Modifier.isStatic(x.getModifiers()) && !x.isSynthetic()).toList();
        if (aliases.isEmpty() && commons.getSuperclass() == Object.class && own.size() == 1
                && backingField(own.get(0).getName())) {
            aliases.add(own.get(0).getName());
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
                // v1.6: also through one local, 'final T t = (T) Objects.requireNonNull(..);
                // this.F = t;' (UnmodifiableTrie)
                return scope.length == 2
                        && Pattern.compile("this\\." + f + " = Objects\\.requireNonNull\\("
                        + "|(\\w+) = (\\([\\w.<>, ?]+\\) )?Objects\\.requireNonNull\\([^;]*\\);"
                        + "\\s*this\\." + f + " = \\1;")
                        .matcher(text.get().substring(scope[0], scope[1])).find();
            }
        }
        return false;
    }

    Emit shape(Atom a, String bodyAndNames) {
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
        Matcher validated = M3DecoratorPatterns.VALIDATE_GUARD.matcher(body);
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
        Matcher inverse = M3DecoratorPatterns.INVERSE_CACHE.matcher(body);
        if (inverse.matches() && names.isEmpty()) {
            return inverseCache(a, inverse.group(1), inverse.group(2)).orElse(
                    new Emit("MANUAL", body));
        }
        String wrapper = "(new \\w+<>|\\w+\\.\\w+|\\w+)";
        // v1.6: super.m() of this no-argument atom reads as decorated().m() when the superclass
        // body of m is the delegate (R6(1) guard)
        String superCall = "super." + a.name + "()";
        String source = body;
        try {
            if (names.isEmpty() && body.contains(superCall) && superDelegates(a)) {
                source = body.replace(superCall, "decorated()." + a.name + "()");
            }
        } catch (IOException e) {
            return new Emit("MANUAL", body + " (" + e + ")");
        }
        String inlined = source.replaceAll(
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
                : M3DecoratorPatterns.WRAP.getOrDefault(m.group(1) + ":" + m.group(2),
                        M3DecoratorPatterns.WRAP.get(m.group(1)));
        if (template == null && m.group(1).matches("Collections\\.unmodifiable\\w+")
                && importsJdkCollections()) {
            // v1.6: the Commons class already wraps with the JDK owner itself
            template = "java.util." + m.group(1) + "(%s)";
        }
        Matcher iterator = Pattern.compile("^Unmodifiable((?:Ordered)?MapIterator)"
                + "\\.unmodifiable\\1$").matcher(m.group(1));
        boolean view = template == null && iterator.matches()
                && resolvesTo("Unmodifiable" + iterator.group(1),
                "org.apache.commons.collections4.iterators.Unmodifiable" + iterator.group(1));
        if (view) {
            // v1.6 H4: no JDK owner for an unmodifiable MapIterator; the helper is emitted once
            String kind = iterator.group(1);
            helpers.put("Unmodifiable" + kind + "View",
                    M3DecoratorTemplates.UNMODIFIABLE_MAP_ITERATOR_HELPER.formatted(kind,
                    kind.startsWith("Ordered")
                            ? M3DecoratorTemplates.ORDERED_MAP_ITERATOR_MEMBERS : ""));
            template = "new Unmodifiable" + kind + "View<>(%s)";
        }
        if (template == null) {
            return new Emit("MANUAL", body);
        }
        String expr = template.replace("%s", delegated).replace("%b", "backing").replace("%c",
                a.name + "(" + args(a) + ")");
        return new Emit(view ? "UNMODIFIABLE_MAP_ITERATOR" : "WRAP", "return " + expr + ";");
    }

    /**
     * v1.6 INVERSE_CACHE guards: the view is this class, the Commons class declares the field
     * {@code inverse} as itself with the type parameters swapped, and the decorated call is
     * {@code inverseBidiMap()}. The scaffold signature keeps the Commons modifiers
     * ({@code synchronized}).
     */
    private Optional<Emit> inverseCache(Atom a, String view, String call) {
        String[] tp = typeParamNames();
        String swapped = tp.length == 2 ? tp[1] + ", " + tp[0] : "";
        boolean field = Arrays.stream(commons.getDeclaredFields()).anyMatch(
                x -> x.getName().equals("inverse") && x.getGenericType().getTypeName()
                .equals(commons.getName() + "<" + swapped + ">"));
        if (tp.length != 2 || !field || !view.equals(commons.getSimpleName())
                || !call.equals("inverseBidiMap")) {
            return Optional.empty();
        }
        if (fields.isEmpty()) {
            fields.add("    /** The inverse view, created once and linked back (INVERSE_CACHE). */"
                    + "\n"
                    + "    private " + m3Simple + "<" + swapped + "> inverse;\n");
        }
        return Optional.of(new Emit("INVERSE_CACHE", "if (inverse == null) {\n"
                + "            inverse = new " + m3Simple + "<>(backing." + call + "());\n"
                + "            inverse.inverse = this;\n"
                + "        }\n"
                + "        return inverse;"));
    }

    /** Whether the simple name resolves, as the declaring Commons source sees it, to fqn. */
    private boolean resolvesTo(String simple, String fqn) {
        try {
            return imported(simple).filter(c -> c.getName().equals(fqn)).isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    /** Whether the Commons source of the current atom imports {@code java.util.Collections}. */
    private boolean importsJdkCollections() {
        try {
            return source(declaringNow == null ? commons : declaringNow).filter(
                    s -> s.contains("\nimport java.util.Collections;")).isPresent();
        } catch (IOException e) {
            return false;
        }
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
        if (typeParamNames().length > 1) {
            // v1.6: a K, V owner (Unmodifiable map family): the factory parameter type as declared
            // and the null message of the Commons constructor chain
            String backing = backingType();
            String cast = factoryParamType.equals(backing) ? "" : "(" + backing + ") ";
            return new Emit("CONSTRUCTOR", "this.backing = " + cast
                    + "java.util.Objects.requireNonNull(arg0, " + nullMessage() + ");\u0000"
                    + factoryParamType);
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
        String[] tp = typeParamNames();
        return factoryParamType.replaceAll("<.*>", "")
                + (tp.length > 1 ? "<" + String.join(", ", tp) + ">" : "<E>");
    }

    /**
     * v1.6: the message literal of the first {@code Objects.requireNonNull(p, MSG)} in a
     * constructor of the Commons class chain ({@code "map"}, {@code "trie"}); SFA: the literal is
     * data, copied unchanged.
     */
    private String nullMessage() {
        try {
            for (Class<?> c = commons; c != null && c != Object.class; c = c.getSuperclass()) {
                Optional<String> text = source(c);
                int[] scope = text.isPresent() ? scope(text.get(), c) : new int[0];
                if (scope.length == 2) {
                    Matcher m = Pattern.compile("\\b" + c.getSimpleName()
                            + "\\([^)]*\\)\\s*\\{[^}]*?Objects\\.requireNonNull\\(\\w+, ("
                            + M3DecoratorPatterns.LIT
                            + ")\\)").matcher(text.get().substring(scope[0], scope[1]));
                    if (m.find()) {
                        return m.group(1);
                    }
                }
            }
        } catch (IOException e) {
            LOG.fine(() -> "no constructor source: " + e);
        }
        return "\"collection\"";
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

    /**
     * The GENERATED block comment of an owned class: provenance, how its atoms are emitted and an
     * optional extra
     * line. Long lines are wrapped later by {@link M3DecoratorJdkStandard#layout(String)}.
     */
    private String generated(String how, String extra) {
        return "/*\n * GENERATED by " + GENERATOR
                + " (pattern DECORATOR_FILL v" + VERSION + ") from the five-provider scaffold;\n"
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
                + " FACTORY_NEW, SYNC, GUARD, VIEW or UNMODIFIABLE_MAP_ITERATOR and emitted from a"
                + " template; no provider implementation code is copied.", "");
        return text.replaceFirst(M3DecoratorPatterns.GENERATED_BLOCK,
                Matcher.quoteReplacement(generated))
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
                                                "Object lock;\n" : "")
                                + String.join("", fields)))
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
    void lanesConstructor() throws IOException {
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
            Matcher m = M3DecoratorPatterns.CTOR_STMT.matcher(rest);
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
        Matcher lit = M3DecoratorPatterns.LITERAL.matcher(text);
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
        Matcher m = M3DecoratorPatterns.LANE_CHECK.matcher(rest);
        while (m.lookingAt()) {
            boolean lanesOk = laneTypes.containsKey(m.group(2))
                    && (m.group(1) == null || m.group(1).equals(m.group(2)))
                    && names.contains(m.group(3))
                            && Arrays.stream(
                            M3DecoratorPatterns.LITERAL.matcher(m.group(4)).replaceAll("\"\"")
                            .split(" \\+ "))
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
            m = M3DecoratorPatterns.LANE_CHECK.matcher(rest);
        }
        // v1.5: the transforming hook (TransformedMap.checkSetValue): the lane's result is returned
        Matcher hook = Pattern.compile("^return (\\w+)\\.apply\\((\\w+)\\);$").matcher(rest);
        if (out.isEmpty() && hook.matches() && isTransformerLane(hook.group(1))
                && names.contains(hook.group(2))
                && ps[names.indexOf(hook.group(2))].getTypeName().equals(returnType)) {
            return Optional.of("return " + hook.group(1) + ".apply("
                    + rename.get(hook.group(2)) + ");");
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
        return superDelegates(a)
                ? new Emit("DELEGATE", ret(a) + "backing." + a.name + "(" + args(a) + ");") :
                        new Emit("MANUAL", rest);
    }

    /**
     * Whether {@code super.m(..)} of atom a is the delegate: the nearest superclass body of m,
     * classified in its own class, passes the R6(1) guard.
     */
    private boolean superDelegates(Atom a) throws IOException {
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
        return superShape.filter(this::superIsDelegate).isPresent();
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
                helpers.put("EntrySetMapIterator", M3DecoratorTemplates.MAP_ITERATOR_HELPER);
                return Optional.of(
                        new Emit("MAP_ITERATOR_ADAPTER",
                        "return new EntrySetMapIterator<>(entrySet());"));
            }
            Matcher checked = M3DecoratorPatterns.CHECKED_ENTRY_SET.matcher(body);
            if (checked.matches()) {
                return Optional.of(
                        checkedEntrySet(a, checked.group(1)).orElse(new Emit("MANUAL", body)));
            }
            return transformShape(a, body, names);
        } catch (IOException e) {
            return Optional.of(new Emit("MANUAL", body + " (" + e + ")"));
        }
    }

    /**
     * v1.5: a policy lane of this class (or, for a nested helper, its owner) of Transformer type.
     * laneType() stores the deprecated Commons Transformer as its java.util.function.Function
     * supertype, so both spellings are a transformer lane.
     */
    private boolean isTransformerLane(String lane) {
        M3DecoratorFill o = outer != null ? outer : this;
        String t = o.laneTypes.get(lane);
        return t != null && (t.startsWith("org.apache.commons.collections4.Transformer<")
                || t.startsWith("java.util.function.Function<"));
    }

    /** The owner class's type parameter names (E, or K and V). */
    private String[] typeParamNames() {
        M3DecoratorFill o = outer != null ? outer : this;
        return Arrays.stream(o.commons.getTypeParameters())
                .map(java.lang.reflect.TypeVariable::getName).toArray(String[]::new);
    }

    /**
     * v1.5: {@code name} is a static factory of {@code c} whose whole body is the plain
     * constructor call over its parameters ({@code return new C<>(p0, p1);}), such as Commons
     * {@code transformingSet}: the same view as {@code new C<>(..)}.
     */
    private boolean plainNewFactory(Class<?> c, String name, int arity) throws IOException {
        Optional<Method> m = Arrays.stream(c.getDeclaredMethods())
                .filter(x -> x.getName().equals(name) && Modifier.isStatic(x.getModifiers())
                        && x.getParameterCount() == arity)
                .findFirst();
        if (m.isEmpty()) {
            return false;
        }
        Optional<String> b = memberBody(m.get());
        if (b.isEmpty()) {
            return false;
        }
        String body = b.get().substring(0, b.get().indexOf('\u0000'));
        String params = b.get().substring(b.get().indexOf('\u0000') + 1).replace(",", ", ");
        return body.equals("return new " + c.getSimpleName() + "<>(" + params + ");");
    }

    /**
     * v1.5 TRANSFORMED family. TRANSFORM_DELEGATE: parameters pass through a lane transform and
     * the call delegates, inline ({@code return decorated().m(transform(p), q);}) or assigned
     * first ({@code p = transform(p); decorated().m(p, q);}). The transform members are emitted
     * once per class (TRANSFORM_* lane members). TRANSFORMED_FACTORY: the static factory that
     * also transforms the existing contents (toArray, clear, add each; or transformMap, clear,
     * putAll), which FACTORY_NEW must never match.
     */
    private Optional<Emit> transformShape(Atom a, String body, List<String> names)
            throws IOException {
        if (Arrays.stream(typeParamNames()).count() == 0
                || !(isTransformerLane("transformer")
                || isTransformerLane("keyTransformer") && isTransformerLane("valueTransformer"))) {
            return Optional.empty();
        }
        if (a.isStatic) {
            return transformedFactory(a, body, names);
        }
        String call = body;
        Matcher asg = Pattern.compile("^((?:\\w+ = transform(?:Key|Value|Map)?\\(\\w+\\); )+)"
                + "((?:return )?decorated\\(\\)\\.\\w+\\([^()]*\\);)$").matcher(body);
        if (asg.matches()) {
            call = asg.group(2);
            for (String st : asg.group(1).trim().replaceAll(";$", "").split("; ")) {
                String p = st.substring(0, st.indexOf(" = "));
                String wrapped = st.substring(st.indexOf(" = ") + 3);
                if (!wrapped.endsWith("(" + p + ")") || !names.contains(p)) {
                    return Optional.empty();
                }
                call = call.replaceAll("\\b" + p + "\\b", Matcher.quoteReplacement(wrapped));
            }
        }
        String arg = "(?:[^(),]+|transform(?:Key|Value|Map)?\\(\\w+\\))";
        Matcher m = Pattern.compile("^(return )?decorated\\(\\)\\.(\\w+)\\((" + arg + "?(?:, "
                + arg + ")*)\\);$").matcher(call);
        if (!m.matches() || !m.group(2).equals(a.name)
                || (m.group(1) != null) == ret(a).isEmpty()) {
            return Optional.empty();
        }
        List<String> given = m.group(3).isEmpty() ? List.of() : List.of(m.group(3).split(", "));
        if (given.size() != names.size()) {
            return Optional.empty();
        }
        List<String> out = new ArrayList<>();
        Set<String> needed = new LinkedHashSet<>();
        for (int i = 0; i < names.size(); i++) {
            Matcher t = M3DecoratorPatterns.TRANSFORM_ARG.matcher(given.get(i));
            if (t.matches()) {
                if (!t.group(2).equals(names.get(i))) {
                    return Optional.empty();
                }
                String kind = t.group(1) == null ? "" : t.group(1);
                Optional<List<String>> h = transformHelpers(kind, a.params.get(i)[0]);
                if (h.isEmpty()) {
                    return Optional.empty();
                }
                needed.addAll(h.get());
                out.add("transform" + kind + "(arg" + i + ")");
            } else if (given.get(i).equals(names.get(i))) {
                out.add("arg" + i);
            } else {
                return Optional.empty();
            }
        }
        if (needed.isEmpty()) {
            return Optional.empty();
        }
        M3DecoratorFill o = outer != null ? outer : this;
        needed.forEach(h -> o.helpers.put(h, transformHelperText(h)));
        return Optional.of(new Emit("TRANSFORM_DELEGATE",
                ret(a) + "backing." + a.name + "(" + String.join(", ", out) + ");"));
    }

    /** The TRANSFORM_* lane members a wrapped parameter needs, by lane kind and parameter type. */
    private Optional<List<String>> transformHelpers(String kind, String paramType) {
        String[] tp = typeParamNames();
        switch (kind) {
            case "":
                if (!isTransformerLane("transformer") || tp.length != 1) {
                    return Optional.empty();
                }
                if (paramType.equals(tp[0])) {
                    return Optional.of(List.of("TRANSFORM_ELEMENT"));
                }
                return paramType.startsWith("java.util.Collection<")
                        ? Optional.of(List.of("TRANSFORM_ELEMENT", "TRANSFORM_COLLECTION"))
                        : Optional.empty();
            case "Key":
                return isTransformerLane("keyTransformer") && tp.length == 2
                        && paramType.equals(tp[0]) ? Optional.of(List.of("TRANSFORM_KEY"))
                        : Optional.empty();
            case "Value":
                return isTransformerLane("valueTransformer") && tp.length == 2
                        && paramType.equals(tp[1]) ? Optional.of(List.of("TRANSFORM_VALUE"))
                        : Optional.empty();
            case "Map":
                return isTransformerLane("keyTransformer") && isTransformerLane("valueTransformer")
                        && tp.length == 2 && paramType.startsWith("java.util.Map<")
                        ? Optional.of(List.of("TRANSFORM_KEY", "TRANSFORM_VALUE", "TRANSFORM_MAP"))
                        : Optional.empty();
            default:
                return Optional.empty();
        }
    }

    /**
     * The TRANSFORM_* lane member source (one blank line first), over the owner's type parameter
     * names.
     */
    private String transformHelperText(String key) {
        String[] tp = typeParamNames();
        String e = tp[0];
        String k = tp[0];
        String v = tp.length > 1 ? tp[1] : "V";
        return "\n" + switch (key) {
            case "TRANSFORM_ELEMENT" ->
                "    /** TRANSFORM lane member: one element through the {@code transformer}"
                + " lane. */\n"
                + "    private " + e + " transform(final " + e + " object) {\n"
                + "        return transformer.apply(object);\n    }\n";
            case "TRANSFORM_COLLECTION" ->
                "    /**\n     * TRANSFORM lane member: every element through the lane, in"
                + " iteration order, into a new list.\n     */\n"
                + "    private java.util.Collection<" + e + "> transform("
                + "final java.util.Collection<? extends " + e + "> coll) {\n"
                + "        final java.util.List<" + e + "> list = "
                + "new java.util.ArrayList<>(coll.size());\n"
                + "        for (final " + e + " item : coll) {\n"
                + "            list.add(transform(item));\n"
                + "        }\n        return list;\n    }\n";
            case "TRANSFORM_KEY" ->
                "    /** TRANSFORM lane member: a key through the {@code keyTransformer} lane;"
                + " identity without a lane. */\n"
                + "    private " + k + " transformKey(final " + k + " object) {\n"
                + "        return keyTransformer == null ? object"
                + " : keyTransformer.apply(object);\n"
                + "    }\n";
            case "TRANSFORM_VALUE" ->
                "    /** TRANSFORM lane member: a value through the {@code valueTransformer}"
                + " lane; identity without a lane. */\n"
                + "    private " + v + " transformValue(final " + v + " object) {\n"
                + "        return valueTransformer == null ? object"
                + " : valueTransformer.apply(object);\n"
                + "    }\n";
            case "TRANSFORM_MAP" ->
                "    /**\n     * TRANSFORM lane member: a copy of the map with both lanes"
                + " applied, in iteration order; the copy is a\n     * JDK LinkedHashMap (the"
                + " provider uses its own insertion-ordered map; both keep that order).\n"
                + "     */\n"
                + "    @SuppressWarnings(\"unchecked\")\n"
                + "    private java.util.Map<" + k + ", " + v + "> transformMap("
                + "final java.util.Map<? extends " + k + ", ? extends " + v + "> map) {\n"
                + "        if (map.isEmpty()) {\n            return (java.util.Map<" + k + ", " + v
                + ">) map;\n        }\n"
                + "        final java.util.Map<" + k + ", " + v + "> result = new"
                + " java.util.LinkedHashMap<>(map.size());\n"
                + "        for (final java.util.Map.Entry<? extends " + k + ", ? extends " + v
                + "> entry : map.entrySet()) {\n"
                + "            result.put(transformKey(entry.getKey()),"
                + " transformValue(entry.getValue()));\n        }\n        return result;\n    }\n";
            default -> throw new IllegalArgumentException(key);
        };
    }

    /**
     * TRANSFORMED_FACTORY: the Commons factory that decorates and then transforms the existing
     * contents through the backing (toArray, clear, add each transformed value; maps: transformMap,
     * clear, putAll). Emitted over the owned class and its backing field.
     */
    private Optional<Emit> transformedFactory(Atom a, String body, List<String> names) {
        String self = commons.getSimpleName();
        String tps = "<" + String.join(", ", typeParamNames()) + ">";
        Matcher coll = Pattern.compile("^final " + self + "<[^>]+> decorated = new " + self
                + "<>\\((\\w+), (\\w+)\\); if \\(!\\1\\.isEmpty\\(\\)\\) \\{ "
                + "(?:@SuppressWarnings\\(\"unchecked\"\\) )?final (\\w+)\\[\\] values = "
                + "\\(\\3\\[\\]\\) \\1\\.toArray\\(\\); \\1\\.clear\\(\\); "
                + "for \\(final \\3 value : values\\) \\{ "
                + "decorated\\.decorated\\(\\)\\.add\\(\\2\\.apply\\(value\\)\\); \\} \\}"
                + " return decorated;$").matcher(body);
        if (coll.matches() && names.equals(List.of(coll.group(1), coll.group(2)))
                && isTransformerLane("transformer")) {
            String e = coll.group(3);
            return Optional.of(new Emit("TRANSFORMED_FACTORY",
                    "final " + m3Simple + tps + " decorated = new " + m3Simple + "<>(arg0, arg1);\n"
                    + "        if (!arg0.isEmpty()) {\n"
                    + "            @SuppressWarnings(\"unchecked\")\n"
                    + "            final " + e + "[] values = (" + e + "[]) arg0.toArray();\n"
                    + "            arg0.clear();\n"
                    + "            for (final " + e + " value : values) {\n"
                    + "                decorated.backing.add(arg1.apply(value));\n"
                    + "            }\n        }\n        return decorated;"));
        }
        Matcher map = Pattern.compile("^final " + self + "<[^>]+> decorated = new " + self
                + "<>\\((\\w+), (\\w+), (\\w+)\\); if \\(!\\1\\.isEmpty\\(\\)\\) \\{ "
                + "final Map<K, V> transformed = decorated\\.transformMap\\(\\1\\); "
                + "decorated\\.clear\\(\\); "
                + "decorated\\.decorated\\(\\)\\.putAll\\(transformed\\); \\} return decorated;$")
                .matcher(body);
        if (map.matches() && names.equals(List.of(map.group(1), map.group(2), map.group(3)))
                && isTransformerLane("keyTransformer") && isTransformerLane("valueTransformer")) {
            List.of("TRANSFORM_KEY", "TRANSFORM_VALUE", "TRANSFORM_MAP")
                    .forEach(h -> helpers.put(h, transformHelperText(h)));
            return Optional.of(new Emit("TRANSFORMED_FACTORY",
                    "final " + m3Simple + tps + " decorated = new " + m3Simple
                    + "<>(arg0, arg1, arg2);\n"
                    + "        if (!arg0.isEmpty()) {\n"
                    + "            final java.util.Map" + tps + " transformed = "
                    + "decorated.transformMap(arg0);\n"
                    + "            decorated.clear();\n"
                    + "            decorated.backing.putAll(transformed);\n"
                    + "        }\n        return decorated;"));
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
        helpers.put("CheckedEntrySet", M3DecoratorTemplates.CHECKED_ENTRY_SET_HELPER);
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
            // v1.5: the class's own, or a same-package sibling's, plain-constructor factory
            // (Commons transformingX) is the same view as new X<>(..)
            int arity = lanes.size() + 1;
            if (plainNewFactory(commons, w, arity)) {
                return new Emit("LANE_VIEW/self", "return new " + m3Simple + "<>(" + delegated
                        + ");");
            }
            Matcher sf = Pattern.compile("^(\\w+)\\.(\\w+)$").matcher(w);
            if (sf.matches()) {
                Optional<Class<?>> sc = imported(sf.group(1))
                        .filter(c -> !Modifier.isAbstract(c.getModifiers()));
                if (sc.isPresent() && plainNewFactory(sc.get(), sf.group(2), arity)) {
                    Optional<String> h = siblingHelper(sc.get(), lanes);
                    if (h.isPresent()) {
                        return new Emit("LANE_VIEW/sibling", "return new " + h.get() + "<>("
                                + delegated + ");");
                    }
                }
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
    static String synth(Class<?> c, String name, boolean statics) {
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

}
