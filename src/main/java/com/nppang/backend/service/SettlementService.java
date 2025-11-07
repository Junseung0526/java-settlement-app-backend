package com.nppang.backend.service;

import com.nppang.backend.dto.ReceiptDto;
import com.nppang.backend.entity.Receipt;
import com.nppang.backend.entity.Settlement;
import com.nppang.backend.entity.UserGroup;
import com.nppang.backend.repository.ReceiptRepository;
import com.nppang.backend.repository.SettlementRepository;
import com.nppang.backend.repository.UserGroupRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final ReceiptRepository receiptRepository;
    private final UserGroupRepository userGroupRepository;
    private final OcrService ocrService;

    @Transactional
    public Settlement createSettlement(String settlementName, Long groupId) {
        UserGroup userGroup = null;
        if (groupId != null) {
            userGroup = userGroupRepository.findById(groupId)
                    .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + groupId));
        }

        Settlement settlement = new Settlement();
        settlement.setName(settlementName);
        settlement.setUserGroup(userGroup);
        return settlementRepository.save(settlement);
    }

    @Transactional
    public Receipt addReceiptToSettlement(Long settlementId, MultipartFile file) throws TesseractException, IOException {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new EntityNotFoundException("Settlement not found with id: " + settlementId));

        ReceiptDto parsedInfo = ocrService.doOcrAndParse(file);

        Receipt receipt = Receipt.builder()
                .settlement(settlement)
                .storeName(parsedInfo.getStoreName())
                .transactionDate(parsedInfo.getTransactionDate())
                .totalAmount(parsedInfo.getTotalAmount())
                .alcoholAmount(parsedInfo.getAlcoholAmount())
                .rawText(parsedInfo.getRawText())
                .build();

        return receiptRepository.save(receipt);
    }

    public Settlement getSettlement(Long settlementId) {
        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new EntityNotFoundException("Settlement not found with id: " + settlementId));
    }
}
