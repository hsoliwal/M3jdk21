/*
 * Copyright (c) 2026, Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

/**
 * JDK-internal MIndex functional plans and fused primitive pipelines.
 *
 * <p>This package is an implementation substrate, not Java SE API. Plans implement the standard
 * primitive java.util.function interfaces so ordinary JDK call sites can use them without public
 * signature changes. Plan-to-plan composition is fused; non-MIndex functions retain ordinary JDK
 * fallback behavior.</p>
 *
 * <p>Stateful pipeline execution uses caller-reusable workspaces. The package deliberately avoids
 * changing java.util.function or java.util.stream in its first admission step; later compiler or
 * OpenRewrite lowering may target these plans only after semantic proof.</p>
 */
package jdk.internal.mindex.function;
