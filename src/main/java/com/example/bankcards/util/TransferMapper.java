package com.example.bankcards.util;

import com.example.bankcards.dto.transfer.TransferDto;
import com.example.bankcards.entity.Transfer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferMapper {

    @Mapping(target = "fromCardId", source = "fromCard.id")
    @Mapping(target = "fromCardMaskedNumber", expression = "java(transfer.getFromCard().getMaskedNumber())")
    @Mapping(target = "toCardId", source = "toCard.id")
    @Mapping(target = "toCardMaskedNumber", expression = "java(transfer.getToCard().getMaskedNumber())")
    TransferDto toDto(Transfer transfer);
}