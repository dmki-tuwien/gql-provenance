# Experiment Setup for Neo4j

## Import Data

1. Update the import.env file
2. Make sure an import script is there in the `setup/scripts` folder by `import_<dataset-name>.sh`
2. Import data (This also copies db files to a another folder in case you want to easily switch dbs)
```angular2html
./setup.sh <dataset-name>
```