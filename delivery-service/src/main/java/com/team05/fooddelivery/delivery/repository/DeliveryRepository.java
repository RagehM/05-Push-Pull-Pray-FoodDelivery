package com.team05.fooddelivery.delivery.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.team05.fooddelivery.delivery.model.Delivery;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

	Optional<Delivery> findByOrderId(Long orderId);

	@Query(value = "SELECT * FROM deliveries d WHERE " +
			"(CAST(:status AS varchar) IS NULL OR d.status = CAST(:status AS varchar)) " +
			"ORDER BY d.updated_at DESC",
		nativeQuery = true)
	List<Delivery> findByStatus(@Param("status") String status);

	@Query(value = "SELECT * FROM deliveries d WHERE d.order_id = :orderId AND " +
			"(CAST(:status AS varchar) IS NULL OR d.status = CAST(:status AS varchar)) " +
			"ORDER BY d.updated_at DESC LIMIT 1",
		nativeQuery = true)
	Optional<Delivery> findByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") String status);

	@Query(value = """
    SELECT
		 d.id,
		 d.driver_name,
		 d.order_id,
		 (d.latitude)::numeric AS latitude,
		 (d.longitude)::numeric AS longitude,
		 (
			 SQRT(
				 POWER((d.latitude)::numeric - :lat, 2) +
				 POWER((d.longitude)::numeric - :lon, 2)
			 ) * 111
		 ) AS distanceKm
	FROM deliveries d
    WHERE d.status IN ('ASSIGNED', 'PICKED_UP', 'IN_TRANSIT')
    AND (
        SQRT(
            POWER((d.latitude)::numeric - :lat, 2) +
            POWER((d.longitude)::numeric - :lon, 2)
        ) * 111
    ) <= :radiusKm
    ORDER BY distanceKm ASC
	""", nativeQuery = true)
	List<Object[]> findNearbyDeliveries(
			Double lat,
			Double lon,
			Double radiusKm
	);

	@Query("""
    SELECT d
    FROM Delivery d
    WHERE d.orderId = :orderId
      AND (:start IS NULL OR d.updatedAt >= :start)
      AND (:end IS NULL OR d.updatedAt <= :end)
    ORDER BY d.updatedAt ASC
""")
	List<Delivery> findOrderDeliveryHistory(
			@Param("orderId") Long orderId,
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end
	);

	//// Check for the existence of order
	@Query(value = "SELECT COUNT(*) > 0 FROM orders WHERE id = :orderId", nativeQuery = true)
	boolean orderExists(@Param("orderId") Long orderId);

	@Query(value = "SELECT * FROM deliveries d WHERE d.order_id = :orderId ORDER BY d.updated_at DESC LIMIT 1", nativeQuery = true)
	Optional<Delivery> findLatestByOrderId(@Param("orderId") Long orderId);

	@Query(value = "SELECT * FROM deliveries d WHERE d.metadata ->> :key = :value", nativeQuery = true)
	List<Delivery> findByMetadataEquals(@Param("key") String key, @Param("value") String value);

	@Query(value = "SELECT * FROM deliveries d WHERE CAST(d.metadata ->> :key AS numeric) > CAST(:value AS numeric)", nativeQuery = true)
	List<Delivery> findByMetadataGreaterThan(@Param("key") String key, @Param("value") String value);

	@Query(value = "SELECT * FROM deliveries d WHERE CAST(d.metadata ->> :key AS numeric) < CAST(:value AS numeric)", nativeQuery = true)
	List<Delivery> findByMetadataLessThan(@Param("key") String key, @Param("value") String value);
	@Query(value = """
    SELECT
        d.id,
        d.driver_name,
        d.order_id,
        d.latitude::numeric,
        d.longitude::numeric,
        (d.metadata->>'estimatedArrival')::numeric,
        d.updated_at
    FROM deliveries d
    WHERE d.status IN ('ASSIGNED', 'PICKED_UP', 'IN_TRANSIT')
      AND (d.metadata->>'estimatedArrival')::numeric > :maxEstimatedArrival
      AND d.updated_at >= NOW() - (:sinceMinutes * INTERVAL '1 minute')
""", nativeQuery = true)
	List<Object[]> findDelayedDeliveries(
			@Param("maxEstimatedArrival") Double maxEstimatedArrival,
			@Param("sinceMinutes") int sinceMinutes
	);

	@Query(value = "SELECT COUNT(*) FROM deliveries d WHERE " +
			"d.status = :status AND " +
			"d.updated_at < CAST(:cutoff AS timestamp)",
		nativeQuery = true)
	long countOldByStatus(@Param("status") String status, @Param("cutoff") java.time.LocalDateTime cutoff);

	@Modifying
	@Transactional
	@Query(value = "DELETE FROM deliveries d WHERE " +
			"d.status = :status AND " +
			"d.updated_at < CAST(:cutoff AS timestamp)",
		nativeQuery = true)
	int deleteOldByStatus(@Param("status") String status, @Param("cutoff") java.time.LocalDateTime cutoff);
	
	@Query(value = """
    SELECT
        d.driver_name,
        COUNT(*),
        AVG((d.metadata->>'speed')::numeric),
        MAX((d.metadata->>'speed')::numeric),
        MIN(d.updated_at),
        MAX(d.updated_at)
    FROM deliveries d
    WHERE d.driver_name = :driverName
      AND d.updated_at >= :start
      AND d.updated_at <= :end
    GROUP BY d.driver_name
""", nativeQuery = true)
	List<Object[]> findPerformanceSummary(
			@Param("driverName") String driverName,
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end
	);

	@Query(value = """
        SELECT COUNT(*)
        FROM deliveries d
        JOIN orders o ON d.order_id = o.id
        WHERE (CAST(:start AS timestamp) IS NULL OR o.order_date >= CAST(:start AS timestamp))
          AND (CAST(:end AS timestamp) IS NULL OR o.order_date <= CAST(:end AS timestamp))
        """, nativeQuery = true)
	Number countTotalDeliveries(
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end
	);


	@Query(value = """
        SELECT AVG(minutes_taken)
        FROM (
            SELECT DISTINCT o.id,
                   EXTRACT(EPOCH FROM
                      (o.delivered_at - o.order_date)
                   ) / 60.0 AS minutes_taken
            FROM deliveries d
            JOIN orders o ON d.order_id = o.id
            WHERE o.status = 'DELIVERED'
              AND o.delivered_at IS NOT NULL
              AND (CAST(:start AS timestamp) IS NULL OR o.order_date >= CAST(:start AS timestamp))
              AND (CAST(:end AS timestamp) IS NULL OR o.order_date <= CAST(:end AS timestamp))
        ) x
        """, nativeQuery = true)
	Number averageDeliveryMinutes(
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end
	);


	@Query(value = """
        SELECT COUNT(DISTINCT o.id)
        FROM deliveries d
        JOIN orders o ON d.order_id = o.id
        WHERE o.status = 'DELIVERED'
          AND (CAST(:start AS timestamp) IS NULL OR o.order_date >= CAST(:start AS timestamp))
          AND (CAST(:end AS timestamp) IS NULL OR o.order_date <= CAST(:end AS timestamp))
        """, nativeQuery = true)
	Number countDeliveredOrders(
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end
	);


	@Query(value = """
        SELECT COUNT(DISTINCT o.id)
        FROM deliveries d
        JOIN orders o ON d.order_id = o.id
        WHERE o.status = 'DELIVERED'
          AND o.delivered_at IS NOT NULL
          AND EXTRACT(EPOCH FROM
              (o.delivered_at - o.order_date)
          ) / 60.0 <= 45
          AND (CAST(:start AS timestamp) IS NULL OR o.order_date >= CAST(:start AS timestamp))
          AND (CAST(:end AS timestamp) IS NULL OR o.order_date <= CAST(:end AS timestamp))
        """, nativeQuery = true)
	Number countOnTimeDeliveredOrders(
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end
	);


	@Query(value = """
        SELECT d.status, COUNT(*)
        FROM deliveries d
        JOIN orders o ON d.order_id = o.id
        WHERE (CAST(:start AS timestamp) IS NULL OR o.order_date >= CAST(:start AS timestamp))
          AND (CAST(:end AS timestamp) IS NULL OR o.order_date <= CAST(:end AS timestamp))
        GROUP BY d.status
        """, nativeQuery = true)
	List<Object[]> countByStatus(
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end
	);
}
