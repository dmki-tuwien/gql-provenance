# Finbench-Datagen Docker Image

# Use an official OpenJDK 8 image as the base
FROM eclipse-temurin:8-jdk

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive
ENV SCALA_VERSION=2.12.15
ENV SPARK_VERSION=3.2.4
ENV HADOOP_VERSION=3.2

# Install system dependencies
RUN apt-get update && \
    apt-get install -y \
    curl \
    git \
    python3 \
    python3-venv \
    python3-pip \
    python3-setuptools \
    maven \
    scala \
    && rm -rf /var/lib/apt/lists/*

ENV VIRTUAL_ENV=/opt/venv
RUN python3 -m venv $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

# Install Python dependencies
RUN pip3 install --no-cache-dir --upgrade pip setuptools wheel && \
    pip3 install --no-cache-dir numpy pandas pyspark==3.2.4 matplotlib

# Clone the FinBench DataGen repository
RUN git clone https://github.com/ldbc/ldbc_finbench_datagen.git /home/user/ldbc_finbench_datagen

#WORKDIR /home/user/ldbc_finbench_datagen
#RUN chmod +x ./scripts/get-spark-to-home.sh
#
#COPY scripts/run_local.sh ./scripts/run_local.sh
#RUN chmod +x ./scripts/run_local.sh

## Deploy Spark
#RUN ./scripts/get-spark-to-home.sh

# Set environment variables
#ENV SPARK_HOME=/root/spark-3.2.2-bin-hadoop3.2
#ENV PATH=$SPARK_HOME/bin:$PATH

# Build the project using Maven
#RUN mvn clean package -DskipTests

# Set the working directory
WORKDIR /workspace

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

CMD ["/entrypoint.sh"]