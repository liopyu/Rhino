/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * This is a specialization of LambdaSlot where the getter and setter are passed the actual
 * receiving object (rather than always acting on a single captured value). This makes it
 * useful for implementing properties on shared prototypes where each instance needs its own
 * backing state, unlike LambdaSlot whose setter is only invoked when the property is defined
 * directly on the object being assigned to.
 */
public class LambdaAccessorSlot extends Slot {
	transient Function<Scriptable, Object> getter;
	transient BiConsumer<Scriptable, Object> setter;
	private LambdaFunction getterFunction;
	private LambdaFunction setterFunction;

	LambdaAccessorSlot(Object name, int index) {
		super(name, index, 0);
	}

	LambdaAccessorSlot(Slot oldSlot) {
		super(oldSlot);
	}

	@Override
	LambdaAccessorSlot copySlot() {
		var newSlot = new LambdaAccessorSlot(this);
		newSlot.value = value;
		newSlot.getter = getter;
		newSlot.setter = setter;
		newSlot.getterFunction = getterFunction;
		newSlot.setterFunction = setterFunction;
		newSlot.next = null;
		newSlot.orderedNext = null;
		return newSlot;
	}

	@Override
	boolean isValueSlot() {
		return false;
	}

	@Override
	boolean isSetterSlot() {
		return true;
	}

	@Override
	ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
		return buildPropertyDescriptor(cx);
	}

	/**
	 * The method exists to avoid changing the getPropertyDescriptor signature and at the same time
	 * to make it explicit that we don't use the Scriptable scope parameter of getPropertyDescriptor,
	 * since it can be problematic when called from inside a slotMap.compute lambda.
	 */
	public ScriptableObject buildPropertyDescriptor(Context cx) {
		ScriptableObject desc = new NativeObject(cx.factory);
		desc.setCommonDescriptorProperties(cx, getAttributes(), getterFunction == null && setterFunction == null);
		if (getterFunction != null) {
			desc.defineProperty(cx, "get", getterFunction, ScriptableObject.EMPTY);
		}
		if (setterFunction != null) {
			desc.defineProperty(cx, "set", setterFunction, ScriptableObject.EMPTY);
		}
		return desc;
	}

	@Override
	boolean setValue(Object value, Scriptable owner, Scriptable start, Context cx, boolean isThrow) {
		if (setter == null) {
			if (getter != null) {
				if (isThrow) {
					throwNoSetterException(cx, start, value);
				}
				return true;
			}
		} else {
			setter.accept(start, value);
			return true;
		}
		return super.setValue(value, owner, start, cx, isThrow);
	}

	@Override
	Object getValue(Scriptable start, Context cx) {
		if (getter != null) {
			return getter.apply(start);
		}
		return super.getValue(start, cx);
	}

	public void setGetter(Context cx, Scriptable scope, Function<Scriptable, Object> getter) {
		this.getter = getter;
		if (getter != null) {
			Function<Scriptable, Object> g = getter;
			getterFunction = new LambdaFunction(cx, scope, "get " + name, 0, (cx1, scope1, thisObj, args) -> g.apply(thisObj));
		}
	}

	public void setSetter(Context cx, Scriptable scope, BiConsumer<Scriptable, Object> setter) {
		this.setter = setter;
		if (setter != null) {
			BiConsumer<Scriptable, Object> s = setter;
			setterFunction = new LambdaFunction(cx, scope, "set " + name, 1, (cx1, scope1, thisObj, args) -> {
				s.accept(thisObj, args[0]);
				return Undefined.INSTANCE;
			});
		}
	}

	public void replaceWith(LambdaAccessorSlot slot) {
		this.getterFunction = slot.getterFunction;
		this.getter = slot.getter;
		this.setterFunction = slot.setterFunction;
		this.setter = slot.setter;
		setAttributes(slot.getAttributes());
	}
}
