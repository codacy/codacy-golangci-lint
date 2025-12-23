package com.codacy.golangcilint

import java.nio.file.{Path, Paths}

import com.codacy.analysis.core.model.IssuesAnalysis
import com.codacy.analysis.core.model.IssuesAnalysis.FileResults
import com.codacy.tool.ClientSideToolEngine

object GolangCILint extends ClientSideToolEngine(toolName = "golangci-lint") {

  private def golangcilintReportToIssuesAnalysis(report: GolangCILintResult): IssuesAnalysis = {
    val reportFileResults = report.issues
      .groupBy(_.file)
      .view
      .map {
        case (path, res) =>
          FileResults(Paths.get(path), res.view.map(_.toCodacyIssue(toolName)).toSet)
      }
      .to(Set)

    IssuesAnalysis.Success(reportFileResults)
  }

  override def convert(lines: Seq[String], relativizeTo: Path): IssuesAnalysis = {
    GolangCILintReportParser.fromJson(lines, relativizeTo) match {
      case Right(report) => golangcilintReportToIssuesAnalysis(report)
      case Left(err) => IssuesAnalysis.Failure(err.getMessage)
    }
  }
}
