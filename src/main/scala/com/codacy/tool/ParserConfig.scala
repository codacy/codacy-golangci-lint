package com.codacy.tool

import java.nio.charset.Charset
import java.nio.file.{Path, Paths}

import scopt.OptionParser

case class ParserConfig(
    encoding: Charset = Charset.forName("UTF-8"),
    relativizeTo: Path = Paths.get(System.getProperty("user.dir"))
)

object ParserConfig {

  def parse(toolName: String): OptionParser[ParserConfig] = new OptionParser[ParserConfig](toolName) {
    opt[String]('e', "encoding")
      .action { (encoding, config) =>
        config.copy(encoding = Charset.forName(encoding))
      }
      .text(s"input encoding (default: ${ParserConfig().encoding.name})")

    opt[String]('p', "relativize-path")
      .action { (path, config) =>
        config.copy(relativizeTo = Paths.get(path))
      }
      .text(s"path to relativize result files (default: ${Paths.get(System.getProperty("user.dir"))})")
  }

  def withConfig(toolName: String, args: Array[String])(f: ParserConfig => Unit): Int = {
    val parser = this.parse(toolName)

    parser.parse(args, ParserConfig()) match {
      case Some(config) =>
        f(config)
        0
      case None =>
        1
    }
  }
}
