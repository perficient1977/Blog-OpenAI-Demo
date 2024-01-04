package com.perficient.aem.sample.openai.core.services.impl;

import com.perficient.aem.sample.openai.core.bean.CreateThreadRun;
import com.perficient.aem.sample.openai.core.bean.Message;
import com.perficient.aem.sample.openai.core.bean.SummaryBean;
import com.perficient.aem.sample.openai.core.services.ChatGPTAPIService;
import com.perficient.aem.sample.openai.core.services.ChatGptHttpClientFactory;
import com.perficient.aem.sample.openai.core.services.JSONConverter;
import com.perficient.aem.sample.openai.core.services.config.ChatGptHttpClientFactoryConfig;
import com.perficient.aem.sample.openai.core.utils.StringObjectResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.util.concurrent.*;

@Slf4j
@Component(service = ChatGPTAPIService.class)
public class ChatGptAPIServiceImpl implements ChatGPTAPIService {

    private static final StringObjectResponseHandler HANDLER = new StringObjectResponseHandler();

    @Reference
    private ChatGptHttpClientFactory httpClientFactory;

    @Reference
    private JSONConverter jsonConverter;

    //Complete API
    //POST    https://api.openai.com/v1/engines/davinci/completions
    @Override
    public String invokeAPI(String bodyText, int maxTokens) {
        String responseString = StringUtils.EMPTY;
        try {
            responseString = httpClientFactory.getExecutor()
                    .execute(httpClientFactory.post().bodyString(generatePrompt(bodyText, maxTokens), ContentType.APPLICATION_JSON))
                    .handleResponse(HANDLER);
        } catch (IOException e) {
            log.error("Error occured while processing request {}", e.getMessage());
        }
        log.debug("API Request Response {}", responseString);
        return responseString;

    }

    //1. Create Thread and Run (When session not exist)
    //POST   https://api.openai.com/v1/threads/runs
    @Override
    public String creaetThreadAndRun(String role, String content) {
        String responseString = StringUtils.EMPTY;
        try {
            ChatGptHttpClientFactoryConfig config =  httpClientFactory.getConfig();
            String assistantId= config.assistantId();
            String bodyString = generateMessage_createThreadRun(assistantId, content, role);

            responseString = httpClientFactory.getExecutor()
                    .execute(httpClientFactory.postCreateThreadRun().bodyString(bodyString, ContentType.APPLICATION_JSON))
                    .handleResponse(HANDLER);
        } catch (IOException e) {
            log.error("Error occured while create thread and run {}", e.getMessage());
        }
        log.debug("creaetThreadAndRun: {}", responseString);
        return responseString;
    }


    //2. list Message
    //GET  https://api.openai.com/v1/threads/{thread_id}/messages
    @Override
    public String listMessages(String threadId) {
        String responseString = StringUtils.EMPTY;
        try {
            ChatGptHttpClientFactoryConfig config =  httpClientFactory.getConfig();
            String assistantId= config.assistantId();

            responseString = httpClientFactory.getExecutor()
                    .execute(httpClientFactory.listMessages(threadId))
                    .handleResponse(HANDLER);
        } catch (IOException e) {
            log.error("Error occured while list messages {}", e.getMessage());
        }
        log.debug("listMessages: {}", responseString);
        return responseString;
    }

    //3. Create a message
    //POST   https://api.openai.com/v1/threads/{thread_id}/messages
    @Override
    public String createMessage(String threadId,String role, String content) {
        String responseString = StringUtils.EMPTY;
        try {

            String bodyString = getCreateMessageBody(role, content);

            responseString = httpClientFactory.getExecutor()
                    .execute(httpClientFactory.postCreateMessage(threadId).bodyString(bodyString, ContentType.APPLICATION_JSON))
                    .handleResponse(HANDLER);
        } catch (IOException e) {
            log.error("Error occured while create Message {}", e.getMessage());
        }
        log.debug("creaetThreadAndRun: {}", responseString);
        JSONObject jsonObject = new JSONObject(responseString);
        String messageId = jsonObject.getString("id"); //?completed
        return messageId;
    }

    //4. Create a Run
    //POST   https://api.openai.com/v1/threads/{thread_id}/runs
    @Override
    public String createRun(String threadId) {
        String responseString = StringUtils.EMPTY;
        try {
            ChatGptHttpClientFactoryConfig config =  httpClientFactory.getConfig();
            String assistantId= config.assistantId();
            String bodyString = getCreateRunBody(assistantId);

            responseString = httpClientFactory.getExecutor()
                    .execute(httpClientFactory.postCreateRun(threadId).bodyString(bodyString, ContentType.APPLICATION_JSON))
                    .handleResponse(HANDLER);
        } catch (IOException e) {
            log.error("Error occured while run Assistant {}", e.getMessage());
        }
        log.debug("creaetThreadAndRun: {}", responseString);
        JSONObject jsonObject = new JSONObject(responseString);
        String respRunId = jsonObject.getString("id"); //thread id
        return respRunId;
    }

    //5. Check Run
    //GET  https://api.openai.com/v1/threads/{thread_id}/runs/{run_id}
    @Override
    public String retrieveRun(String threadId, String runId) {

        int max_count = 10;
        String status = "";
        try{
            ScheduleGetRun scheduleGetRun = new ScheduleGetRun(httpClientFactory, threadId, runId);
            ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
            for(int i=0; i<max_count;i++){
                ScheduledFuture<String> future = scheduledExecutorService.schedule(scheduleGetRun, 2, TimeUnit.SECONDS);
                status = future.get();
                if(status.equalsIgnoreCase("completed")){
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error occured while list messages {}", e.getMessage());
        }
        return status;
    }

    private class ScheduleGetRun implements Callable<String> {
        ChatGptHttpClientFactory httpClientFactory;
        String runId;
        String threadId;

        public ScheduleGetRun(ChatGptHttpClientFactory httpClientFactory, String threadId, String runId) {
            this.httpClientFactory = httpClientFactory;
            this.runId = runId;
            this.threadId = threadId;
        }

        @Override
        public String call() throws Exception {
            String responseString = StringUtils.EMPTY;
            try {
                responseString = httpClientFactory.getExecutor()
                        .execute(httpClientFactory.retrieveRun(threadId, runId))
                        .handleResponse(HANDLER);
            } catch (IOException e) {
                log.error("Error occured while list messages {}", e.getMessage());
            }
            JSONObject jsonObject = new JSONObject(responseString);
            String status = jsonObject.getString("status"); //?completed

            return status;
        }
    }

    //6. Retrive thread
    //GET  https://api.openai.com/v1/threads/{thread_id}
    @Override
    public String retrieveThread(String threadId) {
        String responseString = StringUtils.EMPTY;
        try {
            responseString = httpClientFactory.getExecutor()
                    .execute(httpClientFactory.retrieveThread(threadId))
                    .handleResponse(HANDLER);
        } catch (IOException e) {
            log.error("Error occured while list messages {}", e.getMessage());
        }
        log.debug("retrieveThread: {}", responseString);
        JSONObject jsonObject = new JSONObject(responseString);
        String respThreadId = jsonObject.getString("id"); //thread id
        return respThreadId;
    }

     //-----------------------------------
    //Functions to generate request body
    //----------------------------------


    //Generate Prompt for Complete API
    private String generatePrompt(String bodyText, int maxTokens) {
        SummaryBean bodyBean = new SummaryBean();
        if(maxTokens != 0) {
        	bodyBean.setMaxTokens(maxTokens);
        }
        bodyBean.setPrompt(bodyText);
        return jsonConverter.convertToJsonString(bodyBean);
    }

    //Generate body for Assistant API - Generate thread and run
    private String generateMessage_createThreadRun(String assistantId, String content, String role) {
        CreateThreadRun body = new CreateThreadRun(assistantId,content,role);
        return jsonConverter.convertToJsonString(body);
    }

    private String getCreateMessageBody(String role, String content){
        Message message = new Message();
        message.setContent(content);
        message.setRole(role);
        return jsonConverter.convertToJsonString(message);
    }

    private String getCreateRunBody(String assistantId){
        return "{\"assistant_id\":\"" + assistantId + "\"}";
    }
}
