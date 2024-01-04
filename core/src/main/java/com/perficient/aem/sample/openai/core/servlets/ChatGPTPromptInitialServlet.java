package com.perficient.aem.sample.openai.core.servlets;

import com.day.cq.tagging.Tag;
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

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

@Slf4j
@Component(service = { Servlet.class }, property = {
        "sling.servlet.paths=" + ChatGPTPromptInitialServlet.RESOURCE_PATH,
        "sling.servlet.methods=GET",
        AuthConstants.AUTH_REQUIREMENTS + "=-"+ ChatGPTPromptInitialServlet.RESOURCE_PATH})
public class ChatGPTPromptInitialServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    static final String RESOURCE_PATH = "/bin/initalPrompt";
    static final String PAGE_PATH = "pagePath";
    private static final String PROPERTY_DESCRIPTION = "jcr:description";
    private static final String PROPERTY_TITLE = "jcr:title";
    private static final String PROPERTY_TAGS = "jcr:tags";


    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        String pagePath = request.getParameter(PAGE_PATH);
        JSONObject jsonObject = new JSONObject();


        if(StringUtils.isNoneEmpty(pagePath)) {
            Resource pageResource = request.getResourceResolver().resolve(pagePath);
            if(!ResourceUtil.isNonExistingResource(pageResource)) {
                Page page = pageResource.adaptTo(Page.class);
                String pageDescription = page.getProperties().get(PROPERTY_DESCRIPTION, StringUtils.EMPTY);
                String pageTitle = page.getProperties().get(PROPERTY_TITLE, StringUtils.EMPTY);
                Tag[] tags = page.getTags();
                try {
                    jsonObject.put("pageDescription", pageDescription);
                    jsonObject.put("pageTitle", pageTitle);
                    jsonObject.put("pageTags", getTagValue(tags));
                } catch (JSONException e) {
                    log.error(e.getMessage());
                }
                response.setContentType("text/html; charset=UTF-8");
                response.getWriter().print(jsonObject);
            }
        }
    }

    private String getTagValue(Tag[] tags){
        String result = "";
        if(tags.length > 0){
            for (int i=0; i< tags.length; i++){
                result += tags[i].getTitle();
                if(i< (tags.length-1)){
                    result += ", ";
                }
            }
        }
        return result;
    }
}
