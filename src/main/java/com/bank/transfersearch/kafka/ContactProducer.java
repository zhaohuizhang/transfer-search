package com.bank.transfersearch.kafka;

import com.bank.transfersearch.entity.ContactDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContactProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "contact-sync";

    public void sendContactSyncEvent(ContactDocument document) {
        log.info("Sending contact sync event for user: {}, contact: {}", document.getUserId(),
                document.getContactName());
        kafkaTemplate.send(TOPIC, String.valueOf(document.getUserId()), document);
    }
}
