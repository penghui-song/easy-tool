package com.sph.easytool.conf

import org.apache.commons.lang3.StringUtils
import org.json4s.jackson.JsonMethods
import org.json4s.{DefaultFormats, Formats}
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter

import java.util.Properties
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.tools.nsc.interpreter.InputStream

/**
  * Container to store your configuration
  */
class Props extends mutable.HashMap[String, String] {

  /**
    * properties add to this
    * @param properties properties
    */
  def putAll(properties: Properties): Props = {
    properties.foreach(kv => {
      this.put(kv._1, kv._2)
    })
    this
  }

  /**
    * props add to this
    * @param props properties
    */
  def putAll(props: Props): Props = {
    props.foreach(kv => {
      this.put(kv._1, kv._2)
    })
    this
  }

  /**
    * get string value by key, if null, default is ""
    */
  def getString(key: String, defaultValue: String = ""): String = {
    this.getOrElse(key, defaultValue)
  }

  /**
    * get int value by key, if null, default is 0
    */
  def getInt(key: String, defaultValue: Int = 0): Int = {
    this.get(key) match {
      case Some(x) => x.toInt
      case None    => defaultValue
    }
  }

  /**
    * get long value by key, if null, default is 0
    */
  def getLong(key: String, defaultValue: Long = 0): Long = {
    this.get(key) match {
      case Some(x) => x.toLong
      case None    => defaultValue
    }
  }

  /**
    * get list values by key, separate with separator
    *
    * @param key key
    * @param separator separator
    * @param convert string convert to T
    * @tparam T the type in list
    * @return values
    */
  def getList[T](key: String, separator: String = ",")(
      convert: String => T
  ): List[T] = {
    this.getOrElse(key, "").split(separator).toList.map(convert)
  }

  /**
    * get boolean value by key, if null, default is false
    */
  def getBoolean(key: String, defaultValue: Boolean = false): Boolean = {
    this.get(key) match {
      case Some(x) => x.toBoolean
      case None    => defaultValue
    }
  }

  /**
    * get double value by key, if null, default is 0
    */
  def getDouble(key: String, defaultValue: Double = 0): Double = {
    this.get(key) match {
      case Some(x) => x.toDouble
      case None    => defaultValue
    }
  }

  /**
    * bind props to case class
    * @param prefix prefix from bind case class
    * @param formats json4s formats, such as #EnumNameSerializer
    * @param mf Manifest
    * @tparam T case class type
    * @return case class object
    */
  def bind[T <: Product](
      prefix: String = "",
      formats: Formats = DefaultFormats
  )(implicit
      mf: scala.reflect.Manifest[T]
  ): T = {
    val converter = new PropertiesToJsonConverter()
    var jValue = JsonMethods
      .parse(converter.convertToJson(this.asJava))
      .camelizeKeys
    if (StringUtils.isNoneBlank(prefix))
      jValue = jValue.\\(prefix)
    jValue
      .extract[T](formats, mf)
  }

  /**
    * 获取key的所有子集并转为map
    */
  def toMap(key: String): Map[String, String] = {
    this
      .filter(!_._1.equals(key))
      .filter(_._1.startsWith(key))
      .map(p => (p._1.diff(key + "."), p._2))
      .toMap
  }

  /**
    * load props from input stream
    */
  def load(inputStream: InputStream): Props = {
    val ps = new Properties()
    ps.load(inputStream)
    this.putAll(ps)
  }

  /**
    * load props from reader
    */
  def load(reader: java.io.Reader): Props = {
    val ps = new Properties()
    ps.load(reader)
    this.putAll(ps)
  }

}

object Props {
  def apply(): Props = new Props()
}
