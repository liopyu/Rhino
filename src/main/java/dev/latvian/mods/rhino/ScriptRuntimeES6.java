/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

public class ScriptRuntimeES6 {

	public static Object requireObjectCoercible(Context cx, Object val, IdFunctionObject idFuncObj) {
		return requireObjectCoercible(cx, val, idFuncObj.getTag(), idFuncObj.getFunctionName());
	}

	public static Object requireObjectCoercible(Context cx, Object val, Object tag, Object methodName) {
		if (val == null || Undefined.isUndefined(val)) {
			throw ScriptRuntime.typeError2(cx, "msg.called.null.or.undefined", tag, methodName);
		}
		return val;
	}

	/** Registers the symbol <code>[Symbol.species]</code> on the given constructor function. */
	public static void addSymbolSpecies(Context cx, Scriptable scope, IdScriptableObject constructor) {
		ScriptableObject speciesDescriptor = (ScriptableObject) cx.newObject(scope);
		speciesDescriptor.put(cx, "enumerable", speciesDescriptor, Boolean.FALSE);
		speciesDescriptor.put(cx, "configurable", speciesDescriptor, Boolean.TRUE);
		speciesDescriptor.put(cx, "get", speciesDescriptor,
				new LambdaFunction(cx, scope, "get [Symbol.species]", 0,
						(Context lcx, Scriptable lscope, Scriptable thisObj, Object[] args) -> thisObj, false));
		constructor.defineOwnProperty(cx, SymbolKey.SPECIES, speciesDescriptor, false);
	}
}
