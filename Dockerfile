FROM sbtscala/scala-sbt:eclipse-temurin-17.0.4_1.7.1_3.2.0 AS builder

WORKDIR /app

COPY build.sbt .
COPY project/*.sbt project/*.scala ./project/
RUN sbt update

COPY src ./src
RUN sbt stage

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/universal/stage .

RUN useradd -m appuser &&     chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["./bin/cookcookhnya-backend"]
