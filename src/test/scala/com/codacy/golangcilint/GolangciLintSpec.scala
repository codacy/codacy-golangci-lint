package com.codacy.golangcilint

import com.codacy.analysis.core.model.IssuesAnalysis
import com.codacy.analysis.core.model.IssuesAnalysis.{Failure, Success}
import org.scalatest.PrivateMethodTester
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GolangCILintSpec extends AnyWordSpec with Matchers with PrivateMethodTester {
  val golangcilintReportToIssuesAnalysis = PrivateMethod[IssuesAnalysis](Symbol("golangcilintReportToIssuesAnalysis"))

  "GolangCILint" should {
    "parse correctly" in {
      val result = GolangCILint.convert(Seq(CommonTestMock.resultJsonText), CommonTestMock.currentDir)

      val expectedResult = Success(Set(CommonTestMock.fileResults))

      result mustEqual expectedResult
    }

    "fail when invalid format given" in {
      val text = Seq("""<test><xmlformat></xmlformat></test>""")
      val result = GolangCILint.convert(text, CommonTestMock.fileNamePath)

      result mustBe a[Failure]
    }

    "fail the parse when json with missing information is given" in {
      val lines = Seq(s"""{
                         |    "Issues": [
                         |        {
                         |            "severity": "LOW"
                         |        }
                         |    ]
                         |}""".stripMargin)

      val result = GolangCILint.convert(lines, CommonTestMock.currentDir)

      result mustBe a[Failure]
    }

    "convert golangcilint report into codacy file results report" in {
      val fileResults = GolangCILint invokePrivate golangcilintReportToIssuesAnalysis(CommonTestMock.resultAsGolangCILintResult)

      fileResults mustEqual Success(Set(CommonTestMock.fileResults))
    }

    "return success" in {
      val issuesAnalysis = GolangCILint invokePrivate golangcilintReportToIssuesAnalysis(CommonTestMock.resultAsGolangCILintResult)
      issuesAnalysis mustBe a[Success]
    }
  }

}
