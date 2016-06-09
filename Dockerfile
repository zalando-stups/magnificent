FROM registry.opensource.zalan.do/stups/openjdk:8-26

MAINTAINER Zalando SE

COPY target/magnificent.jar /

CMD java $JAVA_OPTS $(java-dynamic-memory-opts 60) $(appdynamics-agent) -jar /magnificent.jar

ADD target/scm-source.json /scm-source.json
