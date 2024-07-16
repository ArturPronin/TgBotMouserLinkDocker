FROM maven:3.8.4-openjdk-17-slim AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests

FROM openjdk:17-oracle

WORKDIR /app

COPY --from=build /app/target/TgBotMouserLink-1.0-SNAPSHOT.jar .

CMD ["java", "-jar", "TgBotMouserLink-1.0-SNAPSHOT.jar"]