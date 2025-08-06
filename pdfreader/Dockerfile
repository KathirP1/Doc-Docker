# Build Stage
FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Run Stage
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
COPY application.yml /app/config/application.yml
EXPOSE 9090
ENV SPRING_CONFIG_LOCATION=classpath:/,file:/app/config/
ENTRYPOINT ["java", "-jar", "app.jar"]
