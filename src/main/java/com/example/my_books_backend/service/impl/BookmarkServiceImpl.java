package com.example.my_books_backend.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.bookmark.BookmarkRequest;
import com.example.my_books_backend.dto.bookmark.BookmarkResponse;
import com.example.my_books_backend.entity.BookChapter;
import com.example.my_books_backend.entity.BookChapterPageContent;
import com.example.my_books_backend.entity.Bookmark;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.ForbiddenException;
import com.example.my_books_backend.exception.NotFoundException;
import com.example.my_books_backend.mapper.BookmarkMapper;
import com.example.my_books_backend.repository.BookChapterPageContentRepository;
import com.example.my_books_backend.repository.BookChapterRepository;
import com.example.my_books_backend.repository.BookmarkRepository;
import com.example.my_books_backend.service.BookmarkService;
import com.example.my_books_backend.util.PageableUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkServiceImpl implements BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final BookmarkMapper bookmarkMapper;

    private final BookChapterRepository bookChapterRepository;
    private final BookChapterPageContentRepository bookChapterPageContentRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResponse<BookmarkResponse> getUserBookmarks(
        User user,
        Long page,
        Long size,
        String sortString,
        String bookId
    ) {
        Pageable pageable = PageableUtils.of(
            page,
            size,
            sortString,
            PageableUtils.BOOKMARK_ALLOWED_FIELDS
        );
        Page<Bookmark> pageObj = (bookId == null)
            ? bookmarkRepository.findByUserAndIsDeletedFalse(user, pageable)
            : bookmarkRepository.findByUserAndIsDeletedFalseAndBookId(user, bookId, pageable);

        // 2クエリ戦略を適用
        Page<Bookmark> updatedPageObj = PageableUtils.applyTwoQueryStrategy(
            pageObj,
            bookmarkRepository::findAllByIdInWithRelations,
            Bookmark::getId
        );

        PageResponse<BookmarkResponse> response = bookmarkMapper.toPageResponse(updatedPageObj);

        // 章タイトルを設定
        enrichWithChapterTitles(response, pageObj.getContent());

        return response;
    }

    /**
     * ブックマークレスポンスに章タイトルを追加する
     * 
     * @param response ブックマークレスポンス
     * @param bookmarks 元のブックマークリスト（書籍IDの取得用）
     */
    private void enrichWithChapterTitles(PageResponse<BookmarkResponse> response, List<Bookmark> bookmarks) {
        // 空のリストの場合は早期リターン
        if (bookmarks.isEmpty() || response.getData().isEmpty()) {
            return;
        }

        // 書籍IDを収集
        Set<String> bookIds = bookmarks.stream()
            .map(bookmark -> bookmark.getPageContent().getBookId())
            .collect(Collectors.toSet());

        // 書籍ごとの章タイトルマップを作成
        Map<String, Map<Long, String>> bookChapterTitleMaps = createBookChapterTitleMaps(bookIds);

        // レスポンスに章タイトルを設定
        for (int i = 0; i < response.getData().size() && i < bookmarks.size(); i++) {
            BookmarkResponse bookmarkResponse = response.getData().get(i);
            Bookmark originalBookmark = bookmarks.get(i);

            Map<Long, String> chapterTitleMap = bookChapterTitleMaps.get(originalBookmark.getPageContent().getBookId());
            if (chapterTitleMap != null) {
                String chapterTitle = chapterTitleMap.get(originalBookmark.getPageContent().getChapterNumber());
                if (chapterTitle != null) {
                    bookmarkResponse.setChapterTitle(chapterTitle);
                }
            }
        }
    }

    /**
     * 指定された書籍IDリストから書籍ごとの章タイトルマップを作成
     * 
     * @param bookIds 書籍IDのセット
     * @return 書籍ID -> (章番号 -> 章タイトル) のマップ
     */
    private Map<String, Map<Long, String>> createBookChapterTitleMaps(Set<String> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Map<Long, String>> result = new HashMap<>();

        for (String bookId : bookIds) {
            List<BookChapter> bookChapters = bookChapterRepository.findByBookId(bookId);
            Map<Long, String> chapterTitleMap = bookChapters.stream()
                .collect(
                    Collectors.toMap(
                        bookChapter -> bookChapter.getId().getChapterNumber(),
                        BookChapter::getTitle
                    )
                );
            result.put(bookId, chapterTitleMap);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public BookmarkResponse createBookmark(BookmarkRequest request, User user) {
        BookChapterPageContent pageContent = bookChapterPageContentRepository
            .findByBookIdAndChapterNumberAndPageNumber(
                request.getBookId(),
                request.getChapterNumber(),
                request.getPageNumber()
            )
            .orElseThrow(() -> new NotFoundException("BookChapterPageContent not found"));

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserAndPageContent(user, pageContent);

        Bookmark bookmark = new Bookmark();
        if (existingBookmark.isPresent()) {
            bookmark = existingBookmark.get();
            if (bookmark.getIsDeleted()) {
                bookmark.setIsDeleted(false);
            } else {
                throw new ConflictException("すでにこのページにはブックマークが登録されています。");
            }
        }
        bookmark.setUser(user);
        bookmark.setPageContent(pageContent);
        bookmark.setNote(request.getNote());

        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        return bookmarkMapper.toBookmarkResponse(savedBookmark);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public BookmarkResponse updateBookmark(Long id, BookmarkRequest request, User user) {
        Bookmark bookmark = bookmarkRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Bookmark not found"));

        if (!bookmark.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("このブックマークを編集する権限がありません。");
        }

        String note = request.getNote();

        if (note != null) {
            bookmark.setNote(note);
        }

        Bookmark savedBookmark = bookmarkRepository.save(bookmark);
        return bookmarkMapper.toBookmarkResponse(savedBookmark);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteBookmark(Long id, User user) {
        Bookmark bookmark = bookmarkRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Bookmark not found"));

        if (!bookmark.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("このブックマークを削除する権限がありません");
        }

        bookmark.setIsDeleted(true);
        bookmarkRepository.save(bookmark);
    }
}
