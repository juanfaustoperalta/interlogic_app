package com.interlogic.app.integrations;


import com.interlogic.app.dtos.PhoneDTO;
import com.interlogic.app.kafka.Topic;
import com.interlogic.app.kafka.consumer.FixPhoneValidationConsumer;
import com.interlogic.app.kafka.producer.Producer;
import com.interlogic.app.model.Phone;
import com.interlogic.app.model.Status;
import com.interlogic.app.repository.PhoneRepository;
import com.interlogic.app.services.PhoneValidatorService;
import com.interlogic.app.utils.MapperHelper;
import org.junit.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@EmbeddedKafka
@RunWith(SpringRunner.class)
@Sql(value = "classpath:migration/init.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "classpath:migration/destroy.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
public class PhoneValidatorServiceTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private MapperHelper mapperHelper;

    @SpyBean
    private Producer producer;

    @SpyBean
    private FixPhoneValidationConsumer consumer;

    @Autowired
    private PhoneRepository repository;

    @Autowired
    private PhoneValidatorService service;

    @Captor
    ArgumentCaptor<Phone> phoneArgumentCaptor;

    @Captor
    ArgumentCaptor<Topic> topicArgumentCaptor;

    @Captor
    ArgumentCaptor<String> stringArgumentCaptor;

    @Test
    public void uploadAFile() throws IOException {

        MultipartFile file = getFile("example1.csv");

        service.process(file);

        List<Phone> phones = repository.findAll();

        List<Phone> phoneCorrected = phones.stream().filter(phoneTmp ->
                phoneTmp.getStatus().equals(Status.CORRECT)).collect(Collectors.toList());

        assertEquals(phoneCorrected.size(), 1);
        var phone = phoneCorrected.get(0);
        assertEquals(phone.getPhone(), "27371679142");
        assertEquals(phone.getExternalId(), Long.valueOf("103343262"));

        List<Phone> phoneIncorrectLenght = phones.stream().filter(phoneTmp ->
                phoneTmp.getStatus().equals(Status.INCORRECT_LENGTH_NUMBER)).collect(Collectors.toList());
        assertEquals(phoneIncorrectLenght.size(), 1);
        phone = phoneIncorrectLenght.get(0);
        assertEquals(phone.getPhone(), "273716791");
        assertEquals(phone.getExternalId(), Long.valueOf("103426541"));
    }

    @Test
    public void uploadFileAndInteractedWithKafka() throws IOException, InterruptedException {

        MultipartFile file = getFile("example2.csv");

        service.process(file);

        verify(producer, times(1)).sendMessage(topicArgumentCaptor.capture(),
                phoneArgumentCaptor.capture());

        TimeUnit.MILLISECONDS.sleep(1000);

        verify(consumer, timeout(5000).times(1)).listenFixPhoneValidationTopic(stringArgumentCaptor.capture());

        repository.findAll();

        Phone phone = phoneArgumentCaptor.getValue();
        Topic topic = topicArgumentCaptor.getValue();
        PhoneDTO phoneDTOMsg = mapperHelper.toObject(stringArgumentCaptor.getValue());

        assertEquals(phone.getPhone(), phoneDTOMsg.getPhone());
        assertEquals(topic, Topic.FIX_PHONE);
    }

    private MultipartFile getFile(String name) throws IOException {
        return new MockMultipartFile(name, name, "text/csv",
                getClass().getClassLoader().getResourceAsStream(name));
    }


}
