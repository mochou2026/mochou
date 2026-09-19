package com.mochou.zhiji.photo;

import com.mochou.zhiji.common.ApiResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 照片墙接口。
 *
 * <ul>
 *   <li>POST /api/photos            上传图片（multipart：file + username）</li>
 *   <li>GET  /api/photos            图片列表（?username= 只看某个人的）</li>
 *   <li>GET  /api/photos/{id}/raw   图片原图</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PostMapping
    public ApiResponse<PhotoSummary> upload(@RequestParam("file") MultipartFile file,
                                            @RequestParam("username") String username) {
        return ApiResponse.ok("上传成功", photoService.upload(username, file));
    }

    @GetMapping
    public ApiResponse<List<PhotoSummary>> list(
            @RequestParam(value = "username", required = false) String username) {
        return ApiResponse.ok(photoService.list(username));
    }

    @GetMapping("/{id}/raw")
    public ResponseEntity<byte[]> raw(@PathVariable long id) {
        return photoService.data(id)
                .map(photo -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, photo.contentType())
                        .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                        .body(photo.data()))
                .orElseGet(() -> ResponseEntity.notFound()
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build());
    }
}
