FROM openjdk:17

WORKDIR /home/draco

# Add prebuilt frontend
#COPY frontend             /home/draco/frontend
#COPY node_modules         /home/draco/node_modules
#COPY package.json         /home/draco/package.json
#COPY package-lock.json    /home/draco/package-lock.json
#COPY types.d.ts           /home/draco/types.d.ts
#COPY webpack.config.js    /home/draco/webpack.config.js
#COPY webpack.generated.js /home/draco/webpack.generated.js

# Add bootable jar
COPY build/libs/*.jar /home/draco/draco.jar

CMD ["java", "-jar", "draco.jar"]
