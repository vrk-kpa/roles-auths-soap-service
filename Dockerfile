# Pull base image
FROM ubuntu@sha256:eae3725cb06909d017a06a813b9530006187fcce82cf45005b94a34c8cf43f8d

# Install common tools
RUN \
  apt-get update && \
  apt-get -y install python-software-properties && \
  apt-get -y install software-properties-common

# Install Java
RUN \
  echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
  add-apt-repository ppa:webupd8team/java && \
  apt-get update && \
  apt-get install -y oracle-java8-installer && \
  apt-get install -y ca-certificates && \
  apt-get install -y oracle-java8-unlimited-jce-policy && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk8-installer

# Define JAVA_HOME
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

RUN apt-get update && \
    apt-get install -yq --no-install-recommends wget pwgen ca-certificates && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Deploy project
RUN mkdir -p /opt/rova/roles-auths-soap-service/
ADD target/roles-auths-soap-service.jar /opt/rova/roles-auths-soap-service/
ADD service.properties.template /opt/rova/roles-auths-soap-service/
WORKDIR /opt/rova/roles-auths-soap-service/

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "roles-auths-soap-service.jar"]
