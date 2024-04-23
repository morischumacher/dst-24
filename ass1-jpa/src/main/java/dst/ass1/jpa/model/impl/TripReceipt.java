package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.*;
import dst.ass1.jpa.util.*;

import javax.persistence.*;

@Entity
@Table(name = Constants.T_TRIP_RECEIPT)
public class TripReceipt implements ITripReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = Constants.I_TRIP_RECEIPT)
    private Long id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="currency", column= @Column(name="currency")),
            @AttributeOverride(name="currencyValue", column= @Column(name="currencyValue"))
    })
    private Money total;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="currency", column= @Column(name="tipCurrency")),
            @AttributeOverride(name="currencyValue", column= @Column(name="tipCurrencyValue"))
    })
    private Money tip;

    private boolean paid;

    @ManyToOne
    @JoinColumn(name = Constants.I_PAYMENT_INFO)
    private TripInfo tripInfo;

    @OneToOne
    @JoinColumn(name = Constants.I_TRIP_INFO)
    private PaymentInfo paymentInfo;

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public IMoney getTotal() {
        return this.total;
    }

    @Override
    public void setTotal(IMoney total) {
        this.total = (Money) total;
    }

    @Override
    public IMoney getTip() {
        return this.tip;
    }

    @Override
    public void setTip(IMoney tip) {
        this.tip = (Money) tip;
    }

    @Override
    public boolean isPaid() {
        return this.paid;
    }

    @Override
    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    @Override
    public IPaymentInfo getPaymentInfo() {
        return this.paymentInfo;
    }

    @Override
    public void setPaymentInfo(IPaymentInfo paymentInfo) {
        this.paymentInfo = (PaymentInfo) paymentInfo;
    }

    @Override
    public ITripInfo getTripInfo() {
        return this.tripInfo;
    }

    @Override
    public void setTripInfo(ITripInfo tripInfo) {
        this.tripInfo = (TripInfo) tripInfo;
    }
}