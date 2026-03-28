FROM maven:3.9-eclipse-temurin-25 AS deps
WORKDIR /app
COPY pom.xml .
COPY ai-agent/pom.xml ai-agent/
COPY api-common/pom.xml api-common/
COPY bot/pom.xml bot/
COPY build-report-aggregate/pom.xml build-report-aggregate/
COPY scrapper/pom.xml scrapper/
RUN mvn dependency:go-offline

FROM deps AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/scrapper/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
