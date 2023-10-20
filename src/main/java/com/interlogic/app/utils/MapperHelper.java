package com.interlogic.app.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interlogic.app.dtos.PhoneDTO;
import com.interlogic.app.exceptions.Error;
import com.interlogic.app.exceptions.InterlogicException;
import com.interlogic.app.model.Phone;
import org.springframework.stereotype.Component;

@Component
public class MapperHelper {

    private final ObjectMapper mapper;

    public MapperHelper(ObjectMapper mapper) {
        this.mapper = mapper;
    }


    public String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public PhoneDTO toObject(String msg) {
        try {
            return mapper.readValue(msg, PhoneDTO.class);
        } catch (JsonProcessingException e) {
            throw new InterlogicException(Error.ERROR_TO_PASING_MSG, e);
        }
    }
}
