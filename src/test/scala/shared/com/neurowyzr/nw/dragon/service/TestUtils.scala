package com.neurowyzr.nw.dragon.service

import scala.io.Source

object TestUtils {

  def loadResourceFile(pathname: String): String = {
    val resource                = Source.fromResource(pathname)
    val lines: Iterator[String] = resource.getLines()
    lines.mkString(System.lineSeparator)
  }

}
