// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;

/**
 * Builds typed {@link M3Atom}s from javac declaration trees. Every text is the exact source
 * span of a javac subtree (whitespace collapsed), so the projection is identical for a
 * parse-only CLI tree and the compiler's own tree inside the plugin.
 *
 * <p>Exposure and the declaration contract port Synexia M3FileAtomContract (#13966) with the
 * open ABI corrections of #14042 (modifiers in the contract, narrowing is drift), #14066
 * (accessible type headers), #14069 (record components), #14084 (enum constant order) and
 * #14100 (exposed constant initializers).</p>
 */
//FLLDCIM: MOP — one projection for javac Plugin and CLI trees; spans, not pretty-printing
final class M3AtomTyping {
    static final String SHAPE_DOMAIN = "M3_MOP_SHAPE_V1";
    private static final Set<String> CONSTANT_TYPES = Set.of("boolean", "byte", "char", "short",
            "int", "long", "float", "double", "String", "java.lang.String");

    private M3AtomTyping() { }

    /** One parsed compilation unit with its exact text. */
    record Source(String path, String packageName, CompilationUnitTree unit,
            SourcePositions positions, String text) {
        long start(Tree tree) { return positions.getStartPosition(unit, tree); }

        long end(Tree tree) { return positions.getEndPosition(unit, tree); }

        boolean spans(Tree tree) {
            return tree != null && start(tree) >= 0 && end(tree) > start(tree)
                    && end(tree) <= text.length();
        }

        String clean(Tree tree) {
            if (tree == null) return "";
            return M3Atom.clean(spans(tree) ? text.substring((int) start(tree), (int) end(tree))
                    : tree.toString());
        }

        int line(long position) {
            return Math.toIntExact(Math.max(1L, unit.getLineMap().getLineNumber(Math.max(0L, position))));
        }
    }

    /** Enclosing named type: qualified name, reachability and implicit public members. */
    record Owner(String qualified, boolean reachable, boolean implicitPublic, Tree.Kind kind) { }

    private record Typing(String inputs, String output, String effects, String failure,
            String limits) { }

    private record Decl(String kind, String name, boolean exposed, int start, int end,
            List<String> structure) { }

    static M3Atom method(Source source, Owner owner, MethodTree method) {
        M3AtomFacts facts = M3AtomFacts.of(method.getBody());
        String output = method.getReturnType() == null ? "<init>" : source.clean(method.getReturnType());
        Stream<String> length = longBody(source, method.getBody()) ? Stream.of(M3Atom.METHOD_15)
                : Stream.empty();
        Typing typing = new Typing(inputs(source, method.getParameters()), output, facts.effects(),
                failure(source, method.getThrows(), facts), limits(length, facts));
        return atom(source, owner, decl(source, "METHOD", method.getName().toString(),
                exposed(owner, method.getModifiers()), method), typing, methodContract(source, method, output));
    }

    private static String methodContract(Source source, MethodTree method, String output) {
        return "METHOD:" + modifiers(source, method.getModifiers()) + ":typeParams="
                + list(source, method.getTypeParameters()) + ":receiver="
                + source.clean(method.getReceiverParameter()) + ':' + method.getName() + '('
                + list(source, method.getParameters().stream().map(VariableTree::getType).toList())
                + "):" + output + ":throws=" + list(source, method.getThrows()) + ":default="
                + source.clean(method.getDefaultValue());
    }

    static M3Atom field(Source source, Owner owner, VariableTree field) {
        M3AtomFacts facts = M3AtomFacts.of(field.getInitializer());
        boolean exposed = exposed(owner, field.getModifiers()) || enumConstant(owner, field);
        String contract = "FIELD:" + modifiers(source, field.getModifiers()) + ':'
                + field.getName() + ':' + source.clean(field.getType())
                + constantInitializer(source, field, exposed);
        Typing typing = new Typing(M3Atom.NONE, M3Atom.clean(source.clean(field.getType())),
                facts.effects(), failure(source, List.of(), facts), limits(Stream.empty(), facts));
        return atom(source, owner, decl(source, "FIELD", field.getName().toString(), exposed, field),
                typing, contract);
    }

    /** Header atom of a named type: modifiers through the opening brace. */
    static M3Atom type(Source source, Owner parent, Owner self, ClassTree type) {
        List<Tree> header = header(type);
        int start = (int) source.start(type);
        long last = header.stream().filter(source::spans).mapToLong(source::end).max().orElse(start);
        int brace = source.text().indexOf('{', (int) last);
        int end = brace < 0 ? (int) source.end(type) : brace + 1;
        String owner = parent == null ? packageOwner(source) : parent.qualified();
        Decl decl = new Decl("TYPE", type.getSimpleName().toString(), self.reachable(), start, end,
                header.stream().flatMap(tree -> structure(tree).stream()).toList());
        Typing typing = new Typing(components(source, type), self.qualified(), "PURE", M3Atom.NONE,
                M3Atom.NONE);
        return atom(source, new Owner(owner, true, false, Tree.Kind.CLASS), decl, typing,
                typeContract(source, self, type));
    }

    /** Merges a comma-declared field group that shares one start position. */
    static M3Atom mergeFields(Source source, M3Atom first, M3Atom next) {
        int end = Math.max(first.endChar(), next.endChar());
        String text = source.text().substring(first.startChar(), end);
        return new M3Atom(first.path(), first.owner(), first.kind(), first.name() + ',' + next.name(),
                first.exposed() || next.exposed(), first.startLine(), source.line(end - 1L),
                first.startChar(), end, first.inputs(), first.output(),
                union(first.effects(), next.effects(), "PURE"), union(first.failure(), next.failure(),
                M3Atom.NONE), union(first.limits(), next.limits(), M3Atom.NONE), anchors(text),
                M3MopReceipt.sha256(text), M3MopReceipt.canonical(SHAPE_DOMAIN, first.shapeKey(),
                next.shapeKey()), first.contract() + ';' + next.contract());
    }

    private static M3Atom atom(Source source, Owner owner, Decl decl, Typing typing, String contract) {
        String text = source.text().substring(decl.start(), decl.end());
        return new M3Atom(source.path(), owner.qualified(), decl.kind(), decl.name(), decl.exposed(),
                source.line(decl.start()), source.line(decl.end() - 1L), decl.start(), decl.end(),
                typing.inputs(), typing.output(), typing.effects(), typing.failure(), typing.limits(),
                anchors(text), M3MopReceipt.sha256(text), M3MopReceipt.canonical(SHAPE_DOMAIN,
                Stream.concat(Stream.of(decl.kind()), decl.structure().stream()).toArray(String[]::new)),
                contract);
    }

    private static Decl decl(Source source, String kind, String name, boolean exposed, Tree tree) {
        return new Decl(kind, name, exposed, (int) source.start(tree), (int) source.end(tree),
                structure(tree));
    }

    private static String typeContract(Source source, Owner self, ClassTree type) {
        return String.join("|", self.qualified(), type.getKind().name(),
                modifiers(source, type.getModifiers()), list(source, type.getTypeParameters()),
                source.clean(type.getExtendsClause()), list(source, type.getImplementsClause()),
                list(source, type.getPermitsClause()), "components=" + components(source, type),
                "constants=" + constants(type));
    }

    private static List<Tree> header(ClassTree type) {
        List<Tree> header = new ArrayList<>();
        header.add(type.getModifiers());
        header.addAll(type.getTypeParameters());
        if (type.getExtendsClause() != null) header.add(type.getExtendsClause());
        header.addAll(type.getImplementsClause());
        header.addAll(type.getPermitsClause());
        header.addAll(recordComponents(type));
        return header;
    }

    private static List<VariableTree> recordComponents(ClassTree type) {
        if (type.getKind() != Tree.Kind.RECORD) return List.of();
        return type.getMembers().stream().filter(VariableTree.class::isInstance)
                .map(VariableTree.class::cast)
                .filter(field -> !field.getModifiers().getFlags().contains(Modifier.STATIC)).toList();
    }

    private static String components(Source source, ClassTree type) {
        return joined(inputs(source, recordComponents(type)));
    }

    private static String constants(ClassTree type) {
        return type.getMembers().stream().filter(VariableTree.class::isInstance)
                .map(VariableTree.class::cast)
                .filter(field -> field.getInitializer() instanceof NewClassTree created
                        && created.getIdentifier().toString().equals(type.getSimpleName().toString()))
                .map(field -> field.getName().toString()).collect(Collectors.joining(","));
    }

    private static boolean enumConstant(Owner owner, VariableTree field) {
        return owner.reachable() && owner.kind() == Tree.Kind.ENUM
                && field.getInitializer() instanceof NewClassTree created
                && owner.qualified().replaceAll(".*[.$]", "")
                        .equals(created.getIdentifier().toString());
    }

    private static String constantInitializer(Source source, VariableTree field, boolean exposed) {
        Set<Modifier> flags = field.getModifiers().getFlags();
        boolean constant = exposed && flags.contains(Modifier.STATIC) && flags.contains(Modifier.FINAL)
                && field.getInitializer() != null
                && CONSTANT_TYPES.contains(source.clean(field.getType()));
        return constant ? "=" + source.clean(field.getInitializer()) : "";
    }

    private static boolean exposed(Owner owner, ModifiersTree modifiers) {
        Set<Modifier> flags = modifiers.getFlags();
        if (!owner.reachable() || flags.contains(Modifier.PRIVATE)) return false;
        return flags.contains(Modifier.PUBLIC) || flags.contains(Modifier.PROTECTED)
                || owner.implicitPublic();
    }

    private static String modifiers(Source source, ModifiersTree modifiers) {
        String flags = modifiers.getFlags().stream().map(Enum::name).sorted()
                .collect(Collectors.joining(","));
        return flags + ":annotations=" + modifiers.getAnnotations().stream().map(source::clean)
                .sorted().collect(Collectors.joining(","));
    }

    private static String inputs(Source source, List<? extends VariableTree> parameters) {
        return joined(parameters.stream()
                .map(parameter -> source.clean(parameter.getType()) + " " + parameter.getName())
                .collect(Collectors.joining(";")));
    }

    private static String failure(Source source, List<? extends Tree> thrown, M3AtomFacts facts) {
        Stream<String> declared = thrown.stream().map(source::clean);
        Stream<String> statement = facts.throwsStatement() ? Stream.of("THROW") : Stream.empty();
        return joined(Stream.concat(declared, statement).collect(Collectors.toCollection(TreeSet::new))
                .stream().collect(Collectors.joining("|")));
    }

    private static String limits(Stream<String> length, M3AtomFacts facts) {
        Stream<String> loop = facts.loop() ? Stream.of(M3Atom.LOOP) : Stream.empty();
        Stream<String> hoist = facts.repeatedLiteral() ? Stream.of(M3Atom.CONST_HOIST) : Stream.empty();
        return joined(Stream.of(length, loop, hoist).flatMap(value -> value)
                .collect(Collectors.joining("|")));
    }

    private static boolean longBody(Source source, BlockTree body) {
        if (body == null || !source.spans(body)) return false;
        return source.line(source.end(body) - 1L) - source.line(source.start(body)) + 1 > 15;
    }

    private static String list(Source source, List<? extends Tree> trees) {
        return trees.stream().map(source::clean).collect(Collectors.joining(","));
    }

    private static String union(String left, String right, String empty) {
        return joined(Stream.of(left, right).flatMap(value -> Stream.of(value.split("\\|")))
                .filter(value -> !value.equals(empty)).collect(Collectors.toCollection(TreeSet::new))
                .stream().collect(Collectors.joining("|")), empty);
    }

    private static String joined(String value) {
        return joined(value, M3Atom.NONE);
    }

    private static String joined(String value, String empty) {
        return value.isEmpty() ? empty : value;
    }

    private static String packageOwner(Source source) {
        return source.packageName().isEmpty() ? "<default>" : source.packageName();
    }

    private static int anchors(String text) {
        return (text.length() - text.replace(M3Atom.ANCHOR, "").length()) / M3Atom.ANCHOR.length();
    }

    /** Pre-order javac tree kinds: names and literal values are not part of the shape. */
    private static List<String> structure(Tree tree) {
        List<String> kinds = new ArrayList<>();
        new TreeScanner<Void, List<String>>() {
            @Override
            public Void scan(Tree node, List<String> sink) {
                if (node != null) sink.add(node.getKind().name());
                return node == null ? null : super.scan(node, sink);
            }
        }.scan(Objects.requireNonNull(tree, "tree"), kinds);
        return kinds;
    }
}
