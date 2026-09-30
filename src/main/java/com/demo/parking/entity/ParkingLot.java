package com.demo.parking.entity;

import java.util.ArrayList;
import java.util.List;

import com.demo.parking.constant.DefaultValues;
import com.demo.parking.constant.ParameterValues;
import com.demo.parking.converter.StringToStingConverter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "parking_lot")
@NamedEntityGraphs(
    @NamedEntityGraph(
        name = "parking-lot",
        attributeNodes = {
            @NamedAttributeNode(value = "slots")
        }
    )
)
@AllArgsConstructor
@NoArgsConstructor 
public class ParkingLot {

    @Id
    @Pattern(regexp = ParameterValues.LOT_ID_REGEX)
    private String lotId;
    @Convert(converter = StringToStingConverter.class)
    private String name = DefaultValues.EMPTY_STRING;
    @Convert(converter = StringToStingConverter.class)
    private String location = DefaultValues.EMPTY_STRING;
    @Embedded
    private OperatingHours operatingHours;
    @OneToMany(
        mappedBy = "parkingLot", 
        cascade = CascadeType.ALL, 
        orphanRemoval = true
    )
    private List<ParkingSlot> slots = new ArrayList<>();
    @Version
    @Column(name = "row_version")
    private Long version;
}
