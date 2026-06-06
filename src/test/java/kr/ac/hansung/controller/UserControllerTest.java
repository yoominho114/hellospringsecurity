package kr.ac.hansung.controller;

import kr.ac.hansung.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DisplayName("UserController 테스트")
class UserControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @MockitoBean
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    @Test
    @WithMockUser(username = "user@hansung.ac.kr", roles = "USER")
    @DisplayName("인증 사용자 - 비밀번호 변경 폼 조회")
    void passwordForm_authenticated_returns200() throws Exception {
        mockMvc.perform(get("/user/password"))
            .andExpect(status().isOk())
            .andExpect(view().name("user/password"))
            .andExpect(model().attributeExists("passwordChangeDto"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("비인증 사용자 - 비밀번호 변경 폼 접근 시 로그인 이동")
    void passwordForm_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/user/password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "user@hansung.ac.kr", roles = "USER")
    @DisplayName("인증 사용자 - 현재 비밀번호 확인 후 변경 성공")
    void changePassword_success_redirectsHome() throws Exception {
        willDoNothing().given(userService)
            .changePassword("user@hansung.ac.kr", "oldpassword", "newpassword");

        mockMvc.perform(post("/user/password")
                .with(csrf())
                .param("currentPassword", "oldpassword")
                .param("newPassword", "newpassword")
                .param("confirmPassword", "newpassword"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/home"))
            .andExpect(flash().attribute("successMessage", "비밀번호가 변경되었습니다"));
    }

    @Test
    @WithMockUser(username = "user@hansung.ac.kr", roles = "USER")
    @DisplayName("인증 사용자 - 현재 비밀번호 불일치 시 오류 표시")
    void changePassword_wrongCurrentPassword_returnsForm() throws Exception {
        willThrow(new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다"))
            .given(userService)
            .changePassword("user@hansung.ac.kr", "wrongpassword", "newpassword");

        mockMvc.perform(post("/user/password")
                .with(csrf())
                .param("currentPassword", "wrongpassword")
                .param("newPassword", "newpassword")
                .param("confirmPassword", "newpassword"))
            .andExpect(status().isOk())
            .andExpect(view().name("user/password"))
            .andExpect(model().attributeHasFieldErrors("passwordChangeDto", "currentPassword"));
    }

    @Test
    @WithMockUser(username = "user@hansung.ac.kr", roles = "USER")
    @DisplayName("인증 사용자 - 새 비밀번호 확인 불일치 시 오류 표시")
    void changePassword_mismatchConfirmPassword_returnsForm() throws Exception {
        mockMvc.perform(post("/user/password")
                .with(csrf())
                .param("currentPassword", "oldpassword")
                .param("newPassword", "newpassword")
                .param("confirmPassword", "different"))
            .andExpect(status().isOk())
            .andExpect(view().name("user/password"))
            .andExpect(model().attributeHasFieldErrors("passwordChangeDto", "confirmPassword"));
    }
}
