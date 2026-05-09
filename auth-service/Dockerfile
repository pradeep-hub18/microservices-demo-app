FROM eclipse-temurin:17-jre

WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
ARG JAR_FILE=target/auth-service-*.jar
COPY --chown=app:app ${JAR_FILE} app.jar

EXPOSE 8080
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
