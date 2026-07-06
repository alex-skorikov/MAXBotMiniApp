FROM bellsoft/liberica-openjdk-debian:17

RUN apt-get update && apt-get install -y ca-certificates && update-ca-certificates

# Корневые сертификаты Минцифры
COPY russian_trusted_root_ca_pem.crt /usr/local/share/ca-certificates/russian_trusted_root_ca_pem.crt
COPY russian_trusted_root_ca_gost_2025_pem.crt /usr/local/share/ca-certificates/russian_trusted_root_ca_gost_2025_pem.crt

# Промежуточный сертификат Минцифры (ОБЯЗАТЕЛЬНО)
COPY russian_trusted_sub_ca_pem.crt /usr/local/share/ca-certificates/russian_trusted_sub_ca_pem.crt

RUN update-ca-certificates

COPY target/app.jar /app/app.jar
WORKDIR /app

CMD ["java", "-jar", "app.jar"]
