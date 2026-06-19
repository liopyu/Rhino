package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Covers __lookupGetter__/__lookupSetter__ when the registered getter/setter
 * is a reflection-based {@code MemberBox} (via {@link ScriptableObject#defineProperty(Context, String, Class, int)})
 * rather than a plain JS function, matching Mozilla/rhino@a3ded2cc2.
 */
public class LookupGetterSetterTests {
	public static class Delegate {
		public static Object getX(Scriptable obj) {
			return "hello";
		}

		public static void setX(Scriptable obj, Object value) {
			// no-op setter for the lookup test
		}
	}

	private static ScriptableObject newFooWithMemberBoxAccessors(Context cx, Scriptable scope) {
		ScriptableObject foo = (ScriptableObject) cx.newObject(scope);
		foo.defineProperty(cx, "x", Delegate.class, 0);
		ScriptableObject.putProperty(scope, "foo", foo, cx);
		return foo;
	}

	@Test
	public void lookupGetterReturnsCallableFunction() {
		var factory = new TestContextFactory();
		var cx = factory.enter();
		var scope = cx.initStandardObjects();
		newFooWithMemberBoxAccessors(cx, scope);

		Object result = cx.evaluateString(scope, "typeof foo.__lookupGetter__('x')", "test", 1, null);
		Assertions.assertEquals("function", result);
	}

	@Test
	public void lookupSetterReturnsCallableFunction() {
		var factory = new TestContextFactory();
		var cx = factory.enter();
		var scope = cx.initStandardObjects();
		newFooWithMemberBoxAccessors(cx, scope);

		Object result = cx.evaluateString(scope, "typeof foo.__lookupSetter__('x')", "test", 1, null);
		Assertions.assertEquals("function", result);
	}

	@Test
	public void lookedUpGetterCanBeInvoked() {
		var factory = new TestContextFactory();
		var cx = factory.enter();
		var scope = cx.initStandardObjects();
		newFooWithMemberBoxAccessors(cx, scope);

		Object result = cx.evaluateString(scope, "foo.__lookupGetter__('x').call(foo)", "test", 1, null);
		Assertions.assertEquals("hello", result);
	}
}
