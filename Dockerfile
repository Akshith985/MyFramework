# Stage 1: Build the App
# We use a Maven image with Java 21 to compile the code
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build the jar file
RUN mvn clean package -DskipTests

# Stage 2: Run the App
# We use a lightweight Java 21 image to run it
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy the built jar from Stage 1
COPY --from=build /app/target/*.jar app.jar

# Open port 8080
EXPOSE 8080

# Command to start the server
ENTRYPOINT ["java", "-jar", "app.jar"]