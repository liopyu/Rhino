/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * A Slot is the base class for all properties stored in the ScriptableObject class. There are a
 * number of different types of slots. This base class represents an "ordinary" property such as
 * one that is created with a plain value.
 */
public class Slot {
	Object name; // This can change due to caching
	int indexOrHash;
	Object value;
	transient Slot next; // next in hash table bucket
	transient Slot orderedNext; // next in linked list
	private short attributes;

	Slot(Object name, int index, int attributes) {
		this.name = name;
		this.indexOrHash = name == null ? index : name.hashCode();
		this.attributes = (short) attributes;
	}

	/**
	 * Copy constructor, used when "upgrading" a plain Slot to a more specialized subclass
	 * (e.g. {@link AccessorSlot} or {@link LambdaSlot}), or when "downgrading" an accessor
	 * slot back to a plain Slot.
	 */
	protected Slot(Slot oldSlot) {
		name = oldSlot.name;
		indexOrHash = oldSlot.indexOrHash;
		attributes = oldSlot.attributes;
		value = oldSlot.value;
		next = oldSlot.next;
		orderedNext = oldSlot.orderedNext;
	}

	Slot copySlot() {
		var newSlot = new Slot(this);
		newSlot.next = null;
		newSlot.orderedNext = null;
		return newSlot;
	}

	/**
	 * Return true if this is a base-class "Slot". Sadly too much code breaks if we try to do this
	 * any other way.
	 */
	boolean isValueSlot() {
		return true;
	}

	/**
	 * Return true if this is a "setter slot", which we need to know for some legacy support.
	 */
	boolean isSetterSlot() {
		return false;
	}

	boolean setValue(Object value, Scriptable owner, Scriptable start, Context cx) {
		return setValue(value, owner, start, cx, cx.isStrictMode());
	}

	boolean setValue(Object value, Scriptable owner, Scriptable start, Context cx, boolean isThrow) {
		if ((attributes & ScriptableObject.READONLY) != 0) {
			if (isThrow) {
				throw ScriptRuntime.typeError1(cx, "msg.modify.readonly", name);
			}
			return true;
		}
		if (owner == start) {
			this.value = value;
			return true;
		}
		return false;
	}

	Object getValue(Scriptable start, Context cx) {
		return value;
	}

	int getAttributes() {
		return attributes;
	}

	void setAttributes(int value) {
		ScriptableObject.checkValidAttributes(value);
		attributes = (short) value;
	}

	ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
		return ScriptableObject.buildDataDescriptor(scope, value, attributes, cx);
	}

	protected void throwNoSetterException(Context cx, Scriptable start, Object newValue) {
		String prop = "";
		if (name != null) {
			prop = "[" + start.getClassName() + "]." + name;
		}
		throw ScriptRuntime.typeError2(cx, "msg.set.prop.no.setter", prop, ScriptRuntime.toString(cx, newValue));
	}

	/**
	 * Return a JavaScript function that represents the "getter". This is used by some legacy
	 * functionality. Return null if there is no getter.
	 */
	Function getGetterFunction(Context cx, String name, Scriptable scope) {
		return null;
	}

	/**
	 * Same for the "setter."
	 */
	Function getSetterFunction(Context cx, String name, Scriptable scope) {
		return null;
	}

	/**
	 * Compare the JavaScript function that represents the "setter" to the provided Object. We do
	 * this to avoid generating a new function object when it might not be required. Specifically,
	 * if we have a cached function object that has not yet been generated then we don't have to
	 * generate it because it cannot be the same as the provided function.
	 */
	boolean isSameSetterFunction(Context cx, Object function) {
		return false;
	}

	/** Same for the "getter" function. */
	boolean isSameGetterFunction(Context cx, Object function) {
		return false;
	}
}
