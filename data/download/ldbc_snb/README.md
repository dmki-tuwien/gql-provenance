# LDBC SNB Data Setup
This contains the data setup pipeline for LDBC SNB-BI benchmark

## Download Data

1. Download data to your data directory. Modify `download.sh` to fetch only the required scale factors.

```angular2html
download.sh $DATA_DIRECTORY
```

3. Rename parameter files
```angular2html
./move_params.sh $PARAM_DIRECTORY
```