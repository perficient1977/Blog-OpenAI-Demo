package com.perficient.aem.sample.openai.core.servlets;

import com.perficient.aem.sample.openai.core.services.ChatGPTAPIService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.auth.core.AuthConstants;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

@Slf4j
@Component(service = { Servlet.class }, property = {
        "sling.servlet.paths=" + ChatGPTAPIServletV2.RESOURCE_PATH,
        "sling.servlet.methods=GET",
        AuthConstants.AUTH_REQUIREMENTS + "=-"+ ChatGPTAPIServletV2.RESOURCE_PATH})
public class ChatGPTAPIServletV2 extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    static final String RESOURCE_PATH = "/bin/chatGPTAPIServlet2";
    static final String PROMPT = "prompt";
    static final String DESC_TOKEN = "descToken";
    private static final String TEASER_DESCRIPTION = "jcr:description";

    @Reference
    private ChatGPTAPIService apiInvoker;

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        String chatGPTPrompt = request.getParameter(PROMPT);
        String descTokenParam = request.getParameter(DESC_TOKEN);
        int descToken = 0;
        JSONObject jsonObject = new JSONObject();
        if(StringUtils.isNoneEmpty(chatGPTPrompt)) {
                try {
                	if( StringUtils.isNotEmpty(chatGPTPrompt) && StringUtils.isNotEmpty(descTokenParam)) {
                		descToken = Integer.parseInt(descTokenParam);
                        jsonObject.put(TEASER_DESCRIPTION, apiInvoker.invokeAPI(chatGPTPrompt, descToken));
                	}
                } catch (JSONException e) {
                    log.error(e.getMessage());
                }
                response.setContentType("text/html; charset=UTF-8");
                response.getWriter().print(jsonObject);
        }
    }
}
