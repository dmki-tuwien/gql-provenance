# Datasets

In this repository, we use the LDBC Finbench and LDBC SNB-BI benchmark datasets.

## Data
| **Data**                                                                                                                  | 0.3 | 1  | 3  | 10 |
|---------------------------------------------------------------------------------------------------------------------------|-----|----|----|----|
| [LDBC Finbench](https://ldbcouncil.org/benchmarks/finbench/datasets/)                                                     | ✓   | ✓  | ✓  | ✓  | 
| [LDBC SNB-BI](https://ldbcouncil.org/benchmarks/snb/datasets/) <br>&nbsp;&nbsp;&nbsp;(Compressed CSVs in the composite-projected-fk format) | -   | ✓  | ✓  | ✓  |

## Parameters

| **Parameters**                                                                           | 0.3 | 1 | 3 | 10 |
|------------------------------------------------------------------------------------------|----|---|--|---|
| **LDBC Finbench**                                                                        |    |   |  |   | 
| &nbsp;&nbsp;&nbsp;Published in [official site](https://ldbcouncil.org/benchmarks/finbench/datasets/)       | -  | ✓ | - | ✓ |
| &nbsp;&nbsp;&nbsp;Generated using [official data generator](https://github.com/ldbc/ldbc_finbench_datagen) |✓  | - |✓ | - |
| **LDBC SNB-BI**                                                                          |    |   |  |   |
| &nbsp;&nbsp;&nbsp;Published in [official site](https://repository.surfsara.nl/datasets/cwi/ldbc-snb-bi#files)       |-   | ✓ | ✓ | ✓ |

# Setup

## Download Data
Download the load-ready data and query parameters published [here](https://zenodo.org/records/18865136).

## Load Data
The [**load**](./load) directory contains scripts for loading the processed CSV datasets into graph databases.
Currently, we support,
* [Neo4j](./load/neo4j/README.md)

# Data Preprocessing

The published data were prepared using the following preprocessing pipeline.

The [**download**](./download) directory contains benchmark-specific scripts to:
* download the datasets
* update CSV headers when required
* generate query parameters for evaluation

Detailed instructions are available in
* [LDBC Finbench ReadMe](./download/ldbc_finbench/README.md), and
* [LDBC SNB-BI ReadMe](./download/ldbc_snb/README.md).

