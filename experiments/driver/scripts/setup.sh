#!/bin/bash

mkdir -p /app/result/metrics

chown -R --reference=/app/scripts /app/result
chmod -R --reference=/app/scripts /app/result