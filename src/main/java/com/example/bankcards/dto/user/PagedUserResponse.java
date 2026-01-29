package com.example.bankcards.dto.user;

import com.example.bankcards.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagedUserResponse {
    private List<UserDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
