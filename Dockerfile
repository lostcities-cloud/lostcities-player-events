FROM gradle:7.2.0-jdk16

WORKDIR /player-events

COPY ./ ./

ARG actor
ARG token

ENV GITHUB_ACTOR=$actor
ENV GITHUB_TOKEN=$token
ENV GRADLE_USER_HOME="/var/lib/gradle"
ENV GRADLE_OPTS="-Dorg.gradle.project.buildDir=/tmp/gradle-build"

RUN gradle clean build --no-daemon

EXPOSE 8080
EXPOSE 5005

CMD gradle bootRun --no-daemon -Pdebug_jvm
