package com.example.my_books_backend.dto.book;

import com.example.my_books_backend.dto.favorite.FavoriteStatsResponse;
import com.example.my_books_backend.dto.review.ReviewStatsResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookStatsResponse {
    private String bookId;
    private ReviewStatsResponse reviews;
    private FavoriteStatsResponse favorites;
}