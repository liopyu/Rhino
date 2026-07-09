/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;

import java.util.Iterator;

/**
 * This class implements iterator objects. See
 * http://developer.mozilla.org/en/docs/New_in_JavaScript_1.7#Iterators
 *
 * @author Norris Boyd
 */
public final class NativeIterator extends ScriptableObject {
	public static final String ITERATOR_PROPERTY_NAME = "__iterator__";
	private static final Object ITERATOR_TAG = "Iterator";
	private static final String STOP_ITERATION = "StopIteration";
	private static final String CLASS_NAME = "Iterator";

	public static class StopIteration extends NativeObject {
		private Object value = Undefined.INSTANCE;

		public StopIteration(Context cx) {
			super(cx.factory);
		}

		public StopIteration(Context cx, Object val) {
			this(cx);
			this.value = val;
		}

		public Object getValue() {
			return value;
		}

		@Override
		public String getClassName() {
			return STOP_ITERATION;
		}

		/* StopIteration has custom instanceof behavior since it
		 * doesn't have a constructor.
		 */
		@Override
		public boolean hasInstance(Context cx, Scriptable instance) {
			return instance instanceof StopIteration;
		}
	}

	static public class WrappedJavaIterator {
		private final Context localContext;
		private final Iterator<?> iterator;
		private final Scriptable scope;

		WrappedJavaIterator(Context cx, Iterator<?> iterator, Scriptable scope) {
			this.localContext = cx;
			this.iterator = iterator;
			this.scope = scope;
		}

		public Object next() {
			if (!iterator.hasNext()) {
				// Out of values. Throw StopIteration.
				throw new JavaScriptException(localContext, NativeIterator.getStopIterationObject(scope, localContext), null, 0);
			}
			return iterator.next();
		}

		public Object __iterator__(boolean b) {
			return this;
		}
	}

	static void init(Context cx, ScriptableObject scope, boolean sealed) {
		LambdaConstructor constructor = new LambdaConstructor(cx, scope, CLASS_NAME, 2, NativeIterator::jsConstructorCall, NativeIterator::jsConstructor);
		constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

		NativeIterator proto = new NativeIterator();
		constructor.setPrototypeScriptable(proto, cx);

		constructor.definePrototypeMethod(cx, scope, "next", 0, NativeIterator::js_next);
		constructor.definePrototypeMethod(cx, scope, ITERATOR_PROPERTY_NAME, 1, NativeIterator::js_iteratorMethod);

		ScriptableObject.defineProperty(scope, CLASS_NAME, constructor, DONTENUM, cx);
		if (sealed) {
			constructor.sealObject(cx);
			((ScriptableObject) constructor.getPrototypeProperty(cx)).sealObject(cx);
		}

		// Generator
		ES6Generator.init(scope, sealed, cx);

		// StopIteration
		NativeObject obj = new StopIteration(cx);
		obj.setPrototype(getObjectPrototype(scope, cx));
		obj.setParentScope(scope);
		if (sealed) {
			obj.sealObject(cx);
		}
		defineProperty(scope, STOP_ITERATION, obj, DONTENUM, cx);
		// Use "associateValue" so that generators can continue to
		// throw StopIteration even if the property of the global
		// scope is replaced or deleted.
		scope.associateValue(ITERATOR_TAG, obj);
	}

	/**
	 * Get the value of the "StopIteration" object. Note that this value
	 * is stored in the top-level scope using "associateValue" so the
	 * value can still be found even if a script overwrites or deletes
	 * the global "StopIteration" property.
	 *
	 * @param scope a scope whose parent chain reaches a top-level scope
	 * @return the StopIteration object
	 */
	public static Object getStopIterationObject(Scriptable scope, Context cx) {
		Scriptable top = getTopLevelScope(scope);
		return getTopScopeValue(top, ITERATOR_TAG, cx);
	}

	/**
	 * Called as a function ({@code Iterator(x)}): convert to an iterator if possible
	 * (wrap a Java iterator, invoke {@code __iterator__}), otherwise fall back to
	 * property enumeration.
	 */
	private static Object jsConstructorCall(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable target = requireIteratorTarget(cx, scope, args);
		boolean keyOnly = isKeyOnly(cx, args);

		Iterator<?> iterator = getJavaIterator(target);
		if (iterator != null) {
			Scriptable topScope = getTopLevelScope(scope);
			return cx.wrap(topScope, new WrappedJavaIterator(cx, iterator, topScope), TypeInfo.of(WrappedJavaIterator.class));
		}

		Scriptable jsIterator = ScriptRuntime.toIterator(cx, scope, target, keyOnly);
		if (jsIterator != null) {
			return jsIterator;
		}

		return createNativeIterator(cx, scope, target, keyOnly);
	}

	/**
	 * Invoked with {@code new} ({@code new Iterator(x)}): always set up property enumeration.
	 */
	private static Scriptable jsConstructor(Context cx, Scriptable scope, Object[] args) {
		Scriptable target = requireIteratorTarget(cx, scope, args);
		boolean keyOnly = isKeyOnly(cx, args);
		return createNativeIterator(cx, scope, target, keyOnly);
	}

	private static Scriptable requireIteratorTarget(Context cx, Scriptable scope, Object[] args) {
		if (args.length == 0 || args[0] == null || args[0] == Undefined.INSTANCE) {
			Object argument = args.length == 0 ? Undefined.INSTANCE : args[0];
			throw ScriptRuntime.typeError1(cx, "msg.no.properties", ScriptRuntime.toString(cx, argument));
		}
		return ScriptRuntime.toObject(cx, scope, args[0]);
	}

	private static boolean isKeyOnly(Context cx, Object[] args) {
		return args.length > 1 && ScriptRuntime.toBoolean(cx, args[1]);
	}

	private static Scriptable createNativeIterator(Context cx, Scriptable scope, Scriptable target, boolean keyOnly) {
		// Do not call __iterator__ method.
		IdEnumeration objectIterator = ScriptRuntime.enumInit(cx, scope, target, keyOnly ? ScriptRuntime.ENUMERATE_KEYS_NO_ITERATOR : ScriptRuntime.ENUMERATE_ARRAY_NO_ITERATOR);
		objectIterator.enumNumbers = true;
		NativeIterator result = new NativeIterator(objectIterator);
		result.setPrototype(getClassPrototype(scope, result.getClassName(), cx));
		result.setParentScope(scope);
		return result;
	}

	/**
	 * If "obj" is a java.util.Iterator or a java.lang.Iterable, return a
	 * wrapping as a JavaScript Iterator. Otherwise, return null.
	 * This method is in VMBridge since Iterable is a JDK 1.5 addition.
	 */
	static private Iterator<?> getJavaIterator(Object obj) {
		if (obj instanceof Wrapper) {
			Object unwrapped = ((Wrapper) obj).unwrap();
			Iterator<?> iterator = null;
			if (unwrapped instanceof Iterator) {
				iterator = (Iterator<?>) unwrapped;
			}
			if (unwrapped instanceof Iterable) {
				iterator = ((Iterable<?>) unwrapped).iterator();
			}
			return iterator;
		}
		return null;
	}

	private IdEnumeration objectIterator;

	/**
	 * Only for constructing the prototype object.
	 */
	private NativeIterator() {
	}

	private NativeIterator(IdEnumeration objectIterator) {
		this.objectIterator = objectIterator;
	}

	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	private static NativeIterator realThis(Context cx, Scriptable thisObj) {
		return LambdaConstructor.convertThisObject(cx, thisObj, NativeIterator.class);
	}

	private static Object js_next(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		NativeIterator iterator = realThis(cx, thisObj);
		return iterator.objectIterator.nextExec(cx, scope);
	}

	private static Object js_iteratorMethod(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return realThis(cx, thisObj); // XXX: what about argument? SpiderMonkey apparently ignores it
	}
}
