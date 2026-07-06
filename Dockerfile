FROM bellsoft/liberica-openjdk-debian:17

# Устанавливаем системные CA
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

# Копируем сертификаты Минцифры
COPY russian_trusted_root_ca_pem.crt /usr/local/share/ca-certificates/russian_trusted_root_ca_pem.crt
COPY russian_trusted_root_ca_gost_2025_pem.crt /usr/local/share/ca-certificates/russian_trusted_root_ca_gost_2025_pem.crt
COPY russian_trusted_sub_ca_pem.crt /usr/local/share/ca-certificates/russian_trusted_sub_ca_pem.crt

# Обновляем системный truststore Debian
RUN update-ca-certificates

# Добавляем сертификаты Минцифры в Java truststore
ENV JAVA_HOME=/usr/lib/jvm/bellsoft-java17-amd64
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

# Копируем приложение
COPY target/app.jar /app/app.jar
WORKDIR /app

CMD ["java", "-jar", "app.jar"]
