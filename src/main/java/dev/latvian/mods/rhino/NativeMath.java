/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * This class implements the Math native object.
 * See ECMA 15.8.
 *
 * @author Norris Boyd
 */

final class NativeMath extends ScriptableObject {
	private static final double LOG2E = 1.4426950408889634;
	private static final Double Double32 = 32d;

	static void init(Scriptable scope, boolean sealed, Context cx) {
		NativeMath obj = new NativeMath();
		obj.setPrototype(getObjectPrototype(scope, cx));
		obj.setParentScope(scope);

		obj.defineProperty(cx, scope, "toSource", 0, (lcx, lscope, thisObj, args) -> "Math", DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "abs", 1, NativeMath::abs, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "acos", 1, NativeMath::acos, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "acosh", 1, NativeMath::acosh, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "asin", 1, NativeMath::asin, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "asinh", 1, NativeMath::asinh, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "atan", 1, NativeMath::atan, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "atanh", 1, NativeMath::atanh, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "atan2", 2, NativeMath::atan2, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "cbrt", 1, NativeMath::cbrt, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "ceil", 1, NativeMath::ceil, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "clz32", 1, NativeMath::clz32, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "cos", 1, NativeMath::cos, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "cosh", 1, NativeMath::cosh, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "exp", 1, NativeMath::exp, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "expm1", 1, NativeMath::expm1, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "f16round", 1, NativeMath::f16round, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "floor", 1, NativeMath::floor, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "fround", 1, NativeMath::fround, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "hypot", 2, NativeMath::hypot, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "imul", 2, NativeMath::imul, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "log", 1, NativeMath::log, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "log1p", 1, NativeMath::log1p, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "log10", 1, NativeMath::log10, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "log2", 1, NativeMath::log2, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "max", 2, NativeMath::max, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "min", 2, NativeMath::min, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "pow", 2, NativeMath::pow, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "random", 0, NativeMath::random, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "round", 1, NativeMath::round, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "sign", 1, NativeMath::sign, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "sin", 1, NativeMath::sin, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "sinh", 1, NativeMath::sinh, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "sqrt", 1, NativeMath::sqrt, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "tan", 1, NativeMath::tan, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "tanh", 1, NativeMath::tanh, DONTENUM, DONTENUM | READONLY);
		obj.defineProperty(cx, scope, "trunc", 1, NativeMath::trunc, DONTENUM, DONTENUM | READONLY);

		obj.defineProperty(cx, "E", ScriptRuntime.wrapNumber(Math.E), DONTENUM | READONLY | PERMANENT);
		obj.defineProperty(cx, "PI", ScriptRuntime.wrapNumber(Math.PI), DONTENUM | READONLY | PERMANENT);
		obj.defineProperty(cx, "LN10", ScriptRuntime.wrapNumber(2.302585092994046), DONTENUM | READONLY | PERMANENT);
		obj.defineProperty(cx, "LN2", ScriptRuntime.wrapNumber(0.6931471805599453), DONTENUM | READONLY | PERMANENT);
		obj.defineProperty(cx, "LOG2E", ScriptRuntime.wrapNumber(LOG2E), DONTENUM | READONLY | PERMANENT);
		obj.defineProperty(cx, "LOG10E", ScriptRuntime.wrapNumber(0.4342944819032518), DONTENUM | READONLY | PERMANENT);
		obj.defineProperty(cx, "SQRT1_2", ScriptRuntime.wrapNumber(0.7071067811865476), DONTENUM | READONLY | PERMANENT);
		obj.defineProperty(cx, "SQRT2", ScriptRuntime.wrapNumber(1.4142135623730951), DONTENUM | READONLY | PERMANENT);

		obj.defineProperty(cx, SymbolKey.TO_STRING_TAG, "Math", DONTENUM | READONLY);

		if (sealed) {
			obj.sealObject(cx);
		}
		defineProperty(scope, "Math", obj, DONTENUM, cx);
	}

	private NativeMath() {
	}

	@Override
	public String getClassName() {
		return "Math";
	}

	private static Object abs(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		// abs(-0.0) should be 0.0, but -0.0 < 0.0 == false
		x = (x == 0.0) ? 0.0 : (x < 0.0) ? -x : x;
		return ScriptRuntime.wrapNumber(x);
	}

	private static Object acos(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		x = (!Double.isNaN(x) && -1.0 <= x && x <= 1.0) ? Math.acos(x) : Double.NaN;
		return ScriptRuntime.wrapNumber(x);
	}

	private static Object asin(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		x = (!Double.isNaN(x) && -1.0 <= x && x <= 1.0) ? Math.asin(x) : Double.NaN;
		return ScriptRuntime.wrapNumber(x);
	}

	private static Object acosh(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		if (!Double.isNaN(x)) {
			return Math.log(x + Math.sqrt(x * x - 1.0));
		}
		return ScriptRuntime.NaNobj;
	}

	private static Object asinh(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		if (Double.isInfinite(x)) {
			return x;
		}
		if (!Double.isNaN(x)) {
			if (x == 0) {
				return (1 / x > 0) ? ScriptRuntime.zeroObj : ScriptRuntime.negativeZeroObj;
			}
			return Math.log(x + Math.sqrt(x * x + 1.0));
		}
		return ScriptRuntime.NaNobj;
	}

	private static Object atan(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(Math.atan(ScriptRuntime.toNumber(cx, args, 0)));
	}

	private static Object atanh(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		if (!Double.isNaN(x) && -1.0 <= x && x <= 1.0) {
			if (x == 0) {
				return (1 / x > 0) ? ScriptRuntime.zeroObj : ScriptRuntime.negativeZeroObj;
			}
			return 0.5 * Math.log((1.0 + x) / (1.0 - x));
		}
		return ScriptRuntime.NaNobj;
	}

	private static Object atan2(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		return ScriptRuntime.wrapNumber(Math.atan2(x, ScriptRuntime.toNumber(cx, args, 1)));
	}

	private static Object cbrt(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(Math.cbrt(ScriptRuntime.toNumber(cx, args, 0)));
	}

	private static Object ceil(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(Math.ceil(ScriptRuntime.toNumber(cx, args, 0)));
	}

	private static Object clz32(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		if (x == 0 || Double.isNaN(x) || Double.isInfinite(x)) {
			return Double32;
		}
		long n = ScriptRuntime.toUint32(x);
		if (n == 0) {
			return Double32;
		}
		return (double) Integer.numberOfLeadingZeros((int) n);
	}

	private static Object cos(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		return ScriptRuntime.wrapNumber(Double.isInfinite(x) ? Double.NaN : Math.cos(x));
	}

	private static Object cosh(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(Math.cosh(ScriptRuntime.toNumber(cx, args, 0)));
	}

	private static Object exp(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		x = (x == Double.POSITIVE_INFINITY) ? x : (x == Double.NEGATIVE_INFINITY) ? 0.0 : Math.exp(x);
		return ScriptRuntime.wrapNumber(x);
	}

	private static Object expm1(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(Math.expm1(ScriptRuntime.toNumber(cx, args, 0)));
	}

	private static Object f16round(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(js_f16round(ScriptRuntime.toNumber(cx, args, 0)));
	}

	private static Object floor(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(Math.floor(ScriptRuntime.toNumber(cx, args, 0)));
	}

	private static Object fround(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		// Rely on Java to truncate down to a "float" here
		return ScriptRuntime.wrapNumber((float) ScriptRuntime.toNumber(cx, args, 0));
	}

	private static Object hypot(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(js_hypot(args, cx));
	}

	private static Object imul(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(js_imul(args, cx));
	}

	private static Object log(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		// Java's log(<0) = -Infinity; we need NaN
		return ScriptRuntime.wrapNumber((x < 0) ? Double.NaN : Math.log(x));
	}

	private static Object log1p(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(Math.log1p(ScriptRuntime.toNumber(cx, args, 0)));
	}

	private static Object log10(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(Math.log10(ScriptRuntime.toNumber(cx, args, 0)));
	}

	private static Object log2(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		// Java's log(<0) = -Infinity; we need NaN
		return ScriptRuntime.wrapNumber((x < 0) ? Double.NaN : Math.log(x) * LOG2E);
	}

	private static Object max(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = Double.NEGATIVE_INFINITY;
		for (Object arg : args) {
			// if (x < d) x = d; does not work due to -0.0 >= +0.0
			x = Math.max(x, ScriptRuntime.toNumber(cx, arg));
		}
		return ScriptRuntime.wrapNumber(x);
	}

	private static Object min(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = Double.POSITIVE_INFINITY;
		for (Object arg : args) {
			x = Math.min(x, ScriptRuntime.toNumber(cx, arg));
		}
		return ScriptRuntime.wrapNumber(x);
	}

	private static Object pow(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		return ScriptRuntime.wrapNumber(js_pow(x, ScriptRuntime.toNumber(cx, args, 1)));
	}

	private static Object random(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(Math.random());
	}

	private static Object round(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		if (!Double.isNaN(x) && !Double.isInfinite(x)) {
			// Round only finite x
			long l = Math.round(x);
			if (l != 0) {
				x = l;
			} else {
				// We must propagate the sign of d into the result
				if (x < 0.0) {
					x = ScriptRuntime.negativeZero;
				} else if (x != 0.0) {
					x = 0.0;
				}
			}
		}
		return ScriptRuntime.wrapNumber(x);
	}

	private static Object sign(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		if (!Double.isNaN(x)) {
			if (x == 0) {
				return (1 / x > 0) ? ScriptRuntime.zeroObj : ScriptRuntime.negativeZeroObj;
			}
			return Math.signum(x);
		}
		return ScriptRuntime.NaNobj;
	}

	private static Object sin(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		double x = ScriptRuntime.toNumber(cx, args, 0);
		return ScriptRuntime.wrapNumber(Double.isInfinite(x) ? Double.NaN : Math.sin(x));
	}

	private static Object sinh(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(Math.sinh(ScriptRuntime.toNumber(cx, args, 0)));
	}

	private static Object sqrt(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(Math.sqrt(ScriptRuntime.toNumber(cx, args, 0)));
	}

	private static Object tan(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(Math.tan(ScriptRuntime.toNumber(cx, args, 0)));
	}

	private static Object tanh(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(Math.tanh(ScriptRuntime.toNumber(cx, args, 0)));
	}

	private static Object trunc(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return ScriptRuntime.wrapNumber(js_trunc(ScriptRuntime.toNumber(cx, args, 0)));
	}

	// See Ecma 15.8.2.13
	private static double js_pow(double x, double y) {
		double result;
		if (Double.isNaN(y)) {
			// y is NaN, result is always NaN
			result = y;
		} else if (y == 0) {
			// Java's pow(NaN, 0) = NaN; we need 1
			result = 1.0;
		} else if (x == 0) {
			// Many differences from Java's Math.pow
			if (1 / x > 0) {
				result = (y > 0) ? 0 : Double.POSITIVE_INFINITY;
			} else {
				// x is -0, need to check if y is an odd integer
				long y_long = (long) y;
				if (y_long == y && (y_long & 0x1) != 0) {
					result = (y > 0) ? -0.0 : Double.NEGATIVE_INFINITY;
				} else {
					result = (y > 0) ? 0.0 : Double.POSITIVE_INFINITY;
				}
			}
		} else {
			result = Math.pow(x, y);
			if (Double.isNaN(result)) {
				// Check for broken Java implementations that gives NaN
				// when they should return something else
				if (y == Double.POSITIVE_INFINITY) {
					if (x < -1.0 || 1.0 < x) {
						result = Double.POSITIVE_INFINITY;
					} else if (-1.0 < x && x < 1.0) {
						result = 0;
					}
				} else if (y == Double.NEGATIVE_INFINITY) {
					if (x < -1.0 || 1.0 < x) {
						result = 0;
					} else if (-1.0 < x && x < 1.0) {
						result = Double.POSITIVE_INFINITY;
					}
				} else if (x == Double.POSITIVE_INFINITY) {
					result = (y > 0) ? Double.POSITIVE_INFINITY : 0.0;
				} else if (x == Double.NEGATIVE_INFINITY) {
					long y_long = (long) y;
					if (y_long == y && (y_long & 0x1) != 0) {
						// y is odd integer
						result = (y > 0) ? Double.NEGATIVE_INFINITY : -0.0;
					} else {
						result = (y > 0) ? Double.POSITIVE_INFINITY : 0.0;
					}
				}
			}
		}
		return result;
	}

	// Based on code from https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Math/hypot
	private static double js_hypot(Object[] args, Context cx) {
		if (args == null) {
			return 0.0;
		}
		double y = 0.0;

		// Spec and tests say that any "Infinity" result takes precedence.
		boolean hasNaN = false;
		boolean hasInfinity = false;

		for (Object o : args) {
			double d = ScriptRuntime.toNumber(cx, o);
			if (Double.isNaN(d)) {
				hasNaN = true;
			} else if (Double.isInfinite(d)) {
				hasInfinity = true;
			} else {
				y += d * d;
			}
		}

		if (hasInfinity) {
			return Double.POSITIVE_INFINITY;
		}
		if (hasNaN) {
			return Double.NaN;
		}
		return Math.sqrt(y);
	}

	private static double js_f16round(double x) {
		// Handle special cases
		if (Double.isNaN(x)) {
			return Double.NaN;
		}
		if (x == 0.0) {
			return x; // Preserve sign of zero
		}
		if (Double.isInfinite(x)) {
			return x;
		}

		// Extract components from double precision
		long bits = Double.doubleToLongBits(x);
		int sign = (int) (bits >>> 63);
		int exponent = (int) ((bits >>> 52) & 0x7FF);
		long mantissa = bits & 0x000FFFFFFFFFFFFFL;

		// Adjust from double bias (1023) to float16 bias (15)
		exponent = exponent - 1023 + 15;

		// Handle overflow to infinity
		if (exponent >= 31) {
			return (sign != 0) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
		}

		// Handle underflow and subnormal values
		if (exponent < 0) {
			return handleSubnormalF16(sign, exponent, mantissa);
		}

		// Normal value: round mantissa from 52 to 10 bits
		return handleNormalF16(sign, exponent, mantissa);
	}

	private static double handleSubnormalF16(int sign, int exponent, long mantissa) {
		// Values below 2^-24 underflow to zero
		if (exponent < -10) {
			return (sign != 0) ? -0.0 : 0.0;
		}

		// Special case: exactly 2^-25 rounds to zero (ties-to-even)
		if (exponent == -10 && mantissa == 0) {
			return (sign != 0) ? -0.0 : 0.0;
		}

		// Special case: slightly above 2^-25 rounds to 2^-24
		if (exponent == -10 && mantissa > 0) {
			double smallestSubnormal = 5.960464477539063e-8; // 2^-24
			return (sign != 0) ? -smallestSubnormal : smallestSubnormal;
		}

		// Convert to subnormal representation
		int totalShift = 42 + (1 - exponent);
		mantissa = mantissa | (1L << 52); // Add implicit 1 bit

		// Extract rounding information before shift
		long roundBit = (mantissa >> (totalShift - 1)) & 1;
		long stickyBits = mantissa & ((1L << (totalShift - 1)) - 1);

		// Shift to get 10-bit mantissa
		mantissa >>>= totalShift;

		// Apply ties-to-even rounding
		if (roundBit == 1 && (stickyBits != 0 || (mantissa & 1) == 1)) {
			mantissa++;
		}

		// Reconstruct subnormal value
		if (mantissa == 0) {
			return (sign != 0) ? -0.0 : 0.0;
		}

		// Check for overflow to normal range
		if (mantissa >= (1L << 10)) {
			// Smallest normal = 2^-14
			return (sign != 0) ? -6.103515625e-5 : 6.103515625e-5;
		}

		// Subnormal value = 2^-14 * (mantissa / 1024)
		double value = Math.scalb((double) mantissa / 1024.0, -14);
		return (sign != 0) ? -value : value;
	}

	private static double handleNormalF16(int sign, int exponent, long mantissa) {
		// Add implicit 1 bit for normal values
		long fullMantissa = mantissa | (1L << 52);

		// Extract rounding information
		long roundBit = (fullMantissa >> 41) & 1;
		long stickyBits = fullMantissa & ((1L << 41) - 1);
		fullMantissa >>>= 42;

		// Handle boundary between largest subnormal and smallest normal
		if (exponent == 0) {
			if (fullMantissa == 2046) {
				// Exactly the largest subnormal
				return reconstructSubnormalF16(sign, 1023);
			} else if (fullMantissa == 2047 && roundBit == 0 && stickyBits == 0) {
				// Midpoint: ties-to-even rounds to smallest normal
				return reconstructNormalF16(sign, 1, 0);
			}
		}

		// Extract 10-bit mantissa (remove implicit 1)
		mantissa = fullMantissa & 0x3FF;

		// Apply ties-to-even rounding
		if (roundBit == 1 && (stickyBits != 0 || (mantissa & 1) == 1)) {
			mantissa++;
		}

		// Handle mantissa overflow
		if (mantissa >= (1L << 10)) {
			mantissa = 0;
			exponent++;
			if (exponent >= 31) {
				return (sign != 0) ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
			}
		}

		// Reconstruct the value
		if (exponent == 0) {
			return reconstructSubnormalF16(sign, mantissa);
		} else {
			return reconstructNormalF16(sign, exponent, mantissa);
		}
	}

	private static double reconstructSubnormalF16(int sign, long mantissa) {
		if (mantissa == 0) {
			return (sign != 0) ? -0.0 : 0.0;
		}
		double value = Math.scalb((double) mantissa / 1024.0, -14);
		return (sign != 0) ? -value : value;
	}

	private static double reconstructNormalF16(int sign, int exponent, long mantissa) {
		long resultBits = ((long) sign << 63) | (((long) (exponent + 1023 - 15)) << 52) | (mantissa << 42);
		return Double.longBitsToDouble(resultBits);
	}

	private static double js_trunc(double d) {
		return ((d < 0.0) ? Math.ceil(d) : Math.floor(d));
	}

	// From EcmaScript 6 section 20.2.2.19
	private static int js_imul(Object[] args, Context cx) {
		if (args == null) {
			return 0;
		}

		int x = ScriptRuntime.toInt32(cx, args, 0);
		int y = ScriptRuntime.toInt32(cx, args, 1);
		return x * y;
	}
}
