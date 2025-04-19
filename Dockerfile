FROM sbtscala/scala-sbt:eclipse-temurin-alpine-21.0.6_7_1.10.11_3.6.4 AS builder

WORKDIR /app

COPY build.sbt .
COPY src ./src
COPY project ./project
COPY .env .env

RUN sbt stage

FROM alpine:latest AS runner

RUN apk add bash ca-certificates
COPY --from=builder /app/target/universal/stage /app/stage

WORKDIR /app/stage
CMD /app/stage/bin/mizunonde
