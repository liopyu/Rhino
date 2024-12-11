/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This is a specialization of property access using some lambda functions. It behaves exactly like
 * any other slot that has only a value, but instead of getting the value directly, it comes from
 * calling the functions. This makes it different from AccessorSlot, which lets the user see directly
 * that there is a getter or a setter function involved. This makes this class useful for
 * implementing properties that behave like any other JavaScript property but which are implemented
 * using some native functionality without using reflection.
 */
public class LambdaSlot extends Slot {
	transient Supplier<Object> getter;
	transient Consumer<Object> setter;

	LambdaSlot(Object name, int index) {
		super(name, index, 0);
	}

	LambdaSlot(Slot oldSlot) {
		super(oldSlot);
	}

	@Override
	LambdaSlot copySlot() {
		var newSlot = new LambdaSlot(this);
		newSlot.value = value;
		newSlot.getter = getter;
		newSlot.setter = setter;
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
		return false;
	}

	@Override
	ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
		ScriptableObject desc = new NativeObject(cx.factory);
		ScriptRuntime.setBuiltinProtoAndParent(cx, scope, desc, TopLevel.Builtins.Object);
		if (getter != null) {
			desc.defineProperty(cx, "value", getter.get(), ScriptableObject.EMPTY);
		} else {
			desc.defineProperty(cx, "value", value, ScriptableObject.EMPTY);
		}
		desc.setCommonDescriptorProperties(cx, getAttributes(), true);
		return desc;
	}

	@Override
	boolean setValue(Object value, Scriptable owner, Scriptable start, Context cx, boolean isThrow) {
		if (setter != null) {
			if (owner == start) {
				setter.accept(value);
				return true;
			}
			return false;
		}
		return super.setValue(value, owner, start, cx, isThrow);
	}

	@Override
	Object getValue(Scriptable start, Context cx) {
		if (getter != null) {
			return getter.get();
		}
		return super.getValue(start, cx);
	}
}
