package com.ssafy.home.dto;

import lombok.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
@Setter
@ToString
@NoArgsConstructor
@RequiredArgsConstructor
public class PageDTO<T> {
    private @NonNull List<T> list;
    private @NonNull int currentPage;
    private int totalPages;
    private @NonNull int totalRecords;
    private @NonNull int pageSize;
    private int startPage;
    private int endPage;
    private boolean hasPrev;
    private boolean hasNext;
}