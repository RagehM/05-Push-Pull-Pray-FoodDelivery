package com.team05.fooddelivery.delivery.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.team05.fooddelivery.delivery.model.Delivery;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

	Optional<Delivery> findByOrderId(Long orderId);

	@Query(value = "SELECT * FROM deliveries d WHERE " +
			"(CAST(:status AS deliverystatus) IS NULL OR d.status = CAST(:status AS deliverystatus)) " +
			"ORDER BY d.updated_at DESC",
		nativeQuery = true)
	List<Delivery> findByStatus(@Param("status") String status);

	@Query(value = "SELECT * FROM deliveries d WHERE d.order_id = :orderId AND " +
			"(CAST(:status AS deliverystatus) IS NULL OR d.status = CAST(:status AS deliverystatus)) " +
			"ORDER BY d.updated_at DESC LIMIT 1",
		nativeQuery = true)
	Optional<Delivery> findByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") String status);

	@Query(value = """
    SELECT
		 d.id,
		 d.driver_name,
		 d.order_id,
		 (d.metadata->>'latitude')::numeric AS latitude,
		 (d.metadata->>'longitude')::numeric AS longitude,
		 (
			 SQRT(
				 POWER((d.metadata->>'latitude')::numeric - :lat, 2) +
				 POWER((d.metadata->>'longitude')::numeric - :lon, 2)
			 ) * 111
		 ) AS distanceKm
	FROM deliveries d
    WHERE d.status IN ('ASSIGNED', 'PICKED_UP', 'IN_TRANSIT')
    AND (
        SQRT(
            POWER((d.metadata->>'latitude')::numeric - :lat, 2) +
            POWER((d.metadata->>'longitude')::numeric - :lon, 2)
        ) * 111
    ) <= :radiusKm
    ORDER BY distanceKm ASC
	""", nativeQuery = true)
	List<Object[]> findNearbyDeliveries(
			Double lat,
			Double lon,
			Double radiusKm
	);

	List<Delivery> findByOrderIdAndUpdatedAtBetweenOrderByUpdatedAtAsc(
			Long orderId,
			LocalDateTime start,
			LocalDateTime end
	);

	//// Check for the existence of order
	@Query(value = "SELECT COUNT(*) > 0 FROM orders WHERE id = :orderId", nativeQuery = true)
	boolean orderExists(@Param("orderId") Long orderId);
}
