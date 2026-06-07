# Code Graph Engine

This document defines the local deterministic code graph layer. The design intentionally follows the CodeGraph approach from `examples/codegraph-main`, adapted to Chatly Code's Java/Spring/JavaFX product architecture.

## Goal

The agent should not discover the project by repeatedly running grep and reading random files. Chatly Code must pre-index the codebase into a local graph and give the agent precise, compact context.

```text
Without graph:
agent -> grep -> read -> grep -> read -> maybe understands

With graph:
agent -> graph query -> focused context -> plan -> safe edit
```

## Non-Goals

- Do not add a separate analytical database for MVP.
- Do not use LLM as the source of truth for code structure.
- Do not build a huge general graph database product.
- Do not require cloud services for indexing.

## Storage

Use SQLite for the graph, same local-first idea as CodeGraph.

Per-project data can live under:

```text
.chatly-code/
  graph.db
  locks/
  cache/
```

The main app database can still store product state. For MVP, both product tables and graph tables may live in one SQLite database if simpler. The important part is a clean schema boundary.

## Pipeline

```text
Project files
-> File inventory
-> Language detection
-> AST parsing
-> Node extraction
-> Edge extraction
-> Unresolved reference collection
-> Reference resolution
-> Framework resolution
-> Graph queries
-> Context builder
-> Agent / C4 / Problems
```

## Scan-to-Prompt Path

This is the required path from raw source code to an agent prompt. Do not skip steps and do not replace deterministic graph extraction with LLM guessing.

```text
1. ProjectScanner finds source files and ignores generated/vendor files.
2. CodeGraphIndexer detects language for each file.
3. AstExtractor parses source into AST.
4. NodeExtractor emits CodeNode records.
5. EdgeExtractor emits direct CodeEdge records.
6. UnresolvedReferenceCollector stores references that cannot be resolved per-file.
7. ReferenceResolver resolves imports, packages, aliases and known symbols.
8. FrameworkResolver adds framework-aware nodes/edges, for example Spring routes.
9. GraphQueryService answers search/callers/callees/impact/layer queries.
10. ContextBuilder builds compact model-ready context with source snippets and line numbers.
11. PromptAssembler combines task, rules, graph context, C4 context, risks and allowed tools.
12. AgentCore asks the model for a plan or next action.
```

Prompt assembly must look like this:

```text
System:
- product rules
- project rules
- tool policy

Task:
- user/task text
- acceptance criteria
- risk level

Project context:
- detected stack
- build/test commands
- git state

Graph context:
- relevant nodes
- relevant edges
- callers/callees
- impact radius
- source snippets with line numbers
- unresolved/stale warnings

Architecture context:
- related C4 nodes
- layer boundaries
- architecture violations

Instruction:
- first produce a plan
- do not edit before approval
- use tools through AgentCore
```

The graph context should be generated before the model is asked to reason. The model receives evidence; it does not invent the project structure.

## Porting Plan from CodeGraph

Do not invent this pipeline from scratch. Port the proven CodeGraph design from `C:\projects\chatly-code\examples\codegraph-main` into Java and bind it to Chatly Code's desktop workflow.

Use these files as implementation references:

```text
CodeGraph reference                                           Chatly Code Java target
----------------------------------------------------------------------------------------------------
src/index.ts                                                  CodeGraphFacade / ProjectGraphService
src/types.ts                                                  CodeNode, CodeEdge, CodeFile, NodeKind, EdgeKind
src/db/schema.sql                                             SQLite graph schema migrations
src/db/queries.ts                                             CodeGraphRepository / prepared queries
src/extraction/index.ts                                       CodeGraphIndexer
src/extraction/tree-sitter.ts                                 TreeSitterAstExtractor
src/extraction/tree-sitter-types.ts                           LanguageExtractor SPI
src/extraction/tree-sitter-helpers.ts                         AstNodeHelpers
src/extraction/generated-detection.ts                         GeneratedFileDetector
src/extraction/parse-worker.ts                                Bounded parse workers; virtual threads only for IO orchestration
src/extraction/languages/*.ts                                 Java LanguageExtractor implementations
src/resolution/index.ts                                       ReferenceResolver
src/resolution/import-resolver.ts                             ImportResolver / JavaImportResolver
src/resolution/name-matcher.ts                                NameMatcher
src/resolution/path-aliases.ts                                BuildPathAliasResolver
src/resolution/workspace-packages.ts                          WorkspaceModuleResolver
src/resolution/frameworks/*                                   SpringFrameworkResolver and later Rust resolvers
src/graph/traversal.ts                                        GraphTraverser
src/graph/queries.ts                                          GraphQueryService
src/context/index.ts                                          AgentContextBuilder
src/context/formatter.ts                                      AgentContextFormatter
src/mcp/tools.ts                                              Internal graph tools + optional MCP tools
src/sync/watcher.ts                                           CodeGraphFileWatcher
src/sync/index.ts                                             GraphSyncService
```

The Java implementation can use different libraries, but the responsibilities and data flow should stay the same.

## Java Porting Examples

## Language Plugin Boundary

Generic code graph engine must not hardcode Java/Spring, Rust or TypeScript-specific logic.

Use language plugins:

```text
language-spi
language-java
language-rust
language-typescript
```

Core graph pipeline calls plugins through interfaces:

```text
LanguagePlugin
LanguageExtractor
ReferenceResolver
FrameworkResolver
ProblemRule
BuildManifestDetector
TestCommandDetector
```

Rules:

- `code-graph-engine` owns normalized nodes/edges/files/unresolved references;
- language plugins own AST/language/framework-specific extraction details;
- problem-detector runs rules registered by plugins;
- prompt assembler consumes normalized evidence, not plugin internals;
- Java/Spring plugin must be removable or extractable without changing graph core;
- Rust/TypeScript plugins must not depend on Java/Spring plugin.

### Node and Edge records

```java
public record CodeNode(
    String id,
    NodeKind kind,
    String name,
    String qualifiedName,
    Path filePath,
    String language,
    int startLine,
    int endLine,
    String signature,
    String docstring,
    Instant updatedAt
) {}

public record CodeEdge(
    String sourceId,
    String targetId,
    EdgeKind kind,
    EdgeProvenance provenance,
    double confidence,
    Integer line,
    String metadataJson
) {}
```

### Extraction result

```java
public record ExtractionResult(
    List<CodeNode> nodes,
    List<CodeEdge> edges,
    List<CodeUnresolvedReference> unresolvedReferences,
    List<ExtractionError> errors,
    Duration duration
) {}
```

### Language extractor SPI

```java
public interface LanguageExtractor {
    String language();
    boolean supports(Path file, String source);
    ExtractionResult extract(Path file, String source, AstTree tree);
}
```

### Graph context for prompt

```java
public record AgentGraphContext(
    String query,
    List<CodeNode> entryPoints,
    List<CodeEdge> relevantEdges,
    List<SourceSnippet> snippets,
    List<Path> relatedFiles,
    boolean stale,
    String confidenceNote
) {}
```

This should map directly to prompt sections.

## Core Tables

### code_files

```text
path
content_hash
language
size
modified_at
indexed_at
node_count
generated
test_file
errors_json
```

### code_nodes

```text
id
kind
name
qualified_name
file_path
language
start_line
end_line
start_column
end_column
docstring
signature
visibility
is_exported
is_async
is_static
is_abstract
decorators_json
type_parameters_json
updated_at
```

### code_edges

```text
id
source_id
target_id
kind
metadata_json
line
column
provenance
confidence
```

### code_unresolved_refs

```text
id
from_node_id
reference_name
reference_kind
file_path
language
line
column
candidates_json
```

### code_graph_metadata

```text
key
value
updated_at
```

## Node Kinds

Use a controlled list:

```text
file
module
package
namespace
class
interface
enum
enum_member
record
annotation
function
method
constructor
property
field
variable
constant
parameter
import
export
route
component
database_table
external_service
```

## Edge Kinds

Use a controlled list:

```text
contains
imports
exports
calls
references
extends
implements
type_of
returns
instantiates
overrides
decorates
handles_route
reads_from
writes_to
publishes
subscribes
configured_by
tests
```

## Provenance

Every edge must say how it was created:

```text
tree_sitter
language_resolver
framework_resolver
build_manifest
heuristic
user_confirmed
```

Heuristic edges must have lower confidence and visible metadata.

## Extraction Layer

Responsibilities:

- parse source files;
- create file node;
- create package/namespace nodes;
- create declarations;
- create containment edges;
- collect direct calls/references when possible;
- collect unresolved references when target is unknown during per-file extraction.

Implementation notes:

- Java first-class support is required.
- Rust support should be planned early.
- Generated files should be detected and deprioritized.
- Test files should be marked.
- Parsing must run off the JavaFX thread.
- Large files need limits and error recording.

## Reference Resolver

Runs after extraction.

Responsibilities:

- resolve imports;
- resolve package names;
- resolve Java classes where file name and class name differ;
- resolve static imports;
- resolve method references where possible;
- resolve build/workspace aliases;
- resolve framework-specific links.

Important rule:

It is better to leave a reference unresolved than to create a confident but wrong edge.

## Framework Resolvers

Framework resolvers add higher-level links.

Java/Spring examples:

```text
@RestController -> route/component node
@GetMapping/@PostMapping -> route node
route -> controller method edge
controller method -> service calls
repository interface -> database access component
@Service/@Component/@Repository -> component classification
@Autowired/constructor injection -> dependency edge
@Entity -> persistence entity classification
DTO naming/usage -> DTO classification
@Configuration/@Bean -> configuration node/component
@KafkaListener -> message consumer node
@Scheduled -> scheduled job node
@FeignClient -> external client node
RestClient/WebClient usage -> external HTTP client edge
```

Rust examples later:

```text
Cargo workspace member -> module/container
axum route -> handler
actix route -> handler
trait impl -> implements edge
```

## Graph Queries

Required MVP queries:

```text
searchSymbols(query)
getNode(id)
getNodeContext(id)
getCallers(id)
getCallees(id)
getFileDependencies(path)
getFileDependents(path)
getImpactRadius(id, depth)
findCircularDependencies()
findLayerViolations()
findRoutes()
findTestsForNode(id)
```

## Context Builder

The context builder converts graph query results into model-ready context.

Responsibilities:

- find relevant symbols from task text;
- expand graph around entry points;
- include source snippets with line numbers;
- include callers/callees;
- include related files;
- keep output bounded;
- mark low-confidence results honestly;
- avoid telling agent to read files unless graph is stale or insufficient.

Design target:

```text
One graph context call should answer most "where is this implemented?" questions.
```

## Freshness and Sync

Use CodeGraph-style freshness:

- content hash for indexed files;
- file watcher;
- debounce sync;
- connect-time catch-up;
- stale-file banner when a referenced file has pending changes;
- manual resync command.

Agent must know if graph context may be stale.

## MCP Surface

Expose graph tools to the internal agent and optionally external MCP clients:

```text
chatly_graph_status
chatly_graph_search
chatly_graph_node
chatly_graph_explore
chatly_graph_callers
chatly_graph_callees
chatly_graph_impact
chatly_graph_routes
chatly_graph_layer_violations
```

The primary tool should be an `explore`-style tool that returns enough code and relationships for the agent to stop searching manually.

## C4 Integration

C4 should be built from graph facts:

```text
routes + controllers + services + repositories + external clients
-> components
packages/modules/build manifests
-> containers
dependencies and framework edges
-> relationships
```

LLM can improve descriptions, but graph evidence controls structure.

## Problem Detection Integration

Problem detector should query the graph:

```text
controllers importing repositories
entities exposed to API/UI DTO boundaries
services depending on UI
repositories calling services
cycles between modules
god classes by method/edge count
huge classes by line/member count
huge methods by line/complexity threshold
public methods with no callers
routes without tests
high blast-radius files
too many dependencies from one class/module
```

## Success Criteria

- Index is local and deterministic.
- Agent uses graph context before file reading.
- Graph answers reduce tool calls.
- C4 diagrams are traceable to source files.
- Problems include evidence from nodes/edges.
- Incremental sync keeps graph fresh.
