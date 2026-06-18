package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

public class ExponentiationOperatorTest {
	public static final RhinoTest TEST = new RhinoTest("exponentiationOperator");

	@Test
	public void basic() {
		TEST.test("basic", "console.info(2 ** 10);", "1024");
	}

	@Test
	public void rightAssociative() {
		TEST.test("rightAssociative", "console.info(2 ** 3 ** 2);", "512");
	}

	@Test
	public void compoundAssign() {
		TEST.test("compoundAssign", """
			let x = 2;
			x **= 10;
			console.info(x);
			""", "1024");
	}

	@Test
	public void compoundAssignRightAssociativeRhs() {
		TEST.test("compoundAssignRightAssociativeRhs", """
			let x = 2;
			x **= 3 ** 2;
			console.info(x);
			""", "512");
	}
}
