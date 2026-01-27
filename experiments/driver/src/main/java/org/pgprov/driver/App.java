package org.pgprov.driver;


import org.apache.commons.lang3.tuple.Pair;
import org.pgprov.driver.db.DbDriver;
import org.pgprov.driver.db.DbDriverFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.pgprov.driver.db.Neo4jDbDriver;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Hello world!
 *
 */
public class App 
{

    public static Properties appSettings;
    public static List<String> provenanceModels = List.of("How");

    public static Double median(List<Double> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("List is empty");
        }

        List<Double> copy = new ArrayList<>(values); // don’t mutate caller
        Collections.sort(copy);

        int n = copy.size();
        if (n % 2 == 1) {
            return copy.get(n / 2);
        } else {
            return (copy.get(n / 2 - 1) + copy.get(n / 2)) / 2.0;
        }
    }

    private static void runExperiments(Neo4jDbDriver driver, Map<String, Pair<String, Map<String,Object>>> finalQueries, String provModel){

        Map<String, Double> queryRunTimes = new HashMap<>();  // stores per query per param
        Map<String, Integer> queryRunCount = new HashMap<>(); // stores per query per param

        String latencyFilePath = appSettings.getProperty("output_folder")+"_latency.csv";
        List<Map.Entry<String, Pair<String, Map<String,Object>>>> queryList = new ArrayList<>(finalQueries.entrySet());

        int execCount = Integer.parseInt(appSettings.getProperty("test_query_count"));

//        for (int j=0;j< execCount;j++) {

//            System.out.println("-----------------------------------------Start Execution Round ("+j+")---------------------------------------------");
            Collections.shuffle(queryList); // randomize query execution order

            int j=0;
            for (Map.Entry<String, Pair<String, Map<String, Object>>> query : queryList) {

                String queryParamKey = query.getKey().substring(0, query.getKey().lastIndexOf('_'));
                Pair<String, Map<String, Object>> queryParamMap = query.getValue();
                System.out.println("Sending request = "+ queryParamKey+","+queryParamMap.getRight());

                // query execution
                Pair<Double, Integer> testResults = driver.runTestProcedureQuery(queryParamKey, queryParamMap.getKey(), queryParamMap.getValue());

                // Cache execTimes
                if (queryRunTimes.containsKey(query.getKey())) {
                    queryRunTimes.put(query.getKey(), queryRunTimes.get(query.getKey()) + testResults.getLeft());
                } else {
                    queryRunTimes.put(query.getKey(), testResults.getLeft());
                }


                if(!queryRunCount.containsKey(query.getKey())){
                    queryRunCount.put(query.getKey(), testResults.getRight());
                }
//                System.out.println(query.getKey() + ", " + queryParamMap.getValue() + ", " + durationMs);
                j+=1;
                if(j%100==0) System.out.println("Execution finished for :"+ j);

            }
//        }

        // Write execTimes to file
        File latencyFile = new File(latencyFilePath);

        boolean latencyFieExists = latencyFile.exists();
        try (FileWriter latencyWriter = new FileWriter(latencyFilePath,true)) {

            if(!latencyFieExists) {
                latencyWriter.write("dataset,scaleFactor,provModel,query,parameter,mean,resultSize\n");
            }

            for (Map.Entry<String, Double> entry : queryRunTimes.entrySet()) {

                String queryParamKey = entry.getKey().substring(0, entry.getKey().lastIndexOf('_')+1);
                String paramKey = entry.getKey().substring(entry.getKey().lastIndexOf('_'));
                latencyWriter.write(appSettings.getProperty("dataset") + ","
                        + appSettings.getProperty("scale_factor") + ","
                        + provModel + ","
                        + queryParamKey + ","
                        + paramKey + ","
                        + (entry.getValue()/execCount) +","
                        + (queryRunCount.get(entry.getKey()))+"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static void runLimitExperiments(Neo4jDbDriver driver, Map<String, Pair<String, Map<String,Object>>> finalQueries, String provModel){

        Map<String, Double> queryRunTimes = new HashMap<>();
        Map<String, Integer> queryRunCount = new HashMap<>();

        List<String> limits = List.of(appSettings.getProperty("limits").split(","));
        String latencyFilePath = appSettings.getProperty("output_folder")+"_latency.csv";
        List<Map.Entry<String, Pair<String, Map<String,Object>>>> queryList = new ArrayList<>(finalQueries.entrySet());

        int execCount = Integer.parseInt(appSettings.getProperty("test_query_count"));

        for(String limit : limits) {

            for (int j = 0; j < execCount; j++) {

                Collections.shuffle(queryList); // randomize query execution order

                for (Map.Entry<String, Pair<String, Map<String, Object>>> query : queryList) {

                    String resultKey = query.getKey().substring(0, query.getKey().lastIndexOf('_'));
                    System.out.println("Sending request = " + query.getValue());
                    Pair<String, Map<String, Object>> queryParamMap = query.getValue();
                    Map<String, Object> parameters = queryParamMap.getValue();
                    parameters.put("LIMIT", limit);

                    // query execution
                    Pair<Double, Integer> testResults = driver.runTestProcedureQuery(query.getKey(), queryParamMap.getKey(), parameters);

                    // Cache execTimes
                    if (queryRunTimes.containsKey(resultKey)) {
                        queryRunTimes.put(resultKey, queryRunTimes.get(resultKey) + testResults.getLeft());
                    } else {
                        queryRunTimes.put(resultKey, testResults.getLeft());
                    }

                    if (testResults.getRight() > 0) {
                        if (queryRunCount.containsKey(resultKey)) {
                            queryRunCount.put(resultKey, queryRunCount.get(resultKey) + 1);
                        } else {
                            queryRunCount.put(resultKey, 1);
                        }
                    } else {
                        if (!queryRunCount.containsKey(resultKey)) {
                            queryRunCount.put(resultKey, 0);
                        }
                    }
                    //                System.out.println(query.getKey() + ", " + queryParamMap.getValue() + ", " + durationMs);

                }
            }

            // Write execTimes to file
            File latencyFile = new File(latencyFilePath);

            boolean latencyFieExists = latencyFile.exists();
            try (FileWriter latencyWriter = new FileWriter(latencyFilePath, true)) {

                if (!latencyFieExists) {
                    latencyWriter.write("dataset,scaleFactor,provModel,limit,query,mean,hitRate\n");
                }

                for (Map.Entry<String, Double> entry : queryRunTimes.entrySet()) {
                    latencyWriter.write(appSettings.getProperty("dataset") + ","
                            + appSettings.getProperty("scale_factor") + ","
                            + provModel + ","
                            + limit +","
                            + entry.getKey() + ","
                            + (entry.getValue() / execCount) + ","
                            + (queryRunCount.get(entry.getKey()) / execCount) + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private static void warmUpCache(Neo4jDbDriver driver, Map<String, String> tsrQueries, Map<String, List<Map<String, Object>>> map){

        //Warm up cache
        int execCount = Integer.parseInt(appSettings.getProperty("warm_up_query_count"));

        Random rng = new Random();

        // loop the query set an execCount times
        for (int i = 0; i <execCount; i++) {

            List<Map.Entry<String, String>> queryList = new ArrayList<>(tsrQueries.entrySet());
            Collections.shuffle(queryList); // randomize query execution order

            for (Map.Entry<String, String> query : queryList) {

                String paramKey = query.getKey();

                List<Map<String, Object>> params = map.get(paramKey);
                int randomNum = rng.nextInt(0, params.size());

               // System.out.println("Sending request = "+query.getValue() + "," + params.get(randomNum));

                // query execution
                driver.runQuery(query.getKey(), query.getValue(), params.get(randomNum));

                //System.out.println("Done");
            }
            if(i%10==0) System.out.println("Round :"+i);
        }
        System.out.println("Warm cache : Done");

    }

    private static List<Map<String, Object>> readParams(String path, String paramMapPath) throws IOException {
        Reader in = new FileReader(path);
        Reader paramMapFile = new FileReader(paramMapPath);
        System.out.println("Path: "+path);

        // Load all CSV records into a list
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setDelimiter('|')
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        CSVFormat paramCsv = CSVFormat.DEFAULT.builder()
                .setDelimiter(',')
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        Iterable<CSVRecord> iterable = csvFormat.parse(in);
        Iterable<CSVRecord> paramIterable = paramCsv.parse(paramMapFile);

        Map<String, String> paramHeaderMap = new HashMap<>();
        Map<String, String> paramHeaderTypeMap = new HashMap<>();

        for(CSVRecord paramRecord : paramIterable){

            String queryParam = paramRecord.get("query");
            String fileParam = paramRecord.get("header");
            String dataType = paramRecord.get("dataType");

            paramHeaderMap.put(queryParam, fileParam);
            paramHeaderTypeMap.put(queryParam, dataType);
        }

        List<Map<String, Object>> records = new ArrayList<>();
        List<String> headers = null;

        for (CSVRecord record : iterable) {

            Map<String, Object> recordMap = new HashMap<>();

            if(headers == null){
                headers = record.getParser().getHeaderNames();
            }

            for(Map.Entry<String, String> paramHeader : paramHeaderMap.entrySet()){

                String queryParam = paramHeader.getKey();
                String fileParam = paramHeader.getValue();

                if(headers.contains(fileParam)) {
                    if (paramHeaderTypeMap.containsKey(queryParam) && Objects.equals(paramHeaderTypeMap.get(queryParam), "long")) {
                        recordMap.put(queryParam, Long.parseLong(record.get(fileParam)));
                    } else if (paramHeaderTypeMap.containsKey(queryParam) && Objects.equals(paramHeaderTypeMap.get(queryParam), "double")) {
                        recordMap.put(queryParam, Double.parseDouble(record.get(fileParam)));
                    } else if (paramHeaderTypeMap.containsKey(queryParam) && Objects.equals(paramHeaderTypeMap.get(queryParam), "int")) {
                        recordMap.put(queryParam, Integer.parseInt(record.get(fileParam)));
                    }else if (paramHeaderTypeMap.containsKey(queryParam) && Objects.equals(paramHeaderTypeMap.get(queryParam), "datetime")) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                            ZonedDateTime dt = ZonedDateTime.parse(record.get(fileParam), formatter);
                            // Convert to UTC explicitly
                        recordMap.put(queryParam, dt.withZoneSameInstant(ZoneOffset.UTC));
                    } else if (paramHeaderTypeMap.containsKey(queryParam) && Objects.equals(paramHeaderTypeMap.get(queryParam), "date")) {
                            LocalDate date = LocalDate.parse(record.get(fileParam), DateTimeFormatter.ISO_LOCAL_DATE);
                        recordMap.put(queryParam, date.atStartOfDay(ZoneOffset.UTC));
                    }else{
                        recordMap.put(queryParam, record.get(fileParam));
                    }

                }
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

            //Create the query paramMap
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Detect TSR start
                if (line.startsWith("//"+appSettings.getProperty("dataset"))) {
                    // Save previous TSR query if exists
                    if (currentTsr != null) {
                        tsrQueries.put(currentTsr, queryBuilder.toString().trim());
                    }
                    // Start new TSR
                    currentTsr = line.substring(2).trim(); // of the format {dataset}-{n}
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

        System.out.println("Reading queries : Done");

        // Retrieve Parameters
        String paramFolder = appSettings.getProperty("param_folder");

        Map<String, List<Map<String, Object>>> paramMap = new HashMap<>();

        Path parentDir = Paths.get(paramFolder);

        Files.walkFileTree(parentDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String fileName = dir.getFileName().toString().toLowerCase();
                if (fileName.contains("param") && !dir.getParent().getFileName().toString().startsWith("__")) {
                    System.out.println("Found folder: " + dir.toAbsolutePath());

                    // Construct a paramMap of parameter records
                    for(String queryKey : tsrQueries.keySet()){
                        String key = queryKey.substring(appSettings.getProperty("dataset").length()+1);

                        paramMap.put(queryKey, readParams(dir.toAbsolutePath()+"/params_"+key+".csv", appSettings.getProperty("param_map_file")));
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });


        try(Neo4jDbDriver driver = DbDriverFactory.createDriver()){

            driver.connect();

            System.out.println("Warm up cache: Start");
            warmUpCache(driver,tsrQueries, paramMap);

            for(String provModel: provenanceModels) {

                System.out.println("-------------------------------------------- Running experiments for dataset: "+ appSettings.getProperty("dataset")
                        +", scaleFactor: "+appSettings.getProperty("scale_factor")
                        + ", provenance model: "+provModel+" -----------------------------");

                Map<String, Pair<String, Map<String,Object>>> finalQueries = driver.generateTestQuerySet(tsrQueries, provModel, paramMap);
                runExperiments(driver, finalQueries, provModel);

                //driver.clearResults();
            }

//            driver.printResultLengths();

        }catch (Exception e){
            System.out.println(e.toString());
            System.exit(1);
        }


    }
}
