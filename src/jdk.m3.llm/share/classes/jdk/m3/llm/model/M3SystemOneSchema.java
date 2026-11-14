// SPDX-License-Identifier: Apache-2.0
// Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.llm.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable request structure for the public Jev NOUL, Choice and Score question shapes.
 *
 * <p>This independent product contract validates question structure and exact schema identity.
 * It does not implement Jev, invoke a provider, produce probabilities or validate model quality.
 * Text uses ordinary immutable Strings; no additional text pool is introduced.</p>
 */
public final class M3SystemOneSchema {
  public static final int MAX_LEVELS = 255;

  public enum Kind { NOUL, CHOICE, SCORE }

  /** One named Choice outcome. A null criterion means the name defines the outcome. */
  public record ChoiceOption(String name, String criterion) {
    public ChoiceOption {
      name = required(name, "choice name");
      criterion = optional(criterion);
    }
  }

  public sealed interface Question permits NoulQuestion, ChoiceQuestion, ScoreQuestion {
    String id();
    String instructions();
    Kind kind();

    /** Required number of probability components for a complete answer. */
    int probabilityWidth();
  }

  /** Yes/no probability. Public Jev NOUL has no separate confidence field. */
  public record NoulQuestion(String id, String instructions, String whenTrue, String whenFalse)
      implements Question {
    public NoulQuestion {
      id = required(id, "question id");
      instructions = optional(instructions);
      whenTrue = optional(whenTrue);
      whenFalse = optional(whenFalse);
    }

    @Override
    public Kind kind() { return Kind.NOUL; }

    @Override
    public int probabilityWidth() { return 1; }
  }

  /** Finite named alternatives; a complete answer contains every alternative's probability. */
  public record ChoiceQuestion(String id, String instructions, List<ChoiceOption> options)
      implements Question {
    public ChoiceQuestion {
      id = required(id, "question id");
      instructions = optional(instructions);
      options = List.copyOf(Objects.requireNonNull(options, "options"));
      if (options.size() < 2 || options.size() > MAX_LEVELS) {
        throw new IllegalArgumentException("choice requires 2.." + MAX_LEVELS + " options");
      }
      Set<String> names = new HashSet<>();
      for (ChoiceOption option : options) {
        ChoiceOption checked = Objects.requireNonNull(option, "choice option");
        if (!names.add(checked.name())) {
          throw new IllegalArgumentException("duplicate choice option " + checked.name());
        }
      }
    }

    @Override
    public Kind kind() { return Kind.CHOICE; }

    @Override
    public int probabilityWidth() { return options.size(); }
  }

  /** Ordered rubric. Level position is its numeric score, beginning at zero. */
  public record ScoreQuestion(String id, String instructions, List<String> levels)
      implements Question {
    public ScoreQuestion {
      id = required(id, "question id");
      instructions = optional(instructions);
      levels = List.copyOf(Objects.requireNonNull(levels, "levels"));
      if (levels.isEmpty() || levels.size() > MAX_LEVELS) {
        throw new IllegalArgumentException("score requires 1.." + MAX_LEVELS + " levels");
      }
      ArrayList<String> checked = new ArrayList<>(levels.size());
      for (String level : levels) checked.add(required(level, "score level"));
      levels = List.copyOf(checked);
    }

    public static ScoreQuestion of(String id, String instructions, List<String> levels) {
      return new ScoreQuestion(id, instructions, levels);
    }

    @Override
    public Kind kind() { return Kind.SCORE; }

    @Override
    public int probabilityWidth() { return levels.size(); }
  }

  private final List<Question> questions;
  private final int probabilityWidth;
  private final String rootHash;

  private M3SystemOneSchema(List<Question> questions) {
    this.questions = questions;
    int width = 0;
    for (Question question : questions) width = Math.addExact(width, question.probabilityWidth());
    probabilityWidth = width;
    rootHash = computeRoot(questions);
  }

  public static M3SystemOneSchema compile(List<? extends Question> questions) {
    Objects.requireNonNull(questions, "questions");
    if (questions.isEmpty()) {
      throw new IllegalArgumentException("System One schema requires at least one question");
    }
    ArrayList<Question> copy = new ArrayList<>(questions.size());
    Set<String> ids = new HashSet<>();
    for (int index = 0; index < questions.size(); index++) {
      Question question = Objects.requireNonNull(questions.get(index), "questions[" + index + "]");
      if (!ids.add(question.id())) {
        throw new IllegalArgumentException("duplicate System One question " + question.id());
      }
      copy.add(question);
    }
    return new M3SystemOneSchema(List.copyOf(copy));
  }

  public int size() { return questions.size(); }

  public Question question(int index) {
    return questions.get(Objects.checkIndex(index, questions.size()));
  }

  public List<Question> questions() { return questions; }
  public int probabilityWidth() { return probabilityWidth; }

  public int indexOf(CharSequence id) {
    String wanted = Objects.requireNonNull(id, "id").toString();
    for (int index = 0; index < questions.size(); index++) {
      if (questions.get(index).id().equals(wanted)) return index;
    }
    return -1;
  }

  /** Exact framed UTF-16 schema identity, preserving order and null versus empty text. */
  public String rootHash() { return rootHash; }

  private static String computeRoot(List<Question> questions) {
    M3ModelInput.ExactHash hash =
        new M3ModelInput.ExactHash("SYNEXIA_MINDEX_SYSTEM_ONE_SCHEMA_UTF16_V1");
    hash.integer(questions.size());
    for (Question question : questions) {
      hash.text(question.id());
      hash.text(question.kind().name());
      hash.text(question.instructions());
      switch (question) {
        case NoulQuestion noul -> {
          hash.text(noul.whenTrue());
          hash.text(noul.whenFalse());
        }
        case ChoiceQuestion choice -> {
          hash.integer(choice.options().size());
          for (ChoiceOption option : choice.options()) {
            hash.text(option.name());
            hash.text(option.criterion());
          }
        }
        case ScoreQuestion score -> {
          hash.integer(score.levels().size());
          for (String level : score.levels()) hash.text(level);
        }
      }
    }
    return hash.finish();
  }

  private static String required(String value, String field) {
    String checked = Objects.requireNonNull(value, field);
    if (checked.isBlank() || checked.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("invalid " + field);
    }
    return checked;
  }

  private static String optional(String value) {
    if (value != null && value.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("text contains NUL");
    }
    return value;
  }
}
