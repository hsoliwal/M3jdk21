// SPDX-License-Identifier: Apache-2.0
import java.lang.reflect.*;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;

/** Whole baseline/candidate classes and their real source dependencies; no framework stubs. */
public final class PageContract {
    private static final String PKG = "org.eclipse.nebula.widgets.pagination.";
    private static int assertions;
    private PageContract() { }
    public static void verify(Map<String, String> baseline, Map<String, String> candidate, Path evidence) throws Exception {
        Path oldDir = compile(baseline, evidence.resolve("baseline")), newDir = compile(candidate, evidence.resolve("candidate"));
        try (URLClassLoader before = loader(oldDir); URLClassLoader after = loader(newDir)) {
            Random random = new Random(0x4d33504147454cL);
            int[] edges = {Integer.MIN_VALUE, -100, -1, 0, 1, 2, 10, 100, Integer.MAX_VALUE};
            for (int offset : edges) for (int size : edges) for (int length : new int[]{0, 1, 9, 10, 23}) {
                compare(page(before, offset, size, length, 0, 0, false), page(after, offset, size, length, 0, 0, false));
            }
            for (int i = 0; i < 4000; i++) {
                int offset = random.nextInt(150)-20, size = random.nextInt(30)-3, length = random.nextInt(80);
                int direction = new int[]{0, 128, 1024}[i % 3], fault = i % 17;
                compare(page(before, offset, size, length, direction, fault, i % 2 == 0), page(after, offset, size, length, direction, fault, i % 2 == 0));
            }
            Object[] values = {null, -1, 0, 1, "", "same", "other", new Bean(1), new Bean(3), new Object(), new BrokenBean()};
            for (Object a : values) for (Object b : values) for (String property : Arrays.asList(null, "value", "missing")) for (int direction : new int[]{0, 128, 1024}) {
                compare(comparator(before,a,b,property,direction), comparator(after,a,b,property,direction));
            }
            for (String name : List.of("collections.PageListHelper", "collections.BeanComparator", "collections.PageResult", "PageableController")) {
                List<String> oldApi = api(before.loadClass(PKG + name)), newApi = api(after.loadClass(PKG + name));
                compare(oldApi, newApi);
                Files.writeString(evidence.resolve(name.replace('.', '_') + "-api.txt"), String.join("\n", newApi) + "\n");
            }
        }
        String report = "PASS wholeClassDifferentialAssertions=" + assertions + " sourceClosure=8 noStubs=true publicApiSets=4\n";
        System.out.print(report); Files.writeString(evidence.resolve("contracts.txt"), report);
    }

    private static Path compile(Map<String, String> sources, Path work) throws Exception {
        Path out = work.resolve("classes"); Files.createDirectories(out);
        List<String> args = new ArrayList<>(List.of("--release", "21", "-encoding", "UTF-8", "-proc:none", "-classpath", System.getProperty("java.class.path"), "-d", out.toString()));
        for (var entry : sources.entrySet()) {
            Path file = work.resolve("src").resolve(entry.getKey()); Files.createDirectories(file.getParent()); Files.writeString(file, entry.getValue()); args.add(file.toString());
        }
        Path probe = work.resolve("src/Probe.java");
        Files.writeString(probe, """
                import org.eclipse.nebula.widgets.pagination.PageableController;
                public final class Probe extends PageableController {
                    public int offset, size, direction; public String trace = "";
                    public Probe(int offset, int size, int direction) { this.offset=offset;this.size=size;this.direction=direction; }
                    @Override public int getSortDirection() { trace+="direction;"; return direction; }
                    @Override public int getPageSize() { trace+="size;"; return size; }
                    @Override public int getPageOffset() { trace+="offset;"; return offset; }
                    @Override public String getSortPropertyName() { trace+="property;"; return null; }
                }
                """);
        args.add(probe.toString());
        int exit = ToolProvider.getSystemJavaCompiler().run(null, System.out, System.err, args.toArray(String[]::new));
        if (exit != 0) throw new AssertionError("REAL_NEBULA_COMPILE_FAILED:" + exit);
        return out;
    }
    private static URLClassLoader loader(Path dir) throws Exception { return new URLClassLoader(new java.net.URL[]{dir.toUri().toURL()}, PageContract.class.getClassLoader()); }
    private static Object page(ClassLoader loader, int offset, int size, int length, int direction, int fault, boolean realController) throws Exception {
        Class<?> controllerType = loader.loadClass(PKG + "PageableController");
        Object controller;
        if (realController) {
            controller = controllerType.getConstructor(int.class).newInstance(size);
            controllerType.getMethod("setCurrentPage",int.class).invoke(controller,offset);
            controllerType.getMethod("setSort",String.class,int.class).invoke(controller,null,direction);
        } else controller = loader.loadClass("Probe").getConstructor(int.class,int.class,int.class).newInstance(offset,size,direction);
        List<Integer> data = new ArrayList<>(); for (int i=0;i<length;i++) data.add(length-i);
        List<Integer> list = fault == 3 ? Collections.unmodifiableList(data) : fault == 4 ? new LinkedList<>(data) : data;
        if (fault == 5) list = null;
        if (fault == 6) controller = null;
        StringBuilder trace = new StringBuilder();
        Class<?> sortType = loader.loadClass(PKG + "collections.SortProcessor");
        Object sorter = Proxy.newProxyInstance(loader, new Class<?>[]{sortType}, (proxy, method, arguments) -> {
            if (!method.getName().equals("sort")) throw new AssertionError(method);
            trace.append("sort;"); if (fault == 8) throw new IllegalStateException("SORT_PROBE_FAILURE");
            Collections.reverse((List<?>) arguments[0]); return null;
        });
        if (fault == 7) sorter = null;
        Class<?> helper = loader.loadClass(PKG + "collections.PageListHelper");
        Object outcome;
        try {
            Object result = fault == 1 ? helper.getMethod("createPage",List.class,controllerType).invoke(null,list,controller)
                : helper.getMethod("createPage",List.class,controllerType,sortType).invoke(null,list,controller,sorter);
            List<?> content = (List<?>) result.getClass().getMethod("getContent").invoke(result);
            String initial = content.toString(); Object total = result.getClass().getMethod("getTotalElements").invoke(result);
            String backed = "empty";
            if (!content.isEmpty() && list != null && fault != 3) {
                Integer first = (Integer) content.get(0); int at = list.indexOf(first);
                list.set(at, 1234567); backed = content.toString();
            }
            outcome = "OK:" + initial + ":total=" + total + ":backed=" + backed;
        } catch (InvocationTargetException e) { outcome = failure(e.getCause()); }
        String controllerTrace = controller == null || realController ? "" : (String) controller.getClass().getField("trace").get(controller);
        return Arrays.asList(outcome, String.valueOf(list), trace.toString(), controllerTrace);
    }
    private static Object comparator(ClassLoader loader, Object a, Object b, String property, int direction) throws Exception {
        Class<?> type = loader.loadClass(PKG + "collections.BeanComparator"); Object instance = type.getConstructor(String.class,int.class).newInstance(property,direction);
        try { return type.getMethod("compare",Object.class,Object.class).invoke(instance,a,b); }
        catch (InvocationTargetException e) { return failure(e.getCause()); }
    }
    private static String failure(Throwable e) { return e.getClass().getName() + ":" + e.getMessage(); }
    private static List<String> api(Class<?> type) {
        List<String> result = new ArrayList<>();
        for (Method m : type.getDeclaredMethods()) if (Modifier.isPublic(m.getModifiers()) || Modifier.isProtected(m.getModifiers())) result.add(m.toGenericString());
        for (Constructor<?> c : type.getDeclaredConstructors()) if (Modifier.isPublic(c.getModifiers()) || Modifier.isProtected(c.getModifiers())) result.add(c.toGenericString());
        for (Field f : type.getDeclaredFields()) if (Modifier.isPublic(f.getModifiers()) || Modifier.isProtected(f.getModifiers())) result.add(f.toGenericString());
        Collections.sort(result); return List.copyOf(result);
    }
    private static void compare(Object before, Object after) {
        assertions++; if (!Objects.equals(before,after)) throw new AssertionError("DIFFERENTIAL:" + before + " != " + after);
    }
    public static final class Bean { private final int value; public Bean(int value) { this.value=value; } public int getValue() { return value; } }
    public static final class BrokenBean { public int getValue() { throw new IllegalStateException("BEAN_PROBE_FAILURE"); } }
}
