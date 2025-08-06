# ---------- Build Stage ----------
FROM eclipse-temurin:21-jdk as builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# ---------- Runtime Stage ----------
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/target/pdfreader-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9090

ENV SPRING_CONFIG_LOCATION=classpath:/,file:/app/config/
ENTRYPOINT ["java", "-jar", "app.jar"]
