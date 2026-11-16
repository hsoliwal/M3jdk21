/*
 * Copyright 2012 by Thomas Mauch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id$
 */
package jdk.m3.collections.brownies.primitive;

import java.io.Serializable;

import jdk.m3.collections.brownies.IList;

/**
 * Class {@link IPrimitiveList} is the basic interface for all lists storing primitives.
 *
 * @author Thomas Mauch
 */
public interface IPrimitiveList extends Cloneable, Serializable {

    /** Returns primitive class of the values stored in this list */
    Class<?> getPrimitiveClass();

    /** Converts this list into a generic IList, e.g. a {@code IntGapList} will be converted to a {@code IList<Integer>} */
    IList<?> toIList();

    /**
     * Returns this list into a generic IList, e.g. a {@code IntGapList} will be converted to a {@code IntObjGapList}.
     * The returned class offers full read and write access to the data of this IList, i.e. the elements are shared.
     */
    IList<?> asObjIList();

}