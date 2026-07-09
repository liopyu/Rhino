/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.ScriptRuntime.StringIdOrIndex;
import dev.latvian.mods.rhino.regexp.RegExp;

import java.text.Collator;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static dev.latvian.mods.rhino.ScriptRuntimeES6.requireObjectCoercible;

/**
 * This class implements the String native object.
 *
 * <p>See ECMA 15.5.
 *
 * <p>String methods for dealing with regular expressions are ported directly from C. Latest port is
 * from version 1.40.12.19 in the JSFUN13_BRANCH.
 *
 * @author Mike McCabe
 * @author Norris Boyd
 * @author Ronald Brill
 */
final class NativeString extends ScriptableObject implements Wrapper {
	private static final long serialVersionUID = 920268368584188687L;

	private static final String CLASS_NAME = "String";

	private final CharSequence string;

	static void init(Scriptable scope, boolean sealed, Context cx) {
		LambdaConstructor c =
			new LambdaConstructor(
				cx,
				scope,
				CLASS_NAME,
				1,
				NativeString::js_constructorFunc,
				NativeString::js_constructor);
		c.setPrototypePropertyAttributes(DONTENUM | READONLY | PERMANENT);
		c.setPrototypeScriptable(new NativeString(""), cx);

		defConsMethod(cx, c, scope, "fromCharCode", 1, NativeString::js_fromCharCode);
		defConsMethod(cx, c, scope, "fromCodePoint", 1, NativeString::js_fromCodePoint);
		defConsMethod(cx, c, scope, "raw", 1, NativeString::js_raw);

		/*
		 * All of the methods below are on the constructor for compatibility with ancient Rhino
		 * versions. They are no longer part of ECMAScript. The "wrapConstructor" method is a
		 * technique used in the past in Rhino to adapt the instance functions so that they
		 * may be called on the constructor directly.
		 */
		defConsMethod(cx, c, scope, "charAt", 1, wrapConstructor(NativeString::js_charAt));
		defConsMethod(cx, c, scope, "charCodeAt", 1, wrapConstructor(NativeString::js_charCodeAt));
		defConsMethod(cx, c, scope, "indexOf", 2, wrapConstructor(NativeString::js_indexOf));
		defConsMethod(cx, c, scope, "lastIndexOf", 2, wrapConstructor(NativeString::js_lastIndexOf));
		defConsMethod(cx, c, scope, "split", 3, wrapConstructor(NativeString::js_split));
		defConsMethod(cx, c, scope, "substring", 3, wrapConstructor(NativeString::js_substring));
		defConsMethod(cx, c, scope, "toLowerCase", 1, wrapConstructor(NativeString::js_toLowerCase));
		defConsMethod(cx, c, scope, "toUpperCase", 1, wrapConstructor(NativeString::js_toUpperCase));
		defConsMethod(cx, c, scope, "substr", 3, wrapConstructor(NativeString::js_substr));
		defConsMethod(cx, c, scope, "concat", 2, wrapConstructor(NativeString::js_concat));
		defConsMethod(cx, c, scope, "slice", 3, wrapConstructor(NativeString::js_slice));
		defConsMethod(
			cx,
			c,
			scope,
			"equalsIgnoreCase",
			2,
			wrapConstructor(NativeString::js_equalsIgnoreCase));
		defConsMethod(cx, c, scope, "match", 2, wrapConstructor(NativeString::js_match));
		defConsMethod(cx, c, scope, "search", 2, wrapConstructor(NativeString::js_search));
		defConsMethod(cx, c, scope, "replace", 2, wrapConstructor(NativeString::js_replace));
		defConsMethod(cx, c, scope, "replaceAll", 2, wrapConstructor(NativeString::js_replaceAll));
		defConsMethod(
			cx,
			c, scope, "localeCompare", 2, wrapConstructor(NativeString::js_localeCompare));
		defConsMethod(
			cx,
			c,
			scope,
			"toLocaleLowerCase",
			1,
			wrapConstructor(NativeString::js_toLocaleLowerCase));

		/* Back to prototype methods -- these are all part of ECMAScript */
		defProtoMethod(cx, c, scope, SymbolKey.ITERATOR, 0, NativeString::js_iterator);
		defProtoMethod(cx, c, scope, "toString", 0, NativeString::js_toString);
		defProtoMethod(cx, c, scope, "toSource", 0, NativeString::js_toSource);
		defProtoMethod(cx, c, scope, "valueOf", 0, NativeString::js_toString);
		defProtoMethodWithoutProto(cx, c, scope, "charAt", 1, NativeString::js_charAt);
		defProtoMethodWithoutProto(cx, c, scope, "charCodeAt", 1, NativeString::js_charCodeAt);
		defProtoMethodWithoutProto(cx, c, scope, "indexOf", 1, NativeString::js_indexOf);
		defProtoMethodWithoutProto(cx, c, scope, "lastIndexOf", 1, NativeString::js_lastIndexOf);
		defProtoMethodWithoutProto(cx, c, scope, "split", 2, NativeString::js_split);
		defProtoMethodWithoutProto(cx, c, scope, "substring", 2, NativeString::js_substring);
		defProtoMethodWithoutProto(cx, c, scope, "toLowerCase", 0, NativeString::js_toLowerCase);
		defProtoMethodWithoutProto(cx, c, scope, "toUpperCase", 0, NativeString::js_toUpperCase);
		defProtoMethodWithoutProto(cx, c, scope, "substr", 2, NativeString::js_substr);
		defProtoMethodWithoutProto(cx, c, scope, "concat", 1, NativeString::js_concat);
		defProtoMethodWithoutProto(cx, c, scope, "slice", 2, NativeString::js_slice);
		defProtoMethod(cx, c, scope, "bold", 0, NativeString::js_bold);
		defProtoMethod(cx, c, scope, "italics", 0, NativeString::js_italics);
		defProtoMethod(cx, c, scope, "fixed", 0, NativeString::js_fixed);
		defProtoMethod(cx, c, scope, "strike", 0, NativeString::js_strike);
		defProtoMethod(cx, c, scope, "small", 0, NativeString::js_small);
		defProtoMethod(cx, c, scope, "big", 0, NativeString::js_big);
		defProtoMethod(cx, c, scope, "blink", 0, NativeString::js_blink);
		defProtoMethod(cx, c, scope, "sup", 0, NativeString::js_sup);
		defProtoMethod(cx, c, scope, "sub", 0, NativeString::js_sub);
		defProtoMethod(cx, c, scope, "fontsize", 0, NativeString::js_fontsize);
		defProtoMethod(cx, c, scope, "fontcolor", 0, NativeString::js_fontcolor);
		defProtoMethod(cx, c, scope, "link", 0, NativeString::js_link);
		defProtoMethod(cx, c, scope, "anchor", 0, NativeString::js_anchor);
		defProtoMethod(cx, c, scope, "equals", 1, NativeString::js_equals);
		defProtoMethod(cx, c, scope, "equalsIgnoreCase", 1, NativeString::js_equalsIgnoreCase);
		defProtoMethodWithoutProto(cx, c, scope, "match", 1, NativeString::js_match);
		defProtoMethodWithoutProto(cx, c, scope, "matchAll", 1, NativeString::js_matchAll);
		defProtoMethodWithoutProto(cx, c, scope, "search", 1, NativeString::js_search);
		defProtoMethodWithoutProto(cx, c, scope, "replace", 2, NativeString::js_replace);
		defProtoMethodWithoutProto(cx, c, scope, "replaceAll", 2, NativeString::js_replaceAll);
		defProtoMethod(cx, c, scope, "at", 1, NativeString::js_at);
		defProtoMethodWithoutProto(cx, c, scope, "localeCompare", 1, NativeString::js_localeCompare);
		defProtoMethodWithoutProto(
			cx,
			c, scope, "toLocaleLowerCase", 0, NativeString::js_toLocaleLowerCase);
		defProtoMethodWithoutProto(
			cx,
			c, scope, "toLocaleUpperCase", 0, NativeString::js_toLocaleUpperCase);
		defProtoMethod(cx, c, scope, "trim", 0, NativeString::js_trim);
		defProtoMethod(cx, c, scope, "trimLeft", 0, NativeString::js_trimLeft);
		defProtoMethod(cx, c, scope, "trimStart", 0, NativeString::js_trimLeft);
		defProtoMethod(cx, c, scope, "trimRight", 0, NativeString::js_trimRight);
		defProtoMethod(cx, c, scope, "trimEnd", 0, NativeString::js_trimRight);
		defProtoMethod(cx, c, scope, "includes", 1, NativeString::js_includes);
		defProtoMethod(cx, c, scope, "startsWith", 1, NativeString::js_startsWith);
		defProtoMethod(cx, c, scope, "endsWith", 1, NativeString::js_endsWith);
		defProtoMethod(cx, c, scope, "normalize", 0, NativeString::js_normalize);
		defProtoMethod(cx, c, scope, "repeat", 1, NativeString::js_repeat);
		defProtoMethod(cx, c, scope, "codePointAt", 1, NativeString::js_codePointAt);
		defProtoMethod(cx, c, scope, "padStart", 1, NativeString::js_padStart);
		defProtoMethod(cx, c, scope, "padEnd", 1, NativeString::js_padEnd);
		defProtoMethod(cx, c, scope, "isWellFormed", 0, NativeString::js_isWellFormed);
		defProtoMethod(cx, c, scope, "toWellFormed", 0, NativeString::js_toWellFormed);

		if (sealed) {
			c.sealObject(cx);
		}
		ScriptableObject.defineProperty(scope, CLASS_NAME, c, DONTENUM, cx);
	}

	private static void defConsMethod(
		Context cx, LambdaConstructor c, Scriptable scope, String name, int length, Callable target) {
		c.defineConstructorMethod(cx, scope, name, length, target, DONTENUM);
	}

	private static void defProtoMethod(
		Context cx, LambdaConstructor c, Scriptable scope, String name, int length, Callable target) {
		c.definePrototypeMethod(cx, scope, name, length, target, DONTENUM, DONTENUM | READONLY);
	}

	private static void defProtoMethod(
		Context cx, LambdaConstructor c, Scriptable scope, SymbolKey key, int length, Callable target) {
		LambdaFunction f = new LambdaFunction(cx, scope, "[Symbol.iterator]", length, target, false);
		f.setStandardPropertyAttributes(DONTENUM | READONLY);
		c.definePrototypeProperty(cx, key, f, DONTENUM);
	}

	private static void defProtoMethodWithoutProto(
		Context cx, LambdaConstructor c, Scriptable scope, String name, int length, Callable target) {
		c.definePrototypeMethod(cx, scope, name, length, target, DONTENUM, DONTENUM | READONLY);
	}

	NativeString(CharSequence s) {
		string = s;
		// These need to happen right here because ScriptRuntime sometimes
		// constructs strings directly without using the JS constructor.
		defineProperty("length", s::length, null, DONTENUM | READONLY | PERMANENT);

		// namespace and path (maybe, KubeJS specifically should add these to the prototype instead)
		defineProperty("namespace", () -> {
			String str = s.toString();
			int colon = str.indexOf(':');
			return colon == -1 ? "minecraft" : str.substring(0, colon);
		}, null, DONTENUM | READONLY | PERMANENT);
		defineProperty("path", () -> {
			String str = s.toString();
			int colon = str.indexOf(':');
			return colon == -1 ? str : str.substring(colon + 1);
		}, null, DONTENUM | READONLY | PERMANENT);
	}

	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	@Override
	public Object unwrap() {
		return string;
	}

	@Override
	public MemberType getTypeOf() {
		return MemberType.STRING;
	}

	private static Callable wrapConstructor(Callable target) {
		return (Context cx, Scriptable scope, Scriptable origThis, Object[] origArgs) -> {
			Scriptable thisObj;
			Object[] newArgs;
			if (origArgs.length > 0) {
				thisObj =
					ScriptRuntime.toObject(
						cx, scope, ScriptRuntime.toCharSequence(cx, origArgs[0]));
				newArgs = new Object[origArgs.length - 1];
				System.arraycopy(origArgs, 1, newArgs, 0, newArgs.length);
			} else {
				thisObj = ScriptRuntime.toObject(cx, scope, ScriptRuntime.toCharSequence(cx, origThis));
				newArgs = origArgs;
			}
			return target.call(cx, scope, thisObj, newArgs);
		};
	}

	private static Scriptable js_constructor(Context cx, Scriptable scope, Object[] args) {
		CharSequence s;
		if (args.length == 0) {
			s = "";
		} else {
			s = ScriptRuntime.toCharSequence(cx, args[0]);
		}
		return new NativeString(s);
	}

	private static Object js_constructorFunc(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		CharSequence s;
		if (args.length == 0) {
			s = "";
		} else if (ScriptRuntime.isSymbol(args[0])) {
			// 19.4.3.2 et.al. Convert a symbol to a string with String() but not
			// new String()
			s = args[0].toString();
		} else {
			s = ScriptRuntime.toCharSequence(cx, args[0]);
		}
		// String(val) converts val to a string value.
		return s instanceof String ? s : s.toString();
	}

	private static Object js_fromCharCode(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		int n = args.length;
		if (n < 1) {
			return "";
		}
		char[] chars = new char[n];
		for (int i = 0; i != n; ++i) {
			chars[i] = ScriptRuntime.toUint16(cx, args[i]);
		}
		return new String(chars);
	}

	private static Object js_fromCodePoint(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		int n = args.length;
		if (n < 1) {
			return "";
		}
		int[] codePoints = new int[n];
		for (int i = 0; i != n; i++) {
			Object arg = args[i];
			int codePoint = ScriptRuntime.toInt32(cx, arg);
			double num = ScriptRuntime.toNumber(cx, arg);
			if (!ScriptRuntime.eqNumber(cx, num, codePoint) || !Character.isValidCodePoint(codePoint)) {
				throw ScriptRuntime.rangeError(cx, "Invalid code point " + ScriptRuntime.toString(cx, arg));
			}
			codePoints[i] = codePoint;
		}
		return new String(codePoints, 0, n);
	}

	private static Object js_charAt(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return charAt(cx, thisObj, args, false);
	}

	private static Object js_charCodeAt(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return charAt(cx, thisObj, args, true);
	}

	private static Object charAt(Context cx, Scriptable thisObj, Object[] args, boolean getCode) {
		// See ECMA 15.5.4.[4,5]
		CharSequence target =
			ScriptRuntime.toCharSequence(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "charAt"));
		double pos = ScriptRuntime.toInteger(cx, args, 0);
		if (pos < 0 || pos >= target.length()) {
			if (!getCode) {
				return "";
			}
			return ScriptRuntime.NaNobj;
		}
		char c = target.charAt((int) pos);
		if (!getCode) {
			return String.valueOf(c);
		}
		return ScriptRuntime.wrapNumber(c);
	}

	private static Object js_indexOf(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String target =
			ScriptRuntime.toString(cx, requireObjectCoercible(cx, thisObj, CLASS_NAME, "indexOf"));
		String searchStr = ScriptRuntime.toString(cx, args, 0);
		double position = ScriptRuntime.toInteger(cx, args, 1);

		if (searchStr.isEmpty()) {
			return position > target.length() ? target.length() : (int) position;
		}
		if (position > target.length()) {
			return -1;
		}
		if (position < 0) {
			position = 0;
		}
		return target.indexOf(searchStr, (int) position);
	}

	private static Object js_startsWith(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String target =
			ScriptRuntime.toString(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "startsWith"));
		checkValidRegex(cx, args, 0, "startsWith");
		String searchStr = ScriptRuntime.toString(cx, args, 0);
		double position = ScriptRuntime.toInteger(cx, args, 1);
		if (position < 0) {
			position = 0;
		} else if (position > target.length()) {
			position = target.length();
		}
		return target.startsWith(searchStr, (int) position);
	}

	private static Object js_endsWith(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String target =
			ScriptRuntime.toString(cx, requireObjectCoercible(cx, thisObj, CLASS_NAME, "endsWith"));
		checkValidRegex(cx, args, 0, "endsWith");
		String searchStr = ScriptRuntime.toString(cx, args, 0);
		double position = ScriptRuntime.toInteger(cx, args, 1);
		if (position < 0) {
			position = 0;
		} else if (Double.isNaN(position) || position > target.length()) {
			position = target.length();
		}
		if (args.length == 0
			|| args.length == 1
			|| (args.length == 2 && Undefined.isUndefined(args[1]))) {
			position = target.length();
		}
		return target.substring(0, (int) position).endsWith(searchStr);
	}

	private static Object js_includes(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String target =
			ScriptRuntime.toString(cx, requireObjectCoercible(cx, thisObj, CLASS_NAME, "includes"));
		String searchStr = ScriptRuntime.toString(cx, args, 0);
		checkValidRegex(cx, args, 0, "includes");
		int position = (int) ScriptRuntime.toInteger(cx, args, 1);
		return target.indexOf(searchStr, position) != -1;
	}

	private static void checkValidRegex(Context cx, Object[] args, int pos, String functionName) {
		if (args.length > pos && args[pos] instanceof Scriptable arg) {
			var reProxy = cx.getRegExp();
			if (reProxy != null) {
				if (reProxy.isRegExp(arg)) {
					if (ScriptableObject.isTrue(ScriptableObject.getProperty(arg, SymbolKey.MATCH, cx), cx)) {
						throw ScriptRuntime.typeError2(cx,
							"msg.first.arg.not.regexp", CLASS_NAME, functionName);
					}
				}
			}
		}
	}

	private static Object js_split(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String thisStr =
			ScriptRuntime.toString(cx, requireObjectCoercible(cx, thisObj, CLASS_NAME, "split"));
		return cx.getRegExp().js_split(cx, scope, thisStr, args);
	}

	private static NativeString realThis(Context cx, Scriptable thisObj) {
		return LambdaConstructor.convertThisObject(cx, thisObj, NativeString.class);
	}

	private static Object js_iterator(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return new NativeStringIterator(cx, scope, requireObjectCoercible(cx, thisObj, CLASS_NAME, "[Symbol.iterator]"));
	}

	private static Object js_toString(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		// ECMA 15.5.4.2: the toString function is not generic.
		CharSequence cs = realThis(cx, thisObj).string;
		return cs instanceof String ? cs : cs.toString();
	}

	private static Object js_toSource(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		CharSequence s = realThis(cx, thisObj).string;
		return "(new String(\"" + ScriptRuntime.escapeString(s.toString(), '"') + "\"))";
	}

	/*
	 * HTML composition aids.
	 */
	private static String tagify(
		Context cx,
		Scriptable thisObj,
		String functionName,
		String tag,
		String attribute,
		Object[] args) {
		String str =
			ScriptRuntime.toString(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, functionName));
		StringBuilder result = new StringBuilder();
		result.append('<').append(tag);

		if (attribute != null && !attribute.isEmpty()) {
			String attributeValue = ScriptRuntime.toString(cx, args, 0);
			attributeValue = attributeValue.replace("\"", "&quot;");
			result.append(' ').append(attribute).append("=\"").append(attributeValue).append('"');
		}
		result.append('>').append(str).append("</").append(tag).append('>');
		return result.toString();
	}

	public CharSequence toCharSequence() {
		return string;
	}

	@Override
	public String toString() {
		return string instanceof String ? (String) string : string.toString();
	}

	/* Make array-style property lookup work for strings.
	 * XXX is this ECMA?  A version check is probably needed. In js too.
	 */
	@Override
	public Object get(Context cx, int index, Scriptable start) {
		if (0 <= index && index < string.length()) {
			return String.valueOf(string.charAt(index));
		}
		return super.get(cx, index, start);
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		if (0 <= index && index < string.length()) {
			return;
		}
		super.put(cx, index, start, value);
	}

	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		if (0 <= index && index < string.length()) {
			return true;
		}
		return super.has(cx, index, start);
	}

	@Override
	public int getAttributes(Context cx, int index) {
		if (0 <= index && index < string.length()) {
			return READONLY | PERMANENT;
		}
		return super.getAttributes(cx, index);
	}

	@Override
	Object[] getIds(Context cx, boolean getNonEnumerable, boolean getSymbols) {
		// Strings have an entry in the property map for each character.
		Object[] sids = super.getIds(cx, getNonEnumerable, getSymbols);
		Object[] a = new Object[sids.length + string.length()];
		int i;
		for (i = 0; i < string.length(); i++) {
			a[i] = i;
		}
		System.arraycopy(sids, 0, a, i, sids.length);
		return a;
	}

	@Override
	protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
		if (!(id instanceof Symbol)) {
			StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, id);
			if (s.stringId == null && 0 <= s.index && s.index < string.length()) {
				String value = String.valueOf(string.charAt(s.index));
				return defaultIndexPropertyDescriptor(cx, value);
			}
		}
		return super.getOwnPropertyDescriptor(cx, id);
	}

	private ScriptableObject defaultIndexPropertyDescriptor(Context cx, Object value) {
		Scriptable scope = getParentScope();
		if (scope == null) {
			scope = this;
		}
		ScriptableObject desc = new NativeObject(cx.factory);
		ScriptRuntime.setBuiltinProtoAndParent(cx, scope, desc, TopLevel.Builtins.Object);
		desc.defineProperty(cx, "value", value, EMPTY);
		desc.defineProperty(cx, "writable", Boolean.FALSE, EMPTY);
		desc.defineProperty(cx, "enumerable", Boolean.TRUE, EMPTY);
		desc.defineProperty(cx, "configurable", Boolean.FALSE, EMPTY);
		return desc;
	}

	/*
	 *
	 * See ECMA 22.1.3.13
	 *
	 */
	private static Object js_match(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object o = requireObjectCoercible(cx, thisObj, CLASS_NAME, "match");
		Object regexp = args.length > 0 ? args[0] : Undefined.INSTANCE;
		var regExpProxy = cx.getRegExp();
		if (regexp != null && !Undefined.isUndefined(regexp)) {
			Object matcher = ScriptRuntime.getObjectElem(cx, scope, regexp, SymbolKey.MATCH);
			// If method is not undefined, it should be a Callable
			if (matcher != null && !Undefined.isUndefined(matcher)) {
				if (!(matcher instanceof Callable)) {
					throw ScriptRuntime.notFunctionError(cx, regexp, matcher, SymbolKey.MATCH.getName());
				}
				return ((Callable) matcher)
					.call(cx, scope, ScriptRuntime.toObject(cx, scope, regexp), new Object[]{o});
			}
		}

		String s = ScriptRuntime.toString(cx, o);
		String regexpToString = Undefined.isUndefined(regexp) ? "" : ScriptRuntime.toString(cx, regexp);

		String flags = null;

		Object compiledRegExp = regExpProxy.compileRegExp(cx, regexpToString, flags);
		Scriptable rx = regExpProxy.wrapRegExp(cx, scope, compiledRegExp);

		Object method = ScriptRuntime.getObjectElem(cx, scope, rx, SymbolKey.MATCH);
		if (!(method instanceof Callable)) {
			throw ScriptRuntime.notFunctionError(cx, rx, method, SymbolKey.MATCH.getName());
		}
		return ((Callable) method).call(cx, scope, rx, new Object[]{s});
	}

	/*
	 *
	 * See ECMA 15.5.4.7
	 *
	 */
	private static Object js_lastIndexOf(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String target =
			ScriptRuntime.toString(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "lastIndexOf"));
		String search = ScriptRuntime.toString(cx, args, 0);
		double end = ScriptRuntime.toNumber(cx, args, 1);

		if (Double.isNaN(end) || end > target.length()) {
			end = target.length();
		} else if (end < 0) {
			end = 0;
		}

		return target.lastIndexOf(search, (int) end);
	}

	/*
	 * See ECMA 15.5.4.15
	 */
	private static Object js_substring(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		CharSequence target =
			ScriptRuntime.toCharSequence(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "substring"));
		int length = target.length();
		double start = ScriptRuntime.toInteger(cx, args, 0);
		double end;

		if (start < 0) {
			start = 0;
		} else if (start > length) {
			start = length;
		}

		if (args.length <= 1 || args[1] == Undefined.INSTANCE) {
			end = length;
		} else {
			end = ScriptRuntime.toInteger(cx, args[1]);
			if (end < 0) {
				end = 0;
			} else if (end > length) {
				end = length;
			}

			// swap if end < start
			if (end < start) {
				double temp = start;
				start = end;
				end = temp;
			}
		}
		return target.subSequence((int) start, (int) end);
	}

	private static Object js_toLowerCase(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		// See ECMA 15.5.4.11
		String thisStr =
			ScriptRuntime.toString(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "toLowerCase"));
		return thisStr.toLowerCase(Locale.ROOT);
	}

	private static Object js_toUpperCase(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		// See ECMA 15.5.4.12
		String thisStr =
			ScriptRuntime.toString(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "toUpperCase"));
		return thisStr.toUpperCase(Locale.ROOT);
	}

	int getLength() {
		return string.length();
	}

	/*
	 * Non-ECMA methods.
	 */
	private static CharSequence js_substr(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		CharSequence target =
			ScriptRuntime.toCharSequence(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "substr"));
		if (args.length < 1) {
			return target;
		}

		double begin = ScriptRuntime.toInteger(cx, args[0]);
		double end;
		int length = target.length();

		if (begin < 0) {
			begin += length;
			if (begin < 0) {
				begin = 0;
			}
		} else if (begin > length) {
			begin = length;
		}

		end = length;
		if (args.length > 1) {
			Object lengthArg = args[1];

			if (!Undefined.isUndefined(lengthArg)) {
				end = ScriptRuntime.toInteger(cx, lengthArg);
				if (end < 0) {
					end = 0;
				}
				end += begin;
				if (end > length) {
					end = length;
				}
			}
		}

		return target.subSequence((int) begin, (int) end);
	}

	/*
	 * Python-esque sequence operations.
	 */
	private static String js_concat(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String target =
			ScriptRuntime.toString(cx, requireObjectCoercible(cx, thisObj, CLASS_NAME, "concat"));
		int N = args.length;
		if (N == 0) {
			return target;
		} else if (N == 1) {
			String arg = ScriptRuntime.toString(cx, args[0]);
			return target.concat(arg);
		}

		// Find total capacity for the final string to avoid unnecessary
		// re-allocations in StringBuilder
		int size = target.length();
		String[] argsAsStrings = new String[N];
		for (int i = 0; i != N; ++i) {
			String s = ScriptRuntime.toString(cx, args[i]);
			argsAsStrings[i] = s;
			size += s.length();
		}

		StringBuilder result = new StringBuilder(size);
		result.append(target);
		for (int i = 0; i != N; ++i) {
			result.append(argsAsStrings[i]);
		}
		return result.toString();
	}

	private static Object js_slice(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		CharSequence target =
			ScriptRuntime.toCharSequence(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "slice"));
		double begin = args.length < 1 ? 0 : ScriptRuntime.toInteger(cx, args[0]);
		double end;
		int length = target.length();
		if (begin < 0) {
			begin += length;
			if (begin < 0) {
				begin = 0;
			}
		} else if (begin > length) {
			begin = length;
		}

		if (args.length < 2 || args[1] == Undefined.INSTANCE) {
			end = length;
		} else {
			end = ScriptRuntime.toInteger(cx, args[1]);
			if (end < 0) {
				end += length;
				if (end < 0) {
					end = 0;
				}
			} else if (end > length) {
				end = length;
			}
			if (end < begin) {
				end = begin;
			}
		}
		return target.subSequence((int) begin, (int) end);
	}

	private static Object js_at(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String str = ScriptRuntime.toString(cx, requireObjectCoercible(cx, thisObj, CLASS_NAME, "at"));
		Object targetArg = (args.length >= 1) ? args[0] : Undefined.INSTANCE;
		int len = str.length();
		int relativeIndex = (int) ScriptRuntime.toInteger(cx, targetArg);

		int k = (relativeIndex >= 0) ? relativeIndex : len + relativeIndex;

		if ((k < 0) || (k >= len)) {
			return Undefined.INSTANCE;
		}

		return str.substring(k, k + 1);
	}

	private static Object js_equals(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String s1 = ScriptRuntime.toString(cx, thisObj);
		String s2 = ScriptRuntime.toString(cx, args, 0);
		return s1.equals(s2);
	}

	private static Object js_equalsIgnoreCase(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String s1 = ScriptRuntime.toString(cx, thisObj);
		String s2 = ScriptRuntime.toString(cx, args, 0);
		return s1.equalsIgnoreCase(s2);
	}

	private static Object js_search(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		requireObjectCoercible(cx, thisObj, CLASS_NAME, "search");
		return cx.getRegExp()
			.action(cx, scope, thisObj, args, RegExp.RA_SEARCH);
	}

	private static Object js_replace(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		requireObjectCoercible(cx, thisObj, CLASS_NAME, "replace");
		return cx.getRegExp()
			.action(cx, scope, thisObj, args, RegExp.RA_REPLACE);
	}

	private static Object js_replaceAll(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		requireObjectCoercible(cx, thisObj, CLASS_NAME, "replaceAll");
		return cx.getRegExp()
			.action(cx, scope, thisObj, args, RegExp.RA_REPLACE_ALL);
	}

	private static Object js_matchAll(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		// See ECMAScript spec 22.1.3.14
		Object o = requireObjectCoercible(cx, thisObj, CLASS_NAME, "matchAll");
		Object regexp = args.length > 0 ? args[0] : Undefined.INSTANCE;
		var regExpProxy = cx.getRegExp();
		if (regexp != null && !Undefined.isUndefined(regexp)) {
			boolean isRegExp =
				regexp instanceof Scriptable && regExpProxy.isRegExp((Scriptable) regexp);
			if (isRegExp) {
				Object flags = ScriptRuntime.getObjectProp(cx, scope, regexp, "flags");
				requireObjectCoercible(cx, flags, CLASS_NAME, "matchAll");
				String flagsStr = ScriptRuntime.toString(cx, flags);
				if (!flagsStr.contains("g")) {
					throw ScriptRuntime.typeError0(cx, "msg.str.match.all.no.global.flag");
				}
			}

			Object matcher = ScriptRuntime.getObjectElem(cx, scope, regexp, SymbolKey.MATCH_ALL);
			// If method is not undefined, it should be a Callable
			if (matcher != null && !Undefined.isUndefined(matcher)) {
				if (!(matcher instanceof Callable)) {
					throw ScriptRuntime.notFunctionError(cx, regexp, matcher, SymbolKey.MATCH_ALL.getName());
				}
				return ((Callable) matcher)
					.call(cx, scope, ScriptRuntime.toObject(cx, scope, regexp), new Object[]{o});
			}
		}

		String s = ScriptRuntime.toString(cx, o);
		String regexpToString = Undefined.isUndefined(regexp) ? "" : ScriptRuntime.toString(cx, regexp);
		Object compiledRegExp = regExpProxy.compileRegExp(cx, regexpToString, "g");
		Scriptable rx = regExpProxy.wrapRegExp(cx, scope, compiledRegExp);

		Object method = ScriptRuntime.getObjectElem(cx, scope, rx, SymbolKey.MATCH_ALL);
		if (!(method instanceof Callable)) {
			throw ScriptRuntime.notFunctionError(cx, rx, method, SymbolKey.MATCH_ALL.getName());
		}
		return ((Callable) method).call(cx, scope, rx, new Object[]{s});
	}

	private static Object js_localeCompare(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		// For now, create and configure a collator instance. I can't
		// actually imagine that this'd be slower than caching them
		// a la ClassCache, so we aren't trying to outsmart ourselves
		// with a caching mechanism for now.
		String thisStr =
			ScriptRuntime.toString(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "localeCompare"));
		// Fork has no per-context locale/Intl feature; use the default collator + Locale.ROOT.
		Collator collator = Collator.getInstance();
		collator.setStrength(Collator.IDENTICAL);
		collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
		return collator.compare(thisStr, ScriptRuntime.toString(cx, args, 0));
	}

	private static Object js_toLocaleLowerCase(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String thisStr =
			ScriptRuntime.toString(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "toLocaleLowerCase"));
		return thisStr.toLowerCase(Locale.ROOT);
	}

	private static Object js_toLocaleUpperCase(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String thisStr =
			ScriptRuntime.toString(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "toLocaleUpperCase"));
		return thisStr.toUpperCase(Locale.ROOT);
	}

	private static Object js_trim(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String str =
			ScriptRuntime.toString(cx, requireObjectCoercible(cx, thisObj, CLASS_NAME, "trim"));
		char[] chars = str.toCharArray();

		int start = 0;
		while (start < chars.length && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[start])) {
			start++;
		}
		int end = chars.length;
		while (end > start && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[end - 1])) {
			end--;
		}

		return str.substring(start, end);
	}

	private static Object js_trimLeft(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String str =
			ScriptRuntime.toString(cx, requireObjectCoercible(cx, thisObj, CLASS_NAME, "trimLeft"));
		char[] chars = str.toCharArray();

		int start = 0;
		while (start < chars.length && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[start])) {
			start++;
		}
		int end = chars.length;

		return str.substring(start, end);
	}

	private static Object js_trimRight(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String str =
			ScriptRuntime.toString(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "trimRight"));
		char[] chars = str.toCharArray();

		int start = 0;

		int end = chars.length;
		while (end > start && ScriptRuntime.isJSWhitespaceOrLineTerminator(chars[end - 1])) {
			end--;
		}

		return str.substring(start, end);
	}

	private static Object js_normalize(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (args.length == 0 || Undefined.isUndefined(args[0])) {
			return Normalizer.normalize(
				ScriptRuntime.toString(cx,
					requireObjectCoercible(cx, thisObj, CLASS_NAME, "normalize")),
				Normalizer.Form.NFC);
		}

		final String formStr = ScriptRuntime.toString(cx, args, 0);

		final Normalizer.Form form;
		if (Normalizer.Form.NFD.name().equals(formStr)) {
			form = Normalizer.Form.NFD;
		} else if (Normalizer.Form.NFKC.name().equals(formStr)) {
			form = Normalizer.Form.NFKC;
		} else if (Normalizer.Form.NFKD.name().equals(formStr)) {
			form = Normalizer.Form.NFKD;
		} else if (Normalizer.Form.NFC.name().equals(formStr)) {
			form = Normalizer.Form.NFC;
		} else {
			throw ScriptRuntime.rangeError(cx,
				"The normalization form should be one of 'NFC', 'NFD', 'NFKC', 'NFKD'.");
		}

		return Normalizer.normalize(
			ScriptRuntime.toString(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "normalize")),
			form);
	}

	private static String js_repeat(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String str =
			ScriptRuntime.toString(cx, requireObjectCoercible(cx, thisObj, CLASS_NAME, "repeat"));
		double cnt = ScriptRuntime.toInteger(cx, args, 0);

		if ((cnt < 0.0) || (cnt == Double.POSITIVE_INFINITY)) {
			throw ScriptRuntime.rangeError(cx, "Invalid count value");
		}

		if (cnt == 0.0 || str.isEmpty()) {
			return "";
		}

		long size = str.length() * (long) cnt;
		// Check for overflow
		if ((cnt > Integer.MAX_VALUE) || (size > Integer.MAX_VALUE)) {
			throw ScriptRuntime.rangeError(cx, "Invalid size or count value");
		}

		StringBuilder retval = new StringBuilder((int) size);
		retval.append(str);

		int i = 1;
		int icnt = (int) cnt;
		while (i <= (icnt / 2)) {
			retval.append(retval);
			i *= 2;
		}
		if (i < icnt) {
			retval.append(retval, 0, str.length() * (icnt - i));
		}

		return retval.toString();
	}

	private static Object js_codePointAt(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		String str =
			ScriptRuntime.toString(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "codePointAt"));
		double cnt = ScriptRuntime.toInteger(cx, args, 0);
		return (cnt < 0 || cnt >= str.length()) ? Undefined.INSTANCE : str.codePointAt((int) cnt);
	}

	/**
	 * @see https://www.ecma-international.org/ecma-262/8.0/#sec-string.prototype.padstart
	 * @see https://www.ecma-international.org/ecma-262/8.0/#sec-string.prototype.padend
	 */
	private static String pad(
		Context cx, Scriptable thisObj, String functionName, Object[] args, boolean atStart) {
		String pad =
			ScriptRuntime.toString(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, functionName));
		long intMaxLength = ScriptRuntime.toLength(cx, args, 0);
		if (intMaxLength <= pad.length()) {
			return pad;
		}

		String filler = " ";
		if (args.length >= 2 && !Undefined.isUndefined(args[1])) {
			filler = ScriptRuntime.toString(cx, args[1]);
			if (filler.isEmpty()) {
				return pad;
			}
		}

		// cast is not really correct here
		int fillLen = (int) (intMaxLength - pad.length());
		StringBuilder concat = new StringBuilder();
		do {
			concat.append(filler);
		} while (concat.length() < fillLen);
		concat.setLength(fillLen);

		if (atStart) {
			return concat.append(pad).toString();
		}

		return concat.insert(0, pad).toString();
	}

	private static Object js_padStart(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return pad(cx, thisObj, "padStart", args, true);
	}

	private static Object js_padEnd(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return pad(cx, thisObj, "padEnd", args, false);
	}

	/**
	 *
	 *
	 * <h1>String.raw (template, ...substitutions)</h1>
	 *
	 * <p>22.1.2.4 String.raw [Draft ECMA-262 / April 28, 2021]
	 */
	private static Object js_raw(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		/* step 1-2 */
		Object arg0 = args.length > 0 ? args[0] : Undefined.INSTANCE;
		Scriptable cooked = ScriptRuntime.toObject(cx, scope, arg0);
		/* step 3 */
		Object rawValue = ScriptRuntime.getObjectProp(cx, cooked, "raw");
		Scriptable raw = ScriptRuntime.toObject(cx, scope, rawValue);
		/* step 4-5 */
		long rawLength = NativeArray.getLengthProperty(cx, raw);
		if (rawLength > Integer.MAX_VALUE) {
			throw ScriptRuntime.rangeError(cx, "raw.length > " + Integer.MAX_VALUE);
		}
		int literalSegments = (int) rawLength;
		if (literalSegments <= 0) {
			return "";
		}
		/* step 6-7 */
		StringBuilder elements = new StringBuilder();
		int nextIndex = 0;
		for (; ; ) {
			/* step 8 a-i */
			Object next;
			next = ScriptRuntime.getObjectIndex(cx, raw, nextIndex);
			String nextSeg = ScriptRuntime.toString(cx, next);
			elements.append(nextSeg);
			nextIndex += 1;
			if (nextIndex == literalSegments) {
				break;
			}

			if (args.length > nextIndex) {
				next = args[nextIndex];
				String nextSub = ScriptRuntime.toString(cx, next);
				elements.append(nextSub);
			}
		}
		return elements;
	}

	private static Object js_isWellFormed(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		CharSequence str =
			ScriptRuntime.toCharSequence(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "isWellFormed"));
		int len = str.length();
		boolean foundLeadingSurrogate = false;
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			if (Character.isHighSurrogate(c)) {
				if (foundLeadingSurrogate) {
					return false;
				}
				foundLeadingSurrogate = true;
			} else if (Character.isLowSurrogate(c)) {
				if (!foundLeadingSurrogate) {
					return false;
				}
				foundLeadingSurrogate = false;
			} else if (foundLeadingSurrogate) {
				return false;
			}
		}
		return !foundLeadingSurrogate;
	}

	private static Object js_toWellFormed(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		CharSequence str =
			ScriptRuntime.toCharSequence(cx,
				requireObjectCoercible(cx, thisObj, CLASS_NAME, "toWellFormed"));
		// true represents a surrogate pair
		// false represents a singular surrogate
		// normal characters aren't present
		Map<Integer, Boolean> surrogates = new HashMap<>();

		int len = str.length();
		char prev = 0;
		int firstSurrogateIndex = -1;
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);

			if (Character.isHighSurrogate(prev) && Character.isLowSurrogate(c)) {
				surrogates.put(i - 1, Boolean.TRUE);
				surrogates.put(i, Boolean.TRUE);
			} else if (Character.isHighSurrogate(c) || Character.isLowSurrogate(c)) {
				surrogates.put(i, Boolean.FALSE);
				if (firstSurrogateIndex == -1) {
					firstSurrogateIndex = i;
				}
			}

			prev = c;
		}

		if (surrogates.isEmpty()) {
			return str.toString();
		}

		StringBuilder sb = new StringBuilder(str.subSequence(0, firstSurrogateIndex));
		for (int i = firstSurrogateIndex; i < len; i++) {
			char c = str.charAt(i);
			Boolean pairOrNormal = surrogates.get(i);
			if (pairOrNormal == null || pairOrNormal) {
				sb.append(c);
			} else {
				sb.append('\uFFFD');
			}
		}

		return sb.toString();
	}

	private static Object js_bold(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return tagify(cx, thisObj, "bold", "b", null, args);
	}

	private static Object js_italics(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return tagify(cx, thisObj, "italics", "i", null, args);
	}

	private static Object js_fixed(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return tagify(cx, thisObj, "fixed", "tt", null, args);
	}

	private static Object js_strike(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return tagify(cx, thisObj, "strike", "strike", null, args);
	}

	private static Object js_small(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return tagify(cx, thisObj, "small", "small", null, args);
	}

	private static Object js_big(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return tagify(cx, thisObj, "big", "big", null, args);
	}

	private static Object js_blink(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return tagify(cx, thisObj, "blink", "blink", null, args);
	}

	private static Object js_sup(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return tagify(cx, thisObj, "sup", "sup", null, args);
	}

	private static Object js_sub(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return tagify(cx, thisObj, "sub", "sub", null, args);
	}

	private static Object js_fontsize(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return tagify(cx, thisObj, "fontsize", "font", "size", args);
	}

	private static Object js_fontcolor(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return tagify(cx, thisObj, "fontcolor", "font", "color", args);
	}

	private static Object js_link(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return tagify(cx, thisObj, "link", "a", "href", args);
	}

	private static Object js_anchor(
		Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return tagify(cx, thisObj, "anchor", "a", "name", args);
	}
}
