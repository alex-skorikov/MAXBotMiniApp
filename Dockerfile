FROM bellsoft/liberica-openjdk-debian:17

RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

# Копируем сертификаты Минцифры
COPY russian_trusted_root_ca_pem.crt /usr/local/share/ca-certificates/
COPY russian_trusted_root_ca_gost_2025_pem.crt /usr/local/share/ca-certificates/
COPY russian_trusted_sub_ca_pem.crt /usr/local/share/ca-certificates/

# Обновляем системный truststore Debian
RUN update-ca-certificates

# Путь к Liberica JDK (точно найденный)
ENV JAVA_HOME=/usr/lib/jvm/jdk-17.0.19-bellsoft-x86_64
ENV CACERTS=${JAVA_HOME}/lib/security/cacerts
ENV STOREPASS=changeit

# Импорт сертификатов Минцифры в Java truststore
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

# Копируем JAR из корня проекта
COPY app.jar /app/app.jar
# Копируем файл с токенами внутрь контейнера
COPY token.env /app/token.env
WORKDIR /app

CMD ["java", "-jar", "app.jar"]
