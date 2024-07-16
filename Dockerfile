FROM openjdk:17-oracle

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file into the container
COPY target/TgBotMouserLink-1.0-SNAPSHOT.jar /app/TgBotMouserLink-1.0-SNAPSHOT.jar

# Define the command to run your application
CMD ["java", "-jar", "TgBotMouserLink-1.0-SNAPSHOT.jar"]
