package com.codacy.tool

abstract class ClientSideToolEngine(protected val toolName: String) extends IssuesAnalysisConverter {

  def main(args: Array[String]): Unit = {
    val exitStatus = ParserConfig.withConfig(toolName, args) { parserConfig =>
      run(this.toolName, parserConfig)
    }
    sys.exit(exitStatus)
  }
}
