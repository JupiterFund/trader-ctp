# Copyright (c) HyperCloud Development Team.
# Distributed under the terms of the Modified BSD License.

# Global Arguments
ARG build_image=gradle:6.0.1-jdk8
ARG base_image=openjdk:8-jdk

FROM $build_image AS build

LABEL maintainer="Junxiang Wei <kevinprotoss.wei@gmail.com>"

COPY --chown=gradle:gradle . /home/gradle/workspace
WORKDIR /home/gradle/workspace
RUN gradle build copy bootJar --no-daemon

FROM $base_image

ARG trader_ctp_version=0.2.1

ENV LD_LIBRARY_PATH /app
ENV JASYPT_ENCRYPTOR_PASSWORD default

COPY --from=build /home/gradle/workspace/build/distributions/* /tmp/
RUN unzip /tmp/trader-ctp-boot-$trader_ctp_version.zip
RUN mkdir /app
RUN mv trader-ctp-boot-$trader_ctp_version/lib/trader-ctp-$trader_ctp_version.jar /app/trader-ctp.jar
RUN mv /tmp/*.so /app/

ENTRYPOINT ["java", "-jar", "/app/trader-ctp.jar"]
