package com.perficient.aem.sample.openai.core.services;

public interface ChatGPTAPIService {

    public String invokeAPI(String bodyText, int maxTokens);
    public String creaetThreadAndRun(String role, String content);

    public String listMessages(String threadId);
    public String createMessage(String threadId,String role, String content);
    public String createRun(String threadId);
    public String retrieveRun(String threadId, String runId);

    String retrieveThread(String threadId);
}
