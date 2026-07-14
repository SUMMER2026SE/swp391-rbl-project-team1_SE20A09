package com.sportvenue.repository;

import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Report;
import com.sportvenue.entity.Role;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.ReportCategory;
import com.sportvenue.entity.enums.StadiumNodeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReportRepositoryTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findWithDetailsByReportId_handlesMultipleEvidenceUrls() {
        User customer = persistUser("Customer");
        User ownerUser = persistUser("Owner");
        Booking booking = persistBooking(customer, ownerUser);
        Report report = Report.builder()
                .reporter(ownerUser)
                .reportee(customer)
                .booking(booking)
                .category(ReportCategory.PROPERTY_DAMAGE)
                .description("Damage with two evidence images")
                .evidenceUrls(List.of(
                        "https://example.com/evidence-1.png",
                        "https://example.com/evidence-2.png"))
                .build();

        entityManager.persistAndFlush(report);
        Integer reportId = report.getReportId();
        entityManager.clear();

        Report found = reportRepository.findWithDetailsByReportId(reportId).orElseThrow();

        assertThat(found.getReporter().getUserId()).isEqualTo(ownerUser.getUserId());
        assertThat(found.getReportee().getUserId()).isEqualTo(customer.getUserId());
        assertThat(found.getEvidenceUrls())
                .containsExactlyInAnyOrder(
                        "https://example.com/evidence-1.png",
                        "https://example.com/evidence-2.png");
    }

    private User persistUser(String roleName) {
        String suffix = UUID.randomUUID().toString();
        String shortSuffix = suffix.substring(0, 8);
        Role role = Role.builder()
                .roleName(roleName + "-" + shortSuffix)
                .build();
        entityManager.persist(role);

        User user = User.builder()
                .role(role)
                .firstName("Test")
                .lastName(roleName)
                .phoneNumber(suffix.substring(0, 15))
                .email(roleName.toLowerCase() + "-" + suffix + "@example.com")
                .passwordHash("hashed-password")
                .build();
        return entityManager.persist(user);
    }

    private Booking persistBooking(User customer, User ownerUser) {
        Owner owner = Owner.builder()
                .user(ownerUser)
                .businessName("Test Owner")
                .approvedStatus(ApprovedStatus.APPROVED)
                .build();
        entityManager.persist(owner);

        SportType sportType = SportType.builder()
                .sportName("Football " + UUID.randomUUID())
                .sportCode("FB-" + UUID.randomUUID().toString().substring(0, 8))
                .build();
        entityManager.persist(sportType);

        StadiumComplex complex = StadiumComplex.builder()
                .owner(owner)
                .name("Test Complex")
                .address("Test address")
                .approvedStatus(ApprovedStatus.APPROVED)
                .build();
        entityManager.persist(complex);

        Stadium facility = Stadium.builder()
                .owner(owner)
                .sportType(sportType)
                .stadiumName("Test Facility")
                .address("Test address")
                .approvedStatus(ApprovedStatus.APPROVED)
                .nodeType(StadiumNodeType.FACILITY)
                .complex(complex)
                .pricePerHour(BigDecimal.valueOf(100000))
                .build();
        entityManager.persist(facility);

        Stadium court = Stadium.builder()
                .owner(owner)
                .sportType(sportType)
                .stadiumName("Test Court")
                .address("Test address")
                .approvedStatus(ApprovedStatus.APPROVED)
                .nodeType(StadiumNodeType.COURT)
                .complex(complex)
                .parentStadium(facility)
                .pricePerHour(BigDecimal.valueOf(100000))
                .build();
        entityManager.persist(court);

        TimeSlot slot = TimeSlot.builder()
                .stadium(court)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .pricePerSlot(BigDecimal.valueOf(100000))
                .build();
        entityManager.persist(slot);

        Booking booking = Booking.builder()
                .user(customer)
                .stadium(court)
                .slot(slot)
                .totalPrice(BigDecimal.valueOf(100000))
                .serviceFee(BigDecimal.valueOf(10000))
                .reservationDate(LocalDate.now().plusDays(1))
                .build();
        return entityManager.persist(booking);
    }
}
