package com.interlogic.app.services;

import com.interlogic.app.dtos.PhoneDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PhoneValidatorService {

    void process(MultipartFile file) throws IOException;

    List<PhoneDTO> getInfo();

    void fixPhone(PhoneDTO phoneDTO);

    PhoneDTO validate(PhoneDTO phone);
}
