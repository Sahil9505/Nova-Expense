package com.nova.finance.account;

import com.nova.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class Account extends com.nova.common.BaseEntity {

    public enum Type {
        CHECKING, SAVINGS, CREDIT, WALLET, INVESTMENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal balance;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public Account(User user, String name, Type type, String currency, BigDecimal balance) {
        this.user = user;
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.balance = balance;
        this.active = true;
    }
}
