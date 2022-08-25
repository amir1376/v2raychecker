
FROM openjdk:11 AS build
ENV APP_HOME=/usr/src/

WORKDIR $APP_HOME
COPY gradlew ./
COPY gradle ./gradle
RUN ./gradlew
COPY build.gradle.kts gradle.properties settings.gradle.kts ./
RUN ./gradlew build
COPY . .
RUN ./gradlew installDist

FROM openjdk:11

#install v2ray
WORKDIR /root
COPY v2ray.sh /root/v2ray.sh
RUN apt update
RUN set -ex \
	&& apt install -y tzdata openssl ca-certificates \
	&& mkdir -p /etc/v2ray /usr/local/share/v2ray /var/log/v2ray \
	&& chmod +x /root/v2ray.sh \
	&& /root/v2ray.sh "linux/amd64" "v4.45.2"

# copy java compiled files
ENV APP_HOME=/usr/src
WORKDIR $APP_HOME
COPY --from=build $APP_HOME/build/install/V2RayTester .
WORKDIR "/app/"
ENTRYPOINT ["/usr/src/bin/V2RayTester"]
