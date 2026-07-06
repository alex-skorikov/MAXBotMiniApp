FROM bellsoft/liberica-openjdk-debian:17

# Устанавливаем системные CA
RUN apt-get update && apt-get install -y ca-certificates && update-ca-certificates

# Копируем сертификаты Минцифры
COPY russian_trusted_root_ca_pem.crt /usr/local/share/ca-certificates/russian_trusted_root_ca_pem.crt
COPY russian_trusted_root_ca_gost_2025_pem.crt /usr/local/share/ca-certificates/russian_trusted_root_ca_gost_2025_pem.crt

# Обновляем системный truststore
RUN update-ca-certificates

# Копируем приложение
COPY target/app.jar /app/app.jar

WORKDIR /app

CMD ["java", "-jar", "app.jar"]


