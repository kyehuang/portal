# NATS Architecture

This project uses NATS to send execution requests from a producer to one worker instance through a subject.

```mermaid
flowchart LR
    Client[Client / Python / Portal] -->|publish / request<br/>subject: portal.execute| NATS[NATS Server]

    NATS -->|deliver one message| W1[Worker A<br/>subscribe: portal.execute<br/>queue: portal-workers]
    NATS -.same queue group<br/>not duplicated.-> W2[Worker B<br/>subscribe: portal.execute<br/>queue: portal-workers]
    NATS -.same queue group<br/>not duplicated.-> W3[Worker C<br/>subscribe: portal.execute<br/>queue: portal-workers]

    W1 -->|POST /execute| Sidecar[Python Sidecar]
    Sidecar -->|ExecuteResponse| W1

    W1 -->|reply, if request/reply| NATS
    NATS -->|response| Client
```

## Routing

Producers only send to a subject:

```text
subject = portal.execute
```

Workers subscribe to the same subject with a queue group:

```text
subject = portal.execute
queue   = portal-workers
```

NATS delivers each message to only one subscriber inside the same queue group. This allows multiple worker instances to share load without processing the same request more than once.

## Request/Reply Flow

```mermaid
sequenceDiagram
    participant Client as Client / Python / Portal
    participant NATS as NATS Server
    participant Worker as Worker
    participant Sidecar as Python Sidecar

    Client->>NATS: request subject portal.execute
    NATS->>Worker: deliver request
    Worker->>Sidecar: POST /execute
    Sidecar-->>Worker: ExecuteResponse
    Worker-->>NATS: reply
    NATS-->>Client: response
```
