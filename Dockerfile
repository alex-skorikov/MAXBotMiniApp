FROM bellsoft/liberica-openjdk-debian:17

# Обновляем пакеты и устанавливаем ca-certificates для Debian
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*

# Копируем сертификаты Минцифры в системную директорию Debian
COPY russian_trusted_root_ca_pem.crt /usr/local/share/ca-certificates/
COPY russian_trusted_root_ca_gost_2025_pem.crt /usr/local/share/ca-certificates/
COPY russian_trusted_sub_ca_pem.crt /usr/local/share/ca-certificates/

# Обновляем системный truststore ОС
RUN update-ca-certificates

# Автоматически определяем путь к Java и задаем переменные окружения
ENV JAVA_HOME=/usr/lib/jvm/bellsoft-java17-amd64
ENV CACERTS=${JAVA_HOME}/lib/security/cacerts
ENV STOREPASS=changeit

# Импорт сертификатов Минцифры в Java truststore (cacerts)
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

# Настройка рабочей директории и копирование приложения
WORKDIR /app
COPY app.jar /app/app.jar
COPY token.env /app/token.env

CMD ["java", "-jar", "app.jar"]
