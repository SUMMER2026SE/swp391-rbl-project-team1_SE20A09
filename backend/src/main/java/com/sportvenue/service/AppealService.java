package com.sportvenue.service;

import com.sportvenue.dto.request.CreateAppealRequest;
import com.sportvenue.dto.request.ReviewAppealRequest;
import com.sportvenue.dto.response.AppealResponse;
import com.sportvenue.entity.enums.AppealStatus;
import com.sportvenue.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppealService {

    AppealResponse createAppeal(CreateAppealRequest request, UserPrincipal userPrincipal);

    AppealResponse getMyLatestAppeal(UserPrincipal userPrincipal);

    Page<AppealResponse> getAppeals(AppealStatus status, Pageable pageable);

    AppealResponse reviewAppeal(Integer appealId, ReviewAppealRequest request, UserPrincipal adminPrincipal);
}
