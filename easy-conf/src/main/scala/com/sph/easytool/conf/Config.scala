package com.sph.easytool.conf

import java.io.InputStream
import java.util.Properties

import com.sph.easytool.conf.Config.{ACTIVE_PROFILE_SUFFIX, DEFAULT_CONFIG}
import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  *
  * Quickly to config your application
  *
  * Priority for configuration
  *     (1) Command-line arguments
  *     (2) The external file specified with config: --$conf file:///xxx
  *     (3) Configuration file with profile ($profile/$conf.yml or $profile/$conf.properties) outside the JAR
  *     (4) Configuration file ($conf.yml or $conf.properties) outside the JAR
  *     (5) Configuration file with profile ($profile/$conf.yml or $profile/$conf.properties) in the classpath
  *     (6) Configuration file ($conf.yml or $conf.properties) in the classpath
  *
  * Special parameters:
  *    (1) --$conf: Specify external configuration file
  *        example: --app file:///tmp/app.yml
  *    (2) --$conf.profile: Specify active profile
  *        example: --app.profile: test
  * @param args  args from command line
  * @param configs configs you will use
  */
class Config(
    args: Array[String],
    configs: Seq[String],
    noArgConfigs: Seq[String]
) extends Logging {

  private val confMap: mutable.Map[String, Props] = mutable.Map()

  init()

  def getConfig(config: String = DEFAULT_CONFIG): Props = confMap(config)

  private def init(): Unit = {
    val argsProps = fromArgs(args)
    configs.foreach(initForOne(_, argsProps, hasArgs = true))
    noArgConfigs.foreach(initForOne(_, argsProps, hasArgs = false))
  }

  private def initForOne(
      confPrefix: String,
      argsProps: Properties,
      hasArgs: Boolean
  ): Unit = {
    val props = new Props
    tryFailAndLogWarnNecessary { props.putAll(fromClassPathAuto(confPrefix)) }
    tryFailAndLogWarnNecessary { props.putAll(fromFile(confPrefix)) }
    val profileConf = confPrefix + ACTIVE_PROFILE_SUFFIX
    val profileActive = argsProps.getProperty(
      profileConf,
      props.getOrElse(profileConf, "")
    )
    val externalFile = argsProps.getProperty(
      confPrefix,
      props.getOrElse(confPrefix, "")
    )
    if (profileActive.nonEmpty) {
      tryFailAndLogWarnNecessary {
        props.putAll(fromClassPathAuto(s"$profileActive/$confPrefix"))
        props.putAll(fromFile(confPrefix))
      }
      tryFailAndLogWarnNecessary {
        props.putAll(fromFile(s"$profileActive/$confPrefix"))
      }
      log.info(s"$confPrefix activated profile: " + profileActive)
    }
    if (externalFile.nonEmpty) {
      props.putAll(fromFile(externalFile))
      log.info(
        s"$confPrefix  conf is overwritten by external file: $externalFile"
      )
    }
    if (hasArgs) props.putAll(argsProps)
    confMap(confPrefix) = props
  }

  private def tryFailAndLogWarnNecessary[A](
      any: => A,
      msg: String = ""
  ): Unit = {
    Try(any) match {
      case Success(_) =>
      case Failure(_) => if (msg.nonEmpty) log.warn(msg)
    }
  }

  private def fromClassPathAuto(filePrefix: String): Properties = {
    var in: InputStream = this.getClass.getResourceAsStream(s"/$filePrefix.yml")
    var file: String = ""
    if (in == null) {
      in = this.getClass.getResourceAsStream(s"/$filePrefix.yaml")
      if (in == null) {
        file = s"$filePrefix.properties"
      } else {
        file = s"$filePrefix.yaml"
      }
    } else {
      file = s"$filePrefix.yml"
    }
    log.debug(s"load classpath file: $file")
    fromClassPath(file)
  }

  private def fromClassPath(file: String): Properties = {
    if (isYml(file)) {
      fromClassPathYml(file)
    } else {
      val ps = new Properties()
      val in = this.getClass.getResourceAsStream("/" + file)
      in match {
        case null => log.debug(s"classpath file not exists : $file")
        case _    => ps.load(in)
      }
      ps
    }
  }

  private def fromClassPathYml(file: String): Properties = {
    val ymlFile = this.getClass.getResourceAsStream("/" + file)
    val ps = new Properties()
    ymlFile match {
      case null => log.debug(s"classpath file not exists : $file")
      case _ =>
        val yaml = new Yaml()
        val ymlMap =
          yaml.load(ymlFile).asInstanceOf[java.util.HashMap[String, Object]]
        ps.putAll(ymlMap2Properties(ymlMap))
    }
    ps
  }

  private def ymlMap2Properties(
      ymlMap: java.util.Map[String, Object]
  ): Properties = {
    val ps = new Properties()
    ymlMap.foreach(ele => {
      ele._2 match {
        case e if e == null => ps.setProperty(ele._1, "")
        case e: java.util.Map[_, _] =>
          ps.putAll(
            ymlMap2Properties(
              e.map(m => (ele._1 + "." + m._1, m._2.asInstanceOf[Object]))
            )
          )
        case e: java.util.List[_] => ps.setProperty(ele._1, e.mkString(","))
        case _                    => ps.setProperty(ele._1, ele._2.toString)
      }
    })
    ps
  }

  private def fromArgs(args: Array[String]): Properties = {
    val ps = new Properties()
    val argsPair = args.map(_.trim).partition(_.startsWith("--"))
    argsPair._1
      .zip(argsPair._2)
      .foreach(pair => ps.setProperty(pair._1.drop(2), pair._2))
    ps
  }

  private def fromFile(file: String): Properties = {
    if (isYml(file)) {
      fromFileYml(file)
    } else {
      val ps = new Properties()
      ps.load(Source.fromFile(file).reader())
      ps
    }
  }

  private def fromFileYml(file: String): Properties = {
    val ps = new Properties()
    val yaml = new Yaml()
    val confMap = yaml
      .load(Source.fromFile(file).reader())
      .asInstanceOf[java.util.HashMap[String, Object]]
    ps.putAll(ymlMap2Properties(confMap))
    ps
  }

  private def isYml(file: String): Boolean = {
    file match {
      case f if f.endsWith(".yml") || f.endsWith(".yaml") => true
      case _                                              => false
    }
  }

}

object Config {

  private val DEFAULT_CONFIG: String = "app"
  private val ACTIVE_PROFILE_SUFFIX = ".profile"

  def apply(
      args: Array[String],
      configs: Seq[String] = Seq(DEFAULT_CONFIG),
      noArgConfigs: Seq[String] = Seq()
  ): Config = new Config(args, configs, noArgConfigs)

  /**
    * 获取key的所有子集并转为map
    */
  def toMap(key: String, props: Properties): Map[String, String] = {
    props
      .filter(!_._1.equals(key))
      .filter(_._1.startsWith(key))
      .map(p => (p._1.diff(key + "."), p._2))
      .toMap
  }
}
