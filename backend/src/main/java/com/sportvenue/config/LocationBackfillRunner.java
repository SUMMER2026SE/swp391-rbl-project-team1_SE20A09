package com.sportvenue.config;

import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.repository.StadiumComplexRepository;
import com.sportvenue.util.location.VietnamLocationResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Backfill cột province/district cho các StadiumComplex tạo trước khi có 2 cột này (migration
 * V7.9) — suy ra từ address tự do đã có sẵn qua VietnamLocationResolver, không gọi geocode
 * ngoài. Idempotent: chỉ xử lý complex có province NULL, nên từ lần chạy thứ 2 trở đi là no-op.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocationBackfillRunner implements CommandLineRunner {

    private final StadiumComplexRepository stadiumComplexRepository;
    private final VietnamLocationResolver locationResolver;

    @Override
    @Transactional
    public void run(String... args) {
        List<StadiumComplex> pending = stadiumComplexRepository.findAllByProvinceIsNull();
        if (pending.isEmpty()) {
            return;
        }

        List<StadiumComplex> resolved = new ArrayList<>();
        for (StadiumComplex complex : pending) {
            VietnamLocationResolver.LocationMatch match = locationResolver.deriveFromAddress(complex.getAddress());
            if (match.province() != null || match.district() != null) {
                complex.setProvince(match.province());
                complex.setDistrict(match.district());
                resolved.add(complex);
            }
        }
        if (!resolved.isEmpty()) {
            stadiumComplexRepository.saveAll(resolved);
        }
        log.info("Backfilled {} / {} stadium complexes with location data", resolved.size(), pending.size());
    }
}
