package com.team05.fooddelivery.delivery.adapter;

import com.team05.fooddelivery.delivery.dto.DeliveryEventDTO;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.Map;

public class MongoDocumentAdapter {

    @SuppressWarnings("unchecked")
    public DeliveryEventDTO adapt(Document document) {
        Map<String, Object> details = document.get("details", Map.class);
        return new DeliveryEventDTO(
                document.getObjectId("_id") != null ? document.getObjectId("_id").toString() : null,
                document.getLong("deliveryId"),
                document.getString("action"),
                document.get("timestamp", LocalDateTime.class),
                details
        );
    }
}
