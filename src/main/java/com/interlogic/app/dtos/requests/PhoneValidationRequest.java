package com.interlogic.app.dtos.requests;

import com.interlogic.app.dtos.PhoneDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PhoneValidationRequest {

    private PhoneDTO data;

}
