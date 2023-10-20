package com.interlogic.app.kafka.consumer;

import com.interlogic.app.services.PhoneValidatorService;
import com.interlogic.app.utils.MapperHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FixPhoneValidationConsumer {

    private final PhoneValidatorService service;
    private final MapperHelper mapperHelper;

    public FixPhoneValidationConsumer(PhoneValidatorService service, MapperHelper mapperHelper) {
        this.service = service;
        this.mapperHelper = mapperHelper;
    }

    @KafkaListener(topics = "FIX_PHONE", groupId = "interlogic")
    public void listenFixPhoneValidationTopic(String msg) {
        log.info("handle a message : {}", msg);
        service.fixPhone(mapperHelper.toObject(msg));
    }

}
