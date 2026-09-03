package com.handmadeart.ecommerce.controller;

import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.service.CurrentUserService;
import com.handmadeart.ecommerce.service.CustomOrderImageContentService;
import com.handmadeart.ecommerce.service.CustomOrderImageContentService.ImageContent;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/custom-request-images")
public class CustomOrderImageController {
    private final CurrentUserService currentUserService;
    private final CustomOrderImageContentService contentService;

    public CustomOrderImageController(CurrentUserService currentUserService,
                                      CustomOrderImageContentService contentService) {
        this.currentUserService = currentUserService;
        this.contentService = contentService;
    }

    @GetMapping("/{imageId}/content")
    public ResponseEntity<byte[]> getImageContent(@PathVariable Long imageId) {
        AppUser currentUser = currentUserService.getAuthenticatedUser();
        ImageContent content = contentService.getImage(currentUser, imageId);
        return ResponseEntity.ok()
                .contentType(content.contentType())
                .contentLength(content.bytes().length)
                .cacheControl(CacheControl.noStore())
                .body(content.bytes());
    }
}
