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

    sbt "show GraalVMNativeImage/packageBin"

#### Build fat-jar

    sbt assembly

#### Generate documentation

```bash
cd doc-generation
go run main.go -docFolder=../docs
```

## How to update the tool

#### Change Dockerfile

Change the GolangCI-Lint version at the end of the line to the most recent one: 
`RUN wget -O- -nv https://raw.githubusercontent.com/golangci/golangci-lint/master/install.sh | sh -s -- -b /usr/local/bin v2.7.2`

#### Generate documentation

```bash
cd doc-generation
go run main.go -docFolder=../docs
```

## Agent Playbook: Updating This Repository End-to-End

This section is written for an AI coding agent (or a human) tasked with updating this repo — most commonly bumping the wrapped `golangci-lint` version, but also Go/Docker base image or CircleCI orb bumps. Follow it top to bottom; it tells you what to change, how to regenerate derived files, how to test locally, and how to interpret CI so you can iterate on failures without guessing.

### 1. What this repository is

This is **not a full Codacy analysis engine** — it is a **client-side result converter**. The Scala code (`src/main/scala/com/codacy/golangcilint/GolangCILint.scala`, built on `ClientSideToolEngine`) reads `golangci-lint`'s JSON output from stdin and converts it into Codacy's issue format on stdout; it does not invoke `golangci-lint` itself and the packaged Docker image cannot run analysis (`entry.sh` just prints an error and exits 1 — the customer is expected to run `golangci-lint` themselves and pipe the output through the `codacy-golangci-lint` binary/jar, as documented above in "Sending the results to Codacy").

The `docs/` directory, however, mirrors the pattern-engine convention used by other Codacy tool wrappers and **is machine-generated, not hand-written**:

- `docs/patterns.json` — the full list of `golangci-lint` sub-linters ("patterns"), their category/level, and whether enabled by default. Generated file, do not hand-edit.
- `docs/description/description.json` + `docs/description/*.md` — one Markdown file and one JSON entry per linter, with its title/description. Generated file, do not hand-edit.
- `docs/tool-description.md` — short hand-maintained blurb about the tool.

Both generated artifacts come from **`doc-generation/main.go`** (module `github.com/codacy/gosec-doc-generator`, despite the name — it's actually the golangci-lint doc generator), which shells out to the **actual `golangci-lint` binary** (`golangci-lint linters --json` and `golangci-lint --version`) and reshapes its output into Codacy's schema. This means the generator needs the target `golangci-lint` version **installed and on `PATH`** locally — it does not clone or scrape any upstream repo.

### 2. Files that encode versions — check all of these on every update

| File | What it controls | What to check |
|---|---|---|
| `Dockerfile` → `RUN wget ... install.sh \| sh -s -- -b /usr/local/bin v2.7.2` | Which `golangci-lint` release is installed in the builder stage and used to generate `docs/` | Bump the `v2.7.2` tag to the target release; confirm it exists at https://github.com/golangci/golangci-lint/releases. |
| `Dockerfile` → `FROM golang:1.25-alpine3.23 AS builder` / `FROM alpine:3.23` | Go toolchain used to build `doc-generation`, and the final runtime base image | Only bump if the new `golangci-lint` version requires a newer Go, or to pick up an Alpine security patch. |
| `doc-generation/go.mod` → `go 1.23.0` / `toolchain go1.25.5` and `github.com/codacy/codacy-engine-golang-seed/v6` | Go language version and shared Codacy Go seed library used by the doc generator itself | Bump the seed dependency independently if a newer version is published; this is unrelated to the `golangci-lint` version. |
| `build.sbt` → `scalaVersionNumber`, `circeVersion`, `graalVersion`, `com.codacy" %% "codacy-analysis-cli-model"` | Scala/Circe/GraalVM toolchain and the Codacy analysis model library used by the converter itself | Only bump as part of a dependency-maintenance task, not a `golangci-lint` version bump. |
| `.circleci/config.yml` → `codacy/base@13.0.0` orb | Shared CircleCI steps (checkout, sbt, docker publish, etc.) | Check the latest published version of the `codacy/base` orb if asked to bump CI tooling. |

Note there is **no `go.mod` at the repo root** pinning `golangci-lint` as a Go dependency — it is installed as a prebuilt binary via the official install script, so the version string only lives in the `Dockerfile`.

### 3. Step-by-step update procedure

1. **Bump the `golangci-lint` version** in the `Dockerfile` (the `v2.7.2` tag), and the Go/Alpine base images or the `codacy/base` orb version only if scoped by the task.
2. **Regenerate the docs.** Install the target `golangci-lint` version locally (e.g. `wget -O- -nv https://raw.githubusercontent.com/golangci/golangci-lint/master/install.sh | sh -s -- -b /usr/local/bin v<version>` so it's on `PATH`), then run:
   ```bash
   cd doc-generation
   go run main.go -docFolder=../docs
   ```
   This deletes and rewrites `docs/patterns.json` and the whole `docs/description/` folder (except `tool-description.md`, which lives outside that regeneration and is copied separately in the Dockerfile). Review the diff for new/removed/renamed linters.
3. **Compile and format the Scala converter**: `sbt compile`, then `sbt "scalafmtAll; scalafmtSbt"`.
4. **Run the native Scala test suite**: `sbt test` (covers `GolangCILintReportParser`, `GolangCILintResult`, and `GolangCILint` conversion logic in `src/test/scala/com/codacy/golangcilint/`). This repo has **no `codacy-plugins-test` integration** — the Docker image is not a runnable analysis engine, so there is nothing for `codacy-plugins-test` to drive.
5. **Build the Docker image** to confirm the `doc-generation` build stage still succeeds against the new `golangci-lint` version: `docker build -t codacy-golangci-lint .`
6. **Iterate on failures**, re-running only the relevant command (`go run main.go ...`, `sbt test`, or `docker build`) after each fix.
7. **Commit** the version bump(s) together with the regenerated `docs/patterns.json` and `docs/description/*` files in one change.
8. **Push and open a PR.**
9. **Poll the PR's real CI checks until they all pass — local validation is NOT the finish line.** After every push, run `gh pr checks <pr-url>` and keep re-polling (short sleep while any check is `pending`) until all checks finish. If a check fails, fetch its actual log (don't guess), find the true root cause, fix it, push again (never `--no-verify`, never force-push), and re-poll. Repeat until every check is green. **The CI environment's toolchain can differ from your local one**, so a clean local run does not guarantee CI passes. Only stop iterating when every check passes, or you hit a genuine product/infra decision that needs a human.

### 4. Common failure modes and fixes

| Symptom | Likely cause | Fix |
|---|---|---|
| `go run main.go` fails with "ensure golangci-lint is installed" | The doc generator shells out to a `golangci-lint` binary on `PATH` that isn't installed, or is the wrong version | Reinstall the exact target version via the official install script before regenerating docs. |
| `docs/patterns.json` / `docs/description/*` diff shows unrelated linters appearing or disappearing | You regenerated docs against a different installed `golangci-lint` version than the one just pinned in the `Dockerfile` | Make sure the locally installed CLI version matches the `Dockerfile` tag exactly before regenerating. |

### 5. Definition of done

- `golangci-lint` version bumped in the `Dockerfile` (and Go/Alpine base images or CircleCI orb, only if in scope).
- `docs/patterns.json` and `docs/description/*` regenerated via `doc-generation/main.go` against the exact new version, with the diff reviewed.
- `sbt compile`, `sbt "scalafmtAll; scalafmtSbt"`, and `sbt test` pass locally.
- `docker build -t codacy-golangci-lint .` succeeds.
- **After pushing and opening/updating the PR, every CI check on it is green.** Poll `gh pr checks <pr-url>` and iterate on any failure until all pass.

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
