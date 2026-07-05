# Stage 1: build the jar using the Maven wrapper
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: run the jar in a lightweight JRE-only image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/finguard-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
