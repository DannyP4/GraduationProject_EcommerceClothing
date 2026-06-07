package com.uniform.store.service.impl;

import com.uniform.store.config.GeminiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmbeddingServiceImplTest {

    private MockRestServiceServer server;
    private EmbeddingServiceImpl service;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        GeminiProperties props = new GeminiProperties();
        props.setBaseUrl("https://gen.test/v1beta");
        props.setEmbeddingModel("gemini-embedding-001");
        props.setEmbeddingDim(3072);
        service = new EmbeddingServiceImpl(props, client);
    }

    @Test
    void embedQuery_usesRetrievalQueryTaskType_andNormalizes() {
        server.expect(requestTo("https://gen.test/v1beta/models/gemini-embedding-001:embedContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.taskType").value("RETRIEVAL_QUERY"))
                .andExpect(jsonPath("$.outputDimensionality").doesNotExist())
                .andRespond(withSuccess("{\"embedding\":{\"values\":[0.6,0.8]}}", APPLICATION_JSON));

        float[] v = service.embedQuery("black jacket");

        assertThat(v).hasSize(2);
        assertThat(v[0]).isEqualTo(0.6f, within(1e-4f));
        assertThat(Math.sqrt(v[0] * v[0] + v[1] * v[1])).isCloseTo(1.0, within(1e-5));
        server.verify();
    }

    @Test
    void embedDocuments_usesRetrievalDocumentTaskType_andReturnsAllVectors() {
        server.expect(requestTo("https://gen.test/v1beta/models/gemini-embedding-001:batchEmbedContents"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.requests.length()").value(2))
                .andExpect(jsonPath("$.requests[0].taskType").value("RETRIEVAL_DOCUMENT"))
                .andExpect(jsonPath("$.requests[0].model").value("models/gemini-embedding-001"))
                .andRespond(withSuccess(
                        "{\"embeddings\":[{\"values\":[1,0]},{\"values\":[0,1]}]}", APPLICATION_JSON));

        List<float[]> out = service.embedDocuments(List.of("a navy shirt", "a red dress"));

        assertThat(out).hasSize(2);
        assertThat(out.get(0)[0]).isEqualTo(1.0f, within(1e-4f));
        assertThat(out.get(1)[1]).isEqualTo(1.0f, within(1e-4f));
        server.verify();
    }

    @Test
    void embedDocuments_emptyInput_skipsHttpCall() {
        assertThat(service.embedDocuments(List.of())).isEmpty();
        server.verify();
    }
}
