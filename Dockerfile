FROM bellsoft/liberica-openjdk-debian:17 AS build
WORKDIR /app
COPY . .
RUN ./mvnw -q -DskipTests package

FROM bellsoft/liberica-openjdk-debian:17
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

