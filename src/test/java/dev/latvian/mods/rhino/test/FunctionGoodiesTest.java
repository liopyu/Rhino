package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

/**
 * Tests for various different goodies for function parameters, namely
 * default parameters, destructuring parameters, and rest parameters;
 * especially mixed use thereof.
 */
@SuppressWarnings("unused")
public class FunctionGoodiesTest {
	public static final RhinoTest TEST = new RhinoTest("functionGoodies");

	@Test
	public void simpleDefault() {
		TEST.test("simpleDefault", """
			function f(a = 1, b = 2, c) { return a + ',' + b + ',' + c; }
			console.info(f());
			console.info(f(9));
			console.info(f(9, 8, 7));
			""", """
			1,2,undefined
			9,2,undefined
			9,8,7
			""");
	}

	@Test
	public void arrowDefault() {
		TEST.test("arrowDefault", """
			let f = (a = 5) => a;
			console.info(f());
			console.info(f(99));
			""", """
			5
			99
			""");
	}

	@Test
	public void defaultThenDestructureWithDefault() {
		TEST.test("defaultThenDestructureWithDefault", """
			function f(a = 1, {b = 2} = {}) { return a + ',' + b; }
			console.info(f());
			console.info(f(9, {b: 8}));
			""", """
			1,2
			9,8
			""");
	}

	@Test
	public void arrowFuncDefaultDestruct() {
		TEST.test("arrowFuncDefaultDestruct", """
			let f = ({a, b = 9} = {}) => a + ',' + b;
			console.info(f());
			console.info(f({a: 1}));
			console.info(f({a: 1, b: 2}));
			""", """
			undefined,9
			1,9
			1,2
			""");
	}
	
	@Test
	public void defaultParamBackref() {
		TEST.test("defaultParamBackref", """
			function f({a}, b = a * 2) { return a + ',' + b; }
			console.info(f({a: 6}));
			console.info(f({a: 6, b: 9}, 7));
			""", """
			6,12
			6,7
			""");
	}

	@Test
	public void defaultThenRest() {
		TEST.test("defaultThenRest", """
			function f(a, b = 2, ...rest) { return a + ',' + b + ',' + rest.join(';'); }
			console.info(f(1));
			console.info(f(1, 9));
			console.info(f(1, 9, 8, 7));
			""", """
			1,2,
			1,9,
			1,9,8;7
			""");
	}

	@Test
	public void destructRest() {
		TEST.test("destructRest", """
			function f({a, b}, ...rest) { return a + ',' + b + ',' + rest.join(';'); }
			console.info(f({a: 1, b: 2}, 8, 9));
			""", """
			1,2,8;9
			""");
	}

	@Test
	public void destructDefaultRest() {
		TEST.test("destructDefaultRest", """
			function f({a = 1, b = 2} = {}, ...rest) { return a + ',' + b + ',' + rest.join(';'); }
			console.info(f());
			console.info(f({a: 9}, 5, 6));
			""", """
			1,2,
			9,2,5;6
			""");
	}

	@Test
	public void arrayDestructWithDefault() {
		TEST.test("arrayDestructWithDefault", """
			function f([a, b = 5], ...rest) { return a + ',' + b + ',' + rest.join(';'); }
			console.info(f([1], 2, 3));
			console.info(f([1, 9], 2, 3));
			""", """
			1,5,2;3
			1,9,2;3
			""");
	}

	@Test
	public void paramAfterRestErrors() {
		TEST.test("paramAfterRestErrors", """
				try {
					eval('function f(...rest, s) {}');
					console.info('SUCCESS');
				} catch (e) {
					console.info('FAIL')
				}
			""", "FAIL");
	}
}
