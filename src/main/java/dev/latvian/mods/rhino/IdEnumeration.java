package dev.latvian.mods.rhino;

/**
 * This is the enumeration needed by the for..in statement.
 * <p>
 * See ECMA 12.6.3.
 * <p>
 * IdEnumeration maintains a ObjToIntMap to make sure a given
 * id is enumerated only once across multiple objects in a
 * prototype chain.
 * <p>
 * XXX - ECMA delete doesn't hide properties in the prototype,
 * but js/ref does. This means that the js/ref for..in can
 * avoid maintaining a hash table and instead perform lookups
 * to see if a given property has already been enumerated.
 */
public class IdEnumeration {
	Scriptable obj;
	Object[] ids;
	ObjToIntMap used;
	Object currentId;
	int index;
	int enumType; /* one of ENUM_INIT_KEYS, ENUM_INIT_VALUES, ENUM_INIT_ARRAY, ENUMERATE_VALUES_IN_ORDER */
	// if true, integer ids will be returned as numbers rather than strings
	boolean enumNumbers;
	Scriptable iterator;

	public Boolean next(Context cx) {
		if (iterator != null) {
			if (enumType == ScriptRuntime.ENUMERATE_VALUES_IN_ORDER) {
				return enumNextInOrder(cx);
			}

			Object v = ScriptableObject.getProperty(iterator, ES6Iterator.NEXT_METHOD, cx);
			if (!(v instanceof Callable f)) {
				return Boolean.FALSE;
			}
			try {
				currentId = f.call(cx, iterator.getParentScope(), iterator, ScriptRuntime.EMPTY_OBJECTS);
				return Boolean.TRUE;
			} catch (JavaScriptException e) {
				if (e.getValue() instanceof NativeIterator.StopIteration) {
					return Boolean.FALSE;
				}
				throw e;
			}
		}

		for (; ; ) {
			if (obj == null) {
				return Boolean.FALSE;
			}
			if (index == ids.length) {
				obj = obj.getPrototype(cx);
				changeObject(cx);
				continue;
			}
			Object id = ids[index++];
			if (used != null && used.has(id)) {
				continue;
			}
			if (id instanceof Symbol) {
				continue;
			} else if (id instanceof String strId) {
				if (!obj.has(cx, strId, obj)) {
					continue;   // must have been deleted
				}
				currentId = strId;
			} else {
				int intId = ((Number) id).intValue();
				if (!obj.has(cx, intId, obj)) {
					continue;   // must have been deleted
				}
				currentId = enumNumbers ? Integer.valueOf(intId) : String.valueOf(intId);
			}
			return Boolean.TRUE;
		}
	}

	private Boolean enumNextInOrder(Context cx) {
		Object v = ScriptableObject.getProperty(iterator, ES6Iterator.NEXT_METHOD, cx);
		if (!(v instanceof Callable f)) {
			throw ScriptRuntime.notFunctionError(cx, iterator, ES6Iterator.NEXT_METHOD);
		}
		Scriptable scope = iterator.getParentScope();
		Object r = f.call(cx, scope, iterator, ScriptRuntime.EMPTY_OBJECTS);
		Scriptable iteratorResult = ScriptRuntime.toObject(cx, scope, r);
		Object done = ScriptableObject.getProperty(iteratorResult, ES6Iterator.DONE_PROPERTY, cx);
		if (done != Scriptable.NOT_FOUND && ScriptRuntime.toBoolean(cx, done)) {
			return Boolean.FALSE;
		}
		currentId = ScriptableObject.getProperty(iteratorResult, ES6Iterator.VALUE_PROPERTY, cx);
		return Boolean.TRUE;
	}

	public void changeObject(Context cx) {
		Object[] nids = null;
		while (obj != null) {
			nids = obj.getIds(cx);
			if (nids.length != 0) {
				break;
			}
			obj = obj.getPrototype(cx);
		}
		if (obj != null && ids != null) {
			Object[] previous = ids;
			int L = previous.length;
			if (used == null) {
				used = new ObjToIntMap(L);
			}
			for (int i = 0; i != L; ++i) {
				used.intern(previous[i]);
			}
		}
		ids = nids;
		index = 0;
	}

	public Object getId(Context cx) {
		if (iterator != null) {
			return currentId;
		}

		switch (enumType) {
			case ScriptRuntime.ENUMERATE_KEYS:
			case ScriptRuntime.ENUMERATE_KEYS_NO_ITERATOR:
				return currentId;
			case ScriptRuntime.ENUMERATE_VALUES:
			case ScriptRuntime.ENUMERATE_VALUES_NO_ITERATOR:
				return getValue(cx);
			case ScriptRuntime.ENUMERATE_ARRAY:
			case ScriptRuntime.ENUMERATE_ARRAY_NO_ITERATOR:
				Object[] elements = {currentId, getValue(cx)};
				return cx.newArray(ScriptableObject.getTopLevelScope(obj), elements);
			default:
				throw Kit.codeBug();
		}
	}

	public Object getValue(Context cx) {
		Object result;

		if (ScriptRuntime.isSymbol(currentId)) {
			SymbolScriptable so = ScriptableObject.ensureSymbolScriptable(obj, cx);
			result = so.get(cx, (Symbol) currentId, obj);
		} else {
			ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, currentId);
			if (s.stringId == null) {
				result = obj.get(cx, s.index, obj);
			} else {
				result = obj.get(cx, s.stringId, obj);
			}
		}

		return result;
	}

	public Object nextExec(Context cx, Scriptable scope) {
		Boolean b = next(cx);

		if (!b) {
			// Out of values. Throw StopIteration.
			throw new JavaScriptException(cx, NativeIterator.getStopIterationObject(scope, cx), null, 0);
		}

		return getId(cx);
	}
}
