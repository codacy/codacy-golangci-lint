package com.codacy.golangcilint

import java.nio.file.{Path, Paths}
import com.codacy.analysis.core.model.IssuesAnalysis.FileResults
import com.codacy.analysis.core.model.{FullLocation, Issue}
import com.codacy.plugins.api.results

object CommonTestMock {
  val currentDir = Paths.get(System.getProperty("user.dir"))
  val filename = "main.go"
  val fileNamePath: Path = Paths.get(filename)

  // golangci-lint specific fields
  val patternId = "errcheck" // The actual linter that found the issue
  val resultPatternId = s"golangci-lint_$patternId"
  
  val issueText = "Error return value of `ioutil.WriteFile` is not checked"
  val line = 10
  val column = 4
  val severity = "info"

  val resultAsGolangCILintResult: GolangCILintResult = GolangCILintResult(
    Seq(
      GolangCILintIssue(
        severity,
        resultPatternId,
        issueText,
        filename,
        line,
        column
      )
    )
  )

  val fileResults: FileResults = FileResults(
    fileNamePath,
    Set(
      Issue(
        results.Pattern.Id(resultPatternId),
        fileNamePath,
        Issue.Message(issueText),
        results.Result.Level.Info,
        Some(results.Pattern.Category.ErrorProne),
        FullLocation(line, column)
      )
    )
  )

  val resultJsonText: String = generateResultJsonText()

  def generateResultJsonText(
      linter: String = patternId,
      lineNum: Int = line,
      text: String = issueText,
      severity: String = severity
  ): String =
    s"""{
       |  "Issues": [
       |    {
       |      "FromLinter": "$resultPatternId",
       |      "Text": "$text",
       |      "Severity": "$severity",
       |      "SourceLines": [
       |        "ioutil.WriteFile(path.Join(docsFolder,docsDescriptionFolder,example.ID+\\".md\\\",),[]byte(exampleMD),0644,)"
       |      ],
       |      "Replacement": null,
       |      "Pos": {
       |        "Filename": "$filename",
       |        "Offset": 0,
       |        "Line": $lineNum,
       |        "Column": $column
       |      }
       |    }
       |  ],
       |  "Report": {
       |    "Linters": [
       |      {
       |        "Name": "$linter",
       |        "Enabled": true,
       |        "EnabledByDefault": true
       |      }
       |    ]
       |  }
       |}""".stripMargin
}