package com.codacy.golangcilint

import java.nio.file.{Path, Paths}
import io.circe.parser.decode
import io.circe.{Decoder, DecodingFailure, HCursor}
import scala.util.Try

object GolangCILintReportParser {

  def parseLine(line: String): Option[Int] = {
    line.split("-").headOption.flatMap(s => Try(s.toInt).toOption)
  }

  implicit val golangcilintIssueDecoder: Decoder[GolangCILintIssue] = (c: HCursor) => {
    val pos = c.downField("Pos")

    for {
      ruleId <- c.downField("FromLinter").as[String]
      details <- c.downField("Text").as[String]
      fileStr <- pos.downField("Filename").as[String]

      line <- pos.downField("Line").as[Int].orElse {
        pos.downField("Line").as[String].flatMap { s =>
          parseLine(s).toRight(DecodingFailure(s"Invalid line format: $s", pos.history))
        }
      }

      column <- pos.downField("Column").as[Int]
    } yield GolangCILintIssue(ruleId = ruleId, details = details, file = fileStr, line = line, column = column)
  }

  implicit val golangcilintResultDecoder: Decoder[GolangCILintResult] = (c: HCursor) =>
    for {
      issues <- c.downField("Issues").as[Seq[GolangCILintIssue]]
    } yield GolangCILintResult(issues)

  def fromJson(lines: Seq[String], relativizeTo: Path): Either[io.circe.Error, GolangCILintResult] = {
    val content = lines.mkString.trim

    // Find the boundaries of the JSON object
    val firstBrace = content.indexOf('{')
    val lastBrace = content.lastIndexOf('}')

    if (firstBrace == -1 || lastBrace == -1) {
      // If we can't find braces, it's definitely not valid JSON
      Left(io.circe.ParsingFailure("No JSON object found in input", new Exception()))
    } else {
      // Slice the string to ignore anything before the first { or after the last }
      val cleanedJson = content.substring(firstBrace, lastBrace + 1)

      decode[GolangCILintResult](cleanedJson).map { report =>
        val relativizedIssues = report.issues.map { issue =>
          val relativePathStr = relativizeTo.relativize(Paths.get(issue.file).toAbsolutePath).toString
          issue.copy(file = relativePathStr)
        }
        GolangCILintResult(relativizedIssues)
      }
    }
  }
}
