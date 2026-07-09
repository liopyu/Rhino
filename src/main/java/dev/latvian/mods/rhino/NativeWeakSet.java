/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.WeakHashMap;

/**
 * This is an implementation of the ES6 WeakSet class. It is very similar to
 * NativeWeakMap, with the exception being that it doesn't store any values.
 * Java will GC the key only when there is no longer any reference to it other
 * than the weak reference. That means that it is important that the "value"
 * that we put in the WeakHashMap here is not one that contains the key.
 */
public class NativeWeakSet extends ScriptableObject {
	private static final String CLASS_NAME = "WeakSet";

	private final transient WeakHashMap<Scriptable, Boolean> map = new WeakHashMap<>();
	private boolean instanceOfWeakSet = false;

	static void init(Context cx, Scriptable scope, boolean sealed) {
		LambdaConstructor constructor = new LambdaConstructor(cx, scope, CLASS_NAME, 0, LambdaConstructor.CONSTRUCTOR_NEW, NativeWeakSet::jsConstructor);
		constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

		constructor.definePrototypeMethod(cx, scope, "add", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "add", lcx).js_add(lcx, NativeMap.key(args)), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "delete", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "delete", lcx).js_delete(NativeMap.key(args)), DONTENUM, DONTENUM | READONLY);
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
		NativeWeakSet ns = new NativeWeakSet();
		ns.instanceOfWeakSet = true;
		if (args.length > 0) {
			NativeSet.loadFromIterable(cx, scope, ns, NativeMap.key(args));
		}
		return ns;
	}

	private Object js_add(Context cx, Object key) {
		// As the spec says, only a true "Object" (or an unregistered symbol) can be the key to a
		// WeakSet. Use the default object equality here. ScriptableObject does not override
		// equals or hashCode, which means that in effect we are only keying on object identity.
		// This is all correct according to the ECMAscript spec.
		if (!isValidValue(key)) {
			throw ScriptRuntime.typeError1(cx, "msg.arg.not.object", ScriptRuntime.typeof(cx, key));
		}
		// Add a value to the map, but don't make it the key -- otherwise the WeakHashMap
		// will never GC anything.
		map.put((Scriptable) key, Boolean.TRUE);
		return this;
	}

	private Object js_delete(Object key) {
		if (!isValidValue(key)) {
			return Boolean.FALSE;
		}
		return map.remove(key) != null;
	}

	private Object js_has(Object key) {
		if (!isValidValue(key)) {
			return Boolean.FALSE;
		}
		return map.containsKey(key);
	}

	private static boolean isValidValue(Object v) {
		return ScriptRuntime.isUnregisteredSymbol(v) || ScriptRuntime.isObject(v);
	}

	private static NativeWeakSet realThis(Scriptable thisObj, String name, Context cx) {
		NativeWeakSet ns = LambdaConstructor.convertThisObject(cx, thisObj, NativeWeakSet.class);
		if (!ns.instanceOfWeakSet) {
			// Check for "Set internal data tag"
			throw ScriptRuntime.typeError1(cx, "msg.incompat.call", name);
		}
		return ns;
	}
}
