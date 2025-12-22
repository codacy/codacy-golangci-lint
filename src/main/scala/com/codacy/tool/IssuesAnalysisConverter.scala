package com.codacy.tool

import java.nio.file.Path

import com.codacy.analysis.core.model.{IssuesAnalysis, ToolResults}
import com.codacy.analysis.core.serializer.IssuesReportSerializer

trait IssuesAnalysisConverter {
  protected def convert(lines: Seq[String], relativizeTo: Path): IssuesAnalysis

  def run(toolName: String, config: ParserConfig): Unit = {
    val stdin = scala.io.Source.fromInputStream(System.in)(config.encoding)
    val lines = stdin.getLines().to(LazyList)
    val issuesResults = convert(lines, relativizeTo = config.relativizeTo)
    val toolResults = ToolResults(toolName, issuesResults)
    val jsonResult = IssuesReportSerializer.toJsonString(Set(toolResults))
    println(jsonResult)
  }
}
