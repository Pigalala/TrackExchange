package me.pigalala.trackexchange.butler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.pigalala.trackexchange.TrackExchange;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public final class ButlerFacade {

    public static Optional<byte[]> get(String name) {
        Optional<String> maybeUrl = TrackExchange.getButlerUrl();
        if (maybeUrl.isEmpty()) {
            TrackExchange.instance.getSLF4JLogger().warn("Attempted to fetch from butler, but the butler URL is empty. Please update the config");
            return Optional.empty();
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create(maybeUrl.get().concat("get/").concat(name)))
                .GET();
        TrackExchange.getButlerKey().ifPresent(key -> request.header("Authorization", "Key ".concat(key)));

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpResponse<String> response = httpClient.send(request.build(), r -> HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8));

            switch (response.statusCode()) {
                case 200 -> { /* Looks like nothing broke. Yippee */}
                case 401 -> {
                    TrackExchange.instance.getSLF4JLogger().warn("Could not fetch from Butler, as we are unauthorized.");
                    return Optional.empty();
                }
                case 500 -> {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    TrackExchange.instance.getSLF4JLogger().warn("Could not fetch from Butler, due to a remote error: {}", json.get("error").getAsString());
                    return Optional.empty();
                }
                default -> TrackExchange.instance.getSLF4JLogger().info("Requested {} from butler, and received code {}", name, response.statusCode());
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            byte[] decodedTrackExchange = Base64.getDecoder().decode(json.get("track_exchange").getAsString());
            return Optional.of(decodedTrackExchange);
        } catch (Exception e) {
            TrackExchange.instance.getSLF4JLogger().error("Could not fetch from butler", e);
            return Optional.empty();
        }
    }

    // Assumes that bytes is the bytes of a zipped trackexchange
    public static void upload(String name, byte[] bytes) {
        Optional<String> maybeUrl = TrackExchange.getButlerUrl();
        if (maybeUrl.isEmpty()) {
            TrackExchange.instance.getSLF4JLogger().warn("Attempted to upload to Butler, but the butler URL is empty. Please update the config");
            return;
        }

        var json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("track_exchange", Base64.getEncoder().encodeToString(bytes));

        var request = HttpRequest.newBuilder()
                .uri(URI.create(maybeUrl.get().concat("upload/")))
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()));
        TrackExchange.getButlerKey().ifPresent(key -> request.header("Authorization", "Key ".concat(key)));

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            httpClient.send(request.build(), r -> HttpResponse.BodySubscribers.discarding());
        } catch (Exception e) {
            TrackExchange.instance.getSLF4JLogger().error("Could not upload to butler", e);
        }

    }
}
