package dogapi;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * BreedFetcher implementation that relies on the dog.ceo API.
 * Note that all failures get reported as BreedNotFoundException
 * exceptions to align with the requirements of the BreedFetcher interface.
 */
public class DogApiBreedFetcher implements BreedFetcher {
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Fetch the list of sub breeds for the given breed from the dog.ceo API.
     * @param breed the breed to fetch sub breeds for
     * @return list of sub breeds for the given breed
     * @throws BreedNotFoundException if the breed does not exist (or if the API call fails for any reason)
     */
    @Override
    public List<String> getSubBreeds(String breed) throws BreedFetcher.BreedNotFoundException {
        if (breed == null || breed.trim().isEmpty()) {
            throw new BreedFetcher.BreedNotFoundException("Breed cannot be null or empty");
        }

        String normalized = breed.trim().toLowerCase(Locale.ROOT);
        String url = "https://dog.ceo/api/breed/" + normalized + "/list";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response == null || !response.isSuccessful() || response.body() == null) {
                int code = (response == null) ? -1 : response.code();
                throw new BreedFetcher.BreedNotFoundException("HTTP error while fetching breed '" + normalized + "': " + code);
            }

            String body = response.body().string();
            JSONObject root = new JSONObject(body);

            String status = root.optString("status", "");
            if ("error".equalsIgnoreCase(status)) {
                // Example: {"status":"error","message":"Breed not found (main breed does not exist)","code":404}
                String msg = root.optString("message", "Unknown API error");
                throw new BreedFetcher.BreedNotFoundException(msg);
            }

            if (!"success".equalsIgnoreCase(status)) {
                throw new BreedFetcher.BreedNotFoundException("Unexpected API status: " + status);
            }

            JSONArray arr = root.getJSONArray("message");
            List<String> subBreeds = new ArrayList<>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                subBreeds.add(arr.getString(i));
            }
            return Collections.unmodifiableList(subBreeds);
        } catch (IOException e) {
            // Per interface contract, turn IO issues into BreedNotFoundException
            throw new BreedFetcher.BreedNotFoundException("Failed to call Dog API for breed '" + breed + "'", e);
        } catch (Exception e) {
            // JSON or other unexpected issues -> wrap as specified
            throw new BreedFetcher.BreedNotFoundException("Unexpected error processing API response for breed '" + breed + "': " + e.getMessage(), e);
        }
    }
}
