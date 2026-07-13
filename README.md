# QueueCTL

QueueCTL is a CLI-based background job queue system built in Java. It provides persistent job storage, command-line job management, and support for running multiple workers.

This project was developed as part of a Backend Developer Internship assignment.

## Tech Stack

- Java 17
- Maven
- Picocli
- SQLite
- JDBC
- Jackson
- JUnit 5

## Features

Currently implemented:

- Enqueue jobs using JSON input
- Persist jobs in SQLite
- List jobs
- Filter jobs by state
- Start multiple workers
- Execute jobs in the background
- Track worker state
- View queue and worker status
- Graceful worker shutdown
- Executable fat JAR
- `queuectl` command wrapper for Windows

Additional assignment features such as retry/backoff, Dead Letter Queue behavior, and crash recovery are part of the QueueCTL design.

## Project Structure

```text
src/main/java/org/example/
│
├── Main.java
│
├── cli/
│   ├── Config.java
│   ├── DeadLetterQueue.java
│   ├── Queuecli.java
│   ├── Status.java
│   └── Worker.java
│
├── db/
│   └── DatabaseManager.java
│
├── Helper/
│   ├── PropertyHelper.java
│   └── TimeHelper.java
│
├── model/
│   ├── Config.java
│   ├── Job.java
│   ├── State.java
│   ├── StateWorker.java
│   └── Worker.java
│
├── Repository/
│   ├── ConfigRepository.java
│   ├── JobRepository.java
│   └── WorkerRepository.java
│
└── Subcommands/
    ├── DlqList.java
    ├── Enqueue.java
    ├── ListAll.java
    ├── ListByState.java
    ├── Retry.java
    ├── Set.java
    ├── WorkerStart.java
    └── WorkerStop.java
```

## Prerequisites

Make sure the following are installed:

- Java 17 or later
- Maven
- Git

Verify the installation:

```bash
java -version
mvn -version
git --version
```

## Setup

Clone the repository:

```bash
git clone https://github.com/Momin2005-ui/queuectl.git
cd queuectl
```

Build the project:

```bash
mvn clean package
```

This creates an executable JAR inside the `target` directory.

## Running QueueCTL

### Using the JAR directly

```bash
java -jar target/queuectl-1.0-SNAPSHOT.jar <command>
```

Example:

```bash
java -jar target/queuectl-1.0-SNAPSHOT.jar status
```

### Windows CLI Wrapper

The repository contains `queuectl.bat`, which forwards CLI arguments to the executable JAR.

From PowerShell in the project directory:

```powershell
.\queuectl status
```

To use `queuectl` directly from any directory, add the project directory to the Windows `PATH`.

```powershell
.\queuectl status
```

## CLI Usage

### Enqueue a Job

```bash
.\queuectl enqueue "{\"id\":\"job1\",\"command\":\"echo Hello\"}"
```

A job contains an ID and the shell command that should be executed.

### Start Workers

```bash
.\queuectl worker start --count 3
```

Starts three workers.

### Stop Workers

```bash
.\queuectl worker stop
```

Requests running workers to stop gracefully.

### View Status

```bash
.\queuectl status
```

Displays queue state information and active worker information.

### List Jobs by State

```bash
.\queuectl list --state pending
```

JSON output:

```bash
.\queuectl list --state pending --json
```

### Dead Letter Queue

List dead jobs:

```bash
.\queuectl dlq list
```

Retry a dead job:

```bash
.\queuectl dlq retry job1
```

### Configuration

Example:

```bash
.\queuectl config set max-retries 3
```

## Job Lifecycle

Jobs move through the following states:

```text
pending
   |
   v
processing
   |
   +------> completed
   |
   +------> failed
                |
                +------> retry
                |
                +------> dead (DLQ)
```

| State | Description |
|---|---|
| `pending` | Waiting for a worker |
| `processing` | Currently being executed |
| `completed` | Successfully executed |
| `failed` | Execution failed and may be retried |
| `dead` | Retry limit exhausted |

## Architecture

QueueCTL follows a layered structure:

```text
CLI Layer
    |
    v
Subcommands
    |
    v
Repositories
    |
    v
SQLite Database
```

### CLI Layer

Picocli parses commands and routes them to the corresponding subcommand.

### Repository Layer

Repository classes encapsulate database operations:

- `JobRepository` — job persistence and state management
- `WorkerRepository` — worker persistence and state management
- `ConfigRepository` — configuration persistence

### Persistence Layer

SQLite is used for persistent local storage. Job and worker state can therefore survive CLI process termination and application restarts.

### Worker Execution

Workers retrieve jobs from persistent storage, execute their commands, and update job state according to the execution result.

Multiple workers can be started using:

```bash
.\queuectl worker start --count <number>
```

## Building the Executable JAR

The project uses the Maven Shade Plugin to create an executable fat JAR.

The generated JAR contains:

- QueueCTL application classes
- Picocli
- SQLite JDBC driver
- Jackson dependencies

The JAR manifest defines the application entry point, allowing the application to run with:

```bash
java -jar target/queuectl-1.0-SNAPSHOT.jar
```

## Testing

Run the test suite with:

```bash
mvn test
```

Important scenarios to verify include:

1. A basic job completes successfully.
2. A failing job is handled correctly.
3. Multiple workers process jobs without duplicate execution.
4. Worker termination does not permanently leave jobs stuck in `processing`.
5. Jobs survive application restarts.

## Demo

CLI demo recording:

https://drive.google.com/file/d/1i39LRCQPn8ca3iE55wY_pJe3L6M47PZO/view

## Design Decisions

Detailed implementation decisions and trade-offs are documented in:

```text
DECISIONS.md
```

## Repository

GitHub: https://github.com/Momin2005-ui/queuectl
