package com.sph.easytool.conf

import com.sph.easytool.conf.ConfigTest.Status.{FINISHED, Status}
import com.sph.easytool.conf.ConfigTest.{Application, Status}
import org.json4s.DefaultFormats
import org.json4s.ext.EnumNameSerializer
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
    assertResult("rt02-rt01")(app03.getProp("app.test02"))
    assertResult("rt03-drt031")(app03.getProp("app.test03"))
    assertResult("rt04-drt041")(app03.getProp("app.test04"))
  }

  test("testBind") {
    val args: Array[String] = Array()
    val conf = Config(args, Seq("bind01"))
    val bind01 = conf.getConfig("bind01")
    val application = bind01.bind[Application]()
    application
  }

  test("testBindEnum") {
    val args: Array[String] = Array()
    val conf = Config(args, Seq("bind01"))
    val bind01 = conf.getConfig("bind01")
    val formats = DefaultFormats + new EnumNameSerializer(Status)
    val application = bind01.bind[Application](formats)
    application
  }

}

object ConfigTest {
  case class Application(
      name: String,
      sourceField: Seq[String],
      sourceConfig: SourceConfig,
      status: Status = FINISHED
  )
  case class SourceConfig(source: String, sink: String)
  object Status extends Enumeration {
    type Status = Value
    val RUNNING, FINISHED = Value
  }
}
