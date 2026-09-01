# https://hub.docker.com/r/epicsoft/openjdk — public, Alpine based, and its
# busybox brings the wget the container health check needs. (A Debian based JRE
# image carries neither wget nor curl; the check then fails on a perfectly
# healthy application, while the application's own log looks fine.)
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

# 8080 serves the API, 8090 the actuator. The management port stays inside the
# compose network so the health check can use it without exposing it.
EXPOSE 8080 8090

ARG JAR_FILE
COPY ${JAR_FILE} /app.jar

# Numeric only: no passwd entry is needed to run a JVM, and it keeps this
# working on any base image.
USER 10001:10001

ENTRYPOINT [ "sh", "-c", "java $JAVA_SERVER $JAVA_DOCKER $JAVA_ENCODING $JAVA_TIMEZONE $JAVA_ERROR $JAVA_OPTS -jar /app.jar" ]
