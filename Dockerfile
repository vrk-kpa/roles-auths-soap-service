# Pull base image
FROM docker-registry.kapa.ware.fi/roles-auths-java-base@sha256:84db8eb95099600b8fe3b0d072145da6a679cd2646b559ac3d2ddbea2717805a

# Deploy project
RUN mkdir -p /opt/rova/roles-auths-soap-service/
ADD target/roles-auths-soap-service.jar /opt/rova/roles-auths-soap-service/
ADD service.properties.template /opt/rova/roles-auths-soap-service/
ADD LICENSE /opt/rova/roles-auths-soap-service/license/LICENSE
ADD target/site /opt/rova/roles-auths-soap-service/license/dependency-report
WORKDIR /opt/rova/roles-auths-soap-service/

EXPOSE 8080
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "roles-auths-soap-service.jar"]
