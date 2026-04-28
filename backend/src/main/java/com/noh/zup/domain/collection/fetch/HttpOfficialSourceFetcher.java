package com.noh.zup.domain.collection.fetch;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class HttpOfficialSourceFetcher implements OfficialSourceFetcher {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String USER_AGENT = "ZupOfficialSourceCollector/1.0";

    private final HttpClient httpClient;

    public HttpOfficialSourceFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public FetchResult fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .version(HttpClient.Version.HTTP_1_1)
                .timeout(TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return FetchResult.success(statusCode, response.body());
            }
            return FetchResult.failure(statusCode, "HTTP status " + statusCode);
        } catch (IOException exception) {
            return FetchResult.failure(0, "I/O error while fetching source: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return FetchResult.failure(0, "Fetch interrupted");
        } catch (IllegalArgumentException exception) {
            return FetchResult.failure(0, "Invalid source URL: " + exception.getMessage());
        }
    }
}
