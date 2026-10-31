/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.core.tests.dom;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;

/**
 * Comparative probe for directly owned child-list storage. Run in separate JVMs
 * for the sealed before/after source builds. Sizes come from Instrumentation;
 * they are not the DOM's approximate subtreeBytes() values. The measured retained
 * region is NodeList plus its backing list/array, not the entire compiler heap.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ASTNodeListStorageProbe {

	private static Instrumentation instrumentation;
	private static volatile Object retained;

	public static void premain(String arguments, Instrumentation agent) {
		instrumentation = agent;
	}

	private static Block[] workload(int blockCount, int children) {
		AST ast = AST.newAST(AST.getJLSLatest(), false);
		Block[] blocks = new Block[blockCount];
		for (int i = 0; i < blocks.length; i++) {
			Block block = ast.newBlock();
			for (int child = 0; child < children; child++) {
				block.statements().add(ast.newEmptyStatement());
			}
			if (children > 1) {
				ASTNode first = (ASTNode) block.statements().remove(0);
				block.statements().add(children - 1, first);
			}
			blocks[i] = block;
		}
		retained = blocks;
		return blocks;
	}

	private static long[] retainedListStorage(Block[] blocks) throws Exception {
		IdentityHashMap<Object, Boolean> counted = new IdentityHashMap<>();
		Field nodeStore = blocks[0].statements().getClass().getDeclaredField("store");
		nodeStore.setAccessible(true);
		Field arrayListData = ArrayList.class.getDeclaredField("elementData");
		arrayListData.setAccessible(true);
		long bytes = 0;
		long objects = 0;
		for (Block block : blocks) {
			Object list = block.statements();
			Object store = nodeStore.get(list);
			Object array = store instanceof ArrayList ? arrayListData.get(store) : store;
			for (Object value : new Object[] { list, store, array }) {
				if (counted.put(value, Boolean.TRUE) == null) {
					bytes += instrumentation.getObjectSize(value);
					objects++;
				}
			}
		}
		return new long[] { bytes, objects };
	}

	private static long processRssKiB() {
		Path status = Path.of("/proc/self/status");
		if (!Files.isReadable(status)) {
			return -1;
		}
		try {
			for (String line : Files.readAllLines(status)) {
				if (line.startsWith("VmRSS:")) {
					return Long.parseLong(line.split("\\s+")[1]);
				}
			}
		} catch (Exception unavailable) {
			return -1;
		}
		return -1;
	}

	public static void main(String[] arguments) throws Exception {
		if (instrumentation == null) {
			throw new IllegalStateException("Run with the probe JAR as -javaagent");
		}
		String variant = arguments.length > 0 ? arguments[0] : "unspecified";
		int blocks = arguments.length > 1 ? Integer.parseInt(arguments[1]) : 10000;
		int children = arguments.length > 2 ? Integer.parseInt(arguments[2]) : 4;
		if (blocks < 1 || children < 0) {
			throw new IllegalArgumentException("Positive block count and nonnegative child count required");
		}
		com.sun.management.ThreadMXBean bean =
				(com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
		bean.setThreadAllocatedMemoryEnabled(true);
		bean.setThreadCpuTimeEnabled(true);
		long thread = Thread.currentThread().threadId();
		for (int warmup = 0; warmup < 10; warmup++) {
			workload(blocks, children);
		}
		List<String> samples = new ArrayList<>();
		Block[] last = null;
		for (int sample = 0; sample < 7; sample++) {
			long allocated = bean.getThreadAllocatedBytes(thread);
			long cpu = bean.getCurrentThreadCpuTime();
			long wall = System.nanoTime();
			last = workload(blocks, children);
			wall = System.nanoTime() - wall;
			cpu = bean.getCurrentThreadCpuTime() - cpu;
			allocated = bean.getThreadAllocatedBytes(thread) - allocated;
			samples.add("{\"allocatedBytes\":" + allocated + ",\"threadCpuNanos\":" + cpu
					+ ",\"wallNanos\":" + wall + "}");
		}
		long[] storage = retainedListStorage(last);
		System.out.println("{\"variant\":\"" + variant + "\",\"java\":\""
				+ System.getProperty("java.runtime.version") + "\",\"blocks\":" + blocks
				+ ",\"childrenPerBlock\":" + children + ",\"retainedChildListStorageBytes\":"
				+ storage[0] + ",\"retainedChildListStorageObjects\":" + storage[1]
				+ ",\"processRssKiB\":" + processRssKiB()
				+ ",\"samples\":[" + String.join(",", samples) + "]}");
	}
}
