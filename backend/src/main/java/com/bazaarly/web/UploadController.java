package com.bazaarly.web;

import com.bazaarly.config.*;
import com.bazaarly.entity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController @RequestMapping("/api/upload")
public class UploadController {
    private static final Set<String> EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
    @Value("${app.upload-dir}") private String dir;

    @PostMapping
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        User u = Auth.require();
        if (u.getRole() == Enums.Role.CUSTOMER) throw ApiException.forbidden("Only sellers and admins can upload images");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!EXT.contains(ext) || file.getContentType() == null || !file.getContentType().startsWith("image/")) throw ApiException.bad("Only JPG, PNG, WEBP or GIF images are allowed");
        Path root = Paths.get(dir).toAbsolutePath(); Files.createDirectories(root);
        String saved = UUID.randomUUID() + "." + ext;
        Files.copy(file.getInputStream(), root.resolve(saved), StandardCopyOption.REPLACE_EXISTING);
        return Map.of("url", "/uploads/" + saved);
    }
}
