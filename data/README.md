# General

This module contains data processing pipelines for the LDBC Finbench and SNB-BI benchmarks, organized as follows:
* [**download**](./download): Handles dataset retrieval, updates CSV file headers if necessary, and generates required parameters for evaluation.
* [**load**](./load): Loads CSV data into a Neo4j database.


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

