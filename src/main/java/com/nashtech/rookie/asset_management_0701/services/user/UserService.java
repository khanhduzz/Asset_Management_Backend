package com.nashtech.rookie.asset_management_0701.services.user;

import com.nashtech.rookie.asset_management_0701.dtos.requests.user.ChangePasswordRequest;
import com.nashtech.rookie.asset_management_0701.dtos.requests.user.UserRequest;
import com.nashtech.rookie.asset_management_0701.dtos.requests.user.UserSearchDto;
import com.nashtech.rookie.asset_management_0701.dtos.responses.PaginationResponse;
import com.nashtech.rookie.asset_management_0701.dtos.responses.user.UserResponse;

public interface UserService {
    UserResponse createUser (UserRequest userRequest);

    PaginationResponse<UserResponse> getAllUser (UserSearchDto userSearchDto);

    void changePassword (ChangePasswordRequest changePasswordRequest);
}
