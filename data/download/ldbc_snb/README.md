# LDBC SNB Data Setup

## How to

1. Download data to your data directory

```angular2html
download.sh $DATA_DIRECTORY
```
 You can update the download links in download.sh if needed.

2. Update the headers before importing for neo4j
```angular2html
./update_headers.sh $DATA_DIRECTORY $SCALE_FACTOR
```
