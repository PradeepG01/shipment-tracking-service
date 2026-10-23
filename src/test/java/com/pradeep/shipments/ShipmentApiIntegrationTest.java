package com.pradeep.shipments;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ShipmentApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private static final String NEW_SHIPMENT = """
            {"senderName": "Kumar Traders", "recipientName": "Asha Rao",
             "originPincode": "560001", "destinationPincode": "110001", "weightKg": 2.5}
            """;

    private String createShipment() throws Exception {
        String body = mvc.perform(post("/api/v1/shipments").contentType(MediaType.APPLICATION_JSON).content(NEW_SHIPMENT))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/shipments/LQ")))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.events", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(body);
        return json.get("trackingNumber").asText();
    }

    private String statusUpdate(String status, String location) {
        return "{\"status\": \"" + status + "\", \"location\": \"" + location + "\"}";
    }

    @Test
    void createsAndTracksAShipmentThroughDelivery() throws Exception {
        String tn = createShipment();
        for (String[] step : new String[][] {
                {"PICKED_UP", "Bengaluru"}, {"IN_TRANSIT", "Nagpur"}, {"OUT_FOR_DELIVERY", "Delhi"}, {"DELIVERED", "Delhi"}}) {
            mvc.perform(patch("/api/v1/shipments/{tn}/status", tn)
                            .contentType(MediaType.APPLICATION_JSON).content(statusUpdate(step[0], step[1])))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(step[0]));
        }
        mvc.perform(get("/api/v1/shipments/{tn}", tn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events", hasSize(5)))
                .andExpect(jsonPath("$.events[4].status").value("DELIVERED"));
    }

    @Test
    void lookupIsCaseInsensitive() throws Exception {
        String tn = createShipment();
        mvc.perform(get("/api/v1/shipments/{tn}", tn.toLowerCase())).andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidStatusTransitionWithConflict() throws Exception {
        String tn = createShipment();
        mvc.perform(patch("/api/v1/shipments/{tn}/status", tn)
                        .contentType(MediaType.APPLICATION_JSON).content(statusUpdate("DELIVERED", "Delhi")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid status transition"));
    }

    @Test
    void validatesRequestBody() throws Exception {
        String invalid = """
                {"senderName": "", "recipientName": "Asha", "originPincode": "012345",
                 "destinationPincode": "110001", "weightKg": 0}
                """;
        mvc.perform(post("/api/v1/shipments").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.senderName").exists())
                .andExpect(jsonPath("$.errors.originPincode").exists())
                .andExpect(jsonPath("$.errors.weightKg").exists());
    }

    @Test
    void returnsNotFoundForUnknownTrackingNumber() throws Exception {
        mvc.perform(get("/api/v1/shipments/LQ9999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Shipment not found"));
    }

    @Test
    void listsShipmentsFilteredByStatusWithPagination() throws Exception {
        String tn = createShipment();
        mvc.perform(patch("/api/v1/shipments/{tn}/status", tn)
                        .contentType(MediaType.APPLICATION_JSON).content(statusUpdate("CANCELLED", "Bengaluru")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/shipments").param("status", "CANCELLED").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.items[0].status").value("CANCELLED"));
        mvc.perform(get("/api/v1/shipments").param("size", "500")).andExpect(status().isBadRequest());
    }

    @Test
    void exposesHealthEndpoint() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
