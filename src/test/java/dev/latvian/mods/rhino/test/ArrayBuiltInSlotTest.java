package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link dev.latvian.mods.rhino.BuiltInSlot} based on NativeArray's
 * {@code length} property (which is its only major use thus far). Tests
 * are meant to ensure the slot updates properly, supports redefinition,
 * and throws for negative numbers.
 */
@SuppressWarnings("unused")
public class ArrayBuiltInSlotTest {
	public static final RhinoTest TEST = new RhinoTest("array-builtinslot");

	@Test
	public void lengthDescriptor() {
		TEST.test("lengthDescriptor", """
			const a = [1, 2, 3];
			console.info(a.hasOwnProperty('length'));
			const d = Object.getOwnPropertyDescriptor(a, 'length');
			console.info(d.value + ',' + d.writable + ',' + d.enumerable + ',' + d.configurable);
			console.info('get' in d);
			""", """
			true
			3,true,false,false
			false
			""");
	}

	@Test
	public void lengthTracksBackingField() {
		TEST.test("lengthTracksBackingField", """
			const a = [1, 2, 3];
			a.push(4);
			console.info(a.length);
			a[9] = 'x';
			console.info(a.length);
			delete a[9];
			console.info(a.length);
			""", """
			4
			10
			10
			""");
	}

	@Test
	public void assignmentTruncatesAndGrows() {
		TEST.test("assignmentTruncatesAndGrows", """
			const a = [1, 2, 3, 4, 5];
			a.length = 2;
			console.info(a.join(','));
			console.info(a.hasOwnProperty(2) + ',' + (2 in a));
			a.length = 4;
			console.info(a.length);
			console.info(a.hasOwnProperty(3));
			""", """
			1,2
			false,false
			4
			false
			""");
	}

	@Test
	public void invalidLengthThrowsRangeError() {
		TEST.test("invalidLengthThrowsRangeError", """
			function setLen(v) {
				const g = [1, 2, 3];
				try { g.length = v; return 'len ' + g.length; }
				catch (e) { return e.name; }
			}
			console.info(setLen(-1));
			console.info(setLen(1.5));
			console.info(setLen(4294967296));
			console.info(setLen('abc'));
			console.info(setLen('3'));
			""", """
			RangeError
			RangeError
			RangeError
			RangeError
			len 3
			""");
	}

	@Test
	public void definePropertyValueTruncates() {
		// this is the main thing BuiltInSlot is actually used for:
		// Object.defineProperty with a smaller {value} goes through
		// BuiltInSlot.applyNewDescriptor and truncates the array
		// like an assignment would
		TEST.test("definePropertyValueTruncates", """
			const a = [1, 2, 3, 4];
			Object.defineProperty(a, 'length', {value: 2});
			console.info(a.join(','));
			console.info(a.hasOwnProperty(2));
			Object.defineProperty(a, 'length', {value: 3});
			console.info(a.length + ',' + a.hasOwnProperty(2));
			""", """
			1,2
			false
			3,false
			""");
	}
}
