package com.codacy.golangcilint

import java.nio.file.Paths
import com.codacy.analysis.core.model.{FullLocation, Issue}
import com.codacy.plugins.api.results

case class GolangCILintResult(issues: Seq[GolangCILintIssue])

case class GolangCILintIssue(
    severity: String,
    ruleId: String,
    details: String,
    file: String,
    line: Int,
    column: Int
) {

  def toCodacyIssue(toolName: String): Issue = {
    Issue(
      results.Pattern.Id(ruleId),
      Paths.get(file),
      Issue.Message(details),
      convertLevel(severity),
      convertCategory(ruleId),
      FullLocation(line, column)
    )
  }

  private def convertLevel(level: String): results.Result.Level.Value = level.toLowerCase match {
    case "error" | "high" => results.Result.Level.Err
    case "warning" | "medium" => results.Result.Level.Warn
    case "info" | "low" => results.Result.Level.Info
    case _ => results.Result.Level.Info
  }

  private def convertCategory(ruleId: String): Option[results.Pattern.Category] = {
    if (ruleId.contains("gosec")) Some(results.Pattern.Category.Security)
    else if (ruleId.contains("unused") || ruleId.contains("deadcode")) Some(results.Pattern.Category.UnusedCode)
    else Some(results.Pattern.Category.ErrorProne)
  }
}
