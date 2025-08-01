package com.example.my_books_backend.service.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.example.my_books_backend.dto.review.ReviewStatsResponse;
import com.example.my_books_backend.entity.Book;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.repository.BookRepository;
import com.example.my_books_backend.repository.ReviewRepository;
import com.example.my_books_backend.service.BookStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateBookStatsBatch(List<String> bookIds) {
        log.info("バッチ処理開始: {}件の書籍の統計情報を更新", bookIds.size());

        for (String bookId : bookIds) {
            try {
                updateBookStats(bookId);
            } catch (Exception e) {
                log.error("書籍ID {} の統計情報更新に失敗: {}", bookId, e.getMessage());
                // 個別の失敗はログに記録するが、バッチ処理は継続
            }
        }

        log.info("バッチ処理完了: {}件の書籍の統計情報を更新", bookIds.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Async
    public CompletableFuture<Void> updateBookStatsBatchAsync(List<String> bookIds) {
        try {
            updateBookStatsBatch(bookIds);
            log.info("{}件の書籍の統計情報を非同期で一括更新完了", bookIds.size());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("書籍統計情報の非同期一括更新に失敗: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateAllBookStats() {
        log.info("全書籍の統計情報更新を開始");

        long pageSize = 100;
        long pageNumber = 0;
        long totalProcessed = 0;

        while (true) {
            Page<Book> bookPage = bookRepository.findByIsDeletedFalse(
                PageRequest.of((int) pageNumber, (int) pageSize)
            );

            if (bookPage.isEmpty()) {
                break;
            }

            List<String> bookIds = bookPage.getContent()
                .stream()
                .map(Book::getId)
                .toList();

            updateBookStatsBatch(bookIds);

            totalProcessed += bookPage.getContent().size();
            pageNumber++;

            log.info("進捗: {}件の書籍を処理完了", totalProcessed);
        }

        log.info("全書籍の統計情報更新完了: 合計{}件", totalProcessed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Async
    public CompletableFuture<Void> updateAllBookStatsAsync() {
        try {
            updateAllBookStats();
            log.info("全書籍の統計情報を非同期で更新完了");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("全書籍統計情報の非同期更新に失敗: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
}
