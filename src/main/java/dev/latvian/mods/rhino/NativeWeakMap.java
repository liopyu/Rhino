/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.WeakHashMap;

/**
 * This is an implementation of the ES6 WeakMap class. As per the spec, keys must be
 * ordinary objects or unregistered symbols. Since there is no defined "equality" for
 * objects, comparisons are done strictly by object equality. Both ES6 and the
 * java.util.WeakHashMap class have the same basic structure -- entries are removed
 * automatically when the sole remaining reference to the key is a weak reference.
 * Therefore, we can use WeakHashMap as the basis of this implementation and preserve the
 * same semantics.
 */
public class NativeWeakMap extends ScriptableObject {
	private static final String CLASS_NAME = "WeakMap";
	private static final Object NULL_VALUE = new Object();

	private final transient WeakHashMap<Scriptable, Object> map = new WeakHashMap<>();
	private boolean instanceOfWeakMap = false;

	static void init(Context cx, Scriptable scope, boolean sealed) {
		LambdaConstructor constructor = new LambdaConstructor(cx, scope, CLASS_NAME, 0, LambdaConstructor.CONSTRUCTOR_NEW, NativeWeakMap::jsConstructor);
		constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

		constructor.definePrototypeMethod(cx, scope, "set", 2, (lcx, lscope, thisObj, args) -> realThis(thisObj, "set", lcx).js_set(lcx, NativeMap.key(args), args.length > 1 ? args[1] : Undefined.INSTANCE), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "delete", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "delete", lcx).js_delete(NativeMap.key(args)), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "get", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "get", lcx).js_get(NativeMap.key(args)), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "has", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "has", lcx).js_has(NativeMap.key(args)), DONTENUM, DONTENUM | READONLY);

		constructor.definePrototypeProperty(cx, SymbolKey.TO_STRING_TAG, CLASS_NAME, DONTENUM | READONLY);

		ScriptRuntimeES6.addSymbolSpecies(cx, scope, constructor);
		ScriptableObject.defineProperty(scope, CLASS_NAME, constructor, DONTENUM, cx);

		if (sealed) {
			constructor.sealObject(cx);
		}
	}

	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	private static Scriptable jsConstructor(Context cx, Scriptable scope, Object[] args) {
		NativeWeakMap nm = new NativeWeakMap();
		nm.instanceOfWeakMap = true;
		if (args.length > 0) {
			NativeMap.loadFromIterable(cx, scope, nm, NativeMap.key(args));
		}
		return nm;
	}

	private Object js_delete(Object key) {
		if (!isValidKey(key)) {
			return Boolean.FALSE;
		}
		return map.remove(key) != null;
	}

	private Object js_get(Object key) {
		if (!isValidKey(key)) {
			return Undefined.INSTANCE;
		}
		Object result = map.get(key);
		if (result == null) {
			return Undefined.INSTANCE;
		} else if (result == NULL_VALUE) {
			return null;
		}
		return result;
	}

	private Object js_has(Object key) {
		if (!isValidKey(key)) {
			return Boolean.FALSE;
		}
		return map.containsKey(key);
	}

	private Object js_set(Context cx, Object key, Object v) {
		// As the spec says, only a true "Object" (or an unregistered symbol) can be the key to a
		// WeakMap. Use the default object equality here. ScriptableObject does not override
		// equals or hashCode, which means that in effect we are only keying on object identity.
		// This is all correct according to the ECMAscript spec.
		if (!isValidKey(key)) {
			throw ScriptRuntime.typeError1(cx, "msg.arg.not.object", ScriptRuntime.typeof(cx, key));
		}
		// Map.get() does not distinguish between "not found" and a null value. So,
		// replace true null here with a marker so that we can re-convert in "get".
		final Object value = (v == null ? NULL_VALUE : v);
		map.put((Scriptable) key, value);
		return this;
	}

	private static boolean isValidKey(Object key) {
		return ScriptRuntime.isUnregisteredSymbol(key) || ScriptRuntime.isObject(key);
	}

	private static NativeWeakMap realThis(Scriptable thisObj, String name, Context cx) {
		NativeWeakMap nm = LambdaConstructor.convertThisObject(cx, thisObj, NativeWeakMap.class);
		if (!nm.instanceOfWeakMap) {
			// Check for "Map internal data tag"
			throw ScriptRuntime.typeError1(cx, "msg.incompat.call", name);
		}
		return nm;
	}
}
