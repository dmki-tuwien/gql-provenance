#!/bin/bash

cd ${HOME}

rm -rf /var/lib/neo4j/data/databases/neo4j
rm -rf /var/lib/neo4j/data/transactions/neo4j

/var/lib/neo4j/bin/neo4j-admin database import full \
    --nodes=Place="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/place_0_0.csv" \
    --nodes=Organisation="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/organisation_0_0.csv" \
    --nodes=TagClass="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/tagclass_0_0.csv" \
    --nodes=Tag="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/tag_0_0.csv" \
    --nodes=Comment:Message="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/comment_0_0.csv" \
    --nodes=Forum="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/forum_0_0.csv" \
    --nodes=Person="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_0_0.csv" \
    --nodes=Post:Message="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/post_0_0.csv" \
    --relationships=IS_PART_OF="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/place_isPartOf_place_0_0.csv" \
    --relationships=IS_SUBCLASS_OF="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/tagclass_isSubclassOf_tagclass_0_0.csv" \
    --relationships=IS_LOCATED_IN="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/organisation_isLocatedIn_place_0_0.csv" \
    --relationships=HAS_TYPE="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/static/tag_hasType_tagclass_0_0.csv" \
    --relationships=HAS_CREATOR="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/comment_hasCreator_person_0_0.csv" \
    --relationships=IS_LOCATED_IN="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/comment_isLocatedIn_place_0_0.csv" \
    --relationships=REPLY_OF="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/comment_replyOf_comment_0_0.csv" \
    --relationships=REPLY_OF="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/comment_replyOf_post_0_0.csv" \
    --relationships=CONTAINER_OF="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/forum_containerOf_post_0_0.csv" \
    --relationships=HAS_MEMBER="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/forum_hasMember_person_0_0.csv" \
    --relationships=HAS_MODERATOR="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/forum_hasModerator_person_0_0.csv" \
    --relationships=HAS_TAG="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/forum_hasTag_tag_0_0.csv" \
    --relationships=HAS_INTEREST="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_hasInterest_tag_0_0.csv" \
    --relationships=IS_LOCATED_IN="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_isLocatedIn_place_0_0.csv" \
    --relationships=KNOWS="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_knows_person_0_0.csv" \
    --relationships=LIKES="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_likes_comment_0_0.csv" \
    --relationships=LIKES="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_likes_post_0_0.csv" \
    --relationships=HAS_CREATOR="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/post_hasCreator_person_0_0.csv" \
    --relationships=HAS_TAG="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/comment_hasTag_tag_0_0.csv" \
    --relationships=HAS_TAG="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/post_hasTag_tag_0_0.csv" \
    --relationships=IS_LOCATED_IN="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/post_isLocatedIn_place_0_0.csv" \
    --relationships=STUDY_AT="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_studyAt_organisation_0_0.csv" \
    --relationships=WORK_AT="/import/${DATASET}/social_network-sf${SCALE_FACTOR}-CsvBasic-LongDateFormatter/converted/dynamic/person_workAt_organisation_0_0.csv" \
     --delimiter "|" neo4j --verbose

echo "Done importing ${DATASET} data"