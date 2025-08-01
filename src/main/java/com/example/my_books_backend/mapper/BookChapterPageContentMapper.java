package com.example.my_books_backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.example.my_books_backend.dto.book_chapter_page_content.BookChapterPageContentResponse;
import com.example.my_books_backend.entity.BookChapterPageContent;

@Mapper(componentModel = "spring")
public abstract class BookChapterPageContentMapper {

    @Mapping(target = "chapterTitle", ignore = true) // 必要に応じてサービス層で設定
    public abstract BookChapterPageContentResponse toBookChapterPageContentResponse(BookChapterPageContent pageContent);
}