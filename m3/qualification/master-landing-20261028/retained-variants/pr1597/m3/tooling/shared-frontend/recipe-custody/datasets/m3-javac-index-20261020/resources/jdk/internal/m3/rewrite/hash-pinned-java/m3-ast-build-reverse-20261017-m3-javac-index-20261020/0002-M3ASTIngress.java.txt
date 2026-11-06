// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.Modifier;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Bounded projections over *existing* Eclipse/JDT, Maven/XML and Jackson AST
 * owners, plus syntax-only lexical indexing of Gradle DSL source.
 *
 * <p>No parser is replaced: JDT's original DOM node handles, the XML DOM, and
 * Jackson's original M3AST remain authoritative. Reflection is restricted to
 * original JDT and JSON public contracts so this optional parser JMOD has no
 * forced Eclipse/Jackson dependency. Jackson/JDT integration must be tested
 * with the real jars before product promotion. Gradle's lexical scan is not
 * Groovy/Kotlin semantic parsing or the resolved Gradle project model.
 */
public final class M3ASTIngress {
    /** Explicit, independent limits on input and detached indexed occurrences. */
    public record Limits(int maxSourceUnits, int maxNodes, int maxDepth) {
        public Limits {
            if (maxSourceUnits < 1 || maxNodes < 1 || maxNodes > 20_000_000
                    || maxDepth < 1 || maxDepth > 100_000) {
                throw new IllegalArgumentException("invalid AST ingress limits");
            }
        }
        public static Limits defaults() { return new Limits(8_388_608, 1_000_000, 512); }
    }

    private M3ASTIngress() { }


    /**
     * Index the caller's original javac CompilationUnitTree in a single bounded
     * preorder pass. This NEVER constructs a JavacTask or reparses source.
     * The caller must supply an M3File snapshot from the same parse generation.
     * The URI is checked here; use the parsedSource overload to check spelling.
     *
     * <p>Original javac trees/types/positions are not retained in the resulting
     * primitive index. Source and semantic-environment seals remain independent:
     * this syntax index does not authorize reuse of stale attributed symbols.
     */
    public static M3ASTIndex javac(M3File source, CompilationUnitTree root,
            Trees trees, Limits limits) {
        check(source, limits);
        Objects.requireNonNull(root, "original javac compilation unit");
        Objects.requireNonNull(trees, "original javac Trees");
        if (root.getSourceFile() == null
                || !source.uri().equals(root.getSourceFile().toUri())) {
            throw new IllegalArgumentException("javac source URI does not match M3File owner");
        }
        M3ASTIndex.Builder output = M3ASTIndex.builder(
                source, M3ASTIndex.Domain.JAVAC, limits.maxNodes());
        SourcePositions positions = trees.getSourcePositions();
        int capacity = Math.min(32, limits.maxNodes());
        Tree[] pending = new Tree[capacity];
        int[] parents = new int[capacity];
        int[] depth = new int[capacity];
        pending[0] = root; parents[0] = -1; depth[0] = 1;
        int size = 1;
        int visited = 0;
        ArrayList<Tree> children = new ArrayList<>();
        JavacDirectChildren collector = new JavacDirectChildren();
        while (size != 0) {
            Tree node = pending[--size];
            int parent = parents[size], level = depth[size];
            pending[size] = null;
            if (level > limits.maxDepth()) {
                throw new IllegalArgumentException("javac nesting exceeds AST budget");
            }
            if (visited++ == limits.maxNodes()) {
                throw new IllegalArgumentException("javac AST node budget exceeded");
            }
            long startPosition = positions.getStartPosition(root, node);
            long endPosition = positions.getEndPosition(root, node);
            int start = -1, end = -1;
            int flags = M3ASTIndex.SYNTAX_ONLY;
            if (parent == -1) {
                // The root source envelope is exact even when javac does not
                // provide a JCCompilationUnit endPosition.
                start = 0; end = source.length();
                flags |= M3ASTIndex.EXACT_RANGE;
            } else if (startPosition >= 0 && endPosition >= startPosition) {
                if (endPosition > source.length()) {
                    throw new IllegalArgumentException("javac source span outside snapshot");
                }
                start = (int) startPosition;
                end = (int) endPosition;
                flags |= M3ASTIndex.EXACT_RANGE;
            }
            ModifiersTree modifiers = null;
            String name = null;
            String literal = null;
            if (node instanceof ClassTree declaration) {
                modifiers = declaration.getModifiers();
                name = declaration.getSimpleName().toString();
                if (node.getKind() == Tree.Kind.INTERFACE
                        || node.getKind() == Tree.Kind.ANNOTATION_TYPE) {
                    flags |= M3ASTIndex.INTERFACE;
                }
            } else if (node instanceof MethodTree declaration) {
                modifiers = declaration.getModifiers();
                name = declaration.getName().toString();
            } else if (node instanceof VariableTree declaration) {
                modifiers = declaration.getModifiers();
                name = declaration.getName().toString();
            } else if (node instanceof IdentifierTree identifier) {
                name = identifier.getName().toString();
            } else if (node instanceof MemberSelectTree select) {
                name = select.getIdentifier().toString();
            } else if (node instanceof TypeParameterTree parameter) {
                name = parameter.getName().toString();
            } else if (node instanceof LiteralTree value) {
                Object originalValue = value.getValue();
                literal = originalValue == null ? "null" : originalValue.toString();
            }
            if (modifiers != null) {
                if (modifiers.getFlags().contains(Modifier.STATIC)) flags |= M3ASTIndex.STATIC;
                if (modifiers.getFlags().contains(Modifier.FINAL)) flags |= M3ASTIndex.FINAL;
            }
            int row = output.add(parent, node.getKind().name(), start, end, flags);
            if (name != null || literal != null) output.annotate(row, name, literal);

            children.clear();
            collector.remaining = limits.maxNodes() - visited - size;
            node.accept(collector, children);
            for (int i = children.size() - 1; i >= 0; i--) {
                if (size == pending.length) {
                    int expanded = Math.min(limits.maxNodes(),
                            pending.length + (pending.length >>> 1) + 1);
                    if (expanded <= size) throw new IllegalArgumentException("javac pending node budget");
                    pending = Arrays.copyOf(pending, expanded);
                    parents = Arrays.copyOf(parents, expanded);
                    depth = Arrays.copyOf(depth, expanded);
                }
                pending[size] = children.get(i);
                parents[size] = row;
                depth[size++] = level + 1;
            }
        }
        return output.seal();
    }

    /**
     * Verify the exact original parsed characters without reading the filesystem.
     * Callers with M3AST.Unit use its sealed source text from the parse callback.
     */
    public static M3ASTIndex javac(M3File source, CompilationUnitTree root,
            Trees trees, CharSequence parsedSource, Limits limits) {
        Objects.requireNonNull(parsedSource, "original parsed source");
        if (source.length() != parsedSource.length()) {
            throw new IllegalArgumentException("javac source generation length changed");
        }
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) != parsedSource.charAt(i)) {
                throw new IllegalArgumentException("javac source generation content changed");
            }
        }
        return javac(source, root, trees, limits);
    }

    /** Reuse one scanner whose scan() collects only direct children. */
    private static final class JavacDirectChildren extends TreeScanner<Void, ArrayList<Tree>> {
        int remaining;

        @Override public Void scan(Tree node, ArrayList<Tree> children) {
            if (node != null) {
                if (children.size() >= remaining) {
                    throw new IllegalArgumentException("javac AST pending budget exceeded");
                }
                children.add(node);
            }
            return null;
        }
    }

    /** Secure, complete POM XML parsing; exact element byte offsets are unavailable. */
    public static M3ASTIndex mavenPom(M3File source, Limits limits) throws IOException {
        check(source, limits);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setValidating(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(
                    new InputSource(new StringReader(source.text())));
            if (document.getDocumentElement() == null) {
                throw new IOException("XML input has no Maven POM project root");
            }
            String rootName = document.getDocumentElement().getLocalName();
            if (rootName == null) rootName = document.getDocumentElement().getNodeName();
            if (!"project".equals(rootName)) {
                throw new IOException("XML input is not a Maven POM project root");
            }
            String namespace = document.getDocumentElement().getNamespaceURI();
            if (namespace != null && !namespace.isEmpty()
                    && !"http://maven.apache.org/POM/4.0.0".equals(namespace)) {
                throw new IOException("foreign XML namespace is not a Maven POM");
            }
            M3ASTIndex.Builder output = M3ASTIndex.builder(
                    source, M3ASTIndex.Domain.MAVEN_POM, limits.maxNodes());
            Node[] stack = new Node[32];
            int[] parent = new int[32], depth = new int[32];
            int size = 1;
            stack[0] = document; parent[0] = -1; depth[0] = 1;
            ArrayList<Node> children = new ArrayList<>();
            while (size > 0) {
                Node node = stack[--size];
                int owner = parent[size], level = depth[size];
                stack[size] = null;
                if (level > limits.maxDepth()) throw new IllegalArgumentException("POM depth budget exceeded");
                boolean root = owner == -1;
                String kind = switch (node.getNodeType()) {
                    case Node.DOCUMENT_NODE -> "XML_DOCUMENT";
                    case Node.ELEMENT_NODE -> "XML_ELEMENT:" + node.getNodeName();
                    case Node.ATTRIBUTE_NODE -> "XML_ATTRIBUTE:" + node.getNodeName();
                    case Node.TEXT_NODE -> "XML_TEXT";
                    case Node.CDATA_SECTION_NODE -> "XML_CDATA";
                    case Node.COMMENT_NODE -> "XML_COMMENT";
                    case Node.PROCESSING_INSTRUCTION_NODE -> "XML_PI";
                    default -> "XML_NODE_" + node.getNodeType();
                };
                int flags = M3ASTIndex.SYNTAX_ONLY;
                int position = output.add(owner, kind, root ? 0 : -1,
                        root ? source.length() : -1, flags | (root ? M3ASTIndex.EXACT_RANGE : 0));
                children.clear();
                NamedNodeMap attrs = node.getAttributes();
                if (attrs != null) {
                    for (int index = 0; index < attrs.getLength(); index++) children.add(attrs.item(index));
                }
                NodeList list = node.getChildNodes();
                for (int index = 0; index < list.getLength(); index++) children.add(list.item(index));
                for (int index = children.size() - 1; index >= 0; index--) {
                    if (size == stack.length) {
                        int newSize = Math.min(limits.maxNodes(), stack.length + (stack.length >>> 1) + 1);
                        if (newSize <= stack.length) throw new IllegalArgumentException("POM occurrence budget");
                        stack = Arrays.copyOf(stack, newSize);
                        parent = Arrays.copyOf(parent, newSize);
                        depth = Arrays.copyOf(depth, newSize);
                    }
                    stack[size] = children.get(index);
                    parent[size] = position;
                    depth[size++] = level + 1;
                }
            }
            return output.seal();
        } catch (ParserConfigurationException | SAXException problem) {
            throw new IOException("secure Maven POM XML syntax refused", problem);
        }
    }

    /**
     * Snapshot an actual org.eclipse.jdt.core.dom.ASTNode without retaining it.
     * It must be obtained from the original Eclipse ASTParser/working copy;
     * ASTParser settings, recovery, bindings and edits remain Eclipse's authority.
     */
    public static M3ASTIndex eclipseJdt(M3File source, Object root, Limits limits) {
        check(source, limits);
        Objects.requireNonNull(root, "original Eclipse AST root");
        try {
            ClassLoader loader = root.getClass().getClassLoader();
            Class<?> nodeType = Class.forName("org.eclipse.jdt.core.dom.ASTNode", false, loader);
            Class<?> propertyType = Class.forName(
                    "org.eclipse.jdt.core.dom.StructuralPropertyDescriptor", false, loader);
            if (!nodeType.isInstance(root)) throw new IllegalArgumentException("not a JDT DOM ASTNode");
            Method position = nodeType.getMethod("getStartPosition");
            Method length = nodeType.getMethod("getLength");
            Method properties = nodeType.getMethod("structuralPropertiesForType");
            Method property = nodeType.getMethod("getStructuralProperty", propertyType);
            Method getFlags = nodeType.getMethod("getFlags");
            int recoveredBit = nodeType.getField("RECOVERED").getInt(null);
            int malformedBit = nodeType.getField("MALFORMED").getInt(null);
            M3ASTIndex.Builder output = M3ASTIndex.builder(
                    source, M3ASTIndex.Domain.ECLIPSE_JDT, limits.maxNodes());
            Object[] stack = new Object[32];
            int[] parent = new int[32], depth = new int[32];
            stack[0] = root; parent[0] = -1; depth[0] = 1;
            int size = 1;
            ArrayList<Object> children = new ArrayList<>();
            while (size > 0) {
                Object node = stack[--size];
                int owner = parent[size], level = depth[size];
                stack[size] = null;
                if (level > limits.maxDepth()) throw new IllegalArgumentException("JDT depth budget");
                int start = (Integer) position.invoke(node);
                int units = (Integer) length.invoke(node);
                int end = start < 0 ? -1 : Math.addExact(start, units);
                int jdtFlags = (Integer) getFlags.invoke(node);
                int flags = M3ASTIndex.SYNTAX_ONLY;
                if (start >= 0) flags |= M3ASTIndex.EXACT_RANGE;
                if ((jdtFlags & (recoveredBit | malformedBit)) != 0) flags |= M3ASTIndex.RECOVERED;
                String kind = "JDT_" + node.getClass().getSimpleName();
                int row = output.add(owner, kind, start, end, flags);
                children.clear();
                List<?> descriptors = (List<?>) properties.invoke(node);
                for (Object descriptor : descriptors) {
                    Object child = property.invoke(node, descriptor);
                    if (nodeType.isInstance(child)) children.add(child);
                    else if (child instanceof List<?> members) {
                        for (Object member : members) {
                            if (nodeType.isInstance(member)) children.add(member);
                        }
                    }
                }
                for (int i = children.size() - 1; i >= 0; i--) {
                    if (size == stack.length) {
                        int newSize = Math.min(limits.maxNodes(), stack.length + (stack.length >>> 1) + 1);
                        if (newSize <= stack.length) throw new IllegalArgumentException("JDT occurrence budget");
                        stack = Arrays.copyOf(stack, newSize);
                        parent = Arrays.copyOf(parent, newSize);
                        depth = Arrays.copyOf(depth, newSize);
                    }
                    stack[size] = children.get(i);
                    parent[size] = row;
                    depth[size++] = level + 1;
                }
            }
            return output.seal();
        } catch (ReflectiveOperationException error) {
            throw new IllegalArgumentException("Eclipse JDT AST owner is missing/incompatible", error);
        }
    }

    /**
     * Project an existing jdk.m3.jackson.M3AST's public occurrence API. Its
     * current public wrappers are transient: this is NOT the 100MB zero-wrapper
     * direct-pool fast path. Its token names and values remain with the original
     * Jackson AST and are not copied into this structural index.
     */
    public static M3ASTIndex jacksonJson(M3File source, Object ast, Limits limits) {
        check(source, limits);
        Objects.requireNonNull(ast, "original Jackson M3AST");
        if (!"jdk.m3.jackson.M3AST".equals(ast.getClass().getName())) {
            throw new IllegalArgumentException("expected the original jdk.m3.jackson.M3AST owner");
        }
        try {
            Method root = ast.getClass().getMethod("root");
            Object rootAtom = root.invoke(ast);
            Class<?> atomType = rootAtom.getClass();
            Method kind = atomType.getMethod("kind");
            Method count = atomType.getMethod("childCount");
            Method child = atomType.getMethod("child", int.class);
            M3ASTIndex.Builder output = M3ASTIndex.builder(
                    source, M3ASTIndex.Domain.JACKSON_JSON, limits.maxNodes());
            Object[] stack = new Object[32];
            int[] parent = new int[32], depth = new int[32];
            stack[0] = rootAtom; parent[0] = -1; depth[0] = 1;
            int size = 1;
            while (size > 0) {
                Object node = stack[--size];
                int owner = parent[size], level = depth[size];
                stack[size] = null;
                if (level > limits.maxDepth()) throw new IllegalArgumentException("JSON AST depth budget");
                int row = output.add(owner, "JSON_" + kind.invoke(node), -1, -1,
                        M3ASTIndex.SYNTAX_ONLY | M3ASTIndex.SOURCE_UNVERIFIED);
                int n = (Integer) count.invoke(node);
                for (int i = n - 1; i >= 0; i--) {
                    if (size == stack.length) {
                        int expanded = Math.min(limits.maxNodes(), stack.length + (stack.length >>> 1) + 1);
                        if (expanded <= stack.length) throw new IllegalArgumentException("JSON occurrences budget");
                        stack = Arrays.copyOf(stack, expanded);
                        parent = Arrays.copyOf(parent, expanded);
                        depth = Arrays.copyOf(depth, expanded);
                    }
                    stack[size] = child.invoke(node, i);
                    parent[size] = row;
                    depth[size++] = level + 1;
                }
            }
            return output.seal();
        } catch (ReflectiveOperationException error) {
            throw new IllegalArgumentException("Jackson AST public occurrence contract unavailable", error);
        }
    }

    /**
     * Lossless lexical envelope for Gradle Groovy/Kotlin scripts. Does not parse
     * plugin DSL semantics, slashy/GString interpolation, dependency resolution,
     * Gradle provider graphs or Kotlin compiler bindings.
     */
    public static M3ASTIndex gradleScript(M3File source, boolean kotlin, Limits limits) {
        check(source, limits);
        M3ASTIndex.Builder output = M3ASTIndex.builder(source,
                kotlin ? M3ASTIndex.Domain.GRADLE_KOTLIN : M3ASTIndex.Domain.GRADLE_GROOVY,
                limits.maxNodes());
        int root = output.add(-1, "GRADLE_SOURCE", 0, source.length(),
                M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY);
        int[] stack = new int[16], start = new int[16];
        char[] closer = new char[16];
        int depth = 1, offset = 0;
        stack[0] = root; closer[0] = '\0'; start[0] = 0;
        String text = source.text();
        while (offset < text.length()) {
            char c = text.charAt(offset);
            if (Character.isWhitespace(c)) { offset++; continue; }
            int begin = offset;
            int flags = M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY;
            if (c == '/' && offset + 1 < text.length() && text.charAt(offset + 1) == '/') {
                offset += 2;
                while (offset < text.length() && text.charAt(offset) != '\n'
                        && text.charAt(offset) != '\r') offset++;
                output.add(stack[depth - 1], "LINE_COMMENT", begin, offset, flags);
                continue;
            }
            if (c == '/' && offset + 1 < text.length() && text.charAt(offset + 1) == '*') {
                offset += 2;
                int nested = 1;
                while (offset < text.length() && nested > 0) {
                    if (offset + 1 < text.length() && text.charAt(offset) == '/'
                            && text.charAt(offset + 1) == '*' && kotlin) {
                        offset += 2; nested++;
                    } else if (offset + 1 < text.length() && text.charAt(offset) == '*'
                            && text.charAt(offset + 1) == '/') {
                        offset += 2; nested--;
                    } else offset++;
                }
                if (nested != 0) throw new IllegalArgumentException("unclosed Gradle block comment");
                output.add(stack[depth - 1], "BLOCK_COMMENT", begin, offset, flags);
                continue;
            }
            if (c == '"' || c == '\'') {
                boolean triple = offset + 2 < text.length() && text.charAt(offset + 1) == c
                        && text.charAt(offset + 2) == c;
                offset += triple ? 3 : 1;
                boolean closed = false;
                while (offset < text.length()) {
                    if (!triple && text.charAt(offset) == '\\') {
                        if (offset + 1 >= text.length()) break;
                        offset += 2; continue;
                    }
                    if (text.charAt(offset) == c && (!triple
                            || (offset + 2 < text.length()
                                && text.charAt(offset + 1) == c && text.charAt(offset + 2) == c))) {
                        offset += triple ? 3 : 1;
                        closed = true; break;
                    }
                    offset++;
                }
                if (!closed) throw new IllegalArgumentException("unterminated Gradle literal");
                output.add(stack[depth - 1], "STRING", begin, offset, flags);
                continue;
            }
            char close = switch (c) { case '{' -> '}'; case '(' -> ')'; case '[' -> ']'; default -> '\0'; };
            if (close != '\0') {
                if (depth == limits.maxDepth()) throw new IllegalArgumentException("Gradle nesting budget");
                int node = output.add(stack[depth - 1], switch (c) {
                    case '{' -> "BRACE_BLOCK"; case '(' -> "PAREN_GROUP"; default -> "BRACKET_GROUP";
                }, -1, -1, M3ASTIndex.SYNTAX_ONLY);
                if (depth == stack.length) {
                    stack = Arrays.copyOf(stack, stack.length * 2);
                    start = Arrays.copyOf(start, start.length * 2);
                    closer = Arrays.copyOf(closer, closer.length * 2);
                }
                stack[depth] = node; start[depth] = begin; closer[depth] = close;
                depth++; offset++; continue;
            }
            if (c == '}' || c == ')' || c == ']') {
                if (depth == 1 || c != closer[depth - 1]) {
                    throw new IllegalArgumentException("mismatched Gradle delimiter at UTF-16 " + offset);
                }
                depth--;
                output.completeRange(stack[depth], start[depth], offset + 1, true);
                offset++; continue;
            }
            if (Character.isJavaIdentifierStart(c)) {
                offset++;
                while (offset < text.length() && Character.isJavaIdentifierPart(text.charAt(offset))) offset++;
                output.add(stack[depth - 1], "IDENTIFIER", begin, offset, flags);
                continue;
            }
            if (Character.isDigit(c)) {
                offset++;
                while (offset < text.length()
                        && (Character.isLetterOrDigit(text.charAt(offset))
                            || text.charAt(offset) == '.' || text.charAt(offset) == '_')) offset++;
                output.add(stack[depth - 1], "NUMBER_TOKEN", begin, offset, flags);
                continue;
            }
            offset++;
            output.add(stack[depth - 1], "PUNCTUATION", begin, offset, flags);
        }
        if (depth != 1) throw new IllegalArgumentException("unclosed Gradle delimiter");
        return output.seal();
    }

    private static void check(M3File source, Limits limits) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(limits, "limits");
        if (source.length() > limits.maxSourceUnits()) {
            throw new IllegalArgumentException("AST source exceeds UTF-16 input limit");
        }
    }
}
