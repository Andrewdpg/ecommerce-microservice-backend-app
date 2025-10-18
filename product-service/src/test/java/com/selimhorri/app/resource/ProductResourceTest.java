package com.selimhorri.app.resource;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.service.ProductService;

@WebMvcTest(ProductResource.class)
class ProductResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductDto testProductDto;

    @BeforeEach
    void setUp() {
        testProductDto = ProductDto.builder()
                .productId(1)
                .productTitle("Test Product")
                .imageUrl("http://example.com/image.jpg")
                .sku("TEST-SKU-001")
                .priceUnit(99.99)
                .quantity(10)
                .categoryDto(CategoryDto.builder()
                        .categoryId(1)
                        .categoryTitle("Test Category")
                        .imageUrl("http://example.com/image.jpg")
                        .build())
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllProducts() throws Exception {
        // Given
        List<ProductDto> products = Arrays.asList(testProductDto);
        DtoCollectionResponse<ProductDto> response = new DtoCollectionResponse<>(products);
        when(productService.findAll()).thenReturn(products);

        // When & Then
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray())
                .andExpect(jsonPath("$.collection[0].productId").value(1))
                .andExpect(jsonPath("$.collection[0].productTitle").value("Test Product"))
                .andExpect(jsonPath("$.collection[0].sku").value("TEST-SKU-001"));

        verify(productService).findAll();
    }

    @Test
    void testFindById_ShouldReturnProduct() throws Exception {
        // Given
        Integer productId = 1;
        when(productService.findById(productId)).thenReturn(testProductDto);

        // When & Then
        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productTitle").value("Test Product"))
                .andExpect(jsonPath("$.sku").value("TEST-SKU-001"));

        verify(productService).findById(productId);
    }

    @Test
    void testSave_ShouldCreateProduct() throws Exception {
        // Given
        when(productService.save(any(ProductDto.class))).thenReturn(testProductDto);

        // When & Then
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProductDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productTitle").value("Test Product"));

        verify(productService).save(any(ProductDto.class));
    }

    @Test
    void testUpdate_ShouldUpdateProduct() throws Exception {
        // Given
        when(productService.update(any(ProductDto.class))).thenReturn(testProductDto);

        // When & Then
        mockMvc.perform(put("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProductDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productTitle").value("Test Product"));

        verify(productService).update(any(ProductDto.class));
    }

    @Test
    void testUpdateById_ShouldUpdateProduct() throws Exception {
        // Given
        Integer productId = 1;
        when(productService.update(eq(productId), any(ProductDto.class))).thenReturn(testProductDto);

        // When & Then
        mockMvc.perform(put("/api/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProductDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productTitle").value("Test Product"));

        verify(productService).update(eq(productId), any(ProductDto.class));
    }

    @Test
    void testDeleteById_ShouldDeleteProduct() throws Exception {
        // Given
        Integer productId = 1;
        doNothing().when(productService).deleteById(productId);

        // When & Then
        mockMvc.perform(delete("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(productService).deleteById(productId);
    }

    @Test
    void testSave_WithValidProduct_ShouldSucceed() throws Exception {
        // Given
        ProductDto newProduct = ProductDto.builder()
                .productTitle("New Product")
                .sku("NEW-SKU-001")
                .priceUnit(49.99)
                .quantity(5)
                .build();

        ProductDto savedProduct = ProductDto.builder()
                .productId(2)
                .productTitle("New Product")
                .sku("NEW-SKU-001")
                .priceUnit(49.99)
                .quantity(5)
                .build();

        when(productService.save(any(ProductDto.class))).thenReturn(savedProduct);

        // When & Then
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(2))
                .andExpect(jsonPath("$.productTitle").value("New Product"));

        verify(productService).save(any(ProductDto.class));
    }
}
