FROM openjdk:16

ARG jar

RUN mkdir /usr/app
RUN mkdir /usr/app/mounted

COPY ./build/libs/${jar} /usr/app
COPY mgfsm.jar /usr/app

ARG jar_path=/usr/app/${jar}

ENV jar_path_env=$jar_path



#ENTRYPOINT ["java", "-jar", "$jar_path_env"]
ENTRYPOINT ["sh", "-c", "java -jar $jar_path_env $@"]
