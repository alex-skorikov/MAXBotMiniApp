FROM bellsoft/liberica-openjdk-debian:17

# Устанавливаем системные CA
RUN apt-get update && apt-get install -y ca-certificates && update-ca-certificates

# Копируем сертификаты Минцифры
COPY russian_trusted_root_ca_pem.crt /usr/local/share/ca-certificates/russian_trusted_root_ca_pem.crt
COPY russian_trusted_root_ca_gost_2025_pem.crt /usr/local/share/ca-certificates/russian_trusted_root_ca_gost_2025_pem.crt
COPY russian_trusted_sub_ca_pem.crt /usr/local/share/ca-certificates/russian_trusted_sub_ca_pem.crt

# Обновляем системный truststore Debian
RUN update-ca-certificates

# Добавляем сертификаты Минцифры в Java truststore
RUN keytool -importcert -noprompt \
    -alias russian_root \
    -file /usr/local/share/ca-certificates/russian_trusted_root_ca_pem.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit

RUN keytool -importcert -noprompt \
    -alias russian_root_gost \
    -file /usr/local/share/ca-certificates/russian_trusted_root_ca_gost_2025_pem.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit

RUN keytool -importcert -noprompt \
    -alias russian_sub_ca \
    -file /usr/local/share/ca-certificates/russian_trusted_sub_ca_pem.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit

# Копируем приложение
COPY target/app.jar /app/app.jar
WORKDIR /app

CMD ["java", "-jar", "app.jar"]
