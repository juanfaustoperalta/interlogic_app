package com.interlogic.app.dtos.responses;

import com.interlogic.app.dtos.PhoneDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PhoneResponse {

    private PhoneDTO data;

}
