/*
 * Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
 * Oracle-only Guava/Spring JSON observation pattern. Pinned-source expectations
 * are hypotheses; API-law witnesses are separate. Failures require classification,
 * never candidate bug emulation. No collection implementation is generated.
 */
import java.io.Serial;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.ResettableIterator;
import org.apache.commons.collections4.SetValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedLinkedHashMap;
import org.apache.commons.collections4.multimap.LinkedHashSetValuedLinkedHashMap;

public final class ApacheMultiValuePreimageProbe {
 private static final String DONOR =
  "apache/commons-collections@7f7fb0244abc940a2e9dd28b67508c0483a58c3e";
 private static int cases, operations, lawWitnesses, lawFailures, fixtureFailures;
 private static final Map<String, Integer> groups = new LinkedHashMap<>();

 private ApacheMultiValuePreimageProbe() { }

 @FunctionalInterface
 private interface Action { Object run() throws Exception; }
 @FunctionalInterface
 private interface Scenario { void run(Context context) throws Exception; }

 private static final class CallbackFailure extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;
  CallbackFailure(String message) { super(message); }
 }

 private record Outcome(Object returned, String error, String message) { }

 private static MultiValuedMap<String, String> fresh(String kind) {
  return switch (kind) {
  case "list" -> new ArrayListValuedLinkedHashMap<>();
  case "set" -> new LinkedHashSetValuedLinkedHashMap<>();
  default -> throw new IllegalArgumentException("unknown provider kind");
  };
 }

 private static final class Context {
  final String kind;
  MultiValuedMap<String, String> map;
  final List<Object> observations = new ArrayList<>();
  final List<Object> laws = new ArrayList<>();
  final List<String> callbacks = new ArrayList<>();
  final IdentityHashMap<Object, String> aliases = new IdentityHashMap<>();
  final List<Object> fixtureErrors = new ArrayList<>();

  Context(String kind) { this.kind = kind; map = fresh(kind); }

  ListValuedMap<String, String> list() {
  return (ListValuedMap<String, String>) map;
  }

  SetValuedMap<String, String> set() {
  return (SetValuedMap<String, String>) map;
  }

  void trace(String value) { callbacks.add(value); }

  void label(Object value, String name) {
  if (value != null) aliases.put(value, name);
  }

  Outcome observe(String name, Action action) {
  Map<String, Object> before = state(map);
  int callbackStart = callbacks.size();
  Object returned = null, displayed = null;
  String error = "", message = "";
  try { returned = action.run(); displayed = describe(returned); }
  catch (Throwable failure) {
  returned = null;
  error = failure.getClass().getName();
  message = String.valueOf(failure.getMessage());
  }
  Outcome outcome = new Outcome(returned, error, message);
  observations.add(object("operation", name, "before", before,
  "outcome", object("return", displayed, "error", error, "message", message),
  "after", state(map),
  "callbacks", new ArrayList<>(callbacks.subList(callbackStart, callbacks.size()))));
  operations++;
  return outcome;
  }

  void alias(String name, Object value) {
  observe("read-alias/" + name, () -> value);
  }

  void law(String name, BooleanSupplier condition) {
  boolean passed = false;
  String error = "";
  try { passed = condition.getAsBoolean(); }
  catch (Throwable failure) { error = failure.getClass().getName(); }
  laws.add(object("law", name, "passed", passed, "error", error,
  "classification", "INDEPENDENT_API_LAW_WITNESS"));
  lawWitnesses++;
  if (!passed) lawFailures++;
  }

  Object describe(Object value) {
  if (value == null || value instanceof String || value instanceof Number
  || value instanceof Boolean) return value;
  if (value instanceof MultiValuedMap<?, ?> multi) {
  return object("class", value.getClass().getName(), "state", state(multi));
  }
  if (value instanceof MultiSet<?> keys)
  return object("class", value.getClass().getName(), "size", keys.size(),
  "traversal", boundedKeys(keys),
  "serializableMarker", value instanceof java.io.Serializable,
  "unmodifiableMarker", value instanceof org.apache.commons.collections4.Unmodifiable);
  if (value instanceof MultiSet.Entry<?> entry) {
  return object("class", value.getClass().getName(), "element", entry.getElement(),
  "count", entry.getCount(), "identity", aliases.get(value));
  }
  if (value instanceof Map.Entry<?, ?> entry) {
  return object("class", value.getClass().getName(), "key", describe(entry.getKey()),
  "value", describe(entry.getValue()), "identity", aliases.get(value));
  }
  if (value instanceof Collection<?> collection) {
  List<Object> items = new ArrayList<>();
  for (Object item : collection) items.add(describe(item));
  return object("class", value.getClass().getName(), "size", collection.size(),
  "items", items, "identity", aliases.get(value));
  }
  if (value instanceof Map<?, ?> mapping) {
  List<Object> entries = new ArrayList<>();
  for (Map.Entry<?, ?> entry : mapping.entrySet())
  entries.add(object("key", describe(entry.getKey()), "value", describe(entry.getValue())));
  return object("class", value.getClass().getName(), "size", mapping.size(),
  "entries", entries, "identity", aliases.get(value));
  }
  if (value instanceof Object[] array) {
  List<Object> items = new ArrayList<>();
  for (Object item : array) items.add(describe(item));
  return object("class", value.getClass().getName(), "items", items);
  }
  if (value instanceof Iterator<?>)
  return object("class", value.getClass().getName(), "identity", aliases.get(value),
  "resettable", value instanceof ResettableIterator<?>);
  return object("class", value.getClass().getName(), "text", String.valueOf(value),
  "identity", aliases.get(value));
  }

  void commonLaws() {
  law("pair-size-group-sum", () -> {
  int sum = 0;
  for (Collection<String> values : map.asMap().values()) sum += values.size();
  return sum == map.size();
  });
  law("keySet-asMap-key-count",
  () -> map.keySet().size() == map.asMap().size());
  law("keys-size-pair-count",
  () -> map.keys().size() == map.size());
  law("keys-count-group-size", () -> {
  for (String key : map.keySet())
  if (map.keys().getCount(key) != map.get(key).size()) return false;
  return true;
  });
  law("entries-pair-count",
  () -> iterationCount(map.entries()) == map.size());
  law("values-pair-count",
  () -> iterationCount(map.values()) == map.size());
  law("repeated-keys-pair-count",
  () -> iterationCount(map.keys()) == map.size());
  law("asMap-values-are-nonempty", () -> {
  for (Collection<String> values : map.asMap().values())
  if (values.isEmpty()) return false;
  return true;
  });
  law("isEmpty-means-no-key-value-pairs",
  () -> map.isEmpty() == (map.size() == 0));
  }
 }

 private static int iterationCount(Iterable<?> values) {
  int count = 0;
  Iterator<?> iterator = values.iterator();
  while (count < 128 && iterator.hasNext()) { iterator.next(); count++; }
  return iterator.hasNext() ? -1 : count;
 }

 private static Map<String, Object> boundedKeys(MultiSet<?> keys) {
  List<Object> sequence = new ArrayList<>();
  Iterator<?> iterator = keys.iterator();
  while (sequence.size() < 128 && iterator.hasNext()) sequence.add(iterator.next());
  return object("sequence", sequence, "truncatedAt128", iterator.hasNext());
 }

 private static int serializedBytes(Object value) throws Exception {
  var bytes = new java.io.ByteArrayOutputStream();
  try (var output = new java.io.ObjectOutputStream(bytes)) { output.writeObject(value); }
  return bytes.size();
 }

 private static Map<String, Object> state(MultiValuedMap<?, ?> map) {
  try {
  List<Object> groupRows = new ArrayList<>();
  for (Map.Entry<?, ? extends Collection<?>> entry : map.asMap().entrySet())
  groupRows.add(object("key", entry.getKey(), "values", new ArrayList<>(entry.getValue())));
  List<Object> pairs = new ArrayList<>();
  for (Map.Entry<?, ?> entry : map.entries())
  pairs.add(object("key", entry.getKey(), "value", entry.getValue()));
  List<Object> counts = new ArrayList<>();
  for (MultiSet.Entry<?> entry : map.keys().entrySet())
  counts.add(object("key", entry.getElement(), "count", entry.getCount()));
  return object("size", map.size(), "isEmpty", map.isEmpty(), "keyCount", map.keySet().size(),
  "asMapSize", map.asMap().size(), "keyOrder", new ArrayList<>(map.keySet()),
  "groups", groupRows, "entries", pairs, "values", new ArrayList<>(map.values()),
  "keysReportedSize", map.keys().size(), "keysRepeated", boundedKeys(map.keys()),
  "keyCountEntries", counts);
  } catch (Throwable failure) {
  return object("snapshotError", failure.getClass().getName(),
  "snapshotMessage", String.valueOf(failure.getMessage()));
  }
 }

 private static void emit(String kind, String group, String name, String expectation,
  Scenario scenario) {
  Context context = new Context(kind);
  Map<String, Object> before = state(context.map);
  try { scenario.run(context); }
  catch (Throwable failure) {
  context.fixtureErrors.add(object("error", failure.getClass().getName(),
  "message", String.valueOf(failure.getMessage())));
  fixtureFailures++;
  }
  context.commonLaws();
  System.out.println(json(object("case", kind + "/" + group + "/" + name,
  "group", group, "provider", context.map.getClass().getName(),
  "source", DONOR, "sourceExpectation", expectation,
  "sourceExpectationStatus", "SOURCE_INFERRED__ACTUAL_OUTPUT_REQUIRED",
  "before", before, "operations", context.observations, "after", state(context.map),
  "callbacks", context.callbacks, "apiLawWitnesses", context.laws,
  "fixtureErrors", context.fixtureErrors,
  "scope", "Oracle observations; classify failures before candidate work.")));
  cases++;
  groups.merge(group, 1, Integer::sum);
 }

 private static void populate(Context c) {
  c.map.put("a", "a0"); c.map.put("a", "a1"); c.map.put("b", "b0");
 }

 private static void absentViews(String kind) {
  emit(kind, "absent", "get-read-and-containsAll", "Absent get/asMap differ; containsAll short-circuits.", c -> {
  Collection<String> first = c.map.get("missing"), second = c.map.get("missing");
  c.label(first, "first-get"); c.label(second, "second-get");
  c.alias("first-get", first); c.alias("second-get", second);
  c.observe("same-get-descriptor-reference", () -> first == second);
  c.observe("asMap-get-absent", () -> c.map.asMap().get("missing"));
  Outcome empty = c.observe("absent-containsAll-empty", () -> first.containsAll(Collections.emptyList()));
  c.law("empty-Collection-containsAll-empty-is-true",
  () -> empty.error().isEmpty() && Boolean.TRUE.equals(empty.returned()));
  Outcome nul = c.observe("absent-containsAll-null", () -> first.containsAll(null));
  c.law("Collection-containsAll-null-throws-NPE",
  () -> nul.error().equals(NullPointerException.class.getName()));
  Iterator<String> emptyIterator = first.iterator();
  c.alias("absent-iterator", emptyIterator);
  c.observe("absent-iterator-hasNext", emptyIterator::hasNext);
  c.observe("absent-iterator-next", emptyIterator::next);
  c.observe("absent-iterator-remove", () -> { emptyIterator.remove(); return "void"; });
  });
  emit(kind, "absent", "addAll-empty", "View addAll publishes before backing no-op.", c -> {
  Collection<String> view = c.map.get("missing");
  c.observe("get-view-addAll-empty", () -> view.addAll(Collections.emptyList()));
  c.alias("held-get-after-empty-addAll", view);
  c.observe("containsKey-after-no-op", () -> c.map.containsKey("missing"));
  });
  emit(kind, "absent", "addAll-null", "View addAll publishes before null validation.", c -> {
  Collection<String> view = c.map.get("missing");
  c.observe("get-view-addAll-null", () -> view.addAll(null));
  c.alias("held-get-after-null-addAll", view);
  });
  emit(kind, "absent", "putAll-empty-and-null", "putAll skips empty input and validates null.", c -> {
  c.observe("putAll-key-empty-Collection", () -> c.map.putAll("a", Collections.emptyList()));
  Iterable<String> empty = () -> Collections.<String>emptyIterator();
  c.observe("putAll-key-empty-Iterable", () -> c.map.putAll("b", empty));
  c.observe("putAll-key-null-Iterable", () -> c.map.putAll("c", (Iterable<String>) null));
  c.observe("putAll-null-Map", () -> c.map.putAll((Map<String, String>) null));
  c.observe("putAll-null-MultiValuedMap", () -> c.map.putAll((MultiValuedMap<String, String>) null));
  c.law("empty-putAll-adds-no-key-value-mappings", () -> c.map.size() == 0);
  });
  emit(kind, "absent", "bulk-short-circuit-and-reconnect", "Absent bulk short-circuit; held get reconnects by key.", c -> {
  Collection<String> held = c.map.get("a");
  c.label(held, "held-get");
  c.observe("removeAll-null-absent", () -> held.removeAll(null));
  c.observe("retainAll-null-absent", () -> held.retainAll(null));
  c.observe("held-add-first", () -> held.add("x"));
  c.observe("remove-key", () -> c.map.remove("a"));
  c.alias("held-after-key-remove", held);
  c.observe("held-add-recreates", () -> held.add("y"));
  c.alias("held-after-recreate", held);
  c.law("get-mutations-update-parent", () -> c.map.containsMapping("a", "y"));
  });
 }

 private static void listPublicationAndAliases() {
  for (int index : new int[] {-1, 1}) {
  emit("list", "indexed", "add-invalid-" + index,
  "Indexed add publishes before bounds check.", c -> {
  c.observe("add-invalid-index", () -> { c.list().get("a").add(index, "x"); return "void"; });
  });
  emit("list", "indexed", "addAll-invalid-" + index,
  "Indexed addAll checks backing before publication.", c -> {
  c.observe("addAll-invalid-index-empty", () -> c.list().get("a").addAll(index, Collections.emptyList()));
  c.observe("listIterator-invalid-index", () -> c.list().get("b").listIterator(index));
  });
  }
  emit("list", "indexed", "valid-listIterator", "Absent cursor add creates group; test two-way mutation.", c -> {
  ListIterator<String> it = c.list().get("a").listIterator();
  c.observe("listIterator-before-next-remove", () -> { it.remove(); return "void"; });
  c.observe("listIterator-before-next-set", () -> { it.set("bad"); return "void"; });
  c.observe("listIterator-add-x", () -> { it.add("x"); return "void"; });
  c.observe("listIterator-add-y", () -> { it.add("y"); return "void"; });
  c.observe("listIterator-previous", it::previous);
  c.observe("listIterator-set-Y", () -> { it.set("Y"); return "void"; });
  c.observe("listIterator-next", it::next);
  c.observe("listIterator-nextIndex", it::nextIndex);
  c.observe("listIterator-previousIndex", it::previousIndex);
  c.observe("listIterator-remove", () -> { it.remove(); return "void"; });
  c.observe("listIterator-previous-x", it::previous);
  c.observe("listIterator-remove-last", () -> { it.remove(); return "void"; });
  c.law("valid-cursor-removal-removes-all-pairs", () -> c.map.size() == 0);
  });
  emit("list", "aliases", "raw-subList-key-vacuum", "Raw subList clear may leave empty directory key.", c -> {
  c.map.put("a", "x"); c.map.put("a", "y");
  List<String> wrapper = c.list().get("a"), raw = wrapper.subList(0, 2);
  c.label(wrapper, "get-wrapper"); c.label(raw, "raw-subList");
  c.alias("raw-subList-before", raw);
  c.observe("raw-subList-clear", () -> { raw.clear(); return "void"; });
  c.alias("get-wrapper-after-raw-clear", wrapper);
  c.alias("raw-subList-after-clear", raw);
  c.observe("asMap-get-after-raw-clear", () -> c.map.asMap().get("a"));
  });
  emit("list", "aliases", "absent-empty-subList", "Absent subList uses immutable empty-list backing.", c -> {
  List<String> wrapper = c.list().get("a"), raw = wrapper.subList(0, 0);
  c.observe("empty-subList-add", () -> raw.add("x"));
  c.observe("wrapper-add", () -> wrapper.add("y"));
  c.alias("empty-subList-after-wrapper-add", raw);
  c.alias("wrapper-after-wrapper-add", wrapper);
  });
  emit("list", "aliases", "nested-subList-replacement", "Nested raw aliases mutate; repeated get returns descriptors.", c -> {
  c.map.putAll("a", List.of("a0", "a1", "a2", "a3"));
  List<String> first = c.list().get("a"), second = c.list().get("a");
  List<String> sub = first.subList(1, 4), nested = sub.subList(1, 2);
  c.observe("repeated-get-reference", () -> first == second);
  c.observe("nested-set", () -> nested.set(0, "changed"));
  c.alias("first-wrapper", first); c.alias("second-wrapper", second);
  c.alias("subList", sub); c.alias("nested-subList", nested);
  c.law("raw-alias-set-updates-parent", () -> c.map.containsMapping("a", "changed"));
  });
 }

 private static void removeAndDescriptorAliases(String kind) {
  for (boolean viaAsMap : new boolean[] {false, true}) {
  emit(kind, "detach", viaAsMap ? "asMap-remove" : "outer-remove",
  viaAsMap ? "asMap.remove copies and clears original."
  : "Outer remove detaches original without clearing.", c -> {
  populate(c);
  Collection<String> first = c.map.get("a"), second = c.map.get("a");
  Collection<String> asGet = c.map.asMap().get("a");
  Collection<String> asEntry = c.map.asMap().entrySet().iterator().next().getValue();
  Iterator<String> captured = first.iterator();
  List<String> raw = kind.equals("list") ? c.list().get("a").subList(0, 2) : null;
  c.label(first, "first-get"); c.label(second, "second-get");
  c.label(asGet, "asMap-get"); c.label(asEntry, "asMap-entry-value");
  c.alias("asMap-get-group", asGet); c.alias("asMap-entry-group", asEntry);
  c.observe("wrapper-identity", () -> first == second);
  c.observe("asMap-group-is-get-descriptor", () -> first == asGet);
  Outcome removed = c.observe("remove-a", () -> viaAsMap ? c.map.asMap().remove("a") : c.map.remove("a"));
  c.alias("first-get-after-remove", first); c.alias("asMap-get-after-remove", asGet);
  if (raw != null) c.alias("held-raw-subList-after-remove", raw);
  c.observe("captured-group-iterator-next-after-parent-remove", captured::next);
  c.observe("held-get-recreates-a", () -> first.add("reborn"));
  c.alias("second-get-after-recreate", second); c.alias("asMap-entry-value-after-recreate", asEntry);
  c.observe("detached-return-add", () -> {
  if (!(removed.returned() instanceof Collection<?>)) throw new AssertionError("removed collection missing");
  @SuppressWarnings("unchecked")
  Collection<String> detached = (Collection<String>) removed.returned();
  return detached.add("detached-only");
  });
  c.law("removed-return-mutation-does-not-update-parent",
  () -> !c.map.containsMapping("a", "detached-only") && c.map.containsMapping("a", "reborn"));
  });
  }
  emit(kind, "detach", "clear-keySet-entrySet-aliases", "Directory removals detach; get/asMap descriptors reconnect.", c -> {
  populate(c);
  Collection<String> get = c.map.get("a"), fromMap = c.map.asMap().get("a");
  Map.Entry<String, Collection<String>> entry = c.map.asMap().entrySet().iterator().next();
  c.observe("asMap-entry-setValue-refusal", () -> entry.setValue(new ArrayList<>()));
  c.observe("asMap-put-refusal", () -> c.map.asMap().put("z", new ArrayList<>()));
  c.observe("asMap-putAll-empty", () -> { c.map.asMap().putAll(Collections.emptyMap()); return "void"; });
  c.observe("asMap-entrySet-remove", () -> c.map.asMap().entrySet().remove(
  new AbstractMap.SimpleImmutableEntry<>("a", kind.equals("list")
  ? new ArrayList<>(Arrays.asList("a0", "a1")) : Set.of("a0", "a1"))));
  c.alias("held-get-after-entry-remove", get); c.alias("held-asMap-group-after-entry-remove", fromMap);
  c.observe("recreate-a", () -> get.add("new"));
  c.observe("keySet-remove-a", () -> c.map.keySet().remove("a"));
  c.alias("held-get-after-keySet-remove", get);
  c.observe("map-clear", () -> { c.map.clear(); return "void"; });
  });
  emit(kind, "detach", "absent-remove-returns", "Absent outer remove is empty; asMap remove is null.", c -> {
  Outcome outer = c.observe("outer-remove-absent", () -> c.map.remove("missing"));
  c.observe("asMap-remove-absent", () -> c.map.asMap().remove("missing"));
  c.observe("absent-detached-return-add", () -> {
  @SuppressWarnings("unchecked")
  Collection<String> collection = (Collection<String>) outer.returned();
  return collection.add("x");
  });
  });
 }

 private static void wrappedListDefaults() {
  emit("list", "defaults", "wrapped-removeIf-throw-prefix", "Wrapper removeIf uses iterator prefix mutations.", c -> {
  c.map.putAll("a", List.of("a", "b", "c", "d"));
  c.observe("wrapped-removeIf-throw-on-c", () -> c.list().get("a").removeIf(v -> {
  c.trace("predicate:" + v);
  if (v.equals("c")) throw new CallbackFailure("predicate-c");
  return true;
  }));
  });
  emit("list", "defaults", "raw-subList-removeIf-throw-prefix", "Raw subList removeIf uses backing-owner dispatch.", c -> {
  c.map.putAll("a", List.of("a", "b", "c", "d"));
  List<String> raw = c.list().get("a").subList(0, 4);
  c.observe("raw-subList-removeIf-throw-on-c", () -> raw.removeIf(v -> {
  c.trace("predicate:" + v);
  if (v.equals("c")) throw new CallbackFailure("predicate-c");
  return true;
  }));
  c.alias("raw-subList-after-failure", raw);
  });
  for (boolean fail : new boolean[] {false, true}) {
  emit("list", "defaults", "wrapped-sort-" + (fail ? "throw" : "success"),
  "Wrapped sort copies array, then iterator writeback.", c -> {
  c.map.putAll("a", List.of("d", "c", "b", "a"));
  Iterator<String> held = c.list().get("a").iterator();
  Comparator<String> comparator = (left, right) -> {
  c.trace("compare:" + left + ":" + right);
  if (fail) throw new CallbackFailure("comparator");
  return left.compareTo(right);
  };
  c.observe("wrapped-sort", () -> { c.list().get("a").sort(comparator); return "void"; });
  c.observe("held-iterator-after-sort", held::next);
  });
  emit("list", "defaults", "wrapped-replaceAll-" + (fail ? "throw" : "success"),
  "Wrapped replaceAll mutates callback prefix.", c -> {
  c.map.putAll("a", List.of("a", "b", "c"));
  Iterator<String> held = c.list().get("a").iterator();
  c.observe("wrapped-replaceAll", () -> {
  c.list().get("a").replaceAll(v -> {
  c.trace("replace:" + v);
  if (fail && v.equals("b")) throw new CallbackFailure("replace-b");
  return v.toUpperCase(java.util.Locale.ROOT);
  });
  return "void";
  });
  c.observe("held-iterator-after-replaceAll", held::next);
  });
  }
  emit("list", "defaults", "null-callbacks-and-sort-absent", "Null callback and empty natural-sort paths differ.", c -> {
  c.observe("absent-removeIf-null", () -> c.list().get("a").removeIf(null));
  c.observe("absent-replaceAll-null", () -> { c.list().get("a").replaceAll(null); return "void"; });
  c.observe("absent-sort-null-natural", () -> { c.list().get("a").sort(null); return "void"; });
  c.map.putAll("a", Arrays.asList("b", null, "a"));
  c.observe("present-sort-null-natural-with-null-value", () -> { c.list().get("a").sort(null); return "void"; });
  });
  emit("list", "defaults", "structural-removeIf-callback", "Reentrant timing is inventory, not blanket API parity.", c -> {
  c.map.putAll("a", List.of("a", "b", "c"));
  c.observe("predicate-adds-to-same-group", () -> c.list().get("a").removeIf(v -> {
  c.trace("predicate:" + v); c.map.put("a", "extra"); return false;
  }));
  });
 }

 private static void setViews() {
  emit("set", "set-values", "duplicates-and-view-remove", "Set duplicate suppression and last-value key cleanup.", c -> {
  c.observe("put-first", () -> c.map.put("a", "x"));
  Outcome duplicate = c.observe("put-equal-duplicate", () -> c.map.put("a", new String("x")));
  c.law("SetValuedMap-duplicate-put-does-not-increase-size",
  () -> Boolean.FALSE.equals(duplicate.returned()) && c.map.size() == 1);
  Set<String> view = c.set().get("a");
  c.observe("set-view-add-equal", () -> view.add(new String("x")));
  c.observe("set-view-add-y", () -> view.add("y"));
  c.observe("set-view-remove-x", () -> view.remove("x"));
  c.observe("set-view-remove-last-y", () -> view.remove("y"));
  c.law("last-set-view-removal-removes-key", () -> !c.map.containsKey("a"));
  });
  emit("set", "set-values", "iterator-bulk-removeIf", "Set wrapper supports iterator/bulk mutation.", c -> {
  c.map.putAll("a", List.of("a", "b", "c"));
  Set<String> view = c.set().get("a");
  Iterator<String> it = view.iterator();
  c.observe("set-iterator-next", it::next);
  c.observe("set-iterator-remove", () -> { it.remove(); return "void"; });
  c.observe("set-removeIf-throw-on-c", () -> view.removeIf(v -> {
  c.trace("predicate:" + v);
  if (v.equals("c")) throw new CallbackFailure("set-predicate-c");
  return true;
  }));
  c.observe("set-retainAll-empty", () -> view.retainAll(Collections.emptySet()));
  });
  emit("set", "set-values", "set-equality-and-order", "Set equality ignores order; linked traversal keeps order.", c -> {
  c.map.putAll("a", List.of("c", "a", "b", "a"));
  Set<String> view = c.set().get("a");
  c.observe("set-equals-content", () -> view.equals(Set.of("a", "b", "c")));
  c.observe("set-hash", view::hashCode);
  c.observe("set-remove-reinsert-a", () -> { view.remove("a"); return view.add("a"); });
  c.alias("ordered-group", view);
  });
 }

 private static void countedKeysAndMapIterator(String kind) {
  emit(kind, "counted-keys", "counts-and-held-entry-lifetime", "Count entry captures group; key recreation may differ.", c -> {
  populate(c);
  MultiSet<String> keys = c.map.keys();
  MultiSet.Entry<String> entry = keys.entrySet().iterator().next();
  c.label(entry, "held-count-entry");
  c.alias("keys-view", keys); c.alias("uniqueSet", keys.uniqueSet());
  c.alias("count-entry-before", entry);
  c.observe("add-to-existing-a", () -> c.map.put("a", "a2"));
  c.alias("count-entry-after-group-growth", entry);
  c.observe("removeMapping-a0", () -> c.map.removeMapping("a", "a0"));
  c.alias("count-entry-after-one-remove", entry);
  c.observe("remove-a", () -> c.map.remove("a"));
  c.alias("count-entry-after-key-remove", entry);
  c.observe("current-key-count-after-remove", () -> keys.getCount("a"));
  c.observe("recreate-a", () -> c.map.put("a", "new"));
  c.alias("count-entry-after-key-recreate", entry);
  c.observe("current-key-count-after-recreate", () -> keys.getCount("a"));
  });
  emit(kind, "counted-keys", "all-refusals", "Count-view mutators refuse even empty/zero/null.", c -> {
  populate(c); MultiSet<String> keys = c.map.keys();
  c.observe("keys-add", () -> keys.add("a"));
  c.observe("keys-add-count-zero", () -> keys.add("a", 0));
  c.observe("keys-remove", () -> keys.remove("a"));
  c.observe("keys-remove-count-zero", () -> keys.remove("a", 0));
  c.observe("keys-setCount-same", () -> keys.setCount("a", 2));
  c.observe("keys-addAll-empty", () -> keys.addAll(Collections.emptyList()));
  c.observe("keys-removeAll-empty", () -> keys.removeAll(Collections.emptyList()));
  c.observe("keys-retainAll-null", () -> keys.retainAll(null));
  c.observe("keys-removeIf-null", () -> keys.removeIf(null));
  c.observe("keys-clear", () -> { keys.clear(); return "void"; });
  Iterator<String> it = keys.iterator();
  c.observe("keys-iterator-next", it::next);
  c.observe("keys-iterator-remove", () -> { it.remove(); return "void"; });
  c.observe("keys-uniqueSet-remove", () -> keys.uniqueSet().remove("a"));
  Iterator<MultiSet.Entry<String>> entries = keys.entrySet().iterator();
  c.observe("keys-entrySet-iterator-next", entries::next);
  c.observe("keys-entrySet-iterator-remove", () -> { entries.remove(); return "void"; });
  c.law("readonly-key-view-refusals-preserve-pairs", () -> c.map.size() == 3);
  });
  emit(kind, "counted-keys", "markers-serialization-normal-and-empty",
  "Serializable/Unmodifiable markers do not promise serializable KeysMultiSet backing.", c -> {
  populate(c);
  MultiSet<String> keys = c.map.keys();
  c.alias("normal-keys-markers-and-bounded-traversal", keys);
  c.observe("normal-keys-serialization", () -> serializedBytes(keys));
  c.observe("normal-keys-count-a", () -> keys.getCount("a"));
  c.observe("normal-keys-contains-a", () -> keys.contains("a"));
  c.observe("clear-before-empty-group", () -> { c.map.clear(); return "void"; });
  c.observe("publish-empty-group", () -> c.map.get("empty").addAll(Collections.emptyList()));
  c.alias("empty-group-keys-markers-and-bounded-traversal", keys);
  c.observe("empty-group-keys-reported-size", keys::size);
  c.observe("empty-group-keys-count", () -> keys.getCount("empty"));
  c.observe("empty-group-keys-contains", () -> keys.contains("empty"));
  c.alias("empty-group-count-entrySet", keys.entrySet());
  c.observe("empty-group-keys-serialization", () -> serializedBytes(keys));
  });
  emit(kind, "mapIterator", "empty-protocol", "Empty cursor has resettable shape and state errors.", c -> {
  MapIterator<String, String> it = c.map.mapIterator();
  c.alias("empty-mapIterator", it);
  c.observe("empty-getKey", it::getKey); c.observe("empty-getValue", it::getValue);
  c.observe("empty-setValue", () -> it.setValue("x"));
  c.observe("empty-next", it::next);
  c.observe("empty-remove", () -> { it.remove(); return "void"; });
  c.observe("empty-resettable", () -> it instanceof ResettableIterator<?>);
  if (it instanceof ResettableIterator<?> resettable)
  c.observe("empty-reset", () -> { resettable.reset(); return "void"; });
  });
  emit(kind, "mapIterator", "remove-delivered-state", "Cursor removes pair, retains delivered value, refuses set.", c -> {
  populate(c); MapIterator<String, String> it = c.map.mapIterator();
  c.observe("before-next-getKey", it::getKey);
  c.observe("before-next-getValue", it::getValue);
  c.observe("before-next-setValue", () -> it.setValue("bad"));
  c.observe("before-next-remove", () -> { it.remove(); return "void"; });
  c.observe("next-first", it::next); c.observe("first-getValue", it::getValue);
  c.observe("setValue-after-next", () -> it.setValue("bad"));
  c.observe("remove-first", () -> { it.remove(); return "void"; });
  c.observe("getKey-after-remove", it::getKey); c.observe("getValue-after-remove", it::getValue);
  c.observe("setValue-after-remove", () -> it.setValue("bad"));
  c.observe("double-remove", () -> { it.remove(); return "void"; });
  while (it.hasNext()) {
  c.observe("next-remaining", it::next);
  c.observe("remaining-getValue", it::getValue);
  c.observe("remove-remaining", () -> { it.remove(); return "void"; });
  }
  c.observe("next-at-end", it::next);
  c.observe("getKey-after-end-failure", it::getKey);
  c.observe("getValue-after-end-failure", it::getValue);
  c.law("valid-mapIterator-removals-remove-all-pairs", () -> c.map.size() == 0);
  });
  emit(kind, "flattened-views", "entries-values-removal", "Flattened immutable entries/values support removal.", c -> {
  populate(c);
  Map.Entry<String, String> entry = c.map.entries().iterator().next();
  c.observe("entries-entry-setValue", () -> entry.setValue("bad"));
  c.observe("entries-remove-one-pair", () -> c.map.entries().remove(
  new AbstractMap.SimpleImmutableEntry<>("a", "a0")));
  c.observe("values-remove-a1", () -> c.map.values().remove("a1"));
  c.observe("asMap-group-clear", () -> { c.map.asMap().get("b").clear(); return "void"; });
  c.law("last-group-values-view-removal-cleans-directory", () -> c.map.isEmpty());
  });
 }

 private static void nullsCopiesCapacitiesAndEpochs(String kind) {
  emit(kind, "nulls-copies", "null-pairs-and-copy", "Both allow null; list/set duplicate counts differ.", c -> {
  c.observe("put-null-null-first", () -> c.map.put(null, null));
  c.observe("put-null-null-second", () -> c.map.put(null, null));
  c.observe("put-key-null", () -> c.map.put("k", null));
  c.observe("contains-null-mapping", () -> c.map.containsMapping(null, null));
  MultiValuedMap<String, String> copied = kind.equals("list")
  ? new ArrayListValuedLinkedHashMap<>(c.map)
  : new LinkedHashSetValuedLinkedHashMap<>(c.map);
  c.alias("MultiValuedMap-copy-before-mutation", copied);
  c.observe("copy-add-independent", () -> copied.put("copy-only", "v"));
  c.law("copy-added-key-not-in-source", () -> !c.map.containsKey("copy-only"));
  c.observe("remove-one-null-pair", () -> c.map.removeMapping(null, null));
  c.alias("copy-after-source-mutation", copied);
  Map<String, String> ordinary = new LinkedHashMap<>();
  ordinary.put("first", "v0"); ordinary.put(null, null); ordinary.put("last", "v2");
  c.observe("Map-copy-constructor", () -> kind.equals("list")
  ? new ArrayListValuedLinkedHashMap<>(ordinary)
  : new LinkedHashSetValuedLinkedHashMap<>(ordinary));
  c.observe("null-Map-copy-constructor", () -> kind.equals("list")
  ? new ArrayListValuedLinkedHashMap<>((Map<String, String>) null)
  : new LinkedHashSetValuedLinkedHashMap<>((Map<String, String>) null));
  c.observe("null-MultiValuedMap-copy-constructor", () -> kind.equals("list")
  ? new ArrayListValuedLinkedHashMap<>((MultiValuedMap<String, String>) null)
  : new LinkedHashSetValuedLinkedHashMap<>((MultiValuedMap<String, String>) null));
  });
  for (int[] capacity : new int[][] {{-1, 3}, {0, -1}, {0, 0}, {1, 1}}) {
  int directory = capacity[0], values = capacity[1];
  emit(kind, "capacities", "directory-" + directory + "-values-" + values,
  "Directory validates now; group capacity may validate later.", c -> {
  Outcome constructor = c.observe("two-capacity-constructor", () -> {
  c.map = kind.equals("list")
  ? new ArrayListValuedLinkedHashMap<>(directory, values)
  : new LinkedHashSetValuedLinkedHashMap<>(directory, values);
  return c.map;
  });
  if (constructor.error().isEmpty()) {
  c.observe("first-put-after-construction", () -> c.map.put("a", "x"));
  c.observe("get-add-after-construction", () -> c.map.get("b").add("y"));
  }
  });
  }
  emit(kind, "capacities", "one-value-capacity-negative", "One-int constructor controls deferred group capacity.", c -> {
  c.observe("one-capacity-constructor", () -> {
  c.map = kind.equals("list")
  ? new ArrayListValuedLinkedHashMap<>(-1)
  : new LinkedHashSetValuedLinkedHashMap<>(-1);
  return c.map;
  });
  c.observe("first-put-with-negative-value-capacity", () -> c.map.put("a", "x"));
  });
  emit(kind, "epochs", "empty-group-addAll-and-iterator", "Held iterator epochs are concrete observations.", c -> {
  c.map.putAll("a", List.of("x", "y"));
  Iterator<String> held = c.map.get("a").iterator();
  c.observe("existing-group-addAll-empty", () -> c.map.get("a").addAll(Collections.emptyList()));
  c.observe("held-next-after-empty-addAll", held::next);
  Iterator<String> absent = c.map.get("b").iterator();
  c.observe("create-b-after-absent-iterator", () -> c.map.put("b", "z"));
  c.observe("old-absent-iterator-next", absent::next);
  c.observe("keySet-remove-b", () -> c.map.keySet().remove("b"));
  });
 }

 private static void listConcreteEpochs() {
  emit("list", "epochs", "raw-subList-sort-versus-wrapper", "Wrapped/raw subList sort epochs may differ.", c -> {
  c.map.putAll("a", List.of("c", "b", "a"));
  Iterator<String> beforeWrapper = c.list().get("a").iterator();
  c.observe("wrapper-sort-natural", () -> { c.list().get("a").sort(null); return "void"; });
  c.observe("held-next-after-wrapper-sort", beforeWrapper::next);
  Iterator<String> beforeRaw = c.list().get("a").iterator();
  List<String> raw = c.list().get("a").subList(0, 3);
  c.observe("raw-subList-sort-reverse", () -> { raw.sort(Comparator.reverseOrder()); return "void"; });
  c.observe("held-next-after-raw-sort", beforeRaw::next);
  });
  emit("list", "epochs", "trimToSize-iterator", "Concrete trimToSize visits raw group ArrayLists.", c -> {
  c.map.putAll("a", List.of("x", "y"));
  Iterator<String> held = c.list().get("a").iterator();
  c.observe("concrete-trimToSize", () -> {
  ((ArrayListValuedLinkedHashMap<String, String>) c.map).trimToSize(); return "void";
  });
  c.observe("held-next-after-trim", held::next);
  });
 }

 private static Map<String, Object> object(Object... pairs) {
  if ((pairs.length & 1) != 0) throw new AssertionError("odd JSON field list");
  Map<String, Object> result = new LinkedHashMap<>();
  for (int index = 0; index < pairs.length; index += 2)
  result.put((String) pairs[index], pairs[index + 1]);
  return result;
 }

 private static String json(Object value) {
  if (value == null) return "null";
  if (value instanceof Boolean || value instanceof Number) return value.toString();
  if (value instanceof String text) {
  StringBuilder output = new StringBuilder("\"");
  for (int index = 0; index < text.length(); index++) {
  char character = text.charAt(index);
  switch (character) {
  case '"' -> output.append("\\\"");
  case '\\' -> output.append("\\\\");
  case '\b' -> output.append("\\b");
  case '\f' -> output.append("\\f");
  case '\n' -> output.append("\\n");
  case '\r' -> output.append("\\r");
  case '\t' -> output.append("\\t");
  default -> {
  if (character < 32)
  output.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) character));
  else output.append(character);
  }
  }
  }
  return output.append('"').toString();
  }
  if (value instanceof Map<?, ?> map) {
  StringBuilder output = new StringBuilder("{");
  boolean comma = false;
  for (Map.Entry<?, ?> entry : map.entrySet()) {
  if (comma) output.append(',');
  output.append(json(String.valueOf(entry.getKey()))).append(':').append(json(entry.getValue()));
  comma = true;
  }
  return output.append('}').toString();
  }
  if (value instanceof Iterable<?> values) {
  StringBuilder output = new StringBuilder("[");
  boolean comma = false;
  for (Object item : values) {
  if (comma) output.append(',');
  output.append(json(item)); comma = true;
  }
  return output.append(']').toString();
  }
  throw new AssertionError("unhandled JSON value " + value.getClass().getName());
 }

 private static String artifact(Class<?> provider) {
  var source = provider.getProtectionDomain().getCodeSource();
  if (source == null) throw new AssertionError("actual provider CodeSource required");
  try {
  Path name = Path.of(source.getLocation().toURI()).getFileName();
  if (name == null) throw new AssertionError("actual artifact basename required");
  return name.toString();
  } catch (URISyntaxException failure) {
  throw new AssertionError("invalid provider CodeSource", failure);
  }
 }

 public static void main(String[] args) {
  if (args.length != 0) throw new IllegalArgumentException("no mutable probe options");
  for (String kind : List.of("list", "set")) {
  absentViews(kind);
  removeAndDescriptorAliases(kind);
  countedKeysAndMapIterator(kind);
  nullsCopiesCapacitiesAndEpochs(kind);
  }
  listPublicationAndAliases();
  wrappedListDefaults();
  setViews();
  listConcreteEpochs();
  System.out.println(json(object("summary", "APACHE_MULTIVALUE_PREIMAGE_ORACLE_INVENTORY",
  "source", DONOR, "cases", cases, "operations", operations, "groups", groups,
  "apiLawWitnesses", lawWitnesses, "apiLawFailures", lawFailures,
  "fixtureFailures", fixtureFailures,
  "listProviderArtifact", artifact(ArrayListValuedLinkedHashMap.class),
  "setProviderArtifact", artifact(LinkedHashSetValuedLinkedHashMap.class),
  "implementationVersion", ArrayListValuedLinkedHashMap.class.getPackage().getImplementationVersion(),
  "candidates", 0, "admission", "NONE",
  "scope", "Runtime observations when executed; hypotheses and API-law witnesses are separate.")));
  if (cases != 65) throw new AssertionError("expected 65 fixed oracle cases");
  if (fixtureFailures != 0) throw new AssertionError("probe fixture failure; full observations retained");
 }
}
