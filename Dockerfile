FROM eclipse-temurin:17-jre-focal

WORKDIR /app

# Ensure we wait for dependencies conceptually, but docker-compose 'depends_on' handles order
# (Though depends_on doesn't wait for readiness, in a real env a wait-for-it script is better)

COPY target/transfer-search-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
