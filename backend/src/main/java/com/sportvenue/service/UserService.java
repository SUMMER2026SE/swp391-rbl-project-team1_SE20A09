package com.sportvenue.service;

import com.sportvenue.dto.UserProfileResponse;
import com.sportvenue.entity.User;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.mapper.UserMapper;
import com.sportvenue.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Retrieves the profile of the currently logged-in user by email.
     *
     * @param email user email from authenticated principal
     * @return mapped UserProfileResponse DTO
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toProfileResponse(user);
    }
}
