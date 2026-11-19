# Packages the jar that Gradle builds. Run the bootJar task before building the image.

# Stage 1: split the jar into layers, as the Spring Boot reference recommends.
FROM eclipse-temurin:25-jre-alpine AS builder
WORKDIR /builder
COPY build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# Stage 2: the runtime image.
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /application
# The layers go from the least to the most frequently changed, so Docker can reuse them.
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
USER app:app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
