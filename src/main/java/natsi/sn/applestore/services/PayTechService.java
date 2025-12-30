package natsi.sn.applestore.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayTechService {

    private final RestTemplate restTemplate;

    @Value("${paytech.api.url:https://paytech.sn/api}")
    private String apiUrl;

    @Value("${paytech.api.key:}")
    private String apiKey;

    @Value("${paytech.api.secret:}")
    private String apiSecret;

    public Map<String, Object> initiatePayment(Map<String, Object> paymentData) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("API_KEY", apiKey);
        headers.set("API_SECRET", apiSecret);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(paymentData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                apiUrl + "/payment/requestpayment",
                request,
                Map.class
        );

        return response.getBody();
    }

    public Map<String, Object> verifyPayment(String transactionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("API_KEY", apiKey);
        headers.set("API_SECRET", apiSecret);

        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl + "/payment/verify/" + transactionId,
                HttpMethod.GET,
                request,
                Map.class
        );

        return response.getBody();
    }
}

