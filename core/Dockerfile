FROM eclipse-temurin:25-jre

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

RUN useradd --system --uid 10001 --no-create-home vc
USER 10001

ENTRYPOINT [ "sh", "-c", "java $JAVA_SERVER $JAVA_DOCKER $JAVA_ENCODING $JAVA_TIMEZONE $JAVA_ERROR $JAVA_OPTS -jar /app.jar" ]
