FROM maven:3.9-eclipse-temurin-25 AS deps
WORKDIR /app
COPY pom.xml .
COPY ai-agent/pom.xml ai-agent/
COPY api-common/pom.xml api-common/
COPY bot/pom.xml bot/
COPY build-report-aggregate/pom.xml build-report-aggregate/
COPY scrapper/pom.xml scrapper/
RUN mvn -pl scrapper -am dependency:go-offline

FROM deps AS build
WORKDIR /app
COPY . .
RUN mvn -pl scrapper -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/scrapper/target/scrapper-0.0.1.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
