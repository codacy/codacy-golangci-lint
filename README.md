# Codacy GolangCI-Lint

A standalone tool that converts GolangCI-Lint results to Codacy's format. It allows the integration of GolangCI-Lint into your Codacy workflow.

## How it works

1.  The tool receives as input the GolangCI-Lint result from stdin. The GolangCI-Lint result must be in JSON format.

2.  Converts GolangCI-Lint result into Codacy's format

3.  Prints Codacy's format to stdout

> NOTE: the tool must be run in the project root folder.

## Usage

### Requirements

To get your GolangCI-Lint results into Codacy you'll need to:

-   [Enable GolangCI-Lint](https://docs.codacy.com/repositories-configure/configuring-code-patterns/) and configure the corresponding code patterns on your repository **Code patterns** page
-   Enable the setting **Run analysis through build server** on your repository **Settings**, tab **General**, **Repository analysis**
-   Obtain a [project API token](https://docs.codacy.com/codacy-api/api-tokens/#project-api-tokens)
-   Install [GolangCI-Lint](https://golangci-lint.run/docs/welcome/install/local/)
-   Download the `codacy-golangci-lint` binary (or Java jar) from [the releases page](https://github.com/codacy/codacy-golangci-lint/releases)


### Sending the results to Codacy

Sending the results of running GolangCI-Lint to Codacy involves the steps below, which you can automate in your CI build process:

1.  Run GolangCI-Lint
2.  Convert the GolangCI-Lint output to a format that the Codacy API accepts using the [codacy-GolangCI-Lint](https://github.com/codacy/codacy-golangci-lint/releases) binary
3.  Send the results to Codacy
4.  Finally, signal that Codacy can use the sent results and start a new analysis

> When the option **“Run analysis through build server”** is enabled, the Codacy analysis will not start until you call the endpoint `/2.0/commit/{commitUuid}/resultsFinal` signalling that Codacy can use the sent results and start a new analysis.

```bash
export PROJECT_TOKEN="YOUR-TOKEN"
export COMMIT="COMMIT-UUID"

golangci-lint run --output.json.path stdout 2>/dev/null | \
./codacy-golangci-lint-"<version>" | \
curl -XPOST -L -H "project-token: $PROJECT_TOKEN" \
    -H "Content-type: application/json" -d @- \
    "https://api.codacy.com/2.0/commit/$COMMIT/issuesRemoteResults"

curl -XPOST -L -H "project-token: $PROJECT_TOKEN" \
	-H "Content-type: application/json" \
	"https://api.codacy.com/2.0/commit/$COMMIT/resultsFinal"
```

## Building

#### Compile

    sbt compile

#### Format

    sbt "scalafmtAll; scalafmtSbt"

#### Tests

    sbt test

##### Build native image (requires docker)

`sbt "show GraalVMNativeImage/packageBin"`

#### Build fat-jar

    sbt assembly

#### Generate documentation

```bash
cd doc-generation
go run main.go -docFolder=../docs
```

## How to update the tool

#### Change .version file

If the version is, for example, 0.0.1, update to the next minor version. This is important to create a new version of the artifact.

#### Change Dockerfile

Change the GolangCI-Lint version at the end of the line to the most recent one: 
`RUN wget -O- -nv https://raw.githubusercontent.com/golangci/golangci-lint/master/install.sh | sh -s -- -b /usr/local/bin v2.7.2`

#### Generate documentation

```bash
cd doc-generation
go run main.go -docFolder=../docs
```

## What is Codacy?

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features:

-   Identify new Static Analysis issues
-   Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
-   Auto-comments on Commits and Pull Requests
-   Integrations with Slack, HipChat, Jira, YouTrack
-   Track issues Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.
