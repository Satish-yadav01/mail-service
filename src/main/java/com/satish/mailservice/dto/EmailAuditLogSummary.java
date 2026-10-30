package com.satish.mailservice.dto;

import com.satish.mailservice.entity.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailAuditLogSummary {
    private Long tid;
    private Long userId;
    private String url;
    private Integer statusCode;
    private String statusMsg;
    private TransactionStatus txnStatus;
}
