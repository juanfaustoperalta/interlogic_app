package com.interlogic.app.kafka.producer;

import com.interlogic.app.kafka.Topic;
import com.interlogic.app.utils.MapperHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Producer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MapperHelper mapperHelper;

    public Producer(KafkaTemplate<String, String> kafkaTemplate, MapperHelper mapperHelper) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapperHelper = mapperHelper;
    }

    public void sendMessage(Topic topicName, Object msg) {
        String json = mapperHelper.toJson(msg);
        log.info("sending message : {}", json);
        kafkaTemplate.send(topicName.name(), json);
    }
}
