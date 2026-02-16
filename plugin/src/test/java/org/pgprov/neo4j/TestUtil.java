package org.pgprov.neo4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.json.JSONException;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.internal.InternalPath;
import org.neo4j.driver.internal.InternalRelationship;
import org.neo4j.driver.types.Entity;
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

                Result all = session.run("MATCH (n) RETURN elementId(n) AS id, n AS ann, \"node\" AS kind " +
                        "UNION ALL " +
                        "MATCH ()-[r]->() RETURN elementId(r) AS id, r AS ann, \"relationship\" AS kind");

                while (all.hasNext()) {
                    Record rec = all.next();

                    String elementId = rec.get("id").asString();
                    Map<String, String> recordMap =
                            ((Entity)(
                                    rec.get("kind").asString().equals("node")
                                            ? rec.get("ann").asNode()
                                            : rec.get("ann").asRelationship()
                            ))
                                    .asMap()
                                    .entrySet()
                                    .stream()
                                    .filter(e -> e.getKey().startsWith("__"))
                                    .collect(Collectors.toMap(
                                            e -> e.getValue().toString(),                  // v
                                            e -> {
                                                String key = e.getKey();

                                                if (key.equals("__n") || key.equals("__e")) {
                                                    return elementId;
                                                }
                                                if (key.startsWith("__l_")) {
                                                    return elementId + ":" + key.substring(4);
                                                }
                                                if (key.startsWith("__k_")) {
                                                    return elementId + "." + key.substring(4);
                                                }

                                                throw new IllegalStateException("Unknown __ property: " + key);
                                            }          // elementId.x
                                    ));


                    testValue.forEach(map -> {
                        if(provModel.equals("Why")){
                            JsonNode provenance = map.get("prov");
                            ArrayNode provArray = (ArrayNode) provenance;

                            for (int i = 0; i < provArray.size(); i++) {
                                JsonNode inner = provArray.get(i);

                                if (inner.isArray()) {
                                    ArrayNode innerArray = (ArrayNode) inner;

                                    for (int j = 0; j < innerArray.size(); j++) {
                                        String value = innerArray.get(j).asText();

                                        if (recordMap.containsKey(value)) {
                                            innerArray.set(j, new TextNode(recordMap.get(value)));
                                        }
                                    }
                                }
                            }

                        } else if(provModel.equals("Where")){
                            JsonNode provenance = map.get("prov");

                            if (provenance != null && provenance.isObject()) {
                                ObjectNode provObject = (ObjectNode) provenance;

                                provObject.fields().forEachRemaining(entry -> {
                                    JsonNode valueNode = entry.getValue();

                                    if (valueNode.isArray()) {
                                        ArrayNode array = (ArrayNode) valueNode;

                                        for (int i = 0; i < array.size(); i++) {

                                            if (array.get(i).isArray()) {
                                                ArrayNode innerArray = (ArrayNode) array.get(i);

                                                for (int k = 0; k < innerArray.size(); k++) {
                                                    String value = innerArray.get(k).asText();

                                                    if (recordMap.containsKey(value)) {
                                                        innerArray.set(k, new TextNode(recordMap.get(value)));
                                                    }

                                                }

                                            }else {
                                                String value = array.get(i).asText();

                                                if (recordMap.containsKey(value)) {
                                                    array.set(i, new TextNode(recordMap.get(value)));
                                                }
                                            }
                                        }
                                    }
                                });
                            }


                        }

                    });
                }



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
