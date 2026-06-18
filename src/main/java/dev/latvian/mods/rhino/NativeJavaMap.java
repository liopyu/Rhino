/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.Deletable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class NativeJavaMap extends NativeJavaObject {
	static void init(ScriptableObject scope, boolean sealed, Context cx) {
		NativeJavaMapIterator.init(scope, sealed, cx);
	}

	public final Map map;
	public final TypeInfo mapKeyType;
	public final TypeInfo mapValueType;

	public NativeJavaMap(Context cx, Scriptable scope, Object jo, Map map, TypeInfo type) {
		super(scope, jo, type, cx);
		this.map = map;
		this.mapKeyType = type.param(0);
		this.mapValueType = type.param(1);
	}

	@Override
	public String getClassName() {
		return "JavaMap";
	}

	private static final Object INVALID_KEY = new Object();

	private Object toMapKey(Context cx, Object jsKey) {
		try {
			return cx.jsToJava(jsKey, mapKeyType);
		} catch (Exception ex) {
			return INVALID_KEY;
		}
	}

	/**
	 * Instead of upstream, which deliberately only supports {@code String} or {@code Integer} as key,
	 * we use {@link #toMapKey} to try and convert the input first.
	 */
	private boolean safeHas(Object key) {
		if (key == INVALID_KEY) {
			return false;
		}

		try {
			return map.containsKey(key);
		} catch (ClassCastException | NullPointerException ex) {
			return false;
		}
	}

	@Override
	public boolean has(Context cx, String name, Scriptable start) {
		if (safeHas(toMapKey(cx, name))) {
			return true;
		}
		return super.has(cx, name, start);
	}

	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		if (safeHas(toMapKey(cx, index))) {
			return true;
		}
		return super.has(cx, index, start);
	}

	@Override
	public boolean has(Context cx, Symbol key, Scriptable start) {
		return SymbolKey.ITERATOR.equals(key);
	}

	@Override
	public Object get(Context cx, String name, Scriptable start) {
		Object key = toMapKey(cx, name);
		if (safeHas(key)) {
			return cx.javaToJS(map.get(key), start, mapValueType);
		}
		return super.get(cx, name, start);
	}

	@Override
	public Object get(Context cx, int index, Scriptable start) {
		Object key = toMapKey(cx, index);
		if (safeHas(key)) {
			return cx.javaToJS(map.get(key), start, mapValueType);
		}
		return super.get(cx, index, start);
	}

	@Override
	public Object get(Context cx, Symbol key, Scriptable start) {
		if (SymbolKey.ITERATOR.equals(key)) {
			return symbol_iterator;
		}
		return super.get(cx, key, start);
	}

	@Override
	public void put(Context cx, String name, Scriptable start, Object value) {
		map.put(cx.jsToJava(name, mapKeyType), cx.jsToJava(value, mapValueType));
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		map.put(cx.jsToJava(index, mapKeyType), cx.jsToJava(value, mapValueType));
	}

	@Override
	public Object[] getIds(Context cx) {
		List<Object> ids = new ArrayList<>(map.size());
		for (Object key : map.keySet()) {
			if (key instanceof Integer) {
				ids.add(key);
			} else {
				ids.add(ScriptRuntime.toString(cx, key));
			}
		}
		return ids.toArray();
	}

	@Override
	public void delete(Context cx, String name) {
		Object key = toMapKey(cx, name);
		if (safeHas(key)) {
			Deletable.deleteObject(map.remove(key));
		}
	}

	@Override
	public void delete(Context cx, int index) {
		Object key = toMapKey(cx, index);
		if (safeHas(key)) {
			Deletable.deleteObject(map.remove(key));
		}
	}

	@Override
	protected void initMembers(Context cx, Scriptable scope) {
		super.initMembers(cx, scope);
		addCustomFunction("hasOwnProperty", TypeInfo.BOOLEAN, this::hasOwnProperty, TypeInfo.STRING);
	}

	private boolean hasOwnProperty(Context cx, Object[] args) {
		return safeHas(toMapKey(cx, ScriptRuntime.toString(cx, args[0])));
	}

	private static final Callable symbol_iterator = (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) -> {
		if (!(thisObj instanceof NativeJavaMap njm)) {
			throw ScriptRuntime.typeError1(cx, "msg.incompat.call", SymbolKey.ITERATOR);
		}
		return new NativeJavaMapIterator(cx, scope, njm);
	};

	/**
	 * Symbol.iterator implementation for NativeJavaMap: yields a JS array
	 * {@code [key, value]} for each entry in the wrapped Map.
	 */
	private static final class NativeJavaMapIterator extends ES6Iterator {
		private static final String ITERATOR_TAG = "JavaMapIterator";

		static void init(ScriptableObject scope, boolean sealed, Context cx) {
			init(scope, sealed, new NativeJavaMapIterator(), ITERATOR_TAG, cx);
		}

		/**
		 * Only for constructing the prototype object.
		 */
		private NativeJavaMapIterator() {
			super();
			this.iterator = Collections.emptyIterator();
			this.keyType = TypeInfo.NONE;
			this.valueType = TypeInfo.NONE;
		}

		NativeJavaMapIterator(Context cx, Scriptable scope, NativeJavaMap njm) {
			super(scope, ITERATOR_TAG, cx);
			this.iterator = njm.map.entrySet().iterator();
			this.keyType = njm.mapKeyType;
			this.valueType = njm.mapValueType;
		}

		@Override
		public String getClassName() {
			return "Java Map Iterator";
		}

		@Override
		protected boolean isDone(Context cx, Scriptable scope) {
			return !iterator.hasNext();
		}

		@Override
		protected Object nextValue(Context cx, Scriptable scope) {
			if (!iterator.hasNext()) {
				return cx.newArray(scope, new Object[]{Undefined.INSTANCE, Undefined.INSTANCE});
			}
			Map.Entry e = iterator.next();
			Object key = cx.javaToJS(e.getKey(), scope, keyType);
			Object value = cx.javaToJS(e.getValue(), scope, valueType);
			return cx.newArray(scope, new Object[]{key, value});
		}

		@Override
		protected String getTag() {
			return ITERATOR_TAG;
		}

		private final Iterator<Map.Entry> iterator;
		private final TypeInfo keyType;
		private final TypeInfo valueType;
	}
}
