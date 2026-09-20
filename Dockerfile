FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN apk add --no-cache maven \
    && mvn package -DskipTests -q \
    && mv target/*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 3030

ENTRYPOINT ["java", "-jar", "app.jar"]
