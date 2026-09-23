#!/bin/bash

chown -R --reference=/scripts /var/lib/neo4j/data
chmod -R --reference=/scripts /var/lib/neo4j/data

chown -R --reference=/scripts /var/lib/neo4j/plugins
chmod -R --reference=/scripts /var/lib/neo4j/plugins

chown -R --reference=/scripts /var/lib/neo4j/logs
chmod -R --reference=/scripts /var/lib/neo4j/logs

chown -R --reference=/scripts /var/lib/neo4j/conf
chmod -R --reference=/scripts /var/lib/neo4j/conf

if [ -d "/import" ]; then
  chown -R --reference=/scripts /import
  chmod -R --reference=/scripts /import
fi