#!/bin/bash

chown -R --reference=/scripts /var/lib/neo4j/data
chmod -R --reference=/scripts /var/lib/neo4j/data

chown -R --reference=/scripts /var/lib/neo4j/plugins
chmod -R --reference=/scripts /var/lib/neo4j/plugins

if [ -d "/import" ]; then
  chown -R --reference=/scripts /import
  chmod -R --reference=/scripts /import
fi