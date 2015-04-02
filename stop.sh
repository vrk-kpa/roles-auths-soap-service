#!/bin/bash
basepath=$1
cd $basepath
export CATALINA_BASE=/data00/services/kapa-rova-api
export CATALINA_HOME=/devtools/apache-tomcat-8.0.18
/devtools/apache-tomcat-8.0.18/bin/catalina.sh stop
