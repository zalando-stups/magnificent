FROM registry.opensource.zalan.do/stups/openjdk:latest

MAINTAINER Zalando SE

COPY target/magnificent.jar /

CMD java $JAVA_OPTS $(java-dynamic-memory-opts 60) $(appdynamics-agent) -jar /magnificent.jar
