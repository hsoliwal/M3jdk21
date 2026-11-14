/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * All rights reserved.
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
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef M3_OBJECT_H
#define M3_OBJECT_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/* These values mirror java.lang.ObjectEvent's field-change masks. */
#define M3_OBJECT_REFERENCE 1
#define M3_OBJECT_VALUE     2

/*
 * Notify the existing Java observation owner after a successful logical
 * mutation of source. This is an explicit producer hook, not a field/store
 * interceptor. The producer is responsible for completion and no-op checks.
 *
 * env belongs to the current attached thread. All references are borrowed for
 * this call only. No Java listener runs inline; the Java owner queues delivery.
 * Required null arguments throw NullPointerException.
 *
 * JNI_TRUE means notification completed without an exception, including when
 * observation is disabled or source is unobserved. It does not promise delivery.
 * JNI_FALSE preserves an existing or newly raised pending exception. An error
 * in notification cannot roll back the producer's already committed mutation.
 */
JNIEXPORT jboolean JNICALL
M3Object_NotifyContentChanged(JNIEnv *env, jobject source);

/*
 * Notify an existing observation owner after a successful unequal transition.
 * The exact source, declaring class, field name and field type identify an
 * already active field slot when one exists. Otherwise only root subscribers
 * can receive this notification. No access plan or observation state is created.
 * mask must be M3_OBJECT_REFERENCE or M3_OBJECT_VALUE and agree with the slot
 * type. Other mask values throw IllegalArgumentException. The remaining
 * lifetime, exception and result contract is the same as the content helper.
 */
JNIEXPORT jboolean JNICALL
M3Object_NotifyFieldChanged(JNIEnv *env, jobject source,
                          jclass declaring_type, jstring field_name,
                          jclass field_type, jint mask);

#ifdef __cplusplus
}
#endif

#endif /* M3_OBJECT_H */
