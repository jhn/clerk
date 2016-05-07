FROM java:8

RUN apt-get update

ADD target/clerk-0.1.0-SNAPSHOT-standalone.jar /srv/clerk.jar

CMD ["java", "-jar", "/srv/clerk.jar"]

