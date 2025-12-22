package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"os/exec"
	"path"
	"sort"
	"strings"

	codacy "github.com/codacy/codacy-engine-golang-seed/v6"
)

const toolName = "golangci-lint"

// Linter CLI Output structures
type LinterMetadata struct {
	Name             string `json:"name"`
	Description      string `json:"description"`
	EnabledByDefault bool   `json:"enabled_by_default"`
	Deprecated       bool   `json:"deprecated"`
}

type LintersOutput struct {
	Enabled  []LinterMetadata `json:"Enabled"`
	Disabled []LinterMetadata `json:"Disabled"`
}

var docFolder string

func main() {
	flag.StringVar(&docFolder, "docFolder", "docs", "Tool documentation folder")
	flag.Parse()
	if err := run(); err != nil {
		fmt.Printf("Error: %v\n", err)
		os.Exit(1)
	}
}

func run() error {
	// Get linters from the binary (filtering out deprecated)
	linters, version, err := getLintersFromCLI()
	if err != nil {
		return err
	}

	// Transform to Codacy models
	patterns := toCodacyPatterns(linters)
	descriptions := toCodacyPatternsDescription(linters)

	// Remove old documentation to ensure deprecated linters are deleted
	// Delete patterns.json
	patternsPath := path.Join(docFolder, "patterns.json")
	if err := os.Remove(patternsPath); err != nil && !os.IsNotExist(err) {
		return fmt.Errorf("failed to remove old patterns.json: %w", err)
	}

	// Delete and recreate description folder (removes description.json and all .md files)
	descPath := path.Join(docFolder, "description")
	if err := os.RemoveAll(descPath); err != nil {
		return fmt.Errorf("failed to clean description folder: %w", err)
	}
	if err := os.MkdirAll(descPath, 0755); err != nil {
		return err
	}

	// Write new files
	if err := createPatternsJSONFile(patterns, version); err != nil {
		return err
	}

	if err := createDescriptionFiles(descriptions); err != nil {
		return err
	}

	fmt.Printf("Successfully regenerated documentation for %d active linters.\n", len(linters))
	return nil
}

func getLintersFromCLI() ([]LinterMetadata, string, error) {
	fmt.Println("Fetching linters from golangci-lint...")

	// Get Version
	versionOut, _ := exec.Command("golangci-lint", "--version").Output()
	versionParts := strings.Fields(string(versionOut))
	version := "unknown"
	if len(versionParts) >= 4 {
		version = versionParts[3]
	}

	// Get Linters JSON
	cmd := exec.Command("golangci-lint", "linters", "--json")
	output, err := cmd.Output()
	if err != nil {
		return nil, "", fmt.Errorf("ensure golangci-lint is installed: %w", err)
	}

	var data LintersOutput
	if err := json.Unmarshal(output, &data); err != nil {
		return nil, "", err
	}

	// Combine and Filter
	var activeLinters []LinterMetadata
	allFromCLI := append(data.Enabled, data.Disabled...)

	for _, l := range allFromCLI {
		// Only include linters that are NOT deprecated
		if !l.Deprecated {
			activeLinters = append(activeLinters, l)
		}
	}

	sort.Slice(activeLinters, func(i, j int) bool {
		return activeLinters[i].Name < activeLinters[j].Name
	})

	return activeLinters, version, nil
}

func toCodacyPatterns(linters []LinterMetadata) []codacy.Pattern {
	var patterns []codacy.Pattern
	for _, l := range linters {
		patterns = append(patterns, codacy.Pattern{
			ID:       l.Name,
			Category: mapCategory(l.Name),
			Level:    "Info",
			ScanType: "SAST",
		})
	}
	return patterns
}

func toCodacyPatternsDescription(linters []LinterMetadata) []codacy.PatternDescription {
	var descriptions []codacy.PatternDescription
	for _, l := range linters {
		descriptions = append(descriptions, codacy.PatternDescription{
			PatternID:   l.Name,
			Description: l.Description,
			Title:       l.Name,
		})
	}
	return descriptions
}

func mapCategory(name string) string {
	name = strings.ToLower(name)
	switch {
	case strings.Contains(name, "sec") || strings.Contains(name, "gosec"):
		return "Security"
	case strings.Contains(name, "unused") || strings.Contains(name, "dead") || strings.Contains(name, "unparam"):
		return "UnusedCode"
	case strings.Contains(name, "style") || strings.Contains(name, "fmt") || strings.Contains(name, "lint") || strings.Contains(name, "whitespace"):
		return "CodeStyle"
	case strings.Contains(name, "perf") || strings.Contains(name, "prealloc"):
		return "Performance"
	case strings.Contains(name, "complexity") || strings.Contains(name, "cognitive"):
		return "Complexity"
	default:
		return "ErrorProne"
	}
}

func createPatternsJSONFile(patterns []codacy.Pattern, version string) error {
	tool := codacy.ToolDefinition{
		Name:     toolName,
		Version:  version,
		Patterns: &patterns,
	}
	data, _ := json.MarshalIndent(tool, "", "  ")
	return os.WriteFile(path.Join(docFolder, "patterns.json"), data, 0644)
}

func createDescriptionFiles(descriptions []codacy.PatternDescription) error {
	for _, d := range descriptions {
		content := fmt.Sprintf("## %s\n\n%s\n", d.PatternID, d.Description)
		err := os.WriteFile(path.Join(docFolder, "description", d.PatternID+".md"), []byte(content), 0644)
		if err != nil {
			return err
		}
	}

	data, _ := json.MarshalIndent(descriptions, "", "  ")
	return os.WriteFile(path.Join(docFolder, "description", "description.json"), data, 0644)
}
