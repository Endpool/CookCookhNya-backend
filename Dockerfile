FROM sbtscala/scala-sbt:eclipse-temurin-17.0.4_1.7.1_3.2.0 as sbt

WORKDIR /app

COPY . .

RUN sbt stage

WORKDIR ./target/universal/stage/bin

RUN chmod +x ./cookcookhny-backend

ENTRYPOINT ["./cookcookhny-backend"]

