package com.satish.mailservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_audit_req_resp_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tid;

    private String method;

    private String url;

    @Column(columnDefinition = "TEXT")
    private String req;

    @Column(columnDefinition = "TEXT")
    private String resp;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime reqTime;

    private LocalDateTime respTime;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private Integer statusCode;

    private String statusMsg;

    @Enumerated(EnumType.STRING)
    private TransactionStatus txnStatus;
}
