#!/usr/bin/env bash
#
#CALL apoc.export.csv.query(
#  "CALL(){
#MATCH (m:Message) RETURN m.id AS messageId
#UNION
#MATCH (i:Message)-[:REPLY_OF]->(:Message)
#RETURN i.id AS messageId
#UNION
#MATCH (:Message)-[:REPLY_OF]->(j:Message)
#RETURN j.id AS messageId
#}
#RETURN messageId
#ORDER BY rand()
#LIMIT 300", "/import/snb/sf_0.1/substitution_parameters-sf0.1/interactive_15_param.txt",
#  {}
#);

set -euo pipefail

param_folder=$1

# mapping: target file -> source file
declare -A FILE_MAP=(


  [1]=bi-1
  [2]=bi-2a
  [3]=bi-3
  [4]=bi-4
  [5]=bi-5
  [6]=bi-6
  [7]=bi-7
  [8]=bi-8a
  [9]=bi-9
  [10]=bi-10a
  [11]=bi-11
  [12]=bi-12
  [13]=bi-13
  [14]=bi-14a
  [15]=bi-16a
  [16]=bi-17
  [17]=bi-18
  [18]=bi-19a
  [19]=bi-20a
  [20]=bi-10a

#  [1]=bi_1
#  [2]=bi_10
#  [3]=bi_4
#  [4]=bi_10
#  [5]=bi_6
#  [6]=bi_6
#  [7]=bi_7
#  [8]=bi_8
#  [9]=bi_14
#  [10]=bi_16
#  [11]=bi_11
#  [12]=bi_12
#  [13]=bi_13
#  [14]=bi_14
#  [15]=bi_16
#  [16]=bi_17
#  [17]=bi_18
#  [18]=bi_19
#  [19]=company
#  [20]=interactive_12

#  [1]=1
#  [2]=2
#  [3]=3
#  [4]=4
#  [5]=5
#  [6]=6
#  [7]=7
#  [8]=8
#  [9]=9
#  [10]=10
#  [11]=11
#  [12]=12
#  [13]=1
#  [14]=8
#  [15]=11
#  [16]=15
#  [17]=15
#  [18]=15
#  [19]=15
#  [20]=6
#
#    [1]=1
#    [2]=1
#    [3]=2
#    [4]=5
#    [5]=8
#    [6]=12
#    [7]=3
#    [8]=6
#    [9]=8
#    [10]=11
)

for file in "${!FILE_MAP[@]}"; do
  dst="params_${file}.csv"
  dstfilepath="$param_folder/$dst"

  srcfilepath="$param_folder/${FILE_MAP[$file]}.csv"

  if [[ ! -f "$srcfilepath" ]]; then
    echo "Source file missing: $srcfilepath"
    continue
  fi

  echo "Copying $srcfilepath -> $dstfilepath"
  cp "$srcfilepath" "$dstfilepath"
done

