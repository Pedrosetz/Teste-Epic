package org.example;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class request {
    private final String BASE_URL ="https://store-site-backend-static-ipv4.ak.epicgames.com";

    private final String ENDPOINT = "/freeGamesPromotions?locale=pt-BR&country=BR&allowCountries=BR";

    private final HttpClient CLIENT;

public request() {
    CLIENT = HttpClient.newHttpClient();
}

public JsonArray create() throws IOException, InterruptedException {

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + '/' + ENDPOINT))
            .GET()
            .build();

    HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println("Status Code: " + response.statusCode());

    Gson gson = new Gson();
    JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
    JsonArray freeGames = new JsonArray();

    JsonObject data = jsonResponse.getAsJsonObject("data");
    JsonObject catalog = data.getAsJsonObject("Catalog");
    JsonObject searchStore = catalog.getAsJsonObject("searchStore");
    JsonArray elements = searchStore.getAsJsonArray("elements");

    for (JsonElement element : elements){
        JsonObject promo = element.getAsJsonObject();
        if (promo.has("status") && promo.get("status").getAsString().equals("ACTIVE")) {
            JsonObject priceObject = promo.getAsJsonObject("price").getAsJsonObject("totalPrice");
            double discountPrice = priceObject.get("discountPrice").getAsDouble();
            if (discountPrice == 0.0) {
                freeGames.add(promo);
            }
        }
    }

    return freeGames;

}
    public static void main(String[] args) {
        String webhookUrl = "https://discord.com/api/webhooks/1268001667920429118/RUjphTYtMzbJYvZ1oZDZakkT4HFMu2PryrfSwWjHkVJbokc3p4TKKbnOUVh-G0LCVol8";

        try {
            request req = new request();
            JsonArray freeGames = req.create();

            if (freeGames.size() > 0) {
                StringBuilder messageBuilder = new StringBuilder("Jogos gratuitos disponíveis:\n");
                for (JsonElement gameElement : freeGames) {
                    JsonObject game = gameElement.getAsJsonObject();
                    String title = getValueAsString(game, "title");
                    String description = getValueAsString(game, "description");
                    String productSlug = getValueAsString(game, "productSlug");
                    String url = productSlug != null ? "https://www.epicgames.com/store/pt-BR/p/" + productSlug : "URL não disponível";

                    String gameInfo = String.format("**%s**\n%s\n[Link para o jogo](%s)\n\n", title, description, url);

                    if (messageBuilder.length() + gameInfo.length() > 2000) {
                        sendDiscordMessage(webhookUrl, messageBuilder.toString());
                        messageBuilder.setLength(0);
                        messageBuilder.append(gameInfo);
                    } else {
                        messageBuilder.append(gameInfo);
                    }
                }
                if (messageBuilder.length() > 0) {
                    sendDiscordMessage(webhookUrl, messageBuilder.toString());
                }
            } else {
                System.out.println("Nenhum jogo gratuito disponível no momento.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getValueAsString(JsonObject jsonObject, String key) {
        JsonElement element = jsonObject.get(key);
        if (element != null && !element.isJsonNull()) {
            return element.getAsString();
        }
        return "";
    }

    private static void sendDiscordMessage(String webhookUrl, String message) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        JsonObject json = new JsonObject();
        json.addProperty("content", message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 204) {
            System.out.println("Mensagem enviada para o Discord com sucesso!");
        } else {
            System.out.println("Falha ao enviar mensagem. Código de status: " + response.statusCode());
            System.out.println("Resposta do servidor: " + response.body());
        }
    }
}


