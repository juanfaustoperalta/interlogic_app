package com.interlogic.app.controllers;


import com.google.common.collect.Lists;
import com.interlogic.app.dtos.PhoneDTO;
import com.interlogic.app.dtos.requests.PhoneValidationRequest;
import com.interlogic.app.dtos.responses.PhoneListsResponse;
import com.interlogic.app.dtos.responses.PhoneResponse;
import com.interlogic.app.services.PhoneValidatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/validate/phone")
public class PhoneValidatorController {

    private final PhoneValidatorService service;

    public PhoneValidatorController(PhoneValidatorService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        service.process(file);
        return ResponseEntity.ok().build();
    }

    @PostMapping()
    public ResponseEntity<PhoneResponse> validatePhone(@RequestBody PhoneValidationRequest phoneValidationRequest) {
        PhoneDTO phoneDTO = service.validate(phoneValidationRequest.getData());
        return ResponseEntity.ok(PhoneResponse.builder().data(phoneDTO).build());
    }

    @GetMapping
    public ResponseEntity<PhoneListsResponse> getInfo() {
        List<PhoneDTO> info = service.getInfo();
        return ResponseEntity.ok().body(PhoneListsResponse.builder().data(info).build());
    }

}
