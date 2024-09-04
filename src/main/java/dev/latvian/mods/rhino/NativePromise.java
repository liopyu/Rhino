/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.TopLevel.NativeErrors;

import java.util.ArrayList;

public class NativePromise extends ScriptableObject {

	enum State {
		PENDING,
		FULFILLED,
		REJECTED
	}

	enum ReactionType {
		FULFILL,
		REJECT
	}

	private State state = State.PENDING;
	private Object result = null;
    private boolean handled = false;

	private ArrayList<Reaction> fulfillReactions = new ArrayList<>();
	private ArrayList<Reaction> rejectReactions = new ArrayList<>();

	public static void init(Context cx, Scriptable scope, boolean sealed) {
		LambdaConstructor constructor = new LambdaConstructor(cx, scope, "Promise", 1, LambdaConstructor.CONSTRUCTOR_NEW, NativePromise::constructor);
		constructor.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);

		constructor.defineConstructorMethod(cx, scope, "resolve", 1, NativePromise::resolve, DONTENUM, DONTENUM | READONLY);
		constructor.defineConstructorMethod(cx, scope, "reject", 1, NativePromise::reject, DONTENUM, DONTENUM | READONLY);
		constructor.defineConstructorMethod(cx, scope, "all", 1, NativePromise::all, DONTENUM, DONTENUM | READONLY);
		constructor.defineConstructorMethod(cx, scope, "allSettled", 1, NativePromise::allSettled, DONTENUM, DONTENUM | READONLY);
		constructor.defineConstructorMethod(cx, scope, "race", 1, NativePromise::race, DONTENUM, DONTENUM | READONLY);
		constructor.defineConstructorMethod(cx, scope, "any", 1, NativePromise::any, DONTENUM, DONTENUM | READONLY);

		ScriptableObject speciesDescriptor = (ScriptableObject) cx.newObject(scope);
		ScriptableObject.putProperty(speciesDescriptor, "enumerable", false, cx);
		ScriptableObject.putProperty(speciesDescriptor, "configurable", true, cx);
		ScriptableObject.putProperty(speciesDescriptor, "get", new LambdaFunction(cx, scope, "get [Symbol.species]", 0, (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> thisObj), cx);
		constructor.defineOwnProperty(cx, SymbolKey.SPECIES, speciesDescriptor);

		constructor.definePrototypeMethod(cx, scope, "then", 2, (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> {
			NativePromise self = LambdaConstructor.convertThisObject(lcx, thisObj, NativePromise.class);
			return self.then(lcx, lscope, constructor, args);
		}, DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "catch", 1, NativePromise::doCatch, DONTENUM, DONTENUM | READONLY);
		constructor.definePrototypeMethod(cx, scope, "finally", 1, (Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> doFinally(lcx, lscope, thisObj, constructor, args), DONTENUM, DONTENUM | READONLY);

		constructor.definePrototypeProperty(cx, SymbolKey.TO_STRING_TAG, "Promise", DONTENUM | READONLY);

		ScriptableObject.defineProperty(scope, "Promise", constructor, DONTENUM, cx);
		if (sealed) {
			constructor.sealObject(cx);
		}
	}

	private static Scriptable constructor(Context cx, Scriptable scope, Object[] args) {
		if (args.length < 1 || !(args[0] instanceof Callable)) {
			throw ScriptRuntime.typeError0(cx, "msg.function.expected");
		}
		Callable executor = (Callable) args[0];
		NativePromise promise = new NativePromise();
		ResolvingFunctions resolving = new ResolvingFunctions(cx, scope, promise);

		try {
			executor.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{resolving.resolve, resolving.reject});
		} catch (RhinoException re) {
			resolving.reject.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{getErrorObject(cx, scope, re)});
		}

		return promise;
	}

	@Override
	public String getClassName() {
		return "Promise";
	}

    Object getResult() {
        return result;
    }

	// Promise.resolve
	private static Object resolve(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!ScriptRuntime.isObject(thisObj)) {
			throw ScriptRuntime.typeError1(cx, "msg.arg.not.object", ScriptRuntime.typeof(cx, thisObj).toString());
		}
		Object arg = (args.length > 0 ? args[0] : Undefined.INSTANCE);
		return resolveInternal(cx, scope, thisObj, arg);
	}

	// PromiseResolve abstract operation
	private static Object resolveInternal(Context cx, Scriptable scope, Object constructor, Object arg) {
		if (arg instanceof NativePromise) {
			Object argConstructor = ScriptRuntime.getObjectProp(cx, scope, arg, "constructor");
			if (argConstructor == constructor) {
				return arg;
			}
		}
		Capability cap = new Capability(cx, scope, constructor);
		cap.resolve.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{arg});
		return cap.promise;
	}

	// Promise.reject
	private static Object reject(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!ScriptRuntime.isObject(thisObj)) {
			throw ScriptRuntime.typeError1(cx, "msg.arg.not.object", ScriptRuntime.typeof(cx, thisObj).toString());
		}
		Object arg = (args.length > 0 ? args[0] : Undefined.INSTANCE);
		Capability cap = new Capability(cx, scope, thisObj);
		cap.reject.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{arg});
		return cap.promise;
	}

	// Promise.all
	private static Object all(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return doAll(cx, scope, thisObj, args, true);
	}

	// Promise.allSettled
	private static Object allSettled(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return doAll(cx, scope, thisObj, args, false);
	}

	private static Object doAll(Context cx, Scriptable scope, Scriptable thisObj, Object[] args, boolean failFast) {
		Capability cap = new Capability(cx, scope, thisObj);
		Object arg = (args.length > 0 ? args[0] : Undefined.INSTANCE);

		IteratorLikeIterable iterable;
		try {
			Object maybeIterable = ScriptRuntime.callIterator(cx, scope, arg);
			iterable = new IteratorLikeIterable(cx, scope, maybeIterable);
		} catch (RhinoException re) {
			cap.reject.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{getErrorObject(cx, scope, re)});
			return cap.promise;
		}

		IteratorLikeIterable.Itr iterator = iterable.iterator();
		try {
			PromiseAllResolver resolver = new PromiseAllResolver(iterator, thisObj, cap, failFast);
			try {
				return resolver.resolve(cx, scope);
			} finally {
				if (!iterator.isDone()) {
					iterable.close();
				}
			}
		} catch (RhinoException re) {
			cap.reject.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{getErrorObject(cx, scope, re)});
			return cap.promise;
		}
	}

	// Promise.any
	private static Object any(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Capability cap = new Capability(cx, scope, thisObj);
		Object arg = (args.length > 0 ? args[0] : Undefined.INSTANCE);

		IteratorLikeIterable iterable;
		try {
			Object maybeIterable = ScriptRuntime.callIterator(cx, scope, arg);
			iterable = new IteratorLikeIterable(cx, scope, maybeIterable);
		} catch (RhinoException re) {
			cap.reject.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{getErrorObject(cx, scope, re)});
			return cap.promise;
		}

		IteratorLikeIterable.Itr iterator = iterable.iterator();
		try {
			PromiseAnyRejector rejector = new PromiseAnyRejector(iterator, thisObj, cap);
			try {
				return rejector.reject(cx, scope);
			} finally {
				if (!iterator.isDone()) {
					iterable.close();
				}
			}
		} catch (RhinoException re) {
			cap.reject.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{getErrorObject(cx, scope, re)});
			return cap.promise;
		}
	}

	// Promise.race
	private static Object race(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Capability cap = new Capability(cx, scope, thisObj);
		Object arg = (args.length > 0 ? args[0] : Undefined.INSTANCE);

		IteratorLikeIterable iterable;
		try {
			Object maybeIterable = ScriptRuntime.callIterator(cx, scope, arg);
			iterable = new IteratorLikeIterable(cx, scope, maybeIterable);
		} catch (RhinoException re) {
			cap.reject.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{getErrorObject(cx, scope, re)});
			return cap.promise;
		}

		IteratorLikeIterable.Itr iterator = iterable.iterator();
		try {
			try {
				return performRace(cx, scope, iterator, thisObj, cap);
			} finally {
				if (!iterator.isDone()) {
					iterable.close();
				}
			}
		} catch (RhinoException re) {
			cap.reject.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{getErrorObject(cx, scope, re)});
			return cap.promise;
		}
	}

	private static Object performRace(Context cx, Scriptable scope, IteratorLikeIterable.Itr iterator, Scriptable thisObj, Capability cap) {
		Callable resolve = ScriptRuntime.getPropFunctionAndThis(cx, scope, thisObj, "resolve");
		Scriptable localThis = cx.lastStoredScriptable();

		// Manually iterate for exception handling purposes
		while (true) {
			boolean hasNext;
			Object nextVal = Undefined.INSTANCE;
			boolean nextOk = false;
			try {
				hasNext = iterator.hasNext();
				if (hasNext) {
					nextVal = iterator.next();
				}
				nextOk = true;
			} finally {
				if (!nextOk) {
					iterator.setDone(true);
				}
			}

			if (!hasNext) {
				return cap.promise;
			}

			// Call "resolve" to get the next promise in the chain
			Object nextPromise = resolve.call(cx, scope, localThis, new Object[]{nextVal});

			// And then call "then" on it.
			// Logic in the resolution function ensures we don't deliver duplicate results
			Callable thenFunc = ScriptRuntime.getPropFunctionAndThis(cx, scope, nextPromise, "then");
			thenFunc.call(cx, scope, cx.lastStoredScriptable(), new Object[]{cap.resolve, cap.reject});
		}
	}

	// Promise.prototype.then
	private Object then(Context cx, Scriptable scope, LambdaConstructor defaultConstructor, Object[] args) {
		Constructable constructable = AbstractEcmaObjectOperations.speciesConstructor(cx, this, defaultConstructor);
		Capability capability = new Capability(cx, scope, constructable);

		Callable onFulfilled = null;
		if (args.length >= 1 && args[0] instanceof Callable) {
			onFulfilled = (Callable) args[0];
		}
		Callable onRejected = null;
		if (args.length >= 2 && args[1] instanceof Callable) {
			onRejected = (Callable) args[1];
		}

		Reaction fulfillReaction = new Reaction(capability, ReactionType.FULFILL, onFulfilled);
		Reaction rejectReaction = new Reaction(capability, ReactionType.REJECT, onRejected);

		if (state == State.PENDING) {
			fulfillReactions.add(fulfillReaction);
			rejectReactions.add(rejectReaction);
		} else if (state == State.FULFILLED) {
			cx.enqueueMicrotask(() -> fulfillReaction.invoke(cx, scope, result));
		} else {
			assert (state == State.REJECTED);
			markHandled(cx);
			cx.enqueueMicrotask(() -> rejectReaction.invoke(cx, scope, result));
		}
		return capability.promise;
	}

	private void markHandled(Context cx) {
		if (!handled) {
			cx.getUnhandledPromiseTracker().promiseHandled(this);
			handled = true;
		}
	}

	// Promise.prototype.catch
	private static Object doCatch(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object arg = (args.length > 0 ? args[0] : Undefined.INSTANCE);
		Scriptable coercedThis = ScriptRuntime.toObject(cx, scope, thisObj);
		// No guarantee that the caller didn't change the prototype of "then"!
		Callable thenFunc = ScriptRuntime.getPropFunctionAndThis(cx, scope, coercedThis, "then");
		return thenFunc.call(cx, scope, cx.lastStoredScriptable(), new Object[]{Undefined.INSTANCE, arg});
	}

	// Promise.prototype.finally
	private static Object doFinally(Context cx, Scriptable scope, Scriptable thisObj, LambdaConstructor defaultConstructor, Object[] args) {
		if (!ScriptRuntime.isObject(thisObj)) {
			throw ScriptRuntime.typeError1(cx, "msg.arg.not.object", ScriptRuntime.typeof(cx, thisObj).toString());
		}
		Object onFinally = args.length > 0 ? args[0] : Undefined.SCRIPTABLE_INSTANCE;
		Object thenFinally = onFinally;
		Object catchFinally = onFinally;
		Constructable constructor = AbstractEcmaObjectOperations.speciesConstructor(cx, thisObj, defaultConstructor);
		if (onFinally instanceof Callable) {
			Callable callableOnFinally = (Callable) thenFinally;
			thenFinally = makeThenFinally(cx, scope, constructor, callableOnFinally);
			catchFinally = makeCatchFinally(cx, scope, constructor, callableOnFinally);
		}
		Callable thenFunc = ScriptRuntime.getPropFunctionAndThis(cx, scope, thisObj, "then");
		Scriptable to = cx.lastStoredScriptable();
		return thenFunc.call(cx, scope, to, new Object[]{thenFinally, catchFinally});
	}

	// Abstract "Then Finally Function"
	private static Callable makeThenFinally(Context ccx, Scriptable scope, Object constructor, Callable onFinally) {
		return new LambdaFunction(ccx, scope, 1, (Context cx, Scriptable ls, Scriptable thisObj, Object[] args) -> {
			Object value = args.length > 0 ? args[0] : Undefined.INSTANCE;
			LambdaFunction valueThunk = new LambdaFunction(cx, scope, 0, (Context vc, Scriptable vs, Scriptable vt, Object[] va) -> value);
			Object result = onFinally.call(cx, ls, Undefined.SCRIPTABLE_INSTANCE, ScriptRuntime.EMPTY_OBJECTS);
			Object promise = resolveInternal(cx, scope, constructor, result);
			Callable thenFunc = ScriptRuntime.getPropFunctionAndThis(cx, scope, promise, "then");
			return thenFunc.call(cx, scope, cx.lastStoredScriptable(), new Object[]{valueThunk});
		});
	}

	// Abstract "Catch Finally Thrower"
	private static Callable makeCatchFinally(Context ccx, Scriptable scope, Object constructor, Callable onFinally) {
		return new LambdaFunction(ccx, scope, 1, (Context cx, Scriptable ls, Scriptable thisObj, Object[] args) -> {
			Object reason = args.length > 0 ? args[0] : Undefined.INSTANCE;
			LambdaFunction reasonThrower = new LambdaFunction(cx, scope, 0, (Context vc, Scriptable vs, Scriptable vt, Object[] va) -> {
				throw new JavaScriptException(vc, reason, null, 0);
			});
			Object result = onFinally.call(cx, ls, Undefined.SCRIPTABLE_INSTANCE, ScriptRuntime.EMPTY_OBJECTS);
			Object promise = resolveInternal(cx, scope, constructor, result);
			Callable thenFunc = ScriptRuntime.getPropFunctionAndThis(cx, scope, promise, "then");
			return thenFunc.call(cx, scope, cx.lastStoredScriptable(), new Object[]{reasonThrower});
		});
	}

	// Abstract operation to fulfill a promise
	private Object fulfillPromise(Context cx, Scriptable scope, Object value) {
		assert (state == State.PENDING);
		result = value;
		ArrayList<Reaction> reactions = fulfillReactions;
		fulfillReactions = new ArrayList<>();
		if (!rejectReactions.isEmpty()) {
			rejectReactions = new ArrayList<>();
		}
		state = State.FULFILLED;
		for (Reaction r : reactions) {
			cx.enqueueMicrotask(() -> r.invoke(cx, scope, value));
		}
		return Undefined.INSTANCE;
	}

	// Abstract operation to reject a promise.
	private Object rejectPromise(Context cx, Scriptable scope, Object reason) {
		assert (state == State.PENDING);
		result = reason;
		ArrayList<Reaction> reactions = rejectReactions;
		rejectReactions = new ArrayList<>();
		if (!fulfillReactions.isEmpty()) {
			fulfillReactions = new ArrayList<>();
		}
		state = State.REJECTED;
        cx.getUnhandledPromiseTracker().promiseRejected(this);
		for (Reaction r : reactions) {
			cx.enqueueMicrotask(() -> r.invoke(cx, scope, reason));
		}
		if (!reactions.isEmpty()) {
			markHandled(cx);
		}
		return Undefined.INSTANCE;
	}

	// Promise Resolve Thenable Job.
	// This gets called by the "resolving func" as a microtask.
	private void callThenable(Context cx, Scriptable scope, Object resolution, Callable thenFunc) {
		ResolvingFunctions resolving = new ResolvingFunctions(cx, scope, this);
		Scriptable thisObj = (resolution instanceof Scriptable ? (Scriptable) resolution : Undefined.SCRIPTABLE_INSTANCE);
		try {
			thenFunc.call(cx, scope, thisObj, new Object[]{resolving.resolve, resolving.reject});
		} catch (RhinoException re) {
			resolving.reject.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{getErrorObject(cx, scope, re)});
		}
	}

	private static Object getErrorObject(Context cx, Scriptable scope, RhinoException re) {
		if (re instanceof JavaScriptException) {
			return ((JavaScriptException) re).getValue();
		}

		TopLevel.NativeErrors constructor = NativeErrors.Error;
		if (re instanceof EcmaError ee) {
			switch (ee.getName()) {
				case "EvalError" -> constructor = NativeErrors.EvalError;
				case "RangeError" -> constructor = NativeErrors.RangeError;
				case "ReferenceError" -> constructor = NativeErrors.ReferenceError;
				case "SyntaxError" -> constructor = NativeErrors.SyntaxError;
				case "TypeError" -> constructor = NativeErrors.TypeError;
				case "URIError" -> constructor = NativeErrors.URIError;
				case "InternalError" -> constructor = NativeErrors.InternalError;
				case "JavaException" -> constructor = NativeErrors.JavaException;
				default -> {
				}
			}
		}
		return ScriptRuntime.newNativeError(cx, scope, constructor, new Object[]{re.getMessage()});
	}

	// Output of "CreateResolvingFunctions." Carries with it an "alreadyResolved" state,
	// so we make it a separate object. This actually fires resolution functions on
	// the passed callbacks.
	private static class ResolvingFunctions {

		private boolean alreadyResolved = false;
		LambdaFunction resolve;
		LambdaFunction reject;

		ResolvingFunctions(Context ccx, Scriptable topScope, NativePromise promise) {
			resolve = new LambdaFunction(ccx, topScope, 1, (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) -> resolve(cx, scope, promise, (args.length > 0 ? args[0] : Undefined.INSTANCE)));
			reject = new LambdaFunction(ccx, topScope, 1, (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) -> reject(cx, scope, promise, (args.length > 0 ? args[0] : Undefined.INSTANCE)));
		}

		private Object reject(Context cx, Scriptable scope, NativePromise promise, Object reason) {
			if (alreadyResolved) {
				return Undefined.INSTANCE;
			}
			alreadyResolved = true;
			return promise.rejectPromise(cx, scope, reason);
		}

		private Object resolve(Context cx, Scriptable scope, NativePromise promise, Object resolution) {
			if (alreadyResolved) {
				return Undefined.INSTANCE;
			}
			alreadyResolved = true;

			if (resolution == promise) {
				Object err = ScriptRuntime.newNativeError(cx, scope, NativeErrors.TypeError, new Object[]{"No promise self-resolution"});
				return promise.rejectPromise(cx, scope, err);
			}

			if (!ScriptRuntime.isObject(resolution)) {
				return promise.fulfillPromise(cx, scope, resolution);
			}

			Scriptable sresolution = ScriptableObject.ensureScriptable(resolution, cx);
			Object thenObj = ScriptableObject.getProperty(sresolution, "then", cx);
			if (!(thenObj instanceof Callable)) {
				return promise.fulfillPromise(cx, scope, resolution);
			}

			cx.enqueueMicrotask(() -> promise.callThenable(cx, scope, resolution, (Callable) thenObj));
			return Undefined.INSTANCE;
		}
	}

	// "Promise Reaction" record. This is an input to the microtask.
	private static class Reaction {
		Capability capability;
		ReactionType reaction = ReactionType.REJECT;
		Callable handler;

		Reaction(Capability cap, ReactionType type, Callable handler) {
			this.capability = cap;
			this.reaction = type;
			this.handler = handler;
		}

		// Implementation of NewPromiseReactionJob
		void invoke(Context cx, Scriptable scope, Object arg) {
			try {
				Object result = null;
				if (handler == null) {
					switch (reaction) {
						case FULFILL:
							result = arg;
							break;
						case REJECT:
							capability.reject.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{arg});
							return;
					}
				} else {
					result = handler.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{arg});
				}
				capability.resolve.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{result});

			} catch (RhinoException re) {
				capability.reject.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{getErrorObject(cx, scope, re)});
			}
		}
	}

	// "Promise Capability Record"
	// This abstracts a promise from the specific native implementation by keeping track
	// of the "resolve" and "reject" functions.
	private static class Capability {
		Object promise;
		private Object rawResolve = Undefined.INSTANCE;
		Callable resolve;
		private Object rawReject = Undefined.INSTANCE;
		Callable reject;

		// Given an object that represents a constructor function, execute it as if it
		// meets the "Promise" constructor pattern, which takes a function that will
		// be called with "resolve" and "reject" functions.
		Capability(Context topCx, Scriptable topScope, Object pc) {
			if (!(pc instanceof Constructable)) {
				throw ScriptRuntime.typeError0(topCx, "msg.constructor.expected");
			}
			Constructable promiseConstructor = (Constructable) pc;
			LambdaFunction executorFunc = new LambdaFunction(topCx, topScope, 2, (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) -> executor(cx, args));

			promise = promiseConstructor.construct(topCx, topScope, new Object[]{executorFunc});

			if (!(rawResolve instanceof Callable)) {
				throw ScriptRuntime.typeError0(topCx, "msg.function.expected");
			}
			resolve = (Callable) rawResolve;

			if (!(rawReject instanceof Callable)) {
				throw ScriptRuntime.typeError0(topCx, "msg.function.expected");
			}
			reject = (Callable) rawReject;
		}

		private Object executor(Context cx, Object[] args) {
			if (!Undefined.isUndefined(rawResolve) || !Undefined.isUndefined(rawReject)) {
				throw ScriptRuntime.typeError0(cx, "msg.promise.capability.state");
			}
			if (args.length > 0) {
				rawResolve = args[0];
			}
			if (args.length > 1) {
				rawReject = args[1];
			}
			return Undefined.INSTANCE;
		}
	}

	// This object keeps track of the state necessary to execute Promise.all
	private static class PromiseAllResolver {
		// Limit the number of promises in Promise.all the same as it is in V8.
		private static final int MAX_PROMISES = 1 << 21;

		final ArrayList<Object> values = new ArrayList<>();
		int remainingElements = 1;

		IteratorLikeIterable.Itr iterator;
		Scriptable thisObj;
		Capability capability;
        boolean failFast;

        PromiseAllResolver(IteratorLikeIterable.Itr iter, Scriptable thisObj, Capability cap, boolean failFast) {
			this.iterator = iter;
			this.thisObj = thisObj;
			this.capability = cap;
            this.failFast = failFast;
		}

		Object resolve(Context topCx, Scriptable topScope) {
			int index = 0;
			// Do this first because we should catch any exception before
			// invoking the iterator.
			Callable resolve = ScriptRuntime.getPropFunctionAndThis(topCx, topScope, thisObj, "resolve");
			Scriptable storedThis = topCx.lastStoredScriptable();

			// Iterate manually because we need to catch exceptions in a special way.
			while (true) {
				if (index == MAX_PROMISES) {
					throw ScriptRuntime.rangeError(topCx, ScriptRuntime.getMessage0("msg.promise.all.toobig"));
				}
				boolean hasNext;
				Object nextVal = Undefined.INSTANCE;
				boolean nextOk = false;
				try {
					hasNext = iterator.hasNext();
					if (hasNext) {
						nextVal = iterator.next();
					}
					nextOk = true;
				} finally {
					if (!nextOk) {
						iterator.setDone(true);
					}
				}

				if (!hasNext) {
					if (--remainingElements == 0) {
						finalResolution(topCx, topScope);
					}
					return capability.promise;
				}

				values.add(Undefined.INSTANCE);

				// Call "resolve" to get the next promise in the chain
				Object nextPromise = resolve.call(topCx, topScope, storedThis, new Object[]{nextVal});

				// Create a resolution func that will stash its result in the right place
				PromiseElementResolver eltResolver = new PromiseElementResolver(index);
				LambdaFunction resolveFunc = new LambdaFunction(topCx, topScope, 1, (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) -> {
					Object value = (args.length > 0 ? args[0] : Undefined.INSTANCE);
					if (!failFast) {
						Scriptable elementResult = cx.newObject(scope);
						elementResult.put(cx, "status", elementResult, "fulfilled");
						elementResult.put(cx, "value", elementResult, value);
						value = elementResult;
					}
					return eltResolver.resolve(cx, scope, value, this);
				});

				Callable rejectFunc = capability.reject;
				if (!failFast) {
					LambdaFunction resolveSettledRejection = new LambdaFunction(topCx, topScope, 1, (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) -> {
						Scriptable result = cx.newObject(scope);
						result.put(cx, "status", result, " rejected");
						result.put(cx, "reason", result, (args.length > 0 ? args[0] : Undefined.INSTANCE));
						return eltResolver.resolve(cx, scope, result, this);
					});
					resolveSettledRejection.setStandardPropertyAttributes(DONTENUM | READONLY);
					rejectFunc = resolveSettledRejection;
				}
				remainingElements++;

				// Call "then" on the promise with the resolution func
				Callable thenFunc = ScriptRuntime.getPropFunctionAndThis(topCx, topScope, nextPromise, "then");
				thenFunc.call(topCx, topScope, topCx.lastStoredScriptable(), new Object[]{resolveFunc, rejectFunc});
				index++;
			}
		}

		void finalResolution(Context cx, Scriptable scope) {
			Scriptable newArray = cx.newArray(scope, values.toArray());
			capability.resolve.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{newArray});
		}
	}

	// This object keeps track of the state necessary to execute Promise.any
	private static class PromiseAnyRejector {
		// Limit the number of promises in Promise.any the same as it is in V8.
		private static final int MAX_PROMISES = 1 << 21;

		final ArrayList<Object> errors = new ArrayList<>();
		int remainingElements = 1;

		IteratorLikeIterable.Itr iterator;
		Scriptable thisObj;
		Capability capability;

		PromiseAnyRejector(IteratorLikeIterable.Itr iter, Scriptable thisObj, Capability cap) {
			this.iterator = iter;
			this.thisObj = thisObj;
			this.capability = cap;
		}

		Object reject(Context topCx, Scriptable topScope) {
			int index = 0;
			// Do this first because we should catch any exception before
			// invoking the iterator.
			Callable resolve = ScriptRuntime.getPropFunctionAndThis(topCx, topScope, thisObj, "resolve");
			Scriptable storedThis = topCx.lastStoredScriptable();

			// Iterate manually because we need to catch exceptions in a special way.
			while (true) {
				if (index == MAX_PROMISES) {
					throw ScriptRuntime.rangeError(topCx, ScriptRuntime.getMessage0("msg.promise.any.toobig"));
				}
				boolean hasNext;
				Object nextVal = Undefined.INSTANCE;
				boolean nextOk = false;
				try {
					hasNext = iterator.hasNext();
					if (hasNext) {
						nextVal = iterator.next();
					}
					nextOk = true;
				} finally {
					if (!nextOk) {
						iterator.setDone(true);
					}
				}

				if (!hasNext) {
					if (--remainingElements == 0) {
						Scriptable newArray = topCx.newArray(topScope, errors.toArray());
						NativeError error = (NativeError) topCx.newObject(topScope, "AggregateError", new Object[]{newArray});
						throw new JavaScriptException(topCx, error, null, 0);
					}
					return capability.promise;
				}

				errors.add(Undefined.INSTANCE);

				// Call "resolve" to get the next promise in the chain
				Object nextPromise = resolve.call(topCx, topScope, storedThis, new Object[]{nextVal});

				// Create a resolution func that will stash its result in the right place
				PromiseElementResolver eltResolver = new PromiseElementResolver(index);
				LambdaFunction rejectFunc = new LambdaFunction(topCx, topScope, 1, (Context cx, Scriptable scope, Scriptable thisObj, Object[] args) -> {
					Object value = (args.length > 0 ? args[0] : Undefined.INSTANCE);
					return eltResolver.reject(cx, scope, value, this);
				});
				remainingElements++;

				// Call "then" on the promise with the resolution func
				Callable thenFunc = ScriptRuntime.getPropFunctionAndThis(topCx, topScope, nextPromise, "then");
				thenFunc.call(topCx, topScope, topCx.lastStoredScriptable(), new Object[]{capability.resolve, rejectFunc});
				index++;
			}
		}

		void finalRejection(Context cx, Scriptable scope) {
			Scriptable newArray = cx.newArray(scope, errors.toArray());
			NativeError error = (NativeError) cx.newObject(scope, "AggregateError", new Object[]{newArray});
			capability.reject.call(cx, scope, Undefined.SCRIPTABLE_INSTANCE, new Object[]{error});
		}
	}

	// This object keeps track of the state necessary to resolve one element in Promise.all
    // and Promise.any
	private static class PromiseElementResolver {

		private boolean alreadyCalled = false;
		private final int index;

		PromiseElementResolver(int ix) {
			this.index = ix;
		}

		Object resolve(Context cx, Scriptable scope, Object result, PromiseAllResolver resolver) {
			if (alreadyCalled) {
				return Undefined.INSTANCE;
			}
			alreadyCalled = true;
			resolver.values.set(index, result);
			if (--resolver.remainingElements == 0) {
				resolver.finalResolution(cx, scope);
			}
			return Undefined.INSTANCE;
		}

		Object reject(Context cx, Scriptable scope, Object result, PromiseAnyRejector rejector) {
			if (alreadyCalled) {
				return Undefined.INSTANCE;
			}
			alreadyCalled = true;
			rejector.errors.set(index, result);
			if (--rejector.remainingElements == 0) {
				rejector.finalRejection(cx, scope);
			}
			return Undefined.INSTANCE;
		}
	}
}
