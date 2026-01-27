package com.example.bankcards.service;

import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final CardRepository cardInfoRepository;

    private static final String NOT_FOUND_SUFFIX = " not found!";
    private static final String USER_WITH_EMAIL = "User with email ";
    private static final String PREFIX_WITH_ID = "User with id ";



}
