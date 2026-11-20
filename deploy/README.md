# deploy

Docker Compose files and environment configuration. They run the whole stack on one machine.

To run the system tests, one command is enough: `./gradlew systemTest` builds the image, starts the stack, runs the tests and stops the stack. The commands below are for a stack that stays up.

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
