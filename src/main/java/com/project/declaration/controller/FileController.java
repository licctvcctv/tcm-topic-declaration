package com.project.declaration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.declaration.common.Result;
import com.project.declaration.entity.Institution;
import com.project.declaration.entity.TopicDeclaration;
import com.project.declaration.entity.ExpertReviewTask;
import com.project.declaration.entity.User;
import com.project.declaration.security.SecurityUtils;
import com.project.declaration.mapper.InstitutionMapper;
import com.project.declaration.mapper.TopicDeclarationMapper;
import com.project.declaration.mapper.ExpertReviewTaskMapper;
import com.project.declaration.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private InstitutionMapper institutionMapper;

    @Autowired
    private TopicDeclarationMapper topicMapper;

    @Autowired
    private ExpertReviewTaskMapper expertReviewTaskMapper;

    @Autowired
    private UserMapper userMapper;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".pdf", ".doc", ".docx", ".png", ".jpg", ".jpeg");

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        if (file.getSize() > 50 * 1024 * 1024) {
            return Result.error("文件大小超出 50MB 限制");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.error("不支持的文件格式。仅允许上传 PDF, Word(doc/docx) 或图片(png/jpg/jpeg) 格式。");
        }

        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String newFilename = UUID.randomUUID().toString() + extension;
            File targetFile = new File(dir, newFilename);
            file.transferTo(targetFile);

            String downloadUrl = "/api/files/download/" + newFilename;
            return Result.success(downloadUrl);
        } catch (IOException e) {
            return Result.error("文件保存失败: " + e.getMessage());
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        User currentUser = securityUtils.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        String fileUrl = "/api/files/download/" + filename;
        boolean hasAccess = false;
        
        if ("SUPER_ADMIN".equals(currentUser.getRole())) {
            hasAccess = true;
        } else {
            Institution inst = institutionMapper.selectOne(
                    new LambdaQueryWrapper<Institution>().eq(Institution::getLicenseUrl, fileUrl));
            if (inst != null) {
                if ("ORG_ADMIN".equals(currentUser.getRole()) && currentUser.getOrgId().equals(inst.getId())) {
                    hasAccess = true;
                }
            }

            User expertUser = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getExpertSignature, fileUrl));
            if (expertUser != null) {
                if (currentUser.getId().equals(expertUser.getId())) {
                    hasAccess = true;
                }
            }

            TopicDeclaration topic = topicMapper.selectOne(
                    new LambdaQueryWrapper<TopicDeclaration>()
                            .eq(TopicDeclaration::getTaskBookUrl, fileUrl)
                            .or()
                            .eq(TopicDeclaration::getAnonymousPageUrl, fileUrl));
            if (topic != null) {
                if (currentUser.getId().equals(topic.getDeclarerId())) {
                    hasAccess = true;
                }
                else if ("ORG_ADMIN".equals(currentUser.getRole()) && currentUser.getOrgId().equals(topic.getOrgId())) {
                    hasAccess = true;
                }
                else if ("EXPERT".equals(currentUser.getRole()) && fileUrl.equals(topic.getAnonymousPageUrl())) {
                    if (topic.getStatus() < 6) {
                        Long taskCount = expertReviewTaskMapper.selectCount(
                                new LambdaQueryWrapper<ExpertReviewTask>()
                                        .eq(ExpertReviewTask::getTopicId, topic.getId())
                                        .eq(ExpertReviewTask::getExpertId, currentUser.getId())
                                        .eq(ExpertReviewTask::getInvitationStatus, 1));
                        if (taskCount > 0) {
                            hasAccess = true;
                        }
                    }
                }
            }
        }

        if (!hasAccess) {
            return ResponseEntity.status(403).build();
        }

        try {
            Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = basePath.resolve(filename).normalize();
            if (!filePath.startsWith(basePath)) {
                return ResponseEntity.status(403).build();
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                String contentType = "application/octet-stream";
                if (filename.toLowerCase().endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (filename.toLowerCase().endsWith(".png")) {
                    contentType = "image/png";
                } else if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (filename.toLowerCase().endsWith(".doc") || filename.toLowerCase().endsWith(".docx")) {
                    contentType = "application/msword";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
