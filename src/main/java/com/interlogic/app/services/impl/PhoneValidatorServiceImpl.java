package com.interlogic.app.services.impl;

import com.google.common.collect.Lists;
import com.interlogic.app.dtos.CorrectionDTO;
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
import com.interlogic.app.services.PhoneValidatorService;
import com.interlogic.app.utils.CSVHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Slf4j
public class PhoneValidatorServiceImpl implements PhoneValidatorService {

    private final PhoneRepository repository;
    private final Producer producer;
    private final BusinessProperty businessProperty;

    public PhoneValidatorServiceImpl(PhoneRepository repository, Producer producer, BusinessProperty businessProperty) {
        this.repository = repository;
        this.producer = producer;
        this.businessProperty = businessProperty;
    }

    @Override
    public void process(MultipartFile file) {
        log.info("[phoneValidatorService.process] init process");
        if (CSVHelper.hasCSVFormat(file)) {
            try {
                var phoneDTOS = CSVHelper.csvToPhoneValidationDTO(file.getInputStream());
                log.info("[phoneValidatorService.process] {} phones to process", phoneDTOS.size());
                List<Phone> phones = this.buildPhones(phoneDTOS);
                repository.saveAll(phones);
                this.sendToFix(phones);
            } catch (IOException e) {
                throw new InterlogicException(Error.ERROR_TO_PARSING_CSV, e);
            }
            log.info("[phoneValidatorService.process] finish process");
        } else {
            throw new InterlogicException(Error.UNCOMPATIBLE_FORMAT);
        }
    }

    @Override
    public List<PhoneDTO> getInfo() {
        var phoneList = repository.findAll();
        return this.buildPhoneDTOs(phoneList);
    }

    @Override
    public void fixPhone(PhoneDTO phoneDTO) {
        var phone = repository.findById(phoneDTO.getId()).orElseThrow(() -> new InterlogicException(Error.PHONE_NOT_FOUND));
        //split sequence for _ splitter
        Optional<String> phoneOpt = this.fixPhone(phone.getPhone());
        if (phoneOpt.isPresent()) {
            phone.setPhone(phoneOpt.get());
            phone.setStatus(Status.FIXED_UP);
            phone.setCorrections(Lists.newArrayList(Correction.builder().whatDidDo("DELETED NON NUMERIC CHARACTER").build()));
        } else {
            phone.setStatus(Status.UNFIXABLE);
        }
        repository.save(phone);
    }

    private Optional<String> fixPhone(String phone) {
        String[] phoneSequence = phone.split("_");
        Optional<String> phoneOpt = Arrays.stream(phoneSequence).findAny().filter(phoneTmp -> this.validateLight(phoneTmp).equals(Status.CORRECT));
        return phoneOpt;
    }

    @Override
    public PhoneDTO validate(PhoneDTO phoneDTO) {
        Phone phone = buildPhone(phoneDTO);
        if (phone.getStatus().equals(Status.POSSIBLE_TO_FIX)) {
            Optional<String> phoneOpt = this.fixPhone(phone.getPhone());
            if (phoneOpt.isPresent()) {
                phone.setPhone(phoneOpt.get());
                phone.setStatus(Status.FIXED_UP);
                phone.setCorrections(Lists.newArrayList(Correction.builder()
                        .whatDidDo("DELETED NON NUMERIC CHARACTER").build()));
            } else {
                phone.setStatus(Status.UNFIXABLE);
            }
        }
        return this.buildPhoneDTO(repository.save(phone));
    }

    private void sendToFix(List<Phone> phones) {
        phones.forEach(this::sendASinglePhoneToFix);
    }

    private void sendASinglePhoneToFix(Phone phone) {
        if (phone.getStatus().equals(Status.POSSIBLE_TO_FIX)) {
            log.info("[phoneValidatorService.sendASinglePhoneToFix] send phone {} to fixing", phone.getPhone());
            producer.sendMessage(Topic.FIX_PHONE, phone);
        }
    }

    private Status validateLight(String phone) {
        if (phone.length() == businessProperty.getLengthOfPhone()) {
            if (phone.startsWith(businessProperty.getCountryCode())) {
                return Status.CORRECT;
            } else {
                return Status.INCORRECT_COUNTRY_CODE;
            }
        } else if (phone.length() > businessProperty.getLengthOfPhone()) {
            return Status.POSSIBLE_TO_FIX;
        } else {
            return Status.INCORRECT_LENGTH_NUMBER;
        }
    }

    private List<Phone> buildPhones(List<PhoneDTO> phoneDTOS) {
        return phoneDTOS.stream().map(this::buildPhone).collect(Collectors.toList());
    }

    private Phone buildPhone(PhoneDTO phoneDTO) {
        return Phone.builder().id(phoneDTO.getId())
                .externalId(phoneDTO.getExternalId())
                .phone(phoneDTO.getPhone())
                .status(this.validateLight(phoneDTO.getPhone()))
                .build();
    }

    private List<PhoneDTO> buildPhoneDTOs(List<Phone> phones) {
        return phones.stream().map(this::buildPhoneDTO).collect(Collectors.toList());
    }

    private PhoneDTO buildPhoneDTO(Phone phone) {
        return PhoneDTO.builder().id(phone.getId())
                .externalId(phone.getExternalId())
                .phone(phone.getPhone())
                .status(phone.getStatus())
                .corrections(Optional.ofNullable(phone.getCorrections()).stream()
                        .flatMap(Collection::stream).map(correction ->
                                CorrectionDTO.builder().whatDidDO(correction.getWhatDidDo()).build())
                        .collect(Collectors.toList()))
                .build();
    }
}
