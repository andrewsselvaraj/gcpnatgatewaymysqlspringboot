# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /src/target/*.jar app.jar
USER app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
# Cloud Run sets PORT (default 8080); application.yml reads it
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
