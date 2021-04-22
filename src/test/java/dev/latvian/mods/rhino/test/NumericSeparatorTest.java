package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

public class NumericSeparatorTest {
	public static final RhinoTest TEST = new RhinoTest("numeric_separator");

	@Test
	public void decimalSeparator() {
		TEST.test("decimalSeparator", "console.info(1_000_000)", "1000000");
	}

	@Test
	public void hexSeparator() {
		TEST.test("hexSeparator", "console.info(0x1_F)", "31");
	}

	@Test
	public void binarySeparator() {
		TEST.test("binarySeparator", "console.info(0b1010_1010)", "170");
	}

	@Test
	public void octalSeparator() {
		TEST.test("octalSeparator", "console.info(0o17_5)", "125");
	}

	@Test
	public void fractionAndExponentSeparator() {
		TEST.test("fractionAndExponentSeparator", "console.info(1_0e1_0)", "100000000000");
	}

	@Test
	public void trailingSeparatorIsError() {
		TEST.test("trailingSeparatorIsError", "try { eval('1_'); console.info('FAIL'); } catch (e) { console.info('OK') }", "OK");
	}

	@Test
	public void doubleSeparatorIsError() {
		TEST.test("doubleSeparatorIsError", "try { eval('1__0'); console.info('FAIL'); } catch (e) { console.info('OK') }", "OK");
	}

	@Test
	public void legacyOctalRejectsSeparator() {
		TEST.test("legacyOctalRejectsSeparator", "try { eval('0123_4'); console.info('FAIL'); } catch (e) { console.info('OK') }", "OK");
	}
}
