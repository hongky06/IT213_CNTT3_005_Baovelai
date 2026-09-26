package org.example.hackathon_de05.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class TracingConfig {
    @Bean(destroyMethod = "close")
    public SdkTracerProvider tracerProvider(
            @Value("${app.langfuse.enabled:false}") boolean enabled,
            @Value("${app.langfuse.otlp-endpoint:}") String endpoint,
            @Value("${app.langfuse.public-key:}") String publicKey,
            @Value("${app.langfuse.secret-key:}") String secretKey) {
        SdkTracerProviderBuilder provider = SdkTracerProvider.builder();
        if (enabled) {
            if (endpoint.isBlank() || publicKey.isBlank() || secretKey.isBlank()) {
                throw new IllegalStateException(
                        "Langfuse tracing requires LANGFUSE_OTLP_ENDPOINT, LANGFUSE_PUBLIC_KEY, and LANGFUSE_SECRET_KEY.");
            }
            String credentials = Base64.getEncoder().encodeToString(
                    (publicKey + ":" + secretKey).getBytes(StandardCharsets.UTF_8));
            OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
                    .setEndpoint(endpoint)
                    .addHeader("Authorization", "Basic " + credentials)
                    .build();
            provider.addSpanProcessor(BatchSpanProcessor.builder(exporter).build());
        }
        return provider.build();
    }

    @Bean
    public OpenTelemetry openTelemetry(SdkTracerProvider tracerProvider) {
        return OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
    }

    @Bean
    public Tracer applicationTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("de-005-parking-assistant");
    }
}
