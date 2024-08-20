FROM amazoncorretto:21

USER nobody

COPY build/libs/ /opt/

CMD ["java","-jar","/opt/frc-codex.jar"]
EXPOSE 8080
