package com.chatlycode.cli;

import com.chatlycode.agent.domain.AgentRun;
import com.chatlycode.appserver.facade.ChatlyCodeFacade;
import com.chatlycode.appserver.facade.ProjectSession;
import com.chatlycode.graph.domain.IndexProgress;
import com.chatlycode.problem.domain.DetectedProblem;
import com.chatlycode.task.domain.EngineeringTask;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ChatlyCodeCli {

    private final ChatlyCodeFacade facade;
    private final Console console;

    public ChatlyCodeCli(ChatlyCodeFacade facade, Console console) {
        this.facade = facade;
        this.console = console;
    }

    public static void main(String[] args) {
        int exitCode = new ChatlyCodeCli(ChatlyCodeFacade.createDefault(), new Console()).run(args);
        System.exit(exitCode);
    }

    int run(String[] args) {
        CliCommand command = CliCommand.parse(args);
        if (command.help()) {
            printHelp();
            return 0;
        }
        if (command.name().isBlank()) {
            console.error("Missing command.");
            printHelp();
            return 2;
        }
        try {
            return switch (command.name()) {
                case "scan" -> scan(command);
                case "ask" -> ask(command);
                case "agent-plan" -> agentPlan(command);
                case "verify" -> verify(command);
                case "llm-status" -> llmStatus();
                default -> unknown(command.name());
            };
        } catch (IllegalArgumentException exception) {
            console.error(exception.getMessage());
            return 2;
        } catch (RuntimeException exception) {
            console.error("Command failed: " + exception.getMessage());
            return 1;
        }
    }

    private int scan(CliCommand command) {
        ProjectSession session = open(command);
        printProject(session);
        printProblemSummary(session.problems());
        printProblems(session.problems(), command.intOption("limit", 5));
        printTasks(session.tasks(), command.intOption("limit", 5));
        return 0;
    }

    private int ask(CliCommand command) {
        ProjectSession session = open(command);
        String question = command.requiredOption("question");
        var answer = facade.askGraph(session, question);
        console.out("Answer");
        console.out(answer.summary());
        if (!answer.evidence().isEmpty()) {
            console.out("");
            console.out("Evidence");
            answer.evidence().forEach(item -> console.out("- " + item));
        }
        return 0;
    }

    private int agentPlan(CliCommand command) {
        ProjectSession session = open(command);
        String taskText = command.option("task", "");
        AgentRun run;
        if (taskText.isBlank()) {
            EngineeringTask task = firstTask(session);
            run = facade.startAgentRun(session, task);
        } else {
            run = facade.submitDirectTask(session, taskText);
        }
        printRun(run);
        return 0;
    }

    private int verify(CliCommand command) {
        ProjectSession session = open(command);
        EngineeringTask task = firstTask(session);
        var result = facade.verifyTask(session, task);
        console.out("Verification");
        console.out("exit=" + result.exitCode());
        if (!result.stdout().isBlank()) {
            console.out("");
            console.out(result.stdout());
        }
        if (!result.stderr().isBlank()) {
            console.out("");
            console.out(result.stderr());
        }
        return result.exitCode() == 0 ? 0 : 1;
    }

    private int llmStatus() {
        var status = facade.llmStatus();
        console.out("LLM");
        console.out("configured=" + status.configured());
        console.out("provider=" + status.profile().provider());
        console.out("model=" + status.profile().model());
        if (!status.endpoint().isBlank()) {
            console.out("endpoint=" + status.endpoint());
        }
        console.out(status.message());
        return 0;
    }

    private ProjectSession open(CliCommand command) {
        Path project = command.pathOption("project", Path.of(".")).toAbsolutePath().normalize();
        if (!Files.isDirectory(project)) {
            throw new IllegalArgumentException("Project path is not a directory: " + project);
        }
        boolean quiet = command.booleanOption("quiet", false);
        return facade.openAndScan(project, progress -> {
            if (!quiet) {
                printProgress(progress);
            }
        });
    }

    private void printProgress(IndexProgress progress) {
        if (progress == null) {
            return;
        }
        String file = progress.currentFile() == null || progress.currentFile().isBlank()
                ? ""
                : " " + progress.currentFile();
        console.out("scan: " + progress.phase() + " " + progress.current() + "/" + progress.total() + file);
    }

    private EngineeringTask firstTask(ProjectSession session) {
        return session.tasks().stream()
                .min(Comparator.comparing(EngineeringTask::createdAt))
                .orElseThrow(() -> new IllegalArgumentException("No generated tasks are available for this project."));
    }

    private void printProject(ProjectSession session) {
        var project = session.project();
        var buildProfile = project.buildProfile();
        console.out("Project");
        console.out("name=" + project.displayName());
        console.out("root=" + project.root());
        console.out("stacks=" + project.stacks());
        console.out("build=" + joinCommand(buildProfile.buildCommand()));
        console.out("test=" + joinCommand(buildProfile.testCommand()));
        console.out("graph.nodes=" + session.graph().nodes().size());
        console.out("graph.edges=" + session.graph().edges().size());
        console.out("problems=" + session.problems().size());
        console.out("tasks=" + session.tasks().size());
        console.out("");
        console.out("Architecture");
        console.out("files=" + session.architecture().fileCount());
        console.out("nodes=" + session.architecture().nodeCount());
        console.out("edges=" + session.architecture().edgeCount());
        console.out("topPackages=" + session.architecture().topPackages());
        console.out("c4JsonBytes=" + session.architecture().structurizrJson().length());
        if (!session.architecture().structurizrDsl().isBlank()) {
            console.out("");
            console.out(session.architecture().structurizrDsl());
        }
    }

    private void printProblems(List<DetectedProblem> problems, int limit) {
        if (problems.isEmpty() || limit <= 0) {
            return;
        }
        console.out("");
        console.out("Top Problems");
        problems.stream().limit(limit).forEach(problem -> {
            console.out("- [" + problem.severity() + "] " + problem.title());
            console.out("  at: " + problem.primaryPath() + ":" + problem.line());
            if (!problem.evidence().isEmpty()) {
                console.out("  evidence: " + String.join("; ", problem.evidence()));
            }
        });
    }

    private void printProblemSummary(List<DetectedProblem> problems) {
        if (problems.isEmpty()) {
            return;
        }
        console.out("");
        console.out("Problem Summary");
        Map<String, Long> counts = problems.stream()
                .collect(Collectors.groupingBy(
                        problem -> problem.severity() + " " + problem.type(),
                        java.util.LinkedHashMap::new,
                        Collectors.counting()
                ));
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .limit(16)
                .forEach(entry -> console.out("- " + entry.getKey() + ": " + entry.getValue()));
    }

    private void printTasks(List<EngineeringTask> tasks, int limit) {
        if (tasks.isEmpty() || limit <= 0) {
            return;
        }
        console.out("");
        console.out("Tasks");
        tasks.stream().limit(limit).forEach(task -> {
            console.out("- [" + task.risk() + "] " + task.title());
            console.out("  done: " + task.definitionOfDone());
        });
    }

    private void printRun(AgentRun run) {
        console.out("Agent Run");
        console.out("id=" + run.id());
        console.out("status=" + run.status());
        console.out("runtime=" + run.runtimeMode());
        console.out("checkpoint=" + run.checkpointRef());
        console.out("");
        console.out("Plan");
        run.plan().steps().forEach(step -> console.out("- " + step));
        console.out("");
        console.out("Actions");
        run.actions().forEach(action -> console.out("- " + action.type() + " | " + action.summary()));
    }

    private int unknown(String name) {
        console.error("Unknown command: " + name);
        printHelp();
        return 2;
    }

    private void printHelp() {
        console.out("""
                Chatly Code CLI

                Usage:
                  chatly-code <command> [options]

                Commands:
                  scan --project <path> [--limit 5]
                  ask --project <path> --question "Where is X used?"
                  agent-plan --project <path> [--task "Add feature"]
                  verify --project <path>
                  llm-status

                Options:
                  --project <path>   Target project path. Defaults to current directory.
                  --limit <number>   Number of problems/tasks to print.
                  --help             Show this help.
                """);
    }

    private String joinCommand(List<String> command) {
        return command.isEmpty() ? "<not detected>" : String.join(" ", command);
    }

    static final class Console {

        void out(String value) {
            System.out.println(value);
        }

        void error(String value) {
            System.err.println(value);
        }
    }

    private record CliCommand(String name, java.util.Map<String, String> options, boolean help) {

        static CliCommand parse(String[] args) {
            if (args == null || args.length == 0) {
                return new CliCommand("", java.util.Map.of(), false);
            }
            if (Arrays.asList(args).contains("--help") || Arrays.asList(args).contains("-h")) {
                String name = args[0].startsWith("-") ? "" : args[0];
                return new CliCommand(name, java.util.Map.of(), true);
            }
            String name = args[0];
            java.util.Map<String, String> options = new java.util.LinkedHashMap<>();
            for (int index = 1; index < args.length; index++) {
                String option = args[index];
                if (!option.startsWith("--")) {
                    throw new IllegalArgumentException("Unexpected argument: " + option);
                }
                String key = option.substring(2);
                if (key.isBlank()) {
                    throw new IllegalArgumentException("Blank option name.");
                }
                if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
                    options.put(key, "true");
                } else {
                    options.put(key, args[++index]);
                }
            }
            return new CliCommand(name, java.util.Map.copyOf(options), false);
        }

        String requiredOption(String name) {
            String value = option(name, "");
            if (value.isBlank()) {
                throw new IllegalArgumentException("Missing required option --" + name);
            }
            return value;
        }

        String option(String name, String defaultValue) {
            return options.getOrDefault(name, defaultValue);
        }

        int intOption(String name, int defaultValue) {
            String value = option(name, Integer.toString(defaultValue));
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Option --" + name + " must be a number.");
            }
        }

        Path pathOption(String name, Path defaultValue) {
            return Path.of(option(name, defaultValue.toString()));
        }

        boolean booleanOption(String name, boolean defaultValue) {
            String value = option(name, Boolean.toString(defaultValue));
            return Boolean.parseBoolean(value);
        }
    }
}
