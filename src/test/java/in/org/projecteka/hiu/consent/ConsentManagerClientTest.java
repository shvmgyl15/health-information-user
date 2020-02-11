package in.org.projecteka.hiu.consent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.org.projecteka.hiu.ConsentManagerServiceProperties;
import in.org.projecteka.hiu.HiuProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import static org.assertj.core.api.Assertions.assertThat;


import static in.org.projecteka.hiu.consent.TestBuilders.consentCreationResponse;
import static in.org.projecteka.hiu.consent.TestBuilders.consentRepresentation;

public class ConsentManagerClientTest {
    private ConsentManagerClient consentManagerClient;
    private MockWebServer mockWebServer;

    @BeforeEach
    public void init() {
        mockWebServer = new MockWebServer();
        WebClient.Builder webClientBuilder = WebClient.builder();
        ConsentManagerServiceProperties consentManagerServiceProperties =
                new ConsentManagerServiceProperties(mockWebServer.url("").toString());
        HiuProperties hiuProperties = new HiuProperties("10000005", "Max Health Care", "localhost:8080");
        consentManagerClient = new ConsentManagerClient(webClientBuilder, consentManagerServiceProperties, hiuProperties);

    }

    @Test
    public void shouldCreateConsentRequest() throws JsonProcessingException, InterruptedException {
        String consentRequestId = "consent-request-id";
        var consentCreationResponse = consentCreationResponse().id(consentRequestId).build();
        var consentCreationResponseJson = new ObjectMapper().writeValueAsString(consentCreationResponse);
        var consentRepresentation = consentRepresentation().build();
        mockWebServer.enqueue( new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(consentCreationResponseJson));


        StepVerifier.create(consentManagerClient.createConsentRequest(consentRepresentation))
                .assertNext(
                        response -> {
                            assertThat(response.getId()).isEqualTo(consentCreationResponse.getId());
                })
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getRequestUrl().toString()).isEqualTo(mockWebServer.url("") + "consent-requests");
        assertThat(recordedRequest.getBody().readUtf8())
                .isEqualTo(new ObjectMapper().writeValueAsString(consentRepresentation));
    }
}