FROM registry.opensource.zalan.do/stups/openjdk:8u91-b14-1-22

MAINTAINER Zalando SE

COPY target/magnificent.jar /

CMD java $JAVA_OPTS $(java-dynamic-memory-opts) $(appdynamics-agent) -jar /magnificent.jar

ADD target/scm-source.json /scm-source.json
