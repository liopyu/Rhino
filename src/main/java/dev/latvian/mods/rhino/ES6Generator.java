/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

public final class ES6Generator extends ScriptableObject {
	private static final Object GENERATOR_TAG = "Generator";

	public static final class YieldStarResult {
		private final Object result;

		public YieldStarResult(Object result) {
			this.result = result;
		}

		Object getResult() {
			return result;
		}
	}

	static ES6Generator init(ScriptableObject scope, boolean sealed, Context cx) {

		ES6Generator prototype = new ES6Generator();
		if (scope != null) {
			prototype.setParentScope(scope);
			prototype.setPrototype(getObjectPrototype(scope, cx));
		}

		// Define prototype methods using LambdaFunction
		prototype.defineProperty(cx, "next", new LambdaFunction(cx, scope, "next", 1, ES6Generator::js_next, false), DONTENUM);
		prototype.defineProperty(cx, "return", new LambdaFunction(cx, scope, "return", 1, ES6Generator::js_return, false), DONTENUM);
		prototype.defineProperty(cx, "throw", new LambdaFunction(cx, scope, "throw", 1, ES6Generator::js_throw, false), DONTENUM);
		prototype.defineProperty(cx, SymbolKey.ITERATOR, new LambdaFunction(cx, scope, "[Symbol.iterator]", 0, ES6Generator::js_iterator, false), DONTENUM);

		if (sealed) {
			prototype.sealObject(cx);
		}

		// Need to access Generator prototype when constructing
		// Generator instances, but don't have a generator constructor
		// to use to find the prototype. Use the "associateValue"
		// approach instead.
		if (scope != null) {
			scope.associateValue(GENERATOR_TAG, prototype);
		}

		return prototype;
	}

	private NativeFunction function;
	private Object savedState;
	private String lineSource;
	private int lineNumber;
	private State state = State.SUSPENDED_START;
	private Object delegee;


	/**
	 * Only for constructing the prototype object.
	 */
	private ES6Generator() {
	}

	public ES6Generator(Scriptable scope, NativeFunction function, Object savedState, Context cx) {
		this.function = function;
		this.savedState = savedState;
		// Set parent and prototype properties. Since we don't have a
		// "Generator" constructor in the top scope, we stash the
		// prototype in the top scope's associated value.
		Scriptable top = ScriptableObject.getTopLevelScope(scope);
		this.setParentScope(top);
		ES6Generator prototype = (ES6Generator) ScriptableObject.getTopScopeValue(top, GENERATOR_TAG, cx);
		this.setPrototype(prototype);
	}

	@Override
	public String getClassName() {
		return "Generator";
	}

	private static ES6Generator realThis(Context cx, Scriptable thisObj) {
		return LambdaConstructor.convertThisObject(cx, thisObj, ES6Generator.class);
	}

	private static Object js_next(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		ES6Generator generator = realThis(cx, thisObj);
		Object value = args.length >= 1 ? args[0] : Undefined.INSTANCE;
		if (generator.delegee == null) {
			return generator.resumeLocal(cx, scope, value);
		}
		return generator.resumeDelegee(cx, scope, value);
	}

	private static Object js_return(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		ES6Generator generator = realThis(cx, thisObj);
		Object value = args.length >= 1 ? args[0] : Undefined.INSTANCE;
		if (generator.delegee == null) {
			return generator.resumeAbruptLocal(cx, scope, GeneratorState.GENERATOR_CLOSE, value);
		}
		return generator.resumeDelegeeReturn(cx, scope, value);
	}

	private static Object js_throw(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		ES6Generator generator = realThis(cx, thisObj);
		Object value = args.length >= 1 ? args[0] : Undefined.INSTANCE;
		if (generator.delegee == null) {
			return generator.resumeAbruptLocal(cx, scope, GeneratorState.GENERATOR_THROW, value);
		}
		return generator.resumeDelegeeThrow(cx, scope, value);
	}

	private static Object js_iterator(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return thisObj;
	}

	private Scriptable resumeDelegee(Context cx, Scriptable scope, Object value) {
		try {
			// Be super-careful and only pass an arg to next if it expects one
			Object[] nextArgs = Undefined.INSTANCE.equals(value) ? ScriptRuntime.EMPTY_OBJECTS : new Object[]{value};

			Callable nextFn = ScriptRuntime.getPropFunctionAndThis(cx, scope, delegee, ES6Iterator.NEXT_METHOD);
			Scriptable nextThis = cx.lastStoredScriptable();
			Object nr = cx.callSync(nextFn, scope, nextThis, nextArgs);

			Scriptable nextResult = ScriptableObject.ensureScriptable(nr, cx);
			if (ScriptRuntime.isIteratorDone(cx, nextResult)) {
				// Iterator is "done".
				delegee = null;
				// Return a result to the original generator
				return resumeLocal(cx, scope, ScriptableObject.getProperty(nextResult, ES6Iterator.VALUE_PROPERTY, cx));
			}
			// Otherwise, we have a normal result and should continue
			return nextResult;

		} catch (RhinoException re) {
			// Exceptions from the delegee should be handled by the enclosing
			// generator, including if they're because functions can't be found.
			delegee = null;
			return resumeAbruptLocal(cx, scope, GeneratorState.GENERATOR_THROW, re);
		}
	}


	private Scriptable resumeDelegeeThrow(Context cx, Scriptable scope, Object value) {
		boolean returnCalled = false;
		try {
			// Delegate to "throw" method. If it's not defined we'll get an error here.
			Callable throwFn = ScriptRuntime.getPropFunctionAndThis(cx, scope, delegee, "throw");
			Scriptable nextThis = cx.lastStoredScriptable();
			Object throwResult = cx.callSync(throwFn, scope, nextThis, new Object[]{value});

			if (ScriptRuntime.isIteratorDone(cx, throwResult)) {
				// Iterator is "done".
				try {
					// Return a result to the original generator, but first optionally call "return"
					returnCalled = true;
					callReturnOptionally(cx, scope, Undefined.INSTANCE);
				} finally {
					delegee = null;
				}
				return resumeLocal(cx, scope, ScriptRuntime.getObjectProp(cx, scope, throwResult, ES6Iterator.VALUE_PROPERTY));
			}
			// Otherwise, we have a normal result and should continue
			return ensureScriptable(throwResult, cx);

		} catch (RhinoException re) {
			// Handle all exceptions, including missing methods, by delegating to original.
			try {
				if (!returnCalled) {
					try {
						callReturnOptionally(cx, scope, Undefined.INSTANCE);
					} catch (RhinoException re2) {
						return resumeAbruptLocal(cx, scope, GeneratorState.GENERATOR_THROW, re2);
					}
				}
			} finally {
				delegee = null;
			}
			return resumeAbruptLocal(cx, scope, GeneratorState.GENERATOR_THROW, re);
		}
	}

	private Scriptable resumeDelegeeReturn(Context cx, Scriptable scope, Object value) {
		try {
			// Call "return" but don't throw if it can't be found
			Object retResult = callReturnOptionally(cx, scope, value);
			if (retResult != null) {
				if (ScriptRuntime.isIteratorDone(cx, retResult)) {
					// Iterator is "done".
					delegee = null;
					// Return a result to the original generator
					return resumeAbruptLocal(cx, scope, GeneratorState.GENERATOR_CLOSE, ScriptRuntime.getObjectPropNoWarn(cx, scope, retResult, ES6Iterator.VALUE_PROPERTY));
				} else {
					// Not actually done yet!
					return ensureScriptable(retResult, cx);
				}
			}

			// No "return" -- let the original iterator return the value.
			delegee = null;
			return resumeAbruptLocal(cx, scope, GeneratorState.GENERATOR_CLOSE, value);

		} catch (RhinoException re) {
			// Exceptions from the delegee should be handled by the enclosing
			// generator, including if they're because functions can't be found.
			delegee = null;
			return resumeAbruptLocal(cx, scope, GeneratorState.GENERATOR_THROW, re);
		}
	}

	private Scriptable resumeLocal(Context cx, Scriptable scope, Object value) {
		if (state == State.COMPLETED) {
			return ES6Iterator.makeIteratorResult(cx, scope, Boolean.TRUE);
		}
		if (state == State.EXECUTING) {
			throw ScriptRuntime.typeError0(cx, "msg.generator.executing");
		}

		Scriptable result = ES6Iterator.makeIteratorResult(cx, scope, Boolean.FALSE);
		state = State.EXECUTING;

		try {
			Object r = function.resumeGenerator(cx, scope, GeneratorState.GENERATOR_SEND, savedState, value);

			if (r instanceof YieldStarResult ysResult) {
				// This special result tells us that we are executing a "yield *"
				state = State.SUSPENDED_YIELD;
				try {
					delegee = ScriptRuntime.callIterator(ysResult.getResult(), cx, scope);
				} catch (RhinoException re) {
					// Need to handle exceptions if the iterator cannot be called.
					return resumeAbruptLocal(cx, scope, GeneratorState.GENERATOR_THROW, re);
				}

				Scriptable delResult;
				try {
					// Re-execute but update state in case we end up back here
					// Value shall be Undefined based on the very complex spec!
					delResult = resumeDelegee(cx, scope, Undefined.INSTANCE);
				} finally {
					state = State.EXECUTING;
				}
				if (ScriptRuntime.isIteratorDone(cx, delResult)) {
					state = State.COMPLETED;
				}
				return delResult;
			}

			ScriptableObject.putProperty(result, ES6Iterator.VALUE_PROPERTY, r, cx);

		} catch (GeneratorState.GeneratorClosedException gce) {
			state = State.COMPLETED;
		} catch (JavaScriptException jse) {
			state = State.COMPLETED;
			if (jse.getValue() instanceof NativeIterator.StopIteration) {
				ScriptableObject.putProperty(result, ES6Iterator.VALUE_PROPERTY, ((NativeIterator.StopIteration) jse.getValue()).getValue(), cx);
			} else {
				lineNumber = jse.lineNumber();
				lineSource = jse.lineSource();
				if (jse.getValue() instanceof RhinoException) {
					throw (RhinoException) jse.getValue();
				}
				throw jse;
			}
		} catch (RhinoException re) {
			lineNumber = re.lineNumber();
			lineSource = re.lineSource();
			throw re;
		} finally {
			if (state == State.COMPLETED) {
				ScriptableObject.putProperty(result, ES6Iterator.DONE_PROPERTY, Boolean.TRUE, cx);
			} else {
				state = State.SUSPENDED_YIELD;
			}
		}
		return result;
	}

	private Scriptable resumeAbruptLocal(Context cx, Scriptable scope, int op, Object value) {
		if (state == State.EXECUTING) {
			throw ScriptRuntime.typeError0(cx, "msg.generator.executing");
		}
		if (state == State.SUSPENDED_START) {
			// Throw right away if we never started
			state = State.COMPLETED;
		}

		Scriptable result = ES6Iterator.makeIteratorResult(cx, scope, Boolean.FALSE);
		if (state == State.COMPLETED) {
			if (op == GeneratorState.GENERATOR_THROW) {
				throw new JavaScriptException(cx, value, lineSource, lineNumber);
			}
			ScriptableObject.putProperty(result, ES6Iterator.VALUE_PROPERTY, value, cx);
			ScriptableObject.putProperty(result, ES6Iterator.DONE_PROPERTY, Boolean.TRUE, cx);
			return result;
		}

		state = State.EXECUTING;

		Object throwValue = value;
		if (op == GeneratorState.GENERATOR_CLOSE) {
			if (!(value instanceof GeneratorState.GeneratorClosedException)) {
				throwValue = new GeneratorState.GeneratorClosedException(value);
			}
		} else {
			if (value instanceof JavaScriptException) {
				throwValue = ((JavaScriptException) value).getValue();
			} else if (value instanceof RhinoException) {
				throwValue = ScriptRuntime.wrapException(cx, scope, (Throwable) value);
			}
		}

		try {
			Object r = function.resumeGenerator(cx, scope, op, savedState, throwValue);
			ScriptableObject.putProperty(result, ES6Iterator.VALUE_PROPERTY, r, cx);
			// If we get here without an exception we can still run.
			state = State.SUSPENDED_YIELD;

		} catch (GeneratorState.GeneratorClosedException gce) {
			state = State.COMPLETED;
			ScriptableObject.putProperty(result, ES6Iterator.VALUE_PROPERTY, gce.getValue(), cx);
		} catch (JavaScriptException jse) {
			state = State.COMPLETED;
			if (jse.getValue() instanceof NativeIterator.StopIteration) {
				ScriptableObject.putProperty(result, ES6Iterator.VALUE_PROPERTY, ((NativeIterator.StopIteration) jse.getValue()).getValue(), cx);
			} else {
				lineNumber = jse.lineNumber();
				lineSource = jse.lineSource();
				if (jse.getValue() instanceof RhinoException) {
					throw (RhinoException) jse.getValue();
				}
				throw jse;
			}
		} catch (RhinoException re) {
			state = State.COMPLETED;
			lineNumber = re.lineNumber();
			lineSource = re.lineSource();
			throw re;
		} finally {
			// After an abrupt completion we are always, umm, complete,
			// and we will never delegate to the delegee again
			if (state == State.COMPLETED) {
				delegee = null;
				ScriptableObject.putProperty(result, ES6Iterator.DONE_PROPERTY, Boolean.TRUE, cx);
			}
		}
		return result;
	}

	private Object callReturnOptionally(Context cx, Scriptable scope, Object value) {
		Object[] retArgs = Undefined.INSTANCE.equals(value) ? ScriptRuntime.EMPTY_OBJECTS : new Object[]{value};
		// Delegate to "return" method. If it's not defined we ignore it
		Object retFnObj = ScriptRuntime.getObjectPropNoWarn(cx, scope, delegee, ES6Iterator.RETURN_METHOD);
		if (!Undefined.INSTANCE.equals(retFnObj)) {
			if (!(retFnObj instanceof Callable)) {
				throw ScriptRuntime.typeError2(cx, "msg.isnt.function", ES6Iterator.RETURN_METHOD, ScriptRuntime.typeof(cx, retFnObj));
			}
			return cx.callSync((Callable) retFnObj, scope, ensureScriptable(delegee, cx), retArgs);
		}
		return null;
	}

	enum State {
		SUSPENDED_START, SUSPENDED_YIELD, EXECUTING, COMPLETED
	}
}
