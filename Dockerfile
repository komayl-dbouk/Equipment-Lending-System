FROM eclipse-temurin:17-jre-jammy

RUN groupadd --system spring && useradd --system --gid spring --create-home spring
WORKDIR /app
COPY target/equipment-lending-system-0.0.1-SNAPSHOT.jar app.jar
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
