package com.octopus;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    private final AtomicInteger count = new AtomicInteger(0);

    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("quote")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        try {
            final List<String> authors = load("/authors.txt");
            final List<String> quotes = load("/quotes.txt");
            final int randomIndex = new Random().nextInt(authors.size());

            final String json = "{\"quote\": \"" + quotes.get(randomIndex) + "\", " +
                    "\"author\": \"" + authors.get(randomIndex) + "\", " +
                    "\"appVersion\": \"" + getVersion() + "\", " +
                    "\"environmentName\": \"AWS Lambda\", " +
                    "\"quoteCount\": \"" + count.getAndIncrement() + "\" " +
                    "}";

            return request.createResponseBuilder(HttpStatus.OK).body(json).build();
        } catch (Exception ex) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(ex.toString()).build();
        }
    }

    private List<String> load(final String path) {
        try (
                final InputStream inputStream = this.getClass().getResourceAsStream(path);
                final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                final Stream<String> lines = bufferedReader.lines()
        ) {
            return lines.collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return List.of("");
        }
    }

    private String getVersion() {
        try {
            final InputStream resourceAsStream = this.getClass().getResourceAsStream(
                    "/META-INF/maven/com.octopus/randomquotesapi-azure/pom.properties");
            final Properties props = new Properties();
            props.load(resourceAsStream);
            return props.get("version").toString();
        } catch (final Exception e) {
            return "unknown";
        }
    }
}
