package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.service.ProductImageContentService;
import com.handmadeart.ecommerce.service.ProductImageContentService.ProductImageContent;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/** Public delivery endpoint for images attached to active catalogue products. */
@RestController
@RequestMapping("/api/v1/product-images")
public class ProductImageController {

    private final ProductImageContentService productImageContentService;

    public ProductImageController(ProductImageContentService productImageContentService) {
        this.productImageContentService = productImageContentService;
    }

    @GetMapping("/{imageId}/content")
    public ResponseEntity<byte[]> getImageContent(@PathVariable Long imageId) {
        ProductImageContent content = productImageContentService.getPublicImage(imageId);
        return ResponseEntity.ok()
                .contentType(content.contentType())
                .contentLength(content.bytes().length)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(content.bytes());
    }
}
