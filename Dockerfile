FROM bellsoft/liberica-openjdk-debian:17

RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

COPY russian_trusted_root_ca_pem.crt /usr/local/share/ca-certificates/
COPY russian_trusted_root_ca_gost_2025_pem.crt /usr/local/share/ca-certificates/
COPY russian_trusted_sub_ca_pem.crt /usr/local/share/ca-certificates/

RUN update-ca-certificates

ENV JAVA_HOME=/usr/lib/jvm/bellsoft-java17
ENV CACERTS=${JAVA_HOME}/lib/security/cacerts
ENV STOREPASS=changeit

RUN keytool -importcert -noprompt -trustcacerts \
    -alias mincifry-root \
    -file /usr/local/share/ca-certificates/russian_trusted_root_ca_pem.crt \
    -keystore "${CACERTS}" -storepass "${STOREPASS}" && \
    keytool -importcert -noprompt -trustcacerts \
    -alias mincifry-root-gost-2025 \
    -file /usr/local/share/ca-certificates/russian_trusted_root_ca_gost_2025_pem.crt \
    -keystore "${CACERTS}" -storepass "${STOREPASS}" && \
    keytool -importcert -noprompt -trustcacerts \
    -alias mincifry-sub \
    -file /usr/local/share/ca-certificates/russian_trusted_sub_ca_pem.crt \
    -keystore "${CACERTS}" -storepass "${STOREPASS}"

COPY target/maxbot-miniapp.jar /app/app.jar
WORKDIR /app

CMD ["java", "-jar", "app.jar"]
