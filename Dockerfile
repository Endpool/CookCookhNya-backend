FROM sbtscala/scala-sbt:graalvm-ce-22.3.3-b1-java17_1.10.11_3.6.4 AS sbt

WORKDIR /app

COPY . .

EXPOSE 8080

CMD ["sbt", "run"]
