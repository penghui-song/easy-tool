package com.sph.easytool.conf

import com.alibaba.nacos.api.NacosFactory
import com.alibaba.nacos.api.config.ConfigType
import com.sph.easytool.conf.Config.{
  NACOS_CONFIG,
  NACOS_CONFIG_DATA_ID,
  NACOS_CONFIG_GROUP
}
import com.sph.easytool.conf.ConfigTest.Status.{FINISHED, Status}
import com.sph.easytool.conf.ConfigTest.{Application, SourceConfig, Status}
import org.json4s.DefaultFormats
import org.json4s.ext.EnumNameSerializer
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.yaml.snakeyaml.Yaml

import java.util.Properties

@RunWith(classOf[JUnitRunner])
class ConfigTest extends AnyFunSuite {

  test("testGetConfig") {

    val args: Array[String] =
      Array("--app.test01", "t01", "--app01.profile", "test")
    val conf = Config(args, Seq("app01", "app02"), Seq("app03"))
    val app01 = conf.getConfig("app01")
    val app02 = conf.getConfig("app02")
    val app03 = conf.getConfig("app03")
    assertResult("t01")(app01.getString("app.test01"))
    assertResult("rt02")(app01.getString("app.test02"))
    assertResult("trt03")(app01.getString("app.test03"))
    assertResult("t01")(app02.getString("app.test01"))
    assertResult("rt02")(app02.getString("app.test02"))
    assertResult("rt03")(app02.getString("app.test03"))
    assertResult("rt01")(app03.getString("app.test01"))
    assertResult("rt02-rt01")(app03.getString("app.test02"))
    assertResult("rt03-drt031")(app03.getString("app.test03"))
    assertResult("rt04-drt041")(app03.getString("app.test04"))
  }

  test("testBind") {
    val args: Array[String] = Array()
    val conf = Config(args, Seq("bind01"))
    val bind01 = conf.getConfig("bind01")
    val application = bind01.bind[Application]()
    assertResult("app01")(application.name)
    assertResult(Seq("field01", "field02"))(application.sourceField)
    assertResult(SourceConfig("source01", "sink01"))(application.sourceConfig)
    assertResult(Status.FINISHED)(application.status)
  }

  test("testBindEnum") {
    val args: Array[String] = Array()
    val conf = Config(args, Seq("bind01"))
    val bind01 = conf.getConfig("bind01")
    val formats = DefaultFormats + new EnumNameSerializer(Status)
    val application = bind01.bind[Application](formats)
    assertResult(Status.RUNNING)(application.status)
  }

  test("testNacos") {
    val props = Config(configs = Seq("nacos")).getConfig("nacos")
    println("Change Before: " + props.getString("job.sink"))
    val yaml = new Yaml()
    val properties = new Properties()
    import scala.collection.JavaConversions._
    properties.putAll(props.toMap(NACOS_CONFIG))
    val configService = NacosFactory.createConfigService(properties)
    configService.publishConfig(
      props.getString(NACOS_CONFIG_DATA_ID),
      props.getString(NACOS_CONFIG_GROUP),
      yaml.dump(
        yaml.load(this.getClass.getResourceAsStream("/test_nacos_02.yml"))
      ),
      ConfigType.YAML.getType
    )
    Thread.sleep(5000)
    println("Change After: " + props.getString("job.sink"))
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
