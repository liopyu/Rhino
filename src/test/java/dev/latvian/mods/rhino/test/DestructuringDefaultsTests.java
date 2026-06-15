package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

/**
 * Coverage for destructuring with default values. Exercises the cases that
 * Mozilla/rhino@e8b23f5b3 targets — array/object literal defaults, function/
 * update/parenthesized expression rhs.
 */
@SuppressWarnings("unused")
public class DestructuringDefaultsTests {
	public static final RhinoTest TEST = new RhinoTest("destructuringDefaults");

	@Test
	public void simpleArrayDefault() {
		TEST.test("simpleArrayDefault", """
			let [a = 7] = [];
			console.info(a);
			let [b = 7] = [9];
			console.info(b);
			""", """
			7
			9
			""");
	}

	@Test
	public void arrayLiteralDefault() {
		TEST.test("arrayLiteralDefault", """
			let [a = [1, 2, 3]] = [];
			console.info(a.join(','));
			let [b = [4, 5]] = [[7, 8]];
			console.info(b.join(','));
			""", """
			1,2,3
			7,8
			""");
	}

	@Test
	public void objectLiteralDefault() {
		TEST.test("objectLiteralDefault", """
			let [x = {a: 1, b: 2}] = [];
			console.info(x.a + ',' + x.b);
			let [{p = 10, q = 20} = {}] = [];
			console.info(p + ',' + q);
			let [{p: pp = 99} = {p: 5}] = [];
			console.info(pp);
			""", """
			1,2
			10,20
			5
			""");
	}

	@Test
	public void functionExprDefault() {
		TEST.test("functionExprDefault", """
			let [f = function () { return 42 }] = [];
			console.info(f());
			""", """
			42
			""");
	}

	@Test
	public void parenthesizedDefault() {
		TEST.test("parenthesizedDefault", """
			let [a = (1 + 2)] = [];
			console.info(a);
			""", """
			3
			""");
	}

	@Test
	public void updateDefault() {
		TEST.test("updateDefault", """
			let i = 5;
			let [a = ++i] = [];
			console.info(a + ',' + i);
			""", """
			6,6
			""");
	}

	@Test
	public void nestedArrayDefault() {
		TEST.test("nestedArrayDefault", """
			let [[a, b] = [10, 20]] = [];
			console.info(a + ',' + b);
			""", """
			10,20
			""");
	}
}
