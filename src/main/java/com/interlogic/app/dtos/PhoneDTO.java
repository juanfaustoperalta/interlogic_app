package com.interlogic.app.dtos;

import com.interlogic.app.model.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhoneDTO {
    private Long id;
    private String phone;
    private Status status;
    private Long externalId;
    private List<CorrectionDTO> corrections;

}
