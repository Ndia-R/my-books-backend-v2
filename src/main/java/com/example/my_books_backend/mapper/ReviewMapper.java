package com.example.my_books_backend.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.review.ReviewResponse;
import com.example.my_books_backend.entity.Review;
import com.example.my_books_backend.util.PageableUtils;

@Mapper(componentModel = "spring")
public abstract class ReviewMapper {

    @Autowired
    protected BookMapper bookMapper;

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "avatarPath", source = "user.avatarPath")
    @Mapping(target = "book", expression = "java(bookMapper.toBookResponse(review.getBook()))")
    public abstract ReviewResponse toReviewResponse(Review review);

    public abstract List<ReviewResponse> toReviewResponseList(List<Review> reviews);

    public PageResponse<ReviewResponse> toPageResponse(Page<Review> reviews) {
        List<ReviewResponse> responses = toReviewResponseList(reviews.getContent());
        return PageableUtils.toPageResponse(reviews, responses);
    }
}
