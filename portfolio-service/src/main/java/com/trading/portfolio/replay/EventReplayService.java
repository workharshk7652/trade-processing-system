package com.trading.portfolio.replay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.common.events.TradeExecutedEvent;
import com.trading.portfolio.entity.EventStoreEntity;
import com.trading.portfolio.repository.EventStoreRepository;
import com.trading.portfolio.service.PortfolioService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventReplayService {

    private final EventStoreRepository eventStoreRepository;
    private final PortfolioService portfolioService;
    private final ObjectMapper objectMapper;

    /*
     * @PostConstruct = runs automatically on startup, after Spring context ready.
     *
     * WHY THIS EXISTS:
     * portfolio_holdings table is derived state — computed from trades.
     * If service crashes and Postgres data is lost or corrupted,
     * we read every TRADE_EXECUTED from event_store (immutable)
     * and replay them top-to-bottom to rebuild exact portfolio state.
     *
     * This is the core value of event sourcing.
     */
    @PostConstruct
    public void replayOnStartup() {
        log.info("Starting event replay from event store...");

        List<EventStoreEntity> tradeEvents = eventStoreRepository
                .findByEventTypeOrderByCreatedAtAsc("TRADE_EXECUTED");

        if (tradeEvents.isEmpty()) {
            log.info("No TRADE_EXECUTED events found. Fresh start.");
            return;
        }

        log.info("Replaying {} TRADE_EXECUTED events...", tradeEvents.size());

        int replayed = 0;
        int failed = 0;

        for (EventStoreEntity storedEvent : tradeEvents) {
            try {
                TradeExecutedEvent tradeEvent = objectMapper.readValue(
                        storedEvent.getPayload(), TradeExecutedEvent.class);

                portfolioService.processTrade(tradeEvent);
                replayed++;

            } catch (Exception e) {
                log.error("Failed to replay event id={} error={}",
                        storedEvent.getId(), e.getMessage());
                failed++;
            }
        }

        log.info("Replay complete. replayed={} failed={}", replayed, failed);
    }
}