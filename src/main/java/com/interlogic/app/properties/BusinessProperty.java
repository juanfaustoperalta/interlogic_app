package com.interlogic.app.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class BusinessProperty {

    @Value(value = "${interlogic.business.countryCode}")
    public String countryCode;

    @Value(value = "${interlogic.business.lengthOfPhone}")
    public int lengthOfPhone;

}
