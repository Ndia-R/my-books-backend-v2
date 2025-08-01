package com.example.my_books_backend.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BookStatsService {

    /**
     * 書籍の統計情報（レビュー数、平均評価、人気度）を更新する
     * 
     * @param bookId 書籍ID
     */
    void updateBookStats(String bookId);

    /**
     * 書籍の統計情報を非同期で更新する
     * 
     * @param bookId 書籍ID
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> updateBookStatsAsync(String bookId);

    /**
     * 複数の書籍の統計情報を一括更新する
     * 
     * @param bookIds 書籍IDリスト
     */
    void updateBookStatsBatch(List<String> bookIds);

    /**
     * 複数の書籍の統計情報を非同期で一括更新する
     * 
     * @param bookIds 書籍IDリスト
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> updateBookStatsBatchAsync(List<String> bookIds);

    /**
     * 全書籍の統計情報を更新する
     */
    void updateAllBookStats();

    /**
     * 全書籍の統計情報を非同期で更新する
     * 
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> updateAllBookStatsAsync();
}
