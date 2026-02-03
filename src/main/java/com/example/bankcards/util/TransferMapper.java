package com.example.bankcards.util;

import com.example.bankcards.dto.transfer.TransferDto;
import com.example.bankcards.entity.Transfer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferMapper {

    @Mapping(target = "fromCardId", source = "fromCard.id")
    @Mapping(target = "fromCardMaskedNumber", ignore = true) // Set manually in TransferService due to encryption
    @Mapping(target = "toCardId", source = "toCard.id")
    @Mapping(target = "toCardMaskedNumber", ignore = true) // Set manually in TransferService due to encryption
    TransferDto toDto(Transfer transfer);
}