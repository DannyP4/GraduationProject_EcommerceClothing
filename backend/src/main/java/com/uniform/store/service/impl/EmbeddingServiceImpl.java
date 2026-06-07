package com.uniform.store.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.uniform.store.config.GeminiProperties;
import com.uniform.store.exception.BadRequestException;
import com.uniform.store.service.EmbeddingService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final String TASK_QUERY = "RETRIEVAL_QUERY";
    private static final String TASK_DOCUMENT = "RETRIEVAL_DOCUMENT";
    private static final int FULL_DIM = 3072;

    private final GeminiProperties props;
    private final RestClient geminiRestClient;

    public EmbeddingServiceImpl(GeminiProperties props, RestClient geminiRestClient) {
        this.props = props;
        this.geminiRestClient = geminiRestClient;
    }

    @Override
    public float[] embedQuery(String text) {
        JsonNode resp = post(":embedContent", singleRequest(text, TASK_QUERY));
        return Vectors.normalize(parseValues(resp.path("embedding").path("values")));
    }

    @Override
    public List<float[]> embedDocuments(List<String> texts) {
        if (texts.isEmpty()) return List.of();
        String model = "models/" + props.getEmbeddingModel();
        List<Map<String, Object>> requests = new ArrayList<>(texts.size());
        for (String t : texts) {
            Map<String, Object> r = singleRequest(t, TASK_DOCUMENT);
            r.put("model", model);
            requests.add(r);
        }
        JsonNode resp = post(":batchEmbedContents", Map.of("requests", requests));
        JsonNode embeddings = resp.path("embeddings");
        if (!embeddings.isArray() || embeddings.size() != texts.size()) {
            throw new BadRequestException("Gemini batch embedding returned " + embeddings.size()
                    + " vectors for " + texts.size() + " inputs");
        }
        List<float[]> out = new ArrayList<>(texts.size());
        for (JsonNode e : embeddings) {
            out.add(Vectors.normalize(parseValues(e.path("values"))));
        }
        return out;
    }

    private Map<String, Object> singleRequest(String text, String taskType) {
        Map<String, Object> content = Map.of("parts", List.of(Map.of("text", text == null ? "" : text)));
        Map<String, Object> req = new HashMap<>();
        req.put("content", content);
        req.put("taskType", taskType);
        int dim = props.getEmbeddingDim();
        if (dim > 0 && dim < FULL_DIM) {
            req.put("outputDimensionality", dim);
        }
        return req;
    }

    private JsonNode post(String op, Object body) {
        URI uri = URI.create(props.getBaseUrl() + "/models/" + props.getEmbeddingModel() + op);
        return GeminiHttp.post(geminiRestClient, uri, body);
    }

    private float[] parseValues(JsonNode values) {
        if (!values.isArray() || values.isEmpty()) {
            throw new BadRequestException("Gemini embedding response missing values");
        }
        float[] v = new float[values.size()];
        for (int i = 0; i < v.length; i++) v[i] = (float) values.get(i).asDouble();
        return v;
    }
}
