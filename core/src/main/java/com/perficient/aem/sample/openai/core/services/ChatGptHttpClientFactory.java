package com.perficient.aem.sample.openai.core.services;

import com.perficient.aem.sample.openai.core.services.config.ChatGptHttpClientFactoryConfig;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;

/**
 * Factory for building pre-configured HttpClient Fluent Executor and Request objects
 * based a configure host, port and (optionally) username/password.
 *
 * Factories will generally be accessed by service lookup using the factory.name property.
 */
public interface ChatGptHttpClientFactory {
    Executor getExecutor();
    Request post();
    Request postCreateThreadRun();

    Request listMessages(String threadId);

    ChatGptHttpClientFactoryConfig getConfig();

    Request postCreateMessage(String threadId);

    Request postCreateRun(String threadId);

    Request retrieveRun(String threadId, String runId);

    Request retrieveThread(String threadId);
}
