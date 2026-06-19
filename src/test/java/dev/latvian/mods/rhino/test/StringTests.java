package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

/**
 * Tests for String and RegExp.
 */
@SuppressWarnings("unused")
public class StringTests {
	public static final RhinoTest TEST = new RhinoTest("strings");

	@Test
	public void stringAt() {
		TEST.test("stringAt", """
			const s = 'abcde';
			console.info(s.at(0));
			console.info(s.at(-1));
			console.info(s.at(10));
			""", """
			a
			e
			undefined
			""");
	}

	@Test
	public void stringReplaceAll() {
		TEST.test("stringReplaceAll", """
			console.info('aabbcc'.replaceAll('b', '.'));
			console.info('aabbcc'.replaceAll('', '-'));
			console.info('aabbcc'.replaceAll(/b/g, '.'));
			console.info('x=1, y=2'.replaceAll(/(\\w)=(\\d)/g, '$2:$1'));
			console.info('aabbcc'.replaceAll('b', m => m.toUpperCase()));
			try {
				'aabbcc'.replaceAll(/b/, '.');
			} catch (e) {
				console.info('type error');
			}
			""", """
			aa..cc
			-a-a-b-b-c-c-
			aa..cc
			1:x, 2:y
			aaBBcc
			type error
			""");
	}

	@Test
	public void regexpConstructorWithFlags() {
		TEST.test("regexpConstructorWithFlags", """
			const base = /ab+c/g;
			const re = new RegExp(base, 'i');
			console.info(re.source);
			console.info(re.global);
			console.info(re.ignoreCase);
			console.info(re.test('ABBC'));
			const copy = new RegExp(base);
			console.info(copy.source + ' ' + copy.global);
			console.info(new RegExp(/x/m, undefined).multiline);
			""", """
			ab+c
			false
			true
			true
			ab+c true
			true
			""");
	}

	@Test
	public void basicTemplateLiteral() {
		TEST.test("basicTemplateLiteral", """
			let x = 1;
			let y = 2;
			console.info(`hello! x=${x} y=${y}`);
			console.info(`sum=${x + y}`);
			""", """
			hello! x=1 y=2
			sum=3
			""");
	}

	@Test
	public void nestedTemplateLiteral() {
		TEST.test("nestedTemplateLiteral", "console.info(`outer-${`inner-${1 + 1}`}`);", "outer-inner-2");
	}

	@Test
	public void multilineTemplateLiteral() {
		TEST.test("multilineTemplateLiteral", """
			console.info(`line1
			line2`);
			""", "line1\nline2");
	}

	@Test
	public void taggedTemplateBasicCall() {
		TEST.test("taggedTemplateBasicCall", """
			function tag(strings, ...values) {
				console.info(strings.length);
				console.info(strings.join('|'));
				console.info(strings.raw.join('|'));
				console.info(values.join(','));
			}
			tag`a${1}b\\nc${2}d`;
			""", """
			3
			a|b
			c|d
			a|b\\nc|d
			1,2
			""");
	}
}
