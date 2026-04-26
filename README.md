# Java Multithreaded Chat Room

A console-based, multi-user chat application built in Java using TCP sockets and multithreading. The server handles multiple concurrent clients, broadcasts messages in real time, and persists chat history to disk.

Built as a course project for **CSC1004 (Computer Science Laboratory using Java)** at CUHK-Shenzhen.

## Features

- **Multiple concurrent users** — server accepts unlimited clients, each handled on its own thread.
- **Real-time message broadcast** — messages from any client are pushed to all other connected clients instantly.
- **Auto-generated user IDs** — each client gets a unique 5-digit ID on connection (e.g., `01234`).
- **Active-user listing** — new joiners see who's already online.
- **Timestamped messages** — every message is logged with `HH:mm:ss`.
- **Persistent chat history** — all messages are saved to `chat_history.txt` on the server, and replayed to clients when they join.
- **Clean disconnect handling** — typing `logout` notifies other users and closes the connection gracefully.

## Architecture

```
┌──────────┐         ┌──────────────────┐         ┌──────────┐
│ Client A │ ──TCP──▶│                  │◀──TCP── │ Client B │
└──────────┘         │     Server       │         └──────────┘
                     │  (port 6666)     │
┌──────────┐         │                  │         ┌──────────┐
│ Client C │ ──TCP──▶│  - 1 thread per  │◀──TCP── │ Client D │
└──────────┘         │    client        │         └──────────┘
                     │  - synchronized  │
                     │    broadcast     │
                     │  - persists to   │
                     │    chat_history  │
                     └──────────────────┘
```

- `Server.java` — listens on port 6666, accepts incoming connections, spawns a `ClientHandler` thread per client. Maintains a thread-safe `Vector` of connected handlers and a synchronized chat-history list backed by a file.
- `Client.java` — connects to the server, runs two threads in parallel: one reads from `stdin` and sends to the server, the other reads from the server and prints to `stdout`.

## Tech stack

- **Java 18** (`java.net.Socket`, `ServerSocket`, `DataInputStream`, `DataOutputStream`, `Thread`, `Runnable`)
- **Maven** for build

## How to run

### Prerequisites
- JDK 18 or higher
- Maven (or just `javac` / `java` if you prefer)

### Build
```bash
mvn compile
```

### Start the server
In one terminal:
```bash
mvn exec:java -Dexec.mainClass="org.example.Server"
```
Or directly:
```bash
java -cp target/classes org.example.Server
```

### Start a client
In another terminal (open as many as you want):
```bash
java -cp target/classes org.example.Client
```

You'll be prompted for a username, then you can start chatting. Type `logout` to exit.

## Possible extensions

- Replace text-file persistence with SQLite for structured queries.
- Add private (DM) messages alongside broadcast.
- Build a JavaFX or web frontend on top of the existing protocol.
- Containerize the server with Docker and deploy to a VPS.
