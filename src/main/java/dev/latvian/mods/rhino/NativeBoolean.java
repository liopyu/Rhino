/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.DefaultValueTypeHint;

/**
 * This class implements the Boolean native object.
 * See ECMA 15.6.
 *
 * @author Norris Boyd
 */
final class NativeBoolean extends ScriptableObject {
	private static final String CLASS_NAME = "Boolean";

	private final boolean booleanValue;

	static void init(Scriptable scope, boolean sealed, Context cx) {
		LambdaConstructor constructor = new LambdaConstructor(cx, scope, CLASS_NAME, 1, NativeBoolean::js_constructorFunc, NativeBoolean::js_constructor);
		constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
		// Boolean is an unusual object in that the prototype is itself a Boolean
		constructor.setPrototypeScriptable(new NativeBoolean(false), cx);

		constructor.definePrototypeMethod(cx, scope, "toString", 0, NativeBoolean::js_toString, DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "toSource", 0, NativeBoolean::js_toSource, DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "valueOf", 0, NativeBoolean::js_valueOf, DONTENUM, DONTENUM | READONLY);

		ScriptableObject.defineProperty(scope, CLASS_NAME, constructor, DONTENUM, cx);
		if (sealed) {
			constructor.sealObject(cx);
		}
	}

	NativeBoolean(boolean b) {
		booleanValue = b;
	}

	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	@Override
	public Object getDefaultValue(Context cx, DefaultValueTypeHint typeHint) {
		// This is actually non-ECMA, but will be proposed
		// as a change in round 2.
		if (typeHint == DefaultValueTypeHint.BOOLEAN) {
			return booleanValue;
		}
		return super.getDefaultValue(cx, typeHint);
	}

	private static boolean toValue(Context cx, Scriptable thisObj) {
		return LambdaConstructor.convertThisObject(cx, thisObj, NativeBoolean.class).booleanValue;
	}

	private static Object js_constructorFunc(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		// ScriptRuntime.toBoolean handles the document.all "avoidObjectDetection" case internally.
		return ScriptRuntime.toBoolean(cx, args.length > 0 ? args[0] : Undefined.INSTANCE);
	}

	private static NativeBoolean js_constructor(Context cx, Scriptable scope, Object[] args) {
		boolean b = ScriptRuntime.toBoolean(cx, args.length > 0 ? args[0] : Undefined.INSTANCE);
		return new NativeBoolean(b);
	}

	private static String js_toString(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return toValue(cx, thisObj) ? "true" : "false";
	}

	private static Object js_valueOf(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return toValue(cx, thisObj);
	}

	private static Object js_toSource(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return "(new Boolean(" + ScriptRuntime.toString(cx, toValue(cx, thisObj)) + "))";
	}
}
