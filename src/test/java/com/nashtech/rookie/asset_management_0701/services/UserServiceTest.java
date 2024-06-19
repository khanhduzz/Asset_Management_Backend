package com.nashtech.rookie.asset_management_0701.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.nashtech.rookie.asset_management_0701.enums.EUserStatus;
import com.nashtech.rookie.asset_management_0701.utils.auth_util.AuthUtilImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import com.nashtech.rookie.asset_management_0701.dtos.requests.user.ChangePasswordRequest;
import com.nashtech.rookie.asset_management_0701.dtos.requests.user.UserRequest;
import com.nashtech.rookie.asset_management_0701.dtos.requests.user.UserSearchDto;
import com.nashtech.rookie.asset_management_0701.dtos.responses.user.UserResponse;
import com.nashtech.rookie.asset_management_0701.entities.Location;
import com.nashtech.rookie.asset_management_0701.entities.User;
import com.nashtech.rookie.asset_management_0701.enums.EGender;
import com.nashtech.rookie.asset_management_0701.enums.ERole;
import com.nashtech.rookie.asset_management_0701.exceptions.AppException;
import com.nashtech.rookie.asset_management_0701.exceptions.ErrorCode;
import com.nashtech.rookie.asset_management_0701.mappers.LocationMapperImpl;
import com.nashtech.rookie.asset_management_0701.mappers.UserMapper;
import com.nashtech.rookie.asset_management_0701.mappers.UserMapperImpl;
import com.nashtech.rookie.asset_management_0701.repositories.UserRepository;
import com.nashtech.rookie.asset_management_0701.services.user.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {UserMapperImpl.class, UserServiceImpl.class, LocationMapperImpl.class, AuthUtilImpl.class})
@Slf4j
class UserServiceTest {

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserServiceImpl userService;

    private User adminUsing;

    private Location adminLocation;

    private User userInDB;

    @BeforeEach
    void setUp () {
        userInDB = new User();
        userInDB.setFirstName("first");
        userInDB.setLastName("Last");
        userInDB.setId(1L);
        userInDB.setStatus(EUserStatus.FIRST_LOGIN);
        userInDB.setGender(EGender.FEMALE);

        adminLocation = new Location();
        adminLocation.setId(1L);
        adminLocation.setName("location");

        adminUsing = new User();
        adminUsing.setFirstName("admin");
        adminUsing.setLastName("admin");
        adminUsing.setUsername("abc.com");
        adminUsing.setId(2L);
        adminUsing.setLocation(adminLocation);
    }

    @Nested
    class HappyCase {

        @Test
        @WithMockUser(roles = "ADMIN", username = "adminName")
        void testCreateUser_validUser_shouldReturnUserResponse() {
            // Setup mocks
            UserRequest userRequest = new UserRequest();
            userRequest.setFirstName("Duy");
            userRequest.setLastName("Nguyen Hoang");
            userRequest.setDob(LocalDate.of(1990, 1, 1));
            userRequest.setJoinDate(LocalDate.of(2024, 6, 3));
            userRequest.setRole(ERole.ADMIN);
            userRequest.setLocationId(1L);
            when(userRepository.count()).thenReturn(1L);
            when(userRepository.save(any())).thenReturn(userInDB);
            when(userRepository.findByUsername("adminName")).thenReturn(Optional.of(new User()));
            when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

            // Run the method
            UserResponse result = userService.createUser(userRequest);

            // Assertions
            assertEquals("SD0001", result.getStaffCode());
        }

        @ParameterizedTest
        @CsvSource({
            "null, null, 'firstName', 'DESC', 1, 20",
            "'first', 'ADMIN', 'firstName', 'DESC', 1, 20",
            "' ', ' ', 'firstName', 'DESC', 1, 20"
        })
        @WithMockUser(username = "abc.com", roles = "ADMIN")
        void testGetAllUse_whenPassInEmpty_shouldReturnCorrectFormat(
                String searchString, String type, String sortBy, String sortDir, Integer pageNumber, Integer pageSize) {
            log.info("searchString:" + searchString);
            log.info("type:" + type);

            // set up
            var searchDto = new UserSearchDto(searchString, type, sortBy, sortDir, pageNumber, pageSize);
            var pageRequest = PageRequest.of(0, 20);
            var resultPage = new PageImpl<>(List.of(userInDB), pageRequest, 1);
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(resultPage);

            when(userRepository.findByUsername("abc.com")).thenReturn(Optional.of(adminUsing));

            // run
            var result = userService.getAllUser(searchDto);

            assertThat(result)
                    .hasFieldOrPropertyWithValue("page", 1)
                    .hasFieldOrPropertyWithValue("total", 1L)
                    .hasFieldOrPropertyWithValue("itemsPerPage", 20);
            assertThat(result.getData().getFirst())
                    .usingRecursiveComparison()
                    .isEqualTo(userMapper.toUserResponse(userInDB));
        }

        @Test
        @WithMockUser(username = "test", roles = "ADMIN")
        void testChangePassword_whenValid_shouldReturnUserResponse () {
            // set up
            var changePasswordRequest = new ChangePasswordRequest("newPassword");
            when(userRepository.findByUsername("test")).thenReturn(Optional.of(userInDB));
            when(passwordEncoder.encode("newPassword")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(userInDB);

            // run
            userService.changePassword(changePasswordRequest);

            // verify
            verify(userRepository, times(1)).save(any(User.class));
        }
    }

    @Nested
    class UnHappyCase {
        @Test
        @WithMockUser(roles = "ADMIN", username = "adminName")
        void testCreateUserJoinDateBeforeDob () {
            // Given
            UserRequest userRequest = new UserRequest();
            userRequest.setFirstName("Duy");
            userRequest.setLastName("Nguyen Hoang");
            userRequest.setDob(LocalDate.of(2000, 1, 1));
            userRequest.setJoinDate(LocalDate.of(1999, 12, 31));

            // When & Then
            AppException exception = assertThrows(AppException.class, () -> userService.createUser(userRequest));

            assertEquals(ErrorCode.JOIN_DATE_BEFORE_DOB, exception.getErrorCode());
        }

        @Test
        @WithMockUser(roles = "ADMIN", username = "adminName")
        void testCreateUserJoinDateWeekend() {
            // Given
            UserRequest userRequest = new UserRequest();
            userRequest.setFirstName("Duy");
            userRequest.setLastName("Nguyen");
            userRequest.setDob(LocalDate.of(2000, 1, 1));
            userRequest.setJoinDate(LocalDate.of(2023, 6, 17)); // Saturday

            // When & Then
            AppException exception = assertThrows(AppException.class, () -> userService.createUser(userRequest));

            assertEquals(ErrorCode.JOIN_DATE_WEEKEND, exception.getErrorCode());
        }

        @Test
        @WithMockUser(username = "abc.com", roles = "ADMIN")
        void testGetAllUse_whenPageNumberIs0_shouldThrowAppExcpetionWithErrorCodeBadPageable() {
            // set up
            var searchDto = new UserSearchDto("first", "ADMIN", "firstName", "DESC", 0, 20);
            var pageRequest = PageRequest.of(0, 20);
            var resultPage = new PageImpl<>(List.of(userInDB), pageRequest, 1);
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(resultPage);
            when(userRepository.findByUsername("abc.com")).thenReturn(Optional.of(adminUsing));

            // run
            var exception = assertThrows(AppException.class, () -> {
                userService.getAllUser(searchDto);
            });

            assertThat(exception).hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PAGEABLE);
        }

        @Test
        @WithMockUser(username = "test", roles = "ADMIN")
        void testChangePassword_whenUserNotFound_shouldThrowAppException () {
            // set up
            var changePasswordRequest = new ChangePasswordRequest("newPassword");
            when(userRepository.findByUsername("test")).thenReturn(Optional.empty());

            // run
            var exception = assertThrows(AppException.class, () -> {
                userService.changePassword(changePasswordRequest);
            });

            // verify
            assertThat(exception).hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }
}
