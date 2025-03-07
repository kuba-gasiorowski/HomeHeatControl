FROM amd64/debian
LABEL authors="kuba.gasiorowski"

RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    nodejs \
    npm \
    wget \
    curl

RUN apt-get install -y python3-yaml

RUN wget https://download.java.net/openjdk/jdk11/ri/openjdk-11+28_linux-x64_bin.tar.gz
RUN tar -xvf openjdk-11+28_linux-x64_bin.tar.gz && mv jdk-11 /opt/

ENV JAVA_HOME=/opt/jdk-11
ENV PATH=$JAVA_HOME/bin:$PATH