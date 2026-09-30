package com.demo.parking.service.seeder;

import java.time.LocalTime;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.parking.constant.ParameterValues;
import com.demo.parking.constant.SlotStatus;
import com.demo.parking.constant.VehicleType;
import com.demo.parking.entity.OperatingHours;
import com.demo.parking.entity.ParkingLot;
import com.demo.parking.entity.ParkingSlot;
import com.demo.parking.exception.InvalidRequestException;
import com.demo.parking.repository.ParkingLotRepository;

@Service
@Transactional
public class ParkingLotSeederService {

    private final ParkingLotRepository lotRepository;

    public ParkingLotSeederService(ParkingLotRepository lotRepository) {
        this.lotRepository = lotRepository;
    }

    @Transactional(readOnly = true)
    public boolean exists(
        String lotId
    ) {
        return lotRepository.existsById(lotId);
    }

    public ParkingLot save(
        ParkingLot lot
    ) {
        return lotRepository.save(lot);
    }

    public ParkingLot newLot(
        String lotId, 
        String name, 
        String location, 
        LocalTime openTime, 
        LocalTime closeTime
    ) {
        var validLotId = requireText(lotId, "lotId");

        if (!Pattern.compile(ParameterValues.LOT_ID_REGEX).matcher(validLotId).matches()) {
            throw new InvalidRequestException("lotId", "lotId '" + lotId + "' must match " + ParameterValues.LOT_ID_REGEX);
        }
        var hours = newOperatingHours(
            openTime, 
            closeTime
        );

        var lot = new ParkingLot();
        lot.setLotId(validLotId);
        lot.setName(requireText(name, "name"));
        lot.setLocation(requireText(location, "location"));
        lot.setOperatingHours(hours);
        return lot;
    }

    public ParkingSlot addSlot(
        ParkingLot lot, 
        String slotId, 
        String vehicleType, 
        int floorLevel
    ) {

        if (lot == null) {
            throw new InvalidRequestException("lot", "lot must not be null");
        }

        var validSlotId = requireText(slotId, "slotId");

        if (!Pattern.compile(ParameterValues.SLOT_ID_REGEX).matcher(validSlotId).matches()) {
            throw new InvalidRequestException("slotId", "slotId '" + slotId + "' must match " + ParameterValues.SLOT_ID_REGEX);
        }

        var duplicate = lot.getSlots().stream().anyMatch(s -> s.getSlotId().equals(validSlotId));
        if (duplicate) {
            throw new InvalidRequestException("slotId", "Slot " + validSlotId + " already exists in lot " + lot.getLotId());
        }

        if (floorLevel < 0) {
            throw new InvalidRequestException("floorLevel", "floorLevel must be >= 0");
        }

        var slot = new ParkingSlot();
        slot.setParkingLot(lot);
        slot.setSlotId(validSlotId);
        slot.setVehicleType(requireVehicleType(vehicleType));
        slot.setFloorLevel(floorLevel);
        slot.setStatus(SlotStatus.AVAILABLE);
        lot.getSlots().add(slot);
        return slot;
    }

    public OperatingHours newOperatingHours(
        LocalTime openTime, 
        LocalTime closeTime
    ) {

        if (openTime == null || closeTime == null) {
            throw new InvalidRequestException("operatingHours", "openTime and closeTime must not be null");
        }

        var open = openTime.withSecond(0).withNano(0);
        var close = closeTime.withSecond(0).withNano(0);

        if (!open.equals(close) && !open.isBefore(close)) {
            throw new InvalidRequestException("operatingHours",
                    "openTime must be before closeTime (use equal times for 24h)");
        }

        var hours = new OperatingHours();
        hours.setOpenTime(open);
        hours.setCloseTime(close);
        return hours;
    }

    private static String requireVehicleType(
        String vehicleType
    ) {
        if (vehicleType == null || !VehicleType.VALUES.contains(vehicleType)) {
            throw new InvalidRequestException("vehicleType",
                    "vehicleType must be one of " + ParameterValues.VEHICLE_TYPE_REGEX.replace("|", ", "));
        }

        return vehicleType;
    }

    private static String requireText(
        String value, 
        String field
    ) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(field, field + " must not be blank");
        }

        return value.trim();
    }
}
