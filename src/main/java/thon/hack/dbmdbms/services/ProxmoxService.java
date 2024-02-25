package thon.hack.dbmdbms.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import thon.hack.dbmdbms.SessionData;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProxmoxService {

    public ProxmoxService() {
    }

    public SessionData createVM(String databaseType, int vmID, String ipAddress) throws Exception {
        System.out.println("databaseType = " + databaseType + ", vmID = " + vmID + ", ipAddress = " + ipAddress);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        sslContext.init(null, trustManagers, new java.security.SecureRandom());


        var baseurl = "https://192.168.1.10:8006/api2/json/";
        var proxnode = "nodes/poweredger";
        var cloneurl = baseurl + proxnode + "/qemu/6969/clone/";
        var configurl = baseurl + proxnode + "/qemu/" + vmID + "/config/";
        var starturl = baseurl + proxnode + "/qemu/" + vmID + "/status/start/";
        var combineip = "ip=" + ipAddress+ ",gw=192.168.1.1";

        var clonebody = Map.of("name", databaseType, "newid", String.valueOf(vmID), "full", true);
        var configbody = Map.of("ipconfig0", String.valueOf(combineip));

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonCloneBody = objectMapper.writeValueAsString(clonebody);
        System.out.println("jsonCloneBody = " + jsonCloneBody);
        String othershit = objectMapper.writeValueAsString(configbody);

        HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();

        HttpRequest clone = HttpRequest.newBuilder().uri(URI.create(cloneurl)).header("Authorization", "PVEAPIToken=gpt@pam!balls=0862a9ef-1ace-4659-aa42-2ed01399d6c8").header("content-type", "application/json").POST(HttpRequest.BodyPublishers.ofString(jsonCloneBody)).build();
        HttpRequest config = HttpRequest.newBuilder().uri(URI.create(configurl)).header("Authorization", "PVEAPIToken=gpt@pam!balls=0862a9ef-1ace-4659-aa42-2ed01399d6c8").header("content-type", "application/json").POST(HttpRequest.BodyPublishers.ofString(othershit)).build();
        HttpRequest startvm = HttpRequest.newBuilder().uri(URI.create(starturl)).header("Authorization", "PVEAPIToken=gpt@pam!balls=0862a9ef-1ace-4659-aa42-2ed01399d6c8").POST(HttpRequest.BodyPublishers.noBody()).build();

        HttpResponse<String> response = null;
        try {
            response = client.send(clone, HttpResponse.BodyHandlers.ofString());
            System.out.println("res = " + response.statusCode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //{"data":"UPID:poweredger:0001A377:0043681F:65DACEEA:qmconfig:157:gpt@pam!balls:"}

        
        var taskstatusurl = "";
        Pattern pattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(response.body());
        if (matcher.find()) {
            // Extract the content within the quotes
            String taskuuid = matcher.group(1);
            taskstatusurl = baseurl + proxnode + "/tasks/" + taskuuid + "/status";
        }
        else {
            throw new RuntimeException();
        }
        Thread.sleep(30000);
//        HttpRequest getstatus = HttpRequest.newBuilder().uri(URI.create(taskstatusurl)).header("Authorization", "PVEAPIToken=gpt@pam!balls=0862a9ef-1ace-4659-aa42-2ed01399d6c8").GET().build();
//        var statusproxstring = "null";
//            try {
//                while (!statusproxstring.contains("running")) {
//                    HttpResponse<String> statusprxo = client.send(getstatus, HttpResponse.BodyHandlers.ofString());
//                    statusproxstring = statusprxo.body();
//                    System.out.println(statusprxo.body());
//                    System.out.println("prox = " + statusprxo.statusCode());
//                    Thread.sleep(1000);
//                }
//            } catch (Exception e) {e.printStackTrace();}
//
            //hopefully above works
            
            try {
                HttpResponse<String> response2 = client.send(config, HttpResponse.BodyHandlers.ofString());
                System.out.println(response2.body());
                System.out.println("res2 = " + response2.statusCode());
            } catch (Exception e) {e.printStackTrace();}
            try {
                HttpResponse<String> response3 = client.send(startvm, HttpResponse.BodyHandlers.ofString());
                System.out.println(response3.body());
                System.out.println("res3 = " + response3.statusCode());
            } catch (Exception e) {e.printStackTrace();}
            
            // TODO: what the fuck does this return
            return new SessionData("name", ipAddress.substring(0, ipAddress.indexOf("/")), 123);
        }
    }

