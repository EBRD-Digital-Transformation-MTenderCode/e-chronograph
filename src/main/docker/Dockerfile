FROM openjdk:8-jre-alpine

RUN mkdir maven
COPY /artifact_runner.sh maven/
COPY maven/ /maven/

EXPOSE ${project.service.port}

CMD /maven/artifact_runner.sh /maven/${final-artifact-name}.jar
