FROM node:22 AS node
WORKDIR /usr/app
COPY package.json ./
RUN npm install

FROM amazoncorretto:21

COPY gradle/ /gradle/
COPY build.gradle /build.gradle
COPY gradlew /gradlew
COPY settings.gradle /settings.gradle

# Download the Gradle distribution
RUN ./gradlew --version

COPY --from=node /usr/app/node_modules /node_modules
COPY src/ /src/

RUN ./gradlew build -x npmInstall

USER nobody

CMD ["java","-jar","/build/libs/frc-codex.jar"]
EXPOSE 8080
