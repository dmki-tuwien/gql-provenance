package org.pgprov.driver;

import org.pgprov.driver.db.DbDriver;
import org.pgprov.driver.db.DbDriverFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 * Hello world!
 *
 */
public class App 
{

    public static Properties appSettings;
    public static List<String> provenanceModels = List.of("Why", "Where","How");

    private static void runExperiments(DbDriver driver, Map<String, String> finalQueries, Map<String, List<Map<String, Object>>> map, String provModel){

        Random rng = new Random();
        Map<String, Double> queryRunTimes = new HashMap<>();
        String latencyFilePath = appSettings.getProperty("output_folder")+"_latency.csv";

        int execCount = Integer.parseInt(appSettings.getProperty("test_query_count"));
        // loop the query set an execCount times
        for (int i = 0; i <execCount; i++) {

            List<Map.Entry<String, String>> queryList = new ArrayList<>(finalQueries.entrySet());
            Collections.shuffle(queryList); // randomize query execution order

            for (Map.Entry<String, String> query : queryList) {

                String paramKey;

                if(query.getKey().contains("prov_") || query.getKey().contains("orig_")){
                    paramKey = (query.getKey().split("_"))[1];
                }else{

                    // if warm up
                    paramKey = query.getKey();
                }

                List<Map<String, Object>> params = map.get(paramKey);
                int randomNum = rng.nextInt(0, params.size());

                // query execution
                double durationMs = driver.runQuery(query.getValue(), query.getKey(), params.get(randomNum));

                // Cache execTimes
                queryRunTimes.merge(query.getKey(), durationMs, Double::sum);

            }
        }

        // Write execTimes to file
        File latencyFile = new File(latencyFilePath);
        boolean latencyFieExists = latencyFile.exists();
        try (FileWriter latencyWriter = new FileWriter(latencyFilePath,true)) {

            if(!latencyFieExists) {
                latencyWriter.write("dataset,scaleFactor,provModel,query,mean\n");
            }

            for (Map.Entry<String, Double> entry : queryRunTimes.entrySet()) {
                latencyWriter.write(appSettings.getProperty("dataset") + ","
                        + appSettings.getProperty("scale_factor") + ","
                        + provModel + ","
                        + entry.getKey() + ","
                        + (entry.getValue() / execCount) + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static void warmUpCache(DbDriver driver, Map<String, String> tsrQueries, Map<String, List<Map<String, Object>>> map){

        //Warm up cache
        int execCount = Integer.parseInt(appSettings.getProperty("warm_up_query_count"));

        Random rng = new Random();

        // loop the query set an execCount times
        for (int i = 0; i <execCount; i++) {

            List<Map.Entry<String, String>> queryList = new ArrayList<>(tsrQueries.entrySet());
            Collections.shuffle(queryList); // randomize query execution order

            for (Map.Entry<String, String> query : queryList) {

                String paramKey;

                if(query.getKey().contains("prov_") || query.getKey().contains("orig_")){
                    paramKey = (query.getKey().split("_"))[1];
                }else{

                    // if warm up
                    paramKey = query.getKey();
                }

                List<Map<String, Object>> params = map.get(paramKey);
                int randomNum = rng.nextInt(0, params.size());

                // query execution
                driver.runQuery(query.getValue(), query.getKey(), params.get(randomNum));
            }
        }
        System.out.println("Warm cache : Done");

    }

    private static List<Map<String, Object>> readParams(String path) throws IOException {
        Reader in = new FileReader(path);

        // Load all CSV records into a list
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter('|')
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        Iterable<CSVRecord> iterable = csvFormat.parse(in);

        List<Map<String, Object>> records = new ArrayList<>();
        List<String> headers = null;

        for (CSVRecord record : iterable) {
            Map<String, Object> recordMap = new HashMap<>();

            if(headers == null){
                headers = record.getParser().getHeaderNames();
            }

            if(headers.contains("id")){
                recordMap.put("ID", record.get("id"));
            }
            if(headers.contains("startTime")){
                recordMap.put("START_TIME", Long.parseLong(record.get("startTime")));
            }
            if(headers.contains("endTime")){
                recordMap.put("END_TIME", Long.parseLong(record.get("endTime")));
            }
            if(headers.contains("threshold")){
                recordMap.put("THRESHOLD", Integer.parseInt(record.get("threshold")));
            }

            records.add(recordMap);
        }

        System.out.println("Read params: "+path);
        return records;
    }

    public static void main( String[] args ) throws IOException {

        // load configs
        appSettings = new Properties();
        try(FileInputStream fis = new FileInputStream("/app/config.properties")){
            appSettings.load(fis);

        }catch (IOException e){
            System.out.println("config.properties not found");
            System.exit(1);
        }


        Map<String, String> tsrQueries = new LinkedHashMap<>();

        // process the query file
        try(FileInputStream fis = new FileInputStream(appSettings.getProperty("query_file"));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);) {

            String line;
            String currentTsr = null;
            StringBuilder queryBuilder = new StringBuilder();

            //Create the query map
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Detect TSR start
                if (line.startsWith("//tsr-")) {
                    // Save previous TSR query if exists
                    if (currentTsr != null) {
                        tsrQueries.put(currentTsr, queryBuilder.toString().trim());
                    }
                    // Start new TSR
                    currentTsr = line.substring(2).trim(); // of the format tsr-{n}
                    queryBuilder = new StringBuilder();
                } else if (line.startsWith("\n") || line.startsWith("//")) {
                    continue;
                } else {
                    queryBuilder.append(line).append("\n");
                }
            }

            // Save last TSR
            if (currentTsr != null) {
                tsrQueries.put(currentTsr, queryBuilder.toString().trim());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Retrieve Parameters
        String paramFolder = appSettings.getProperty("param_folder");
        Map<String, List<Map<String, Object>>> map = new HashMap<>();

        Path parentDir = Paths.get(paramFolder);

        Files.walkFileTree(parentDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.getFileName().toString().toLowerCase().contains("param")) {
                    System.out.println("Found folder: " + dir.toAbsolutePath());

                    // Construct a map of parameter records
                    for(String queryKey : tsrQueries.keySet()){
                        String key = queryKey.substring(4);

                        map.put(queryKey, readParams(dir.toAbsolutePath()+"/params_"+key+".csv"));
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });


        try(DbDriver driver = DbDriverFactory.createDriver()){

            driver.connect();
            warmUpCache(driver,tsrQueries, map);

            for(String provModel: provenanceModels) {

                System.out.println("Running experiments for "+ appSettings.getProperty("result_file")+", provenance model: "+provModel);
                Map<String, String> finalQueries = driver.generateTestQuerySet(tsrQueries, provModel);
                runExperiments(driver, finalQueries, map, provModel);
            }

//            driver.printResultLengths();

        }catch (Exception e){
            System.out.println(e.toString());
            System.exit(1);
        }


    }
}
