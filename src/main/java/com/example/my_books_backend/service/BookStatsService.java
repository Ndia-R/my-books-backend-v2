package com.example.my_books_backend.service;

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
}
