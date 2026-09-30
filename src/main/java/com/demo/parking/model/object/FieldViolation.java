package com.demo.parking.model.object;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldViolation {

    private String field;
    private String reason;
    private String rejectedValue;
}
