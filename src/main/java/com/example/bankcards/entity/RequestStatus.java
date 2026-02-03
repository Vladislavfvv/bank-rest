package com.example.bankcards.entity;

public enum RequestStatus {
    PENDING,    // Awaiting review
    APPROVED,   // Approved (card blocked)
    REJECTED    // Rejected
}