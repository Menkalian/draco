FROM openjdk:17

WORKDIR /home/draco

# Add bootable jar
COPY build/libs/*.jar /home/draco/draco.jar

CMD ["java", "-jar", "draco.jar"]
