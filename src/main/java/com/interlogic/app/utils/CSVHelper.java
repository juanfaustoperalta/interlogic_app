package com.interlogic.app.utils;

import com.interlogic.app.dtos.PhoneDTO;
import lombok.experimental.UtilityClass;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class CSVHelper {
    public String TYPE = "text/csv";

    public boolean hasCSVFormat(MultipartFile file) {
        return TYPE.equals(file.getContentType());
    }

    public List<PhoneDTO> csvToPhoneValidationDTO(InputStream is) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.DEFAULT
                             .withFirstRecordAsHeader()
                             .withIgnoreHeaderCase())) {
            return csvParser.getRecords().stream().map(csvRecord -> PhoneDTO.builder()
                    .externalId(Long.parseLong(csvRecord.get(0)))
                    .phone(csvRecord.get(1))
                    .build()).collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("fail to parse CSV file: " + e.getMessage());
        }
    }

}