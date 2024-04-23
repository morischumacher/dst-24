package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.*;
import dst.ass1.jpa.util.*;

import javax.persistence.*;
import java.util.Date;

@Entity
@NamedQuery(
        name = "countMatchOnDate",
        query = "SELECT COUNT(m) FROM Match m WHERE m.date = :date"
)
public class Match implements IMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = Constants.I_MATCH)
    private Long id;

    @Column(name = Constants.M_MATCH_DATE)
    @Temporal(TemporalType.DATE)
    private Date date;

    @Column(name = Constants.M_MATCH_FARE)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="currency", column= @Column(name="fareCurrency")),
            @AttributeOverride(name="value", column= @Column(name="fareValue"))
    })
    private Money fare;


    @OneToOne
    @JoinColumn(name = Constants.I_TRIP)
    private Trip trip;

    @ManyToOne
    @JoinColumn(name = Constants.I_DRIVER)
    private Driver driver;

    @ManyToOne
    @JoinColumn(name = Constants.I_VEHICLE)
    private Vehicle vehicle;


    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public IMoney getFare() {
        return fare;
    }

    @Override
    public void setFare(IMoney fare) {
        this.fare = (Money) fare;
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
    public IVehicle getVehicle() {
        return vehicle;
    }

    @Override
    public void setVehicle(IVehicle vehicle) {
        this.vehicle = (Vehicle) vehicle;
    }

    @Override
    public IDriver getDriver() {
        return driver;
    }

    @Override
    public void setDriver(IDriver driver) {
        this.driver = (Driver) driver;
    }

}