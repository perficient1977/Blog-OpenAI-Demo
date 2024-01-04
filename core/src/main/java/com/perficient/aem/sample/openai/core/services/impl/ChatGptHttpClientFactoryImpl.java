package com.perficient.aem.sample.openai.core.services.impl;

import com.perficient.aem.sample.openai.core.services.ChatGptHttpClientFactory;
import com.perficient.aem.sample.openai.core.services.config.ChatGptHttpClientFactoryConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of @{@link ChatGptHttpClientFactory}.
 * <p>
 * HttpClientFactory provides service to handle API connection and executor.
 */
@Slf4j
@Component(service = ChatGptHttpClientFactory.class)
@Designate(ocd = ChatGptHttpClientFactoryConfig.class, factory = true)
public class ChatGptHttpClientFactoryImpl implements ChatGptHttpClientFactory {

    private Executor executor;
    private String baseUrl;
    private CloseableHttpClient httpClient;
    private ChatGptHttpClientFactoryConfig config;

    @Reference
    private HttpClientBuilderFactory httpClientBuilderFactory;

    @Activate
    @Modified
    protected void activate(ChatGptHttpClientFactoryConfig config) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        log.info("########### OSGi Configs Start ###############");
        log.info("API Host Name : {}", config.apiHostName());
        log.info("URI Type: {}", config.uriType());
        log.info("########### OSGi Configs End ###############");
        closeHttpConnection();
        this.config = config;
        if (this.config.apiHostName() == null) {
            log.debug("Configuration is not valid. Both hostname is mandatory.");
            throw new IllegalArgumentException("Configuration is not valid. Both hostname is mandatory.");
        }
        this.baseUrl = StringUtils.join(this.config.apiHostName(), this.config.uriType());
        initExecutor();
    }

    private void initExecutor() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        PoolingHttpClientConnectionManager connMgr = null;
        RequestConfig requestConfig = initRequestConfig();
        HttpClientBuilder builder = httpClientBuilderFactory.newBuilder();
        builder.setDefaultRequestConfig(requestConfig);
        if (config.relaxedSSL()) {
            connMgr = initPoolingConnectionManagerWithRelaxedSSL();
        } else {
            connMgr = new PoolingHttpClientConnectionManager();
        }
        connMgr.closeExpiredConnections();
        connMgr.setMaxTotal(config.maxTotalOpenConnections());
        connMgr.setDefaultMaxPerRoute(config.maxConcurrentConnectionPerRoute());
        builder.setConnectionManager(connMgr);
        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("Content-Type", "application/json"));
        headers.add(new BasicHeader("Authorization", "Bearer " + config.apiKey()));
        headers.add(new BasicHeader("OpenAI-Beta", "assistants=v1"));
        builder.setDefaultHeaders(headers);
        builder.setKeepAliveStrategy(keepAliveStratey);
        httpClient = builder.build();
        executor = Executor.newInstance(httpClient);
    }

    private PoolingHttpClientConnectionManager initPoolingConnectionManagerWithRelaxedSSL()
            throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        PoolingHttpClientConnectionManager connMgr;
        SSLContextBuilder sslbuilder = new SSLContextBuilder();
        sslbuilder.loadTrustMaterial(new TrustAllStrategy());
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslbuilder.build(),
                NoopHostnameVerifier.INSTANCE);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslsf).build();
        connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        return connMgr;
    }

    private RequestConfig initRequestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(config.defaultConnectionTimeout())))
                .setSocketTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(config.defaultSocketTimeout())))
                .setConnectionRequestTimeout(
                        Math.toIntExact(TimeUnit.SECONDS.toMillis(config.defaultConnectionRequestTimeout())))
                .build();
    }

    @Deactivate
    protected void deactivate() {
        closeHttpConnection();
    }

    private void closeHttpConnection() {
        if (null != httpClient) {
            try {
                httpClient.close();
            } catch (final IOException exception) {
                log.debug("IOException while clossing API, {}", exception.getMessage());
            }
        }
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @Override
    public ChatGptHttpClientFactoryConfig getConfig(){
        return this.config;
    }


    @Override
    public Request post() {
        return Request.Post(baseUrl);
    }

    @Override
    public Request postCreateThreadRun() {
        String url = config.apiHostName();
        url += "/v1/threads/runs";
        return Request.Post(url);
    }

    @Override
    public Request listMessages(String threadId) {
        String url = config.apiHostName();
        url += "/v1/threads/"+threadId+"/messages";
        return Request.Get(url);
    }

    @Override
    public Request postCreateMessage(String threadId) {
        String url = config.apiHostName();
        url += "/v1/threads/"+threadId+"/messages";
        return Request.Post(url);
    }

    @Override
    public Request postCreateRun(String threadId) {
        String url = config.apiHostName();
        url += "/v1/threads/"+threadId+"/runs";
        return Request.Post(url);
    }

    @Override
    public Request retrieveRun(String threadId, String runId) {
        String url = config.apiHostName();
        url += "/v1/threads/"+threadId+"/runs/"+runId;
        return Request.Get(url);
    }

    @Override
    public Request retrieveThread(String threadId) {
        String url = config.apiHostName();
        url += "/v1/threads/"+threadId;
        return Request.Get(url);
    }

    ConnectionKeepAliveStrategy keepAliveStratey = new ConnectionKeepAliveStrategy() {

        @Override
        public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
            /*
             * HeaderElementIterator headerElementIterator = new BasicHeaderElementIterator(
             * response.headerIterator(HTTP.CONN_KEEP_ALIVE));
             *
             * while (headerElementIterator.hasNext()) { HeaderElement headerElement =
             * headerElementIterator.nextElement(); String param = headerElement.getName();
             * String value = headerElement.getValue(); if (value != null &&
             * param.equalsIgnoreCase("timeout")) { return
             * TimeUnit.SECONDS.toMillis(Long.parseLong(value)); } }
             */

            return TimeUnit.SECONDS.toMillis(config.defaultKeepAliveconnection());
        }
    };
}
