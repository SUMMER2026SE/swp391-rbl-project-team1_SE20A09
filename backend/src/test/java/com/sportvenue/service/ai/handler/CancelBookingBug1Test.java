package com.sportvenue.service.ai.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.service.ai.AiConversationContextService;
import com.sportvenue.service.BookingService;
import com.sportvenue.service.ai.ParamNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CancelBookingBug1Test {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private AiConversationContextService conversationContextService;
    @Mock
    private BookingService bookingService;
    @Mock
    private ParamNormalizer paramNormalizer;

    @InjectMocks
    private CancelBookingHandler handler;

    @Test
    public void testBug1() {
        Stadium parent = new Stadium();
        parent.setStadiumName("Cung Thể thao Tiên Sơn");
        
        Stadium stadium = new Stadium();
        stadium.setStadiumName("Sân 1");
        stadium.setParentStadium(parent);
        
        Booking b = new Booking();
        b.setBookingId(661);
        b.setStadium(stadium);
        b.setBookingStatus(BookingStatus.CONFIRMED);
        
        when(bookingRepository.findByUserUserIdAndBookingStatusInOrderByReservationDateDesc(
                eq(1), any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(b)));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode args = mapper.createObjectNode();
        args.put("keyword", "sân 1 cung thể thao tiên sơn");

        System.out.println("--- RUNNING BUG 1 TEST ---");
        var res = handler.handle(args, "đơn sân 1 cung thể thao tiên sơn", 1, "test-conv");
        System.out.println("Response: " + res.getMessage());
    }
}
