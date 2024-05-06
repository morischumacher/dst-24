package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.*;
import dst.ass1.jpa.util.*;


import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@AttributeOverride(name = "id", column = @Column(name = Constants.I_RIDER, nullable = false))
@AttributeOverride(name = "name", column = @Column(name = Constants.M_RIDER_NAME, nullable = false))
@AttributeOverride(name = "tel", column = @Column(name = Constants.M_RIDER_TEL, nullable = false))
@AttributeOverride(name = "avgRating", column = @Column(name = Constants.M_RIDER_AVG_RATING))
@Table(
        name = Constants.T_RIDER,
        uniqueConstraints = {@UniqueConstraint(columnNames = {Constants.M_RIDER_EMAIL, Constants.M_RIDER_NAME})}
)
@NamedQueries({
        @NamedQuery(
                name = Constants.Q_RIDER_BY_EMAIL,
                query = "select r from Rider r where r.email = :email"
        ),
        @NamedQuery(
                name = Constants.Q_INACTIVE_RIDER,
                query = "select r from Rider r join r.trips t join t.tripInfo ti where ti.completed between :start and :end"
        )
})
public class Rider extends PlatformUser implements IRider {
    @Column(name = Constants.M_RIDER_EMAIL, nullable = false)
    private String email;

    //hash?
    @Column(name = Constants.M_RIDER_PASSWORD, length = 20)
    private byte[] password;

    //should be one to many
    @ManyToMany
    @JoinTable(
            name = Constants.J_RIDER_PAYMENT_INFO,
            joinColumns = @JoinColumn(name = Constants.I_RIDER),
            inverseJoinColumns = @JoinColumn(name = Constants.I_PAYMENT_INFOS)
    )
    Set<PaymentInfo> paymentInfos;
    @OneToMany(mappedBy = "rider")
    Set<Trip> trips = new HashSet<>();

    public Rider() {super();}

    @Override
    public String getEmail() {
        return this.email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public byte[] getPassword() {
        return this.password;
    }

    @Override
    public void setPassword(byte[] password) {
        this.password = password;
    }

    @Override
    public Collection<ITrip> getTrips() {
        return this.trips.stream().map(trip -> (ITrip) trip).collect(Collectors.toSet());
    }

    @Override
    public void setTrips(Collection<ITrip> trips) {
        this.trips = trips.stream().map(trip -> (Trip) trip).collect(Collectors.toSet());
    }

    @Override
    public void addTrip(ITrip trip) {
        this.trips.add((Trip) trip);
    }

    @Override
    public Collection<IPaymentInfo> getPaymentInfos() {
        return this.paymentInfos.stream().map(pi -> (IPaymentInfo) pi).collect(Collectors.toSet());
    }

    @Override
    public void setPaymentInfos(Collection<IPaymentInfo> paymentInfos) {
        this.paymentInfos = paymentInfos.stream().map(pi -> (PaymentInfo) pi).collect(Collectors.toSet());
    }
}
