package com.marketplace.reputation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReputationRepository extends JpaRepository<Reputation, Long> {

    Optional<Reputation> findByShopId(Long shopId);
}
