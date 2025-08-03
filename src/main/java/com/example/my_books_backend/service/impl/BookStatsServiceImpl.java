package com.example.my_books_backend.service.impl;

import java.util.concurrent.CompletableFuture;
import com.example.my_books_backend.dto.review.ReviewStatsResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.repository.BookRepository;
import com.example.my_books_backend.repository.ReviewRepository;
import com.example.my_books_backend.service.BookStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookStatsServiceImpl implements BookStatsService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;

    /**
     * {@inheritDoc}
     */
    @Transactional
    public void updateBookStats(String bookId) {
        ReviewStatsResponse counts = reviewRepository.getReviewStatsResponse(bookId);
        long reviewCount = counts.getReviewCount();
        double averageRating = counts.getAverageRating();

        double popularity = calculatePopularity(reviewCount, averageRating);

        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new NotFoundException("Book not found"));

        book.setReviewCount(reviewCount);
        book.setAverageRating(Math.round(averageRating * 100.0) / 100.0);
        book.setPopularity(Math.round(popularity * 100.0) / 100.0); // 小数点以下2桁に調整

        bookRepository.save(book);
    }

    /**
     * 基本的な重み付きスコアによる人気度計算
     * 計算式: 平均点数 × log(レビュー数 + 1) × 20
     * 
     * @param reviewCount レビュー数
     * @param averageRating 平均評価（0.0-5.0）
     * @return 人気度スコア（0-100程度の範囲）
     */
    private double calculatePopularity(long reviewCount, double averageRating) {
        if (reviewCount == 0 || averageRating == 0.0) {
            return 0.0;
        }

        // 基本的な重み付きスコア
        // Math.log()は自然対数
        double logWeight = Math.log(reviewCount + 1);
        double popularity = averageRating * logWeight * 20;

        return popularity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Async
    public CompletableFuture<Void> updateBookStatsAsync(String bookId) {
        try {
            updateBookStats(bookId);
            log.debug("書籍ID {} の統計情報を非同期で更新完了", bookId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("書籍ID {} の統計情報非同期更新に失敗: {}", bookId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
}
