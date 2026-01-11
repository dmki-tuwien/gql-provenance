package org.pgprov.neo4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONException;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.internal.InternalPath;
import org.neo4j.driver.internal.InternalRelationship;
import org.neo4j.driver.types.Path;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class TestUtil {

    private Object convertNeoValue(Object value) {

        if (value instanceof InternalNode node) {
            Map<String,Object> nodeMap = new HashMap<>();
            nodeMap.put("labels", node.labels());
            nodeMap.put("props", node.asMap());
            return nodeMap;
        } else if (value instanceof InternalRelationship rel) {
            Map<String,Object> relMap = new HashMap<>();
            relMap.put("type", rel.type());
            relMap.put("props", rel.asMap());
            return relMap;
        } else if (value instanceof InternalPath path) {
            Set<Object> pathList = new HashSet<>();
            for (Path.Segment segment : path) {
                pathList.add(convertNeoValue(segment.start()));
                pathList.add(convertNeoValue(segment.relationship()));
                pathList.add(convertNeoValue(segment.end()));
            }
            return pathList;
        } else if (value instanceof List<?> list) {
            return list.stream().map(this::convertNeoValue).toList();
        }  else if (value instanceof Map<?,?> map) {
            Map<String,Object> mapMap = new HashMap<>();
            for(Map.Entry<?,?> entry : map.entrySet()) {
                mapMap.put((String) entry.getKey(), convertNeoValue(entry.getValue()));
            }
            return mapMap;
        } else {
            return value; // primitives like String, Integer, etc.
        }
    }

    public void runTest(Driver driver, String testCase, String provModel ) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<String> provModels = List.of("why","where","how");
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("provenance.json")) {

            JsonNode root = mapper.readTree(is);

            JsonNode test = root.get(testCase);
            String query = test.get("query").asText();
            JsonNode testValue = test.get("test-value");

            testValue.forEach(map -> {
                ObjectNode obj = (ObjectNode) map;

                for(String model : provModels) {
                    if(!model.equals(provModel.toLowerCase())){
                        obj.remove( model+"-prov");
                    }
                }

                JsonNode p = obj.remove(provModel.toLowerCase()+"-prov");
                obj.set("prov", p);
            });

            try (Session session = driver.session()) {

                Result record = session.run("CALL org.pgprov.get"+provModel+"Provenance(\""+query+"\" , {})");

                ArrayNode actualSet = mapper.createArrayNode();

                while (record.hasNext()) {
                    Record rec = record.next();
                    Map<String,Object> recordMap = rec.asMap().entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> convertNeoValue(e.getValue())
                            ));

                    actualSet.add(mapper.valueToTree(recordMap));
                }

                System.out.println(testValue);
                System.out.println(actualSet);

                JSONAssert.assertEquals(actualSet.toString(), testValue.toString(), JSONCompareMode.LENIENT);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
