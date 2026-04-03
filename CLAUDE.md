# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
mvn clean install                          # Build fat JAR
java -jar target/claipo-0.1.0.jar         # Run the application
mvn exec:java                              # Run via Maven (NetBeans-style)
```

The project builds a single fat JAR using maven-shade-plugin. Main class: `vroddon.claipo.Main`.

To test individual AI backends or agents, run their `main()` methods directly:
- `Claude.main()` — test Claude API connectivity
- `AgenteAgenda.main()` — test calendar scheduling agent
- `MCPClient.main()` — test MCP server integration

## Architecture

**Claipo** is an always-on-top Swing desktop app that reads email/calendar context and generates AI-assisted replies, writing the result to the clipboard.

### Core flow

1. `Main` → launches `ClaipoUI` on the Swing EDT
2. `ClaipoUI` reads the user hint, detects intent (`@agenda` → `AgenteAgenda`, else → `AgenteCorreo`), and spawns a `SwingWorker`
3. The agent builds a prompt from context data (email thread or calendar), calls the AI backend, and writes output to the clipboard
4. A tooltip notification confirms the result is ready

### Agent layer (`vroddon.claipo.agenda`)

- **`AgenteCorreo`** — reads `email.json` from the working directory, constructs a reply prompt, delegates to Claude
- **`AgenteAgenda`** — fetches an ICS calendar from a CalDAV URL, parses it with `AgendaParser`, asks Claude for available time slots

### AI backends (`vroddon.claipo.ia`)

| Class | Backend | Auth |
|---|---|---|
| `Claude` | Anthropic SDK | `ANTHROPIC_API_KEY` env var |
| `GeminiSimpleChat` | Google Vertex AI | Service account JSON at `D:\svn\uh\google.json` |
| `GemmaSimpleChat` / `Gemma` | Local Ollama | No auth (localhost:11434) |

Claude is the primary backend. Gemini is a fallback. Vertex AI project is hardcoded as `smallwebs-1508966024969` / `us-central1`.

### MCP (`vroddon.claipo.test.MCPClient`)

Experimental Model Context Protocol client using `io.modelcontextprotocol.sdk`. Connects to an MCP server via stdio subprocess and wraps tool calls into Claude conversations.

## Key dependencies

- `com.anthropic:anthropic-java:2.17.0` — Claude SDK
- `com.google.cloud:google-cloud-vertexai` + `com.google.genai:google-genai` — Gemini/Vertex
- `io.modelcontextprotocol.sdk:mcp:0.17.2` — MCP support
- `com.fasterxml.jackson.core:jackson-databind` — JSON parsing
- Java 11, Maven 3.x

## Runtime data

- `email.json` — email thread consumed by `AgenteCorreo` (working directory)
- `data/` — calendar cache directory (ICS files cached for 1 hour)
- `claipo.log` — runtime log written by `Util`
