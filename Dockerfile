FROM maven:3.6.3-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY hivpn.apk /app/hivpn.apk
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/hivpn-server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
