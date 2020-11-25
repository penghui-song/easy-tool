package com.sph.easytool.conf

import org.slf4j.{Logger, LoggerFactory}

/**
  * Log wrapper
  */
trait Logging {
  protected val log: Logger = LoggerFactory.getLogger(this.getClass.getName)

  def error(msg: String, throwable: Throwable = null): Unit = {
    log.error(msg, throwable)
  }

  def warn(msg: String, throwable: Throwable = null): Unit = {
    log.warn(msg, throwable)
  }

  def info(msg: String, throwable: Throwable = null): Unit = {
    log.debug(msg, throwable)
  }

  def debug(msg: String, throwable: Throwable = null): Unit = {
    log.debug(msg, throwable)
  }

  def trace(msg: String, throwable: Throwable = null): Unit = {
    log.trace(msg, throwable)
  }
}
