/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.Iterator;

public class NativeSet extends ScriptableObject {
	private static final String CLASS_NAME = "Set";
	static final String ITERATOR_TAG = "Set Iterator";
	static final SymbolKey GETSIZE = new SymbolKey("[Symbol.getSize]");

	private final Hashtable entries;
	private boolean instanceOfSet = false;

	public NativeSet(Context cx) {
		entries = new Hashtable(cx);
	}

	static void init(Context cx, Scriptable scope, boolean sealed) {
		LambdaConstructor constructor = new LambdaConstructor(cx, scope, CLASS_NAME, 0, LambdaConstructor.CONSTRUCTOR_NEW, NativeSet::jsConstructor);
		constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

		constructor.definePrototypeMethod(cx, scope, "add", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "add", lcx).js_add(lcx, key(args)), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "delete", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "delete", lcx).js_delete(lcx, key(args)), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "has", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "has", lcx).js_has(lcx, key(args)), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "clear", 0, (lcx, lscope, thisObj, args) -> realThis(thisObj, "clear", lcx).js_clear(lcx), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "values", 0, (lcx, lscope, thisObj, args) -> realThis(thisObj, "values", lcx).js_iterator(lscope, NativeCollectionIterator.Type.VALUES, lcx), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeAlias(cx, "values", "keys", DONTENUM | READONLY);
		constructor.definePrototypeAlias(cx, "values", SymbolKey.ITERATOR, DONTENUM);
		constructor.definePrototypeMethod(cx, scope, "entries", 0, (lcx, lscope, thisObj, args) -> realThis(thisObj, "entries", lcx).js_iterator(lscope, NativeCollectionIterator.Type.BOTH, lcx), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "forEach", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "forEach", lcx).js_forEach(lcx, lscope, key(args), args.length > 1 ? args[1] : Undefined.INSTANCE), DONTENUM, DONTENUM | READONLY);

		// ES2025 Set methods
		constructor.definePrototypeMethod(cx, scope, "intersection", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "intersection", lcx).js_intersection(lcx, lscope, args), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "union", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "union", lcx).js_union(lcx, lscope, args), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "difference", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "difference", lcx).js_difference(lcx, lscope, args), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "symmetricDifference", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "symmetricDifference", lcx).js_symmetricDifference(lcx, lscope, args), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "isSubsetOf", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "isSubsetOf", lcx).js_isSubsetOf(lcx, lscope, args), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "isSupersetOf", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "isSupersetOf", lcx).js_isSupersetOf(lcx, lscope, args), DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "isDisjointFrom", 1, (lcx, lscope, thisObj, args) -> realThis(thisObj, "isDisjointFrom", lcx).js_isDisjointFrom(lcx, lscope, args), DONTENUM, DONTENUM | READONLY);

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

	private static Object key(Object[] args) {
		return args.length > 0 ? args[0] : Undefined.INSTANCE;
	}

	private static Scriptable jsConstructor(Context cx, Scriptable scope, Object[] args) {
		NativeSet ns = new NativeSet(cx);
		ns.instanceOfSet = true;
		if (args.length > 0) {
			loadFromIterable(cx, scope, ns, key(args));
		}
		return ns;
	}

	/**
	 * If an "iterable" object was passed to the constructor, there are many many things
	 * to do. This is common code with NativeWeakSet.
	 */
	static void loadFromIterable(Context cx, Scriptable scope, ScriptableObject set, Object arg1) {
		if ((arg1 == null) || Undefined.INSTANCE.equals(arg1)) {
			return;
		}

		// Call the "[Symbol.iterator]" property as a function.
		Object ito = ScriptRuntime.callIterator(arg1, cx, scope);
		if (Undefined.INSTANCE.equals(ito)) {
			// Per spec, ignore if the iterator returns undefined
			return;
		}

		// Find the "add" function of our own prototype, since it might have
		// been replaced. Since we're not fully constructed yet, create a dummy instance
		// so that we can get our own prototype.
		ScriptableObject dummy = ensureScriptableObject(cx.newObject(scope, set.getClassName()), cx);
		final Callable add = ScriptRuntime.getPropFunctionAndThis(cx, scope, dummy.getPrototype(cx), "add");
		// Clean up the value left around by the previous function
		cx.lastStoredScriptable();

		// Finally, run through all the iterated values and add them!
		try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, ito)) {
			for (Object val : it) {
				final Object finalVal = val == NOT_FOUND ? Undefined.INSTANCE : val;
				add.call(cx, scope, set, new Object[]{finalVal});
			}
		}
	}

	private static NativeSet realThis(Scriptable thisObj, String name, Context cx) {
		NativeSet ns = LambdaConstructor.convertThisObject(cx, thisObj, NativeSet.class);
		if (!ns.instanceOfSet) {
			// If we get here, then this object doesn't have the "Set internal data slot."
			throw ScriptRuntime.typeError1(cx, "msg.incompat.call", name);
		}
		return ns;
	}

	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	private Object js_add(Context cx, Object k) {
		// Special handling of "negative zero" from the spec.
		Object key = k;
		if ((key instanceof Number) && ((Number) key).doubleValue() == ScriptRuntime.negativeZero) {
			key = ScriptRuntime.zeroObj;
		}
		entries.put(cx, key, key);
		return this;
	}

	private Object js_delete(Context cx, Object arg) {
		return entries.deleteEntry(cx, arg);
	}

	private Object js_has(Context cx, Object arg) {
		// Special handling of "negative zero" from the spec.
		if ((arg instanceof Number) && ((Number) arg).doubleValue() == ScriptRuntime.negativeZero) {
			return entries.has(cx, ScriptRuntime.zeroObj);
		}
		return entries.has(cx, arg);
	}

	private Object js_clear(Context cx) {
		entries.clear(cx);
		return Undefined.INSTANCE;
	}

	private Object js_getSize() {
		return entries.size();
	}

	private Object js_iterator(Scriptable scope, NativeCollectionIterator.Type type, Context cx) {
		return new NativeCollectionIterator(scope, ITERATOR_TAG, type, entries.iterator(), cx);
	}

	private Object js_forEach(Context cx, Scriptable scope, Object arg1, Object arg2) {
		if (!(arg1 instanceof final Callable f)) {
			throw ScriptRuntime.notFunctionError(cx, arg1);
		}

		boolean isStrict = cx.isStrictMode();
		Iterator<Hashtable.Entry> i = entries.iterator();
		while (i.hasNext()) {
			// Per spec must convert every time so that primitives are always regenerated...
			Scriptable thisObj = ScriptRuntime.toObjectOrNull(cx, arg2, scope);

			if (thisObj == null && !isStrict) {
				thisObj = scope;
			}
			if (thisObj == null) {
				thisObj = Undefined.SCRIPTABLE_INSTANCE;
			}

			final Hashtable.Entry e = i.next();
			f.call(cx, scope, thisObj, new Object[]{e.value, e.value, this});
		}
		return Undefined.INSTANCE;
	}

	// ES2025 Set Methods Implementation

	private Object js_intersection(Context cx, Scriptable scope, Object[] args) {
		Object otherObj = args.length > 0 ? args[0] : Undefined.INSTANCE;

		NativeSet result = (NativeSet) cx.newObject(scope, CLASS_NAME);
		result.instanceOfSet = true;

		// ES2025: GetSetRecord requires size, has, and keys properties
		Scriptable scriptable = ensureScriptable(otherObj, cx);
		Object sizeVal = getProperty(scriptable, "size", cx);
		Object hasVal = getProperty(scriptable, "has", cx);
		Object keysVal = getProperty(scriptable, "keys", cx);

		validateSetLike(cx, sizeVal, hasVal, keysVal);

		return js_intersectionSetLike(cx, scope, otherObj, result, sizeVal, hasVal, keysVal);
	}

	private Object js_intersectionSetLike(Context cx, Scriptable scope, Object otherObj, NativeSet result, Object sizeVal, Object hasVal, Object keysVal) {
		if (!(hasVal instanceof Callable)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", "has", ScriptRuntime.typeof(cx, hasVal));
		}
		if (!(keysVal instanceof Callable)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", "keys", ScriptRuntime.typeof(cx, keysVal));
		}

		Callable hasMethod = (Callable) hasVal;
		Callable keysMethod = (Callable) keysVal;

		// ES2025: Compare sizes to determine iteration strategy
		int otherSize = toSetSize(cx, sizeVal);
		int thisSize = entries.size();

		if (thisSize <= otherSize) {
			// When this.size <= other.size: iterate through this, call other.has()
			for (Hashtable.Entry entry : entries) {
				Object key = entry.key;
				Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
				if (ScriptRuntime.toBoolean(cx, inOther)) {
					result.js_add(cx, key);
				}
			}
		} else {
			// When this.size > other.size: iterate through other.keys(), call this.has()
			Object iterator = ScriptRuntime.callIterator(keysMethod.call(cx, scope, ensureScriptable(otherObj, cx), ScriptRuntime.EMPTY_OBJECTS), cx, scope);
			try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
				for (Object key : it) {
					if (js_has(cx, key) == Boolean.TRUE) {
						result.js_add(cx, key);
					}
				}
			}
		}

		return result;
	}

	private Object js_union(Context cx, Scriptable scope, Object[] args) {
		Object otherObj = args.length > 0 ? args[0] : Undefined.INSTANCE;

		NativeSet result = (NativeSet) cx.newObject(scope, CLASS_NAME);
		result.instanceOfSet = true;

		for (Hashtable.Entry entry : entries) {
			result.js_add(cx, entry.key);
		}

		Scriptable scriptable = ensureScriptable(otherObj, cx);
		Object sizeVal = getProperty(scriptable, "size", cx);
		Object hasVal = getProperty(scriptable, "has", cx);
		Object keysVal = getProperty(scriptable, "keys", cx);

		validateSetLike(cx, sizeVal, hasVal, keysVal);
		toSetSize(cx, sizeVal);

		if (!(hasVal instanceof Callable)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", "has", ScriptRuntime.typeof(cx, hasVal));
		}
		if (!(keysVal instanceof Callable)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", "keys", ScriptRuntime.typeof(cx, keysVal));
		}

		Callable keysMethod = (Callable) keysVal;
		Object iterator = ScriptRuntime.callIterator(keysMethod.call(cx, scope, scriptable, ScriptRuntime.EMPTY_OBJECTS), cx, scope);
		try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
			for (Object key : it) {
				result.js_add(cx, key);
			}
		}

		return result;
	}

	private Object js_difference(Context cx, Scriptable scope, Object[] args) {
		Object otherObj = args.length > 0 ? args[0] : Undefined.INSTANCE;

		NativeSet result = (NativeSet) cx.newObject(scope, CLASS_NAME);
		result.instanceOfSet = true;

		Scriptable scriptable = ensureScriptable(otherObj, cx);
		Object sizeVal = getProperty(scriptable, "size", cx);
		Object hasVal = getProperty(scriptable, "has", cx);
		Object keysVal = getProperty(scriptable, "keys", cx);

		validateSetLike(cx, sizeVal, hasVal, keysVal);

		return js_differenceSetLike(cx, scope, otherObj, result, sizeVal, hasVal, keysVal);
	}

	private Object js_differenceSetLike(Context cx, Scriptable scope, Object otherObj, NativeSet result, Object sizeVal, Object hasVal, Object keysVal) {
		if (!(hasVal instanceof Callable)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", "has", ScriptRuntime.typeof(cx, hasVal));
		}
		if (!(keysVal instanceof Callable)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", "keys", ScriptRuntime.typeof(cx, keysVal));
		}

		Callable hasMethod = (Callable) hasVal;
		Callable keysMethod = (Callable) keysVal;

		int otherSize = toSetSize(cx, sizeVal);
		int thisSize = entries.size();

		// When this.size > other.size, iterate through other.keys() and remove
		// matching elements, NOT call other.has().
		if (thisSize > otherSize) {
			for (Hashtable.Entry entry : entries) {
				result.js_add(cx, entry.key);
			}

			Object iterator = ScriptRuntime.callIterator(keysMethod.call(cx, scope, ensureScriptable(otherObj, cx), ScriptRuntime.EMPTY_OBJECTS), cx, scope);
			try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
				for (Object key : it) {
					// Convert -0 to +0 as the spec requires
					if (key instanceof Number && ((Number) key).doubleValue() == ScriptRuntime.negativeZero) {
						key = ScriptRuntime.zeroObj;
					}
					result.js_delete(cx, key);
				}
			}
		} else {
			for (Hashtable.Entry entry : entries) {
				Object key = entry.key;
				Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
				if (!ScriptRuntime.toBoolean(cx, inOther)) {
					result.js_add(cx, key);
				}
			}
		}

		return result;
	}

	private Object js_symmetricDifference(Context cx, Scriptable scope, Object[] args) {
		Object otherObj = args.length > 0 ? args[0] : Undefined.INSTANCE;

		NativeSet result = (NativeSet) cx.newObject(scope, CLASS_NAME);
		result.instanceOfSet = true;

		Scriptable scriptable = ensureScriptable(otherObj, cx);
		Object sizeVal = getProperty(scriptable, "size", cx);
		Object hasVal = getProperty(scriptable, "has", cx);
		Object keysVal = getProperty(scriptable, "keys", cx);

		validateSetLike(cx, sizeVal, hasVal, keysVal);
		toSetSize(cx, sizeVal);

		if (!(hasVal instanceof Callable)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", "has", ScriptRuntime.typeof(cx, hasVal));
		}
		if (!(keysVal instanceof Callable)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", "keys", ScriptRuntime.typeof(cx, keysVal));
		}

		Callable hasMethod = (Callable) hasVal;
		Callable keysMethod = (Callable) keysVal;

		// Add elements from this that are not in other
		for (Hashtable.Entry entry : entries) {
			Object key = entry.key;
			Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
			if (!ScriptRuntime.toBoolean(cx, inOther)) {
				result.js_add(cx, key);
			}
		}

		// Add elements from other that are not in this
		Object iterator = ScriptRuntime.callIterator(keysMethod.call(cx, scope, scriptable, ScriptRuntime.EMPTY_OBJECTS), cx, scope);
		try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
			for (Object key : it) {
				if (js_has(cx, key) != Boolean.TRUE) {
					result.js_add(cx, key);
				}
			}
		}

		return result;
	}

	private Object js_isSubsetOf(Context cx, Scriptable scope, Object[] args) {
		Object otherObj = args.length > 0 ? args[0] : Undefined.INSTANCE;

		Scriptable scriptable = ensureScriptable(otherObj, cx);
		Object sizeVal = getProperty(scriptable, "size", cx);
		Object hasVal = getProperty(scriptable, "has", cx);
		Object keysVal = getProperty(scriptable, "keys", cx);

		validateSetLike(cx, sizeVal, hasVal, keysVal);

		if (!(hasVal instanceof Callable)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", "has", ScriptRuntime.typeof(cx, hasVal));
		}
		if (!(keysVal instanceof Callable)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", "keys", ScriptRuntime.typeof(cx, keysVal));
		}

		Callable hasMethod = (Callable) hasVal;

		int otherSize = toSetSize(cx, sizeVal);
		int thisSize = entries.size();

		// If this set is larger than other, it cannot be a subset
		if (thisSize > otherSize) {
			return Boolean.FALSE;
		}

		for (Hashtable.Entry entry : entries) {
			Object key = entry.key;
			Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
			if (!ScriptRuntime.toBoolean(cx, inOther)) {
				return Boolean.FALSE;
			}
		}

		return Boolean.TRUE;
	}

	private Object js_isSupersetOf(Context cx, Scriptable scope, Object[] args) {
		Object otherObj = args.length > 0 ? args[0] : Undefined.INSTANCE;

		Scriptable scriptable = ensureScriptable(otherObj, cx);
		Object sizeVal = getProperty(scriptable, "size", cx);
		Object hasVal = getProperty(scriptable, "has", cx);
		Object keysVal = getProperty(scriptable, "keys", cx);

		validateSetLike(cx, sizeVal, hasVal, keysVal);
		toSetSize(cx, sizeVal);

		if (!(hasVal instanceof Callable)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", "has", ScriptRuntime.typeof(cx, hasVal));
		}
		if (!(keysVal instanceof Callable)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", "keys", ScriptRuntime.typeof(cx, keysVal));
		}

		Callable keysMethod = (Callable) keysVal;
		Object iterator = ScriptRuntime.callIterator(keysMethod.call(cx, scope, scriptable, ScriptRuntime.EMPTY_OBJECTS), cx, scope);
		try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
			for (Object value : it) {
				if (js_has(cx, value) != Boolean.TRUE) {
					return Boolean.FALSE;
				}
			}
		}
		return Boolean.TRUE;
	}

	private Object js_isDisjointFrom(Context cx, Scriptable scope, Object[] args) {
		Object otherObj = args.length > 0 ? args[0] : Undefined.INSTANCE;

		Scriptable scriptable = ensureScriptable(otherObj, cx);
		Object sizeVal = getProperty(scriptable, "size", cx);
		Object hasVal = getProperty(scriptable, "has", cx);
		Object keysVal = getProperty(scriptable, "keys", cx);

		validateSetLike(cx, sizeVal, hasVal, keysVal);

		if (!(hasVal instanceof Callable)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", "has", ScriptRuntime.typeof(cx, hasVal));
		}
		if (!(keysVal instanceof Callable)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", "keys", ScriptRuntime.typeof(cx, keysVal));
		}

		Callable hasMethod = (Callable) hasVal;
		Callable keysMethod = (Callable) keysVal;

		int otherSize = toSetSize(cx, sizeVal);
		int thisSize = entries.size();

		if (thisSize <= otherSize) {
			for (Hashtable.Entry entry : entries) {
				Object key = entry.key;
				Object inOther = callHas(cx, scope, otherObj, hasMethod, key);
				if (ScriptRuntime.toBoolean(cx, inOther)) {
					return Boolean.FALSE;
				}
			}
		} else {
			Object iterator = ScriptRuntime.callIterator(keysMethod.call(cx, scope, scriptable, ScriptRuntime.EMPTY_OBJECTS), cx, scope);
			try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
				for (Object key : it) {
					if (js_has(cx, key) == Boolean.TRUE) {
						return Boolean.FALSE;
					}
				}
			}
		}

		return Boolean.TRUE;
	}

	// Helper methods for Set operations

	private static Object callHas(Context cx, Scriptable scope, Object obj, Object hasMethod, Object key) {
		return ((Callable) hasMethod).call(cx, scope, ensureScriptable(obj, cx), new Object[]{key});
	}

	private static int toSetSize(Context cx, Object sizeVal) {
		double otherSizeDouble = ScriptRuntime.toNumber(cx, sizeVal);
		if (Double.isNaN(otherSizeDouble)) {
			throw ScriptRuntime.typeError(cx, "size is not a number");
		}
		return Double.isInfinite(otherSizeDouble) ? Integer.MAX_VALUE : (int) Math.floor(otherSizeDouble);
	}

	private static void validateSetLike(Context cx, Object sizeVal, Object hasVal, Object keysVal) {
		if (sizeVal == NOT_FOUND) {
			throw ScriptRuntime.typeError(cx, "Set-like object must have a 'size' property");
		}
		if (hasVal == NOT_FOUND) {
			throw ScriptRuntime.typeError(cx, "Set-like object must have a 'has' method");
		}
		if (keysVal == NOT_FOUND) {
			throw ScriptRuntime.typeError(cx, "Set-like object must have a 'keys' method");
		}
	}
}
