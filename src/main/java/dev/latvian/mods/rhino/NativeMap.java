/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.List;
import java.util.Map;

public class NativeMap extends ScriptableObject {
	private static final String CLASS_NAME = "Map";
	static final String ITERATOR_TAG = "Map Iterator";

	private final Hashtable entries;
	private boolean instanceOfMap = false;

	public NativeMap(Context cx) {
		entries = new Hashtable(cx);
	}

	static void init(Context cx, Scriptable scope, boolean sealed) {
		LambdaConstructor constructor = new LambdaConstructor(cx, scope, CLASS_NAME, 0, LambdaConstructor.CONSTRUCTOR_NEW, NativeMap::jsConstructor);
		constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

		constructor.defineConstructorMethod(cx, scope, "groupBy", 2, NativeMap::jsGroupBy, DONTENUM);

		constructor.definePrototypeMethod(cx, scope, "set", 2, (lcx, lscope, thisObj, args) -> realThis(thisObj, "set", lcx).js_set(lcx, key(args), args.length > 1 ? args[1] : Undefined.INSTANCE), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "delete", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "delete", lcx).js_delete(lcx, key(args)), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "get", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "get", lcx).js_get(lcx, key(args)), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "has", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "has", lcx).js_has(lcx, key(args)), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "clear", 0, (lcx, lscope, thisObj, args) -> realThis(thisObj, "clear", lcx).js_clear(lcx), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "keys", 0, (lcx, lscope, thisObj, args) -> realThis(thisObj, "keys", lcx).js_iterator(lscope, NativeCollectionIterator.Type.KEYS, lcx), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "values", 0, (lcx, lscope, thisObj, args) -> realThis(thisObj, "values", lcx).js_iterator(lscope, NativeCollectionIterator.Type.VALUES, lcx), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "forEach", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "forEach", lcx).js_forEach(lcx, lscope, args.length > 0 ? args[0] : Undefined.INSTANCE, args.length > 1 ? args[1] : Undefined.INSTANCE), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "entries", 0, (lcx, lscope, thisObj, args) -> realThis(thisObj, "entries", lcx).js_iterator(lscope, NativeCollectionIterator.Type.BOTH, lcx), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeAlias(cx, "entries", SymbolKey.ITERATOR, DONTENUM);

		// The spec requires very specific handling of the "size" prototype
		// property that's not like other things that we already do.
		constructor.definePrototypeProperty(cx, "size", thisObj -> realThis(thisObj, "size", cx).js_getSize(), DONTENUM);

		constructor.definePrototypeProperty(cx, SymbolKey.TO_STRING_TAG, CLASS_NAME, DONTENUM | READONLY);

		ScriptRuntimeES6.addSymbolSpecies(cx, scope, constructor);
		ScriptableObject.defineProperty(scope, CLASS_NAME, constructor, DONTENUM, cx);

		if (sealed) {
			constructor.sealObject(cx);
		}
	}

	static Object key(Object[] args) {
		return args.length > 0 ? args[0] : Undefined.INSTANCE;
	}

	private static Scriptable jsConstructor(Context cx, Scriptable scope, Object[] args) {
		NativeMap nm = new NativeMap(cx);
		nm.instanceOfMap = true;
		if (args.length > 0) {
			loadFromIterable(cx, scope, nm, key(args));
		}
		return nm;
	}

	private static Object jsGroupBy(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object items = args.length < 1 ? Undefined.INSTANCE : args[0];
		Object callback = args.length < 2 ? Undefined.INSTANCE : args[1];

		Map<Object, List<Object>> groups = AbstractEcmaObjectOperations.groupBy(cx, scope, CLASS_NAME, "groupBy", items, callback, AbstractEcmaObjectOperations.KEY_COERCION.COLLECTION);

		NativeMap map = (NativeMap) cx.newObject(scope, "Map");

		for (Map.Entry<Object, List<Object>> entry : groups.entrySet()) {
			Scriptable elements = cx.newArray(scope, entry.getValue().toArray());
			map.entries.put(cx, entry.getKey(), elements);
		}

		return map;
	}

	/**
	 * If an "iterable" object was passed to the constructor, there are many many things
	 * to do... Make this static because NativeWeakMap has the exact same requirement.
	 */
	static void loadFromIterable(Context cx, Scriptable scope, ScriptableObject map, Object arg1) {
		if ((arg1 == null) || Undefined.INSTANCE.equals(arg1)) {
			return;
		}

		// Call the "[Symbol.iterator]" property as a function.
		final Object ito = ScriptRuntime.callIterator(arg1, cx, scope);
		if (Undefined.INSTANCE.equals(ito)) {
			// Per spec, ignore if the iterator is undefined
			return;
		}

		// Find the "add" function of our own prototype, since it might have
		// been replaced. Since we're not fully constructed yet, create a dummy instance
		// so that we can get our own prototype.
		ScriptableObject dummy = ensureScriptableObject(cx.newObject(scope, map.getClassName()), cx);
		final Callable set = ScriptRuntime.getPropFunctionAndThis(cx, scope, dummy.getPrototype(cx), "set");
		cx.lastStoredScriptable();

		// Finally, run through all the iterated values and add them!
		try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, ito)) {
			for (Object val : it) {
				Scriptable sVal = ensureScriptable(val, cx);
				if (sVal instanceof Symbol) {
					throw ScriptRuntime.typeError1(cx, "msg.arg.not.object", ScriptRuntime.typeof(cx, sVal));
				}
				Object finalKey = sVal.get(cx, 0, sVal);
				if (finalKey == NOT_FOUND) {
					finalKey = Undefined.INSTANCE;
				}
				Object finalVal = sVal.get(cx, 1, sVal);
				if (finalVal == NOT_FOUND) {
					finalVal = Undefined.INSTANCE;
				}
				set.call(cx, scope, map, new Object[]{finalKey, finalVal});
			}
		}
	}

	private static NativeMap realThis(Scriptable thisObj, String name, Context cx) {
		NativeMap nm = LambdaConstructor.convertThisObject(cx, thisObj, NativeMap.class);
		if (!nm.instanceOfMap) {
			// Check for "Map internal data tag"
			throw ScriptRuntime.typeError1(cx, "msg.incompat.call", name);
		}
		return nm;
	}

	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	private Object js_set(Context cx, Object k, Object v) {
		// Special handling of "negative zero" from the spec.
		Object key = k;
		if ((key instanceof Number) && ((Number) key).doubleValue() == ScriptRuntime.negativeZero) {
			key = ScriptRuntime.zeroObj;
		}
		entries.put(cx, key, v);
		return this;
	}

	private Object js_delete(Context cx, Object arg) {
		return entries.deleteEntry(cx, arg);
	}

	private Object js_get(Context cx, Object arg) {
		final Hashtable.Entry entry = entries.getEntry(cx, arg);
		if (entry == null) {
			return Undefined.INSTANCE;
		}
		return entry.value;
	}

	private Object js_has(Context cx, Object arg) {
		return entries.has(cx, arg);
	}

	private Object js_getSize() {
		return entries.size();
	}

	private Object js_iterator(Scriptable scope, NativeCollectionIterator.Type type, Context cx) {
		return new NativeCollectionIterator(scope, ITERATOR_TAG, type, entries.iterator(), cx);
	}

	private Object js_clear(Context cx) {
		entries.clear(cx);
		return Undefined.INSTANCE;
	}

	private Object js_forEach(Context cx, Scriptable scope, Object arg1, Object arg2) {
		if (!(arg1 instanceof final Callable f)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", arg1, ScriptRuntime.typeof(cx, arg1));
		}

		boolean isStrict = cx.isStrictMode();

		for (var entry : entries) {
			// Per spec must convert every time so that primitives are always regenerated...
			Scriptable thisObj = ScriptRuntime.toObjectOrNull(cx, arg2, scope);

			if (thisObj == null && !isStrict) {
				thisObj = scope;
			}
			if (thisObj == null) {
				thisObj = Undefined.SCRIPTABLE_INSTANCE;
			}

			f.call(cx, scope, thisObj, new Object[]{entry.value, entry.key, this});
		}
		return Undefined.INSTANCE;
	}
}
