# Use Java 21 base image
FROM eclipse-temurin:21-jdk as runtime

# Set working directory
WORKDIR /app

# Copy the JAR from target directory
COPY target/pdfreader-0.0.1-SNAPSHOT.jar app.jar

# Copy the application.yml from the src folder
COPY src/main/resources/application.yml /app/config/application.yml

# Expose the port your app runs on
EXPOSE 9090

# Set config location so Spring reads application.yml
ENV SPRING_CONFIG_LOCATION=classpath:/,file:/app/config/

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
