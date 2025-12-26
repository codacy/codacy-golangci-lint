package com.codacy.golangcilint

import java.nio.file.Paths
import com.codacy.analysis.core.model.{FullLocation, Issue}
import com.codacy.plugins.api.results

case class GolangCILintResult(issues: Seq[GolangCILintIssue])

case class GolangCILintIssue(ruleId: String, details: String, file: String, line: Int, column: Int) {

  def toCodacyIssue(toolName: String): Issue = {
    Issue(
      results.Pattern.Id(ruleId),
      Paths.get(file),
      Issue.Message(details),
      results.Result.Level.Info,
      None,
      FullLocation(line, column)
    )
  }
}
