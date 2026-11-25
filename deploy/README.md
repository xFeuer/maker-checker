# deploy

Docker Compose files and environment configuration. They run the whole stack on one machine.

To run the system tests, one command is enough: `./gradlew systemTest` builds the image, starts the stack, runs the tests, saves the logs of the stack and stops it. The commands below are for a stack that stays up.

## Start the stack

The images package the jars that Gradle builds, so build them first:

```
./gradlew bootJar
docker compose --file deploy/compose.yaml up --build --wait
```

The second command returns when every service is healthy. `account-service` then answers on `http://localhost:8080/v1/status`.

## Stop the stack

```
docker compose --file deploy/compose.yaml down
```

## Logs

`./gradlew systemTest` saves the logs of the stack to `build/stack-logs/stack.log` before it stops the stack. It does that even when a test fails or the stack does not become healthy. The file holds what every container wrote, to its standard output and to its standard error.

In CI, the file is the artifact `stack-logs` of a run.

For a stack that stays up, this task writes the same file:

```
./gradlew saveStackLogs
```
