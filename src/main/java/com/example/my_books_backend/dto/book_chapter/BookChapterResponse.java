package com.example.my_books_backend.dto.book_chapter;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookChapterResponse {
    private Long chapterNumber;
    private String chapterTitle;
    private List<Long> pageNumbers;
}
