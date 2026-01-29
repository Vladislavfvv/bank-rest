package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.CardTransferStatsDto;
import com.example.bankcards.dto.transfer.TransferDto;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.UserTransferStatsDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.TransferStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidTransferException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.SecurityUtils;
import com.example.bankcards.util.TransferMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {
    
    private final TransferRepository transferRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final TransferMapper transferMapper;

    /**
     * Выполнить перевод между картами пользователя
     */
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public TransferDto transferBetweenCards(TransferRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = SecurityUtils.getEmailFromToken(auth);
        
        log.info("Processing transfer request from user: {}", userEmail);

        // Получаем карты
        Card fromCard = cardRepository.findById(request.getFromCardId())
                .orElseThrow(() -> new CardNotFoundException("Карта отправителя не найдена"));
        
        Card toCard = cardRepository.findById(request.getToCardId())
                .orElseThrow(() -> new CardNotFoundException("Карта получателя не найдена"));

        // Проверяем права доступа
        validateUserAccess(userEmail, fromCard, toCard);
        
        // Проверяем CVV
        if (!fromCard.verifyCvv(request.getCvv())) {
            throw new InvalidTransferException("Неверный CVV код");
        }

        // Проверяем возможность перевода
        validateTransfer(fromCard, toCard, request.getAmount());

        // Выполняем перевод
        fromCard.debit(request.getAmount());
        toCard.credit(request.getAmount());

        // Сохраняем карты
        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        // Создаем запись о переводе
        Transfer transfer = new Transfer();
        transfer.setFromCard(fromCard);
        transfer.setToCard(toCard);
        transfer.setAmount(request.getAmount());
        transfer.setDescription(request.getDescription());
        transfer.setStatus(TransferStatus.COMPLETED);

        Transfer savedTransfer = transferRepository.save(transfer);

        log.info("Transfer completed successfully: {} -> {}, amount: {}", 
                fromCard.getMaskedNumber(), toCard.getMaskedNumber(), request.getAmount());

        return transferMapper.toDto(savedTransfer);
    }

    /**
     * Получить историю переводов пользователя
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public Page<TransferDto> getUserTransfers(int page, int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = SecurityUtils.getEmailFromToken(auth);

        PageRequest pageRequest = PageRequest.of(page, size, 
                Sort.by("transferDate").descending());

        Page<Transfer> transfers = transferRepository.findByUserEmail(userEmail, pageRequest);
        
        return transfers.map(transferMapper::toDto);
    }

    /**
     * Получить историю переводов по конкретной карте (для пользователя)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public Page<TransferDto> getCardTransfers(Long cardId, int page, int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = SecurityUtils.getEmailFromToken(auth);

        // Проверяем, что карта принадлежит пользователю
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Карта не найдена"));
        
        if (!card.getUser().getEmail().equals(userEmail)) {
            throw new InvalidTransferException("Нет доступа к истории переводов этой карты");
        }

        PageRequest pageRequest = PageRequest.of(page, size, 
                Sort.by("transferDate").descending());

        Page<Transfer> transfers = transferRepository.findByCardId(cardId, pageRequest);
        
        return transfers.map(transferMapper::toDto);
    }

    /**
     * Получить историю переводов по конкретной карте (для админа)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<TransferDto> getCardTransfersForAdmin(Long cardId, int page, int size) {
        // Проверяем, что карта существует
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException("Карта не найдена");
        }

        PageRequest pageRequest = PageRequest.of(page, size, 
                Sort.by("transferDate").descending());

        Page<Transfer> transfers = transferRepository.findByCardId(cardId, pageRequest);
        
        return transfers.map(transferMapper::toDto);
    }

    /**
     * Получить статистику переводов по карте (для админа)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public CardTransferStatsDto getCardTransferStats(Long cardId) {
        // Проверяем, что карта существует
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Карта не найдена"));

        BigDecimal totalIncome = transferRepository.getTotalIncomeByCardId(cardId);
        BigDecimal totalExpense = transferRepository.getTotalExpenseByCardId(cardId);
        Long incomeCount = transferRepository.getIncomeTransfersCountByCardId(cardId);
        Long expenseCount = transferRepository.getExpenseTransfersCountByCardId(cardId);

        return new CardTransferStatsDto(
                cardId,
                card.getMaskedNumber(),
                totalIncome,
                totalExpense,
                card.getBalance(),
                incomeCount,
                expenseCount
        );
    }

    /**
     * Получить статистику переводов по пользователю в разрезе карт (для админа)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public UserTransferStatsDto getUserTransferStats(Long userId) {
        // Проверяем, что пользователь существует
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("Пользователь не найден");
        }

        User user = userRepository.findById(userId).get();
        
        // Получаем общую статистику по пользователю
        BigDecimal totalUserIncome = transferRepository.getTotalIncomeByUserId(userId);
        BigDecimal totalUserExpense = transferRepository.getTotalExpenseByUserId(userId);
        
        // Получаем статистику по каждой карте пользователя
        List<CardTransferStatsDto> cardStats = user.getCards().stream()
                .map(card -> {
                    BigDecimal cardIncome = transferRepository.getTotalIncomeByCardId(card.getId());
                    BigDecimal cardExpense = transferRepository.getTotalExpenseByCardId(card.getId());
                    Long incomeCount = transferRepository.getIncomeTransfersCountByCardId(card.getId());
                    Long expenseCount = transferRepository.getExpenseTransfersCountByCardId(card.getId());
                    
                    return new CardTransferStatsDto(
                            card.getId(),
                            card.getMaskedNumber(),
                            cardIncome,
                            cardExpense,
                            card.getBalance(),
                            incomeCount,
                            expenseCount
                    );
                })
                .collect(Collectors.toList());

        // Общий баланс по всем картам
        BigDecimal totalBalance = user.getCards().stream()
                .map(Card::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new UserTransferStatsDto(
                userId,
                user.getEmail(),
                user.getFullName(),
                totalUserIncome,
                totalUserExpense,
                totalBalance,
                cardStats
        );
    }

    private void validateUserAccess(String userEmail, Card fromCard, Card toCard) {
        if (!fromCard.getUser().getEmail().equals(userEmail)) {
            throw new InvalidTransferException("Нет доступа к карте отправителя");
        }
        if (!toCard.getUser().getEmail().equals(userEmail)) {
            throw new InvalidTransferException("Нет доступа к карте получателя");
        }
    }

    private void validateTransfer(Card fromCard, Card toCard, BigDecimal amount) {
        // Проверяем, что карты разные
        if (fromCard.getId().equals(toCard.getId())) {
            throw new InvalidTransferException("Нельзя переводить на ту же карту");
        }

        // Проверяем статус карт
        if (!fromCard.isActive()) {
            throw new InvalidTransferException("Карта отправителя неактивна");
        }
        if (!toCard.isActive()) {
            throw new InvalidTransferException("Карта получателя неактивна");
        }

        // Проверяем баланс
        if (!fromCard.canDebit(amount)) {
            throw new InsufficientFundsException("Недостаточно средств на карте");
        }
    }
}