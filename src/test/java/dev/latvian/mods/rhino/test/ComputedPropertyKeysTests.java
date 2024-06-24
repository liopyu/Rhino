package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
public class ComputedPropertyKeysTests {
	public static final RhinoTest TEST = new RhinoTest("computedPropertyKeys");

	@Test
	public void stringKey() {
		TEST.test("stringKey", """
			let k = "foo";
			let o = { [k]: 42 };
			console.info(o.foo);
			""", "42");
	}

	@Test
	public void numericKey() {
		TEST.test("numericKey", """
			let i = 7;
			let o = { [i]: "x" };
			console.info(o[7]);
			""", "x");
	}

	@Test
	public void exprKey() {
		TEST.test("exprKey", """
			let o = { ["a" + "b"]: 1 };
			console.info(o.ab);
			""", "1");
	}

	@Test
	public void mixedKeys() {
		TEST.test("mixedKeys", """
			let k = "dynamic";
			let o = { static: 1, [k]: 2 };
			console.info(o.static + ',' + o.dynamic);
			""", "1,2");
	}

	@Test
	public void evaluationOrderInterleaved() {
		// Per spec, keys and values evaluate interleaved. The bug fix in
		// Mozilla/rhino@c37f0b256 was for this exact case.
		TEST.test("evaluationOrderInterleaved", """
			let i = 0;
			let o = {
				[++i]: ++i,
				[++i]: ++i,
			};
			console.info(o[1] + ',' + o[3]);
			""", "2,4");
	}
}
