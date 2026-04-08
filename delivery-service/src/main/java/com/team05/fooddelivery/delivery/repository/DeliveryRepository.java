package com.team05.fooddelivery.delivery.repository;

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
			"(CAST(:status AS deliverystatus) IS NULL OR d.status = CAST(:status AS deliverystatus)) " +
			"ORDER BY d.updated_at DESC",
		nativeQuery = true)
	List<Delivery> findByStatus(@Param("status") String status);

	@Query(value = "SELECT * FROM deliveries d WHERE d.order_id = :orderId AND " +
			"(CAST(:status AS deliverystatus) IS NULL OR d.status = CAST(:status AS deliverystatus)) " +
			"ORDER BY d.updated_at DESC LIMIT 1",
		nativeQuery = true)
	Optional<Delivery> findByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") String status);

	@Query(value = "SELECT COUNT(*) FROM deliveries d WHERE " +
			"d.status = CAST(:status AS deliverystatus) AND " +
			"d.updated_at < CAST(:cutoff AS timestamp)",
		nativeQuery = true)
	long countOldByStatus(@Param("status") String status, @Param("cutoff") java.time.LocalDateTime cutoff);

	@Modifying
	@Transactional
	@Query(value = "DELETE FROM deliveries d WHERE " +
			"d.status = CAST(:status AS deliverystatus) AND " +
			"d.updated_at < CAST(:cutoff AS timestamp)",
		nativeQuery = true)
	int deleteOldByStatus(@Param("status") String status, @Param("cutoff") java.time.LocalDateTime cutoff);
}
