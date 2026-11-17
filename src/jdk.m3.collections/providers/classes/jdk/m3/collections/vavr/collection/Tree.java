/* ____  ______________  ________________________  __________
 * \   \/   /      \   \/   /   __/   /      \   \/   /      \
 *  \______/___/\___\______/___/_____/___/\___\______/___/\___\
 *
 * Copyright 2014-2026 Vavr, https://vavr.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.m3.collections.vavr.collection;

import jdk.m3.collections.vavr.PartialFunction;
import jdk.m3.collections.vavr.Tuple;
import jdk.m3.collections.vavr.Tuple2;
import jdk.m3.collections.vavr.Tuple3;
import jdk.m3.collections.vavr.collection.List.Nil;
import jdk.m3.collections.vavr.control.Option;
import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;

import static jdk.m3.collections.vavr.collection.Tree.*;
import static jdk.m3.collections.vavr.collection.Tree.Order.PRE_ORDER;

/**
 * A general Tree interface.
 *
 * @param <T> component type of this Tree
 * @author Daniel Dietrich, Grzegorz Piwowarek
 */
public interface Tree<T extends Object> extends Traversable<T>, Serializable {

    @Serial
    long serialVersionUID = 1L;

    /**
     * Returns a {@link java.util.stream.Collector} which may be used in conjunction with
     * {@link java.util.stream.Stream#collect(java.util.stream.Collector)} to obtain a {@link Tree}.
     *
     * @param <T> Component type of the Tree.
     * @return A jdk.m3.collections.vavr.collection.Tree Collector.
     */
    static <T extends Object> Collector<T, ArrayList<T>, Tree<T>> collector() {
        final Supplier<ArrayList<T>> supplier = ArrayList::new;
        final BiConsumer<ArrayList<T>, T> accumulator = ArrayList::add;
        final BinaryOperator<ArrayList<T>> combiner = (left, right) -> {
            left.addAll(right);
            return left;
        };
        final Function<ArrayList<T>, Tree<T>> finisher = Tree::ofAll;
        return Collector.of(supplier, accumulator, combiner, finisher);
    }

    /**
     * Returns the singleton empty tree.
     *
     * @param <T> Type of tree values.
     * @return The empty tree.
     */
    static <T extends Object> Empty<T> empty() {
        return Empty.instance();
    }

    /**
     * Narrows a widened {@code Tree<? extends T>} to {@code Tree<T>}
     * by performing a type-safe cast. This is eligible because immutable/read-only
     * collections are covariant.
     *
     * @param tree An {@code Tree}.
     * @param <T>  Component type of the {@code Tree}.
     * @return the given {@code tree} instance as narrowed type {@code Tree<T>}.
     */
    @SuppressWarnings("unchecked")
    static <T extends Object> Tree<T> narrow(Tree<? extends T> tree) {
        return (Tree<T>) tree;
    }

    /**
     * Returns a new Node containing the given value and having no children.
     *
     * @param value A value
     * @param <T>   Value type
     * @return A new Node instance.
     */
    static <T extends Object> Node<T> of(T value) {
        return new Node<>(value, jdk.m3.collections.vavr.collection.List.empty());
    }

    /**
     * Returns a new Node containing the given value and having the given children.
     *
     * @param value    A value
     * @param children The child nodes, possibly empty
     * @param <T>      Value type
     * @return A new Node instance.
     */
    @SuppressWarnings("varargs")
    @SafeVarargs
    static <T extends Object> Node<T> of(T value, Node<T> ... children) {
        Objects.requireNonNull(children, "children is null");
        return new Node<>(value, jdk.m3.collections.vavr.collection.List.of(children));
    }

    /**
     * Returns a new Node containing the given value and having the given children.
     *
     * @param value    A value
     * @param children The child nodes, possibly empty
     * @param <T>      Value type
     * @return A new Node instance.
     */
    static <T extends Object> Node<T> of(T value, Iterable<Node<T>> children) {
        Objects.requireNonNull(children, "children is null");
        return new Node<>(value, jdk.m3.collections.vavr.collection.List.ofAll(children));
    }

    /**
     * Creates a Tree of the given elements.
     *
     * @param <T>    Component type of the List.
     * @param values Zero or more values.
     * @return A Tree containing the given values.
     * @throws NullPointerException if {@code values} is null
     */
    @SuppressWarnings("varargs")
    @SafeVarargs
    static <T extends Object> Tree<T> of(T ... values) {
        Objects.requireNonNull(values, "values is null");
        final jdk.m3.collections.vavr.collection.List<T> list = jdk.m3.collections.vavr.collection.List.of(values);
        return list.isEmpty() ? Empty.instance() : new Node<>(list.head(), list.tail().map(Tree::of));
    }

    /**
     * Creates a Tree of the given elements.
     * <p>
     * If the given iterable is already a Tree, it is returned unmodified; otherwise a new Tree is built from its elements.
     *
     * @param <T>      Component type of the Tree.
     * @param iterable An Iterable of elements.
     * @return A Tree containing the given elements in the same order, or {@code iterable} itself if it is already a Tree.
     * @throws NullPointerException if {@code iterable} is null
     */
    @SuppressWarnings("unchecked")
    static <T extends Object> Tree<T> ofAll(Iterable<? extends T> iterable) {
        Objects.requireNonNull(iterable, "iterable is null");
        if (iterable instanceof Tree) {
            return (Tree<T>) iterable;
        } else {
            final jdk.m3.collections.vavr.collection.List<T> list = jdk.m3.collections.vavr.collection.List.ofAll(iterable);
            return list.isEmpty() ? Empty.instance() : new Node<>(list.head(), list.tail().map(Tree::of));
        }
    }

    /**
     * Creates a Tree that contains the elements of the given {@link java.util.stream.Stream}.
     *
     * @param javaStream A {@link java.util.stream.Stream}
     * @param <T>        Component type of the Stream.
     * @return A Tree containing the given elements in the same order.
     */
    static <T extends Object> Tree<T> ofAll(java.util.stream.Stream<? extends T> javaStream) {
        Objects.requireNonNull(javaStream, "javaStream is null");
        return ofAll(jdk.m3.collections.vavr.collection.Iterator.ofAll(javaStream.iterator()));
    }

    /**
     * Returns a Tree containing {@code n} values of a given Function {@code f}
     * over a range of integer values from 0 to {@code n - 1}.
     *
     * @param <T> Component type of the Tree
     * @param n   The number of elements in the Tree
     * @param f   The Function computing element values
     * @return A Tree consisting of elements {@code f(0),f(1), ..., f(n - 1)}
     * @throws NullPointerException if {@code f} is null
     */
    static <T extends Object> Tree<T> tabulate(int n, Function<? super Integer, ? extends T> f) {
        Objects.requireNonNull(f, "f is null");
        return jdk.m3.collections.vavr.collection.Collections.tabulate(n, f, empty(), Tree::of);
    }

    /**
     * Returns a Tree containing {@code n} values supplied by a given Supplier {@code s}.
     *
     * @param <T> Component type of the Tree
     * @param n   The number of elements in the Tree
     * @param s   The Supplier computing element values
     * @return A Tree of size {@code n}, where each element contains the result supplied by {@code s}.
     * @throws NullPointerException if {@code s} is null
     */
    static <T extends Object> Tree<T> fill(int n, Supplier<? extends T> s) {
        Objects.requireNonNull(s, "s is null");
        return jdk.m3.collections.vavr.collection.Collections.fill(n, s, empty(), Tree::of);
    }

    /**
     * Returns a Tree containing {@code n} times the given {@code element}
     *
     * @param <T>     Component type of the Tree
     * @param n       The number of elements in the Tree
     * @param element The element
     * @return A Tree of size {@code n}, where each element is the given {@code element}.
     */
    static <T extends Object> Tree<T> fill(int n, T element) {
        return jdk.m3.collections.vavr.collection.Collections.fillObject(n, element, empty(), Tree::of);
    }

    /**
     * Recursively builds a non-empty {@code Tree}, starting with the given {@code seed} value and proceeding in depth-first order.
     * <p>
     * The children of a node are created by
     * <ol>
     * <li>applying the {@code descend} function to the node value</li>
     * <li>calling this method recursively by using each derived child value as new seed (in iteration order).</li>
     * </ol>
     * <p>
     * Example:
     * <pre>{@code
     * // = (1 (2 4 5) 3)
     * Tree.recurse(1, i ->
     *   (i == 1) ? List.of(2, 3) :
     *   (i == 2) ? List.of(4, 5) :
     *   List.empty()
     * ).toLispString();
     * }</pre>
     *
     * @param seed    The start value for the Tree
     * @param descend A function to calculate the child values
     * @param <T>     Value type
     * @return a new, non-empty {@code Tree} instance
     * @throws NullPointerException if {@code descend} is null
     */
    static <T extends Object> Node<T> recurse(T seed, Function<? super T, ? extends Iterable<? extends T>> descend) {
        Objects.requireNonNull(descend, "descend is null");
        return Tree.of(seed, Stream.of(seed).flatMap(descend).map(children -> recurse(children, descend)));
    }

    /**
     * Build a {@code List} with roots of {@code Tree} from flat source.
     * <p>
     * {@code parentMapper} must return {@code null} for root element.
     *
     * <pre>{@code
     *  // = [(1, null, "I"), (2, 1, "II"), (3, 1, "III"), (4, 2, "IV"), (5, 2, "V")]
     *  List<MenuItem> items = ...; // MenuItem(id, parentId, label)
     *
     *  //      I
     *  //     / \
     *  //   II  III
     *  //   /\
     *  //  IV V
     *  List<Tree.Node<MenuItem>> roots = Tree.build(items, MenuItem::getId, MenuItem::getParentId);
     *  Tree<MenuItem> menu = roots.head(); // the single root node, "I"
     * }</pre>
     *
     * @param source       Flat source
     * @param idMapper     A mapper from source item to unique identifier of that item
     * @param parentMapper A mapper from source item to unique identifier of parent item. Need return null for root items
     * @param <T>          Value type
     * @param <ID>         Id type
     * @return a new, maybe empty {@code List} instance with non-empty {@code Tree} instances
     * @throws NullPointerException if {@code source}, {@code idMapper} or {@code parentMapper} is null
     */
    static <T extends Object, ID extends Object> List<Node<T>> build(Iterable<? extends T> source, Function<? super T, ? extends ID> idMapper, Function<? super T, ? extends ID> parentMapper) {
        Objects.requireNonNull(source, "source is null");
        Objects.requireNonNull(idMapper, "idMapper is null");
        Objects.requireNonNull(parentMapper, "parentMapper is null");
        final List<T> list = List.ofAll(source);
        final Map<ID, List<T>> byParent = list.groupBy(parentMapper);
        final Function<? super T, Iterable<? extends T>> descend = idMapper
                .andThen(byParent::get)
                .andThen(o -> o.getOrElse(List::empty));
        final List<T> roots = byParent.get(null).getOrElse(List::empty);
        return roots.map(v -> recurse(v, descend));
    }

    @Override
    default <R extends Object> Tree<R> collect(PartialFunction<? super T, ? extends R> partialFunction) {
        return ofAll(iterator().<R> collect(partialFunction));
    }

    /**
     * Gets the value of this tree.
     *
     * @return The value of this tree.
     * @throws java.lang.UnsupportedOperationException if this tree is empty
     */
    T getValue();

    /**
     * Returns the children of this tree.
     *
     * @return the tree's children
     */
    jdk.m3.collections.vavr.collection.List<Node<T>> getChildren();

    /**
     * Checks if this Tree is a leaf. A tree is a leaf if it is a Node with no children.
     * Because the empty tree is no Node, it is not a leaf by definition.
     *
     * @return true if this tree is a leaf, false otherwise.
     */
    boolean isLeaf();

    /**
     * Checks if this Tree is a branch. A Tree is a branch if it is a Node which has children.
     * Because the empty tree is not a Node, it is not a branch by definition.
     *
     * @return true if this tree is a branch, false otherwise.
     */
    default boolean isBranch() {
        return !(isEmpty() || isLeaf());
    }

    /**
     * A {@code Tree} is computed synchronously.
     *
     * @return false
     */
    @Override
    default boolean isAsync() {
        return false;
    }

    @Override
    default boolean isDistinct() {
        return false;
    }

    /**
     * A {@code Tree} is computed eagerly.
     *
     * @return false
     */
    @Override
    default boolean isLazy() {
        return false;
    }

    @Override
    default boolean isSequential() {
        return true;
    }

    /**
     * Traverses this tree values in a specific {@link Order}.
     *
     * @param order A traversal order
     * @return A new Iterator
     */
    default jdk.m3.collections.vavr.collection.Iterator<T> iterator(Order order) {
        return values(order).iterator();
    }

    /**
     * Creates a <a href="https://www.tutorialspoint.com/lisp/lisp_tree.htm">Lisp-like</a> representation of this {@code Tree}.
     *
     * @return This {@code Tree} as Lisp-string, i.e. represented as list of lists.
     */
    String toLispString();

    /**
     * Transforms this {@code Tree}.
     *
     * @param f   A transformation
     * @param <U> Type of transformation result
     * @return An instance of type {@code U}
     * @throws NullPointerException if {@code f} is null
     */
    default <U extends Object> U transform(Function<? super Tree<T>, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        return f.apply(this);
    }

    /**
     * Traverses this tree in {@link Order#PRE_ORDER}.
     *
     * @return A sequence of nodes.
     */
    default Seq<Node<T>> traverse() {
        return traverse(PRE_ORDER);
    }

    /**
     * Traverses this tree in a specific order.
     *
     * @param order the tree traversal order
     * @return A sequence of nodes.
     * @throws java.lang.NullPointerException if order is null
     */
    default Seq<Node<T>> traverse(Order order) {
        Objects.requireNonNull(order, "order is null");
        if (isEmpty()) {
            return Stream.empty();
        } else {
            final Node<T> node = (Node<T>) this;
            switch (order) {
                case PRE_ORDER:
                    return TreeModule.traversePreOrder(node);
                case IN_ORDER:
                    return TreeModule.traverseInOrder(node);
                case POST_ORDER:
                    return TreeModule.traversePostOrder(node);
                case LEVEL_ORDER:
                    return TreeModule.traverseLevelOrder(node);
                default:
                    throw new IllegalStateException("Unknown order: " + order.name());
            }
        }
    }

    /**
     * Traverses this tree values in {@link Order#PRE_ORDER}.
     * Syntactic sugar for {@code traverse().map(Node::getValue)}.
     *
     * @return A sequence of the tree values.
     */
    default Seq<T> values() {
        return traverse(PRE_ORDER).map(Node::getValue);
    }

    /**
     * Traverses this tree values in a specific order.
     * Syntactic sugar for {@code traverse(order).map(Node::getValue)}.
     *
     * @param order the tree traversal order
     * @return A sequence of the tree values.
     * @throws java.lang.NullPointerException if order is null
     */
    default Seq<T> values(Order order) {
        return traverse(order).map(Node::getValue);
    }

    /**
     * Counts the number of branches of this tree. The empty tree and a leaf have no branches.
     *
     * @return The number of branches of this tree.
     */
    default int branchCount() {
        if (isEmpty() || isLeaf()) {
            return 0;
        } else {
            return getChildren().foldLeft(1, (count, child) -> count + child.branchCount());
        }
    }

    /**
     * Counts the number of leaves of this tree. The empty tree has no leaves.
     *
     * @return The number of leaves of this tree.
     */
    default int leafCount() {
        if (isEmpty()) {
            return 0;
        } else if (isLeaf()) {
            return 1;
        } else {
            return getChildren().foldLeft(0, (count, child) -> count + child.leafCount());
        }
    }

    /**
     * Counts the number of nodes (i.e. branches and leaves) of this tree. The empty tree has no nodes.
     *
     * @return The number of nodes of this tree.
     */
    default int nodeCount() {
        return length();
    }

    // -- Methods inherited from Traversable

    @Override
    default Seq<T> distinct() {
        return values().distinct();
    }

    @Override
    default Seq<T> distinctBy(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        if (isEmpty()) {
            return Stream.empty();
        } else {
            return values().distinctBy(comparator);
        }
    }

    @Override
    default <U extends Object> Seq<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor is null");
        if (isEmpty()) {
            return Stream.empty();
        } else {
            return values().distinctBy(keyExtractor);
        }
    }

    @Override
    default Seq<T> drop(int n) {
        if (n >= length()) {
            return Stream.empty();
        } else {
            return values().drop(n);
        }
    }

    @Override
    default Seq<T> dropRight(int n) {
        if (n >= length()) {
            return Stream.empty();
        } else {
            return values().dropRight(n);
        }
    }

    @Override
    default Seq<T> dropUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return dropWhile(predicate.negate());
    }

    @Override
    default Seq<T> dropWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        if (isEmpty()) {
            return Stream.empty();
        } else {
            return values().dropWhile(predicate);
        }
    }

    @Override
    default Seq<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        if (isEmpty()) {
            return Stream.empty();
        } else {
            return values().filter(predicate);
        }
    }

    @Override
    default Seq<T> reject(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        if (isEmpty()) {
            return Stream.empty();
        } else {
            return values().reject(predicate);
        }
    }

    /**
     * Transforms each element of this Tree into an {@code Iterable} of elements and flattens the resulting
     * iterables into a single Tree.
     * <p>
     * Because a Tree is hierarchical, if {@code mapper} produces an empty {@code Iterable} for a node's value,
     * that node and its entire subtree are removed from the result, even if {@code mapper} would have produced
     * elements for descendant nodes.
     *
     * @param mapper A mapper
     * @param <U>    Component type
     * @return a new Tree
     * @throws NullPointerException if {@code mapper} is null
     */
    @Override
    default <U extends Object> Tree<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return isEmpty() ? Empty.instance() : TreeModule.flatMap((Node<T>) this, mapper);
    }

    @Override
    default <U extends Object> U foldRight(U zero, BiFunction<? super T, ? super U, ? extends U> f) {
        Objects.requireNonNull(f, "f is null");
        if (isEmpty()) {
            return zero;
        } else {
            return iterator().foldRight(zero, f);
        }
    }

    @Override
    default <C extends Object> Map<C, Seq<T>> groupBy(Function<? super T, ? extends C> classifier) {
        return jdk.m3.collections.vavr.collection.Collections.groupBy(values(), classifier, Stream::ofAll);
    }

    @Override
    default jdk.m3.collections.vavr.collection.Iterator<Seq<T>> grouped(int size) {
        return sliding(size, size);
    }

    @Override
    default boolean hasDefiniteSize() {
        return true;
    }

    @Override
    default T head() {
        if (isEmpty()) {
            throw new NoSuchElementException("head of empty tree");
        } else {
            return iterator().next();
        }
    }

    @Override
    default Seq<T> init() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("init of empty tree");
        } else {
            return values().init();
        }
    }

    @Override
    default Option<Seq<T>> initOption() {
        return isEmpty() ? Option.none() : Option.some(init());
    }

    @Override
    default boolean isTraversableAgain() {
        return true;
    }

    @Override
    default jdk.m3.collections.vavr.collection.Iterator<T> iterator() {
        return values().iterator();
    }

    @Override
    default <U extends Object> Tree<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return isEmpty() ? Empty.instance() : TreeModule.map((Node<T>) this, mapper);
    }

    @Override
    default <U extends Object> Tree<U> mapTo(U value) {
        return map(ignored -> value);
    }

    @Override
    default Tree<Void> mapToVoid() {
        return this.<Void>map(ignored -> null);
    }

    @Override
    default Tree<T> orElse(Iterable<? extends T> other) {
        return isEmpty() ? ofAll(other) : this;
    }

    @Override
    default Tree<T> orElse(Supplier<? extends Iterable<? extends T>> supplier) {
        return isEmpty() ? ofAll(supplier.get()) : this;
    }

    @SuppressWarnings("unchecked")
    @Override
    default Tuple2<Seq<T>, Seq<T>> partition(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        if (isEmpty()) {
            return Tuple.of(Stream.empty(), Stream.empty());
        } else {
            return (Tuple2<Seq<T>, Seq<T>>) values().partition(predicate);
        }
    }

    @Override
    default Tree<T> peek(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action is null");
        if (!isEmpty()) {
            action.accept(head());
        }
        return this;
    }

    @Override
    default Tree<T> replace(T currentElement, T newElement) {
        if (isEmpty()) {
            return Empty.instance();
        } else {
            return TreeModule.replace((Node<T>) this, currentElement, newElement);
        }
    }

    @Override
    default Tree<T> replaceAll(T currentElement, T newElement) {
        return map(t -> Objects.equals(t, currentElement) ? newElement : t);
    }

    @Override
    default Seq<T> retainAll(Iterable<? extends T> elements) {
        Objects.requireNonNull(elements, "elements is null");
        return values().retainAll(elements);
    }

    @Override
    default Seq<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation) {
        return scanLeft(zero, operation);
    }

    @Override
    default <U extends Object> Seq<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
        return jdk.m3.collections.vavr.collection.Collections.scanLeft(this, zero, operation, jdk.m3.collections.vavr.collection.Iterator::toStream);
    }

    @Override
    default <U extends Object> Seq<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation) {
        return jdk.m3.collections.vavr.collection.Collections.scanRight(this, zero, operation, jdk.m3.collections.vavr.collection.Iterator::toStream);
    }

    @Override
    default jdk.m3.collections.vavr.collection.Iterator<Seq<T>> slideBy(Function<? super T, ?> classifier) {
        return iterator().slideBy(classifier);
    }

    @Override
    default jdk.m3.collections.vavr.collection.Iterator<Seq<T>> sliding(int size) {
        return sliding(size, 1);
    }

    @Override
    default jdk.m3.collections.vavr.collection.Iterator<Seq<T>> sliding(int size, int step) {
        return iterator().sliding(size, step);
    }

    @SuppressWarnings("unchecked")
    @Override
    default Tuple2<Seq<T>, Seq<T>> span(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        if (isEmpty()) {
            return Tuple.of(Stream.empty(), Stream.empty());
        } else {
            return (Tuple2<Seq<T>, Seq<T>>) values().span(predicate);
        }
    }

    @Override
    default String stringPrefix() {
        return "Tree";
    }

    @Override
    default Seq<T> tail() {
        if (isEmpty()) {
            throw new UnsupportedOperationException("tail of empty tree");
        } else {
            return values().tail();
        }
    }

    @Override
    default Option<Seq<T>> tailOption() {
        return isEmpty() ? Option.none() : Option.some(tail());
    }

    @Override
    default Seq<T> take(int n) {
        if (isEmpty()) {
            return Stream.empty();
        } else {
            return values().take(n);
        }
    }

    @Override
    default Seq<T> takeRight(int n) {
        if (isEmpty()) {
            return Stream.empty();
        } else {
            return values().takeRight(n);
        }
    }

    @Override
    default Seq<T> takeUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return values().takeUntil(predicate);
    }

    @Override
    default Seq<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return values().takeWhile(predicate);
    }

    @SuppressWarnings("unchecked")
    @Override
    default <T1 extends Object, T2 extends Object> Tuple2<Tree<T1>, Tree<T2>> unzip(
      Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        if (isEmpty()) {
            return Tuple.of(Empty.instance(), Empty.instance());
        } else {
            return (Tuple2<Tree<T1>, Tree<T2>>) (Object) TreeModule.unzip((Node<T>) this, unzipper);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    default <T1 extends Object, T2 extends Object, T3 extends Object> Tuple3<Tree<T1>, Tree<T2>, Tree<T3>> unzip3(
      Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
        Objects.requireNonNull(unzipper, "unzipper is null");
        if (isEmpty()) {
            return Tuple.of(Empty.instance(), Empty.instance(), Empty.instance());
        } else {
            return (Tuple3<Tree<T1>, Tree<T2>, Tree<T3>>) (Object) TreeModule.unzip3((Node<T>) this, unzipper);
        }
    }

    @Override
    default <U extends Object> Tree<Tuple2<T, U>> zip(Iterable<? extends U> that) {
        return zipWith(that, Tuple::of);
    }

    @Override
    default <U extends Object, R extends Object> Tree<R> zipWith(Iterable<? extends U> that, BiFunction<? super T, ? super U, ? extends R> mapper) {
        Objects.requireNonNull(that, "that is null");
        Objects.requireNonNull(mapper, "mapper is null");
        if (isEmpty()) {
            return Empty.instance();
        } else {
            return TreeModule.zip((Node<T>) this, that.iterator(), mapper);
        }
    }

    @Override
    default <U extends Object> Tree<Tuple2<T, U>> zipAll(Iterable<? extends U> that, T thisElem, U thatElem) {
        Objects.requireNonNull(that, "that is null");
        if (isEmpty()) {
            return jdk.m3.collections.vavr.collection.Iterator.<U> ofAll(that).map(elem -> Tuple.of(thisElem, elem)).toTree();
        } else {
            final java.util.Iterator<? extends U> thatIter = that.iterator();
            final Tree<Tuple2<T, U>> tree = TreeModule.zipAll((Node<T>) this, thatIter, thatElem);
            if (thatIter.hasNext()) {
                final Iterable<Node<Tuple2<T, U>>> remainder = jdk.m3.collections.vavr.collection.Iterator
                        .ofAll(thatIter)
                        .map(elem -> of(Tuple.of(thisElem, elem)));
                return new Node<>(tree.getValue(), tree.getChildren().appendAll(remainder));
            } else {
                return tree;
            }
        }
    }

    @Override
    default Tree<Tuple2<T, Integer>> zipWithIndex() {
        return zipWithIndex(Tuple::of);
    }

    @Override
    default <U extends Object> Tree<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return zipWith(jdk.m3.collections.vavr.collection.Iterator.from(0), mapper);
    }

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    @Override
    String toString();

    /**
     * Creates a neat 2-dimensional drawing of a tree. Unicode characters are used to draw node junctions.
     *
     * @return A nice string representation of the tree.
     */
    String draw();

    /**
     * Represents a tree node.
     *
     * @param <T> value type
     */
    final class Node<T extends Object> implements Tree<T>, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("serial") // Conditionally serializable
        private final T value;
        private final jdk.m3.collections.vavr.collection.List<Node<T>> children;

        private final int size;

        /**
         * Constructs a rose tree node. A node with an empty {@code children} list is a leaf.
         *
         * @param value    A value.
         * @param children The list of children, possibly empty.
         * @throws NullPointerException if children is null
         */
        public Node(T value, jdk.m3.collections.vavr.collection.List<Node<T>> children) {
            Objects.requireNonNull(children, "children is null");
            this.value = value;
            this.children = children;
            size = children.foldLeft(1, (acc, child) -> acc + child.size);
        }

        @Override
        public jdk.m3.collections.vavr.collection.List<Node<T>> getChildren() {
            return children;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public int length() {
            return size;
        }

        @Override
        public boolean isLeaf() {
            return size == 1;
        }

        @Override
        public T last() {
            return children.isEmpty() ? value : children.last().last();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof Node) {
                final Node<?> that = (Node<?>) o;
                return Objects.equals(this.getValue(), that.getValue())
                        && Objects.equals(this.getChildren(), that.getChildren());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Tuple.hash(value, children);
        }

        @Override
        public String toString() {
            return mkString(stringPrefix() + "(", ", ", ")");
        }

        @Override
        public String toLispString() {
            return toLispString(this);
        }

        @Override
        public String draw() {
            final StringBuilder builder = new StringBuilder();
            drawAux("", builder);
            return builder.toString();
        }

        private void drawAux(String indent, StringBuilder builder) {
            builder.append(value);
            for (jdk.m3.collections.vavr.collection.List<Node<T>> it = children; !it.isEmpty(); it = it.tail()) {
                final boolean isLast = it.tail().isEmpty();
                builder.append('\n')
                        .append(indent)
                        .append(isLast ? "└──" : "├──");
                it.head().drawAux(indent + (isLast ? "   " : "│  "), builder);
            }
        }

        private static String toLispString(Tree<?> tree) {
            final String value = String.valueOf(tree.getValue());
            if (tree.isLeaf()) {
                return value;
            } else {
                final String children = tree.getChildren().map(child -> toLispString(child)).mkString(" ");
                return "(" + value + " " + children + ")";
            }
        }

        // -- Serializable implementation

        /**
         * {@code writeReplace} method for the serialization proxy pattern.
         * <p>
         * The presence of this method causes the serialization system to emit a SerializationProxy instance instead of
         * an instance of the enclosing class.
         *
         * @return A SerializationProxy for this enclosing class.
         */
        @Serial
        private Object writeReplace() {
            return new SerializationProxy<>(this);
        }

        /**
         * {@code readObject} method for the serialization proxy pattern.
         * <p>
         * Guarantees that the serialization system will never generate a serialized instance of the enclosing class.
         *
         * @param stream An object serialization stream.
         * @throws java.io.InvalidObjectException This method will throw with the message "Proxy required".
         */
        @Serial
        private void readObject(ObjectInputStream stream) throws InvalidObjectException {
            throw new InvalidObjectException("Proxy required");
        }

        /**
         * A serialization proxy which, in this context, is used to deserialize immutable nodes with final
         * instance fields.
         *
         * @param <T> The component type of the underlying tree.
         */
        // DEV NOTE: The serialization proxy pattern is not compatible with non-final, i.e. extendable,
        // classes. Also, it may not be compatible with circular object graphs.
        private static final class SerializationProxy<T extends Object> implements Serializable {

            @Serial
            private static final long serialVersionUID = 1L;

            // the instance to be serialized/deserialized
            private transient Node<T> node;

            /**
             * Constructor for the case of serialization, called by {@link Node#writeReplace()}.
             * <p/>
             * The constructor of a SerializationProxy takes an argument that concisely represents the logical state of
             * an instance of the enclosing class.
             *
             * @param node a Branch
             */
            SerializationProxy(Node<T> node) {
                this.node = node;
            }

            /**
             * Write an object to a serialization stream.
             *
             * @param s An object serialization stream.
             * @throws java.io.IOException If an error occurs writing to the stream.
             */
            @Serial
            private void writeObject(ObjectOutputStream s) throws IOException {
                s.defaultWriteObject();
                s.writeObject(node.value);
                s.writeObject(node.children);
            }

            /**
             * Read an object from a deserialization stream.
             *
             * @param s An object deserialization stream.
             * @throws ClassNotFoundException If the object's class read from the stream cannot be found.
             * @throws IOException            If an error occurs reading from the stream.
             */
            @Serial
            @SuppressWarnings("unchecked")
            private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
                s.defaultReadObject();
                final T value = (T) s.readObject();
                final jdk.m3.collections.vavr.collection.List<Node<T>> children = (jdk.m3.collections.vavr.collection.List<Node<T>>) s.readObject();
                node = new Node<>(value, children);
            }

            /**
             * {@code readResolve} method for the serialization proxy pattern.
             * <p>
             * Returns a logically equivalent instance of the enclosing class. The presence of this method causes the
             * serialization system to translate the serialization proxy back into an instance of the enclosing class
             * upon deserialization.
             *
             * @return A deserialized instance of the enclosing class.
             */
            @Serial
            private Object readResolve() {
                return node;
            }
        }
    }

    /**
     * The empty tree. Use Tree.empty() to create an instance.
     *
     * @param <T> type of the tree's values
     */
    final class Empty<T extends Object> implements Tree<T>, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private static final Empty<?> INSTANCE = new Empty<>();

        // hidden
        private Empty() {
        }

        @SuppressWarnings("unchecked")
        public static <T extends Object> Empty<T> instance() {
            return (Empty<T>) INSTANCE;
        }

        @Override
        public jdk.m3.collections.vavr.collection.List<Node<T>> getChildren() {
            return Nil.instance();
        }

        @Override
        public T getValue() {
            throw new UnsupportedOperationException("getValue of empty Tree");
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int length() {
            return 0;
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public T last() {
            throw new NoSuchElementException("last of empty tree");
        }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public String toString() {
            return stringPrefix() + "()";
        }

        @Override
        public String toLispString() {
            return "()";
        }

        @Override
        public String draw() { return "▣"; }

        // -- Serializable implementation

        /**
         * Instance control for object serialization.
         *
         * @return The singleton instance of Empty.
         * @see java.io.Serializable
         */
        @Serial
        private Object readResolve() {
            return INSTANCE;
        }
    }

    /**
     * Tree traversal order.
     * <p>
     * Example tree:
     * <pre>
     * {@code
     *         1
     *        / \
     *       /   \
     *      /     \
     *     2       3
     *    / \     /
     *   4   5   6
     *  /       / \
     * 7       8   9
     * }
     * </pre>
     * <p>
     * See also
     * <ul>
     * <li><a href="http://en.wikipedia.org/wiki/Tree_traversal">Tree traversal</a> (wikipedia)</li>
     * <li>See <a href="http://rosettacode.org/wiki/Tree_traversal">Tree traversal</a> (rosetta code)</li>
     * </ul>
     */
    // see http://programmers.stackexchange.com/questions/138766/in-order-traversal-of-m-way-trees
    enum Order {

        /**
         * 1 2 4 7 5 3 6 8 9 (= depth-first)
         */
        PRE_ORDER,

        /**
         * 7 4 2 5 1 8 6 9 3
         */
        IN_ORDER,

        /**
         * 7 4 5 2 8 9 6 3 1
         */
        POST_ORDER,

        /**
         * 1 2 3 4 5 6 7 8 9 (= breadth-first)
         */
        LEVEL_ORDER
    }
}

/**
 * Because the empty tree {@code Empty} cannot be a child of an existing tree, method implementations distinguish between the
 * empty and non-empty case. Because the structure of trees is recursive, often we have commands in the form of module
 * classes with one static method.
 */
interface TreeModule {

    @SuppressWarnings("unchecked")
    static <T extends Object, U extends Object> Tree<U> flatMap(Node<T> node, Function<? super T, ? extends Iterable<? extends U>> mapper) {
        final Tree<U> mapped = ofAll(mapper.apply(node.getValue()));
        if (mapped.isEmpty()) {
            return empty();
        } else {
            final jdk.m3.collections.vavr.collection.List<Node<U>> children = (jdk.m3.collections.vavr.collection.List<Node<U>>) (Object) node
                    .getChildren()
                    .map(child -> flatMap(child, mapper))
                    .filter(Tree::nonEmpty);
            return of(mapped.get(), children.prependAll(mapped.getChildren()));
        }
    }

    static <T extends Object, U extends Object> Node<U> map(Node<T> node, Function<? super T, ? extends U> mapper) {
        final U value = mapper.apply(node.getValue());
        final jdk.m3.collections.vavr.collection.List<Node<U>> children = node.getChildren().map(child -> map(child, mapper));
        return new Node<>(value, children);
    }

    // Idea:
    // Traverse (depth-first) until a match is found, then stop and rebuild relevant parts of the tree.
    // If not found, return the same tree instance.
    static <T extends Object> Node<T> replace(Node<T> node, T currentElement, T newElement) {
        if (Objects.equals(node.getValue(), currentElement)) {
            return new Node<>(newElement, node.getChildren());
        } else {
            for (Node<T> child : node.getChildren()) {
                final Node<T> newChild = replace(child, currentElement, newElement);
                final boolean found = newChild != child;
                if (found) {
                    final jdk.m3.collections.vavr.collection.List<Node<T>> newChildren = node.getChildren().replace(child, newChild);
                    return new Node<>(node.getValue(), newChildren);
                }
            }
            return node;
        }
    }

    static <T extends Object> Stream<Node<T>> traversePreOrder(Node<T> node) {
        return Stream.ofAll(depthFirst(node, Order.PRE_ORDER));
    }

    static <T extends Object> Stream<Node<T>> traverseInOrder(Node<T> node) {
        return Stream.ofAll(depthFirst(node, Order.IN_ORDER));
    }

    // The recursive fold this replaces returned an AppendElements stream whose base holds the
    // post-order of the leftmost leaf's parent without that parent and whose queue holds the
    // rest; rebuilding that shape keeps the toString() of a partly evaluated result unchanged.
    static <T extends Object> Stream<Node<T>> traversePostOrder(Node<T> node) {
        if (node.isLeaf()) {
            return Stream.of(node);
        }
        final ArrayList<Node<T>> out = depthFirst(node, Order.POST_ORDER);
        final int split = postOrderSplit(node);
        return Stream.ofAll(out.subList(0, split))
                .append(out.get(split))
                .appendAll(out.subList(split + 1, out.size()));
    }

    static <T extends Object> Stream<Node<T>> traverseLevelOrder(Node<T> node) {
        final ArrayList<Node<T>> out = new ArrayList<>(node.length());
        out.add(node);
        for (int read = 0; read < out.size(); read++) {
            for (List<Node<T>> it = out.get(read).getChildren(); !it.isEmpty(); it = it.tail()) {
                out.add(it.head());
            }
        }
        return Stream.ofAll(out);
    }

    /**
     * Fills an exact-size lane with the pre-, in- or post-order of {@code root} using
     * (owner, children) frames.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Object> ArrayList<Node<T>> depthFirst(Node<T> root, Order order) {
        final ArrayList<Node<T>> out = new ArrayList<>(root.length());
        final ArrayDeque<Object> frames = new ArrayDeque<>();
        descend(root, frames, out, order);
        while (climb(frames, out, order)) {
            descend(((List<Node<T>>) frames.peek()).head(), frames, out, order);
        }
        return out;
    }

    /**
     * Pushes one (owner, children) frame per branch on the leftmost path from {@code start}, then
     * emits its leaf.
     */
    private static <T extends Object> void descend(Node<T> start, ArrayDeque<Object> frames,
            ArrayList<Node<T>> out, Order order) {
        Node<T> node = start;
        while (!node.isLeaf()) {
            if (order == Order.PRE_ORDER) {
                out.add(node);
            }
            frames.push(node);
            frames.push(node.getChildren());
            node = node.getChildren().head();
        }
        out.add(node);
    }

    /**
     * Pops finished subtrees; emits an in-order owner after its first child and a post-order owner
     * after its last.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Object> boolean climb(ArrayDeque<Object> frames,
            ArrayList<Node<T>> out, Order order) {
        while (!frames.isEmpty()) {
            final List<Node<T>> current = (List<Node<T>>) frames.pop();
            final Node<T> owner = (Node<T>) frames.peek();
            final List<Node<T>> rest = current.tail();
            if ((order == Order.IN_ORDER && current == owner.getChildren())
                    || (order == Order.POST_ORDER && rest.isEmpty())) {
                out.add(owner);
            }
            if (!rest.isEmpty()) {
                frames.push(rest);
                return true;
            }
            frames.pop();
        }
        return false;
    }

    /**
     * Post-order offset where the old AppendElements queue began: size of the leftmost leaf's
     * parent, minus one.
     */
    private static <T extends Object> int postOrderSplit(Node<T> node) {
        Node<T> parent = node;
        while (!parent.getChildren().head().isLeaf()) {
            parent = parent.getChildren().head();
        }
        return parent.length() - 1;
    }

    static <T extends Object, T1 extends Object, T2 extends Object> Tuple2<Node<T1>, Node<T2>> unzip(Node<T> node,
                                                        Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) {
        final Tuple2<? extends T1, ? extends T2> value = unzipper.apply(node.getValue());
        final jdk.m3.collections.vavr.collection.List<Tuple2<Node<T1>, Node<T2>>> children = node
                .getChildren()
                .map(child -> unzip(child, unzipper));
        final Node<T1> node1 = new Node<>(value._1, children.map(t -> t._1));
        final Node<T2> node2 = new Node<>(value._2, children.map(t -> t._2));
        return Tuple.of(node1, node2);
    }

    static <T extends Object, T1 extends Object, T2 extends Object, T3 extends Object> Tuple3<Node<T1>, Node<T2>, Node<T3>> unzip3(Node<T> node,
                                                                       Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
        final Tuple3<? extends T1, ? extends T2, ? extends T3> value = unzipper.apply(node.getValue());
        final jdk.m3.collections.vavr.collection.List<Tuple3<Node<T1>, Node<T2>, Node<T3>>> children = node.getChildren()
                .map(child -> unzip3(child, unzipper));
        final Node<T1> node1 = new Node<>(value._1, children.map(t -> t._1));
        final Node<T2> node2 = new Node<>(value._2, children.map(t -> t._2));
        final Node<T3> node3 = new Node<>(value._3, children.map(t -> t._3));
        return Tuple.of(node1, node2, node3);
    }

    @SuppressWarnings("unchecked")
    static <T extends Object, U extends Object, R extends Object> Tree<R> zip(Node<T> node, java.util.Iterator<? extends U> that, BiFunction<? super T, ? super U, ? extends R> mapper) {
        if (!that.hasNext()) {
            return Empty.instance();
        } else {
            final R value = mapper.apply(node.getValue(), that.next());
            final jdk.m3.collections.vavr.collection.List<Node<R>> children = (jdk.m3.collections.vavr.collection.List<Node<R>>) (Object) node
                    .getChildren()
                    .map(child -> zip(child, that, mapper))
                    .filter(Tree::nonEmpty);
            return new Node<>(value, children);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends Object, U extends Object> Tree<Tuple2<T, U>> zipAll(Node<T> node, java.util.Iterator<? extends U> that, U thatElem) {
        if (!that.hasNext()) {
            return node.map(value -> Tuple.of(value, thatElem));
        } else {
            final Tuple2<T, U> value = Tuple.of(node.getValue(), that.next());
            final jdk.m3.collections.vavr.collection.List<Node<Tuple2<T, U>>> children = (jdk.m3.collections.vavr.collection.List<Node<Tuple2<T, U>>>) (Object) node
                    .getChildren()
                    .map(child -> zipAll(child, that, thatElem))
                    .filter(Tree::nonEmpty);
            return new Node<>(value, children);
        }
    }
}
