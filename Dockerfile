# Pull base image
FROM roles-auths-docker-virtual.vrk-artifactory-01.eden.csc.fi/roles-auths-java-base@sha256:d3627ed406b05342b36ab74db0c8f59eef8a5eefda7fac48278896aa7cdecc68

# Deploy project
RUN mkdir -p /opt/rova/roles-auths-soap-service/
ADD target/roles-auths-soap-service.jar /opt/rova/roles-auths-soap-service/
ADD service.properties.template /opt/rova/roles-auths-soap-service/
ADD LICENSE /opt/rova/roles-auths-soap-service/license/LICENSE
ADD target/site /opt/rova/roles-auths-soap-service/license/dependency-report
WORKDIR /opt/rova/roles-auths-soap-service/

EXPOSE 8080
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "roles-auths-soap-service.jar"]
