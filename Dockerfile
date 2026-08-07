FROM bellsoft/liberica-openjdk-debian:17

RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

COPY russian_trusted_root_ca_pem.crt /usr/local/share/ca-certificates/
COPY russian_trusted_root_ca_gost_2025_pem.crt /usr/local/share/ca-certificates/
COPY russian_trusted_sub_ca_pem.crt /usr/local/share/ca-certificates/

RUN update-ca-certificates

ENV CACERTS=${JAVA_HOME}/lib/security/cacerts
ENV STOREPASS=changeit

RUN keytool -importcert -noprompt -trustcacerts \
    -alias mincifry-root \
    -file /usr/local/share/ca-certificates/russian_trusted_root_ca_pem.crt \
    -keystore "${CACERTS}" -storepass "${STOREPASS}"

RUN keytool -importcert -noprompt -trustcacerts \
    -alias mincifry-root-gost-2025 \
    -file /usr/local/share/ca-certificates/russian_trusted_root_ca_gost_2025_pem.crt \
    -keystore "${CACERTS}" -storepass "${STOREPASS}"

RUN keytool -importcert -noprompt -trustcacerts \
    -alias mincifry-sub \
    -file /usr/local/share/ca-certificates/russian_trusted_sub_ca_pem.crt \
    -keystore "${CACERTS}" -storepass "${STOREPASS}"

WORKDIR /app
RUN mkdir -p /app && chmod 777 /app
# ❗ Копируем JAR из target/
COPY target/*.jar /app/app.jar

CMD ["java", "-jar", "/app/app.jar"]
