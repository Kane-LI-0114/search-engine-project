ARG BASE_IMAGE=tomcat:9.0-jdk17-openjdk
FROM ${BASE_IMAGE}

# Copy WAR
COPY target/search-engine.war /usr/local/tomcat/webapps/ROOT.war

# Copy already prepared db file
COPY searchengine_db.* /usr/local/tomcat/

WORKDIR /usr/local/tomcat

EXPOSE 8080

CMD ["catalina.sh", "run"]