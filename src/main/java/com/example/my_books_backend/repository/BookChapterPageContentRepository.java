package com.example.my_books_backend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.my_books_backend.entity.BookChapterPageContent;

@Repository
public interface BookChapterPageContentRepository extends
    JpaRepository<BookChapterPageContent, Long> {
    
    List<BookChapterPageContent> findByBookIdAndChapterNumber(
        String bookId,
        Long chapterNumber
    );
    
    Optional<BookChapterPageContent> findByBookIdAndChapterNumberAndPageNumber(
        String bookId,
        Long chapterNumber,
        Long pageNumber
    );
}
