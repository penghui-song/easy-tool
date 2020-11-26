package com.sph.easytool.conf

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PlaceholderParserTest extends AnyFunSuite {

  test("testParse") {

    val parser = new PlaceholderParser
    val props = new Props
    props.put("a", "a1")
    props.put("b", "b1")

    assertResult("ab1c")(parser.parse("a${b}c", props))
    assertResult("b1c")(parser.parse("${b}c", props))
    assertResult("b1")(parser.parse("${b}", props))
    assertResult("a1b1")(parser.parse("${a}${b}", props))
    assertResult("ab")(parser.parse("ab", props))
  }

}
