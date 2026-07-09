/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * This is a specialization of property access using some lambda functions designed for properties
 * on built in objects that may be created extremely frequently. It is designed to expose a field on
 * a native Java object as a property via static methods that can get and set this value. Custom
 * operations are also supported for the setting of property attributes (as the class may need to
 * check these during internal operations) and redefinition of the property via a descriptor (as
 * array length has unusual behaviour in this respect).
 *
 * <p>Unlike {@link LambdaSlot}, a BuiltInSlot intercepts descriptor redefinition (via {@link
 * #applyNewDescriptor}) so that a property like array {@code length} keeps its special semantics
 * even when redefined with {@code Object.defineProperty}.
 *
 * <p>It will generate a plain data descriptor when a property descriptor is produced from it, as
 * the properties we might want to internalise do not necessarily have get and set functions.
 *
 * <p>Holding the {@code owner} on this object and passing it to the various accessor functions was a
 * design choice to reduce object creation, and to facilitate the separation of slots and
 * descriptors in future. We store it in the slot's value field as this is not used for any real
 * value storage on a built in slot.
 */
public class BuiltInSlot<T extends ScriptableObject> extends Slot {

	public interface Getter<U extends ScriptableObject> {
		Object apply(U builtIn, Scriptable start, Context cx);
	}

	public interface Setter<U extends ScriptableObject> {
		boolean apply(U builtIn, Object value, Scriptable owner, Scriptable start, boolean isThrow, Context cx);
	}

	public interface AttributeSetter<U extends ScriptableObject> {
		void apply(U builtIn, int attributes);
	}

	public interface PropDescriptionSetter<U extends ScriptableObject> {
		void apply(U builtIn, BuiltInSlot<U> current, Object id, ScriptableObject desc, boolean checkValid, Object key, int index, Context cx);
	}

	private final Getter<T> getter;
	private final Setter<T> setter;
	private final AttributeSetter<T> attrUpdater;
	private final PropDescriptionSetter<T> propDescSetter;

	BuiltInSlot(Object name, int index, int attr, T builtIn, Getter<T> getter, Setter<T> setter, AttributeSetter<T> attrUpdater) {
		this(name, index, attr, builtIn, getter, setter, attrUpdater, BuiltInSlot::defaultPropDescSetter);
	}

	BuiltInSlot(Object name, int index, int attr, T builtIn, Getter<T> getter, Setter<T> setter, AttributeSetter<T> attrUpdater, PropDescriptionSetter<T> propDescSetter) {
		super(name, index, attr);
		this.value = builtIn;
		this.getter = getter;
		this.setter = setter;
		this.attrUpdater = attrUpdater;
		this.propDescSetter = propDescSetter;
	}

	BuiltInSlot(BuiltInSlot<T> slot) {
		super(slot);
		this.getter = slot.getter;
		this.setter = slot.setter;
		this.attrUpdater = slot.attrUpdater;
		this.propDescSetter = slot.propDescSetter;
	}

	@Override
	Slot copySlot() {
		var res = new BuiltInSlot<>(this);
		res.next = null;
		res.orderedNext = null;
		return res;
	}

	@Override
	boolean isValueSlot() {
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	Object getValue(Scriptable start, Context cx) {
		return getter.apply((T) this.value, start, cx);
	}

	@Override
	@SuppressWarnings("unchecked")
	boolean setValue(Object value, Scriptable owner, Scriptable start, Context cx, boolean isThrow) {
		return setter.apply((T) this.value, value, owner, start, isThrow, cx);
	}

	@Override
	@SuppressWarnings("unchecked")
	void setAttributes(int value) {
		attrUpdater.apply((T) this.value, value);
		super.setAttributes(value);
	}

	@Override
	@SuppressWarnings("unchecked")
	ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
		return ScriptableObject.buildDataDescriptor(scope, getValue((T) this.value, cx), getAttributes(), cx);
	}

	@SuppressWarnings("unchecked")
	void applyNewDescriptor(Object id, ScriptableObject desc, boolean checkValid, Object key, int index, Context cx) {
		propDescSetter.apply((T) this.value, this, id, desc, checkValid, key, index, cx);
	}

	private static <T extends ScriptableObject> void defaultPropDescSetter(T builtIn, BuiltInSlot<T> current, Object id, ScriptableObject desc, boolean checkValid, Object key, int index, Context cx) {
		builtIn.defineOrdinaryProperty(cx, id, desc, checkValid, key, index);
	}
}
