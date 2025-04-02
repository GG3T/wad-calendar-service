FROM amazoncorretto:17
WORKDIR /app
COPY target/wad-calendar-service-0.0.1.jar app.jar
CMD ["java", "-jar", "app.jar"]