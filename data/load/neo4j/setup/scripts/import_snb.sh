#!/bin/bash

cd ${HOME}

rm -rf /var/lib/neo4j/data/databases/neo4j
rm -rf /var/lib/neo4j/data/transactions/neo4j


NEO4J_PART_FIND_PATTERN="part-*.csv*"
NEO4J_HEADER_EXTENSION=".csv"

## should place headers in the scripts folder https://github.com/ldbc/ldbc_snb_bi/tree/main/neo4j/headers
## delete all headers from data files. run inside initial_snapshpt folder. find . -type f -name 'part-*.csv' -exec sh -c 'tail -n +2 "$0" > "$0.tmp" && mv "$0.tmp" "$0"' {} \;

#/var/lib/neo4j/bin/neo4j-admin database import full \
#    --nodes=Place="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/place_0_0.csv" \
#    --nodes=Organisation="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/organisation_0_0.csv" \
#    --nodes=TagClass="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/tagclass_0_0.csv" \
#    --nodes=Tag="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/tag_0_0.csv" \
#    --nodes=Comment:Message="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/comment_0_0.csv" \
#    --nodes=Forum="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/forum_0_0.csv" \
#    --nodes=Person="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_0_0.csv" \
#    --nodes=Post:Message="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/post_0_0.csv" \
#    --relationships=IS_PART_OF="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/place_isPartOf_place_0_0.csv" \
#    --relationships=IS_SUBCLASS_OF="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/tagclass_isSubclassOf_tagclass_0_0.csv" \
#    --relationships=IS_LOCATED_IN="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/organisation_isLocatedIn_place_0_0.csv" \
#    --relationships=HAS_TYPE="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/tag_hasType_tagclass_0_0.csv" \
#    --relationships=HAS_CREATOR="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/comment_hasCreator_person_0_0.csv" \
#    --relationships=IS_LOCATED_IN="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/comment_isLocatedIn_place_0_0.csv" \
#    --relationships=REPLY_OF="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/comment_replyOf_comment_0_0.csv" \
#    --relationships=REPLY_OF="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/comment_replyOf_post_0_0.csv" \
#    --relationships=CONTAINER_OF="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/forum_containerOf_post_0_0.csv" \
#    --relationships=HAS_MEMBER="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/forum_hasMember_person_0_0.csv" \
#    --relationships=HAS_MODERATOR="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/forum_hasModerator_person_0_0.csv" \
#    --relationships=HAS_TAG="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/forum_hasTag_tag_0_0.csv" \
#    --relationships=HAS_INTEREST="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_hasInterest_tag_0_0.csv" \
#    --relationships=IS_LOCATED_IN="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_isLocatedIn_place_0_0.csv" \
#    --relationships=KNOWS="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_knows_person_0_0.csv" \
#    --relationships=LIKES="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_likes_comment_0_0.csv" \
#    --relationships=LIKES="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_likes_post_0_0.csv" \
#    --relationships=HAS_CREATOR="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/post_hasCreator_person_0_0.csv" \
#    --relationships=HAS_TAG="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/comment_hasTag_tag_0_0.csv" \
#    --relationships=HAS_TAG="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/post_hasTag_tag_0_0.csv" \
#    --relationships=IS_LOCATED_IN="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/post_isLocatedIn_place_0_0.csv" \
#    --relationships=STUDY_AT="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_studyAt_organisation_0_0.csv" \
#    --relationships=WORK_AT="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_workAt_organisation_0_0.csv" \
#     --delimiter "|" neo4j --verbose

/var/lib/neo4j/bin/neo4j-admin database import full \
    --nodes=Place="/scripts/headers/static/Place${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/static/Place/part-([A-Za-z0-9-]+).csv" \
    --nodes=Organisation="/scripts/headers/static/Organisation${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/static/Organisation/part-([A-Za-z0-9-]+).csv" \
    --nodes=TagClass="/scripts/headers/static/TagClass${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/static/TagClass/part-([A-Za-z0-9-]+).csv" \
    --nodes=Tag="/scripts/headers/static/Tag${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/static/Tag/part-([A-Za-z0-9-]+).csv" \
    --nodes=Forum="/scripts/headers/dynamic/Forum${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Forum/part-([A-Za-z0-9-]+).csv" \
    --nodes=Person="/scripts/headers/dynamic/Person${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Person/part-([A-Za-z0-9-]+).csv" \
    --nodes=Message:Comment="/scripts/headers/dynamic/Comment${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Comment/part-([A-Za-z0-9-]+).csv" \
    --nodes=Message:Post="/scripts/headers/dynamic/Post${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Post/part-([A-Za-z0-9-]+).csv" \
    --relationships=IS_PART_OF="/scripts/headers/static/Place_isPartOf_Place${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/static/Place_isPartOf_Place/part-([A-Za-z0-9-]+).csv" \
    --relationships=IS_SUBCLASS_OF="/scripts/headers/static/TagClass_isSubclassOf_TagClass${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/static/TagClass_isSubclassOf_TagClass/part-([A-Za-z0-9-]+).csv" \
    --relationships=IS_LOCATED_IN="/scripts/headers/static/Organisation_isLocatedIn_Place${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/static/Organisation_isLocatedIn_Place/part-([A-Za-z0-9-]+).csv" \
    --relationships=HAS_TYPE="/scripts/headers/static/Tag_hasType_TagClass${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/static/Tag_hasType_TagClass/part-([A-Za-z0-9-]+).csv" \
    --relationships=HAS_CREATOR="/scripts/headers/dynamic/Comment_hasCreator_Person${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Comment_hasCreator_Person/part-([A-Za-z0-9-]+).csv" \
    --relationships=IS_LOCATED_IN="/scripts/headers/dynamic/Comment_isLocatedIn_Country${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Comment_isLocatedIn_Country/part-([A-Za-z0-9-]+).csv" \
    --relationships=REPLY_OF="/scripts/headers/dynamic/Comment_replyOf_Comment${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Comment_replyOf_Comment/part-([A-Za-z0-9-]+).csv" \
    --relationships=REPLY_OF="/scripts/headers/dynamic/Comment_replyOf_Post${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Comment_replyOf_Post/part-([A-Za-z0-9-]+).csv" \
    --relationships=CONTAINER_OF="/scripts/headers/dynamic/Forum_containerOf_Post${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Forum_containerOf_Post/part-([A-Za-z0-9-]+).csv" \
    --relationships=HAS_MEMBER="/scripts/headers/dynamic/Forum_hasMember_Person${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Forum_hasMember_Person/part-([A-Za-z0-9-]+).csv" \
    --relationships=HAS_MODERATOR="/scripts/headers/dynamic/Forum_hasModerator_Person${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Forum_hasModerator_Person/part-([A-Za-z0-9-]+).csv" \
    --relationships=HAS_TAG="/scripts/headers/dynamic/Forum_hasTag_Tag${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Forum_hasTag_Tag/part-([A-Za-z0-9-]+).csv" \
    --relationships=HAS_INTEREST="/scripts/headers/dynamic/Person_hasInterest_Tag${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Person_hasInterest_Tag/part-([A-Za-z0-9-]+).csv" \
    --relationships=IS_LOCATED_IN="/scripts/headers/dynamic/Person_isLocatedIn_City${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Person_isLocatedIn_City/part-([A-Za-z0-9-]+).csv" \
    --relationships=KNOWS="/scripts/headers/dynamic/Person_knows_Person${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Person_knows_Person/part-([A-Za-z0-9-]+).csv" \
    --relationships=LIKES="/scripts/headers/dynamic/Person_likes_Comment${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Person_likes_Comment/part-([A-Za-z0-9-]+).csv" \
    --relationships=LIKES="/scripts/headers/dynamic/Person_likes_Post${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Person_likes_Post/part-([A-Za-z0-9-]+).csv" \
    --relationships=HAS_CREATOR="/scripts/headers/dynamic/Post_hasCreator_Person${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Post_hasCreator_Person/part-([A-Za-z0-9-]+).csv" \
    --relationships=HAS_TAG="/scripts/headers/dynamic/Comment_hasTag_Tag${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Comment_hasTag_Tag/part-([A-Za-z0-9-]+).csv" \
    --relationships=HAS_TAG="/scripts/headers/dynamic/Post_hasTag_Tag${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Post_hasTag_Tag/part-([A-Za-z0-9-]+).csv" \
    --relationships=IS_LOCATED_IN="/scripts/headers/dynamic/Post_isLocatedIn_Country${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Post_isLocatedIn_Country/part-([A-Za-z0-9-]+).csv" \
    --relationships=STUDY_AT="/scripts/headers/dynamic/Person_studyAt_University${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Person_studyAt_University/part-([A-Za-z0-9-]+).csv" \
    --relationships=WORK_AT="/scripts/headers/dynamic/Person_workAt_Company${NEO4J_HEADER_EXTENSION},/import/${DATASET}/bi-sf${SCALE_FACTOR}-composite-projected-fk/graphs/csv/bi/composite-projected-fk/initial_snapshot/dynamic/Person_workAt_Company/part-([A-Za-z0-9-]+).csv" \
    --delimiter '|' neo4j --verbose --auto-skip-subsequent-headers=true

echo "Done importing ${DATASET} data"