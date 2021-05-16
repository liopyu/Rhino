package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

/**
 * Coverage for catch-clause tests, namely destructuring
 * (`catch ({message})` and `catch ([a,b,c])`)
 * as well as catch without binding.
 */
@SuppressWarnings("unused")
public class CatchClauseTests {
	public static final RhinoTest TEST = new RhinoTest("catchClauses");

	@Test
	public void objectDestructure() {
		TEST.test("objectDestructure", """
			try {
				throw new Error("boom");
			} catch ({message}) {
				console.info(message);
			}
			""", """
			boom
			""");
	}

	@Test
	public void arrayDestructure() {
		TEST.test("arrayDestructure", """
			try {
				throw [10, 20, 30];
			} catch ([a, b, c]) {
				console.info(a + ',' + b + ',' + c);
			}
			""", """
			10,20,30
			""");
	}

	@Test
	public void nestedObjectDestructure() {
		TEST.test("nestedObjectDestructure", """
			try {
				let err = {data: {code: 42, msg: "hi"}};
				throw err;
			} catch ({data: {code, msg}}) {
				console.info(code + ':' + msg);
			}
			""", """
			42:hi
			""");
	}

	@Test
	public void renamedObjectDestructure() {
		TEST.test("renamedObjectDestructure", """
			try {
				throw {message: "fail"};
			} catch ({message: m}) {
				console.info("got: " + m);
			}
			""", """
			got: fail
			""");
	}

	@Test
	public void simpleIdentifierStillWorks() {
		TEST.test("simpleIdentifierStillWorks", """
			try {
				throw new Error("plain");
			} catch (e) {
				console.info(e.message);
			}
			""", """
			plain
			""");
	}

	@Test
	public void noBinding() {
		TEST.test("noBinding", "try { throw 'x'; } catch { console.info('caught') }", "caught");
	}

	@Test
	public void noBindingSafe() {
		TEST.test("noBindingSafe", "try { console.info('try') } catch { console.info('oops') }", "try");
	}
}
