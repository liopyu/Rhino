/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * This is a specialization of Slot to store various types of values that are retrieved dynamically
 * using Java and JavaScript functions. Unlike LambdaSlot, the fact that these values are accessed
 * and mutated by functions is visible via the slot's property descriptor.
 * <p>
 * The getter and setter may each be backed by a different mechanism (a JavaScript function or a
 * Java reflection-based {@link MemberBox}), which is why they are wrapped in the {@link Getter} /
 * {@link Setter} abstraction rather than stored as a single slot type. This avoids the regression
 * where a property could not have, e.g., a Java-based getter and a JavaScript-based setter.
 */
public class AccessorSlot extends Slot {
	transient Getter getter;
	transient Setter setter;

	AccessorSlot(Object name, int index) {
		super(name, index, 0);
	}

	AccessorSlot(Slot oldSlot) {
		super(oldSlot);
	}

	@Override
	AccessorSlot copySlot() {
		var newSlot = new AccessorSlot(this);
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
		return true;
	}

	@Override
	ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
		// It sounds logical that this would be the same as the logic for a normal Slot,
		// but the spec is super pedantic about things like the order of properties here,
		// so we need special support here.
		ScriptableObject desc = new NativeObject(cx.factory);
		ScriptRuntime.setBuiltinProtoAndParent(cx, scope, desc, TopLevel.Builtins.Object);
		desc.setCommonDescriptorProperties(cx, getAttributes(), getter == null && setter == null);
		String fName = name == null ? "f" : name.toString();
		if (getter != null) {
			Function f = getter.asGetterFunction(cx, fName, scope);
			desc.defineProperty(cx, "get", f == null ? Undefined.INSTANCE : f, ScriptableObject.EMPTY);
		}
		if (setter != null) {
			Function f = setter.asSetterFunction(cx, fName, scope);
			desc.defineProperty(cx, "set", f == null ? Undefined.INSTANCE : f, ScriptableObject.EMPTY);
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
				// Assignment to a property with only a getter defined. The
				// assignment is ignored. See bug 478047.
				return true;
			}
		} else {
			return setter.setValue(value, owner, start, cx);
		}
		return super.setValue(value, owner, start, cx, isThrow);
	}

	@Override
	Object getValue(Scriptable start, Context cx) {
		if (getter != null) {
			return getter.getValue(start, cx);
		}
		return super.getValue(start, cx);
	}

	@Override
	Function getGetterFunction(Context cx, String name, Scriptable scope) {
		return getter == null ? null : getter.asGetterFunction(cx, name, scope);
	}

	@Override
	Function getSetterFunction(Context cx, String name, Scriptable scope) {
		return setter == null ? null : setter.asSetterFunction(cx, name, scope);
	}

	@Override
	boolean isSameGetterFunction(Context cx, Object function) {
		if (function == Scriptable.NOT_FOUND) {
			return true;
		}
		if (getter == null) {
			return ScriptRuntime.shallowEq(cx, Undefined.INSTANCE, function);
		}
		return getter.isSameGetterFunction(cx, function);
	}

	@Override
	boolean isSameSetterFunction(Context cx, Object function) {
		if (function == Scriptable.NOT_FOUND) {
			return true;
		}
		if (setter == null) {
			return ScriptRuntime.shallowEq(cx, Undefined.INSTANCE, function);
		}
		return setter.isSameSetterFunction(cx, function);
	}

	abstract static class Getter {
		abstract Object getValue(Scriptable start, Context cx);

		abstract Function asGetterFunction(Context cx, String name, Scriptable scope);

		abstract boolean isSameGetterFunction(Context cx, Object getter);
	}

	/** This is a Getter that delegates to a Java function via a MemberBox. */
	static final class MemberBoxGetter extends Getter {
		final MemberBox member;

		MemberBoxGetter(MemberBox member) {
			this.member = member;
		}

		@Override
		Object getValue(Scriptable start, Context cx) {
			Object getterThis;
			Object[] args;
			if (member.delegateTo == null) {
				getterThis = start;
				args = ScriptRuntime.EMPTY_OBJECTS;
			} else {
				getterThis = member.delegateTo;
				args = new Object[]{cx, start};
			}
			return member.invoke(getterThis, args, cx, start);
		}

		@Override
		Function asGetterFunction(Context cx, String name, Scriptable scope) {
			return member.asGetterFunction(cx, name, scope);
		}

		@Override
		boolean isSameGetterFunction(Context cx, Object function) {
			return member.isSameGetterFunction(cx, function);
		}
	}

	/** This is a getter that delegates to a JavaScript function. */
	static final class FunctionGetter extends Getter {
		// The value of the function might actually be Undefined, so we need an Object here.
		final Object target;

		FunctionGetter(Object target) {
			this.target = target;
		}

		@Override
		Object getValue(Scriptable start, Context cx) {
			if (target instanceof Function t) {
				return t.call(cx, t.getParentScope(), start, ScriptRuntime.EMPTY_OBJECTS);
			}
			return Undefined.INSTANCE;
		}

		@Override
		Function asGetterFunction(Context cx, String name, Scriptable scope) {
			return target instanceof Function ? (Function) target : null;
		}

		@Override
		boolean isSameGetterFunction(Context cx, Object function) {
			return ScriptRuntime.shallowEq(cx, target instanceof Function ? (Function) target : Undefined.INSTANCE, function);
		}
	}

	abstract static class Setter {
		abstract boolean setValue(Object value, Scriptable owner, Scriptable start, Context cx);

		abstract Function asSetterFunction(Context cx, String name, Scriptable scope);

		abstract boolean isSameSetterFunction(Context cx, Object getter);
	}

	/** Invoke the setter on this slot via reflection using MemberBox. */
	static final class MemberBoxSetter extends Setter {
		final MemberBox member;

		MemberBoxSetter(MemberBox member) {
			this.member = member;
		}

		@Override
		boolean setValue(Object value, Scriptable owner, Scriptable start, Context cx) {
			var pTypes = member.parameters().types();
			// XXX: cache tag since it is already calculated in
			// defineProperty ?
			Class<?> valueType = pTypes.getLast();
			int tag = FunctionObject.getTypeTag(valueType);
			Object actualArg = FunctionObject.convertArg(cx, start, value, tag);
			if (member.delegateTo == null) {
				member.invoke(start, new Object[]{actualArg}, cx, start);
			} else {
				member.invoke(member.delegateTo, new Object[]{cx, start, actualArg}, cx, start);
			}
			return true;
		}

		@Override
		Function asSetterFunction(Context cx, String name, Scriptable scope) {
			return member.asSetterFunction(cx, name, scope);
		}

		@Override
		boolean isSameSetterFunction(Context cx, Object function) {
			return member.isSameSetterFunction(cx, function);
		}
	}

	/**
	 * Invoke the setter as a JavaScript function, taking care that it might actually be Undefined.
	 */
	static final class FunctionSetter extends Setter {
		final Object target;

		FunctionSetter(Object target) {
			this.target = target;
		}

		@Override
		boolean setValue(Object value, Scriptable owner, Scriptable start, Context cx) {
			if (target instanceof Function t) {
				t.call(cx, t.getParentScope(), start, new Object[]{value});
			}
			return true;
		}

		@Override
		Function asSetterFunction(Context cx, String name, Scriptable scope) {
			return target instanceof Function ? (Function) target : null;
		}

		@Override
		boolean isSameSetterFunction(Context cx, Object function) {
			return ScriptRuntime.shallowEq(cx, target instanceof Function ? (Function) target : Undefined.INSTANCE, function);
		}
	}
}
