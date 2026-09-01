# https://hub.docker.com/r/epicsoft/openjdk — Alpine based, so busybox provides
# the wget the container health check needs.
ARG IMAGE_VERSION=jre25-latest
FROM epicsoft/openjdk:$IMAGE_VERSION

ARG APP_VERSION="development"
ARG BUILD_DATE="development"

LABEL org.opencontainers.image.title="Version Control Core" \
      org.opencontainers.image.description="Infrastructure version overview — backend" \
      org.opencontainers.image.vendor="epicsoft LLC" \
      org.opencontainers.image.version="$APP_VERSION" \
      org.opencontainers.image.created="$BUILD_DATE" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.source="https://gitlab.com/epicsoft-networks/version-control"

ENV JAVA_SERVER="-server" \
    JAVA_DOCKER="-XX:+UseContainerSupport -XX:MaxRAMPercentage=90" \
    JAVA_ENCODING="-Dfile.encoding=UTF-8" \
    JAVA_TIMEZONE="-Duser.timezone=UTC" \
    JAVA_ERROR="-XX:+ExitOnOutOfMemoryError" \
    JAVA_OPTS=""

# 8080 API, 8090 actuator — the latter stays inside the compose network.
EXPOSE 8080 8090

ARG JAR_FILE
COPY ${JAR_FILE} /app.jar

# Numeric: a JVM needs no passwd entry, and this works on any base image.
USER 10001:10001

ENTRYPOINT [ "sh", "-c", "java $JAVA_SERVER $JAVA_DOCKER $JAVA_ENCODING $JAVA_TIMEZONE $JAVA_ERROR $JAVA_OPTS -jar /app.jar" ]
