package com.perficient.aem.sample.openai.core.servlets;

import com.perficient.aem.sample.openai.core.services.ChatGPTAPIService;
import com.day.cq.wcm.api.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
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
        "sling.servlet.paths=" + ChatGPTAPIServlet.RESOURCE_PATH,
        "sling.servlet.methods=GET",
        AuthConstants.AUTH_REQUIREMENTS + "=-"+ChatGPTAPIServlet.RESOURCE_PATH})
public class ChatGPTAPIServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    static final String RESOURCE_PATH = "/bin/chatGPTAPIServlet";
    static final String PAGE_PATH = "pagePath";
    static final String DESC_TOKEN = "descToken";
    private static final String TEASER_DESCRIPTION = "jcr:description";
    private static final String TEASER_TITLE = "jcr:title";

    @Reference
    private ChatGPTAPIService apiInvoker;

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        String pagePath = request.getParameter(PAGE_PATH);
        String descTokenParam = request.getParameter(DESC_TOKEN);
        int descToken = 0;
        JSONObject jsonObject = new JSONObject();
        if(StringUtils.isNoneEmpty(pagePath)) {
            Resource pageResource = request.getResourceResolver().resolve(pagePath);
            if(!ResourceUtil.isNonExistingResource(pageResource)) {
                Page page = pageResource.adaptTo(Page.class);
                String prompt = "";
                String teaserDescription = page.getProperties().get(TEASER_DESCRIPTION, StringUtils.EMPTY);
                if( teaserDescription != null && StringUtils.isNotEmpty(teaserDescription)){
                    prompt = teaserDescription;
                }
                else{
                    prompt = page.getProperties().get(TEASER_TITLE, StringUtils.EMPTY);
                }
                try {
                	if( StringUtils.isNotEmpty(prompt) && StringUtils.isNotEmpty(descTokenParam)) {
                        jsonObject.put(TEASER_DESCRIPTION, apiInvoker.invokeAPI(teaserDescription, descToken));
                	}

                } catch (JSONException e) {
                    log.error(e.getMessage());
                }
                response.setContentType("text/html; charset=UTF-8");
                response.getWriter().print(jsonObject);
            }
        }
    }
}
