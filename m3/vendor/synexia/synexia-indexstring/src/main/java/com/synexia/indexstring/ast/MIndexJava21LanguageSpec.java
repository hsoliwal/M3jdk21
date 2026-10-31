// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import com.sun.source.tree.DirectiveTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.PatternTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import java.util.Arrays;
import java.util.Objects;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;

/**
 * Frozen Java 21 compiler-tree specification image.
 *
 * <p>Everything here is process-constant. The complete rule table is built once at class
 * initialization and then addressed directly by Tree.Kind.ordinal(). No source file, AST or parser
 * reloads language metadata.</p>
 */
final class MIndexJava21LanguageSpec implements MIndexLanguageSpec {
  static final MIndexJava21LanguageSpec INSTANCE = new MIndexJava21LanguageSpec();

  private static final String[] KEYWORDS =
      sorted(
          "_",
          "abstract",
          "assert",
          "boolean",
          "break",
          "byte",
          "case",
          "catch",
          "char",
          "class",
          "const",
          "continue",
          "default",
          "do",
          "double",
          "else",
          "enum",
          "extends",
          "final",
          "finally",
          "float",
          "for",
          "goto",
          "if",
          "implements",
          "import",
          "instanceof",
          "int",
          "interface",
          "long",
          "native",
          "new",
          "package",
          "private",
          "protected",
          "public",
          "return",
          "short",
          "static",
          "strictfp",
          "super",
          "switch",
          "synchronized",
          "this",
          "throw",
          "throws",
          "transient",
          "try",
          "void",
          "volatile",
          "while");

  private static final String[] CONTEXTUAL_KEYWORDS =
      sorted(
          "exports",
          "module",
          "non-sealed",
          "open",
          "opens",
          "permits",
          "provides",
          "record",
          "requires",
          "sealed",
          "to",
          "transitive",
          "uses",
          "var",
          "when",
          "with",
          "yield");

  private static final String[] LITERAL_TOKENS = sorted("false", "null", "true");

  private static final String[] PRIMITIVE_TYPES =
      sorted("boolean", "byte", "char", "double", "float", "int", "long", "short");

  private static final String[] MODIFIER_TOKENS =
      sorted(
          "abstract",
          "default",
          "final",
          "native",
          "non-sealed",
          "private",
          "protected",
          "public",
          "sealed",
          "static",
          "strictfp",
          "synchronized",
          "transient",
          "volatile");

  private static final long MODIFIER_MASK = buildModifierMask();

  private final MIndexLanguageRule[] rules;

  private MIndexJava21LanguageSpec() {
    Tree.Kind[] kinds = Tree.Kind.values();
    rules = new MIndexLanguageRule[kinds.length];
    for (Tree.Kind kind : kinds) rules[kind.ordinal()] = buildRule(kind);
  }

  @Override
  public String identity() {
    return "java-21";
  }

  @Override
  public String language() {
    return "java";
  }

  @Override
  public int release() {
    return 21;
  }

  @Override
  public SourceVersion sourceVersion() {
    return SourceVersion.RELEASE_21;
  }

  @Override
  public int ruleCount() {
    return rules.length;
  }

  @Override
  public MIndexLanguageRule rule(Tree.Kind kind) {
    return rules[Objects.requireNonNull(kind, "kind").ordinal()];
  }

  @Override
  public MIndexLanguageRule rule(int ruleIndex) {
    return rules[Objects.checkIndex(ruleIndex, rules.length)];
  }

  @Override
  public int keywordCount() {
    return KEYWORDS.length;
  }

  @Override
  public String keywordAt(int index) {
    return KEYWORDS[Objects.checkIndex(index, KEYWORDS.length)];
  }

  @Override
  public boolean isKeyword(CharSequence token) {
    return contains(KEYWORDS, token);
  }

  @Override
  public int contextualKeywordCount() {
    return CONTEXTUAL_KEYWORDS.length;
  }

  @Override
  public String contextualKeywordAt(int index) {
    return CONTEXTUAL_KEYWORDS[Objects.checkIndex(index, CONTEXTUAL_KEYWORDS.length)];
  }

  @Override
  public boolean isContextualKeyword(CharSequence token) {
    return contains(CONTEXTUAL_KEYWORDS, token);
  }

  @Override
  public int literalTokenCount() {
    return LITERAL_TOKENS.length;
  }

  @Override
  public String literalTokenAt(int index) {
    return LITERAL_TOKENS[Objects.checkIndex(index, LITERAL_TOKENS.length)];
  }

  @Override
  public boolean isLiteralToken(CharSequence token) {
    return contains(LITERAL_TOKENS, token);
  }

  @Override
  public int primitiveTypeCount() {
    return PRIMITIVE_TYPES.length;
  }

  @Override
  public String primitiveTypeAt(int index) {
    return PRIMITIVE_TYPES[Objects.checkIndex(index, PRIMITIVE_TYPES.length)];
  }

  @Override
  public boolean isPrimitiveType(CharSequence token) {
    return contains(PRIMITIVE_TYPES, token);
  }

  @Override
  public int modifierCount() {
    return MODIFIER_TOKENS.length;
  }

  @Override
  public String modifierAt(int index) {
    return MODIFIER_TOKENS[Objects.checkIndex(index, MODIFIER_TOKENS.length)];
  }

  @Override
  public boolean isModifierKeyword(CharSequence token) {
    return contains(MODIFIER_TOKENS, token);
  }

  @Override
  public long modifierMask() {
    return MODIFIER_MASK;
  }

  private static MIndexLanguageRule buildRule(Tree.Kind kind) {
    String name = kind.name();
    long categories = categories(kind);
    MIndexOperatorRule operator = operator(name);
    if (operator.isOperator()) categories |= MIndexSyntaxCategory.OPERATOR.bit();
    if (categories == 0L) categories = MIndexSyntaxCategory.OTHER.bit();

    return new MIndexLanguageRule(
        kind.ordinal(),
        kind,
        categories,
        labelPolicy(name),
        flagPolicy(name),
        operator,
        minimumRelease(name),
        isPreview(name));
  }

  private static long categories(Tree.Kind kind) {
    String name = kind.name();
    long mask = 0L;
    Class<? extends Tree> api = kind.asInterface();

    if (api != null) {
      if (ExpressionTree.class.isAssignableFrom(api)) mask |= MIndexSyntaxCategory.EXPRESSION.bit();
      if (StatementTree.class.isAssignableFrom(api)) mask |= MIndexSyntaxCategory.STATEMENT.bit();
      if (PatternTree.class.isAssignableFrom(api)) mask |= MIndexSyntaxCategory.PATTERN.bit();
      if (DirectiveTree.class.isAssignableFrom(api)) mask |= MIndexSyntaxCategory.DIRECTIVE.bit();
    }

    if (isCompilation(name)) mask |= MIndexSyntaxCategory.COMPILATION.bit();
    if (isDeclaration(name)) mask |= MIndexSyntaxCategory.DECLARATION.bit();
    if (isType(name)) mask |= MIndexSyntaxCategory.TYPE.bit();
    if (isLiteral(name)) mask |= MIndexSyntaxCategory.LITERAL.bit();
    if (isControlFlow(name)) mask |= MIndexSyntaxCategory.CONTROL_FLOW.bit();
    if (isName(name)) mask |= MIndexSyntaxCategory.NAME.bit();
    if (isAnnotation(name)) mask |= MIndexSyntaxCategory.ANNOTATION.bit();
    if (isModule(name)) mask |= MIndexSyntaxCategory.MODULE.bit();
    if ("ERRONEOUS".equals(name)) mask |= MIndexSyntaxCategory.ERROR.bit();
    return mask;
  }

  private static boolean isCompilation(String name) {
    return switch (name) {
      case "COMPILATION_UNIT", "PACKAGE", "IMPORT" -> true;
      default -> false;
    };
  }

  private static boolean isDeclaration(String name) {
    return switch (name) {
      case "CLASS",
          "INTERFACE",
          "ENUM",
          "ANNOTATION_TYPE",
          "RECORD",
          "METHOD",
          "VARIABLE",
          "TYPE_PARAMETER",
          "PACKAGE",
          "MODULE" -> true;
      default -> false;
    };
  }

  private static boolean isType(String name) {
    return switch (name) {
      case "PRIMITIVE_TYPE",
          "ARRAY_TYPE",
          "PARAMETERIZED_TYPE",
          "UNION_TYPE",
          "INTERSECTION_TYPE",
          "TYPE_PARAMETER",
          "ANNOTATED_TYPE",
          "UNBOUNDED_WILDCARD",
          "EXTENDS_WILDCARD",
          "SUPER_WILDCARD" -> true;
      default -> false;
    };
  }

  private static boolean isLiteral(String name) {
    return name.endsWith("_LITERAL");
  }

  private static boolean isControlFlow(String name) {
    return switch (name) {
      case "IF",
          "SWITCH",
          "SWITCH_EXPRESSION",
          "CASE",
          "FOR_LOOP",
          "ENHANCED_FOR_LOOP",
          "WHILE_LOOP",
          "DO_WHILE_LOOP",
          "TRY",
          "CATCH",
          "CONDITIONAL_EXPRESSION",
          "BREAK",
          "CONTINUE",
          "RETURN",
          "THROW",
          "YIELD",
          "SYNCHRONIZED" -> true;
      default -> false;
    };
  }

  private static boolean isName(String name) {
    return switch (name) {
      case "IDENTIFIER", "MEMBER_SELECT", "MEMBER_REFERENCE" -> true;
      default -> false;
    };
  }

  private static boolean isAnnotation(String name) {
    return switch (name) {
      case "ANNOTATION", "TYPE_ANNOTATION", "ANNOTATED_TYPE" -> true;
      default -> false;
    };
  }

  private static boolean isModule(String name) {
    return switch (name) {
      case "MODULE", "EXPORTS", "OPENS", "PROVIDES", "REQUIRES", "USES" -> true;
      default -> false;
    };
  }

  private static MIndexLabelPolicy labelPolicy(String name) {
    if (name.endsWith("_LITERAL")) return MIndexLabelPolicy.LITERAL_VALUE;
    return switch (name) {
      case "IDENTIFIER", "MEMBER_SELECT", "MEMBER_REFERENCE" -> MIndexLabelPolicy.IDENTIFIER;
      case "CLASS",
          "INTERFACE",
          "ENUM",
          "ANNOTATION_TYPE",
          "RECORD",
          "METHOD",
          "VARIABLE",
          "TYPE_PARAMETER" -> MIndexLabelPolicy.DECLARATION_NAME;
      case "PRIMITIVE_TYPE" -> MIndexLabelPolicy.PRIMITIVE_TYPE;
      case "LABELED_STATEMENT", "BREAK", "CONTINUE" -> MIndexLabelPolicy.SOURCE_LABEL;
      default -> MIndexLabelPolicy.NONE;
    };
  }

  private static MIndexFlagPolicy flagPolicy(String name) {
    return switch (name) {
      case "MODIFIERS" -> MIndexFlagPolicy.MODIFIER_BITS;
      case "IMPORT" -> MIndexFlagPolicy.BOOLEAN;
      case "MEMBER_REFERENCE", "CASE", "LAMBDA_EXPRESSION", "MODULE" ->
          MIndexFlagPolicy.ENUM_ORDINAL_PLUS_ONE;
      default -> MIndexFlagPolicy.NONE;
    };
  }

  private static int minimumRelease(String name) {
    return switch (name) {
      case "ASSERT" -> 4;
      case "ANNOTATION", "TYPE_ANNOTATION", "ANNOTATION_TYPE", "ENUM",
          "ENHANCED_FOR_LOOP", "TYPE_PARAMETER",
          "PARAMETERIZED_TYPE", "UNBOUNDED_WILDCARD", "EXTENDS_WILDCARD", "SUPER_WILDCARD" -> 5;
      case "UNION_TYPE" -> 7;
      case "LAMBDA_EXPRESSION", "MEMBER_REFERENCE", "ANNOTATED_TYPE", "INTERSECTION_TYPE" -> 8;
      case "MODULE", "EXPORTS", "OPENS", "PROVIDES", "REQUIRES", "USES" -> 9;
      case "SWITCH_EXPRESSION", "YIELD" -> 14;
      case "RECORD", "BINDING_PATTERN" -> 16;
      case "TEMPLATE",
          "ANY_PATTERN",
          "DEFAULT_CASE_LABEL",
          "CONSTANT_CASE_LABEL",
          "PATTERN_CASE_LABEL",
          "DECONSTRUCTION_PATTERN",
          "PARENTHESIZED_PATTERN",
          "GUARDED_PATTERN" -> 21;
      default -> 1;
    };
  }

  private static boolean isPreview(String name) {
    return "TEMPLATE".equals(name) || "ANY_PATTERN".equals(name);
  }

  private static MIndexOperatorRule operator(String name) {
    return switch (name) {
      case "POSTFIX_INCREMENT" -> op("++", 16, MIndexAssociativity.LEFT, 1, false, false);
      case "POSTFIX_DECREMENT" -> op("--", 16, MIndexAssociativity.LEFT, 1, false, false);
      case "PREFIX_INCREMENT" -> op("++", 15, MIndexAssociativity.RIGHT, 1, false, false);
      case "PREFIX_DECREMENT" -> op("--", 15, MIndexAssociativity.RIGHT, 1, false, false);
      case "UNARY_PLUS" -> op("+", 15, MIndexAssociativity.RIGHT, 1, false, false);
      case "UNARY_MINUS" -> op("-", 15, MIndexAssociativity.RIGHT, 1, false, false);
      case "BITWISE_COMPLEMENT" -> op("~", 15, MIndexAssociativity.RIGHT, 1, false, false);
      case "LOGICAL_COMPLEMENT" -> op("!", 15, MIndexAssociativity.RIGHT, 1, false, false);
      case "TYPE_CAST" -> op("(type)", 14, MIndexAssociativity.RIGHT, 1, false, false);
      case "MULTIPLY" -> op("*", 13, MIndexAssociativity.LEFT, 2, false, false);
      case "DIVIDE" -> op("/", 13, MIndexAssociativity.LEFT, 2, false, false);
      case "REMAINDER" -> op("%", 13, MIndexAssociativity.LEFT, 2, false, false);
      case "PLUS" -> op("+", 12, MIndexAssociativity.LEFT, 2, false, false);
      case "MINUS" -> op("-", 12, MIndexAssociativity.LEFT, 2, false, false);
      case "LEFT_SHIFT" -> op("<<", 11, MIndexAssociativity.LEFT, 2, false, false);
      case "RIGHT_SHIFT" -> op(">>", 11, MIndexAssociativity.LEFT, 2, false, false);
      case "UNSIGNED_RIGHT_SHIFT" -> op(">>>", 11, MIndexAssociativity.LEFT, 2, false, false);
      case "LESS_THAN" -> op("<", 10, MIndexAssociativity.LEFT, 2, false, false);
      case "GREATER_THAN" -> op(">", 10, MIndexAssociativity.LEFT, 2, false, false);
      case "LESS_THAN_EQUAL" -> op("<=", 10, MIndexAssociativity.LEFT, 2, false, false);
      case "GREATER_THAN_EQUAL" -> op(">=", 10, MIndexAssociativity.LEFT, 2, false, false);
      case "INSTANCE_OF" -> op("instanceof", 10, MIndexAssociativity.LEFT, 2, false, false);
      case "EQUAL_TO" -> op("==", 9, MIndexAssociativity.LEFT, 2, false, false);
      case "NOT_EQUAL_TO" -> op("!=", 9, MIndexAssociativity.LEFT, 2, false, false);
      case "AND" -> op("&", 8, MIndexAssociativity.LEFT, 2, false, false);
      case "XOR" -> op("^", 7, MIndexAssociativity.LEFT, 2, false, false);
      case "OR" -> op("|", 6, MIndexAssociativity.LEFT, 2, false, false);
      case "CONDITIONAL_AND" -> op("&&", 5, MIndexAssociativity.LEFT, 2, true, false);
      case "CONDITIONAL_OR" -> op("||", 4, MIndexAssociativity.LEFT, 2, true, false);
      case "CONDITIONAL_EXPRESSION" -> op("?:", 3, MIndexAssociativity.RIGHT, 3, true, false);
      case "ASSIGNMENT" -> op("=", 2, MIndexAssociativity.RIGHT, 2, false, true);
      case "MULTIPLY_ASSIGNMENT" -> op("*=", 2, MIndexAssociativity.RIGHT, 2, false, true);
      case "DIVIDE_ASSIGNMENT" -> op("/=", 2, MIndexAssociativity.RIGHT, 2, false, true);
      case "REMAINDER_ASSIGNMENT" -> op("%=", 2, MIndexAssociativity.RIGHT, 2, false, true);
      case "PLUS_ASSIGNMENT" -> op("+=", 2, MIndexAssociativity.RIGHT, 2, false, true);
      case "MINUS_ASSIGNMENT" -> op("-=", 2, MIndexAssociativity.RIGHT, 2, false, true);
      case "LEFT_SHIFT_ASSIGNMENT" -> op("<<=", 2, MIndexAssociativity.RIGHT, 2, false, true);
      case "RIGHT_SHIFT_ASSIGNMENT" -> op(">>=", 2, MIndexAssociativity.RIGHT, 2, false, true);
      case "UNSIGNED_RIGHT_SHIFT_ASSIGNMENT" -> op(">>>=", 2, MIndexAssociativity.RIGHT, 2, false, true);
      case "AND_ASSIGNMENT" -> op("&=", 2, MIndexAssociativity.RIGHT, 2, false, true);
      case "XOR_ASSIGNMENT" -> op("^=", 2, MIndexAssociativity.RIGHT, 2, false, true);
      case "OR_ASSIGNMENT" -> op("|=", 2, MIndexAssociativity.RIGHT, 2, false, true);
      default -> MIndexOperatorRule.NONE;
    };
  }

  private static long buildModifierMask() {
    long mask = 0L;
    for (Modifier modifier : Modifier.values()) {
      if (modifier.ordinal() >= Long.SIZE) {
        throw new ExceptionInInitializerError("Modifier no longer fits 64-bit MIndex flag lane");
      }
      mask |= 1L << modifier.ordinal();
    }
    return mask;
  }

  private static MIndexOperatorRule op(
      String token,
      int precedence,
      MIndexAssociativity associativity,
      int arity,
      boolean shortCircuit,
      boolean assignment) {
    return new MIndexOperatorRule(token, precedence, associativity, arity, shortCircuit, assignment);
  }

  private static String[] sorted(String... values) {
    String[] copy = values.clone();
    Arrays.sort(copy);
    return copy;
  }

  private static boolean contains(String[] values, CharSequence token) {
    Objects.requireNonNull(token, "token");
    int low = 0;
    int high = values.length - 1;
    while (low <= high) {
      int mid = (low + high) >>> 1;
      int comparison = compare(values[mid], token);
      if (comparison < 0) low = mid + 1;
      else if (comparison > 0) high = mid - 1;
      else return true;
    }
    return false;
  }

  private static int compare(String left, CharSequence right) {
    int common = Math.min(left.length(), right.length());
    for (int index = 0; index < common; index++) {
      char a = left.charAt(index);
      char b = right.charAt(index);
      if (a != b) return Character.compare(a, b);
    }
    return Integer.compare(left.length(), right.length());
  }
}
