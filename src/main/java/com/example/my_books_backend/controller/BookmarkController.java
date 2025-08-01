package com.example.my_books_backend.controller;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.example.my_books_backend.dto.bookmark.BookmarkRequest;
import com.example.my_books_backend.dto.bookmark.BookmarkResponse;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmark", description = "ブックマーク")
public class BookmarkController {
    private final BookmarkService bookmarkService;

    @Operation(description = "ブックマーク追加")
    @PostMapping("")
    public ResponseEntity<BookmarkResponse> createBookmark(
        @Valid @RequestBody BookmarkRequest request,
        @AuthenticationPrincipal User user
    ) {
        BookmarkResponse response = bookmarkService.createBookmark(request, user);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.getId())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(description = "ブックマーク更新")
    @PutMapping("/{id}")
    public ResponseEntity<BookmarkResponse> updateBookmark(
        @PathVariable Long id,
        @Valid @RequestBody BookmarkRequest request,
        @AuthenticationPrincipal User user
    ) {
        BookmarkResponse response = bookmarkService.updateBookmark(id, request, user);
        return ResponseEntity.ok(response);
    }

    @Operation(description = "ブックマーク削除")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookmark(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        bookmarkService.deleteBookmark(id, user);
        return ResponseEntity.noContent().build();
    }
}
