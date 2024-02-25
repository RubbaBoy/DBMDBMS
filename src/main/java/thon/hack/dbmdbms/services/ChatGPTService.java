package thon.hack.dbmdbms.services;

import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class ChatGPTService {
    
    private final OpenAiService service;
    
    private final String ansiblePrompt = """
            Create an Ansible script to set up a DynamoDB database on Ubuntu server 22, using default credentials and 
            ports. Then, create a simple python script that hosts an HTTP endpoint on /query that takes in a DynamoDB 
            query, and executes it. Only give me the ansible script code, and the python code with a requirements.txt 
            file, and that's it. Do not return any explanation for anything, just code files. DO NOT INCLUDE ANY ENGLISH
            SURROUNDING ANY CODE BLOCKS! ONLY INCLUDE THE CODE BLOCKS!!! GIVE THEM IN ORDER OF ANSIBLE< PYTHON, AND
            REQUIREMENTS!!! INSTALL PYTHON AS WELL! BEFORE EVERYTHING, DO AN APT UPDATE/UPGRADE!! FOR THE ANSIBLE SCRIPT,
            MAKE hosts: all
            """;
    
    private final String englishToQueryPrompt = """
            write a query for a _ database with absolutely no other text from the following english description. 
            Do not respond with anything besides a query. If there is no valid query possible from the description
            simply return 'no valid query'.ENGLISH DESCRIPTION:
            """;
    
    public ChatGPTService() {
        this.service = new OpenAiService(System.getenv("OPENAI_KEY"), Duration.ofDays(1));
    }
    
    public ChatGPTResponse getChatGptResponse(String databaseType) {
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(List.of(new ChatMessage("user", ansiblePrompt.replace("DynamoDB", databaseType))))
                .model("gpt-4-turbo-preview")
                        .build();
        
        String response = this.service.createChatCompletion(completionRequest).getChoices().get(0).getMessage().getContent();

//        System.out.println("response = " + response);
        
        System.out.println(response);
        
//        var splitResponse = Arrays.stream(response.split("^```$")).map(segment ->
//                Arrays.stream(segment.split("\n"))
//                        .filter(line -> !line.startsWith("###") &&
//                                !line.startsWith("---") &&
//                                !line.startsWith("python") &&
//                                !line.startsWith("yaml") &&
//                                !line.startsWith("plaintext") &&
//                                !line.startsWith("```") &&
//                                !line.startsWith("yml"))
//                        .collect(Collectors.joining("\n")))
//                .filter(line -> !line.isBlank())
//                .toList();
//        
////                .filter(line -> !line.startsWith("```") && !line.startsWith("###")).toList();
//
//        System.out.println("splitResponse size( = " + splitResponse.size());
//        
//        System.out.println("splitResponse = " + splitResponse);
        
        var blocks = extractBlocks(response);
        
        return (new ChatGPTResponse(blocks.get(0), blocks.get(1), blocks.get(2)));
    }

    private static List<String> extractBlocks(String text) {
        var blocks = new ArrayList<String>();

        Pattern pattern = Pattern.compile("```(.*?)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            var block = matcher.group(1);
            block = block.substring(block.indexOf("\n") + 1);

//            System.out.println("block = \"" + block + "\"");
            if (block.startsWith("---")) {
                System.out.println("yep");
                block = block.substring(3);
            }

            blocks.add(block);
        }

        return blocks;
    }
    
    public String getQueryFromEnglish(String databaseType, String englishDescription){
        
        String filledPrompt = englishToQueryPrompt.replace("_", databaseType).concat(englishDescription);
        
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(List.of(new ChatMessage("user", filledPrompt)))
                .model("gpt-4-turbo-preview")
                .build();

        
        
        return this.service.createChatCompletion(completionRequest)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent()
                .replaceAll("^```sql\\n|\\n```$", "");
    }
    
    public record ChatGPTResponse(String ansibleScript, String pythonScript, String requirementsTxt) {}
    
}
