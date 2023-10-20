package com.interlogic.app.services;


import com.google.common.collect.Lists;
import com.interlogic.app.dtos.PhoneDTO;
import com.interlogic.app.exceptions.Error;
import com.interlogic.app.exceptions.InterlogicException;
import com.interlogic.app.kafka.Topic;
import com.interlogic.app.kafka.producer.Producer;
import com.interlogic.app.model.Correction;
import com.interlogic.app.model.Phone;
import com.interlogic.app.model.Status;
import com.interlogic.app.properties.BusinessProperty;
import com.interlogic.app.repository.PhoneRepository;
import com.interlogic.app.services.impl.PhoneValidatorServiceImpl;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PhoneValidatorServiceTest {

    @InjectMocks
    private PhoneValidatorServiceImpl service;

    @Mock
    private PhoneRepository phoneRepository;

    @Mock
    private Producer producer;

    @Mock
    private BusinessProperty businessProperty;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void processAFileWithTwoRowsFirstValidAndSecondInvalid_thenSaveOneValidPhoneAndOneInvalidPhone() throws IOException {
        MultipartFile file = getFile("example1.csv");
        Class<ArrayList<Phone>> listClass = (Class<ArrayList<Phone>>) (Class) ArrayList.class;
        ArgumentCaptor<ArrayList<Phone>> repositoryArgumentCaptor = ArgumentCaptor.forClass(listClass);

        doReturn(Lists.newArrayList()).when(phoneRepository).saveAll(any());
        doReturn(11).when(businessProperty).getLengthOfPhone();
        doReturn("27").when(businessProperty).getCountryCode();


        service.process(file);

        verify(producer, never()).sendMessage(any(), any());
        verify(phoneRepository, times(1)).saveAll(repositoryArgumentCaptor.capture());

        ArrayList<Phone> allValues = repositoryArgumentCaptor.getValue();

        List<Phone> phoneCorrected = allValues.stream().filter(phoneTmp ->
                phoneTmp.getStatus().equals(Status.CORRECT)).collect(Collectors.toList());

        assertEquals(phoneCorrected.size(), 1);
        var phone = phoneCorrected.get(0);
        assertEquals(phone.getPhone(), "27371679142");
        assertEquals(phone.getExternalId(), Long.valueOf("103343262"));

        List<Phone> phoneIncorrectLenght = allValues.stream().filter(phoneTmp ->
                phoneTmp.getStatus().equals(Status.INCORRECT_LENGTH_NUMBER)).collect(Collectors.toList());

        assertEquals(phoneIncorrectLenght.size(), 1);
        phone = phoneIncorrectLenght.get(0);
        assertEquals(phone.getPhone(), "273716791");
        assertEquals(phone.getExternalId(), Long.valueOf("103426541"));
    }

    @Test
    public void processAFileWithOnePossibleToFixPhone_thenSaveAndSentKafkaMsg() throws IOException {
        MultipartFile file = getFile("example2.csv");
        Class<ArrayList<Phone>> listClass = (Class<ArrayList<Phone>>) (Class) ArrayList.class;
        ArgumentCaptor<ArrayList<Phone>> repositoryArgumentCaptor = ArgumentCaptor.forClass(listClass);
        ArgumentCaptor<Topic> topicArgumentCaptor = ArgumentCaptor.forClass(Topic.class);
        ArgumentCaptor<Phone> phoneArgumentCaptor = ArgumentCaptor.forClass(Phone.class);


        doReturn(Lists.newArrayList()).when(phoneRepository).saveAll(any());
        doReturn(11).when(businessProperty).getLengthOfPhone();

        service.process(file);

        verify(producer, times(1)).sendMessage(topicArgumentCaptor.capture(),
                phoneArgumentCaptor.capture());
        verify(phoneRepository, times(1)).saveAll(repositoryArgumentCaptor.capture());

        ArrayList<Phone> allValues = repositoryArgumentCaptor.getValue();

        List<Phone> phoneCorrected = allValues.stream().filter(phoneTmp ->
                phoneTmp.getStatus().equals(Status.POSSIBLE_TO_FIX)).collect(Collectors.toList());

        Topic value = topicArgumentCaptor.getValue();
        Phone phoneToMsg = phoneArgumentCaptor.getValue();

        assertEquals(value, Topic.FIX_PHONE);
        assertEquals(phoneCorrected.size(), 1);
        var phone = phoneCorrected.get(0);
        assertEquals(phone.getPhone(), phoneToMsg.getPhone());
        assertEquals(phone.getId(), phoneToMsg.getId());
        assertEquals(phone.getStatus(), phoneToMsg.getStatus());
    }

    @Test
    public void handleAFixablePhoneAndDontExist_thenThrowCorrectException() {
        var phoneDTO = PhoneDTO.builder().externalId(103343262L).phone("27371679142_DELETED_1239123")
                .status(Status.POSSIBLE_TO_FIX).build();
        doReturn(Optional.empty()).when(phoneRepository).findById(eq(phoneDTO.getId()));

        Throwable throwable = assertThrows(InterlogicException.class, () -> service.fixPhone(phoneDTO));

        assertEquals(throwable.getMessage(), Error.PHONE_NOT_FOUND.name());
    }

    @Test
    public void handleAFixablePhone_thenFixedUp() {
        var phoneDTO = PhoneDTO.builder().externalId(103343262L).phone("27371679142_DELETED_1239123")
                .status(Status.POSSIBLE_TO_FIX).build();
        var phone = Phone.builder().externalId(103343262L).phone("27371679142_DELETED_1239123")
                .status(Status.POSSIBLE_TO_FIX).build();

        doReturn(Optional.of(phone)).when(phoneRepository).findById(eq(phoneDTO.getId()));
        doReturn(Phone.builder().build()).when(phoneRepository).save(any());
        doReturn(11).when(businessProperty).getLengthOfPhone();
        doReturn("27").when(businessProperty).getCountryCode();


        ArgumentCaptor<Phone> phoneArgumentCaptor = ArgumentCaptor.forClass(Phone.class);

        service.fixPhone(phoneDTO);

        verify(phoneRepository, times(1)).save(phoneArgumentCaptor.capture());

        Phone phoneCaptured = phoneArgumentCaptor.getValue();
        assertEquals(phoneCaptured.getPhone(), "27371679142");
        assertEquals(phoneCaptured.getId(), phone.getId());
        assertEquals(phoneCaptured.getStatus(), Status.FIXED_UP);
        assertEquals(phoneCaptured.getCorrections().size(), 1);
        Correction corrections = phoneCaptured.getCorrections().get(0);
        assertEquals(corrections.getWhatDidDo(), "DELETED NON NUMERIC CHARACTER");
    }

    @Test
    public void processASingleFixablePhoneDTO_thenReturnCorrectedPhone() {

        ArgumentCaptor<Phone> repositoryArgumentCaptor = ArgumentCaptor.forClass(Phone.class);
        doReturn(Phone.builder().build()).when(phoneRepository).save(any());
        doReturn(11).when(businessProperty).getLengthOfPhone();
        doReturn("27").when(businessProperty).getCountryCode();


        service.validate(PhoneDTO.builder().externalId(103343262L).phone("27371679142_DELETED_120312").build());

        verify(phoneRepository, times(1)).save(repositoryArgumentCaptor.capture());

        Phone phoneValidated = repositoryArgumentCaptor.getValue();

        assertEquals(phoneValidated.getPhone(), "27371679142");
        assertEquals(phoneValidated.getExternalId(), Long.valueOf("103343262"));
        assertEquals(phoneValidated.getStatus(), Status.FIXED_UP);
        assertEquals(phoneValidated.getCorrections().get(0).getWhatDidDo(), "DELETED NON NUMERIC CHARACTER");
    }

    @Test
    public void processASingleCorrectPhoneDTO_thenReturnCorrectedInformation() {

        ArgumentCaptor<Phone> repositoryArgumentCaptor = ArgumentCaptor.forClass(Phone.class);
        doReturn(Phone.builder().build()).when(phoneRepository).save(any());
        doReturn(11).when(businessProperty).getLengthOfPhone();
        doReturn("27").when(businessProperty).getCountryCode();


        service.validate(PhoneDTO.builder().externalId(103343262L).phone("27371679142").build());

        verify(phoneRepository, times(1)).save(repositoryArgumentCaptor.capture());

        Phone phoneValidated = repositoryArgumentCaptor.getValue();

        assertEquals(phoneValidated.getPhone(), "27371679142");
        assertEquals(phoneValidated.getExternalId(), Long.valueOf("103343262"));
        assertEquals(phoneValidated.getStatus(), Status.CORRECT);
        assertNull(phoneValidated.getCorrections());
    }

    @Test
    public void processASingleShorterPhoneDTO_thenReturnCorrectedInformation() {

        ArgumentCaptor<Phone> repositoryArgumentCaptor = ArgumentCaptor.forClass(Phone.class);
        doReturn(Phone.builder().build()).when(phoneRepository).save(any());
        doReturn(11).when(businessProperty).getLengthOfPhone();


        service.validate(PhoneDTO.builder().externalId(103343262L).phone("2737167912").build());

        verify(phoneRepository, times(1)).save(repositoryArgumentCaptor.capture());

        Phone phoneValidated = repositoryArgumentCaptor.getValue();

        assertEquals(phoneValidated.getPhone(), "2737167912");
        assertEquals(phoneValidated.getExternalId(), Long.valueOf("103343262"));
        assertEquals(phoneValidated.getStatus(), Status.INCORRECT_LENGTH_NUMBER);
        assertNull(phoneValidated.getCorrections());
    }

    @Test
    public void processASingleInvalidContryCodePhoneDTO_thenReturnCorrectedInformation() {

        ArgumentCaptor<Phone> repositoryArgumentCaptor = ArgumentCaptor.forClass(Phone.class);
        doReturn(Phone.builder().build()).when(phoneRepository).save(any());
        doReturn(11).when(businessProperty).getLengthOfPhone();
        doReturn("27").when(businessProperty).getCountryCode();


        service.validate(PhoneDTO.builder().externalId(103343262L).phone("26378167912").build());

        verify(phoneRepository, times(1)).save(repositoryArgumentCaptor.capture());

        Phone phoneValidated = repositoryArgumentCaptor.getValue();

        assertEquals(phoneValidated.getPhone(), "26378167912");
        assertEquals(phoneValidated.getExternalId(), Long.valueOf("103343262"));
        assertEquals(phoneValidated.getStatus(), Status.INCORRECT_COUNTRY_CODE);
        assertNull(phoneValidated.getCorrections());
    }


    private MultipartFile getFile(String name) throws IOException {
        return new MockMultipartFile(name,
                name,
                "text/csv",
                getClass().getClassLoader().getResourceAsStream(name));
    }


}