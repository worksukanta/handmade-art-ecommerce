package com.handmadeart.ecommerce;

import com.handmadeart.ecommerce.entity.AppUser;
import com.handmadeart.ecommerce.entity.CustomOrderImage;
import com.handmadeart.ecommerce.entity.CustomOrderRequest;
import com.handmadeart.ecommerce.entity.UserRole;
import com.handmadeart.ecommerce.exception.ResourceNotFoundException;
import com.handmadeart.ecommerce.repository.CustomOrderImageRepository;
import com.handmadeart.ecommerce.service.CustomOrderImageContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOrderImageContentServiceTest {
    @TempDir Path temporaryDirectory;
    @Mock CustomOrderImageRepository repository;
    private Path root;
    private CustomOrderImageContentService service;

    @BeforeEach void setUp() throws Exception {
        root = Files.createDirectory(temporaryDirectory.resolve("reference-images"));
        service = new CustomOrderImageContentService(root.toString(), repository);
    }

    @Test void ownerReceivesCorrectBytesAndContentType() throws Exception {
        byte[] bytes = {1, 2, 3}; Files.createDirectory(root.resolve("request-7")); Files.write(root.resolve("request-7/picture.png"), bytes);
        AppUser owner = user(10L, UserRole.CUSTOMER); CustomOrderImage image = image(owner, "request-7/picture.png", "image/png");
        when(repository.findById(3L)).thenReturn(Optional.of(image));
        var content = service.getImage(owner, 3L);
        assertThat(content.bytes()).isEqualTo(bytes); assertThat(content.contentType()).isEqualTo(MediaType.IMAGE_PNG);
    }

    @Test void anotherCustomerCannotRetrieveImage() {
        AppUser owner = user(10L, UserRole.CUSTOMER); CustomOrderImage image = image(owner, "request-7/picture.png", "image/png");
        when(repository.findById(3L)).thenReturn(Optional.of(image));
        assertThatThrownBy(() -> service.getImage(user(11L, UserRole.CUSTOMER), 3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void adminCanRetrieveAnyImage() throws Exception {
        Files.createDirectory(root.resolve("request-7")); Files.write(root.resolve("request-7/picture.jpg"), new byte[]{9});
        CustomOrderImage image = image(user(10L, UserRole.CUSTOMER), "request-7/picture.jpg", "image/jpeg");
        when(repository.findById(3L)).thenReturn(Optional.of(image));
        assertThat(service.getImage(user(1L, UserRole.ADMIN), 3L).bytes()).containsExactly(9);
    }

    @Test void unknownImageReturnsNotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getImage(user(10L, UserRole.CUSTOMER), 404L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void traversalCannotEscapeRoot() throws Exception {
        Files.write(temporaryDirectory.resolve("outside.png"), new byte[]{9}); CustomOrderImage image = image(user(10L, UserRole.CUSTOMER), "../outside.png", "image/png");
        when(repository.findById(3L)).thenReturn(Optional.of(image));
        assertThatThrownBy(() -> service.getImage(user(10L, UserRole.CUSTOMER), 3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private AppUser user(Long id, UserRole role) { AppUser user = new AppUser(); ReflectionTestUtils.setField(user, "id", id); user.setRole(role); return user; }
    private CustomOrderImage image(AppUser owner, String reference, String type) { CustomOrderRequest request = new CustomOrderRequest(); request.setUser(owner); CustomOrderImage image = new CustomOrderImage(); image.setCustomOrderRequest(request); image.setStorageReference(reference); image.setContentType(type); return image; }
}
