# WildFly de logica: despliega cargauy.ear (logica + servicios en /api).
# Se construye con el contexto en la raiz del repo:
#   docker build -f infra/wildfly/logica/Dockerfile --target prod .

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY . .
RUN mvn -B -pl ear -am -DskipTests package


FROM quay.io/wildfly/wildfly:41.0.0.Final-jdk25 AS server

ARG POSTGRES_DRIVER_VERSION=42.7.13

ADD --chown=jboss:jboss https://repo1.maven.org/maven2/org/postgresql/postgresql/${POSTGRES_DRIVER_VERSION}/postgresql-${POSTGRES_DRIVER_VERSION}.jar /tmp/postgresql.jar

COPY --chown=jboss:jboss infra/wildfly/logica/configuracion.cli /tmp/configuracion.cli

# Directorio por defecto de ARCHIVOS_DIRECTORIO (en desarrollo se monta ./archivos encima)
USER root
RUN mkdir -p /var/lib/cargauy/archivos && chown jboss:jboss /var/lib/cargauy/archivos
USER jboss

RUN /opt/jboss/wildfly/bin/jboss-cli.sh --command="module add --name=org.postgresql --resources=/tmp/postgresql.jar --dependencies=java.sql,jakarta.transaction.api" \
    && /opt/jboss/wildfly/bin/jboss-cli.sh --file=/tmp/configuracion.cli \
    && rm -rf /tmp/postgresql.jar /tmp/configuracion.cli /opt/jboss/wildfly/standalone/configuration/standalone_xml_history

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

COPY --from=build --chown=jboss:jboss /src/ear/target/cargauy.ear /opt/jboss/wildfly/standalone/deployments/
