package com.team05.fooddelivery.order.adapter;

import java.time.ZoneId;

import org.bson.Document;
import java.util.Map;

import com.team05.fooddelivery.order.dto.OrderEventDTO;

public class MongoDocumentAdapter {

    @SuppressWarnings("unchecked")
    public static OrderEventDTO adapt(Document document) {
        Map<String, Object> details = document.get("details", Map.class);
        return new OrderEventDTO.Builder()
                .id(document.getObjectId("_id").toString())
                .orderId(document.getLong("orderId"))
                .action(document.getString("action"))
                .payload(details)
                .timestamp(document.getDate("timestamp").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .build();
    }
}