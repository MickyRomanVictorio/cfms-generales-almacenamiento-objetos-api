FROM openjdk:17-alpine
LABEL maintainer=dacunac@email.mpfn.gob.pe

ADD /build/libs/cfms-generales-almacenamiento-objetos-api-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Xms512m","-Xmx1g","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
