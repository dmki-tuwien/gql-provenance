# LDBC Finbench Data Setup

This contains the data setup pipeline for LDBC Finbench benchmark

## Download data

Download data to your data directory. Modify `download.sh` to fetch only the required scale factors.

```angular2html
download.sh $DATA_DIRECTORY
```

## Post process
For each scale factor,
1. Update .env file with the scale factor, data directory and whether the parameters need to be generated or not. 
2. Start the post process
```angular2html
docker compose up -d
```
3. Turn down the docker setup
```angular2html
docker compose down -d
```
