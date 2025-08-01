package com.example.my_books_backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.my_books_backend.entity.BookChapter;
import com.example.my_books_backend.entity.BookChapterId;

@Repository
public interface BookChapterRepository extends JpaRepository<BookChapter, BookChapterId> {
    List<BookChapter> findByBookId(String bookId);
}
