package com.team05.fooddelivery.delivery.adapter;

import com.team05.fooddelivery.delivery.dto.DeliveryTrackingDTO;
import com.team05.fooddelivery.delivery.model.cassandra.DeliveryTrackingEvent;

public class CassandraRowAdapter {

    public DeliveryTrackingDTO adapt(DeliveryTrackingEvent row) {
        return new DeliveryTrackingDTO(
                row.getTimestamp(),
                row.getStatus(),
                row.getDriverName(),
                row.getLatitude(),
                row.getLongitude(),
                row.getNotes()
        );
    }
}
