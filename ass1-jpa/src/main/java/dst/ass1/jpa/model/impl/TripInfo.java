package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.*;
import dst.ass1.jpa.util.*;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = Constants.T_TRIP_INFO)
@NamedQuery(
        name = Constants.Q_AVERAGE_RATING_RIDER,
        query = "select r.id, avg(ti.riderRating) from TripInfo ti join ti.trip tr join tr.rider r group by r.id order by avg(ti.riderRating) desc"
)
public class TripInfo implements ITripInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = Constants.I_TRIP_INFO)
    private Long id;

    @Column(name = Constants.M_TRIP_INFO_COMPLETED)
    private Date completed;

    @Column(name = Constants.M_TRIP_INFO_DISTANCE)
    private Double distance;

    @Column(name = Constants.M_TRIP_INFO_DRIVER_RATING)
    private Integer driverRating;

    @Column(name = Constants.M_TRIP_INFO_RIDER_RATING)
    private Integer riderRating;

    @OneToOne
    @JoinColumn(name = Constants.I_TRIP_RECEIPT)
    private TripReceipt tripReceipt;

    @OneToOne
    @JoinColumn(name = Constants.I_TRIP, nullable = false)
    private Trip trip;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Date getCompleted() {
        return completed;
    }

    @Override
    public void setCompleted(Date completed) {
        this.completed = completed;
    }

    @Override
    public Double getDistance() {
        return distance;
    }

    @Override
    public void setDistance(Double distance) {
        this.distance = distance;
    }

    @Override
    public Integer getDriverRating() {
        return driverRating;
    }

    @Override
    public void setDriverRating(Integer driverRating) {
        this.driverRating = driverRating;
    }

    @Override
    public Integer getRiderRating() {
        return riderRating;
    }

    @Override
    public void setRiderRating(Integer riderRating) {
        this.riderRating = riderRating;
    }

    @Override
    public ITrip getTrip() {
        return trip;
    }

    @Override
    public void setTrip(ITrip trip) {
        this.trip = (Trip) trip;
    }

    @Override
    public ITripReceipt getReceipt() {
        return tripReceipt;
    }

    @Override
    public void setReceipt(ITripReceipt receipt) {
        this.tripReceipt = (TripReceipt) receipt;
    }
}
