package com.nova.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Nova-specific AI settings (kept separate from the provider's own
 * {@code spring.ai.openai.*} keys so the model defaults live in one place).
 *
 * <p>Instantiated by {@link OpenRouterConfig#OpenRouterConfig(AiProperties, String)}
 * which also captures whether a provider key is present; the gateway uses that to
 * decide whether a real call can succeed. When no key is present the copilot still
 * starts and answers with a friendly "not configured" message rather than crashing.</p>
 */
@ConfigurationProperties(prefix = "nova.ai")
public class AiProperties {

    /** OpenAI-compatible chat model id (OpenRouter model slug). */
    private String model = "deepseek/deepseek-chat-v3-0324:free";

    /** Lower temperature keeps answers grounded and less inventive. */
    private double temperature = 0.2;

    /** Caps the length of an explanation. */
    private int maxTokens = 1024;

    /** OpenRouter base URL (OpenAI-compatible chat-completions endpoint). */
    private String baseUrl = "https://openrouter.ai/api/v1";

    /** Message shown when the model is unreachable or unconfigured. */
    private String fallbackMessage =
            "I can't reach the AI service right now. Your financial data is safe — please try again in a moment.";

    private boolean apiKeyPresent;

    /** The raw provider key, captured only so the gateway can build a real client. */
    private String apiKey;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getFallbackMessage() {
        return fallbackMessage;
    }

    public void setFallbackMessage(String fallbackMessage) {
        this.fallbackMessage = fallbackMessage;
    }

    public boolean isApiKeyPresent() {
        return apiKeyPresent;
    }

    public void setApiKey(String apiKey) {
        // A placeholder is used so the provider's auto-configuration can still wire
        // up without a real key; treat it as "not configured" so the copilot reports
        // graceful unavailability instead of attempting a doomed call.
        boolean present = StringUtils.hasText(apiKey)
                && !"unconfigured-placeholder-key".equals(apiKey);
        this.apiKeyPresent = present;
        this.apiKey = present ? apiKey : null;
    }

    /** The raw provider key, or {@code null} when not configured. */
    public String getApiKey() {
        return apiKey;
    }

    /** True when a model key is configured and a call can realistically succeed. */
    public boolean isConfigured() {
        return apiKeyPresent;
    }
}
