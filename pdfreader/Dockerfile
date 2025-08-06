# ---------- Build Stage ----------
FROM eclipse-temurin:21-jdk as builder

WORKDIR /app

COPY . .

# Build using Maven wrapper (no need to install Maven)
RUN ./mvnw clean package -DskipTests

# ---------- Runtime Stage ----------
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy the built JAR from previous stage
COPY --from=builder /app/target/pdfreader-0.0.1-SNAPSHOT.jar app.jar

# Copy config
COPY src/main/resources/application.yml /app/config/application.yml

EXPOSE 9090

ENV SPRING_CONFIG_LOCATION=classpath:/,file:/app/config/

ENTRYPOINT ["java", "-jar", "app.jar"]
