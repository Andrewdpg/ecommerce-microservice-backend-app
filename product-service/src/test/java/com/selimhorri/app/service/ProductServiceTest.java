package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.impl.ProductServiceImpl;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private ProductDto testProductDto;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .productId(1)
                .productTitle("Test Product")
                .imageUrl("http://example.com/image.jpg")
                .category(Category.builder()
                        .categoryId(1)
                        .categoryTitle("Test Category")
                        .imageUrl("http://example.com/image.jpg")
                        .build())
                .sku("TEST-SKU-001")
                .priceUnit(99.99)
                .quantity(10)
                .build();

        testProductDto = ProductDto.builder()
                .productId(1)
                .productTitle("Test Product")
                .imageUrl("http://example.com/image.jpg")
                .categoryDto(CategoryDto.builder()
                        .categoryId(1)
                        .categoryTitle("Test Category")
                        .imageUrl("http://example.com/image.jpg")
                        .build())
                .sku("TEST-SKU-001")
                .priceUnit(99.99)
                .quantity(10)
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllProducts() {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        when(productRepository.findAll()).thenReturn(products);

        // When
        List<ProductDto> result = productService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Product", result.get(0).getProductTitle());
        verify(productRepository).findAll();
    }

    @Test
    void testFindById_WhenProductExists_ShouldReturnProduct() {
        // Given
        Integer productId = 1;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        // When
        ProductDto result = productService.findById(productId);

        // Then
        assertNotNull(result);
        assertEquals(productId, result.getProductId());
        assertEquals("Test Product", result.getProductTitle());
        verify(productRepository).findById(productId);
    }

    @Test
    void testFindById_WhenProductNotExists_ShouldThrowException() {
        // Given
        Integer productId = 999;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // When & Then
        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.findById(productId)
        );
        
        assertTrue(exception.getMessage().contains("Product with id: 999 not found"));
        verify(productRepository).findById(productId);
    }

    @Test
    void testSave_ShouldReturnSavedProduct() {
        // Given
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        ProductDto result = productService.save(testProductDto);

        // Then
        assertNotNull(result);
        assertEquals(testProductDto.getProductId(), result.getProductId());
        assertEquals(testProductDto.getProductTitle(), result.getProductTitle());
        verify(productRepository).save(any(Product.class));
    }


    @Test
    void testSave_WithValidData_ShouldSucceed() {
        // Given
        ProductDto newProduct = ProductDto.builder()
                .productTitle("New Product")
                .sku("NEW-SKU-001")
                .categoryDto(CategoryDto.builder()
                        .categoryId(1)
                        .categoryTitle("New Category")
                        .imageUrl("http://example.com/image.jpg")
                        .build())
                .priceUnit(49.99)
                .quantity(5)
                .build();

        Product savedProduct = Product.builder()
                .productId(2)
                .productTitle("New Product")
                .sku("NEW-SKU-001")
                .category(Category.builder()
                        .categoryId(1)
                        .categoryTitle("New Category")
                        .imageUrl("http://example.com/image.jpg")
                        .build())
                .priceUnit(49.99)
                .quantity(5)
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // When
        ProductDto result = productService.save(newProduct);

        // Then
        assertNotNull(result);
        assertEquals("New Product", result.getProductTitle());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void testUpdate_WithValidData_ShouldSucceed() {
        // Given
        ProductDto updatedProduct = ProductDto.builder()
                .productId(1)
                .productTitle("Updated Product")
                .sku("UPDATED-SKU-001")
                .categoryDto(CategoryDto.builder()
                        .categoryId(1)
                        .categoryTitle("Updated Category")
                        .imageUrl("http://example.com/image.jpg")
                        .build())
                .priceUnit(149.99)
                .quantity(15)
                .build();

        Product savedProduct = Product.builder()
                .productId(1)
                .productTitle("Updated Product")
                .sku("UPDATED-SKU-001")
                .category(Category.builder()
                        .categoryId(1)
                        .categoryTitle("Updated Category")
                        .imageUrl("http://example.com/image.jpg")
                        .build())
                .priceUnit(149.99)
                .quantity(15)
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // When
        ProductDto result = productService.update(updatedProduct);

        // Then
        assertNotNull(result);
        assertEquals("Updated Product", result.getProductTitle());
        verify(productRepository).save(any(Product.class));
    }
}
