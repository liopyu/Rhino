/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.DataObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * This class implements the Object native object.
 * See ECMA 15.2.
 *
 * @author Norris Boyd
 */
public class NativeObject extends ScriptableObject implements Map, DataObject {
	private static final String CLASS_NAME = "Object";

	static void init(Context cx, Scriptable scope, boolean sealed) {
		LambdaConstructor ctor = new LambdaConstructor(cx, scope, CLASS_NAME, 1, LambdaConstructor.CONSTRUCTOR_DEFAULT, NativeObject::jsConstructor);
		ctor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

		// Constructor (static) methods — Object.*
		ctor.defineConstructorMethod(cx, scope, "getPrototypeOf", 1, NativeObject::js_getPrototypeOf, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "setPrototypeOf", 2, NativeObject::js_setPrototypeOf, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "keys", 1, NativeObject::js_keys, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "entries", 1, NativeObject::js_entries, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "values", 1, NativeObject::js_values, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "fromEntries", 1, NativeObject::js_fromEntries, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "hasOwn", 2, NativeObject::js_hasOwn, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "getOwnPropertyNames", 1, NativeObject::js_getOwnPropertyNames, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "getOwnPropertySymbols", 1, NativeObject::js_getOwnPropertySymbols, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "getOwnPropertyDescriptor", 2, NativeObject::js_getOwnPropertyDescriptor, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "getOwnPropertyDescriptors", 1, NativeObject::js_getOwnPropertyDescriptors, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "defineProperty", 3, NativeObject::js_defineProperty, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "isExtensible", 1, NativeObject::js_isExtensible, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "preventExtensions", 1, NativeObject::js_preventExtensions, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "defineProperties", 2, NativeObject::js_defineProperties, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "create", 2, NativeObject::js_create, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "isSealed", 1, NativeObject::js_isSealed, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "isFrozen", 1, NativeObject::js_isFrozen, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "seal", 1, NativeObject::js_seal, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "freeze", 1, NativeObject::js_freeze, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "assign", 2, NativeObject::js_assign, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "is", 2, NativeObject::js_is, DONTENUM, DONTENUM | READONLY);
		ctor.defineConstructorMethod(cx, scope, "groupBy", 2, NativeObject::js_groupBy, DONTENUM, DONTENUM | READONLY);

		// Prototype methods — Object.prototype.*
		ctor.definePrototypeMethod(cx, scope, "toString", 0, NativeObject::js_toString, DONTENUM, DONTENUM | READONLY);
		ctor.definePrototypeMethod(cx, scope, "toLocaleString", 0, NativeObject::js_toLocaleString, DONTENUM, DONTENUM | READONLY);
		ctor.definePrototypeMethod(cx, scope, "valueOf", 0, NativeObject::js_valueOf, DONTENUM, DONTENUM | READONLY);
		ctor.definePrototypeMethod(cx, scope, "hasOwnProperty", 1, NativeObject::js_hasOwnProperty, DONTENUM, DONTENUM | READONLY);
		ctor.definePrototypeMethod(cx, scope, "propertyIsEnumerable", 1, NativeObject::js_propertyIsEnumerable, DONTENUM, DONTENUM | READONLY);
		ctor.definePrototypeMethod(cx, scope, "isPrototypeOf", 1, NativeObject::js_isPrototypeOf, DONTENUM, DONTENUM | READONLY);
		ctor.definePrototypeMethod(cx, scope, "toSource", 0, (lcx, lscope, thisObj, args) -> lcx.defaultObjectToSource(lscope, thisObj, args), DONTENUM, DONTENUM | READONLY);
		ctor.definePrototypeMethod(cx, scope, "__defineGetter__", 2, NativeObject::js_defineGetter, DONTENUM, DONTENUM | READONLY);
		ctor.definePrototypeMethod(cx, scope, "__defineSetter__", 2, NativeObject::js_defineSetter, DONTENUM, DONTENUM | READONLY);
		ctor.definePrototypeMethod(cx, scope, "__lookupGetter__", 1, NativeObject::js_lookupGetter, DONTENUM, DONTENUM | READONLY);
		ctor.definePrototypeMethod(cx, scope, "__lookupSetter__", 1, NativeObject::js_lookupSetter, DONTENUM, DONTENUM | READONLY);

		ScriptableObject.defineProperty(scope, CLASS_NAME, ctor, DONTENUM, cx);
		if (sealed) {
			ctor.sealObject(cx);
			((ScriptableObject) ctor.getPrototypeProperty(cx)).sealObject(cx);
		}
	}

	private static Scriptable getCompatibleObject(Context cx, Scriptable scope, Object arg) {
		Scriptable s = ScriptRuntime.toObject(cx, scope, arg);
		return ensureScriptable(s, cx);
	}

	private static Object getValueForId(Context cx, Scriptable obj, Object id) {
		Object value = id instanceof Integer index ? obj.get(cx, index, obj) : obj.get(cx, ScriptRuntime.toString(cx, id), obj);
		return value == NOT_FOUND ? Undefined.INSTANCE : value;
	}

	public final ContextFactory factory;

	public NativeObject(ContextFactory factory) {
		this.factory = factory;
	}

	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		boolean first = true;

		for (Map.Entry<?, ?> entry : entrySet()) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}

			sb.append(entry.getKey());
			sb.append(": ");
			sb.append(entry.getValue());
		}

		return sb.append('}').toString();
	}

	private static Scriptable jsConstructor(Context cx, Scriptable scope, Object[] args) {
		if (args.length == 0 || args[0] == null || Undefined.isUndefined(args[0])) {
			// Use cx.newObject so the prototype and parent scope are wired even on the
			// Object() function-call path (LambdaConstructor.call does not re-wire, unlike
			// the new-Object() construct path). Matches upstream js_constructorCall.
			return cx.newObject(scope);
		}
		return ScriptRuntime.toObject(cx, scope, args[0]);
	}

	// ---- Prototype methods ----

	private static Object js_toString(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.defaultObjectToString(thisObj);
	}

	private static Object js_toLocaleString(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object toString = getProperty(thisObj, "toString", cx);
		if (!(toString instanceof Callable fun)) {
			throw ScriptRuntime.notFunctionError(cx, toString);
		}
		return fun.call(cx, scope, thisObj, ScriptRuntime.EMPTY_OBJECTS);
	}

	private static Object js_valueOf(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (thisObj == null || Undefined.isUndefined(thisObj)) {
			throw ScriptRuntime.typeError0(cx, "msg." + (thisObj == null ? "null" : "undef") + ".to.object");
		}
		return thisObj;
	}

	private static Object js_hasOwnProperty(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (thisObj == null || Undefined.isUndefined(thisObj)) {
			throw ScriptRuntime.typeError0(cx, "msg." + (thisObj == null ? "null" : "undef") + ".to.object");
		}
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		return AbstractEcmaObjectOperations.hasOwnProperty(cx, thisObj, arg);
	}

	private static Object js_propertyIsEnumerable(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (thisObj == null || Undefined.isUndefined(thisObj)) {
			throw ScriptRuntime.typeError0(cx, "msg." + (thisObj == null ? "null" : "undef") + ".to.object");
		}

		boolean result;
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];

		if (arg instanceof Symbol) {
			result = ((SymbolScriptable) thisObj).has(cx, (Symbol) arg, thisObj);
			if (result && thisObj instanceof ScriptableObject so) {
				int attrs = so.getAttributes(cx, (Symbol) arg);
				result = ((attrs & DONTENUM) == 0);
			}
		} else {
			ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, arg);
			// When checking if a property is enumerable, a missing property should return "false" instead of
			// throwing an exception.  See: https://github.com/mozilla/rhino/issues/415
			try {
				if (s.stringId == null) {
					result = thisObj.has(cx, s.index, thisObj);
					if (result && thisObj instanceof ScriptableObject so) {
						int attrs = so.getAttributes(cx, s.index);
						result = ((attrs & DONTENUM) == 0);
					}
				} else {
					result = thisObj.has(cx, s.stringId, thisObj);
					if (result && thisObj instanceof ScriptableObject so) {
						int attrs = so.getAttributes(cx, s.stringId);
						result = ((attrs & DONTENUM) == 0);
					}
				}
			} catch (EvaluatorException ee) {
				if (ee.getMessage().startsWith(ScriptRuntime.getMessage1("msg.prop.not.found", s.stringId == null ? Integer.toString(s.index) : s.stringId))) {
					result = false;
				} else {
					throw ee;
				}
			}
		}
		return result;
	}

	private static Object js_isPrototypeOf(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (thisObj == null || Undefined.isUndefined(thisObj)) {
			throw ScriptRuntime.typeError0(cx, "msg." + (thisObj == null ? "null" : "undef") + ".to.object");
		}

		boolean result = false;
		if (args.length != 0 && args[0] instanceof Scriptable v) {
			do {
				v = v.getPrototype(cx);
				if (v == thisObj) {
					result = true;
					break;
				}
			} while (v != null);
		}
		return result;
	}

	private static Object js_defineGetter(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return js_defineGetterOrSetter(cx, scope, thisObj, args, false);
	}

	private static Object js_defineSetter(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return js_defineGetterOrSetter(cx, scope, thisObj, args, true);
	}

	private static Object js_defineGetterOrSetter(Context cx, Scriptable scope, Scriptable thisObj, Object[] args, boolean isSetter) {
		if (args.length < 2 || !(args[1] instanceof Callable getterOrSetter)) {
			Object badArg = (args.length >= 2 ? args[1] : Undefined.INSTANCE);
			throw ScriptRuntime.notFunctionError(cx, badArg);
		}
		if (!(thisObj instanceof ScriptableObject so)) {
			throw Context.reportRuntimeError2("msg.extend.scriptable", thisObj == null ? "null" : thisObj.getClass().getName(), String.valueOf(args[0]), cx);
		}
		ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, args[0]);
		int index = s.stringId != null ? 0 : s.index;
		so.setGetterOrSetter(cx, s.stringId, index, getterOrSetter, isSetter);
		if (so instanceof NativeArray) {
			((NativeArray) so).setDenseOnly(false);
		}
		return Undefined.INSTANCE;
	}

	private static Object js_lookupGetter(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return js_lookupGetterOrSetter(cx, scope, thisObj, args, false);
	}

	private static Object js_lookupSetter(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return js_lookupGetterOrSetter(cx, scope, thisObj, args, true);
	}

	private static Object js_lookupGetterOrSetter(Context cx, Scriptable scope, Scriptable thisObj, Object[] args, boolean isSetter) {
		if (args.length < 1 || !(thisObj instanceof ScriptableObject so)) {
			return Undefined.INSTANCE;
		}

		ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, args[0]);
		int index = s.stringId != null ? 0 : s.index;
		Function gs;
		for (; ; ) {
			gs = so.getGetterOrSetter(cx, s.stringId, index, scope, isSetter);
			if (gs != null) {
				break;
			}
			// If there is no getter or setter for the object itself,
			// how about the prototype?
			Scriptable v = so.getPrototype(cx);
			if (v == null) {
				break;
			}
			if (v instanceof ScriptableObject) {
				so = (ScriptableObject) v;
			} else {
				break;
			}
		}
		if (gs != null) {
			return gs;
		}
		return Undefined.INSTANCE;
	}

	// ---- Constructor (static) methods: Object.* ----

	private static Object js_getPrototypeOf(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		Scriptable obj = getCompatibleObject(cx, scope, arg);
		return obj.getPrototype(cx);
	}

	private static Object js_setPrototypeOf(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (args.length < 2) {
			throw ScriptRuntime.typeError3(cx, "msg.method.missing.parameter", "Object.setPrototypeOf", "2", Integer.toString(args.length));
		}
		Scriptable proto = (args[1] == null) ? null : ensureScriptable(args[1], cx);
		if (proto instanceof Symbol) {
			throw ScriptRuntime.typeError1(cx, "msg.arg.not.object", ScriptRuntime.typeof(cx, proto));
		}

		final Object arg0 = args[0];
		ScriptRuntimeES6.requireObjectCoercible(cx, arg0, CLASS_NAME, "setPrototypeOf");
		if (!(arg0 instanceof ScriptableObject obj)) {
			return arg0;
		}
		if (!obj.isExtensible()) {
			throw ScriptRuntime.typeError0(cx, "msg.not.extensible");
		}

		// cycle detection
		Scriptable prototypeProto = proto;
		while (prototypeProto != null) {
			if (prototypeProto == obj) {
				throw ScriptRuntime.typeError1(cx, "msg.object.cyclic.prototype", obj.getClass().getSimpleName());
			}
			prototypeProto = prototypeProto.getPrototype(cx);
		}
		obj.setPrototype(proto);
		return obj;
	}

	private static Object js_keys(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		Scriptable obj = getCompatibleObject(cx, scope, arg);
		Object[] ids = obj.getIds(cx);
		for (int i = 0; i < ids.length; i++) {
			ids[i] = ScriptRuntime.toString(cx, ids[i]);
		}
		return cx.newArray(scope, ids);
	}

	private static Object js_entries(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		Scriptable obj = getCompatibleObject(cx, scope, arg);
		Object[] ids = obj.getIds(cx);
		Object[] entries = new Object[ids.length];
		for (int i = 0; i < ids.length; i++) {
			entries[i] = cx.newArray(scope, new Object[]{ScriptRuntime.toString(cx, ids[i]), getValueForId(cx, obj, ids[i])});
		}
		return cx.newArray(scope, entries);
	}

	private static Object js_fromEntries(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		Scriptable iterable = getCompatibleObject(cx, scope, arg);
		Scriptable obj = cx.newObject(scope);
		Object ito = ScriptRuntime.callIterator(iterable, cx, scope);
		if (!Undefined.INSTANCE.equals(ito)) {
			try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, ito)) {
				for (Object val : it) {
					if (!(val instanceof Scriptable entry) || val instanceof Symbol) {
						throw ScriptRuntime.typeError1(cx, "msg.arg.not.object", ScriptRuntime.typeof(cx, val));
					}
					Object key = entry.get(cx, 0, entry);
					if (key == NOT_FOUND) {
						key = Undefined.INSTANCE;
					}
					Object value = entry.get(cx, 1, entry);
					if (value == NOT_FOUND) {
						value = Undefined.INSTANCE;
					}
					if (key instanceof Symbol sym && obj instanceof SymbolScriptable symObj) {
						symObj.put(cx, sym, obj, value);
					} else {
						ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, key);
						if (s.stringId == null) {
							obj.put(cx, s.index, obj, value);
						} else {
							obj.put(cx, s.stringId, obj, value);
						}
					}
				}
			}
		}
		return obj;
	}

	private static Object js_values(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		Scriptable obj = getCompatibleObject(cx, scope, arg);
		Object[] ids = obj.getIds(cx);
		Object[] values = new Object[ids.length];
		for (int i = 0; i < ids.length; i++) {
			values[i] = getValueForId(cx, obj, ids[i]);
		}
		return cx.newArray(scope, values);
	}

	private static Object js_getOwnPropertyNames(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		Scriptable s = getCompatibleObject(cx, scope, arg);
		ScriptableObject obj = ensureScriptableObject(s, cx);
		Object[] ids = obj.getIds(cx, true, false);
		for (int i = 0; i < ids.length; i++) {
			ids[i] = ScriptRuntime.toString(cx, ids[i]);
		}
		return cx.newArray(scope, ids);
	}

	private static Object js_getOwnPropertySymbols(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		Scriptable s = getCompatibleObject(cx, scope, arg);
		ScriptableObject obj = ensureScriptableObject(s, cx);
		Object[] ids = obj.getIds(cx, true, true);
		ArrayList<Object> syms = new ArrayList<>();
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] instanceof Symbol) {
				syms.add(ids[i]);
			}
		}
		return cx.newArray(scope, syms.toArray());
	}

	private static Object js_hasOwn(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		Object propertyName = args.length < 2 ? Undefined.INSTANCE : args[1];
		return AbstractEcmaObjectOperations.hasOwnProperty(cx, arg, propertyName);
	}

	private static Object js_getOwnPropertyDescriptors(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		Scriptable s = getCompatibleObject(cx, scope, arg);
		ScriptableObject obj = ensureScriptableObject(s, cx);

		ScriptableObject descs = (ScriptableObject) cx.newObject(scope);
		for (Object key : obj.getIds(cx, true, true)) {
			Scriptable desc = obj.getOwnPropertyDescriptor(cx, key);
			if (desc == null) {
				continue;
			} else if (key instanceof Symbol sym) {
				descs.put(cx, sym, descs, desc);
			} else if (key instanceof Integer index) {
				descs.put(cx, index, descs, desc);
			} else {
				descs.put(cx, ScriptRuntime.toString(cx, key), descs, desc);
			}
		}
		return descs;
	}

	private static Object js_getOwnPropertyDescriptor(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		Scriptable s = getCompatibleObject(cx, scope, arg);
		ScriptableObject obj = ensureScriptableObject(s, cx);
		Object nameArg = args.length < 2 ? Undefined.INSTANCE : args[1];
		Scriptable desc = obj.getOwnPropertyDescriptor(cx, nameArg);
		return desc == null ? Undefined.INSTANCE : desc;
	}

	private static Object js_defineProperty(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		ScriptableObject obj = ensureScriptableObject(arg, cx);
		Object name = args.length < 2 ? Undefined.INSTANCE : args[1];
		Object descArg = args.length < 3 ? Undefined.INSTANCE : args[2];
		ScriptableObject desc = ensureScriptableObject(descArg, cx);
		obj.defineOwnProperty(cx, name, desc);
		return obj;
	}

	private static Object js_isExtensible(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		if (!(arg instanceof ScriptableObject)) {
			return Boolean.FALSE;
		}

		ScriptableObject obj = ensureScriptableObject(arg, cx);
		return obj.isExtensible();
	}

	private static Object js_preventExtensions(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		if (!(arg instanceof ScriptableObject)) {
			return arg;
		}

		ScriptableObject obj = ensureScriptableObject(arg, cx);
		obj.preventExtensions();
		return obj;
	}

	private static Object js_defineProperties(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		ScriptableObject obj = ensureScriptableObject(arg, cx);
		Object propsObj = args.length < 2 ? Undefined.INSTANCE : args[1];
		Scriptable props = ScriptRuntime.toObject(cx, scope, propsObj);
		obj.defineOwnProperties(cx, ensureScriptableObject(props, cx));
		return obj;
	}

	private static Object js_create(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		Scriptable obj = (arg == null) ? null : ensureScriptable(arg, cx);

		ScriptableObject newObject = new NativeObject(cx.factory);
		newObject.setParentScope(scope);
		newObject.setPrototype(obj);

		if (args.length > 1 && !Undefined.isUndefined(args[1])) {
			Scriptable props = ScriptRuntime.toObject(cx, scope, args[1]);
			newObject.defineOwnProperties(cx, ensureScriptableObject(props, cx));
		}

		return newObject;
	}

	private static Object js_isSealed(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		if (!(arg instanceof ScriptableObject)) {
			return Boolean.TRUE;
		}

		return AbstractEcmaObjectOperations.testIntegrityLevel(cx, arg, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.SEALED);
	}

	private static Object js_isFrozen(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		if (!(arg instanceof ScriptableObject)) {
			return Boolean.TRUE;
		}

		return AbstractEcmaObjectOperations.testIntegrityLevel(cx, arg, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.FROZEN);
	}

	private static Object js_seal(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		if (!(arg instanceof ScriptableObject)) {
			return arg;
		}

		AbstractEcmaObjectOperations.setIntegrityLevel(cx, arg, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.SEALED);

		return arg;
	}

	private static Object js_freeze(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = args.length < 1 ? Undefined.INSTANCE : args[0];
		if (!(arg instanceof ScriptableObject)) {
			return arg;
		}

		AbstractEcmaObjectOperations.setIntegrityLevel(cx, arg, AbstractEcmaObjectOperations.INTEGRITY_LEVEL.FROZEN);

		return arg;
	}

	private static Object js_assign(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable targetObj;
		if (args.length > 0) {
			targetObj = ScriptRuntime.toObject(cx, scope, args[0]);
		} else {
			targetObj = ScriptRuntime.toObject(cx, scope, Undefined.INSTANCE);
		}
		for (int i = 1; i < args.length; i++) {
			if ((args[i] == null) || Undefined.isUndefined(args[i])) {
				continue;
			}
			Scriptable sourceObj = ScriptRuntime.toObject(cx, scope, args[i]);
			Object[] ids;
			if (sourceObj instanceof ScriptableObject so) {
				ids = so.getIds(cx, false, true);
			} else {
				ids = sourceObj.getIds(cx);
			}
			for (Object key : ids) {
				if (key instanceof Integer intId) {
					if (sourceObj.has(cx, intId, sourceObj) && isEnumerable(cx, intId, sourceObj)) {
						Object val = sourceObj.get(cx, intId, sourceObj);
						AbstractEcmaObjectOperations.put(cx, targetObj, intId, val, true);
					}
				} else if (key instanceof String stringId) {
					if (sourceObj.has(cx, stringId, sourceObj) && isEnumerable(cx, stringId, sourceObj)) {
						Object val = sourceObj.get(cx, stringId, sourceObj);
						AbstractEcmaObjectOperations.put(cx, targetObj, stringId, val, true);
					}
				}
			}

			// Separate Symbol pass — they must be copied after string properties.
			if (sourceObj instanceof ScriptableObject sourceSO) {
				for (Object key : ids) {
					if (key instanceof Symbol sym) {
						if (sourceSO.has(cx, sym, sourceObj) && isEnumerable(cx, sym, sourceObj)) {
							Object val = sourceSO.get(cx, sym, sourceObj);
							AbstractEcmaObjectOperations.put(cx, targetObj, sym, val, true);
						}
					}
				}
			}
		}
		return targetObj;
	}

	private static Object js_is(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object a1 = args.length < 1 ? Undefined.INSTANCE : args[0];
		Object a2 = args.length < 2 ? Undefined.INSTANCE : args[1];
		return ScriptRuntime.same(cx, a1, a2);
	}

	private static Object js_groupBy(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object items = args.length < 1 ? Undefined.INSTANCE : args[0];
		Object callback = args.length < 2 ? Undefined.INSTANCE : args[1];

		Map<Object, List<Object>> groups = AbstractEcmaObjectOperations.groupBy(cx, scope, CLASS_NAME, "groupBy", items, callback, AbstractEcmaObjectOperations.KEY_COERCION.PROPERTY);

		NativeObject obj = (NativeObject) cx.newObject(scope);
		obj.setPrototype(null);

		for (Map.Entry<Object, List<Object>> entry : groups.entrySet()) {
			Scriptable elements = cx.newArray(scope, entry.getValue().toArray());

			ScriptableObject desc = (ScriptableObject) cx.newObject(scope);
			desc.put(cx, "enumerable", desc, Boolean.TRUE);
			desc.put(cx, "configurable", desc, Boolean.TRUE);
			desc.put(cx, "value", desc, elements);

			obj.defineOwnProperty(cx, entry.getKey(), desc);
		}

		return obj;
	}

	// ---- java.util.Map implementation ----

	@Override
	public boolean containsKey(Object key) {
		if (key instanceof String) {
			return has(factory.enter(), (String) key, this);
		} else if (key instanceof Number) {
			return has(factory.enter(), ((Number) key).intValue(), this);
		}
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		for (Object obj : values()) {
			if (Objects.equals(value, obj)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Object remove(Object key) {
		Object value = get(key);
		if (key instanceof String) {
			delete(factory.enter(), (String) key);
		} else if (key instanceof Number) {
			delete(factory.enter(), ((Number) key).intValue());
		}
		return value;
	}

	@Override
	public Set<Object> keySet() {
		return new KeySet();
	}

	@Override
	public Collection<Object> values() {
		return new ValueCollection();
	}

	@Override
	public Set<Map.Entry<Object, Object>> entrySet() {
		return new EntrySet();
	}

	@Override
	public Object put(Object key, Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object get(Object key) {
		return get(factory.enter(), key);
	}

	@Override
	public void putAll(Map m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T createDataObject(Supplier<T> instanceFactory, Context cx) {
		T inst = instanceFactory.get();

		try {
			for (Field field : inst.getClass().getFields()) {
				if (Modifier.isPublic(field.getModifiers()) && !Modifier.isTransient(field.getModifiers()) && has(cx, field.getName(), this)) {
					field.setAccessible(true);
					field.set(inst, get(cx, field.getName(), this));
				}
			}
		} catch (Exception ex) {
			Context.throwAsScriptRuntimeEx(ex, cx);
		}

		return inst;
	}

	@Override
	public <T> List<T> createDataObjectList(Supplier<T> instanceFactory, Context cx) {
		return Collections.singletonList(createDataObject(instanceFactory, cx));
	}

	@Override
	public boolean isDataObjectList() {
		return false;
	}

	class EntrySet extends AbstractSet<Entry<Object, Object>> {
		@Override
		public Iterator<Entry<Object, Object>> iterator() {
			return new Iterator<>() {
				final Object[] ids = getIds(factory.enter());
				Object key = null;
				int index = 0;

				@Override
				public boolean hasNext() {
					return index < ids.length;
				}

				@Override
				public Map.Entry<Object, Object> next() {
					final Object ekey = key = ids[index++];
					final Object value = get(key);
					return new Map.Entry<>() {
						@Override
						public Object getKey() {
							return ekey;
						}

						@Override
						public Object getValue() {
							return value;
						}

						@Override
						public Object setValue(Object value) {
							throw new UnsupportedOperationException();
						}

						@Override
						public boolean equals(Object other) {
							if (!(other instanceof Entry<?, ?> e)) {
								return false;
							}
							return (ekey == null ? e.getKey() == null : ekey.equals(e.getKey())) && (value == null ? e.getValue() == null : value.equals(e.getValue()));
						}

						@Override
						public int hashCode() {
							return (ekey == null ? 0 : ekey.hashCode()) ^ (value == null ? 0 : value.hashCode());
						}

						@Override
						public String toString() {
							return ekey + "=" + value;
						}
					};
				}

				@Override
				public void remove() {
					if (key == null) {
						throw new IllegalStateException();
					}
					NativeObject.this.remove(key);
					key = null;
				}
			};
		}

		@Override
		public int size() {
			return NativeObject.this.size();
		}
	}

	class KeySet extends AbstractSet<Object> {

		@Override
		public boolean contains(Object key) {
			return containsKey(key);
		}

		@Override
		public Iterator<Object> iterator() {
			return new Iterator<>() {
				final Object[] ids = getIds(factory.enter());
				Object key;
				int index = 0;

				@Override
				public boolean hasNext() {
					return index < ids.length;
				}

				@Override
				public Object next() {
					try {
						return (key = ids[index++]);
					} catch (ArrayIndexOutOfBoundsException e) {
						key = null;
						throw new NoSuchElementException();
					}
				}

				@Override
				public void remove() {
					if (key == null) {
						throw new IllegalStateException();
					}
					NativeObject.this.remove(key);
					key = null;
				}
			};
		}

		@Override
		public int size() {
			return NativeObject.this.size();
		}
	}

	class ValueCollection extends AbstractCollection<Object> {

		@Override
		public Iterator<Object> iterator() {
			return new Iterator<>() {
				final Object[] ids = getIds(factory.enter());
				Object key;
				int index = 0;

				@Override
				public boolean hasNext() {
					return index < ids.length;
				}

				@Override
				public Object next() {
					return get((key = ids[index++]));
				}

				@Override
				public void remove() {
					if (key == null) {
						throw new IllegalStateException();
					}
					NativeObject.this.remove(key);
					key = null;
				}
			};
		}

		@Override
		public int size() {
			return NativeObject.this.size();
		}
	}

	private static boolean isEnumerable(Context cx, int index, Object obj) {
		if (obj instanceof ScriptableObject so) {
			return (so.getAttributes(cx, index) & DONTENUM) == 0;
		}
		return true;
	}

	private static boolean isEnumerable(Context cx, String key, Object obj) {
		if (obj instanceof ScriptableObject so) {
			return (so.getAttributes(cx, key) & DONTENUM) == 0;
		}
		return true;
	}

	private static boolean isEnumerable(Context cx, Symbol sym, Object obj) {
		if (obj instanceof ScriptableObject so) {
			return (so.getAttributes(cx, sym) & DONTENUM) == 0;
		}
		return true;
	}
}
