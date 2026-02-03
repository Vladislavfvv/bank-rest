package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.user.CreateUserFromTokenRequest;
import com.example.bankcards.dto.user.PagedUserResponse;
import com.example.bankcards.dto.user.UpdateUserDto;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardAlreadyExistsException;
import com.example.bankcards.exception.UserAlreadyExistsException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.UserMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({"all", "SimilarLogMessages"})
public class UserService {
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final UserMapper userMapper;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String NOT_FOUND_SUFFIX = " not found!";
    private static final String USER_WITH_EMAIL = "User with email ";
    private static final String PREFIX_WITH_ID = "User with id ";
    private static final String DELETED_LOG = "Deleted {} cards from database using explicit DELETE query. Card IDs: {}";

    /**
     * Creates user from JWT token.
     * Email is taken from token (for uniqueness check),
     * other data from request.
     *  * @param email user email extracted from JWT token
     *      * @param request user data (firstName, lastName, birthDate)
     *      * @return created user
     *      * @throws UserAlreadyExistsException if user with this email already exists
     */


    public UserDto createUserFromToken(String email, CreateUserFromTokenRequest request) {
        log.info("Creating user from token in database: email={}, firstName={}, lastName={}",
                email, request.getFirstName(), request.getLastName());

        // Check if user exists
        Optional<User> existingUserOpt = userRepository.findByEmailNativeQuery(email);

        if (existingUserOpt.isPresent()) {
            // User already exists - update their data instead of throwing exception
            log.info("User with email {} already exists, updating profile data", email);
            User existingUser = existingUserOpt.get();

            // Update profile data
            existingUser.setFirstName(request.getFirstName());
            existingUser.setLastName(request.getLastName());
            existingUser.setBirthDate(request.getBirthDate());

            // Save updated user
            User saved = userRepository.save(existingUser);

            log.info("User profile updated successfully: id={}, email={}, firstName={}, lastName={}",
                    saved.getId(), saved.getEmail(), saved.getFirstName(), saved.getLastName());

            return userMapper.toDto(saved);
        }

        // User doesn't exist - create new one
        // Create DTO with email from token and data from request
        UserDto userDto = new UserDto();
        userDto.setEmail(email);
        userDto.setFirstName(request.getFirstName());
        userDto.setLastName(request.getLastName());
        userDto.setBirthDate(request.getBirthDate());
        userDto.setCards(null); // Cards can be added later through separate endpoint

        // Create user entity
        User entity = userMapper.toEntity(userDto);
        entity.setCards(new ArrayList<>()); // Empty list of cards

        // Hibernate will save the user
        User saved = userRepository.save(entity);

        log.info("User from token successfully saved to database: id={}, email={}, firstName={}, lastName={}",
                saved.getId(), saved.getEmail(), saved.getFirstName(), saved.getLastName());

        return userMapper.toDto(saved);
    }


    /**
     * Create user (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserDto createUser(UserDto dto) {
        log.info("Creating user in database: email={}, firstName={}, lastName={}",
                dto.getEmail(), dto.getFirstName(), dto.getLastName());

        dto.setId(null);
        if (dto.getCards() != null) {
            dto.getCards().forEach(c -> c.setId(null));
        }

        // Check email uniqueness
        if (userRepository.findByEmailNativeQuery(dto.getEmail()).isPresent()) {
            log.warn("User creation failed: email {} already exists", dto.getEmail());
            throw new UserAlreadyExistsException(USER_WITH_EMAIL + dto.getEmail() + " already exists");
        }

        // Create user entity
        User entity = userMapper.toEntity(dto);

        // Bind cards (old and new)
        List<Card> cards = userMapper.updateCards(entity, dto.getCards());
        cards.forEach(c -> c.setUser(entity));
        entity.setCards(cards);

        // Hibernate will save both user and all their cards (CascadeType.ALL)
        User saved = userRepository.save(entity);

        log.info("User successfully saved to database: id={}, email={}, firstName={}, lastName={}",
                saved.getId(), saved.getEmail(), saved.getFirstName(), saved.getLastName());

        return userMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public UserDto findUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(
                        () -> new UserNotFoundException(PREFIX_WITH_ID + id + NOT_FOUND_SUFFIX));

        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true) // for solving lazy initialization problem
    public PagedUserResponse findAllUsers(int page, int size) {
        // Database-level sorting by ID through Sort in PageRequest
        Page<User> users = userRepository.findAll(PageRequest.of(page, size, Sort.by("id").ascending()));

        List<UserDto> listDto = users.stream()
                .map(userMapper::toDto)
                .toList();

        return new PagedUserResponse(
                listDto,
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        log.debug("Searching for user with email: {}", email);

        // Use native query for more reliable search
        // Try native query first, then JPQL, then named query
        Optional<User> userOpt = userRepository.findByEmailNativeQuery(email);
        if (userOpt.isPresent()) {
            log.debug("User found using native query: {}", email);
        } else {
            log.debug("User not found using native query, trying JPQL: {}", email);
            userOpt = userRepository.findByEmailJPQL(email);
            if (userOpt.isPresent()) {
                log.debug("User found using JPQL: {}", email);
            } else {
                log.debug("User not found using JPQL, trying named query: {}", email);
                userOpt = userRepository.findByEmailNamed(email);
                if (userOpt.isPresent()) {
                    log.debug("User found using named query: {}", email);
                }
            }
        }
        User user = userOpt
                .orElseThrow(() -> {
                    log.error("User not found with email: {} (tried all query methods)", email);
                    return new UserNotFoundException(USER_WITH_EMAIL + email + NOT_FOUND_SUFFIX);
                });

        log.debug("User found: id={}, email={}", user.getId(), user.getEmail());
        return userMapper.toDto(user);
    }

    @SuppressWarnings("unused")
    @Transactional(readOnly = true)
    public UserDto getUserByEmailJPQl(String email) {
        User user = userRepository.findByEmailJPQL(email)
                .orElseThrow(() -> new UserNotFoundException(USER_WITH_EMAIL + email + NOT_FOUND_SUFFIX));

        return userMapper.toDto(user);
    }

    @SuppressWarnings("unused")
    @Transactional(readOnly = true)
    public UserDto getUserByEmailNative(String email) {
        User user = userRepository.findByEmailNativeQuery(email)
                .orElseThrow(() -> new UserNotFoundException(USER_WITH_EMAIL + email + NOT_FOUND_SUFFIX));

        return userMapper.toDto(user);
    }

    /**
     * Updates the current user (by email from token).
     * Performs partial update - only provided fields are updated.
     * For cards, holder is automatically generated from user's firstName + lastName.
     *
     * @param userEmail user email from token
     * @param dto DTO with data for update (all fields are optional)
     * @return updated user
     * @throws UserNotFoundException if user is not found
     */
    @Transactional
    public UserDto updateCurrentUser(String userEmail, UpdateUserDto dto) {
        log.info("Updating current user in database: email={}", userEmail);

        // Find user by email from token
        User existUser = userRepository.findByEmailNativeQuery(userEmail)
                .orElseThrow(() -> new UserNotFoundException(USER_WITH_EMAIL + userEmail + NOT_FOUND_SUFFIX));

        log.debug("User found in database: id={}, email={}", existUser.getId(), existUser.getEmail());

        // Update user (without access check, as this is their own profile)
        UserDto updated = updateUserInternal(existUser, dto);

        log.info("User successfully updated in database: id={}, email={}", updated.getId(), updated.getEmail());
        return updated;
    }

    /**
     * Updates user by ID (only for ADMIN).
     * Admin can update any user without access check.
     * Performs partial update - only provided fields are updated.
     * For cards, holder is automatically generated from user's firstName + lastName.
     *
     * @param id user ID for update
     * @param dto DTO with data for update (all fields are optional)
     * @param adminEmail admin email (for logging)
     * @return updated user
     * @throws UserNotFoundException if user is not found
     */
    @Transactional
    public UserDto updateUserByAdmin(Long id, UpdateUserDto dto, String adminEmail) {
        log.info("Admin {} updating user in database: id={}", adminEmail, id);

        // Get user by ID (without access check for admin)
        User existUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(PREFIX_WITH_ID + id + NOT_FOUND_SUFFIX));

        // Update user (without access check, as this is admin)
        UserDto updated = updateUserInternal(existUser, dto);

        log.info("User successfully updated by admin in database: id={}, email={}", updated.getId(), updated.getEmail());
        return updated;
    }

    /**
     * Internal method for updating user.
     * Contains common logic for updating fields and cards.
     *
     * @param existUser user to update
     * @param dto DTO with data for update
     * @return updated user
     */
    private UserDto updateUserInternal(User existUser, UpdateUserDto dto) {
        // Update basic user fields
        updateUserFields(existUser, dto);
        
        // Generate holder name for cards
        String holder = generateCardHolder(existUser);
        
        // Update cards if provided
        if (dto.getCards() != null) {
            updateUserCards(existUser, dto.getCards(), holder);
        }
        
        // Save and return updated user
        return saveAndReloadUser(existUser);
    }
    
    /**
     * Updates basic user fields from DTO.
     */
    private void updateUserFields(User existUser, UpdateUserDto dto) {
        if (dto.getFirstName() != null) {
            existUser.setFirstName(dto.getFirstName());
        }
        if (dto.getLastName() != null) {
            existUser.setLastName(dto.getLastName());
        }
        if (dto.getBirthDate() != null) {
            existUser.setBirthDate(dto.getBirthDate());
        }
    }
    
    /**
     * Generates card-holder name from user's first and last name.
     */
    private String generateCardHolder(User existUser) {
        String holder = (existUser.getFirstName() != null ? existUser.getFirstName() : "") +
                " " +
                (existUser.getLastName() != null ? existUser.getLastName() : "");
        holder = holder.trim();
        
        // If holder is empty, use email as fallback
        if (holder.isEmpty()) {
            holder = existUser.getEmail();
        }
        
        return holder;
    }
    
    /**
     * Updates user's cards based on provided card DTOs.
     */
    private void updateUserCards(User existUser, List<CardDto> cardDtos, String holder) {
        final Long existUserId = existUser.getId();
        
        // Reload user from DB to get current list of cards
        entityManager.refresh(existUser);
        log.debug("Refreshed user from database before card update. User ID: {}, Cards count: {}",
                existUserId, existUser.getCards() != null ? existUser.getCards().size() : 0);
        
        if (cardDtos.isEmpty()) {
            deleteAllUserCards(existUser);
            return;
        }
        
        processCardUpdates(existUser, cardDtos, holder);
    }
    
    /**
     * Deletes all cards for a user.
     */
    private void deleteAllUserCards(User existUser) {
        log.info("Received empty cards list in DTO. Deleting all existing cards for user ID: {}",
                existUser.getId());
        
        if (existUser.getCards() != null && !existUser.getCards().isEmpty()) {
            // Force load cards collection
            int cardCount = existUser.getCards().size();
            
            List<Long> cardIdsToDelete = existUser.getCards().stream()
                    .map(Card::getId)
                    .filter(Objects::nonNull)
                    .toList();
            
            // Use explicit SQL DELETE
            int deletedCount = cardRepository.deleteByIds(cardIdsToDelete);
            cardRepository.flush();
            entityManager.flush();
            
            // Clear Hibernate cache for deleted cards
            existUser.getCards().forEach(entityManager::detach);
            
            log.info(DELETED_LOG, deletedCount, cardIdsToDelete);
        }
        
        // Clear user collection if it exists
        if (existUser.getCards() != null) {
            existUser.getCards().clear();
        }
    }
    
    /**
     * Processes card updates for a user.
     */
    private void processCardUpdates(User existUser, List<CardDto> cardDtos, String holder) {
        final Long existUserId = existUser.getId();
        
        Map<Long, Card> existingCardsMap = existUser.getCards().stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(Card::getId, c -> c));
        
        Set<String> processedCardNumbers = new HashSet<>();
        List<Card> updatedCards = new ArrayList<>();
        
        log.debug("Processing {} cards from DTO. Existing cards in DB: {}",
                cardDtos.size(), existingCardsMap.size());
        
        // Process each card from DTO
        for (CardDto cardDto : cardDtos) {
            Card processedCard = processCardDto(cardDto, existingCardsMap, processedCardNumbers, 
                    existUserId, existUser, holder);
            if (processedCard != null) {
                updatedCards.add(processedCard);
            }
        }
        
        // Save updated cards and handle deletions
        saveCardsAndHandleDeletions(existUser, updatedCards, cardDtos);
    }
    
    /**
     * Processes a single card DTO.
     */
    private Card processCardDto(CardDto cardDto, Map<Long, Card> existingCardsMap, 
            Set<String> processedCardNumbers, Long existUserId, User existUser, String holder) {
        
        String cardNumber = cardDto.getNumber();
        
        // Check for duplicates in current request
        if (processedCardNumbers.contains(cardNumber)) {
            return null; // Skip duplicate in request
        }
        processedCardNumbers.add(cardNumber);
        
        if (cardDto.getId() != null && existingCardsMap.containsKey(cardDto.getId())) {
            // Update existing card
            return updateExistingCard(existingCardsMap.get(cardDto.getId()), cardDto, holder, existUserId);
        } else {
            // Create new card or update existing by number
            return createOrUpdateCardByNumber(cardDto, existUser, holder, existUserId);
        }
    }
    
    /**
     * Updates an existing card.
     */
    private Card updateExistingCard(Card existingCard, CardDto cardDto, String holder, Long existUserId) {
        String cardNumber = cardDto.getNumber();
        
        // If card number changed, check if new number is not taken
        if (!existingCard.getNumber().equals(cardNumber)) {
            validateCardNumberNotTaken(cardNumber, existUserId);
        }
        
        existingCard.setNumber(cardNumber);
        existingCard.setHolder(holder);
        existingCard.setExpirationDate(cardDto.getExpirationDate());
        
        return existingCard;
    }
    
    /**
     * Creates a new card or updates existing by number.
     */
    private Card createOrUpdateCardByNumber(CardDto cardDto, User existUser, String holder, Long existUserId) {
        String cardNumber = cardDto.getNumber();
        
        // Check if such card already exists for this user
        Optional<Card> existingCardForUser = cardRepository
                .findByNumberAndUserId(cardNumber, existUser.getId());
        
        if (existingCardForUser.isPresent()) {
            // Card already exists for this user - update existing
            Card existingCard = existingCardForUser.get();
            existingCard.setHolder(holder);
            existingCard.setExpirationDate(cardDto.getExpirationDate());
            return existingCard;
        }
        
        // Validate card number is not taken by another user
        validateCardNumberNotTaken(cardNumber, existUserId);
        
        // Create new card
        Card newCard = new Card();
        newCard.setNumber(cardNumber);
        newCard.setHolder(holder);
        newCard.setExpirationDate(cardDto.getExpirationDate());
        newCard.setUser(existUser);
        
        return newCard;
    }
    
    /**
     * Validates that card number is not taken by another user.
     */
    private void validateCardNumberNotTaken(String cardNumber, Long existUserId) {
        cardRepository.findByNumber(cardNumber).ifPresent(otherCard -> {
            if (!otherCard.getUser().getId().equals(existUserId)) {
                throw new CardAlreadyExistsException(
                        "Card with number " + cardNumber + " is already registered to another user");
            }
        });
    }
    
    /**
     * Saves updated cards and handles card deletions.
     */
    private void saveCardsAndHandleDeletions(User existUser, List<Card> updatedCards, List<CardDto> cardDtos) {
        // Save all updated cards
        List<Card> savedCards = cardRepository.saveAll(updatedCards);
        cardRepository.flush();
        
        log.info("Flushed card updates to database. Saved {} cards. Card IDs: {}",
                savedCards.size(),
                savedCards.stream().map(Card::getId).filter(Objects::nonNull).toList());
        
        // Handle card deletions if needed
        if (!cardDtos.isEmpty()) {
            handleCardDeletions(existUser, savedCards);
        }
        
        // Update user collection
        existUser.getCards().clear();
        existUser.getCards().addAll(savedCards);
        
        // Set reverse relationship for all cards
        savedCards.forEach(card -> card.setUser(existUser));
    }
    
    /**
     * Handles deletion of cards that are no longer in the updated list.
     */
    private void handleCardDeletions(User existUser, List<Card> savedCards) {
        entityManager.refresh(existUser);
        
        // Force load cards collection by accessing it
        if (existUser.getCards() != null) {
            // Initialize lazy collection by accessing size (result intentionally ignored)
            @SuppressWarnings("unused")
            int collectionSize = existUser.getCards().size();
        }
        
        Set<Long> savedCardIds = savedCards.stream()
                .map(Card::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        List<Card> cardsToRemove = existUser.getCards().stream()
                .filter(card -> card.getId() != null && !savedCardIds.contains(card.getId()))
                .toList();
        
        if (!cardsToRemove.isEmpty()) {
            deleteRemovedCards(existUser, cardsToRemove);
        } else {
            log.debug("No cards to remove. All existing cards are in the new list.");
        }
    }
    
    /**
     * Deletes cards that were removed from the user.
     */
    private void deleteRemovedCards(User existUser, List<Card> cardsToRemove) {
        List<Long> cardIdsToDelete = cardsToRemove.stream()
                .map(Card::getId)
                .filter(Objects::nonNull)
                .toList();
        
        int deletedCount = cardRepository.deleteByIds(cardIdsToDelete);
        cardRepository.flush();
        entityManager.flush();
        
        // Clear Hibernate cache for deleted cards
        cardsToRemove.forEach(entityManager::detach);
        
        log.info(DELETED_LOG, deletedCount, cardIdsToDelete);
        
        // Reload user after deletion
        reloadUserAfterCardDeletion(existUser);
    }
    
    /**
     * Reloads user from database after card deletion.
     */
    private void reloadUserAfterCardDeletion(User existUser) {
        final Long userIdForReload = existUser.getId();
        entityManager.detach(existUser);
        
        User reloadedUser = userRepository.findById(userIdForReload)
                .orElseThrow(() -> new UserNotFoundException(PREFIX_WITH_ID + userIdForReload + NOT_FOUND_SUFFIX));
        
        // Force load cards collection by accessing it
        if (reloadedUser.getCards() != null) {
            // Initialize lazy collection by accessing size (result intentionally ignored)
            @SuppressWarnings("unused")
            int collectionSize = reloadedUser.getCards().size();
        }
        
        log.debug("Reloaded user from database after card deletion. User ID: {}, Cards count: {}",
                reloadedUser.getId(), reloadedUser.getCards() != null ? reloadedUser.getCards().size() : 0);
        
        // Update reference
        existUser.setCards(reloadedUser.getCards());
        existUser.setFirstName(reloadedUser.getFirstName());
        existUser.setLastName(reloadedUser.getLastName());
        existUser.setBirthDate(reloadedUser.getBirthDate());
    }
    
    /**
     * Saves user and reloads from database to get final state.
     */
    private UserDto saveAndReloadUser(User existUser) {
        // Save user and force synchronize with DB
        User savedUser = userRepository.save(existUser);
        userRepository.flush();
        log.debug("Flushed user updates to database. User ID: {}", savedUser.getId());
        
        // Clear Hibernate cache and reload user
        entityManager.detach(savedUser);
        entityManager.clear();
        
        User refreshedUser = userRepository.findById(savedUser.getId())
                .orElseThrow(() -> new UserNotFoundException(PREFIX_WITH_ID + savedUser.getId() + NOT_FOUND_SUFFIX));
        
        // Force load cards collection by accessing it
        if (refreshedUser.getCards() != null) {
            // Initialize lazy collection by accessing size (result intentionally ignored)
            @SuppressWarnings("unused")
            int collectionSize = refreshedUser.getCards().size();
        }
        
        log.info("Reloaded user from database. User ID: {}, Cards count: {}",
                refreshedUser.getId(), refreshedUser.getCards() != null ? refreshedUser.getCards().size() : 0);
        
        // Log warning if cards are still present after deletion
        if (refreshedUser.getCards() != null && !refreshedUser.getCards().isEmpty()) {
            log.warn("WARNING: Cards still present after deletion! User ID: {}, Cards count: {}, Card IDs: {}",
                    refreshedUser.getId(),
                    refreshedUser.getCards().size(),
                    refreshedUser.getCards().stream().map(Card::getId).toList());
        }
        
        return userMapper.toDto(refreshedUser);
    }

    /**
     * Delete user (only for admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(Long id) {
        // Get user to extract email before deletion
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(PREFIX_WITH_ID + id + NOT_FOUND_SUFFIX));

        String email = user.getEmail();

        // Delete from user-service database
        userRepository.deleteById(id);

        log.info("User successfully deleted from database: id={}, email={}", id, email);
    }

    // ========== ADMIN METHODS ==========

    /**
     * Get all users with pagination (only for admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PagedUserResponse getAllUsersForAdmin(int page, int size) {
        log.info("Admin requesting all users, page: {}, size: {}", page, size);
        
        Page<User> users = userRepository.findAll(PageRequest.of(page, size, Sort.by("id").ascending()));
        
        List<UserDto> dtos = users.stream()
                .map(userMapper::toDto)
                .toList();

        PagedUserResponse response = new PagedUserResponse(
                dtos,
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages()
        );
        
        log.info("Admin retrieved {} users", response.getTotalElements());
        return response;
    }

    /**
     * Update user by ID (only for admin)
     */
    @SuppressWarnings("unused")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserDto updateUserByAdminId(Long id, UpdateUserDto dto) {
        log.info("Admin updating user: id={}", id);

        User existUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(PREFIX_WITH_ID + id + NOT_FOUND_SUFFIX));

        UserDto updated = updateUserInternal(existUser, dto);

        log.info("Admin successfully updated user: id={}, email={}", updated.getId(), updated.getEmail());
        return updated;
    }

    /**
     * Search users by email (only for admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<UserDto> searchUsersByEmail(String emailPattern) {
        log.info("Admin searching users by email pattern: {}", emailPattern);
        
        List<User> users = userRepository.findByEmailContainingIgnoreCase(emailPattern);
        List<UserDto> result = users.stream()
                .map(userMapper::toDto)
                .toList();
        
        log.info("Admin found {} users matching email pattern: {}", result.size(), emailPattern);
        return result;
    }

}
