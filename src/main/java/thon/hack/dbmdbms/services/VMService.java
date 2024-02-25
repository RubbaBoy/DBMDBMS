package thon.hack.dbmdbms.services;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import org.springframework.stereotype.Service;
import thon.hack.dbmdbms.SessionData;
import thon.hack.dbmdbms.StreamGobbler;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class VMService {

    private final SessionService sessionService;
    private final ProxmoxService proxmoxService;
    private final ChatGPTService chatGPTService;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    String result = "";
    String user = "XXXX";
    String host = "XXXX.XXX";
    int port = 22;
    String directory = "/";
    String sshPrivateKeyPath = "id-rsa.priv";
    String sshPrivateKeyPassword = "XXXXX";
    String sshPublicKeyPath = "id-rsa.pub";
    int timeout = 10000;
    private JSch jsch;

    public VMService(ProxmoxService proxmoxService, ChatGPTService chatGPTService, SessionService sessionService) throws JSchException, IOException {
        this.proxmoxService = proxmoxService;
        this.chatGPTService = chatGPTService;
        this.sessionService = sessionService;

        init();
    }

    public void init() throws IOException, JSchException {
        jsch = new JSch();

//        var privateKey = getClass().(context.getBaseURL(), sshPrivateKeyPath);
//        byte[] privateKeyBytes = Files.readAllBytes(Paths.get(privateKey.toURI()));
//
//        var publicKey = AssemblyUtils.locateResource(context.getBaseURL(), sshPublicKeyPath);
//        byte[] publicKeyBytes = Files.readAllBytes(Paths.get(publicKey.toURI()));

//        System.out.println("System.getProperty(\"PRIVATE_KEY\") shittt = " + System.getenv("PRIVATE_KEY"));
//        jsch.addIdentity(Files.readString(Path.of(System.getenv("PRIVATE_KEY"))));
        jsch.addIdentity(System.getenv("PRIVATE_KEY"));
    }

    public SessionData createVM(String databaseType) throws Exception {
//        executeQuery(new SessionData("", "192.168.1.157", 0), "echo hi");

//        if (true) return null;

//        var fullIp = "192.168.1.193";
//        var sessionData = new SessionData(databaseType, fullIp, 0);
        String fullIp = sessionService.getNextIp();
        int newVMId = sessionService.getCurrentIpEndDontIncrement();
        var sessionData = proxmoxService.createVM(databaseType, newVMId, fullIp);

        System.out.println("shit");
        var gptResponse = chatGPTService.getChatGptResponse(databaseType);

        System.out.println("balls");
        Thread.sleep(30000);

        System.out.println("sessionData = " + sessionData);

        System.out.println("gptResponse = " + gptResponse);

        var cockIp = "192.168.1.10";

        var playbookFile = File.createTempFile("playbook_", String.valueOf(System.currentTimeMillis()) + ".yml");
        var requirementsFile = File.createTempFile("requirements_", String.valueOf(System.currentTimeMillis()));
        var pythonFile = File.createTempFile("python_", String.valueOf(System.currentTimeMillis()));
        
        Files.writeString(requirementsFile.toPath(), gptResponse.requirementsTxt());
        Files.writeString(pythonFile.toPath(), gptResponse.pythonScript());
        Files.writeString(playbookFile.toPath(), gptResponse.ansibleScript());


        Files.writeString(Path.of(System.getenv("INVENTORY")), String.format("gpt@%s", sessionData.getIp()));

        var ping = File.createTempFile("ping_", "shit");
//        Files.writeString(ping.toPath(), "ping " + sessionData.getIp());

//        executeQuery("cat " + playbookFile.getAbsolutePath() + " |", sessionData, "cat > playbook.yml");
//        executeBash("/bin/bash", "-c", ping.getAbsolutePath());

        var process = Runtime.getRuntime().exec(String.format("ping %s", sessionData.getIp()));

        StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), line -> {
                    System.out.println("line = " + line);

                    if (line.contains("bytes from")) {
                        System.out.println("got bytes from! VM ready!");
                        process.destroy();
                    }
                });

        Future<?> future = executor.submit(streamGobbler);

        int exitCode = process.waitFor();

        Thread.sleep(1000);
        
        System.out.println("ping exitCode = " + exitCode);

        for (int i = 0; i < 30; i++) {
            if (0 == executeCommandViaScript(String.format("ssh -o StrictHostKeyChecking=no -i %s gpt@%s \"echo 123\"", System.getenv("PRIVATE_KEY"), sessionData.getIp()))) {
                System.out.println("break out! good!");
                break;
            }

            System.out.println("Couldn't connect...");
            
            Thread.sleep(1000);
        }

        Thread.sleep(5000);
        
        if (0 != executeCommandViaScript(String.format("ansible-playbook -b -i %s %s --key-file %s", System.getenv("INVENTORY"), playbookFile.getAbsolutePath(), System.getenv("PRIVATE_KEY")))) {
            System.err.println("Couldn't make ansible playbook");
        } else {
            System.out.println("Set up ansible");
        }
        
        Thread.sleep(1000);
        
        if (0 != executeCommandViaScript(String.format("ansible-playbook -b -i %s %s --key-file %s", System.getenv("INVENTORY"), playbookFile.getAbsolutePath(), System.getenv("PRIVATE_KEY")))) {
            System.err.println("Couldn't make ansible playbook");
        } else {
            System.out.println("Set up ansible");
        }

        if (0 != executeCommandViaScript(String.format("cat %s | ssh -o StrictHostKeyChecking=no -i %s gpt@%s \"cat > requirements.txt\"", requirementsFile.getAbsolutePath(), System.getenv("PRIVATE_KEY"), sessionData.getIp()))) {
            System.err.println("Error writing requirements.txt");
        } else {
            System.out.println("Made requirements.txt");
        }


        if (0 != executeCommandViaScript(String.format("cat %s | ssh -o StrictHostKeyChecking=no -i %s gpt@%s \"cat > cocker.py\"", pythonFile.getAbsolutePath(), System.getenv("PRIVATE_KEY"), sessionData.getIp()))) {
            System.err.println("Error writing cocker.py");
        } else {
            System.out.println("Made cocker.py");
        }

        if (0 != executeCommandViaScript(String.format("ssh -o StrictHostKeyChecking=no -i %s gpt@%s \"pip install -r requirements.txt\"", System.getenv("PRIVATE_KEY"), sessionData.getIp()))) {
            System.err.println("Error running requirements");
        } else {
            System.out.println("good running requirements");
        }

        executeCommandViaScriptAsync(String.format("ssh -o StrictHostKeyChecking=no -i %s gpt@%s \"python cocker.py > log.log\"", System.getenv("PRIVATE_KEY"), sessionData.getIp()));


//        executeQuery("cat " + requirementsFile.getAbsolutePath(), sessionData, "cat > requirements.txt");
//        executeQuery("cat " + pythonFile.getAbsolutePath(), sessionData, "cat > cocker.py");
//        
//        Thread.sleep(3000);
//        
//        var ansibleQuery = executeBash("ansible-playbook", "-b", "-i", System.getenv("INVENTORY"), playbookFile.getAbsolutePath(), "--key-file", System.getenv("PRIVATE_KEY"));
//        System.out.println("ansibleQuery = " + ansibleQuery);
////        Thread.sleep(50);
//        var requirementsQuery = executeQuery(sessionData, "pip install -r requirements.txt");
//        System.out.println("requirementsQuery = " + requirementsQuery);
//        var pythonQuery = executeQuery(sessionData, "python cocker.py");
//        System.out.println("pythonQuery = " + pythonQuery);
//        
//        System.out.println("sessionData = " + sessionData);
//        System.out.println("gptResponse = " + gptResponse);
//        System.out.println("pythonFile = " + pythonFile);

        return sessionData;
    }

    private void executeCommandViaScriptAsync(String script) throws IOException, InterruptedException {
        // Step 1: Create a temporary file
        File tempScript = File.createTempFile("script", ".sh");

        // Ensure the file is deleted on exit
        tempScript.deleteOnExit();

        // Step 2: Write a bash script to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempScript))) {
            writer.write("#!/bin/bash");
            writer.newLine();
            writer.write(script);
            // Add more commands to your script here
        }

        // Step 3: Make the script executable
        boolean isExecutable = tempScript.setExecutable(true);
        if (!isExecutable) {
            System.err.println("Failed to make the script executable.");
            return;
        }

        // Optional: Execute the script from Java
        ProcessBuilder pb = new ProcessBuilder(tempScript.getAbsolutePath());
        Process process = pb.start();

        StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), System.out::println);
        StreamGobbler streamGobbler2 =
                new StreamGobbler(process.getErrorStream(), System.err::println);

        executor.submit(streamGobbler);
        executor.submit(streamGobbler2);
    }

    private int executeCommandViaScript(String script) throws IOException, InterruptedException {
        // Step 1: Create a temporary file
        File tempScript = File.createTempFile("script", ".sh");

        // Ensure the file is deleted on exit
        tempScript.deleteOnExit();

        // Step 2: Write a bash script to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempScript))) {
            writer.write("#!/bin/bash");
            writer.newLine();
            writer.write(script);
            // Add more commands to your script here
        }

        // Step 3: Make the script executable
        boolean isExecutable = tempScript.setExecutable(true);
        if (!isExecutable) {
            System.err.println("Failed to make the script executable.");
            return 99999;
        }

        // Optional: Execute the script from Java
        ProcessBuilder pb = new ProcessBuilder(tempScript.getAbsolutePath());
        Process process = pb.start();

        StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), System.out::println);
        StreamGobbler streamGobbler2 =
                new StreamGobbler(process.getErrorStream(), System.err::println);

        executor.submit(streamGobbler);
        executor.submit(streamGobbler2);
        
        return process.waitFor();
    }

    private int executeCommand(String command) throws IOException, InterruptedException {
        System.out.println("exec command = " + command);
        var reqProcess = Runtime.getRuntime().exec(command);

        StreamGobbler streamGobbler =
                new StreamGobbler(reqProcess.getInputStream(), System.out::println);
        StreamGobbler streamGobbler2 =
                new StreamGobbler(reqProcess.getErrorStream(), System.err::println);

        executor.submit(streamGobbler);
        executor.submit(streamGobbler2);

        return reqProcess.waitFor();
    }

    public String executeQuery(SessionData sessionData, String query) throws IOException {
        return executeQuery("", sessionData, query);
    }

    public String executeQuery(String prePipe, SessionData sessionData, String query) throws IOException {
        var host = sessionData.getIp();

        return executeSSHCommand(prePipe, host, query);
    }

    private String executeSSHCommand(String host, String command) throws IOException {
        return executeSSHCommand("", host, command);
    }

    private String executeSSHCommand(String prePipe, String host, String command) throws IOException {
        var privateKeyFile = System.getenv("PRIVATE_KEY");
        var username = "gpt";
//        var host = "192.168.1.109";
//        var command = "echo 123";

//        var sshCommand = "%sssh -o -i %s %s@%s \"%s\"".formatted(prePipe.isEmpty() ? "" : " " + prePipe, privateKeyFile, username, host, command);
//        System.out.println("sshCommand = " + sshCommand);

        var ogCmd = "ssh -i " + privateKeyFile + String.format(" %s@%s", username, host) + " \"" + command + "\"";

//        var argList = new ArrayList<>(List.of("ssh", "-i", privateKeyFile, String.format("%s@%s", username, host), "\"" + command + "\""));
        if (prePipe != null && !prePipe.isBlank()) {
            var ogString = ogCmd;
            ogCmd = prePipe + " | " + ogString;
            System.out.println("prePipe = " + prePipe);
            System.out.println("ogCmd = " + ogCmd);
//            argList.add("|");
//            argList.add(ogString);

//            argList.addAll(0, Arrays.stream(prePipe.split(" ")).toList());
        }

        var list = List.of("/bin/bash", "-c", ogCmd);

//        String[] shit = new String[] {
//                prePipe.isEmpty() ? "" : " " + prePipe, 
//        };

        System.out.println("list = " + list);
        var process = new ProcessBuilder(list.toArray(String[]::new)).inheritIO().start();
        System.out.println("yeah");
        System.out.println(process.pid());


        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            System.out.println("Here!");
//            reader.lines().forEach(System.out::println);

            int exitVal = process.waitFor();
            System.out.println("exitVal = " + exitVal);
            if (exitVal == 0) {
                System.out.println("Success!");
            } else {
                System.out.println("SSH command execution error.");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return "nulklll";
    }

    public String executeBash(String... commands) throws IOException {
        System.out.println("commands = " + Arrays.toString(commands));
        var process = new ProcessBuilder(commands).inheritIO().start();
        System.out.println(process.pid());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            System.out.println("Here!");
//            reader.lines().forEach(System.out::println);

            int exitVal = process.waitFor();
            System.out.println("exitVal = " + exitVal);
            if (exitVal == 0) {
                System.out.println("Success!");
            } else {
                System.out.println("SSH command execution error.");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return "idk";
    }

    private String executeSSH(String host, String input) throws JSchException {
        var session = jsch.getSession("gpt", host);
        var config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setTimeout(timeout);

        try {
            session.connect(timeout);

            var channel = session.openChannel("exec");

            ((ChannelExec) channel).setCommand(input);

            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);

            // Get input streams for the remote echo command
            java.io.InputStream in = channel.getInputStream();

            channel.connect();

            var stringBuilder = new StringBuilder();

            // Read the output from the command
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    stringBuilder.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    System.err.println("Exit status: " + channel.getExitStatus());
                    break;
                }
                try {Thread.sleep(1000);} catch (Exception ee) {}
            }

            channel.disconnect();
            session.disconnect();

            return stringBuilder.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            session.disconnect();
        }
    }
}
