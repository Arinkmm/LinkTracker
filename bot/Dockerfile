FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY ai-agent/pom.xml ai-agent/pom.xml
COPY api-common/pom.xml api-common/pom.xml
COPY bot/pom.xml bot/pom.xml
COPY build-report-aggregate/pom.xml build-report-aggregate/pom.xml
COPY scrapper/pom.xml scrapper/pom.xml
RUN mvn dependency:go-offline -pl bot -am
COPY ai-agent/src ai-agent/src
COPY api-common/src api-common/src
COPY bot/src bot/src
COPY scrapper/src scrapper/src
RUN mvn package -pl bot -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/bot/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
