package com.sph.easytool.conf

import com.alibaba.nacos.api.NacosFactory
import com.alibaba.nacos.api.config.listener.AbstractListener
import com.sph.easytool.conf.Config._
import org.yaml.snakeyaml.Yaml

import java.io.{InputStream, Reader}
import java.util
import java.util.Properties
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  *
  * Quickly to config your application
  *
  * Priority for configuration
  *     (1) Config from nacos if enabled
  *     (2) Command-line arguments
  *     (3) The external file specified with config: --$conf file:///xxx
  *     (4) Configuration file with profile ($profile/$conf.yml or $profile/$conf.properties) outside the JAR
  *     (5) Configuration file ($conf.yml or $conf.properties) outside the JAR
  *     (6) Configuration file with profile ($profile/$conf.yml or $profile/$conf.properties) in the classpath
  *     (7) Configuration file ($conf.yml or $conf.properties) in the classpath
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
    args: Array[String] = Array(),
    configs: Seq[String] = Seq(DEFAULT_CONFIG),
    noArgConfigs: Seq[String] = Seq()
) extends Logging {

  private val confMap: mutable.Map[String, Props] = mutable.Map()
  private val placeholderParser: PlaceholderParser = PlaceholderParser()
  private val yaml = new Yaml()

  init()

  def getConfig(config: String = DEFAULT_CONFIG): Props = confMap(config)

  private def init(): Unit = {
    val argsProps = fromArgs(args)
    configs.foreach(initForOne(_, argsProps, hasArgs = true))
    noArgConfigs.foreach(initForOne(_, argsProps, hasArgs = false))
  }

  private def initForOne(
      confPrefix: String,
      argsProps: Props,
      hasArgs: Boolean
  ): Unit = {
    val props = new Props
    tryFailAndLogWarnNecessary { props.putAll(fromClassPathAuto(confPrefix)) }
    tryFailAndLogWarnNecessary { props.putAll(fromFile(confPrefix)) }
    val profileConf = confPrefix + ACTIVE_PROFILE_SUFFIX
    val profileActive = argsProps.getString(
      profileConf,
      props.getString(profileConf, "")
    )
    val externalFile = argsProps.getString(
      confPrefix,
      props.getString(confPrefix, "")
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
    if (props.getBoolean(NACOS_CONFIG_ENABLED)) {
      fromNacos(props)
      log.info(
        s"$confPrefix conf is overwritten by nacos config"
      )
    }
    confMap(confPrefix) = placeholderParser.parse(props)
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

  private def fromNacos(props: Props): Unit = {
    val properties = new Properties()
    properties.putAll(props.toMap(NACOS_CONFIG))
    val configService = NacosFactory.createConfigService(properties)
    if (props.getBoolean(NACOS_CONFIG_LISTENER_ENABLED)) {
      configService.addListener(
        props.getString(NACOS_CONFIG_DATA_ID),
        props.getString(NACOS_CONFIG_GROUP),
        new AbstractListener {
          override def receiveConfigInfo(configInfo: String): Unit = {
            props.putAll(ymlMap2Props(loadMap(configInfo)))
          }
        }
      )
    }
    val content = configService.getConfig(
      props.getString(NACOS_CONFIG_DATA_ID),
      props.getString(NACOS_CONFIG_GROUP),
      props.getLong(NACOS_CONFIG_TIMEOUT)
    )
    props.putAll(ymlMap2Props(loadMap(content)))
  }

  private def fromClassPathAuto(filePrefix: String): Props = {
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

  private def fromClassPath(file: String): Props = {
    if (isYml(file)) {
      fromClassPathYml(file)
    } else {
      val ps = Props()
      val in = this.getClass.getResourceAsStream("/" + file)
      in match {
        case null => log.debug(s"classpath file not exists : $file")
        case _    => ps.load(in)
      }
      ps
    }
  }

  private def fromClassPathYml(file: String): Props = {
    val ymlFile = this.getClass.getResourceAsStream("/" + file)
    val ps = Props()
    ymlFile match {
      case null => log.debug(s"classpath file not exists : $file")
      case _ =>
        ps.putAll(ymlMap2Props(loadMap(ymlFile)))
    }
    ps
  }

  private def loadMap(ymlFile: InputStream): util.HashMap[String, Object] = {
    yaml.load[util.HashMap[String, Object]](ymlFile)
  }

  private def loadMap(content: String): util.HashMap[String, Object] = {
    yaml.load[util.HashMap[String, Object]](content)
  }

  private def loadMap(reader: Reader): util.HashMap[String, Object] = {
    yaml.load[util.HashMap[String, Object]](reader)
  }

  private def ymlMap2Props(
      ymlMap: java.util.Map[String, Object]
  ): Props = {
    val ps = Props()
    ymlMap.foreach(ele => {
      ele._2 match {
        case e if e == null => ps.put(ele._1, "")
        case e: java.util.Map[_, _] =>
          ps.putAll(
            ymlMap2Props(
              e.map(m => (ele._1 + "." + m._1, m._2.asInstanceOf[Object]))
            )
          )
        case e: java.util.List[_] => ps.put(ele._1, e.mkString(","))
        case _                    => ps.put(ele._1, ele._2.toString)
      }
    })
    ps
  }

  private def fromArgs(args: Array[String]): Props = {
    val ps = Props()
    val argsPair = args.map(_.trim).partition(_.startsWith("--"))
    argsPair._1
      .zip(argsPair._2)
      .foreach(pair => ps.put(pair._1.drop(2), pair._2))
    ps
  }

  private def fromFile(file: String): Props = {
    if (isYml(file)) {
      fromFileYml(file)
    } else {
      Props().load(Source.fromFile(file).reader())
    }
  }

  private def fromFileYml(file: String): Props = {
    val ps = Props()
    ps.putAll(ymlMap2Props(loadMap(Source.fromFile(file).reader())))
  }

  private def isYml(file: String): Boolean = {
    file match {
      case f if f.endsWith(".yml") || f.endsWith(".yaml") => true
      case _                                              => false
    }
  }

}

object Config {

  val DEFAULT_CONFIG: String = "app"
  val ACTIVE_PROFILE_SUFFIX = ".profile"
  val NACOS_CONFIG = "nacos"
  val NACOS_CONFIG_ENABLED: String = NACOS_CONFIG + ".enabled"
  val NACOS_CONFIG_DATA_ID: String = NACOS_CONFIG + ".data_id"
  val NACOS_CONFIG_GROUP: String = NACOS_CONFIG + ".group"
  val NACOS_CONFIG_TIMEOUT: String = NACOS_CONFIG + ".timeout"
  val NACOS_CONFIG_LISTENER_ENABLED: String = NACOS_CONFIG + ".listener.enabled"

  def apply(
      args: Array[String] = Array(),
      configs: Seq[String] = Seq(DEFAULT_CONFIG),
      noArgConfigs: Seq[String] = Seq()
  ): Config = new Config(args, configs, noArgConfigs)

}
