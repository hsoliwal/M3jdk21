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
package jdk.m3.collections.vavr;

import jdk.m3.collections.vavr.collection.List;
import jdk.m3.collections.vavr.concurrent.Future;
import jdk.m3.collections.vavr.control.Either;
import jdk.m3.collections.vavr.control.Option;
import jdk.m3.collections.vavr.control.Try;
import jdk.m3.collections.vavr.control.Validation;

/**
 * <strong>INTERNAL TYPE - turned to jdk.m3.collections.vavr.Patterns by vavr-match annotation processor.</strong>
 */
class PatternExtractors {

    // -- jdk.m3.collections.vavr

    // Tuple0-N
    static Tuple0 Tuple0(Tuple0 tuple0) { return tuple0; }
    static <T1 extends Object> Tuple1<T1> Tuple1(Tuple1<T1> tuple1) { return tuple1; }
    static <T1 extends Object, T2 extends Object> Tuple2<T1, T2> Tuple2(Tuple2<T1, T2> tuple2) { return tuple2; }
    static <T1 extends Object, T2 extends Object, T3 extends Object> Tuple3<T1, T2, T3> Tuple3(Tuple3<T1, T2, T3> tuple3) { return tuple3; }
    static <T1 extends Object, T2 extends Object, T3 extends Object, T4 extends Object> Tuple4<T1, T2, T3, T4> Tuple4(Tuple4<T1, T2, T3, T4> tuple4) { return tuple4; }
    static <T1 extends Object, T2 extends Object, T3 extends Object, T4 extends Object, T5 extends Object> Tuple5<T1, T2, T3, T4, T5> Tuple5(Tuple5<T1, T2, T3, T4, T5> tuple5) { return tuple5; }
    static <T1 extends Object, T2 extends Object, T3 extends Object, T4 extends Object, T5 extends Object, T6 extends Object> Tuple6<T1, T2, T3, T4, T5, T6> Tuple6(Tuple6<T1, T2, T3, T4, T5, T6> tuple6) { return tuple6; }
    static <T1 extends Object, T2 extends Object, T3 extends Object, T4 extends Object, T5 extends Object, T6 extends Object, T7 extends Object> Tuple7<T1, T2, T3, T4, T5, T6, T7> Tuple7(Tuple7<T1, T2, T3, T4, T5, T6, T7> tuple7) { return tuple7; }
    static <T1 extends Object, T2 extends Object, T3 extends Object, T4 extends Object, T5 extends Object, T6 extends Object, T7 extends Object, T8 extends Object> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> Tuple8(Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> tuple8) { return tuple8; }

    // -- jdk.m3.collections.vavr.collection

    // List
    static <T extends Object> Tuple2<T, List<T>> Cons(List.Cons<T> cons) { return Tuple.of(cons.head(), cons.tail()); }
    static <T extends Object> Tuple0 Nil(List.Nil<T> nil) { return Tuple.empty(); }

    // -- jdk.m3.collections.vavr.concurrent

    static <T extends Object> Tuple1<Option<Try<T>>> Future(Future<T> future) { return Tuple.of(future.getValue()); }

    // -- jdk.m3.collections.vavr.control

    // Either
    static <L extends Object, R extends Object> Tuple1<R> Right(Either.Right<L, R> right) { return Tuple.of(right.get()); }
    static <L extends Object, R extends Object> Tuple1<L> Left(Either.Left<L, R> left) { return Tuple.of(left.getLeft()); }

    // Option
    static <T extends Object> Tuple1<T> Some(Option.Some<T> some) { return Tuple.of(some.get()); }
    static <T extends Object> Tuple0 None(Option.None<T> none) { return Tuple.empty(); }

    // Try
    static <T extends Object> Tuple1<T> Success(Try.Success<T> success) { return Tuple.of(success.get()); }
    static <T extends Object> Tuple1<Throwable> Failure(Try.Failure<T> failure) { return Tuple.of(failure.getCause()); }

    // Validation
    static <E extends Object, T extends Object> Tuple1<T> Valid(Validation.Valid<E, T> valid) { return Tuple.of(valid.get()); }
    static <E extends Object, T extends Object> Tuple1<E> Invalid(Validation.Invalid<E, T> invalid) { return Tuple.of(invalid.getError()); }

}
