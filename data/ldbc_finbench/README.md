# LDBC Finbench Data Setup

## How to

1. Download data to your data directory

```angular2html
download.sh $DATA_DIRECTORY
```
 You can update the download links in download.sh if needed.

2. Update .env file with the relevant scale factors
3. Finish data setup
```angular2html
docker-compose up -d
```

4. Turn down the docker setup
```angular2html
docker-compose down -d
```
