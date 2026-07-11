package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
public class NumberBooleanTests {
	public static final RhinoTest TEST = new RhinoTest("number_boolean");

	@Test
	public void numberPrototypeMethods() {
		TEST.test("numberPrototypeMethods", """
			console.info((255).toString(16))
			console.info((3.14159).toFixed(2))
			console.info((123.456).toPrecision(4))
			console.info((12345.6789).toExponential(2))
			""", """
			ff
			3.14
			123.5
			1.23e+4
			""");
	}

	@Test
	public void numberStatics() {
		TEST.test("numberStatics", """
			console.info(Number.isInteger(5))
			console.info(Number.isInteger(5.5))
			console.info(Number.isNaN(0 / 0))
			console.info(Number.isFinite(1 / 0))
			console.info(Number.isSafeInteger(Number.MAX_SAFE_INTEGER))
			console.info(Number.MAX_SAFE_INTEGER)
			""", """
			true
			false
			true
			false
			true
			9007199254740991
			""");
	}

	@Test
	public void numberCoercionAndValueOf() {
		TEST.test("numberCoercionAndValueOf", """
			console.info(Number("42") + 1)
			console.info(new Number(7).valueOf())
			console.info(String(new Number(7)))
			""", """
			43
			7
			7
			""");
	}

	@Test
	public void booleans() {
		TEST.test("booleans", """
			console.info(Boolean(0))
			console.info(Boolean(1))
			console.info(Boolean(""))
			console.info(new Boolean(false).toString())
			console.info(new Boolean(1).valueOf())
			""", """
			false
			true
			false
			false
			true
			""");
	}
}
