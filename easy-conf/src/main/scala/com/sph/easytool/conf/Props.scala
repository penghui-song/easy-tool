package com.sph.easytool.conf

import org.json4s.jackson.JsonMethods
import org.json4s.{DefaultFormats, Formats}
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter

import java.util.Properties
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Container to store your configuration
  */
class Props extends mutable.HashMap[String, String] {

  /**
    * properties convert to props
    * @param properties properties
    */
  def putAll(properties: Properties): Unit = {
    properties.foreach(kv => {
      this.put(kv._1, kv._2)
    })
  }

  /**
    * get prop by key, if null, default is ""
    * @param key key
    * @return value
    */
  def getProp(key: String): String = {
    this.getOrElse(key, "")
  }

  /**
    * bind props to case class
    * @param formats json4s formats, such as #EnumNameSerializer
    * @param mf Manifest
    * @tparam T case class type
    * @return case class object
    */
  def bind[T <: Product](
      formats: Formats = DefaultFormats
  )(implicit mf: scala.reflect.Manifest[T]): T = {
    val converter = new PropertiesToJsonConverter()
    JsonMethods
      .parse(converter.convertToJson(this.asJava))
      .camelizeKeys
      .extract[T](formats, mf)
  }

}
