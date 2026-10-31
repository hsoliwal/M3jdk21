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

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Pattern DECORATOR_FILL v1 (TmOP generator): turns one five-provider decorator scaffold into an owned decorator.
 * Every scaffold method is one atom. The atom is classified against Apache Commons Collections 4.5.0, which must be
 * on the class path, together with its unpacked sources:
 * <ul>
 * <li>{@code OBJECT}: the declaring class is {@code Object}, so the method is dropped and the inherited one is kept.
 * <li>{@code DEFAULT}: the declaring class is an interface, so {@code Iface.super.m(..)} is emitted. The same default chain runs.
 * <li>{@code THROW}, {@code DELEGATE}, {@code EQUALS}, {@code WRAP} or {@code FACTORY}: the method body is read from the
 * declaring Commons class and classified by its shape. A template is emitted; no provider body is copied.
 * <li>{@code MANUAL}: any other shape. The class is not written and the atom is reported, so a gap is never guessed.
 * </ul>
 * Usage: {@code java -cp commons-collections4-4.5.0.jar M3DecoratorFill.java <sources dir> <scaffold.java> <commons fqn>
 * <out.java> <atoms.tsv>}. Exit 0 = written, 3 = MANUAL atoms reported. The atoms ledger is appended.
 */
public final class M3DecoratorFill {
    private static final Logger LOG = Logger.getLogger(M3DecoratorFill.class.getName());
    private static final String UNMOD = "org.apache.commons.collections4.Unmodifiable";
    private static final String GENERATOR = "m3/collections/qualification/decorator-fill-20261030/M3DecoratorFill.java";
    private static final Pattern METHOD = Pattern.compile(
            "(?m)^    (public|private) (.+) \\{\\R        throw new UnsupportedOperationException\\(M3_SCAFFOLD_MESSAGE\\);\\R    }\\R");
    /** WRAP table: Commons wrapper factory -> owner expression; %s is the delegated call, %b the backing. */
    private static final Map<String, String> WRAP = Map.of(
            "UnmodifiableIterator.unmodifiableIterator:iterator", "java.util.Collections.unmodifiableCollection(%b).iterator()",
            "UnmodifiableIterator.unmodifiableIterator:descendingIterator",
            "java.util.Collections.unmodifiableNavigableSet(%b).descendingIterator()",
            "UnmodifiableListIterator.unmodifiableListIterator:listIterator", "java.util.Collections.unmodifiableList(%b).%c",
            "UnmodifiableSortedSet.unmodifiableSortedSet", "java.util.Collections.unmodifiableSortedSet(%s)",
            "UnmodifiableNavigableSet.unmodifiableNavigableSet", "java.util.Collections.unmodifiableNavigableSet(%s)",
            "UnmodifiableSet.unmodifiableSet", "java.util.Collections.unmodifiableSet(%s)",
            "UnmodifiableList.unmodifiableList", "java.util.Collections.unmodifiableList(%s)");

    record Atom(String visibility, String signature, String name, List<String[]> params, boolean isStatic, String returnType) {
    }

    record Emit(String category, String body) {
    }

    private final Path sources;
    private final Class<?> commons;
    private final String m3Simple;
    private final List<String> directInterfaces;
    private final List<String> ledger = new ArrayList<>();
    private String factoryParamType;
    /** SYNC lane: monitor expression; "this" for the owned class (Commons lock == this), "lock" inside the view helper. */
    private final String lockExpr;
    /** VIEW: the Commons lock-sharing Set view class (SynchronizedBag$SynchronizedBagSet, SynchronizedMultiSet$SynchronizedSet). */
    private Class<?> syncView;
    private final java.util.Set<String> accessors = new java.util.HashSet<>();

    private M3DecoratorFill(Path sources, Class<?> commons, String scaffold) throws IOException {
        this(sources, commons, scaffold, "this");
    }

    private M3DecoratorFill(Path sources, Class<?> commons, String scaffold, String lockExpr) throws IOException {
        this.lockExpr = lockExpr;
        this.sources = sources;
        this.commons = commons;
        accessors();
        this.m3Simple = find(scaffold, "public final class (\\w+)").orElseThrow();
        this.directInterfaces = Arrays.stream(find(scaffold, "(?s)implements(.+?)\\{").orElseThrow().split(",\\s*(?![^<]*>)"))
                .map(s -> s.trim().replaceAll("<.*", "")).filter(s -> !s.isEmpty()).toList();
    }

    public static void main(String[] args) throws IOException, ReflectiveOperationException {
        String scaffold = Files.readString(Path.of(args[1]), StandardCharsets.UTF_8);
        M3DecoratorFill fill = new M3DecoratorFill(Path.of(args[0]), Class.forName(args[2]), scaffold);
        String owned = fill.fill(scaffold);
        Files.write(Path.of(args[4]), fill.ledger, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
        long manual = fill.ledger.stream().filter(r -> r.contains("\tMANUAL\t")).count();
        LOG.info(() -> "M3_DECORATOR_FILL class=" + fill.m3Simple + " atoms=" + fill.ledger.size() + " manual=" + manual);
        if (manual > 0) {
            System.exit(3);
        }
        Files.writeString(Path.of(args[3]), owned, StandardCharsets.UTF_8);
    }

    private String fill(String scaffold) throws IOException {
        List<Atom> atoms = new ArrayList<>();
        METHOD.matcher(scaffold).results().forEach(r -> atoms.add(atom(r.group(1), r.group(2))));
        atoms.stream().filter(a -> a.isStatic && a.params.size() == 1).findFirst()
                .ifPresent(a -> factoryParamType = a.params.get(0)[0]);
        StringBuilder out = new StringBuilder();
        Matcher m = METHOD.matcher(scaffold);
        int i = 0;
        while (m.find()) {
            Atom a = atoms.get(i++);
            Emit e = a.name.equals(m3Simple) ? constructor() : classify(a);
            ledger.add(String.join("\t", m3Simple, a.signature, e.category, e.body.replace('\n', ' ').replace('\u0000', ' ')));
            m.appendReplacement(out, Matcher.quoteReplacement(e.category.equals("OBJECT") ? "" : render(a, e)));
        }
        m.appendTail(out);
        String text = syncView == null ? out.toString() : out.toString().replaceFirst("}\\s*$", Matcher.quoteReplacement(helper() + "}\n"));
        return ledger.stream().anyMatch(r -> r.contains("\tMANUAL\t")) ? text : header(text);
    }

    /**
     * VIEW helper: a lock-sharing Set view. No JDK owner fits (java.util.Collections.synchronizedSet(Set, Object) is
     * package-private, and the public one locks on itself and synchronizes forEach), so a private static nested class is
     * emitted. Its atoms are the public methods declared by the Commons lock owner (SynchronizedCollection), synthesized
     * into a scaffold and filled by this same generator with the lock lane explicit; defaults stay inherited.
     */
    private String helper() throws IOException {
        StringBuilder s = new StringBuilder("public final class SyncSet<E>\n        implements java.util.Set<E>, java.io.Serializable {\n"
                + "    @java.io.Serial\n    private static final long serialVersionUID = 0L;\n\n"
                + "    // Planned lanes (decorator)\n    private transient Object backing;\n    private transient int state;\n\n"
                + "    private SyncSet() {\n        throw new UnsupportedOperationException(M3_SCAFFOLD_MESSAGE);\n    }\n");
        Arrays.stream(syncView.getSuperclass().getDeclaredMethods())
                .filter(x -> java.lang.reflect.Modifier.isPublic(x.getModifiers()) && !java.lang.reflect.Modifier.isStatic(x.getModifiers()))
                .filter(x -> !x.isSynthetic() && !x.isBridge()).sorted(java.util.Comparator.comparing(Method::toGenericString))
                .forEach(x -> s.append("\n    public ").append(signature(x)).append(" {\n        throw new UnsupportedOperationException(M3_SCAFFOLD_MESSAGE);\n    }\n"));
        String scaffold = s.append("}\n").toString();
        M3DecoratorFill view = new M3DecoratorFill(sources, syncView, scaffold, "lock");
        view.factoryParamType = "java.util.Set<E>";
        String text = view.fill(scaffold).replace("public final class SyncSet<E>", "private static final class SyncSet<E>");
        view.ledger.forEach(r -> ledger.add(m3Simple + "$" + r));
        return "\n    /** Lock-sharing Set view (VIEW helper): no JDK owner shares the caller's lock; see DECORATOR_FILL SYNC. */\n"
                + text.lines().map(l -> l.isEmpty() ? l : "    " + l).collect(Collectors.joining("\n")) + "\n";
    }

    private static String signature(Method x) {
        String typeParams = x.getTypeParameters().length == 0 ? ""
                : Arrays.stream(x.getTypeParameters()).map(java.lang.reflect.TypeVariable::getName).collect(Collectors.joining(", ", "<", "> "));
        java.lang.reflect.Type[] ps = x.getGenericParameterTypes();
        return typeParams + x.getGenericReturnType().getTypeName() + " " + x.getName() + "(" + java.util.stream.IntStream.range(0, ps.length)
                .mapToObj(i -> ps[i].getTypeName() + " arg" + i).collect(Collectors.joining(", ")) + ")";
    }

    private static Atom atom(String visibility, String signature) {
        int open = signature.indexOf('(');
        String head = signature.substring(0, open);
        String name = head.substring(head.lastIndexOf(' ') + 1);
        String paramText = signature.substring(open + 1, signature.lastIndexOf(')'));
        List<String[]> params = paramText.isBlank() ? List.of() : Arrays.stream(paramText.split(",\\s*(?![^<]*>)"))
                .map(p -> new String[] {p.substring(0, p.lastIndexOf(' ')), p.substring(p.lastIndexOf(' ') + 1)}).toList();
        boolean isStatic = head.startsWith("static ");
        String noStatic = isStatic ? head.substring(7) : head;
        String noTypeParams = noStatic.startsWith("<") ? noStatic.substring(closing(noStatic) + 2) : noStatic;
        String returnType = noTypeParams.equals(name) ? "" : noTypeParams.substring(0, noTypeParams.length() - name.length() - 1);
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
        return body(a, declaring).map(b -> shape(a, b)).orElse(new Emit("MANUAL", "no source body in " + declaring.getName()));
    }

    private Optional<Method> resolve(Atom a) {
        List<String> erased = a.params.stream().map(p -> erase(p[0])).toList();
        return Arrays.stream(commons.getMethods()).filter(m -> !m.isBridge() && m.getName().equals(a.name))
                .filter(m -> Arrays.stream(m.getParameterTypes()).map(Class::getTypeName).toList().equals(erased))
                .min((x, y) -> x.getReturnType().isAssignableFrom(y.getReturnType()) ? 1 : -1);
    }

    private static String erase(String type) {
        String raw = type.replaceAll("<.*>", "").replace("...", "[]");
        String base = raw.replace("[]", "");
        String dims = raw.substring(base.length());
        String qualified = base.matches("[A-Z][A-Z0-9]*") ? "java.lang.Object"
                : base.contains(".") || base.matches("int|long|short|byte|char|boolean|float|double") ? base : "java.lang." + base;
        return qualified + dims;
    }

    private Emit defaultCall(Atom a, Method m) {
        Class<?> declaring = m.getDeclaringClass();
        String iface = directInterfaces.stream().map(M3DecoratorFill::load).flatMap(Optional::stream)
                .filter(declaring::isAssignableFrom).filter(c -> declares(c, m, declaring))
                .reduce((x, y) -> x.isAssignableFrom(y) ? y : x).map(Class::getName).orElse(declaring.getName());
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

    private Optional<String> body(Atom a, Class<?> declaring) throws IOException {
        Path src = sources.resolve(declaring.getName().replace('.', '/') + ".java");
        if (!Files.exists(src)) {
            return Optional.empty();
        }
        String text = Files.readString(src, StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
        Matcher m = Pattern.compile("[\\s>]" + a.name + "\\(([^)]*)\\)[^;{]*\\{").matcher(text);
        while (m.find()) {
            String[] ps = m.group(1).isBlank() ? new String[0] : m.group(1).split(",(?![^<]*>)");
            if (ps.length == a.params.size() && sameSimpleTypes(ps, a)) {
                return Optional.of(block(text, m.end() - 1) + "\u0000" + names(ps));
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
        return Arrays.stream(ps).map(p -> p.trim().substring(p.trim().lastIndexOf(' ') + 1)).collect(Collectors.joining(","));
    }

    private static String block(String text, int brace) {
        int depth = 0;
        for (int i = brace; i < text.length(); i++) {
            depth += text.charAt(i) == '{' ? 1 : text.charAt(i) == '}' ? -1 : 0;
            if (depth == 0) {
                return text.substring(brace + 1, i).replaceAll("\\s+", " ").trim();
            }
        }
        return "";
    }

    /**
     * ACCESSOR rule: a no-argument, non-public method of the Commons class chain whose normalized body is
     * {@code return [(T) ][super.]decorated();} or {@code return collection;} only re-types the decorated owner
     * (getBag, getSortedBag, decorated overrides). Calls to it are rewritten to {@code decorated()} before shape matching.
     */
    private void accessors() throws IOException {
        for (Class<?> c = commons; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getParameterCount() == 0 && !java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
                    Atom probe = new Atom("protected", m.getName() + "()", m.getName(), List.of(), false, "");
                    Optional<String> b = body(probe, c).map(s -> s.substring(0, s.indexOf('\u0000')));
                    if (b.filter(s -> s.matches("return (\\([\\w.<>, ?]+\\) )?(super\\.)?(decorated\\(\\)|collection);")).isPresent()) {
                        accessors.add(m.getName());
                    }
                }
            }
        }
    }

    private Emit shape(Atom a, String bodyAndNames) {
        String body = bodyAndNames.substring(0, bodyAndNames.indexOf('\u0000')).replaceAll("\\.<[\\w, ]+>(\\w+)\\(", ".$1(");
        for (String accessor : accessors) {
            body = body.replaceAll("\\b" + accessor + "\\(\\)", "decorated()");
        }
        List<String> names = bodyAndNames.endsWith("\u0000") ? List.of()
                : List.of(bodyAndNames.substring(bodyAndNames.indexOf('\u0000') + 1).split(","));
        return shape(a, body, names);
    }

    private Emit shape(Atom a, String body, List<String> names) {
        String call = String.join(", ", names);
        String self = commons.getSimpleName();
        if (a.isStatic && names.size() == 1 && body.equals("return new " + self + "<>(" + call + ");")) {
            return new Emit("FACTORY_NEW", "return new " + m3Simple + "<>(arg0);");
        }
        Matcher guard = Pattern.compile("^if \\((\\w+) == this\\) \\{ return true; \\} (.+)$").matcher(body);
        if (guard.matches() && names.contains(guard.group(1)) && !isEqualsShape(body, names)) {
            Emit rest = shape(a, guard.group(2), names);
            return rest.category.equals("MANUAL") ? new Emit("MANUAL", body)
                    : new Emit("GUARD+" + rest.category, "if (arg" + names.indexOf(guard.group(1)) + " == this) {\n            return true;\n"
                            + "        }\n        " + rest.body);
        }
        Matcher sync = Pattern.compile("^synchronized \\(lock\\) \\{ (.+) \\}$").matcher(body);
        if (sync.matches()) {
            Emit inner = shape(a, sync.group(1), names);
            return inner.category.equals("MANUAL") ? new Emit("MANUAL", body)
                    : new Emit("SYNC(" + inner.category + ")", "synchronized (" + lockExpr + ") {\n            "
                            + inner.body.replace("\n", "\n    ") + "\n        }");
        }
        Matcher view = Pattern.compile("^final [\\w.]+(<.*>)? (\\w+) = decorated\\(\\)\\.(\\w+)\\(\\); return new (\\w+)(<>)?\\(\\2, lock\\);$")
                .matcher(body);
        if (view.matches() && names.isEmpty() && syncView(view.group(4)).isPresent()) {
            syncView = syncView(view.group(4)).get();
            return new Emit("VIEW", "return new SyncSet<>(backing." + view.group(3) + "(), " + lockExpr + ");");
        }
        if (body.equals("throw new UnsupportedOperationException();")) {
            return new Emit("THROW", "throw new UnsupportedOperationException();");
        }
        if (body.matches("(return )?decorated\\(\\)\\." + a.name + "\\(" + Pattern.quote(call) + "\\);")) {
            return new Emit("DELEGATE", ret(a) + "backing." + a.name + "(" + args(a) + ");");
        }
        String n0 = names.isEmpty() ? "" : names.get(0);
        if (names.size() == 1 && (body.equals("return " + n0 + " == this || decorated().equals(" + n0 + ");")
                || body.equals("if (" + n0 + " == this) { return true; } return decorated().equals(" + n0 + ");"))) {
            return new Emit("EQUALS", "return arg0 == this || backing.equals(arg0);");
        }
        if (a.isStatic && body.contains("instanceof Unmodifiable") && body.contains("return new " + self + "<>(" + call + ");")) {
            return factory(a);
        }
        return wrap(a, body, names);
    }

    private static boolean isEqualsShape(String body, List<String> names) {
        String n0 = names.isEmpty() ? "" : names.get(0);
        return names.size() == 1 && body.equals("if (" + n0 + " == this) { return true; } return decorated().equals(" + n0 + ");");
    }

    /**
     * VIEW rule guard: the named Commons member class must implement Set, extend the class that declares the lock lane
     * and declare no method of its own, so its behaviour is exactly the synchronized-collection atoms over (set, lock).
     */
    private Optional<Class<?>> syncView(String simple) {
        for (Class<?> c = commons; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Class<?> member : c.getDeclaredClasses()) {
                if (member.getSimpleName().equals(simple) && java.util.Set.class.isAssignableFrom(member)
                        && declaresLock(member.getSuperclass()) && member.getDeclaredMethods().length == 0) {
                    return Optional.of(member);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean declaresLock(Class<?> c) {
        return c != null && Arrays.stream(c.getDeclaredFields()).anyMatch(f -> f.getName().equals("lock") && f.getType() == Object.class);
    }

    private Emit wrap(Atom a, String body, List<String> names) {
        String wrapper = "(new \\w+<>|\\w+\\.\\w+|\\w+)";
        String inlined = body.replaceAll("^final \\S+(<.*?>)? (\\w+) = (decorated\\(\\)\\.\\w+\\([^;]*\\)); return " + wrapper
                + "\\(\\2\\);$", "return $4($3);");
        Matcher m = Pattern.compile("^return " + wrapper + "\\(decorated\\(\\)\\.(\\w+)\\(([^()]*)\\)\\);$").matcher(inlined);
        if (!m.matches() || !m.group(3).equals(String.join(", ", names))) {
            return new Emit("MANUAL", body);
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
        String expr = template.replace("%s", delegated).replace("%b", "backing").replace("%c", a.name + "(" + args(a) + ")");
        return new Emit("WRAP", "return " + expr + ";");
    }

    private String m3Factory() {
        String s = commons.getSimpleName();
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private Emit factory(Atom a) {
        String param = a.params.get(0)[0];
        String cast = param.equals(a.returnType) ? "" : "(" + a.returnType + ") ";
        return new Emit("FACTORY", "if (arg0 instanceof " + UNMOD + ") {\n            return " + cast + "arg0;\n        }\n"
                + "        return new " + m3Simple + "<>(arg0);");
    }

    private Emit constructor() {
        if (lockExpr.equals("lock")) {
            return new Emit("CONSTRUCTOR_LOCK", "this.backing = java.util.Objects.requireNonNull(arg0, \"collection\");\n"
                    + "        this.lock = java.util.Objects.requireNonNull(arg1, \"lock\");\u0000" + factoryParamType);
        }
        if (factoryParamType == null) {
            return new Emit("MANUAL", "no single-argument static factory: constructor shape outside DECORATOR_FILL v1");
        }
        String typeVar = factoryParamType.replaceAll(".*<(\\? extends )?([A-Z]\\w*)>.*", "$2");
        String param = factoryParamType.replace("<" + (factoryParamType.contains("? extends") ? "? extends " : "") + typeVar + ">",
                factoryParamType.contains("? extends") ? "<? extends E>" : "<E>");
        String backing = backingType();
        String cast = param.equals(backing) ? "" : "(" + backing + ") ";
        return new Emit("CONSTRUCTOR", "this.backing = " + cast + "java.util.Objects.requireNonNull(arg0, \"collection\");\u0000" + param);
    }

    private String backingType() {
        return factoryParamType.replaceAll("<.*>", "") + "<E>";
    }

    private String render(Atom a, Emit e) {
        if (e.category.startsWith("CONSTRUCTOR")) {
            String[] bp = e.body.split("\u0000");
            String suppress = bp[0].contains(") java.util") ? "    @SuppressWarnings(\"unchecked\")\n" : "";
            String lockParam = e.category.equals("CONSTRUCTOR_LOCK") ? ", final Object arg1" : "";
            return suppress + "    private " + m3Simple + "(final " + bp[1] + " arg0" + lockParam + ") {\n        " + bp[0] + "\n    }\n";
        }
        boolean override = !a.isStatic && overrides(a);
        boolean unchecked = e.category.equals("FACTORY") && e.body.contains("return (");
        // MODIFIER rule: the Commons method's ACC_SYNCHRONIZED flag (SynchronizedSortedBag comparator/first/last) is carried over.
        boolean monitor = !a.isStatic && resolve(a).filter(m -> java.lang.reflect.Modifier.isSynchronized(m.getModifiers())).isPresent();
        return (override ? "    @Override\n" : "") + (unchecked ? "    @SuppressWarnings(\"unchecked\")\n" : "") + "    " + a.visibility + " "
                + (monitor ? "synchronized " : "") + a.signature + " {\n        " + e.body + "\n    }\n";
    }

    private boolean overrides(Atom a) {
        return resolve(a).map(m -> java.util.stream.Stream.concat(directInterfaces.stream().map(M3DecoratorFill::load)
                .flatMap(Optional::stream), java.util.stream.Stream.of(Object.class)).anyMatch(c -> has(c, m))).orElse(false);
    }

    private static boolean has(Class<?> c, Method m) {
        try {
            c.getMethod(m.getName(), m.getParameterTypes());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private String header(String text) {
        String generated = "/*\n * GENERATED by " + GENERATOR + " (pattern DECORATOR_FILL v1) from the five-provider scaffold;\n"
                + " * regenerate, do not hand-edit. Behaviour mirrors " + commons.getName() + "\n"
                + " * from Apache Commons Collections 4.5.0 (Apache-2.0): each method atom is classified as THROW, DELEGATE, EQUALS,\n"
                + " * WRAP, DEFAULT, FACTORY, FACTORY_NEW, SYNC, GUARD or VIEW and emitted from a template; no provider implementation\n"
                + " * code is copied.\n */";
        return text.replaceFirst("(?s)/\\*\n \\* GENERATED by .*? \\*/", Matcher.quoteReplacement(generated))
                .replaceFirst("(?s) \\* SCAFFOLD, not an owned implementation: declares the public surface of\n \\* (\\{@code [^}]+})"
                        + " and every body throws \\{@link UnsupportedOperationException}\\.",
                        Matcher.quoteReplacement(" * Owned decorator with the public contract of ") + "$1.")
                .replace("{@code Object backing}, {@code int state}", "{@code backing}")
                .replaceFirst("    private static final String M3_SCAFFOLD_MESSAGE = .*\\R", "")
                .replaceFirst("(?s)    // Planned lanes \\(decorator\\).*?private transient int state;\\R",
                        Matcher.quoteReplacement("    /** The decorated owner: one reference, elements are never copied. */\n"
                                + "    @SuppressWarnings(\"serial\")\n    private final " + backingType() + " backing;\n"
                                + (lockExpr.equals("lock") ? "    /** The monitor shared with the owning decorator. */\n"
                                        + "    @SuppressWarnings(\"serial\")\n    private final Object lock;\n" : "")))
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
}
