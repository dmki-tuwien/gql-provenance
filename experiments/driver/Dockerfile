FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY ./target/pgprov_test_driver-1.0.jar app.jar
RUN apk add --no-cache bash

ENTRYPOINT ["/bin/bash", "-c"]
CMD ["tail -f /dev/null"]