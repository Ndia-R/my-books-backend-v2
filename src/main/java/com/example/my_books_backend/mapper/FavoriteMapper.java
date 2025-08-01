package com.example.my_books_backend.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import com.example.my_books_backend.dto.PageResponse;
import com.example.my_books_backend.dto.favorite.FavoriteResponse;
import com.example.my_books_backend.entity.Favorite;
import com.example.my_books_backend.util.PageableUtils;

@Mapper(componentModel = "spring")
public abstract class FavoriteMapper {

    @Autowired
    protected BookMapper bookMapper;

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "book", expression = "java(bookMapper.toBookResponse(favorite.getBook()))")
    public abstract FavoriteResponse toFavoriteResponse(Favorite favorite);

    public abstract List<FavoriteResponse> toFavoriteResponseList(List<Favorite> favorites);

    public PageResponse<FavoriteResponse> toPageResponse(Page<Favorite> favorites) {
        List<FavoriteResponse> responses = toFavoriteResponseList(favorites.getContent());
        return PageableUtils.toPageResponse(favorites, responses);
    }
}
