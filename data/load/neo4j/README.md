# Experiment Setup for Neo4j

## Import Data

1. Update the [import.env](./env/import.env) file
2. Make sure an import script is there in the `setup/scripts` folder by `import_$DATASET_NAME.sh`
2. Import data (This also copies db files to another folder so that the data for scale factors can be switched)
```angular2html
./setup.sh $DATASET_NAME
```