FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY ./build/libs/community-0.0.1-SNAPSHOT.jar .

EXPOSE 8080

CMD ["java", "-jar", "community-0.0.1-SNAPSHOT.jar", "--spring.profiles.active=docker"]