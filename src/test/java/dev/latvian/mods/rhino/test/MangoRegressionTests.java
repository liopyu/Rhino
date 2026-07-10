package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
public class MangoRegressionTests {
	public static final RhinoTest TEST = new RhinoTest("mangosTesting");

	@Test
	public void computedPropKeyFromVariable() {
		TEST.test("computedPropKeyFromVariable", """
			let key = "mango_test";
			let obj = { [key]: "value" };
			console.info(obj.mango_test);
			""", "value");
	}

	@Test
	public void shorthandPropWithDestructuring() {
		TEST.test("shorthandPropWithDestructuring", """
			(() => {
			  let key = "mango_test";
			  let obj = { key };
			  console.info(obj.key);
			  let { key: value } = obj;
			  console.info(value);
			})();
			""", """
			mango_test
			mango_test
			""");
	}

	@Test
	public void computedPropKeyFromStringLiteral() {
		TEST.test("computedPropKeyFromStringLiteral", """
			(() => {
			  let obj = { ["key"]: "value" };
			  console.info(obj.key);
			})();
			""", "value");
	}

	@Test
	public void defaultParamReferringToEarlierParam() {
		TEST.test("defaultParamReferringToEarlierParam", """
			(() => {
			  let results = [];
			  function f(a, b = a * 2) {
			    results.push(a);
			    results.push(b);
			  }
			  f(5);
			  f(5, 10);
			  f("5");
			  console.info(results.join(","));
			})();
			""", "5,10,5,10,5,10");
	}

	@Test
	public void restParameters() {
		TEST.test("restParameters", """
			(() => {
			  function f(...args) {
			    console.info(`${args}`);
			  }
			  f(5);
			  f(5, 6);
			  f(5, 6, 7);
			})();
			""", """
			[5]
			[5, 6]
			[5, 6, 7]
			""");
	}

	@Test
	public void optionalChainingAndNullishCoalescing() {
		TEST.test("optionalChainingAndNullishCoalescing", """
			(() => {
			  let obj = { a() { console.info("1"); } };
			  obj?.a?.();
			  obj?.b?.();
			  console.info(obj.b ?? "default");
			})();
			""", """
			1
			default
			""");
	}

	@Test
	public void functionApplyFallback() {
		TEST.test("functionApplyFallback", """
			(() => {
			  let arr = [1, 2, 3];
			  function fa(a, b, c) {
			    console.info(`${a} ${b} ${c}`);
			  }
			  fa.apply(null, arr);
			})();
			""", "1 2 3");
	}


	@Test
	public void computedPropKeyFromExpression() {
		TEST.test("computedPropKeyFromExpression", """
			(() => {
			  let obj = { ["a" + "b"]: 1 };
			  console.info(obj.ab);
			})();
			""", "1");
	}


}