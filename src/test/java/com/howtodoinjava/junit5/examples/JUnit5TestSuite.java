package com.howtodoinjava.junit5.examples;

import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.IncludePackages;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

//@SelectPackages("com.howtodoinjava.junit5.examples")
//@IncludePackages("com.howtodoinjava.junit5.examples.packageC")
//@ExcludeTags("PROD")

@Suite
@SelectClasses({ExpectedExceptionTest.class, ParameterizedTests.class})
public class JUnit5TestSuite {

}



