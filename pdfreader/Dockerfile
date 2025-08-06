# Use Java 21 base image
FROM eclipse-temurin:21-jdk as runtime

# Set working directory
WORKDIR /app

# Copy the JAR and config
COPY pdfreader-0.0.1-SNAPSHOT.jar app.jar
COPY application.yml /app/config/application.yml

# Expose the port your app runs on
EXPOSE 9090

# Set config location so Spring reads application.yml
ENV SPRING_CONFIG_LOCATION=classpath:/,file:/app/config/

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
