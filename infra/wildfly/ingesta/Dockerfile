# WildFly de ingesta-tracking: despliega ingesta-tracking.war (en /ingesta).
# Solo usa Kafka y Mongo, cuyos clientes van dentro del WAR: no necesita driver
# ni datasource. Se construye con el contexto en la raiz del repo:
#   docker build -f infra/wildfly/ingesta/Dockerfile --target prod .

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY . .
RUN mvn -B -pl ingesta-tracking -am -DskipTests package


FROM quay.io/wildfly/wildfly:41.0.0.Final-jdk25 AS server

EXPOSE 8080

CMD ["/opt/jboss/wildfly/bin/standalone.sh", \
     "-c", "standalone.xml", \
     "-b", "0.0.0.0"]


FROM server AS dev

RUN /opt/jboss/wildfly/bin/add-user.sh admin admin --silent

EXPOSE 9990 8787

CMD ["/opt/jboss/wildfly/bin/standalone.sh", \
     "-c", "standalone.xml", \
     "-b", "0.0.0.0", \
     "-bmanagement", "0.0.0.0"]


FROM server AS prod

COPY --from=build --chown=jboss:jboss /src/ingesta-tracking/target/ingesta-tracking.war /opt/jboss/wildfly/standalone/deployments/
