package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

/**
 * Parse-time check that destructuring declarations inside ordinary for-loops
 * have initializers (Mozilla/rhino@425cad9ce). The empty-pattern form used
 * to fail at codegen with cryptic errors; now it errors at parse time.
 */
@SuppressWarnings("unused")
public class ForLoopDestructuringErrorTests {
	public static final RhinoTest TEST = new RhinoTest("forLoopDestructuringErrors");

	@Test
	public void destructuringWithoutInitializerRaisesSyntaxError() {
		// for (var {};;) {} should be a parse error, use eval to check that
		TEST.test("destructuringWithoutInitializerRaisesSyntaxError", """
			try {
				eval("for (var {};;) {}");
			} catch (e) {
				console.info(e.name);
			}
			""", """
			SyntaxError
			""");
	}

	@Test
	public void destructuringWithInitializerStillWorks() {
		TEST.test("destructuringWithInitializerStillWorks", """
			let count = 0;
			for (let {a} = {a: 5}; count < 1; count++) {
				console.info(a);
			}
			""", """
			5
			""");
	}
}
