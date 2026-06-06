package kr.ac.hansung.controller;

import kr.ac.hansung.entity.Product;
import kr.ac.hansung.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DisplayName("ProductController 테스트")
class ProductControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @MockitoBean
    private ProductService productService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("인증 사용자 - 상품 목록을 5개 단위로 페이징 조회")
    void listProducts_authenticated_returns200() throws Exception {
        PageRequest pageRequest = PageRequest.of(0, 5, Sort.by("id").ascending());
        given(productService.getProducts(pageRequest)).willReturn(new PageImpl<>(
            List.of(new Product("Spring Boot 4 교재", 35000, "실습서", 50)),
            pageRequest,
            1
        ));

        mockMvc.perform(get("/products")
                .param("page", "0")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(view().name("products/list"))
            .andExpect(model().attributeExists("productPage"))
            .andExpect(model().attribute("keyword", nullValue()));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("인증 사용자 - 상품명 키워드 검색과 페이징 조회")
    void searchProducts_authenticated_returns200() throws Exception {
        PageRequest pageRequest = PageRequest.of(0, 5, Sort.by("id").ascending());
        given(productService.searchProducts("spring", pageRequest)).willReturn(new PageImpl<>(
            List.of(new Product("Spring Boot 4 완벽 가이드", 35000, "Spring Boot 실습서", 50)),
            pageRequest,
            1
        ));

        mockMvc.perform(get("/products")
                .param("keyword", "spring")
                .param("page", "0")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(view().name("products/list"))
            .andExpect(model().attributeExists("productPage"))
            .andExpect(model().attribute("keyword", "spring"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("비인증 사용자 - 상품 목록 접근 시 로그인 페이지로 이동")
    void listProducts_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/products"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN - 상품 등록 폼 조회 성공")
    void addForm_admin_returns200() throws Exception {
        mockMvc.perform(get("/products/add"))
            .andExpect(status().isOk())
            .andExpect(view().name("products/add"))
            .andExpect(model().attributeExists("product"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("일반 USER - 상품 등록 폼 접근 시 403")
    void addForm_user_returns403() throws Exception {
        mockMvc.perform(get("/products/add"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN - 상품 등록 POST 후 목록으로 이동")
    void saveProduct_admin_redirectsToList() throws Exception {
        given(productService.save(any())).willReturn(
            new Product("테스트 상품", 15000, "설명", 10)
        );

        mockMvc.perform(post("/products")
                .with(csrf())
                .param("name", "테스트 상품")
                .param("price", "15000")
                .param("description", "테스트 설명")
                .param("stock", "10"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/products"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("일반 USER - 상품 등록 POST 시 403")
    void saveProduct_user_returns403() throws Exception {
        mockMvc.perform(post("/products")
                .with(csrf())
                .param("name", "테스트 상품")
                .param("price", "15000")
                .param("stock", "10"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN - 상품 삭제 후 목록으로 이동")
    void deleteProduct_admin_redirectsToList() throws Exception {
        willDoNothing().given(productService).deleteById(1L);

        mockMvc.perform(post("/products/1/delete")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/products"));
    }
}
