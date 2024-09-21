package dev.latvian.mods.rhino.test;
import org.junit.jupiter.api.Test;
@SuppressWarnings("unused")
public class DefaultParamsTest {
    public static final RhinoTest TEST = new RhinoTest("defParam");
    @Test public void simple() {
        TEST.test("simple", "function f(a = 7) { return a; }\nconsole.info(f());\nconsole.info(f(99));\nconsole.info(f(undefined));", "7\n99\n7");
    }
    @Test public void multiple() {
        TEST.test("multiple", "function f(a = 1, b = 2, c) { return a + ',' + b + ',' + c; }\nconsole.info(f());\nconsole.info(f(9));\nconsole.info(f(9, 8, 7));", "1,2,undefined\n9,2,undefined\n9,8,7");
    }
    @Test public void laterRefsEarlier() {
        TEST.test("laterRefsEarlier", "function f(a = 10, b = a * 2) { return a + ',' + b; }\nconsole.info(f());\nconsole.info(f(5));", "10,20\n5,10");
    }
    @Test public void arrow() {
        TEST.test("arrow", "let f = (a = 5) => a;\nconsole.info(f());\nconsole.info(f(99));", "5\n99");
    }
    @Test public void mixedWithDestructure() {
        TEST.test("mixedWithDestructure", "function f(a = 1, {b = 2} = {}) { return a + ',' + b; }\nconsole.info(f());\nconsole.info(f(9, {b: 8}));", "1,2\n9,8");
    }
}
