package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.*;
import dst.ass1.jpa.util.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

@NamedQuery(
        name = Constants.Q_TRIP_BY_STATE,
        query = "select t from Trip t where t.state = :state"
)
public class Trip implements ITrip {
    private Long id;
    private Date created;
    private Date updated;
    private TripState state;
    public Location pickup;
    private Location destination;
    private Collection<Location> stops = new ArrayList<>();
    private TripInfo tripInfo;
    private Match match;
    private Rider rider;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public Date getUpdated() {
        return updated;
    }

    @Override
    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    @Override
    public TripState getState() {
        return state;
    }

    @Override
    public void setState(TripState state) {
        this.state = state;
    }

    @Override
    public ILocation getPickup() {
        return pickup;
    }

    @Override
    public void setPickup(ILocation pickup) {
        this.pickup = (Location) pickup;
    }

    @Override
    public ILocation getDestination() {
        return destination;
    }

    @Override
    public void setDestination(ILocation destination) {
        this.destination = (Location) destination;
    }

    @Override
    public Collection<ILocation> getStops() {
        return new ArrayList<>(stops);
    }

    @Override
    public void setStops(Collection<ILocation> stops) {
        if (stops == null) {
            this.stops = null;
            return;
        }

        this.stops = new ArrayList<>();
        for (var stop : stops) {
            addStop(stop);
        }
    }

    @Override
    public void addStop(ILocation stop) {
        if (this.stops == null) {
            this.stops = new ArrayList<>();
        }
        this.stops.add((Location) stop);
    }

    @Override
    public ITripInfo getTripInfo() {
        return tripInfo;
    }

    @Override
    public void setTripInfo(ITripInfo tripInfo) {
        this.tripInfo = (TripInfo) tripInfo;
    }

    @Override
    public IMatch getMatch() {
        return match;
    }

    @Override
    public void setMatch(IMatch match) {
        this.match = (Match) match;
    }

    @Override
    public IRider getRider() {
        return rider;
    }

    @Override
    public void setRider(IRider rider) {
        this.rider = (Rider) rider;
    }

}