package thon.hack.dbmdbms.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import thon.hack.dbmdbms.SessionData;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SessionService {
    
    private static final String IP_FIRST_PART = "192.168.1.";
    private static final int DEFAULT_PORT = 80;
//    private static final int IP_LAST_PART_START = 150;
    private static final int IP_LAST_PART_CAP = 254;
    private int ipLastPartCurrent;
    private Map<String, SessionData> sessionMap;
    
    private final String pathToJsonFile = "persistant/sessionMap.json";
    private final String ipFilePath = "persistant/IpFile.txt";
     
    public SessionService() throws Exception{
        //empty
        var map = this.readMapFile();
        
        if(map != null){
            this.sessionMap = map;
        } else {
            this.sessionMap = new HashMap<String, SessionData>();
        }

        this.ipLastPartCurrent = readIpFile(ipFilePath);
    }

    /**
     * Checks the hashmap to see if the db name 
     * @param name
     * @return
     */
    public Optional<SessionData> checkDB(String name){
        SessionData data = sessionMap.get(name);
        Optional<SessionData> opt = Optional.ofNullable(data);
        return opt;
    }
    
    public void updateSessionMap(String name, SessionData sessionData){
        this.sessionMap.put(name, sessionData);
        writeMapFile();
    }
    
    private void writeMapFile(){
        // Use Jackson's ObjectMapper to write the map to a JSON file
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Writing to a file
            mapper.writeValue(new File(pathToJsonFile), this.sessionMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, SessionData> readMapFile() {
        if (!new File(pathToJsonFile).exists()) {
            return null;
        }
        
        
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Read the JSON file into a Map
            return mapper.readValue(new File(pathToJsonFile), new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }
    
    private int readIpFile(String filePath) throws IOException{
        FileReader fileReader = new FileReader(filePath);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        int output = Integer.parseInt(bufferedReader.readLine());
        bufferedReader.close();
        fileReader.close();
        return output;
    }
    
    private void writeIpFile() throws IOException{ 
        FileWriter fileWriter = new FileWriter(ipFilePath);
        fileWriter.write(String.valueOf(this.ipLastPartCurrent));
        fileWriter.close();
    }
    
    public String getNextIp() throws IOException{
        ipLastPartCurrent++;
        writeIpFile();
        String fullIp = IP_FIRST_PART+ipLastPartCurrent+"/24";
        return fullIp;
    }
    
    public int getCurrentIpEndDontIncrement(){
        return this.ipLastPartCurrent;
    }
    
    // 192.168.1.150
    
    // session consists of: db name, ip
    
    // get next IP to use (start at constant IP or something)
    
    // have session stores in a file
    
    // look up session from database name (normalize name)
    
}
