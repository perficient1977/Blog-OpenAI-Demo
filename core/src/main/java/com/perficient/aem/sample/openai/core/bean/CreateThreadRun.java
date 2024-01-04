package com.perficient.aem.sample.openai.core.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateThreadRun {

    public CreateThreadRun(String assistant_id, String content, String role) {
        this.assistant_id = assistant_id;
        Thread threadObj = new Thread();

        Message message = new Message();
        message.setContnet(content);
        message.setRole(role);

        ArrayList<Message> messages = new ArrayList<>();
        messages.add(message);
        threadObj.setMessages(messages);
        this.thread = threadObj;

    }

    @JsonProperty("assistant_id")
    String assistant_id;

    @JsonProperty("thread")
    Thread thread;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Thread{
        @JsonProperty("messages")
        ArrayList<Message> messages;

    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Message{
        @JsonProperty("role")
        String role = "user";

        @JsonProperty("content")
        String contnet = "";
    }
}
