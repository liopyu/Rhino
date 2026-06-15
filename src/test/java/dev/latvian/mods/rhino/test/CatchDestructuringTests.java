package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

/**
 * Coverage for catch-clause destructuring patterns (Mozilla/rhino@9e3d6c01f).
 * Supports both {@code catch ({message})} and {@code catch ([a, b])}.
 */
@SuppressWarnings("unused")
public class CatchDestructuringTests {
	public static final RhinoTest TEST = new RhinoTest("catchDestructuring");

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
}
