/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

public abstract class ES6Iterator extends ScriptableObject {
	public static final String NEXT_METHOD = "next";
	public static final String DONE_PROPERTY = "done";
	public static final String RETURN_PROPERTY = "return";
	public static final String VALUE_PROPERTY = "value";
	public static final String RETURN_METHOD = "return";

	protected static void init(ScriptableObject scope, boolean sealed, ScriptableObject prototype, String tag, Context cx) {
		if (scope != null) {
			prototype.setParentScope(scope);
			prototype.setPrototype(getObjectPrototype(scope, cx));
		}

		// Define prototype methods using LambdaFunction
		LambdaFunction next = new LambdaFunction(cx, scope, NEXT_METHOD, 0, ES6Iterator::js_next, false);
		prototype.defineProperty(cx, NEXT_METHOD, next, DONTENUM);

		LambdaFunction iterator = new LambdaFunction(cx, scope, "[Symbol.iterator]", 1, ES6Iterator::js_iterator, false);
		prototype.defineProperty(cx, SymbolKey.ITERATOR, iterator, DONTENUM);

		prototype.defineProperty(cx, SymbolKey.TO_STRING_TAG, prototype.getClassName(), DONTENUM | READONLY);

		if (sealed) {
			prototype.sealObject(cx);
		}

		// Need to access Iterator prototype when constructing
		// Iterator instances, but don't have a iterator constructor
		// to use to find the prototype. Use the "associateValue"
		// approach instead.
		if (scope != null) {
			scope.associateValue(tag, prototype);
		}
	}

	// 25.1.1.3 The IteratorResult Interface
	static Scriptable makeIteratorResult(Context cx, Scriptable scope, Boolean done) {
		return makeIteratorResult(cx, scope, done, Undefined.INSTANCE);
	}

	static Scriptable makeIteratorResult(Context cx, Scriptable scope, Boolean done, Object value) {
		final Scriptable iteratorResult = cx.newObject(scope);
		ScriptableObject.putProperty(iteratorResult, VALUE_PROPERTY, value, cx);
		ScriptableObject.putProperty(iteratorResult, DONE_PROPERTY, done, cx);
		return iteratorResult;
	}

	protected boolean exhausted = false;
	private String tag;

	protected ES6Iterator() {
	}

	protected ES6Iterator(Scriptable scope, String tag, Context cx) {
		// Set parent and prototype properties. Since we don't have a
		// "Iterator" constructor in the top scope, we stash the
		// prototype in the top scope's associated value.
		this.tag = tag;
		Scriptable top = ScriptableObject.getTopLevelScope(scope);
		this.setParentScope(top);
		ScriptableObject prototype = (ScriptableObject) ScriptableObject.getTopScopeValue(top, tag, cx);
		setPrototype(prototype);
	}

	private static ES6Iterator realThis(Context cx, Scriptable thisObj) {
		return LambdaConstructor.convertThisObject(cx, thisObj, ES6Iterator.class);
	}

	private static Object js_next(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		ES6Iterator iterator = realThis(cx, thisObj);
		return iterator.next(cx, scope);
	}

	private static Object js_iterator(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return thisObj;
	}

	abstract protected boolean isDone(Context cx, Scriptable scope);

	abstract protected Object nextValue(Context cx, Scriptable scope);

	protected Object next(Context cx, Scriptable scope) {
		Object value = Undefined.INSTANCE;
		boolean done = isDone(cx, scope) || this.exhausted;
		if (!done) {
			value = nextValue(cx, scope);
		} else {
			this.exhausted = true;
		}
		return makeIteratorResult(cx, scope, done, value);
	}

	protected String getTag() {
		return tag;
	}
}
