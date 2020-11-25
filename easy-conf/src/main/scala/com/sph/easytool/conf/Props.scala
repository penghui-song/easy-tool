package com.sph.easytool.conf

import java.util.Properties

import scala.collection.mutable

/**
  * Container to store your configuration
  */
class Props extends mutable.HashMap[String, String] {
  def putAll(properties: Properties): Unit = {
    import scala.collection.JavaConversions._
    properties.foreach(kv => {
      this.put(kv._1, kv._2)
    })
  }

  def getProp(key: String): String = {
    this.getOrElse(key, "")
  }
}
