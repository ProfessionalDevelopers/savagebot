# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean compile assembly:single -DskipTests -q

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/savagebot-*-jar-with-dependencies.jar savagebot.jar
COPY entrypoint.sh .
ENTRYPOINT ["./entrypoint.sh"]
