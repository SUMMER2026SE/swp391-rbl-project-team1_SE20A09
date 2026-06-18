package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateBookingRequest;
import com.sportvenue.dto.response.MessageResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Endpoints for booking management")
@Slf4j
public class BookingController {

    private final BookingRepository bookingRepository;

    @PostMapping
    @Operation(summary = "Create booking", description = "Create a new stadium booking with address and location")
    public ResponseEntity<MessageResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        log.info("Received booking request: {}", request.getCustomerName());
        // TODO: Implement service logic
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Đặt sân thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Customer', 'Owner', 'Admin')")
    @Operation(summary = "Get booking detail", description = "Retrieve detailed information of a booking by ID")
    public ResponseEntity<Booking> getBookingDetail(@PathVariable Integer id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân: " + id));
        
        return ResponseEntity.ok(booking);
    }
}
