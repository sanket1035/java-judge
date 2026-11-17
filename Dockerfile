# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- run ----
FROM eclipse-temurin:17-jre-jammy
# The app starts sandbox containers, so it needs the Docker client binary (not the daemon)
COPY --from=docker:27.3-cli /usr/local/bin/docker /usr/local/bin/docker
WORKDIR /app
COPY --from=build /src/target/java-judge-*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
