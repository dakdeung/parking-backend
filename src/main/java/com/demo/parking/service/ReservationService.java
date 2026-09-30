package com.demo.parking.service;

import com.demo.parking.constant.Lengths;
import com.demo.parking.constant.ParameterValues;
import com.demo.parking.constant.ReservationStatus;
import com.demo.parking.constant.SlotStatus;
import com.demo.parking.constant.VehicleType;
import com.demo.parking.entity.ParkingInvoice;
import com.demo.parking.entity.ParkingLot;
import com.demo.parking.entity.ParkingSlot;
import com.demo.parking.entity.Reservation;
import com.demo.parking.exception.BusinessRuleViolationException;
import com.demo.parking.exception.InvalidRequestException;
import com.demo.parking.exception.ParkingException;
import com.demo.parking.exception.ReservationConflictException;
import com.demo.parking.exception.ResourceNotFoundException;
import com.demo.parking.model.request.AvailabilityRequest;
import com.demo.parking.model.request.CancelReservationRequest;
import com.demo.parking.model.request.ReservationRequest;
import com.demo.parking.model.response.AvailableSlotResponse;
import com.demo.parking.model.response.CheckOutResponse;
import com.demo.parking.model.response.InvoiceResponse;
import com.demo.parking.model.response.ReservationResponse;
import com.demo.parking.repository.ParkingInvoiceRepository;
import com.demo.parking.repository.ParkingLotRepository;
import com.demo.parking.repository.ParkingSlotRepository;
import com.demo.parking.repository.RateCardRepository;
import com.demo.parking.repository.ReservationRepository;
import com.demo.parking.specification.ParkingSlotSpecification;
import com.demo.parking.specification.RateCardSpecification;
import com.demo.parking.specification.ReservationSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;


@Service
@Transactional
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ParkingLotRepository lotRepository;
    private final ParkingSlotRepository slotRepository;
    private final ReservationRepository reservationRepository;
    private final RateCardRepository rateCardRepository;
    private final ParkingInvoiceRepository invoiceRepository;
    private final BillingService billingService;
    private final Clock clock;

    public ReservationService(
        ParkingLotRepository lotRepository,
        ParkingSlotRepository slotRepository,
        ReservationRepository reservationRepository,
        RateCardRepository rateCardRepository,
        ParkingInvoiceRepository invoiceRepository,
        BillingService billingService,
        Clock clock
    ) {
        this.lotRepository = lotRepository;
        this.slotRepository = slotRepository;
        this.reservationRepository = reservationRepository;
        this.rateCardRepository = rateCardRepository;
        this.invoiceRepository = invoiceRepository;
        this.billingService = billingService;
        this.clock = clock;
    }

    /** Find Available */
    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> findAvailableSlots(
        String lotId,
        AvailabilityRequest request
    ) {
        try {
            if (request == null
                    || request.getVehicleType() == null
                    || request.getStartTime() == null
                    || request.getEndTime() == null) {
                throw new InvalidRequestException(
                    "vehicleType",
                    "vehicleType, startTime and endTime are required"
                );
            }
            if (!VehicleType.VALUES.contains(request.getVehicleType())) {
                throw new InvalidRequestException(
                    "vehicleType",
                    "vehicleType must be one of " + VehicleType.VALUES
                );
            }
            return findAvailable(
                lotId,
                request.getVehicleType(),
                request.getStartTime(),
                request.getEndTime()
            );
        } catch (ParkingException e) {
            log.error("Failed to Find Available");
            throw e;
        }
    }

    private List<AvailableSlotResponse> findAvailable(
        String lotId, 
        String vehicleType,
        LocalDateTime startTime, 
        LocalDateTime endTime
    ) {
        var lot = getLot(lotId);
        validateWindow(
            lot, 
            startTime, 
            endTime, 
            "endTime"
        );
        var candidates = slotRepository.findAll(
            Specification.allOf(
                ParkingSlotSpecification.inLot(lot.getLotId())
                .and(ParkingSlotSpecification.hasVehicleType(vehicleType))
            ), Sort.by(
                "floorLevel", 
                "slotId"
            )
        );

        var slots = filterAvailable(
            candidates, 
            lot.getLotId(), 
            startTime, 
            endTime
        );

        return slots.stream()
        .map(slot -> new AvailableSlotResponse(
            slot.getSlotId(), 
            slot.getFloorLevel(), 
            slot.getVehicleType(), 
            slot.getStatus())
        ).toList();
    }

    /** Create Reservation */
    public ReservationResponse createReservation(
        ReservationRequest request
    ) {
        try {
            var lot = getLot(request.getLotId());
            validateWindow(lot, request.getPlannedStartTime(), request.getPlannedEndTime(), "plannedEndTime");

            var lockedSlots = slotRepository.lockByLotAndVehicleType(
                lot.getLotId(), 
                request.getVehicleType()
            );

            if (lockedSlots.isEmpty()) {
                throw new ReservationConflictException(
                        "Lot " + lot.getLotId() + " has no slots for vehicle type " + request.getVehicleType());
            }

            var available = filterAvailable(
                    lockedSlots, 
                    lot.getLotId(), 
                    request.getPlannedStartTime(), 
                    request.getPlannedEndTime()
                );

            var slot = pickSlot(
                request, 
                lockedSlots, 
                available
            );

            var reservation = new Reservation();
            reservation.setReservationId(ParameterValues.RESERVATION_ID_PREFIX + UUID.randomUUID());
            reservation.setLotId(lot.getLotId());
            reservation.setSlotId(slot.getSlotId());
            reservation.setFloorLevel(slot.getFloorLevel());
            reservation.setVehicleType(slot.getVehicleType());
            reservation.setLicensePlate(request.getLicensePlate());
            reservation.setCustomerId(request.getCustomerId());
            reservation.setPlannedStartTime(request.getPlannedStartTime());
            reservation.setPlannedEndTime(request.getPlannedEndTime());
            reservation.setStatus(ReservationStatus.PENDING);
            reservation.setLateCancellation(false);
            reservation.setCreatedAt(LocalDateTime.now(clock));

            markSlotReserved(slot);
            var saved = reservationRepository.saveAndFlush(reservation);
            return toReservationResponse(saved);
        } catch (ParkingException e) {
            log.error("Failed to Create Reservation");
            throw e;
        }
    }

    private ParkingSlot pickSlot(
        ReservationRequest request, 
        List<ParkingSlot> lockedSlots,
        List<ParkingSlot> available
    ) {
        var window = request.getPlannedStartTime() + " - " + request.getPlannedEndTime();
        if (request.getSlotId() == null) {
            return available
                .stream()
                .findFirst().orElseThrow(() -> new ReservationConflictException(
                        "No " + request.getVehicleType() + " slot available in lot " + request.getLotId() + " for " + window
                    )
                );
        }

        var slotExists = lockedSlots
            .stream()
            .anyMatch(s -> 
                s.getSlotId()
                .equals(
                    request.getSlotId()
                )
            );

        if (!slotExists) {
            throw new ResourceNotFoundException(
                    request.getVehicleType() + " slot in lot " + request.getLotId(), request.getSlotId());
        }

        return available.stream()
                .filter(s -> s.getSlotId().equals(request.getSlotId()))
                .findFirst()
                .orElseThrow(() -> new ReservationConflictException(
                        "Slot " + request.getSlotId() + " in lot " + request.getLotId() + " is already booked for " + window));
    }

    /** Get Reservation */
    @Transactional(readOnly = true)
    public ReservationResponse getReservation(
        String reservationId
    ) {
        try {
            var reservation = requireReservation(reservationId);
            return toReservationResponse(reservation);
        } catch (ParkingException e) {
            log.error("Failed to Get Reservation");
            throw e;
        }
    }

    /** Check in */
    public ReservationResponse checkIn(
        String reservationId, 
        LocalDateTime actualStartTime
    ) {
        try {
            var reservation = requireReservation(reservationId);
            var slot = lockSlot(reservation);

            requireStatus(
                reservation, 
                ReservationStatus.PENDING, 
                "checked in"
            );

            var earliest = reservation.getPlannedStartTime().minus(Duration.ofMinutes(Lengths.CHECK_IN_WINDOW_MINUTES));
            var latest = reservation.getPlannedStartTime().plus(Duration.ofMinutes(Lengths.CHECK_IN_WINDOW_MINUTES));

            if (actualStartTime.isBefore(earliest)) {
                throw new BusinessRuleViolationException("Check-in is only allowed from " + earliest
                        + " (" + Duration.ofMinutes(Lengths.CHECK_IN_WINDOW_MINUTES).toMinutes() + " minutes before plannedStartTime)");
            }

            if (actualStartTime.isAfter(latest)) {
                reservation.setStatus(ReservationStatus.NO_SHOW);
                releaseSlot(slot, reservation);
            } else {
                reservation.setActualStartTime(actualStartTime);
                reservation.setStatus(ReservationStatus.ACTIVE);
                markSlotOccupied(slot);
            }

            var saved = reservationRepository.save(reservation);
            return toReservationResponse(saved);
        } catch (ParkingException e) {
            log.error("Failed to Check in");
            throw e;
        }
    }

    /** Check out */
    public CheckOutResponse checkOut(
        String reservationId, 
        LocalDateTime actualEndTime
    ) {
        try {
            var reservation = requireReservation(reservationId);
            var slot = lockSlot(reservation);

            requireStatus(
                reservation, 
                ReservationStatus.ACTIVE, 
                "checked out"
            );

            if (actualEndTime.isBefore(reservation.getActualStartTime())) {
                throw new BusinessRuleViolationException(
                        "actualEndTime must not be before actualStartTime " + reservation.getActualStartTime());
            }
            reservation.setActualEndTime(actualEndTime);
            reservation.setStatus(ReservationStatus.COMPLETED);
            releaseSlot(
                slot, 
                reservation
            );

            var rateCard = rateCardRepository.findOne(RateCardSpecification.forVehicleType(reservation.getVehicleType()))
                    .orElseThrow(() -> new BusinessRuleViolationException(
                            "No rate card configured for vehicle type " + reservation.getVehicleType()));

            var invoice = invoiceRepository.save(
                billingService.generateInvoice(
                    reservation, 
                    rateCard
                )
            );
            var saved = reservationRepository.save(reservation);

            return new CheckOutResponse(
                toReservationResponse(saved), 
                toInvoiceResponse(invoice)
            );
        } catch (ParkingException e) {
            log.error("Failed to Check Out");
            throw e;
        }
    }

    /** Cancel Reservation */
    public ReservationResponse cancelReservation(
        String reservationId, 
        CancelReservationRequest request
    ) {
        try {
            var reservation = requireReservation(reservationId);
            var slot = lockSlot(reservation);

            if (!getBlockingStatus().contains(reservation.getStatus())) {
                throw new BusinessRuleViolationException("Reservation " + reservationId
                        + " cannot be cancelled from status " + reservation.getStatus());
            }

            var now = LocalDateTime.now(clock);
            var lateFrom = reservation.getPlannedStartTime().minus(Duration.ofMinutes(Lengths.LATE_CANCELLATION_WINDOW_MINUTES));
            reservation.setLateCancellation(!now.isBefore(lateFrom));
            reservation.setCancellationReason(request == null ? null : request.getReason());
            reservation.setStatus(ReservationStatus.CANCELLED);
            releaseSlot(slot, reservation);
            var saved = reservationRepository.save(reservation);
            return toReservationResponse(saved);
        } catch (ParkingException e) {
            log.error("Failed to Cancel Reservation");
            throw e;
        }
    }


    /** Extend Reservation */
    public ReservationResponse extendReservation(
        String reservationId, 
        LocalDateTime newEndTime
    ) {
        try {
            var reservation = requireReservation(reservationId);

            if (!getBlockingStatus().contains(reservation.getStatus())) {
                throw new BusinessRuleViolationException("Reservation " + reservationId
                        + " cannot be extended from status " + reservation.getStatus());
            }

            if (!newEndTime.isAfter(reservation.getPlannedEndTime())) {
                throw new InvalidRequestException("newEndTime",
                        "newEndTime must be after the current plannedEndTime " + reservation.getPlannedEndTime());
            }

            var lot = getLot(reservation.getLotId());
            validateWindow(lot, reservation.getPlannedStartTime(), newEndTime, "newEndTime");
            lockSlot(reservation);

            var conflicts = reservationRepository.findAll(
                Specification.allOf(ReservationSpecification.inLot(reservation.getLotId())
                    .and(ReservationSpecification.onSlot(reservation.getSlotId()))
                    .and(ReservationSpecification.statusIn(getBlockingStatus()))
                    .and(ReservationSpecification.overlaps(reservation.getPlannedEndTime(), newEndTime))
                    .and(ReservationSpecification.excluding(reservation.getReservationId())))
            );


            if (!conflicts.isEmpty()) {
                throw new ReservationConflictException("Slot " + reservation.getSlotId() + " is already booked between "
                        + reservation.getPlannedEndTime() + " and " + newEndTime);
            }
            reservation.setPlannedEndTime(newEndTime);
            var saved = reservationRepository.save(reservation);
            return toReservationResponse(saved);
        } catch (ParkingException e) {
            log.error("Failed to Extend Reservation");
            throw e;
        }
    }

    private ReservationResponse toReservationResponse(
        Reservation reservation
    ) {
        return new ReservationResponse(
            reservation.getReservationId(),
            reservation.getLotId(),
            reservation.getSlotId(),
            reservation.getFloorLevel(),
            reservation.getVehicleType(),
            reservation.getLicensePlate(),
            reservation.getCustomerId(),
            reservation.getStatus(),
            reservation.getPlannedStartTime(),
            reservation.getPlannedEndTime(),
            reservation.getActualStartTime(),
            reservation.getActualEndTime(),
            reservation.isLateCancellation(),
            reservation.getCancellationReason());
    }

    private InvoiceResponse toInvoiceResponse(
        ParkingInvoice invoice
    ) {
        return new InvoiceResponse(
            invoice.getInvoiceId(),
            invoice.getReservationId(),
            invoice.getCustomerId(),
            invoice.getBilledDurationMinutes(),
            invoice.getBaseAmount(),
            invoice.getOvernightSurchargeApplied(),
            invoice.getDiscountAmount(),
            invoice.getTotalAmount(),
            invoice.getGeneratedAt()
        );
    }

    private List<String> getBlockingStatus() {
        return List.of(
            ReservationStatus.PENDING, 
            ReservationStatus.ACTIVE
        );
    }

    private List<ParkingSlot> filterAvailable(
        List<ParkingSlot> candidates, 
        String lotId,
        LocalDateTime startTime, 
        LocalDateTime endTime
    ) {
        var bookedFilter = ReservationSpecification.inLot(lotId)
        .and(ReservationSpecification.statusIn(getBlockingStatus()))
        .and(ReservationSpecification.overlaps(startTime, endTime));

        var bookedSlotIds = new HashSet<String>();

        for (var booked : reservationRepository.findAll(bookedFilter)) {
            bookedSlotIds.add(booked.getSlotId());
        }

        return candidates.stream()
            .filter(ReservationService::isInService)
            .filter(slot -> !bookedSlotIds.contains(slot.getSlotId()))
            .toList();
    }

    private static boolean isInService(
        ParkingSlot slot
    ) {
        return !SlotStatus.MAINTENANCE.equals(slot.getStatus());
    }

    private static void markSlotReserved(
        ParkingSlot slot
    ) {
        ensureInService(slot);
        if (!SlotStatus.OCCUPIED.equals(slot.getStatus())) {
            slot.setStatus(SlotStatus.RESERVED);
        }
    }

    private static void markSlotOccupied(
        ParkingSlot slot
    ) {
        ensureInService(slot);
        slot.setStatus(SlotStatus.OCCUPIED);
    }

    private void releaseSlot(
        ParkingSlot slot, 
        Reservation reservation
    ) {
        if (!isInService(slot)) {
            return;
        }
        var slotStillHeld = reservationRepository.findOne(ReservationSpecification.inLot(reservation.getLotId())
            .and(ReservationSpecification.onSlot(reservation.getSlotId()))
            .and(ReservationSpecification.statusIn(getBlockingStatus()))
            .and(ReservationSpecification.excluding(reservation.getReservationId()))).isPresent();

        slot.setStatus(slotStillHeld 
            ? SlotStatus.RESERVED 
            : SlotStatus.AVAILABLE);
    }

    private static void ensureInService(
        ParkingSlot slot
    ) {
        if (!isInService(slot)) {
            throw new BusinessRuleViolationException("Slot " + slot.getSlotId() + " is under maintenance");
        }
    }

    private ParkingSlot lockSlot(
        Reservation reservation
    ) {
        return slotRepository.lockByLotAndSlotId(reservation.getLotId(), reservation.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Slot in lot " + reservation.getLotId(), reservation.getSlotId()));
    }

    private Reservation requireReservation(
        String reservationId
    ) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));
    }

    private static void requireStatus(Reservation reservation, String expected, String action) {
        if (!expected.equals(reservation.getStatus())) {
            throw new BusinessRuleViolationException("Reservation " + reservation.getReservationId()
                    + " cannot be " + action + " from status " + reservation.getStatus());
        }
    }

    private void validateWindow(
        ParkingLot lot, 
        LocalDateTime start, 
        LocalDateTime end, 
        String endField
    ) {
        if (!end.isAfter(start)) {
            throw new InvalidRequestException(endField, endField + " must be after the start time");
        }
        if (!isWithinOperatingHours(lot, start, end)) {
            var hours = lot.getOperatingHours();
            throw new BusinessRuleViolationException("Requested window " + start + " - " + end
                    + " is outside operating hours of lot " + lot.getLotId() + " ("
                    + hours.getOpenTime() + " - " + hours.getCloseTime() + ")");
        }
    }


    private ParkingLot getLot(
        String lotId
    ) {
        return lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking lot", lotId));
    }

    private static boolean isWithinOperatingHours(
        ParkingLot lot, 
        LocalDateTime start, 
        LocalDateTime end
    ) {
        var hours = lot.getOperatingHours();
        if (hours.getOpenTime().equals(hours.getCloseTime())) {
            return true;
        }
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            return false;
        }
        return !start.toLocalTime().isBefore(hours.getOpenTime()) && !end.toLocalTime().isAfter(hours.getCloseTime());
    }
}
