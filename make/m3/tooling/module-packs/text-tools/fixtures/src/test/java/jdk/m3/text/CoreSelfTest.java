// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Explicit check harness: tests do not disappear when assertions are disabled. */
public final class CoreSelfTest {
  private static int checks;
  private static final String IMPL = new Canonical("test-host-v1").text("copy").finish();
  private static final Set<Capability> TEXT = Set.of(Capability.TEXT);
  private CoreSelfTest() {}
  private static void check(boolean value, String name) {
    checks++;
    if (!value) throw new AssertionError(name);
  }
  private static void rejected(Runnable action, String name) {
    checks++;
    try { action.run(); }
    catch (IllegalArgumentException | IllegalStateException expected) { return; }
    throw new AssertionError("accepted: " + name);
  }
  private static Binding binding(String id, Atom atom) {
    return new Binding(id, "reviewed " + id, IMPL, TEXT, atom);
  }
  private static M3UnixRuntime runtime(Binding... bindings) {
    return new M3UnixRuntime(new Registry(List.of(bindings)), Limits.standard(), TEXT);
  }
  public static void main(String[] args) throws Exception {
    String[] samples = {"", "\n", "a", "a\n", "a\r\nb\rc\n", "\r\n\r\n", "हिंदी 😀\n"};
    for (String sample : samples) {
      check(TextFrame.fromString(sample).text().equals(sample), "frame roundtrip");
    }
    rejected(() -> TextFrame.fromString("\ud800"), "surrogate collision");
    check(!new Canonical("x").text("ab").text("c").finish().equals(
        new Canonical("x").text("a").text("bc").finish()), "length framing");
    TextFrame one = new TextFrame(List.of(new TextLine("ab", "")));
    TextFrame two = new TextFrame(List.of(new TextLine("a", ""), new TextLine("b", "")));
    check(one.text().equals(two.text()) && !one.sha256().equals(two.sha256()), "frame boundaries");
    ArrayList<TextLine> mutable = new ArrayList<>(one.lines());
    TextFrame immutable = new TextFrame(mutable); mutable.clear();
    check(immutable.equals(one), "defensive frame copy");
    rejected(() -> new Limits(0, 0, 0, 0), "invalid limits");
    rejected(() -> new Limits(1, 0, 1, 1).check(TextFrame.fromString("é")), "UTF-8 byte cap");
    rejected(() -> Ids.id("../shell"), "path id");
    Binding copy = binding("copy", (i, o) -> i.lines().forEach(o));
    Registry registry = new Registry(List.of(copy));
    Plan single = new Plan(List.of(Node.of("one", copy)), "one");
    TextFrame input = TextFrame.fromString("b\r\na\n");
    M3UnixRuntime run = runtime(copy);
    ExecutionResult good = run.execute(single, input);
    check(good.output().equals(input), "identity output");
    check(run.replay(single, input, good).equals(good), "exact replay");
    rejected(() -> run.replay(single, one, good), "replay preimage");
    rejected(() -> new Registry(List.of(copy, copy)), "duplicate registry ownership");
    rejected(() -> run.execute(new Plan(List.of(), "one"), input), "empty plan");
    rejected(() -> run.execute(new Plan(List.of(Node.of("one", copy)), "no"), input), "output missing");
    rejected(() -> run.execute(new Plan(List.of(Node.of("one", copy, "no")), "one"), input), "dependency missing");
    rejected(() -> run.execute(new Plan(List.of(Node.of("one", copy, "two"),
        Node.of("two", copy, "one")), "one"), input), "cycle");
    rejected(() -> run.execute(new Plan(List.of(Node.of("one", copy),
        Node.of("one", copy)), "one"), input), "duplicate node");
    rejected(() -> new Node("one", "copy", copy.sha256(), List.of("x", "x")), "duplicate edge");
    rejected(() -> run.execute(new Plan(List.of(new Node("one", "copy", IMPL, List.of())), "one"), input), "stale seal");
    rejected(() -> run.execute(new Plan(List.of(new Node("one", "missing", IMPL, List.of())), "one"), input), "unknown binding");
    rejected(() -> run.execute(new Plan(List.of(Node.of("one", copy),
        Node.of("unused", copy)), "one"), input), "unreachable work");
    AtomicInteger calls = new AtomicInteger();
    Binding effect = binding("effect", (i, o) -> { calls.incrementAndGet(); i.lines().forEach(o); });
    Binding files = new Binding("files", "host reads approved snapshot", IMPL,
        Set.of(Capability.FILESYSTEM_READ), (i, o) -> calls.incrementAndGet());
    rejected(() -> runtime(effect, files).execute(new Plan(List.of(Node.of("first", effect),
        Node.of("last", files, "first")), "last"), input), "capability denied");
    check(calls.get() == 0, "full admission precedes all callbacks");
    rejected(() -> new M3UnixRuntime(registry, new Limits(1, 0, 1, 100), TEXT)
        .execute(single, input), "input lines bounded");
    Binding left = binding("left", (i, o) -> o.accept(new TextLine("L", "\n")));
    Binding right = binding("right", (i, o) -> o.accept(new TextLine("R", "\n")));
    List<Node> dag = List.of(Node.of("left", left), Node.of("right", right),
        Node.of("join", copy, "right", "left"));
    Plan dagPlan = new Plan(dag, "join");
    M3UnixRuntime dagRun = runtime(copy, left, right);
    ExecutionResult dagResult = dagRun.execute(dagPlan, input);
    check(dagResult.output().text().equals("R\nL\n"), "semantic dependency order");
    List<Node> reversed = new ArrayList<>(dag); Collections.reverse(reversed);
    check(dagRun.execute(new Plan(reversed, "join"), input).equals(dagResult), "node order invariant");
    rejected(() -> new M3UnixRuntime(new Registry(List.of(copy, left, right)),
        new Limits(2, 2, 100, 1000), TEXT).execute(dagPlan, input), "node budget");
    rejected(() -> new M3UnixRuntime(new Registry(List.of(copy, left, right)),
        new Limits(3, 1, 100, 1000), TEXT).execute(dagPlan, input), "edge budget");
    Binding flood = binding("flood", (i, o) -> {
      o.accept(new TextLine("a", "")); o.accept(new TextLine("b", ""));
    });
    Plan floodPlan = new Plan(List.of(Node.of("flood", flood)), "flood");
    rejected(() -> new M3UnixRuntime(new Registry(List.of(flood)),
        new Limits(1, 0, 1, 100), TEXT).execute(floodPlan, TextFrame.fromString("")), "output collection cap");
    Binding swallow = binding("swallow", (i, o) -> {
      o.accept(new TextLine("a", ""));
      try { o.accept(new TextLine("b", "")); } catch (IllegalArgumentException expected) { /* malicious host */ }
    });
    rejected(() -> new M3UnixRuntime(new Registry(List.of(swallow)),
        new Limits(1, 0, 1, 100), TEXT).execute(new Plan(List.of(Node.of("s", swallow)), "s"),
        TextFrame.fromString("")), "swallowed budget violation cannot succeed");
    Binding failure = binding("failure", (i, o) -> { o.accept(new TextLine("partial", ""));
      throw new IllegalStateException("host failure"); });
    rejected(() -> runtime(failure).execute(new Plan(List.of(Node.of("f", failure)), "f"), input), "partial failure");
    AtomicInteger counter = new AtomicInteger();
    Binding unstable = binding("unstable", (i, o) -> o.accept(new TextLine("" + counter.incrementAndGet(), "")));
    Plan changing = new Plan(List.of(Node.of("u", unstable)), "u");
    M3UnixRuntime unstableRun = runtime(unstable);
    ExecutionResult before = unstableRun.execute(changing, input);
    rejected(() -> unstableRun.replay(changing, input, before), "non-deterministic host rejected");
    check(CommandCatalog.COMMANDS.size() == 14, "complete donor command inventory");
    check(CommandCatalog.lookup("greb", 1).equals(List.of("grep")), "deterministic fuzzy candidate");
    rejected(() -> CommandCatalog.lookup("x", 15), "bounded inventory query");
    check(CommandCatalog.lookup("", 0).isEmpty(), "empty result budget");
    try (var pool = Executors.newFixedThreadPool(4)) {
      var jobs = new ArrayList<java.util.concurrent.Callable<ExecutionResult>>();
      for (int i = 0; i < 32; i++) jobs.add(() -> run.execute(single, input));
      for (var result : pool.invokeAll(jobs)) check(result.get().equals(good), "concurrent isolation");
    }
    // Structured-format contracts are independent additions to the original core suite.
    Utf8SegmentsProbe.main(new String[0]);
    System.out.println("CORE_TEST_PASS checks=" + checks);
    System.out.println("DONOR_RUNTIME_NOT_EXERCISED");
  }
}
