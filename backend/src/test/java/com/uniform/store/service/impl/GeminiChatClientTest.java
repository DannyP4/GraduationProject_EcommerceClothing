package com.uniform.store.service.impl;

import com.uniform.store.config.GeminiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiChatClientTest {

    private MockRestServiceServer server;
    private GeminiChatClient client;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient rest = builder.build();
        GeminiProperties props = new GeminiProperties();
        props.setBaseUrl("https://gen.test/v1beta");
        props.setChatModel("gemini-2.5-flash");
        client = new GeminiChatClient(props, rest);
    }

    @Test
    void generate_sendsSystemAndContents_disablesThinking_parsesText() {
        server.expect(requestTo("https://gen.test/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("be helpful"))
                .andExpect(jsonPath("$.contents[0].role").value("user"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("hi"))
                .andExpect(jsonPath("$.generationConfig.thinkingConfig.thinkingBudget").value(0))
                .andRespond(withSuccess(
                        "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello \"},{\"text\":\"there\"}]}}]}",
                        APPLICATION_JSON));

        String out = client.generate("be helpful", List.of(new GeminiChatClient.Msg("user", "hi")));

        assertThat(out).isEqualTo("Hello there");
        server.verify();
    }

    @Test
    void generate_noCandidates_returnsNull() {
        server.expect(requestTo("https://gen.test/v1beta/models/gemini-2.5-flash:generateContent"))
                .andRespond(withSuccess("{\"candidates\":[]}", APPLICATION_JSON));

        assertThat(client.generate("sys", List.of(new GeminiChatClient.Msg("user", "hi")))).isNull();
        server.verify();
    }

    @Test
    void generateWithTools_sendsToolDeclarations_parsesFunctionCall() {
        server.expect(requestTo("https://gen.test/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.tools[0].functionDeclarations[0].name").value("find_similar_products"))
                .andExpect(jsonPath("$.toolConfig.functionCallingConfig.mode").value("AUTO"))
                .andRespond(withSuccess(
                        "{\"candidates\":[{\"content\":{\"parts\":[{\"functionCall\":"
                                + "{\"name\":\"find_similar_products\",\"args\":{\"product_name\":\"Black Jacket\"},\"id\":\"abc\"}}]}}]}",
                        APPLICATION_JSON));

        GeminiChatClient.FunctionDecl decl = new GeminiChatClient.FunctionDecl(
                "find_similar_products", "desc", java.util.Map.of("type", "object"));
        GeminiChatClient.Reply reply = client.generateWithTools(
                "sys", List.of(GeminiChatClient.Content.text("user", "similar?")), List.of(decl));

        assertThat(reply.isCall()).isTrue();
        assertThat(reply.functionCall().name()).isEqualTo("find_similar_products");
        assertThat(reply.functionCall().args().path("product_name").asText()).isEqualTo("Black Jacket");
        assertThat(reply.functionCall().id()).isEqualTo("abc");
        server.verify();
    }
}
