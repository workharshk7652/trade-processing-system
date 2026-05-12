package com.trading.portfolio.repository;

import com.trading.portfolio.entity.EventStoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventStoreRepository extends JpaRepository<EventStoreEntity, Long> {

    // used by replay — read all TRADE_EXECUTED events in order
    List<EventStoreEntity> findByEventTypeOrderByCreatedAtAsc(String eventType);
}