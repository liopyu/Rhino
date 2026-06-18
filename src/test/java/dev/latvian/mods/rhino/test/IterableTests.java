package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.NativeJavaMap;
import dev.latvian.mods.rhino.NativeJavaObject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test coverage for JS iterators using ({@code Symbol.iterator}).
 * <p>
 * For Java collections and maps, the {@code ES6Iterator} comes from a {@code Callable}
 * in {@link NativeJavaObject} and {@link  NativeJavaMap} respectively, whereas JS arrays
 * implement the {@code Symbol.iterator}/{@code values}/{@code keys}/{@code entries} functions directly.
 * <p>
 * TODO(agent or self): single test scope with all relevant objects added to global scope (or eval done locally)
 */
@SuppressWarnings("unused")
public class IterableTests {
	public static final RhinoTest TEST = new RhinoTest("iterable").withScopeAction((cx, rootScope) -> {
		List<Object> TEST_LIST = List.of("a", "a", 123);
		Set<Object> TEST_SET = new LinkedHashSet<>(TEST_LIST);

		Map<Object, Object> TEST_MAP = LinkedHashMap.newLinkedHashMap(2);
		TEST_MAP.put("a", "b");
		TEST_MAP.put("c", "d");

		List<Object> EMPTY_LIST = List.of();
		Map<Object, Object> EMPTY_MAP = Map.of();


		cx.addToScope(rootScope, "list", TEST_LIST);
		cx.addToScope(rootScope, "set", TEST_SET);
		cx.addToScope(rootScope, "emptyList", EMPTY_LIST);

		cx.addToScope(rootScope, "map", TEST_MAP);
		cx.addToScope(rootScope, "emptyMap", EMPTY_MAP);
	});

	// --- Java collections (NativeJavaObject / NativeJavaMap) ---

	@Test
	public void forOfOverJavaList() {
		TEST.test("forOfOverJavaList", """
			var a = [];
			for (var e of list) a.push(e);
			console.info(a.join(','));
			""", """
			a,a,123
			""");
	}

	@Test
	public void forOfOverJavaSet() {
		TEST.test("forOfOverJavaSet", """
			var a = [];
			for (var e of set) a.push(e);
			console.info(a.join(','));
			""", """
			a,123
			""");
	}

	@Test
	public void forOfOverEmptyJavaList() {
		TEST.test("forOfOverEmptyJavaList", """
			var a = [];
			for (var e of emptyList) a.push(e);
			console.info(a.length);
			""", """
			0
			""");
	}

	@Test
	public void forOfOverJavaMapEntries() {
		TEST.test("forOfOverJavaMapEntries", """
			var a = [];
			for (var [key, value] of map) a.push(key + '=' + value);
			console.info(a.join(','));
			""", """
			a=b,c=d
			""");
	}

	@Test
	public void forOfOverEmptyJavaMap() {
		TEST.test("forOfOverEmptyJavaMap", """
			var a = [];
			for (var [key, value] of emptyMap) a.push(key + '=' + value);
			console.info(a.length);
			""", """
			0
			""");
	}

	@Test
	public void arrayFromOverJavaList() {
		TEST.test("arrayFromOverJavaList", """
			console.info(Array.from(list).join(','));
			""", """
			a,a,123
			""");
	}

	@Test
	public void arrayFromOverJavaSet() {
		TEST.test("arrayFromOverJavaSet", """
			console.info(Array.from(set).join(','));
			""", """
			a,123
			""");
	}

	@Test
	public void arrayFromOverJavaMapEntries() {
		TEST.test("arrayFromOverJavaMapEntries", """
			var a = Array.from(map).map(e => e[0] + '=' + e[1]);
			console.info(a.join(','));
			""", """
			a=b,c=d
			""");
	}

	@Test
	public void manualSymbolIteratorCallOverJavaList() {
		TEST.test("manualSymbolIteratorCallOverJavaList", """
			var it = list[Symbol.iterator]();
			var r1 = it.next();
			var r2 = it.next();
			var r3 = it.next();
			var r4 = it.next();
			console.info(r1.value + ',' + r1.done + ',' + r2.value + ',' + r2.done + ',' + r3.value + ',' + r3.done + ',' + r4.done);
			""", """
			a,false,a,false,123,false,true
			""");
	}

	// --- Native arrays ---

	@Test
	public void forOfOverNativeArray() {
		TEST.test("forOfOverNativeArray", """
			var a = [];
			for (var e of [10, 20, 30]) a.push(e);
			console.info(a.join(','));
			""", """
			10,20,30
			""");
	}

	@Test
	public void manualSymbolIteratorCallOverNativeArray() {
		TEST.test("manualSymbolIteratorCallOverNativeArray", """
			var it = ['x', 'y'][Symbol.iterator]();
			var r1 = it.next();
			var r2 = it.next();
			var r3 = it.next();
			console.info(r1.value + ',' + r1.done + ',' + r2.value + ',' + r2.done + ',' + r3.done);
			""", """
			x,false,y,false,true
			""");
	}

	@Test
	public void nativeArrayValuesKeysEntries() {
		TEST.test("nativeArrayValuesKeysEntries", """
			var arr = ['a', 'b'];
			console.info(Array.from(arr.values()).join(','));
			console.info(Array.from(arr.keys()).join(','));
			console.info(Array.from(arr.entries()).map(e => e[0] + ':' + e[1]).join(','));
			""", """
			a,b
			0,1
			0:a,1:b
			""");
	}

	@Test
	public void arrayFromOverNativeArrayIterator() {
		TEST.test("arrayFromOverNativeArrayIterator", """
			console.info(Array.from([1, 2, 3][Symbol.iterator]()).join(','));
			""", """
			1,2,3
			""");
	}

	// --- Destructuring via iterator (function parameters) ---

	@Test
	public void functionParamIteratorReads() {
		TEST.test("functionParamIteratorReads", """
			function f([a, b, c]) {
				console.info(a + ',' + b + ',' + c);
			}
			f([1, 2, 3]);
			""", """
			1,2,3
			""");
	}

	@Test
	public void functionParamWithGenerator() {
		// Generators expose Symbol.iterator naturally.
		TEST.test("functionParamWithGenerator", """
			function* gen() { yield 10; yield 20; yield 30; }
			function consume([a, b, c]) {
				console.info(a + '+' + b + '+' + c);
			}
			consume(gen());
			""", """
			10+20+30
			""");
	}

	@Test
	public void localDestructureKeepsIndexedAccess() {
		// Non-function-param destructuring should still use indexed access
		// (NOT Symbol.iterator), so this works against plain objects
		// with numeric keys.
		TEST.test("localDestructureKeepsIndexedAccess", """
			let obj = {0: 'a', 1: 'b', length: 2};
			let [x, y] = obj;
			console.info(x + ',' + y);
			""", """
			a,b
			""");
	}
}
