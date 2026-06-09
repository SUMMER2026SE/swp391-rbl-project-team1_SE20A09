package com.sportvenue.service;

import com.sportvenue.dto.home.HomeDashboardResponse;
import com.sportvenue.security.UserPrincipal;

public interface HomeService {
    HomeDashboardResponse getDashboard(UserPrincipal principal);
}
