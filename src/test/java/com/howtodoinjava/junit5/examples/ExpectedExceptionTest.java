package com.howtodoinjava.junit5.examples;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectPackages;

@SelectPackages("com.howtodoinjava.junit5.examples")
public class ExpectedExceptionTest {
	@Test
	void testExpectedException() {

		NumberFormatException thrown = Assertions.assertThrows(NumberFormatException.class, () -> {
			Integer.parseInt("one");
		}, "NumberFormatException was expected");

		Assertions.assertEquals("For input string: \"one\"", thrown.getMessage());
	}

	@Test
	void testExpectedExceptionWithParentType() {

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			Integer.parseInt("One");
		});
	}

	@Test
	void testExpectedExceptionFail() {

		NumberFormatException thrown = Assertions
				.assertThrows(NumberFormatException.class, () -> {
					Integer.parseInt("one");
				}, "NumberFormatException error was expected");

		assertTrue(thrown.getMessage().contains("For input string"));

	}
}
