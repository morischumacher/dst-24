package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.*;
import dst.ass1.jpa.util.*;

import javax.persistence.*;

@Entity
@Table(name = Constants.T_PAYMENT_INFO)
public class PaymentInfo implements IPaymentInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = Constants.I_PAYMENT_INFO)
    private Long id;
    private String name;
    @Enumerated(EnumType.ORDINAL)
    private PaymentMethod method;
    private Boolean preferred;

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return this.method;
    }

    @Override
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.method = paymentMethod;
    }

    @Override
    public Boolean isPreferred() {
        return this.preferred;
    }

    @Override
    public void setPreferred(Boolean preferred) {
        this.preferred = preferred;
    }
}
