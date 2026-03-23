package com.bank.transfersearch.kafka;

import com.bank.transfersearch.entity.ContactDocument;
import com.bank.transfersearch.search.ContactSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContactConsumer {

    private final ContactSearchRepository searchRepository;

    @KafkaListener(topics = "contact-sync", groupId = "transfer-search-group")
    public void consumeContactSyncEvent(ContactDocument document) {
        try {
            log.info("Received contact sync event for user: {}, contact: {}", document.getUserId(),
                    document.getContactName());
            searchRepository.save(document);
            log.info("Successfully synced contact to Elasticsearch");
        } catch (Exception e) {
            log.error("Error syncing contact to Elasticsearch", e);
        }
    }
}
