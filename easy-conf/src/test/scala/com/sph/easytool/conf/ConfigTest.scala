package com.sph.easytool.conf

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ConfigTest extends AnyFunSuite {

  test("testGetConfig") {

    val args: Array[String] =
      Array("--app.test01", "t01", "--app01.profile", "test")
    val conf = Config(args, Seq("app01", "app02"), Seq("app03"))
    val app01 = conf.getConfig("app01")
    val app02 = conf.getConfig("app02")
    val app03 = conf.getConfig("app03")
    assertResult("t01")(app01.getProp("app.test01"))
    assertResult("rt02")(app01.getProp("app.test02"))
    assertResult("trt03")(app01.getProp("app.test03"))
    assertResult("t01")(app02.getProp("app.test01"))
    assertResult("rt02")(app02.getProp("app.test02"))
    assertResult("rt03")(app02.getProp("app.test03"))
    assertResult("rt01")(app03.getProp("app.test01"))
  }

}
