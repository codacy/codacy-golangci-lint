package com.codacy.golangcilint

import com.codacy.plugins.api.results
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GolangCILintResultSpec extends AnyWordSpec with Matchers {
  "GolangCILintIssue" should {
    "be converted into Issue correctly" in {
      val golangcilintIssue = GolangCILintIssue("errcheck", "error not checked", "test.go", 1, 2)

      val issue = golangcilintIssue.toCodacyIssue("golangci-lint")

      issue.patternId.value mustEqual "golangci-lint_errcheck"
      issue.message.text mustEqual "error not checked"
      issue.level mustEqual results.Result.Level.Info
    }
  }
}
