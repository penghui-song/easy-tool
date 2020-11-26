package com.sph.easytool.conf

import com.sph.easytool.conf.PlaceholderParser.{
  DEFAULT_PREFIX,
  DEFAULT_SEPARATOR,
  DEFAULT_SUFFIX
}

import scala.collection.mutable.ArrayBuffer

/**
  *
  * A string placeholder parser to parse value with the props.
  *
  * such as ${t1} will replace by props.get("t1")
  *
  * @param prefix     placeholder prefix, default "${"
  * @param suffix     placeholder suffix, default "}"
  * @param separator  placeholder separator, default ":"
  */
class PlaceholderParser(
    prefix: String = DEFAULT_PREFIX,
    suffix: String = DEFAULT_SUFFIX,
    separator: String = DEFAULT_SEPARATOR
) {

  def parse(props: Props): Props = {
    val propsT = new Props()

    props.foreach(p => {
      propsT.put(p._1, parse(p._2, props))
    })
    propsT
  }

  def parse(value: String, props: Props): String = {
    def process(arg: String): String = {
      var array = arg.split(separator)
      if (array.length == 1) {
        array :+= ""
      }
      props.getOrElse(array(0).trim, array(1).trim)
    }

    val (parts, args) = parse(value)
    val pi = parts.iterator
    val ai = args.iterator
    val builder = new StringBuilder(pi.next())
    while (ai.hasNext) {
      builder append process(ai.next)
      builder append pi.next()
    }
    builder.toString
  }

  private def parse(value: String): (Array[String], Array[String]) = {
    val parts: ArrayBuffer[String] = ArrayBuffer()
    val args: ArrayBuffer[String] = ArrayBuffer()
    var tmpValue = value
    var start = tmpValue.indexOf(prefix)
    var end = tmpValue.indexOf(suffix)
    while (start != -1 && end != -1) {
      val partExt = tmpValue.splitAt(start)
      parts += partExt._1
      val argExt = partExt._2.splitAt(end - partExt._1.length)
      args += argExt._1
        .drop(prefix.length)
      tmpValue = argExt._2.drop(suffix.length)
      start = tmpValue.indexOf(prefix)
      end = tmpValue.indexOf(suffix)
    }
    parts += tmpValue
    (parts.toArray, args.toArray)
  }
}

object PlaceholderParser {
  private val DEFAULT_PREFIX = "${"
  private val DEFAULT_SUFFIX = "}"
  private val DEFAULT_SEPARATOR = ":"

  def apply(
      prefix: String,
      suffix: String,
      separator: String
  ): PlaceholderParser = new PlaceholderParser(prefix, suffix, separator)

  def apply(): PlaceholderParser = new PlaceholderParser()
}
