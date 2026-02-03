package com.example.bankcards.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated user response.
 * Used to return paginated list of users with pagination metadata.
 * Contains user data along with page information for frontend pagination controls.
 */
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
