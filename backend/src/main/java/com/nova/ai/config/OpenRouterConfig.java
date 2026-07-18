package com.nova.ai.config;

import com.nova.ai.AiCopilotException;
import com.nova.ai.port.AiChatGateway;
import com.nova.ai.port.OpenRouterChatGateway;
import com.nova.common.exception.ErrorCode;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Wires Nova's AI Copilot onto OpenRouter through Spring AI's provider-agnostic
 * {@link ChatClient} API. OpenRouter speaks the OpenAI-compatible chat-completions
 * protocol, so the same {@link OpenAiChatModel} used for OpenAI works by pointing its
 * base URL at {@code https://openrouter.ai/api} (Spring AI appends {@code /v1/chat/completions}).
 *
 * <p><b>Optional by design.</b> The application excludes the starter's
 * {@code OpenAiChatAutoConfiguration} (see {@code NovaApplication}) because its
 * {@code openAiApi} bean requires a usable API key at construction time and would
 * otherwise crash the whole context when no key is set. This configuration therefore
 * builds the {@link OpenAiChatModel} itself, <em>only when a real key is present</em>.
 * When no key is present it exposes a non-LLM {@link AiChatGateway} that always reports
 * itself unavailable and answers with a friendly "not configured" message — the app
 * boots and every other feature keeps working.</p>
 *
 * <p>The produced gateway bean is the only AI type the rest of the domain depends on:
 * {@code CopilotService} injects the {@link AiChatGateway} interface, never Spring AI
 * or OpenRouter directly. Swapping providers later means a new {@link AiChatGateway}
 * implementation behind the same contract — no other AI class needs to change.</p>
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class OpenRouterConfig {

    public OpenRouterConfig(AiProperties properties,
            @Value("${nova.ai.api-key:}") String providerApiKey) {
        // Capture whether a real provider key is present (the placeholder used in
        // local/dev is treated as "not configured" so the copilot degrades gracefully
        // instead of attempting a doomed call).
        properties.setApiKey(providerApiKey);
    }

    @Bean
    public AiChatGateway aiChatGateway(AiProperties properties) {
        if (!properties.isConfigured()) {
            return unavailableGateway();
        }
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(properties.getModel())
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .build();
        // Use java.net.HttpURLConnection (via SimpleClientHttpRequestFactory) rather than
        // the default Reactor Netty client. Netty's async DNS resolver ignores
        // java.net.preferIPv4Stack and can stall on an IPv6 AAAA record when the host has
        // no IPv6 route; HttpURLConnection honours the flag and connects promptly. Explicit
        // connect/read timeouts also guarantee a stuck call fails fast into a friendly
        // AI_TIMEOUT instead of hanging the request thread indefinitely.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(20));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);
        OpenAiApi api = new OpenAiApi.Builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .restClientBuilder(restClientBuilder)
                .build();
        ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder().build();
        OpenAiChatModel chatModel = new OpenAiChatModel(
                api, options,
                toolCallingManager,
                RetryTemplate.defaultInstance(),
                ObservationRegistry.NOOP);
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        return new OpenRouterChatGateway(chatClient, properties);
    }

    /** Non-LLM gateway used when no provider key is configured. */
    private AiChatGateway unavailableGateway() {
        return new AiChatGateway() {
            @Override
            public String generate(String systemInstruction, java.util.List<String> conversation) {
                throw new AiCopilotException(ErrorCode.AI_UNAVAILABLE,
                        "The AI assistant is not configured on this server.");
            }

            @Override
            public boolean isAvailable() {
                return false;
            }
        };
    }
}
