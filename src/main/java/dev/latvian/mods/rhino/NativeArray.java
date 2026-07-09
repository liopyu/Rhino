/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.ArrayLikeAbstractOperations.IterativeOperation;
import dev.latvian.mods.rhino.ArrayLikeAbstractOperations.ReduceOperation;
import dev.latvian.mods.rhino.util.DataObject;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import static dev.latvian.mods.rhino.ArrayLikeAbstractOperations.getRawElem;

/**
 * This class implements the Array native object.
 *
 * @author Norris Boyd
 * @author Mike McCabe
 */
public class NativeArray extends ScriptableObject implements List, DataObject {

	/*
	 * Optimization possibilities and open issues:
	 * - Long vs. double schizophrenia.  I suspect it might be better
	 * to use double throughout.
	 *
	 * - Functions that need a new Array call "new Array" in the
	 * current scope rather than using a hardwired constructor;
	 * "Array" could be redefined.  It turns out that js calls the
	 * equivalent of "new Array" in the current scope, except that it
	 * always gets at least an object back, even when Array == null.
	 */

	private static final Object ARRAY_TAG = "Array";
	private static final String CLASS_NAME = "Array";
	private static final Long NEGATIVE_ONE = Long.valueOf(-1);
	private static final String[] UNSCOPABLES = {
		"at",
		"copyWithin",
		"entries",
		"fill",
		"find",
		"findIndex",
		"findLast",
		"findLastIndex",
		"flat",
		"flatMap",
		"includes",
		"keys",
		"toReversed",
		"toSorted",
		"toSpliced",
		"values"
	};

	static void init(Scriptable scope, boolean sealed, Context cx) {
		LambdaConstructor ctor =
				new LambdaConstructor(cx, scope, CLASS_NAME, 1, NativeArray::jsConstructor);

		var proto = new NativeArray(cx, 0);
		ctor.setPrototypeScriptable(proto, cx);

		defineMethodOnConstructor(cx, ctor, scope, "of", 0, NativeArray::js_of);
		defineMethodOnConstructor(cx, ctor, scope, "from", 1, NativeArray::js_from);
		defineMethodOnConstructor(cx, ctor, scope, "isArray", 1, NativeArray::js_isArrayMethod);

		// The following need to appear on the constructor for
		// historical reasons even thought they should no tbe there
		// according to the spec.

		exposeMethodOnConstructor(cx, ctor, scope, "join", 1, NativeArray::js_join);
		exposeMethodOnConstructor(cx, ctor, scope, "reverse", 0, NativeArray::js_reverse);
		exposeMethodOnConstructor(cx, ctor, scope, "sort", 1, NativeArray::js_sort);
		exposeMethodOnConstructor(cx, ctor, scope, "push", 1, NativeArray::js_push);
		exposeMethodOnConstructor(cx, ctor, scope, "pop", 0, NativeArray::js_pop);
		exposeMethodOnConstructor(cx, ctor, scope, "shift", 0, NativeArray::js_shift);
		exposeMethodOnConstructor(cx, ctor, scope, "unshift", 1, NativeArray::js_unshift);
		exposeMethodOnConstructor(cx, ctor, scope, "splice", 2, NativeArray::js_splice);
		exposeMethodOnConstructor(cx, ctor, scope, "concat", 1, NativeArray::js_concat);
		exposeMethodOnConstructor(cx, ctor, scope, "slice", 2, NativeArray::js_slice);
		exposeMethodOnConstructor(cx, ctor, scope, "indexOf", 1, NativeArray::js_indexOf);
		exposeMethodOnConstructor(cx, ctor, scope, "lastIndexOf", 1, NativeArray::js_lastIndexOf);
		exposeMethodOnConstructor(cx, ctor, scope, "every", 1, NativeArray::js_every);
		exposeMethodOnConstructor(cx, ctor, scope, "filter", 1, NativeArray::js_filter);
		exposeMethodOnConstructor(cx, ctor, scope, "forEach", 1, NativeArray::js_forEach);
		exposeMethodOnConstructor(cx, ctor, scope, "map", 1, NativeArray::js_map);
		exposeMethodOnConstructor(cx, ctor, scope, "some", 1, NativeArray::js_some);
		exposeMethodOnConstructor(cx, ctor, scope, "find", 1, NativeArray::js_find);
		exposeMethodOnConstructor(cx, ctor, scope, "findIndex", 1, NativeArray::js_findIndex);
		exposeMethodOnConstructor(cx, ctor, scope, "findLast", 1, NativeArray::js_findLast);
		exposeMethodOnConstructor(cx, ctor, scope, "findLastIndex", 1, NativeArray::js_findLastIndex);
		exposeMethodOnConstructor(cx, ctor, scope, "reduce", 1, NativeArray::js_reduce);
		exposeMethodOnConstructor(cx, ctor, scope, "reduceRight", 1, NativeArray::js_reduceRight);

		defineMethodOnPrototype(cx, ctor, scope, "toString", 0, NativeArray::js_toString);
		defineMethodOnPrototype(cx, ctor, scope, "toLocaleString", 0, NativeArray::js_toLocaleString);
		defineMethodOnPrototype(cx, ctor, scope, "toSource", 0, NativeArray::js_toSource);
		defineMethodOnPrototype(cx, ctor, scope, "join", 1, NativeArray::js_join);
		defineMethodOnPrototype(cx, ctor, scope, "reverse", 0, NativeArray::js_reverse);
		defineMethodOnPrototype(cx, ctor, scope, "sort", 1, NativeArray::js_sort);
		defineMethodOnPrototype(cx, ctor, scope, "push", 1, NativeArray::js_push);
		defineMethodOnPrototype(cx, ctor, scope, "pop", 0, NativeArray::js_pop);
		defineMethodOnPrototype(cx, ctor, scope, "shift", 0, NativeArray::js_shift);
		defineMethodOnPrototype(cx, ctor, scope, "unshift", 1, NativeArray::js_unshift);
		defineMethodOnPrototype(cx, ctor, scope, "splice", 2, NativeArray::js_splice);
		defineMethodOnPrototype(cx, ctor, scope, "concat", 1, NativeArray::js_concat);
		defineMethodOnPrototype(cx, ctor, scope, "slice", 2, NativeArray::js_slice);
		defineMethodOnPrototype(cx, ctor, scope, "indexOf", 1, NativeArray::js_indexOf);
		defineMethodOnPrototype(cx, ctor, scope, "lastIndexOf", 1, NativeArray::js_lastIndexOf);
		defineMethodOnPrototype(cx, ctor, scope, "includes", 1, NativeArray::js_includes);
		defineMethodOnPrototype(cx, ctor, scope, "fill", 1, NativeArray::js_fill);
		defineMethodOnPrototype(cx, ctor, scope, "copyWithin", 2, NativeArray::js_copyWithin);
		defineMethodOnPrototype(cx, ctor, scope, "at", 1, NativeArray::js_at);
		defineMethodOnPrototype(cx, ctor, scope, "flat", 0, NativeArray::js_flat);
		defineMethodOnPrototype(cx, ctor, scope, "flatMap", 1, NativeArray::js_flatMap);
		defineMethodOnPrototype(cx, ctor, scope, "every", 1, NativeArray::js_every);
		defineMethodOnPrototype(cx, ctor, scope, "filter", 1, NativeArray::js_filter);
		defineMethodOnPrototype(cx, ctor, scope, "forEach", 1, NativeArray::js_forEach);
		defineMethodOnPrototype(cx, ctor, scope, "map", 1, NativeArray::js_map);
		defineMethodOnPrototype(cx, ctor, scope, "some", 1, NativeArray::js_some);
		defineMethodOnPrototype(cx, ctor, scope, "find", 1, NativeArray::js_find);
		defineMethodOnPrototype(cx, ctor, scope, "findIndex", 1, NativeArray::js_findIndex);
		defineMethodOnPrototype(cx, ctor, scope, "findLast", 1, NativeArray::js_findLast);
		defineMethodOnPrototype(cx, ctor, scope, "findLastIndex", 1, NativeArray::js_findLastIndex);
		defineMethodOnPrototype(cx, ctor, scope, "reduce", 1, NativeArray::js_reduce);
		defineMethodOnPrototype(cx, ctor, scope, "reduceRight", 1, NativeArray::js_reduceRight);
		defineMethodOnPrototype(cx, ctor, scope, "keys", 0, NativeArray::js_keys);
		defineMethodOnPrototype(cx, ctor, scope, "entries", 0, NativeArray::js_entries);
		defineMethodOnPrototype(cx, ctor, scope, "values", 0, NativeArray::js_values);
		defineMethodOnPrototype(cx, ctor, scope, "toReversed", 0, NativeArray::js_toReversed);
		defineMethodOnPrototype(cx, ctor, scope, "toSorted", 1, NativeArray::js_toSorted);
		defineMethodOnPrototype(cx, ctor, scope, "toSpliced", 2, NativeArray::js_toSpliced);
		defineMethodOnPrototype(cx, ctor, scope, "with", 2, NativeArray::js_with);

		ctor.definePrototypeAlias(cx, "values", SymbolKey.ITERATOR, DONTENUM);
		ScriptRuntimeES6.addSymbolSpecies(cx, scope, ctor);

		proto.defineProperty(cx, SymbolKey.UNSCOPABLES, makeUnscopables(cx, scope), DONTENUM | READONLY);

		ctor.setPrototypePropertyAttributes(PERMANENT | READONLY | DONTENUM);
		ScriptableObject.defineProperty(scope, CLASS_NAME, ctor, DONTENUM, cx);
		if (sealed) {
			ctor.sealObject(cx);
			((NativeArray) ctor.getPrototypeProperty(cx)).sealObject(cx);
		}
	}

	private static void defineMethodOnConstructor(
			Context cx,
			LambdaConstructor constructor,
			Scriptable scope,
			String name,
			int length,
			Callable target) {
		constructor.defineConstructorMethod(
				cx, scope, name, length, target, DONTENUM, DONTENUM | READONLY);
	}

	private static void defineMethodOnPrototype(
			Context cx,
			LambdaConstructor constructor,
			Scriptable scope,
			String name,
			int length,
			Callable target) {
		constructor.definePrototypeMethod(
				cx, scope, name, length, target, DONTENUM, DONTENUM | READONLY);
	}

	private static void exposeMethodOnConstructor(
			Context cx,
			LambdaConstructor constructor,
			Scriptable scope,
			String name,
			int length,
			Callable target) {
		constructor.defineConstructorMethod(
				cx,
				scope,
				name,
				length,
				(c, s, thisObj, args) -> {
					var realThis = ScriptRuntime.toObject(c, scope, args[0]);
					var realArgs = Arrays.copyOfRange(args, 1, args.length);
					return target.call(c, s, realThis, realArgs);
				},
				DONTENUM,
				DONTENUM | READONLY);
	}

	static int getMaximumInitialCapacity() {
		return maximumInitialCapacity;
	}

	static void setMaximumInitialCapacity(int maximumInitialCapacity) {
		NativeArray.maximumInitialCapacity = maximumInitialCapacity;
	}

	public NativeArray(Context cx, long lengthArg) {
		localContext = cx;
		denseOnly = lengthArg <= maximumInitialCapacity;
		if (denseOnly) {
			int intLength = (int) lengthArg;
			if (intLength < DEFAULT_INITIAL_CAPACITY) intLength = DEFAULT_INITIAL_CAPACITY;
			dense = new Object[intLength];
			Arrays.fill(dense, Scriptable.NOT_FOUND);
		}
		length = lengthArg;
		createLengthProp();
	}

	public NativeArray(Context cx, Object[] array) {
		localContext = cx;
		denseOnly = true;
		dense = array;
		length = array.length;
		createLengthProp();
	}

	@Override
	public String getClassName() {
		return "Array";
	}

	private static final int Id_length = 1, MAX_INSTANCE_ID = 1;

	@Override
	public void setPrototype(Scriptable p) {
		super.setPrototype(p);
		if (!(p instanceof NativeArray)) {
			setDenseOnly(false);
		}
	}

	private static Object makeUnscopables(Context cx, Scriptable scope) {
		NativeObject obj;

		obj = (NativeObject) cx.newObject(scope);

		ScriptableObject desc = ScriptableObject.buildDataDescriptor(obj, true, EMPTY, cx);
		for (var k : UNSCOPABLES) {
			obj.defineOwnProperty(cx, k, desc);
		}
		obj.setPrototype(null); // unscopables don't have any prototype
		return obj;
	}

	@Override
	public Object get(Context cx, int index, Scriptable start) {
		if (!denseOnly && isGetterOrSetter(null, index, false)) {
			return super.get(cx, index, start);
		}
		if (dense != null && 0 <= index && index < dense.length) {
			return dense[index];
		}
		return super.get(cx, index, start);
	}

	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		if (!denseOnly && isGetterOrSetter(null, index, false)) {
			return super.has(cx, index, start);
		}
		if (dense != null && 0 <= index && index < dense.length) {
			return dense[index] != NOT_FOUND;
		}
		return super.has(cx, index, start);
	}

	private static long toArrayIndex(Context cx, Object id) {
		if (id instanceof String) {
			return toArrayIndex(cx, (String) id);
		} else if (id instanceof Number) {
			return toArrayIndex(((Number) id).doubleValue());
		}
		return -1;
	}

	// if id is an array index (ECMA 15.4.0), return the number,
	// otherwise return -1L
	private static long toArrayIndex(Context cx, String id) {
		long index = toArrayIndex(ScriptRuntime.toNumber(cx, id));
		// Assume that ScriptRuntime.toString(cx, index) is the same
		// as java.lang.Long.toString(index) for long
		if (Long.toString(index).equals(id)) {
			return index;
		}
		return -1;
	}

	private static long toArrayIndex(double d) {
		if (!Double.isNaN(d)) {
			long index = ScriptRuntime.toUint32(d);
			if (index == d && index != 4294967295L) {
				return index;
			}
		}
		return -1;
	}

	private static int toDenseIndex(Context cx, Object id) {
		long index = toArrayIndex(cx, id);
		return 0 <= index && index < Integer.MAX_VALUE ? (int) index : -1;
	}

	@Override
	public void put(Context cx, String id, Scriptable start, Object value) {
		super.put(cx, id, start, value);
		if (start == this) {
			// If the object is sealed, super will throw exception
			long index = toArrayIndex(cx, id);
			if (index >= length) {
				length = index + 1;
				modCount++;
				denseOnly = false;
			}
		}
	}

	private boolean ensureCapacity(int capacity) {
		if (capacity > dense.length) {
			if (capacity > MAX_PRE_GROW_SIZE) {
				denseOnly = false;
				return false;
			}
			capacity = Math.max(capacity, (int) (dense.length * GROW_FACTOR));
			Object[] newDense = new Object[capacity];
			System.arraycopy(dense, 0, newDense, 0, dense.length);
			Arrays.fill(newDense, dense.length, newDense.length, Scriptable.NOT_FOUND);
			dense = newDense;
		}
		return true;
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		if (start == this && !isSealed(cx) && dense != null && 0 <= index && (denseOnly || !isGetterOrSetter(null, index, true))) {
			if (!isExtensible() && this.length <= index) {
				return;
			} else if (index < dense.length) {
				dense[index] = value;
				if (this.length <= index) {
					this.length = (long) index + 1;
					this.modCount++;
				}
				return;
			} else if (denseOnly && index < dense.length * GROW_FACTOR && ensureCapacity(index + 1)) {
				dense[index] = value;
				this.length = (long) index + 1;
				this.modCount++;
				return;
			} else {
				denseOnly = false;
			}
		}
		super.put(cx, index, start, value);
		if (start == this && (lengthAttr & READONLY) == 0) {
			// only set the array length if given an array index (ECMA 15.4.0)
			if (this.length <= index) {
				// avoid overflowing index!
				this.length = (long) index + 1;
				this.modCount++;
			}
		}
	}

	@Override
	public void delete(Context cx, int index) {
		if (dense != null && 0 <= index && index < dense.length && !isSealed(cx) && (denseOnly || !isGetterOrSetter(null, index, true))) {
			dense[index] = NOT_FOUND;
		} else {
			super.delete(cx, index);
		}
	}

	@Override
	public Object[] getIds(Context cx, boolean nonEnumerable, boolean getSymbols) {
		Object[] superIds = super.getIds(cx, nonEnumerable, getSymbols);
		if (dense == null) {
			return superIds;
		}
		int N = dense.length;
		long currentLength = length;
		if (N > currentLength) {
			N = (int) currentLength;
		}
		if (N == 0) {
			return superIds;
		}
		int superLength = superIds.length;
		Object[] ids = new Object[N + superLength];

		int presentCount = 0;
		for (int i = 0; i != N; ++i) {
			// Replace existing elements by their indexes
			if (dense[i] != NOT_FOUND) {
				ids[presentCount] = i;
				++presentCount;
			}
		}
		if (presentCount != N) {
			// dense contains deleted elems, need to shrink the result
			Object[] tmp = new Object[presentCount + superLength];
			System.arraycopy(ids, 0, tmp, 0, presentCount);
			ids = tmp;
		}
		System.arraycopy(superIds, 0, ids, presentCount, superLength);
		return ids;
	}

	public List<Integer> getIndexIds(Context cx) {
		Object[] ids = getIds(cx);
		List<Integer> indices = new ArrayList<>(ids.length);
		for (Object id : ids) {
			int int32Id = ScriptRuntime.toInt32(cx, id);
			if (int32Id >= 0 && ScriptRuntime.toString(cx, int32Id).equals(ScriptRuntime.toString(cx, id))) {
				indices.add(int32Id);
			}
		}
		return indices;
	}

	private ScriptableObject defaultIndexPropertyDescriptor(Object value, Context cx) {
		Scriptable scope = getParentScope();
		if (scope == null) {
			scope = this;
		}
		ScriptableObject desc = new NativeObject(cx.factory);
		ScriptRuntime.setBuiltinProtoAndParent(cx, scope, desc, TopLevel.Builtins.Object);
		desc.defineProperty(cx, "value", value, EMPTY);
		desc.defineProperty(cx, "writable", Boolean.TRUE, EMPTY);
		desc.defineProperty(cx, "enumerable", Boolean.TRUE, EMPTY);
		desc.defineProperty(cx, "configurable", Boolean.TRUE, EMPTY);
		return desc;
	}

	@Override
	public int getAttributes(Context cx, int index) {
		if (dense != null && index >= 0 && index < dense.length && dense[index] != NOT_FOUND) {
			return EMPTY;
		}
		return super.getAttributes(cx, index);
	}

	@Override
	protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
		if (dense != null) {
			int index = toDenseIndex(cx, id);
			if (0 <= index && index < dense.length && dense[index] != NOT_FOUND) {
				Object value = dense[index];
				return defaultIndexPropertyDescriptor(value, cx);
			}
		}
		return super.getOwnPropertyDescriptor(cx, id);
	}

	@Override
	protected void defineOwnProperty(Context cx, Object id, ScriptableObject desc, boolean checkValid) {
		long index = toArrayIndex(cx, id);
		if (index >= length) {
			length = index + 1;
			modCount++;
		}

		if (index != -1 && dense != null) {
			Object[] values = dense;
			dense = null;
			denseOnly = false;
			for (int i = 0; i < values.length; i++) {
				if (values[i] != NOT_FOUND) {
					if (!isExtensible()) {
						// Force creating a slot, before calling put(...) on the next line, which
						// would otherwise fail on an array on which preventExtensions() has been
						// called
						setAttributes(cx, i, 0);
					}
					put(cx, i, this, values[i]);
				}
			}
		}

		super.defineOwnProperty(cx, id, desc, checkValid);

		if ("length".equals(id)) {
			lengthAttr = getAttributes(cx, "length"); // Update cached attributes value for length property
		}
	}

	/** See ECMA 15.4.1,2 */
	static Scriptable jsConstructor(Context cx, Scriptable scope, Object[] args) {
		if (args.length == 0) return new NativeArray(cx, 0);

		// Follow ECMA and use a single Number arg as the length. (The fork does
		// not support the legacy 1.2 "single arg is first element" behaviour.)
		NativeArray res;
		Object arg0 = args[0];
		if (args.length > 1 || !(arg0 instanceof Number)) {
			res = new NativeArray(cx, args);
		} else {
			long len = ScriptRuntime.toUint32(cx, arg0);
			if (len != ((Number) arg0).doubleValue()) {
				String msg = ScriptRuntime.getMessage0("msg.arraylength.bad");
				throw ScriptRuntime.rangeError(cx, msg);
			}
			res = new NativeArray(cx, len);
		}

		return res;
	}

	private void createLengthProp() {
		ScriptableObject.defineBuiltInProperty(
				this,
				"length",
				DONTENUM | PERMANENT,
				NativeArray::lengthGetter,
				NativeArray::lengthSetter,
				NativeArray::lengthAttrSetter,
				NativeArray::arraySetLength);
	}

	private static Object lengthGetter(NativeArray array, Scriptable start, Context cx) {
		return ScriptRuntime.wrapNumber((double) array.length);
	}

	private static boolean lengthSetter(
			NativeArray builtIn,
			Object value,
			Scriptable owner,
			Scriptable start,
			boolean isThrow,
			Context cx) {
		builtIn.setLength(cx, value);
		return true;
	}

	private static void lengthAttrSetter(NativeArray builtIn, int attrs) {
		builtIn.lengthAttr = attrs;
	}

	protected static void arraySetLength(
			NativeArray builtIn,
			BuiltInSlot<NativeArray> current,
			Object id,
			ScriptableObject desc,
			boolean checkValid,
			Object key,
			int index,
			Context cx) {
		// 10.2.4.2 Step 1.
		Object value = getProperty(desc, "value", cx);

		if (value == NOT_FOUND) {
			builtIn.defineOrdinaryProperty(cx, id, desc, checkValid, key, index);
			return;
		}

		// 10.2.4.2 Steps 2 - 6
		long newLength = checkLength(cx, value);

		Object writable = getProperty(desc, "writable", cx);
		// 10.2.4.2 9 is true by definition

		// 10.2.4.2 10-11
		if (newLength >= builtIn.length) {
			builtIn.defineOrdinaryProperty(cx, id, desc, checkValid, key, index);
			return;
		}

		boolean currentWritable = ((current.getAttributes() & READONLY) == 0);
		if (!currentWritable) {
			throw ScriptRuntime.typeError1(cx, "msg.change.value.with.writable.false", id);
		}
		boolean newWritable = true;
		if (writable != NOT_FOUND) {
			newWritable = isTrue(writable, cx);
			putProperty(desc, "writable", true, cx);
		}

		// The standard set path that will be done by this call will
		// clear any elements as required.
		builtIn.defineOrdinaryProperty(cx, id, desc, checkValid, key, index);
		var currentAttrs = current.getAttributes();
		var newAttrs = newWritable ? (currentAttrs & ~READONLY) : (currentAttrs | READONLY);
		current.setAttributes(newAttrs);
	}

	private static Scriptable callConstructorOrCreateArray(
			Context cx, Scriptable scope, Scriptable arg, long length, boolean lengthAlways) {
		Scriptable result = null;

		if (arg instanceof Constructable) {
			try {
				final Object[] args =
						(lengthAlways || (length > 0))
								? new Object[] {Long.valueOf(length)}
								: ScriptRuntime.EMPTY_OBJECTS;
				result = ((Constructable) arg).construct(cx, scope, args);
			} catch (EcmaError ee) {
				if (!"TypeError".equals(ee.getName())) {
					throw ee;
				}
				// If we get here then it is likely that the function we called is not really
				// a constructor. Unfortunately there's no better way to tell in Rhino right now.
			}
		}

		if (result == null) {
			// "length" below is really a hint so don't worry if it's really large
			result = cx.newArray(scope, (length > Integer.MAX_VALUE) ? 0 : (int) length);
		}

		return result;
	}

	private static Object js_from(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		final Scriptable items =
				ScriptRuntime.toObject(cx, scope, (args.length >= 1) ? args[0] : Undefined.INSTANCE);
		Object mapArg = (args.length >= 2) ? args[1] : Undefined.INSTANCE;
		Scriptable thisArg = Undefined.SCRIPTABLE_INSTANCE;
		final boolean mapping = !Undefined.isUndefined(mapArg);
		Function mapFn = null;

		if (mapping) {
			if (!(mapArg instanceof Function)) {
				throw ScriptRuntime.typeError0(cx, "msg.map.function.not");
			}
			mapFn = (Function) mapArg;
			if (args.length >= 3) {
				thisArg = ensureScriptable(args[2], cx);
			}
		}

		Object iteratorProp = ScriptableObject.getProperty(items, SymbolKey.ITERATOR, cx);
		if (!(items instanceof NativeArray)
				&& (iteratorProp != Scriptable.NOT_FOUND)
				&& !Undefined.isUndefined(iteratorProp)) {
			final Object iterator = ScriptRuntime.callIterator(items, cx, scope);
			if (!Undefined.isUndefined(iterator)) {
				final Scriptable result =
						callConstructorOrCreateArray(cx, scope, thisObj, 0, false);
				long k = 0;
				try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
					for (Object temp : it) {
						if (mapping) {
							temp =
									mapFn.call(
											cx,
											scope,
											thisArg,
											new Object[] {temp, Long.valueOf(k)});
						}
						ArrayLikeAbstractOperations.defineElem(cx, result, k, temp);
						k++;
					}
				}
				setLengthProperty(cx, result, k);
				return result;
			}
		}

		final long length = getLengthProperty(cx, items);
		final Scriptable result = callConstructorOrCreateArray(cx, scope, thisObj, length, true);
		for (long k = 0; k < length; k++) {
			Object temp = getElem(cx, items, k);
			if (mapping) {
				temp = mapFn.call(cx, scope, thisArg, new Object[] {temp, Long.valueOf(k)});
			}
			ArrayLikeAbstractOperations.defineElem(cx, result, k, temp);
		}

		setLengthProperty(cx, result, length);
		return result;
	}

	private static Object js_of(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		final Scriptable result =
				callConstructorOrCreateArray(cx, scope, thisObj, args.length, true);

		if (result instanceof ScriptableObject) {
			ScriptableObject desc = ScriptableObject.buildDataDescriptor(result, null, EMPTY, cx);
			for (int i = 0; i < args.length; i++) {
				desc.put(cx, "value", desc, args[i]);
				((ScriptableObject) result).defineOwnProperty(cx, i, desc);
			}
		} else {
			for (int i = 0; i < args.length; i++) {
				ArrayLikeAbstractOperations.defineElem(cx, result, i, args[i]);
			}
		}
		setLengthProperty(cx, result, args.length);

		return result;
	}

	public long getLength() {
		return length;
	}

	/**
	 * @deprecated Use {@link #getLength()} instead.
	 */
	@Deprecated
	public long jsGet_length() {
		return getLength();
	}

	/**
	 * Change the value of the internal flag that determines whether all storage is handed by a
	 * dense backing array rather than an associative store.
	 *
	 * @param denseOnly new value for denseOnly flag
	 * @throws IllegalArgumentException if an attempt is made to enable denseOnly after it was
	 *     disabled; NativeArray code is not written to handle switching back to a dense
	 *     representation
	 */
	void setDenseOnly(boolean denseOnly) {
		if (denseOnly && !this.denseOnly) throw new IllegalArgumentException();
		this.denseOnly = denseOnly;
	}

	boolean getDenseOnly() {
		return denseOnly;
	}

	private void setLength(Context cx, Object val) {
		/* XXX do we satisfy this?
		 * 15.4.5.1 [[Put]](P, V):
		 * 1. Call the [[CanPut]] method of A with name P.
		 * 2. If Result(1) is false, return.
		 * ?
		 */
		if ((lengthAttr & READONLY) != 0) {
			return;
		}

		double d = ScriptRuntime.toNumber(cx, val);
		long longVal = ScriptRuntime.toUint32(d);
		if (longVal != d) {
			String msg = ScriptRuntime.getMessage0("msg.arraylength.bad");
			throw ScriptRuntime.rangeError(cx, msg);
		}

		if (denseOnly) {
			if (longVal < length) {
				// downcast okay because denseOnly
				Arrays.fill(dense, (int) longVal, dense.length, NOT_FOUND);
				length = longVal;
				modCount++;
				return;
			} else if (longVal < MAX_PRE_GROW_SIZE && longVal < (length * GROW_FACTOR) && ensureCapacity((int) longVal)) {
				length = longVal;
				modCount++;
				return;
			} else {
				denseOnly = false;
			}
		}
		if (longVal < length) {
			// remove all properties between longVal and length
			if (length - longVal > 0x1000) {
				// assume that the representation is sparse
				Object[] e = getIds(cx); // will only find in object itself
				for (Object id : e) {
					if (id instanceof String strId) {
						// > MAXINT will appear as string
						long index = toArrayIndex(cx, strId);
						if (index >= longVal) {
							delete(cx, strId);
						}
					} else {
						int index = (Integer) id;
						if (index >= longVal) {
							delete(cx, index);
						}
					}
				}
			} else {
				// assume a dense representation
				for (long i = longVal; i < length; i++) {
					deleteElem(this, i, cx);
				}
			}
		}
		length = longVal;
		modCount++;
	}

	private static long checkLength(Context cx, Object val) {
		double d = ScriptRuntime.toNumber(cx, val);
		long longVal = ScriptRuntime.toUint32(cx, val);
		if (longVal != d) {
			String msg = ScriptRuntime.getMessage0("msg.arraylength.bad");
			throw ScriptRuntime.rangeError(cx, msg);
		}
		return longVal;
	}

	/* Support for generic Array-ish objects.  Most of the Array
	 * functions try to be generic; anything that has a length
	 * property is assumed to be an array.
	 * getLengthProperty returns 0 if obj does not have the length property
	 * or its value is not convertible to a number.
	 */
	static long getLengthProperty(Context cx, Scriptable obj) {
		// These will give numeric lengths within Uint32 range.
		if (obj instanceof NativeString) {
			return ((NativeString) obj).getLength();
		}
		if (obj instanceof NativeArray) {
			return ((NativeArray) obj).getLength();
		}

		Object len = ScriptableObject.getProperty(obj, "length", cx);
		if (len == Scriptable.NOT_FOUND) {
			// toUint32(undefined) == 0
			return 0;
		}

		double doubleLen = ScriptRuntime.toNumber(cx, len);

		// ToLength
		if (doubleLen > NativeNumber.MAX_SAFE_INTEGER) {
			return (long) NativeNumber.MAX_SAFE_INTEGER;
		}
		if (doubleLen < 0) {
			return 0;
		}
		return (long) doubleLen;
	}

	private static Object setLengthProperty(Context cx, Scriptable target, long length) {
		Object len = ScriptRuntime.wrapNumber((double) length);
		ScriptableObject.putProperty(target, "length", len, cx);
		return len;
	}

	/* Utility functions to encapsulate index > Integer.MAX_VALUE
	 * handling.  Also avoids unnecessary object creation that would
	 * be necessary to use the general ScriptRuntime.get/setElem
	 * functions... though this is probably premature optimization.
	 */
	private static void deleteElem(Scriptable target, long index, Context cx) {
		int i = (int) index;
		if (i == index) {
			target.delete(cx, i);
		} else {
			target.delete(cx, Long.toString(index));
		}
	}

	private static Object getElem(Context cx, Scriptable target, long index) {
		Object elem = getRawElem(target, index, cx);
		return (elem != Scriptable.NOT_FOUND ? elem : Undefined.INSTANCE);
	}

	private static void defineElemOrThrow(Context cx, Scriptable target, long index, Object value) {
		if (index > NativeNumber.MAX_SAFE_INTEGER) {
			throw ScriptRuntime.typeError1(cx, "msg.arraylength.too.big", String.valueOf(index));
		} else {
			ArrayLikeAbstractOperations.defineElem(cx, target, index, value);
		}
	}

	private static void setElem(Context cx, Scriptable target, long index, Object value) {
		if (index > Integer.MAX_VALUE) {
			String id = Long.toString(index);
			ScriptableObject.putProperty(target, id, value, cx);
		} else {
			ScriptableObject.putProperty(target, (int) index, value, cx);
		}
	}

	// Similar as setElem(), but triggers deleteElem() if value is NOT_FOUND
	private static void setRawElem(Context cx, Scriptable target, long index, Object value) {
		if (value == NOT_FOUND) {
			deleteElem(target, index, cx);
		} else {
			setElem(cx, target, index, value);
		}
	}

	private static String js_toString(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return toStringHelper(cx, scope, thisObj, false);
	}

	private static String js_toLocaleString(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return toStringHelper(cx, scope, thisObj, true);
	}

	private static String js_toSource(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return toSource(cx, scope, thisObj);
	}

	private static String toStringHelper(Context cx, Scriptable scope, Scriptable thisObj, boolean toLocale) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		int length = (int) getLengthProperty(cx, o);

		if (length == 0) {
			return "[]";
		}

		StringBuilder result = new StringBuilder(256);
		result.append('[');

		for (int i = 0; i < length; i++) {
			if (i > 0) {
				result.append(", ");
			}
			Object elem = getRawElem(o, i, cx);
			if (elem == NOT_FOUND || elem == null || elem == Undefined.INSTANCE) {
				continue;
			}

			result.append(ScriptRuntime.uneval(cx, scope, elem));
		}

		result.append(']');

		return result.toString();
	}

	private static String toSource(Context cx, Scriptable scope, Scriptable thisObj) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		int length = (int) getLengthProperty(cx, o);

		if (length == 0) {
			return "[]";
		}

		StringBuilder result = new StringBuilder(256);
		result.append('[');

		for (int i = 0; i < length; i++) {
			if (i > 0) {
				result.append(", ");
			}
			Object elem = getRawElem(o, i, cx);
			if (elem == NOT_FOUND || elem == null || elem == Undefined.INSTANCE) {
				continue;
			}

			result.append(ScriptRuntime.uneval(cx, scope, elem));
		}

		result.append(']');
		return result.toString();
	}

	/** See ECMA 15.4.4.3 */
	private static String js_join(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		long llength = getLengthProperty(cx, o);
		int length = (int) llength;
		if (llength != length) {
			throw Context.reportRuntimeError1(
					"msg.arraylength.too.big", String.valueOf(llength), cx);
		}
		// if no args, use "," as separator
		String separator =
				(args.length < 1 || args[0] == Undefined.INSTANCE)
						? ","
						: ScriptRuntime.toString(cx, args[0]);
		if (o instanceof NativeArray) {
			NativeArray na = (NativeArray) o;
			if (na.denseOnly) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < length; i++) {
					if (i != 0) {
						sb.append(separator);
					}
					if (i < na.dense.length) {
						Object temp = na.dense[i];
						if (temp != null
								&& temp != Undefined.INSTANCE
								&& temp != Scriptable.NOT_FOUND) {
							sb.append(ScriptRuntime.toString(cx, temp));
						}
					}
				}
				return sb.toString();
			}
		}
		if (length == 0) {
			return "";
		}
		String[] buf = new String[length];
		int total_size = 0;
		for (int i = 0; i != length; i++) {
			Object temp = getElem(cx, o, i);
			if (temp != null && temp != Undefined.INSTANCE) {
				String str = ScriptRuntime.toString(cx, temp);
				total_size += str.length();
				buf[i] = str;
			}
		}
		total_size += (length - 1) * separator.length();
		StringBuilder sb = new StringBuilder(total_size);
		for (int i = 0; i != length; i++) {
			if (i != 0) {
				sb.append(separator);
			}
			String str = buf[i];
			if (str != null) {
				// str == null for undefined or null
				sb.append(str);
			}
		}
		return sb.toString();
	}

	/** See ECMA 15.4.4.4 */
	private static Scriptable js_reverse(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		if (o instanceof NativeArray) {
			NativeArray na = (NativeArray) o;
			if (na.denseOnly) {
				for (int i = 0, j = ((int) na.length) - 1; i < j; i++, j--) {
					Object temp = na.dense[i];
					na.dense[i] = na.dense[j];
					na.dense[j] = temp;
				}
				return o;
			}
		}
		long len = getLengthProperty(cx, o);

		long half = len / 2;
		for (long i = 0; i < half; i++) {
			long j = len - i - 1;
			Object temp1 = getRawElem(o, i, cx);
			Object temp2 = getRawElem(o, j, cx);
			setRawElem(cx, o, i, temp2);
			setRawElem(cx, o, j, temp1);
		}
		return o;
	}

	/** See ECMA 15.4.4.5 */
	private static Scriptable js_sort(
			final Context cx,
			final Scriptable scope,
			final Scriptable thisObj,
			final Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
		Comparator<Object> comparator =
				ArrayLikeAbstractOperations.getSortComparator(cx, scope, args);
		return sort(cx, o, comparator);
	}

	private static Scriptable sort(Context cx, Scriptable o, Comparator<Object> comparator) {
		long llength = getLengthProperty(cx, o);
		final int length = (int) llength;
		if (llength != length) {
			throw Context.reportRuntimeError1(
					"msg.arraylength.too.big", String.valueOf(llength), cx);
		}
		// copy the JS array into a working array, so it can be
		// sorted cheaply.
		final Object[] working = new Object[length];
		for (int i = 0; i != length; ++i) {
			working[i] = getRawElem(o, i, cx);
		}

		// Java's 'Arrays.sort' is guaranteed to be stable so we can use it; however,
		// if the comparator is not consistent, it throws an IllegalArgumentException.
		// In case where the comparator is not consistent, the ECMAScript specification states
		// that sort order is implementation-defined, so we can just return the original array.
		try {
			Arrays.sort(working, comparator);
		} catch (IllegalArgumentException e) {
			return o;
		}

		// copy the working array back into thisObj
		for (int i = 0; i < length; ++i) {
			setRawElem(cx, o, i, working[i]);
		}

		return o;
	}

	private static Object js_push(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		if (o instanceof NativeArray) {
			NativeArray na = (NativeArray) o;
			if (na.denseOnly && na.ensureCapacity((int) na.length + args.length)) {
				for (Object arg : args) {
					na.dense[(int) na.length++] = arg;
					na.modCount++;
				}
				return ScriptRuntime.wrapNumber((double) na.length);
			}
		}
		long length = getLengthProperty(cx, o);
		for (int i = 0; i < args.length; i++) {
			setElem(cx, o, length + i, args[i]);
		}

		length += args.length;
		Object lengthObj = setLengthProperty(cx, o, length);
		return lengthObj;
	}

	private static Object js_pop(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		Object result;
		if (o instanceof NativeArray) {
			NativeArray na = (NativeArray) o;
			if (na.denseOnly && na.length > 0) {
				na.length--;
				na.modCount++;
				result = na.dense[(int) na.length];
				na.dense[(int) na.length] = NOT_FOUND;
				return result;
			}
		}
		long length = getLengthProperty(cx, o);
		if (length > 0) {
			length--;

			// Get the to-be-deleted property's value.
			result = getElem(cx, o, length);

			// We need to delete the last property, because 'thisObj' may not
			// have setLength which does that for us.
			deleteElem(o, length, cx);
		} else {
			result = Undefined.INSTANCE;
		}
		// necessary to match js even when length < 0; js pop will give a
		// length property to any target it is called on.
		setLengthProperty(cx, o, length);

		return result;
	}

	private static Object js_shift(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		if (o instanceof NativeArray) {
			NativeArray na = (NativeArray) o;
			if (na.denseOnly && na.length > 0) {
				na.length--;
				na.modCount++;
				Object result = na.dense[0];
				System.arraycopy(na.dense, 1, na.dense, 0, (int) na.length);
				na.dense[(int) na.length] = NOT_FOUND;
				return result == NOT_FOUND ? Undefined.INSTANCE : result;
			}
		}
		Object result;
		long length = getLengthProperty(cx, o);
		if (length > 0) {
			long i = 0;
			length--;

			// Get the to-be-deleted property's value.
			result = getElem(cx, o, i);

			/*
			 * Slide down the array above the first element.  Leave i
			 * set to point to the last element.
			 */
			if (length > 0) {
				for (i = 1; i <= length; i++) {
					Object temp = getRawElem(o, i, cx);
					setRawElem(cx, o, i - 1, temp);
				}
			}
			// We need to delete the last property, because 'thisObj' may not
			// have setLength which does that for us.
			deleteElem(o, length, cx);
		} else {
			result = Undefined.INSTANCE;
		}
		setLengthProperty(cx, o, length);
		return result;
	}

	private static Object js_unshift(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		if (o instanceof NativeArray) {
			NativeArray na = (NativeArray) o;
			if (na.denseOnly && na.ensureCapacity((int) na.length + args.length)) {
				System.arraycopy(na.dense, 0, na.dense, args.length, (int) na.length);
				System.arraycopy(args, 0, na.dense, 0, args.length);
				na.length += args.length;
				na.modCount++;
				return ScriptRuntime.wrapNumber((double) na.length);
			}
		}
		long length = getLengthProperty(cx, o);
		int argc = args.length;

		if (argc > 0) {
			if (length + argc > NativeNumber.MAX_SAFE_INTEGER) {
				throw ScriptRuntime.typeError1(cx, "msg.arraylength.too.big", length + argc);
			}

			/*  Slide up the array to make room for args at the bottom */
			if (length > 0) {
				for (long last = length - 1; last >= 0; last--) {
					Object temp = getRawElem(o, last, cx);
					setRawElem(cx, o, last + argc, temp);
				}
			}

			/* Copy from argv to the bottom of the array. */
			for (int i = 0; i < args.length; i++) {
				setElem(cx, o, i, args[i]);
			}
		}
		/* Follow Perl by returning the new array length. */
		length += argc;
		return setLengthProperty(cx, o, length);
	}

	private static Object js_splice(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		NativeArray na = null;
		Object result = ArrayLikeAbstractOperations.arraySpeciesCreate(cx, scope, o, 0);
		NativeArray nar = null;
		boolean denseFrom = false;
		boolean denseRes = false;

		if (o instanceof NativeArray) {
			na = (NativeArray) o;
			denseFrom = na.denseOnly;
		}
		if (result instanceof NativeArray) {
			nar = (NativeArray) result;
			denseRes = nar.denseOnly;
		}

		/* create an empty Array to return. */
		scope = getTopLevelScope(scope);
		int argc = args.length;
		if (argc == 0) return cx.newArray(scope, 0);
		long length = getLengthProperty(cx, o);

		/* Convert the first argument into a starting index. */
		long begin =
				ArrayLikeAbstractOperations.toSliceIndex(ScriptRuntime.toInteger(cx, args[0]), length);
		argc--;

		/* Convert the second argument into count */
		long actualDeleteCount;
		if (args.length == 1) {
			actualDeleteCount = length - begin;
		} else {
			double dcount = ScriptRuntime.toInteger(cx, args[1]);
			if (dcount < 0) {
				actualDeleteCount = 0;
			} else if (dcount > (length - begin)) {
				actualDeleteCount = length - begin;
			} else {
				actualDeleteCount = (long) dcount;
			}
			argc--;
		}

		long end = begin + actualDeleteCount;
		long delta = argc - actualDeleteCount;

		if (length + delta > NativeNumber.MAX_SAFE_INTEGER) {
			throw ScriptRuntime.typeError1(cx, "msg.arraylength.too.big", length + delta);
		}
		if (actualDeleteCount > Integer.MAX_VALUE) {
			String msg = ScriptRuntime.getMessage0("msg.arraylength.bad");
			throw ScriptRuntime.rangeError(cx, msg);
		}

		/* If there are elements to remove, put them into the return value. */
		if (actualDeleteCount != 0) {
			if (denseFrom && denseRes) {
				int intLen = (int) (end - begin);
				Object[] copy = new Object[intLen];
				System.arraycopy(na.dense, (int) begin, copy, 0, intLen);
				nar.dense = copy;
				nar.setLength(cx, intLen);
			} else {
				for (long last = begin; last != end; last++) {
					Object temp = getRawElem(o, last, cx);
					if (temp != NOT_FOUND) {
						ArrayLikeAbstractOperations.defineElem(
								cx, (ScriptableObject) result, last - begin, temp);
					}
				}
				// Need to set length for sparse result array
				setLengthProperty(cx, (ScriptableObject) result, end - begin);
			}
		}

		/* Find the direction (up or down) to copy and make way for argv. */
		if (denseFrom
				&& length + delta < Integer.MAX_VALUE
				&& na.ensureCapacity((int) (length + delta))) {
			System.arraycopy(
					na.dense, (int) end, na.dense, (int) (begin + argc), (int) (length - end));
			if (argc > 0) {
				System.arraycopy(args, 2, na.dense, (int) begin, argc);
			}
			if (delta < 0) {
				Arrays.fill(na.dense, (int) (length + delta), (int) length, NOT_FOUND);
			}
			na.length = length + delta;
			na.modCount++;
			return result;
		}

		if (delta > 0) {
			for (long last = length - 1; last >= end; last--) {
				Object temp = getRawElem(o, last, cx);
				setRawElem(cx, o, last + delta, temp);
			}
		} else if (delta < 0) {
			for (long last = end; last < length; last++) {
				Object temp = getRawElem(o, last, cx);
				setRawElem(cx, o, last + delta, temp);
			}
			// Do this backwards because some implementations might use a
			// non-sparse array and therefore might not be able to handle
			// deleting elements "in the middle". This makes us compatible
			// with older Rhino releases.
			for (long k = length - 1; k >= length + delta; --k) {
				deleteElem(o, k, cx);
			}
		}

		/* Copy from argv into the hole to complete the splice. */
		int argoffset = args.length - argc;
		for (int i = 0; i < argc; i++) {
			setElem(cx, o, begin + i, args[i + argoffset]);
		}

		/* Update length in case we deleted elements from the end. */
		setLengthProperty(cx, o, length + delta);
		return result;
	}

	private static boolean isConcatSpreadable(Context cx, Scriptable scope, Object val) {
		// First, look for the new @@isConcatSpreadable test as per ECMAScript 6 and up
		if (val instanceof Scriptable) {
			final Object spreadable =
					ScriptableObject.getProperty((Scriptable) val, SymbolKey.IS_CONCAT_SPREADABLE, cx);
			if ((spreadable != Scriptable.NOT_FOUND) && !Undefined.isUndefined(spreadable)) {
				// If @@isConcatSpreadable was undefined, we have to fall back to testing for an
				// array.
				// Otherwise, we found some value
				return ScriptRuntime.toBoolean(cx, spreadable);
			}
		}

		// Otherwise, it's only spreadable if it's a native array
		return js_isArray(val);
	}

	// Concat elements of "arg" into the destination, with optimizations for native,
	// dense arrays.
	private static long concatSpreadArg(
			Context cx, Scriptable result, Scriptable arg, long offset) {
		long srclen = getLengthProperty(cx, arg);
		long newlen = srclen + offset;

		if (newlen > NativeNumber.MAX_SAFE_INTEGER) {
			throw ScriptRuntime.typeError1(cx, "msg.arraylength.too.big", newlen);
		}

		// First, optimize for a pair of native, dense arrays
		if ((newlen <= Integer.MAX_VALUE) && (result instanceof NativeArray)) {
			final NativeArray denseResult = (NativeArray) result;
			if (denseResult.denseOnly && (arg instanceof NativeArray)) {
				final NativeArray denseArg = (NativeArray) arg;
				if (denseArg.denseOnly) {
					// Now we can optimize
					denseResult.ensureCapacity((int) newlen);
					System.arraycopy(
							denseArg.dense, 0, denseResult.dense, (int) offset, (int) srclen);
					return newlen;
				}
				// We could also optimize here if we are copying to a dense target from a non-dense
				// native array. However, if the source array is very sparse then the result will be
				// very bad -- so don't.
			}
		}

		// If we get here then we have to do things the generic way
		long dstpos = offset;
		for (long srcpos = 0; srcpos < srclen; srcpos++, dstpos++) {
			final Object temp = getRawElem(arg, srcpos, cx);
			if (temp != Scriptable.NOT_FOUND) {
				ArrayLikeAbstractOperations.defineElem(cx, result, dstpos, temp);
			}
		}
		return newlen;
	}

	private static long doConcat(
			Context cx, Scriptable scope, Scriptable result, Object arg, long offset) {
		if (isConcatSpreadable(cx, scope, arg)) {
			return concatSpreadArg(cx, result, (Scriptable) arg, offset);
		}
		ArrayLikeAbstractOperations.defineElem(cx, result, offset, arg);
		return offset + 1;
	}

	/*
	 * See Ecma 262v3 15.4.4.4
	 */
	private static Scriptable js_concat(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		// create an empty Array to return.
		scope = getTopLevelScope(scope);
		final Scriptable result = ArrayLikeAbstractOperations.arraySpeciesCreate(cx, scope, o, 0);

		long length = doConcat(cx, scope, result, o, 0);
		for (Object arg : args) {
			length = doConcat(cx, scope, result, arg, length);
		}

		setLengthProperty(cx, result, length);
		return result;
	}

	private static Scriptable js_slice(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		long len = getLengthProperty(cx, o);

		long begin, end;
		if (args.length == 0) {
			begin = 0;
			end = len;
		} else {
			begin = ArrayLikeAbstractOperations.toSliceIndex(ScriptRuntime.toInteger(cx, args[0]), len);
			if (args.length == 1 || args[1] == Undefined.INSTANCE) {
				end = len;
			} else {
				end =
						ArrayLikeAbstractOperations.toSliceIndex(
								ScriptRuntime.toInteger(cx, args[1]), len);
			}
		}

		if (end - begin > Integer.MAX_VALUE) {
			String msg = ScriptRuntime.getMessage0("msg.arraylength.bad");
			throw ScriptRuntime.rangeError(cx, msg);
		}

		Scriptable result = ArrayLikeAbstractOperations.arraySpeciesCreate(cx, scope, o, 0);
		for (long slot = begin; slot < end; slot++) {
			Object temp = getRawElem(o, slot, cx);
			if (temp != NOT_FOUND) {
				ArrayLikeAbstractOperations.defineElem(cx, result, slot - begin, temp);
			}
		}
		setLengthProperty(cx, result, Math.max(0, end - begin));

		return result;
	}

	private static Object js_indexOf(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object compareTo = args.length > 0 ? args[0] : Undefined.INSTANCE;

		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
		long length = getLengthProperty(cx, o);
		/*
		 * From http://developer.mozilla.org/en/docs/Core_JavaScript_1.5_Reference:Objects:Array:indexOf
		 * The index at which to begin the search. Defaults to 0, i.e. the
		 * whole array will be searched. If the index is greater than or
		 * equal to the length of the array, -1 is returned, i.e. the array
		 * will not be searched. If negative, it is taken as the offset from
		 * the end of the array. Note that even when the index is negative,
		 * the array is still searched from front to back. If the calculated
		 * index is less than 0, the whole array will be searched.
		 */
		long start;
		if (args.length < 2) {
			// default
			start = 0;
		} else {
			start = (long) ScriptRuntime.toInteger(cx, args[1]);
			if (start < 0) {
				start += length;
				if (start < 0) start = 0;
			}
			if (start > length - 1) return NEGATIVE_ONE;
		}
		if (o instanceof NativeArray) {
			NativeArray na = (NativeArray) o;
			if (na.denseOnly) {
				Scriptable proto = na.getPrototype(cx);
				for (int i = (int) start; i < length; i++) {
					Object val = na.dense[i];
					if (val == NOT_FOUND && proto != null) {
						val = ScriptableObject.getProperty(proto, i, cx);
					}
					if (val != NOT_FOUND && ScriptRuntime.shallowEq(cx, val, compareTo)) {
						return Long.valueOf(i);
					}
				}
				return NEGATIVE_ONE;
			}
		}
		for (long i = start; i < length; i++) {
			Object val = getRawElem(o, i, cx);
			if (val != NOT_FOUND && ScriptRuntime.shallowEq(cx, val, compareTo)) {
				return Long.valueOf(i);
			}
		}
		return NEGATIVE_ONE;
	}

	private static Object js_lastIndexOf(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object compareTo = args.length > 0 ? args[0] : Undefined.INSTANCE;

		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
		long length = getLengthProperty(cx, o);
		/*
		 * From http://developer.mozilla.org/en/docs/Core_JavaScript_1.5_Reference:Objects:Array:lastIndexOf
		 * The index at which to start searching backwards. Defaults to the
		 * array's length, i.e. the whole array will be searched. If the
		 * index is greater than or equal to the length of the array, the
		 * whole array will be searched. If negative, it is taken as the
		 * offset from the end of the array. Note that even when the index
		 * is negative, the array is still searched from back to front. If
		 * the calculated index is less than 0, -1 is returned, i.e. the
		 * array will not be searched.
		 */
		long start;
		if (args.length < 2) {
			// default
			start = length - 1;
		} else {
			start = (long) ScriptRuntime.toInteger(cx, args[1]);
			if (start >= length) start = length - 1;
			else if (start < 0) start += length;
			if (start < 0) return NEGATIVE_ONE;
		}
		if (o instanceof NativeArray) {
			NativeArray na = (NativeArray) o;
			if (na.denseOnly) {
				Scriptable proto = na.getPrototype(cx);
				for (int i = (int) start; i >= 0; i--) {
					Object val = na.dense[i];
					if (val == NOT_FOUND && proto != null) {
						val = ScriptableObject.getProperty(proto, i, cx);
					}
					if (val != NOT_FOUND && ScriptRuntime.shallowEq(cx, val, compareTo)) {
						return Long.valueOf(i);
					}
				}
				return NEGATIVE_ONE;
			}
		}
		for (long i = start; i >= 0; i--) {
			Object val = getRawElem(o, i, cx);
			if (val != NOT_FOUND && ScriptRuntime.shallowEq(cx, val, compareTo)) {
				return Long.valueOf(i);
			}
		}
		return NEGATIVE_ONE;
	}

	/*
	   See ECMA-262 22.1.3.13
	*/
	private static Boolean js_includes(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {

		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
		long len = getLengthProperty(cx, o);
		if (len == 0) return Boolean.FALSE;

		long k;
		if (args.length < 2) {
			k = 0;
		} else {
			k = (long) ScriptRuntime.toInteger(cx, args[1]);
			if (k < 0) {
				k += len;
				if (k < 0) k = 0;
			}
			if (k > len - 1) return Boolean.FALSE;
		}

		Object compareTo = args.length > 0 ? args[0] : Undefined.INSTANCE;
		if (o instanceof NativeArray) {
			NativeArray na = (NativeArray) o;
			if (na.denseOnly) {
				Scriptable proto = na.getPrototype(cx);
				for (int i = (int) k; i < len; i++) {
					Object elementK = na.dense[i];
					if (elementK == NOT_FOUND && proto != null) {
						elementK = ScriptableObject.getProperty(proto, i, cx);
					}
					if (elementK == NOT_FOUND) {
						elementK = Undefined.INSTANCE;
					}
					if (ScriptRuntime.sameZero(cx, elementK, compareTo)) {
						return Boolean.TRUE;
					}
				}
				return Boolean.FALSE;
			}
		}
		for (; k < len; k++) {
			Object elementK = getRawElem(o, k, cx);
			if (elementK == NOT_FOUND) {
				elementK = Undefined.INSTANCE;
			}
			if (ScriptRuntime.sameZero(cx, elementK, compareTo)) {
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}

	private static Object js_fill(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
		long len = getLengthProperty(cx, o);

		long relativeStart = 0;
		if (args.length >= 2) {
			relativeStart = (long) ScriptRuntime.toInteger(cx, args[1]);
		}
		final long k;
		if (relativeStart < 0) {
			k = Math.max((len + relativeStart), 0);
		} else {
			k = Math.min(relativeStart, len);
		}

		long relativeEnd = len;
		if (args.length >= 3 && !Undefined.isUndefined(args[2])) {
			relativeEnd = (long) ScriptRuntime.toInteger(cx, args[2]);
		}
		final long fin;
		if (relativeEnd < 0) {
			fin = Math.max((len + relativeEnd), 0);
		} else {
			fin = Math.min(relativeEnd, len);
		}

		Object value = args.length > 0 ? args[0] : Undefined.INSTANCE;
		for (long i = k; i < fin; i++) {
			setRawElem(cx, thisObj, i, value);
		}

		return thisObj;
	}

	private static Object js_copyWithin(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
		long len = getLengthProperty(cx, o);

		Object targetArg = (args.length >= 1) ? args[0] : Undefined.INSTANCE;
		long relativeTarget = (long) ScriptRuntime.toInteger(cx, targetArg);
		long to;
		if (relativeTarget < 0) {
			to = Math.max((len + relativeTarget), 0);
		} else {
			to = Math.min(relativeTarget, len);
		}

		Object startArg = (args.length >= 2) ? args[1] : Undefined.INSTANCE;
		long relativeStart = (long) ScriptRuntime.toInteger(cx, startArg);
		long from;
		if (relativeStart < 0) {
			from = Math.max((len + relativeStart), 0);
		} else {
			from = Math.min(relativeStart, len);
		}

		long relativeEnd = len;
		if (args.length >= 3 && !Undefined.isUndefined(args[2])) {
			relativeEnd = (long) ScriptRuntime.toInteger(cx, args[2]);
		}
		final long fin;
		if (relativeEnd < 0) {
			fin = Math.max((len + relativeEnd), 0);
		} else {
			fin = Math.min(relativeEnd, len);
		}

		long count = Math.min(fin - from, len - to);
		int direction = 1;
		if (from < to && to < from + count) {
			direction = -1;
			from = from + count - 1;
			to = to + count - 1;
		}

		// Optimize for a native array. If properties were overridden with setters
		// and other non-default options then we won't get here.
		if ((o instanceof NativeArray) && (count <= Integer.MAX_VALUE)) {
			NativeArray na = (NativeArray) o;
			if (na.denseOnly) {
				for (; count > 0; count--) {
					na.dense[(int) to] = na.dense[(int) from];
					from += direction;
					to += direction;
				}

				return thisObj;
			}
		}

		for (; count > 0; count--) {
			final Object temp = getRawElem(o, from, cx);
			if ((temp == Scriptable.NOT_FOUND) || Undefined.isUndefined(temp)) {
				deleteElem(o, to, cx);
			} else {
				setElem(cx, o, to, temp);
			}

			from += direction;
			to += direction;
		}

		return thisObj;
	}

	private static Object js_at(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
		long len = getLengthProperty(cx, o);

		long relativeIndex = 0;
		if (args.length >= 1) {
			relativeIndex = (long) ScriptRuntime.toInteger(cx, args[0]);
		}
		long k = (relativeIndex >= 0) ? relativeIndex : len + relativeIndex;
		if ((k < 0) || (k >= len)) {
			return Undefined.INSTANCE;
		}
		return getElem(cx, thisObj, k);
	}

	private static Object js_flat(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
		double depth;
		if (args.length < 1 || Undefined.isUndefined(args[0])) {
			depth = 1;
		} else {
			depth = ScriptRuntime.toInteger(cx, args[0]);
		}

		return flat(cx, scope, o, depth);
	}

	private static Scriptable flat(Context cx, Scriptable scope, Scriptable source, double depth) {
		long length = getLengthProperty(cx, source);

		Scriptable result;
		result = ArrayLikeAbstractOperations.arraySpeciesCreate(cx, scope, source, 0);
		long j = 0;
		for (long i = 0; i < length; i++) {
			Object elem = getRawElem(source, i, cx);
			if (elem == Scriptable.NOT_FOUND) {
				continue;
			}
			if (depth >= 1 && js_isArray(elem)) {
				Scriptable arr = flat(cx, scope, (Scriptable) elem, depth - 1);
				long arrLength = getLengthProperty(cx, arr);
				for (long k = 0; k < arrLength; k++) {
					Object temp = getRawElem(arr, k, cx);
					defineElemOrThrow(cx, result, j++, temp);
				}
			} else {
				defineElemOrThrow(cx, result, j++, elem);
			}
		}
		setLengthProperty(cx, result, j);
		return result;
	}

	private static Object js_flatMap(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
		Object callbackArg = args.length > 0 ? args[0] : Undefined.INSTANCE;

		Function f = ArrayLikeAbstractOperations.getCallbackArg(cx, callbackArg);
		Scriptable parent = ScriptableObject.getTopLevelScope(f);
		Scriptable thisArg;
		if (args.length < 2 || args[1] == null || args[1] == Undefined.INSTANCE) {
			thisArg = parent;
		} else {
			thisArg = ScriptRuntime.toObject(cx, scope, args[1]);
		}

		long length = getLengthProperty(cx, o);

		Scriptable result = ArrayLikeAbstractOperations.arraySpeciesCreate(cx, scope, o, 0);
		long j = 0;
		for (long i = 0; i < length; i++) {
			Object elem = getRawElem(o, i, cx);
			if (elem == Scriptable.NOT_FOUND) {
				continue;
			}
			Object[] innerArgs = new Object[] {elem, Long.valueOf(i), o};
			Object mapCall = f.call(cx, parent, thisArg, innerArgs);
			if (js_isArray(mapCall)) {
				Scriptable arr = (Scriptable) mapCall;
				long arrLength = getLengthProperty(cx, arr);
				for (long k = 0; k < arrLength; k++) {
					Object temp = getRawElem(arr, k, cx);
					defineElemOrThrow(cx, result, j++, temp);
				}
			} else {
				defineElemOrThrow(cx, result, j++, mapCall);
			}
		}
		setLengthProperty(cx, result, j);
		return result;
	}

	private static Object js_every(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ArrayLikeAbstractOperations.iterativeMethod(
				cx, ARRAY_TAG, "every", IterativeOperation.EVERY, scope, thisObj, args);
	}

	private static Object js_filter(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ArrayLikeAbstractOperations.iterativeMethod(
				cx, ARRAY_TAG, "filter", IterativeOperation.FILTER, scope, thisObj, args);
	}

	private static Object js_forEach(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ArrayLikeAbstractOperations.iterativeMethod(
				cx, ARRAY_TAG, "forEach", IterativeOperation.FOR_EACH, scope, thisObj, args);
	}

	private static Object js_map(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ArrayLikeAbstractOperations.iterativeMethod(
				cx, ARRAY_TAG, "map", IterativeOperation.MAP, scope, thisObj, args);
	}

	private static Object js_some(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ArrayLikeAbstractOperations.iterativeMethod(
				cx, ARRAY_TAG, "some", IterativeOperation.SOME, scope, thisObj, args);
	}

	private static Object js_find(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ArrayLikeAbstractOperations.iterativeMethod(
				cx, ARRAY_TAG, "find", IterativeOperation.FIND, scope, thisObj, args);
	}

	private static Object js_findIndex(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ArrayLikeAbstractOperations.iterativeMethod(
				cx, ARRAY_TAG, "findIndex", IterativeOperation.FIND_INDEX, scope, thisObj, args);
	}

	private static Object js_findLast(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ArrayLikeAbstractOperations.iterativeMethod(
				cx, ARRAY_TAG, "findLast", IterativeOperation.FIND_LAST, scope, thisObj, args);
	}

	private static Object js_findLastIndex(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ArrayLikeAbstractOperations.iterativeMethod(
				cx,
				ARRAY_TAG,
				"findLastIndex",
				IterativeOperation.FIND_LAST_INDEX,
				scope,
				thisObj,
				args);
	}

	private static Object js_reduce(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ArrayLikeAbstractOperations.reduceMethod(
				cx, ReduceOperation.REDUCE, scope, thisObj, args);
	}

	private static Object js_reduceRight(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ArrayLikeAbstractOperations.reduceMethod(
				cx, ReduceOperation.REDUCE_RIGHT, scope, thisObj, args);
	}

	private static Object js_keys(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		thisObj = ScriptRuntime.toObject(cx, scope, thisObj);
		return new NativeArrayIterator(cx, scope, thisObj, NativeArrayIterator.ArrayIteratorType.KEYS);
	}

	private static Object js_entries(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		thisObj = ScriptRuntime.toObject(cx, scope, thisObj);
		return new NativeArrayIterator(cx, scope, thisObj, NativeArrayIterator.ArrayIteratorType.ENTRIES);
	}

	private static Object js_values(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		thisObj = ScriptRuntime.toObject(cx, scope, thisObj);
		return new NativeArrayIterator(cx, scope, thisObj, NativeArrayIterator.ArrayIteratorType.VALUES);
	}

	private static Object js_isArrayMethod(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return Boolean.valueOf(args.length > 0 && js_isArray(args[0]));
	}

	private static boolean js_isArray(Object o) {
		if (!(o instanceof Scriptable)) {
			return false;
		}
		return "Array".equals(((Scriptable) o).getClassName());
	}

	private static Object js_toSorted(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Comparator<Object> comparator =
				ArrayLikeAbstractOperations.getSortComparator(cx, scope, args);

		Scriptable source = ScriptRuntime.toObject(cx, scope, thisObj);
		long len = getLengthProperty(cx, source);

		if (len > Integer.MAX_VALUE) {
			String msg = ScriptRuntime.getMessage0("msg.arraylength.bad");
			throw ScriptRuntime.rangeError(cx, msg);
		}
		Scriptable result = cx.newArray(scope, (int) len);

		for (int k = 0; k < len; ++k) {
			Object fromValue = getElem(cx, source, k);
			setElem(cx, result, k, fromValue);
		}

		sort(cx, result, comparator);
		return result;
	}

	private static Object js_toReversed(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable source = ScriptRuntime.toObject(cx, scope, thisObj);
		long len = getLengthProperty(cx, source);

		if (len > Integer.MAX_VALUE) {
			String msg = ScriptRuntime.getMessage0("msg.arraylength.bad");
			throw ScriptRuntime.rangeError(cx, msg);
		}
		Scriptable result = cx.newArray(scope, (int) len);

		for (int k = 0; k < len; ++k) {
			int from = (int) len - k - 1;
			Object fromValue = getElem(cx, source, from);
			setElem(cx, result, k, fromValue);
		}

		return result;
	}

	private static Object js_toSpliced(
			Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable source = ScriptRuntime.toObject(cx, scope, thisObj);
		long len = getLengthProperty(cx, source);

		long actualStart = 0;
		if (args.length > 0) {
			actualStart =
					ArrayLikeAbstractOperations.toSliceIndex(ScriptRuntime.toInteger(cx, args[0]), len);
		}

		long insertCount = args.length > 2 ? args.length - 2 : 0;

		long actualSkipCount;
		if (args.length == 0) {
			actualSkipCount = 0;
		} else if (args.length == 1) {
			actualSkipCount = len - actualStart;
		} else {
			long sc = ScriptRuntime.toLength(cx, args, 1);
			actualSkipCount = Math.max(0, Math.min(sc, len - actualStart));
		}

		long newLen = len + insertCount - actualSkipCount;
		if (newLen > NativeNumber.MAX_SAFE_INTEGER) {
			throw ScriptRuntime.typeError1(cx, "msg.arraylength.too.big", newLen);
		}
		if (newLen > Integer.MAX_VALUE) {
			String msg = ScriptRuntime.getMessage0("msg.arraylength.bad");
			throw ScriptRuntime.rangeError(cx, msg);
		}

		Scriptable result = cx.newArray(scope, (int) newLen);

		long i = 0;
		long r = actualStart + actualSkipCount;

		while (i < actualStart) {
			Object e = getElem(cx, source, i);
			setElem(cx, result, i, e);
			i++;
		}

		for (int j = 2; j < args.length; j++) {
			setElem(cx, result, i, args[j]);
			i++;
		}

		while (i < newLen) {
			Object e = getElem(cx, source, r);
			setElem(cx, result, i, e);
			i++;
			r++;
		}

		return result;
	}

	private static Object js_with(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable source = ScriptRuntime.toObject(cx, scope, thisObj);

		long len = getLengthProperty(cx, source);
		long relativeIndex = args.length > 0 ? (int) ScriptRuntime.toInteger(cx, args[0]) : 0;
		long actualIndex = relativeIndex >= 0 ? relativeIndex : len + relativeIndex;

		if (actualIndex < 0 || actualIndex >= len) {
			throw ScriptRuntime.rangeError(cx, "index out of range");
		}
		if (len > Integer.MAX_VALUE) {
			String msg = ScriptRuntime.getMessage0("msg.arraylength.bad");
			throw ScriptRuntime.rangeError(cx, msg);
		}

		Scriptable result = cx.newArray(scope, (int) len);
		for (long k = 0; k < len; ++k) {
			Object value;
			if (k == actualIndex) {
				value = args.length > 1 ? args[1] : Undefined.INSTANCE;
			} else {
				value = getElem(cx, source, k);
			}
			setElem(cx, result, k, value);
		}

		return result;
	}

	// methods to implement java.util.List

	@Override
	public boolean contains(Object o) {
		return indexOf(o) > -1;
	}

	@Override
	public Object[] toArray() {
		return toArray(ScriptRuntime.EMPTY_OBJECTS);
	}

	@Override
	public Object[] toArray(Object[] a) {
		int len = size();
		Object[] array =
				a.length >= len
						? a
						: (Object[])
								java.lang.reflect.Array.newInstance(
										a.getClass().getComponentType(), len);
		for (int i = 0; i < len; i++) {
			array[i] = get(i);
		}
		return array;
	}

	@Override
	public boolean containsAll(Collection c) {
		for (Object aC : c) if (!contains(aC)) return false;
		return true;
	}

	@Override
	public int size() {
		long longLen = length;
		if (longLen > Integer.MAX_VALUE) {
			throw new IllegalStateException(
					"list.length (" + length + ") exceeds Integer.MAX_VALUE");
		}
		return (int) longLen;
	}

	@Override
	public boolean isEmpty() {
		return length == 0;
	}

	public Object get(long index) {
		if (index < 0 || index >= length) {
			throw new IndexOutOfBoundsException();
		}
		Object value = getRawElem(this, index, localContext);
		if (value == Scriptable.NOT_FOUND || value == Undefined.INSTANCE) {
			return null;
		} else if (value instanceof Wrapper) {
			return ((Wrapper) value).unwrap();
		} else {
			return value;
		}
	}

	@Override
	public Object get(int index) {
		return get((long) index);
	}

	@Override
	public int indexOf(Object o) {
		int len = size();
		if (o == null) {
			for (int i = 0; i < len; i++) {
				if (get(i) == null) {
					return i;
				}
			}
		} else {
			for (int i = 0; i < len; i++) {
				if (o.equals(get(i))) {
					return i;
				}
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		int len = size();
		if (o == null) {
			for (int i = len - 1; i >= 0; i--) {
				if (get(i) == null) {
					return i;
				}
			}
		} else {
			for (int i = len - 1; i >= 0; i--) {
				if (o.equals(get(i))) {
					return i;
				}
			}
		}
		return -1;
	}

	@Override
	public Iterator iterator() {
		return listIterator(0);
	}

	@Override
	public ListIterator listIterator() {
		return listIterator(0);
	}

	@Override
	public ListIterator listIterator(final int start) {
		final int len = size();

		if (start < 0 || start > len) {
			throw new IndexOutOfBoundsException("Index: " + start);
		}

		return new ListIterator() {

			int cursor = start;
			int modCount = NativeArray.this.modCount;

			@Override
			public boolean hasNext() {
				return cursor < len;
			}

			@Override
			public Object next() {
				checkModCount(modCount);
				if (cursor == len) {
					throw new NoSuchElementException();
				}
				return get(cursor++);
			}

			@Override
			public boolean hasPrevious() {
				return cursor > 0;
			}

			@Override
			public Object previous() {
				checkModCount(modCount);
				if (cursor == 0) {
					throw new NoSuchElementException();
				}
				return get(--cursor);
			}

			@Override
			public int nextIndex() {
				return cursor;
			}

			@Override
			public int previousIndex() {
				return cursor - 1;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void add(Object o) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void set(Object o) {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public boolean add(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, Object element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object set(int index, Object element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List subList(int fromIndex, int toIndex) {
		if (fromIndex < 0) throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
		if (toIndex > size()) throw new IndexOutOfBoundsException("toIndex = " + toIndex);
		if (fromIndex > toIndex)
			throw new IllegalArgumentException(
					"fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");

		return new AbstractList() {
			private int mc = NativeArray.this.modCount;

			@Override
			public Object get(int index) {
				checkModCount(mc);
				return NativeArray.this.get(index + fromIndex);
			}

			@Override
			public int size() {
				checkModCount(mc);
				return toIndex - fromIndex;
			}
		};
	}

	private void checkModCount(int modCount) {
		if (this.modCount != modCount) {
			throw new ConcurrentModificationException();
		}
	}

	/** Captured Context for the java.util.List bridge methods, which have no cx parameter. */
	private final Context localContext;

	/** Internal representation of the JavaScript array's length property. */
	private long length;

	/** Attributes of the array's length property */
	private int lengthAttr = DONTENUM | PERMANENT;

	/** modCount required for subList/iterators */
	private transient int modCount;

	/**
	 * Fast storage for dense arrays. Sparse arrays will use the superclass's hashtable storage
	 * scheme.
	 */
	private Object[] dense;

	/** True if all numeric properties are stored in <code>dense</code>. */
	private boolean denseOnly;

	/** The maximum size of <code>dense</code> that will be allocated initially. */
	private static int maximumInitialCapacity = 10000;

	/** The default capacity for <code>dense</code>. */
	private static final int DEFAULT_INITIAL_CAPACITY = 10;

	/** The factor to grow <code>dense</code> by. */
	private static final double GROW_FACTOR = 1.5;

	private static final int MAX_PRE_GROW_SIZE = (int) (Integer.MAX_VALUE / GROW_FACTOR);

	@Override
	public <T> T createDataObject(Supplier<T> instanceFactory, Context cx) {
		List<T> list = createDataObjectList(instanceFactory, cx);

		if (list.isEmpty()) {
			throw new ArrayIndexOutOfBoundsException("Array doesn't contain any objects");
		}

		return list.getFirst();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> createDataObjectList(Supplier<T> instanceFactory, Context cx) {
		List<T> list = new ArrayList<>();

		for (Object o : this) {
			if (o instanceof DataObject) {
				list.add(((DataObject) o).createDataObject(instanceFactory, cx));
			} else {
				list.add((T) o);
			}
		}

		return list;
	}

	@Override
	public boolean isDataObjectList() {
		return true;
	}
}
