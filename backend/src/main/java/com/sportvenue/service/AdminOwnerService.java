package com.sportvenue.service;

import com.sportvenue.dto.response.AdminOwnerResponse;
import com.sportvenue.dto.response.PageResponse;

public interface AdminOwnerService {
    PageResponse<AdminOwnerResponse> getOwners(String search, String accountStatus, String approvedStatus, int page, int size, String sortBy, String sortDir);
<<<<<<< HEAD
    
    void lockUnlockOwner(Integer ownerId, boolean isEnabled, String reason);
=======
>>>>>>> 11355feea15d9e6141d1ca6e33ad80ca0785a4dc
}
