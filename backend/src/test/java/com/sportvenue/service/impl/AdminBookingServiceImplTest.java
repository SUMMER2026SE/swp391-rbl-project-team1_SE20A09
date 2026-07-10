package com.sportvenue.service.impl;

import com.sportvenue.dto.response.AdminBookingListResponse;
import com.sportvenue.dto.response.AdminBookingResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.entity.enums.PaymentStatus;
import com.sportvenue.repository.BookingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminBookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AdminBookingServiceImpl adminBookingService;

    @SuppressWarnings("unchecked")
    @Test
    void getAdminBookings_ShouldReturnListAndStats() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        
        User customer = User.builder().userId(1).firstName("John").lastName("Doe").email("john@example.com").build();
        Stadium stadium = Stadium.builder().stadiumId(1).stadiumName("San Siro").build();
        TimeSlot slot = TimeSlot.builder().slotId(1).startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(19, 0)).build();
        
        Booking booking = Booking.builder()
                .bookingId(1)
                .user(customer)
                .stadium(stadium)
                .slot(slot)
                .totalPrice(new BigDecimal("150.00"))
                .serviceFee(new BigDecimal("15.00"))
                .bookingStatus(BookingStatus.COMPLETED)
                .paymentStatus(PaymentStatus.PAID)
                .bookingDate(LocalDateTime.now())
                .reservationDate(LocalDate.now())
                .build();
                
        Page<Booking> page = new PageImpl<>(List.of(booking));
        when(bookingRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        // Mock criteria API for stats
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Object[]> query = mock(CriteriaQuery.class);
        Root<Booking> root = mock(Root.class);
        Path<Object> statusPath = mock(Path.class);
        Path<Object> totalPricePath = mock(Path.class);
        Path<Object> serviceFeePath = mock(Path.class);
        
        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Object[].class)).thenReturn(query);
        when(query.from(Booking.class)).thenReturn(root);
        
        when(root.get("bookingStatus")).thenReturn(statusPath);
        when(root.get("totalPrice")).thenReturn(totalPricePath);
        when(root.get("serviceFee")).thenReturn(serviceFeePath);

        Expression<BigDecimal> gmvExpression = mock(Expression.class);
        Expression<BigDecimal> serviceFeeExpression = mock(Expression.class);
        
        // Mock Case expressions
        CriteriaBuilder.Case selectCase1 = mock(CriteriaBuilder.Case.class);
        CriteriaBuilder.Case selectCase2 = mock(CriteriaBuilder.Case.class);
        when(cb.selectCase()).thenReturn(selectCase1, selectCase2);
        
        CriteriaBuilder.Case when1 = mock(CriteriaBuilder.Case.class);
        when(selectCase1.when(any(), any())).thenReturn(when1);
        when(when1.otherwise(any())).thenReturn(gmvExpression);
        
        CriteriaBuilder.Case when2 = mock(CriteriaBuilder.Case.class);
        when(selectCase2.when(any(), any())).thenReturn(when2);
        when(when2.otherwise(any())).thenReturn(serviceFeeExpression);
        
        Expression<BigDecimal> sum1 = mock(Expression.class);
        Expression<BigDecimal> sum2 = mock(Expression.class);
        when(cb.sum(gmvExpression)).thenReturn(sum1);
        when(cb.sum(serviceFeeExpression)).thenReturn(sum2);
        
        CriteriaBuilder.Coalesce coalesce1 = mock(CriteriaBuilder.Coalesce.class);
        CriteriaBuilder.Coalesce coalesce2 = mock(CriteriaBuilder.Coalesce.class);
        when(cb.coalesce(any(), any())).thenReturn(coalesce1, coalesce2);
        
        Expression<Long> countExpr = mock(Expression.class);
        when(cb.count(root)).thenReturn(countExpr);

        TypedQuery<Object[]> typedQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(query)).thenReturn(typedQuery);
        
        Object[] statsResult = new Object[]{ 1L, new BigDecimal("150.00"), new BigDecimal("15.00") };
        when(typedQuery.getSingleResult()).thenReturn(statsResult);

        // Act
        AdminBookingListResponse response = adminBookingService.getAdminBookings(
                null, null, null, null, null, null, null, pageable);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getBookings());
        assertEquals(1, response.getBookings().getContent().size());
        
        AdminBookingResponse record = response.getBookings().getContent().get(0);
        assertEquals("Doe John", record.getCustomerName());
        assertEquals("john@example.com", record.getCustomerEmail());
        assertEquals("San Siro", record.getStadiumName());
        assertEquals(new BigDecimal("150.00"), record.getTotalPrice());
        assertEquals(new BigDecimal("15.00"), record.getServiceFee());
        
        assertNotNull(response.getStats());
        assertEquals(1L, response.getStats().getTotalBookings());
        assertEquals(new BigDecimal("150.00"), response.getStats().getTotalGMV());
        assertEquals(new BigDecimal("15.00"), response.getStats().getTotalServiceFee());
    }
}
