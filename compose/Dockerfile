FROM openjdk:25-ea-24-slim
WORKDIR /app
COPY ../build/libs/the-best-chat-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]