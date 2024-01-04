package com.perficient.aem.sample.openai.core.servlets;

import com.perficient.aem.sample.openai.core.services.ChatGPTAPIService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.auth.core.AuthConstants;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Slf4j
@Component(service = { Servlet.class }, property = {
        "sling.servlet.paths=" + ChatGPTAssistantServlet.RESOURCE_PATH,
        "sling.servlet.methods=GET",
        AuthConstants.AUTH_REQUIREMENTS + "=-"+ ChatGPTAssistantServlet.RESOURCE_PATH})
public class ChatGPTAssistantServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    static final String RESOURCE_PATH = "/bin/assistantServlet";
    static final String CONTENT = "content";
    static final String ROLE = "role";

    @Reference
    private ChatGPTAPIService apiService;

    HttpSession session;

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        String content = request.getParameter(CONTENT);
        String role = request.getParameter(ROLE);
        JSONObject jsonObject = new JSONObject();

        try {
            session = request.getSession(true);
            session.setMaxInactiveInterval(30*60);
            String sessionThreadId = null;
            sessionThreadId = (String)session.getAttribute("thread_id");
            if(sessionThreadId == null || StringUtils.isEmpty(sessionThreadId)){
                jsonObject = createNewThreadRun(role,content);
            }
            else{
                //A threadId exist in session: retrieve the thread
                String respThreadId = apiService.retrieveThread(sessionThreadId);
                if(respThreadId != null && respThreadId.equalsIgnoreCase(sessionThreadId)){
                    //This is a valid thread.
                    jsonObject = addNewMesssageOnThreadThenRun(respThreadId, role, content);
                }
                else{
                    //Thread not exist anymore, need work as a new request:
                    jsonObject = createNewThreadRun(role,content);
                }


            }
        } catch (JSONException e) {
            log.error(e.getMessage());
        }
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().print(jsonObject);

    }

    private JSONObject createNewThreadRun(String role, String content){
        JSONObject jsonObject = new JSONObject();
        String result = apiService.creaetThreadAndRun(role, content);
        JSONObject resultJson = new JSONObject(result);
        String run_Id = resultJson.getString("id");
        String thread_id = resultJson.getString("thread_id");

        //Add thread to session
        session.setAttribute("thread_id", thread_id);
        jsonObject = retrieveRunAndGetAnswer(thread_id, run_Id);

        return jsonObject;
    }

    private JSONObject addNewMesssageOnThreadThenRun(String threadId, String role, String content){
        JSONObject jsonObject = new JSONObject();
        String messageId =  apiService.createMessage(threadId,role,content);
        if(messageId != null){
            //Message is created. create a run
            String runId = apiService.createRun(threadId);
            //retrieveRun and get answers
            if(runId != null){
                jsonObject = retrieveRunAndGetAnswer(threadId, runId);
            }
        }
        return jsonObject;

    }


    private JSONObject retrieveRunAndGetAnswer(String thread_id, String run_Id){

        JSONObject jsonObject = new JSONObject();
        String allAnswsers = "";
        //Retrieve Run to check status
        String status = apiService.retrieveRun(thread_id, run_Id);
        if(status.equalsIgnoreCase("completed")){
            //Run completed, need list messages
            String listMessageResponse = apiService.listMessages(thread_id);
            JSONObject listMessageResponseJson = new JSONObject(listMessageResponse);
            String lastMessage_id = listMessageResponseJson.getString("last_id");
            JSONArray messages =  listMessageResponseJson.getJSONArray("data");
            for(int i=0; i<messages.length(); i++){
                JSONObject aMessage = (JSONObject)messages.get(i);
                if(aMessage.getString("role").equalsIgnoreCase("assistant")){
                    //This is the answer?
                    JSONArray contentArray = aMessage.getJSONArray("content");
                    JSONObject aContent = (JSONObject)contentArray.get(0);
                    allAnswsers += aContent.getJSONObject("text").getString("value") + "\n";
                }
            }

            jsonObject.put("answer", allAnswsers);
        }

        return jsonObject;
    }
}
