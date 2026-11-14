// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3SystemOneTest {
  private static final String MODEL = "a".repeat(64);
  private static final String TOKENIZER = "b".repeat(64);
  private static final String CONTEXT = "c".repeat(64);

  @Test
  void compilesTypedQuestionsAndDetachesMutableLists() {
    var options = new ArrayList<>(List.of(
        new M3SystemOneSchema.ChoiceOption("yes", "supported"),
        new M3SystemOneSchema.ChoiceOption("no", null)));
    var levels = new ArrayList<>(List.of("none", "some", "complete"));
    var choice = new M3SystemOneSchema.ChoiceQuestion("route", "choose", options);
    var score = M3SystemOneSchema.ScoreQuestion.of("coverage", null, levels);
    var questions = new ArrayList<M3SystemOneSchema.Question>(List.of(
        new M3SystemOneSchema.NoulQuestion("valid", null, "supported", "unsupported"),
        choice, score));
    var schema = M3SystemOneSchema.compile(questions);
    String root = schema.rootHash();
    options.clear();
    levels.set(0, "changed");
    questions.clear();

    assertEquals(3, schema.size());
    assertEquals(6, schema.probabilityWidth());
    assertEquals(M3SystemOneSchema.Kind.NOUL, schema.question(0).kind());
    assertEquals(M3SystemOneSchema.Kind.CHOICE, schema.question(1).kind());
    assertEquals(M3SystemOneSchema.Kind.SCORE, schema.question(2).kind());
    assertEquals(List.of("none", "some", "complete"), score.levels());
    assertEquals(2, choice.options().size());
    assertEquals(1, schema.indexOf(new StringBuilder("route")));
    assertEquals(-1, schema.indexOf("missing"));
    assertEquals(root, schema.rootHash());
    assertThrows(UnsupportedOperationException.class, () -> schema.questions().clear());
    assertThrows(UnsupportedOperationException.class, () -> choice.options().clear());
    assertThrows(UnsupportedOperationException.class, () -> score.levels().clear());
    assertThrows(IndexOutOfBoundsException.class, () -> schema.question(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> schema.question(3));
    assertThrows(NullPointerException.class, () -> schema.indexOf(null));
  }

  @Test
  void schemaDigestUsesIndependentRawUtf16FramingOracle() throws Exception {
    String id = "valid-\ud800";
    var schema = M3SystemOneSchema.compile(List.of(
        new M3SystemOneSchema.NoulQuestion(id, null, "", "\udfff")));
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(bytes)) {
      writeText(out, "SYNEXIA_MINDEX_SYSTEM_ONE_SCHEMA_UTF16_V1");
      out.writeInt(1);
      writeText(out, id);
      writeText(out, "NOUL");
      writeText(out, null);
      writeText(out, "");
      writeText(out, "\udfff");
    }
    String expected = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    assertEquals(expected, schema.rootHash());
    assertNotEquals(schema.rootHash(), singleNoul(id, "", "", "\udfff").rootHash());
    assertNotEquals(schema.rootHash(), singleNoul("valid-?", null, "", "\udfff").rootHash());
    assertNotEquals(schema.rootHash(), singleNoul(id, null, "\udfff", "").rootHash());
  }

  @Test
  void schemaIdentityBindsQuestionOrderNamesCriteriaAndOrderedRubric() {
    var a = new M3SystemOneSchema.ChoiceOption("a", null);
    var b = new M3SystemOneSchema.ChoiceOption("b", "rule");
    var choice = new M3SystemOneSchema.ChoiceQuestion("choice", null, List.of(a, b));
    var score = new M3SystemOneSchema.ScoreQuestion("score", null, List.of("low", "high"));
    var original = M3SystemOneSchema.compile(List.of(choice, score));
    assertEquals(original.rootHash(), M3SystemOneSchema.compile(List.of(choice, score)).rootHash());
    assertNotEquals(original.rootHash(), M3SystemOneSchema.compile(List.of(score, choice)).rootHash());
    List<M3SystemOneSchema.Question> changed = List.of(
        new M3SystemOneSchema.ChoiceQuestion("choice", "", List.of(a, b)),
        new M3SystemOneSchema.ChoiceQuestion("other", null, List.of(a, b)),
        new M3SystemOneSchema.ChoiceQuestion("choice", null, List.of(b, a)),
        new M3SystemOneSchema.ChoiceQuestion("choice", null,
            List.of(new M3SystemOneSchema.ChoiceOption("a", ""), b)),
        new M3SystemOneSchema.ChoiceQuestion("choice", null,
            List.of(new M3SystemOneSchema.ChoiceOption("c", null), b)));
    for (var replacement : changed) {
      assertNotEquals(original.rootHash(),
          M3SystemOneSchema.compile(List.of(replacement, score)).rootHash());
    }
    assertNotEquals(original.rootHash(), M3SystemOneSchema.compile(List.of(choice,
        new M3SystemOneSchema.ScoreQuestion("score", null, List.of("high", "low")))).rootHash());
  }

  @Test
  void rejectsAmbiguousOrIncompleteQuestionStructures() {
    var no = new M3SystemOneSchema.NoulQuestion("id", null, null, null);
    var a = new M3SystemOneSchema.ChoiceOption("a", null);
    assertThrows(IllegalArgumentException.class, () -> M3SystemOneSchema.compile(List.of()));
    assertThrows(IllegalArgumentException.class, () -> M3SystemOneSchema.compile(List.of(no, no)));
    assertThrows(NullPointerException.class, () -> M3SystemOneSchema.compile(null));
    assertThrows(NullPointerException.class,
        () -> M3SystemOneSchema.compile(Collections.singletonList(null)));
    assertThrows(IllegalArgumentException.class,
        () -> new M3SystemOneSchema.ChoiceQuestion("id", null, List.of(a)));
    assertThrows(IllegalArgumentException.class,
        () -> new M3SystemOneSchema.ChoiceQuestion("id", null,
            List.of(a, new M3SystemOneSchema.ChoiceOption(new String("a"), "other"))));
    assertThrows(IllegalArgumentException.class,
        () -> new M3SystemOneSchema.ChoiceQuestion("id", null, Collections.nCopies(256, a)));
    assertThrows(IllegalArgumentException.class,
        () -> new M3SystemOneSchema.ScoreQuestion("id", null, List.of()));
    assertThrows(IllegalArgumentException.class,
        () -> new M3SystemOneSchema.ScoreQuestion("id", null, Collections.nCopies(256, "level")));
    assertThrows(IllegalArgumentException.class,
        () -> new M3SystemOneSchema.ScoreQuestion("id", null, List.of(" ")));
    assertEquals(255, new M3SystemOneSchema.ScoreQuestion(
        "id", null, Collections.nCopies(255, "level")).probabilityWidth());
    for (String bad : List.of("", " ", "x\0y")) {
      assertThrows(IllegalArgumentException.class,
          () -> new M3SystemOneSchema.NoulQuestion(bad, null, null, null));
      assertThrows(IllegalArgumentException.class,
          () -> new M3SystemOneSchema.ChoiceOption(bad, null));
    }
    assertThrows(IllegalArgumentException.class,
        () -> new M3SystemOneSchema.NoulQuestion("id", "\0", null, null));
    assertThrows(IllegalArgumentException.class,
        () -> new M3SystemOneSchema.NoulQuestion("id", null, "\0", null));
    assertThrows(IllegalArgumentException.class,
        () -> new M3SystemOneSchema.ChoiceOption("name", "\0"));
  }

  @Test
  void preparationRetainsExactStateAndBindsAllRequestDependencies() {
    String state = new String("source\0\ud800");
    var schema = singleNoul("valid", null, "yes", "no");
    var request = new M3SystemOneRequest("provider-model", state, schema);
    var input = request.prepare(MODEL, TOKENIZER, CONTEXT);
    assertSame(state, request.state());
    assertSame(state, input.text());
    assertEquals(schema.rootHash(), input.namespace().schemaRoot());
    assertTrue(input.matches(request.prepare(MODEL, TOKENIZER, CONTEXT)));
    assertFalse(input.matches(request.prepare("d".repeat(64), TOKENIZER, CONTEXT)));
    assertFalse(input.matches(request.prepare(MODEL, "d".repeat(64), CONTEXT)));
    assertFalse(input.matches(request.prepare(MODEL, TOKENIZER, "d".repeat(64))));
    assertFalse(input.matches(new M3SystemOneRequest("other-model", state, schema)
        .prepare(MODEL, TOKENIZER, CONTEXT)));
    assertFalse(input.matches(new M3SystemOneRequest("provider-model", state,
        singleNoul("valid", "changed", "yes", "no")).prepare(MODEL, TOKENIZER, CONTEXT)));
    assertFalse(input.matches(new M3SystemOneRequest("provider-model", state + "x", schema)
        .prepare(MODEL, TOKENIZER, CONTEXT)));
    assertThrows(IllegalArgumentException.class, () -> request.prepare("latest", TOKENIZER, CONTEXT));
    assertThrows(IllegalArgumentException.class, () -> request.prepare(MODEL, TOKENIZER, "latest"));
    assertThrows(IllegalArgumentException.class, () -> new M3SystemOneRequest(" ", state, schema));
    assertThrows(NullPointerException.class, () -> new M3SystemOneRequest("model", null, schema));
    assertThrows(NullPointerException.class, () -> new M3SystemOneRequest("model", state, null));
  }

  private static M3SystemOneSchema singleNoul(
      String id, String instructions, String whenTrue, String whenFalse) {
    return M3SystemOneSchema.compile(List.of(
        new M3SystemOneSchema.NoulQuestion(id, instructions, whenTrue, whenFalse)));
  }

  private static void writeText(DataOutputStream out, String value) throws Exception {
    out.writeInt(value == null ? -1 : value.length());
    if (value != null) out.writeChars(value);
  }
}
