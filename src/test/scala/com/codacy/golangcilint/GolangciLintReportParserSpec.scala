package com.codacy.golangcilint

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GolangCILintReportParserSpec extends AnyWordSpec with Matchers {

  def assertSuccess(result: Either[io.circe.Error, GolangCILintResult], expectedResult: GolangCILintResult) = {
    result.isRight mustBe true
    result.foreach(x => x mustEqual expectedResult)
  }

  def assertFailure(result: Either[io.circe.Error, GolangCILintResult]) = {
    result.isLeft mustBe true
  }

  "GolangCILint Reporter parser" should {
    "parse the json correctly" in {
      val resultLines = Seq(CommonTestMock.resultJsonText)
      val result = GolangCILintReportParser.fromJson(resultLines,CommonTestMock.currentDir)
      assertSuccess(result, CommonTestMock.resultAsGolangCILintResult)
    }

    "fail parsing on invalid json" in {
      val result = GolangCILintReportParser.fromJson(Seq("""{"Issues": [ { "Pos": { "Line": "not-a-number" } } ] }"""), CommonTestMock.currentDir)
      assertFailure(result)
    }

    "fail parsing on empty input" in {
      val result = GolangCILintReportParser.fromJson(Seq.empty, CommonTestMock.currentDir)
      assertFailure(result)
    }

    "parse the line correctly" in {
      GolangCILintReportParser.parseLine("10") mustBe Some(10)
    }

    "handle range strings if they appear in Text" in {
      GolangCILintReportParser.parseLine("70-78") mustBe Some(70)
    }
  }
}