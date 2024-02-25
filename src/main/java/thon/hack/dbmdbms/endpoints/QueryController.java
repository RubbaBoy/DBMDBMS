package thon.hack.dbmdbms.endpoints;

import jakarta.websocket.Session;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import thon.hack.dbmdbms.SessionData;
import thon.hack.dbmdbms.services.ChatGPTService;
import thon.hack.dbmdbms.services.ProxmoxService;
import thon.hack.dbmdbms.services.SessionService;
import thon.hack.dbmdbms.services.VMService;

import java.util.Optional;

@Controller
@RequestMapping("/api")
public class QueryController {
    
    private final SessionService sessionService;
    private final ProxmoxService proxmoxService;
    private final VMService vmService;
    private final ChatGPTService chatGPTService;
    
    public QueryController(SessionService sessionService, ProxmoxService proxmoxService, VMService vmService, ChatGPTService chatGPTService){
        this.sessionService = sessionService;
        this.proxmoxService = proxmoxService;
        this.vmService = vmService;
        this.chatGPTService = chatGPTService;
    }
    
    @PostMapping("/query/{databaseType}")
    @CrossOrigin(originPatterns = "*")
    public ResponseEntity<?> sendQuery(@PathVariable String databaseType, @RequestBody String query) throws Exception {
        System.out.println("testing");

        System.out.println("databaseType = " + databaseType);
        System.out.println("query = " + query);
        
        //format databaseType
        var finalDatabaseType = databaseType.strip().toLowerCase();
        
        var sessionDataOptional = sessionService.checkDB(databaseType);
        
        // check session service if the database exists
        // if it exists, use VM service to create vm. return SessionData
        var sessionData = sessionDataOptional.orElseGet(() -> {
            // if doesn't exist, use the given SessionData to create a new vm
            try {
                var data = vmService.createVM(finalDatabaseType);
                sessionService.updateSessionMap(finalDatabaseType, data);
                return data;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        if (true) return ResponseEntity.of(Optional.of("what the fuckk"));
        
        // use VMService to make request with the database query to the VM, return result
        String response = vmService.executeQuery(sessionData, query);

        System.out.println("response = " + response);
        
        return ResponseEntity.ok().body("ok");
    }
    
    @GetMapping("/query/fromEnglish/{databaseType}")
    @CrossOrigin(originPatterns = "*")
    public ResponseEntity<?> queryFromEnglish(@PathVariable String databaseType, @RequestParam String englishDescription){
        String response = chatGPTService.getQueryFromEnglish(databaseType, englishDescription);
        
        return ResponseEntity.ok(response);
    }
}
