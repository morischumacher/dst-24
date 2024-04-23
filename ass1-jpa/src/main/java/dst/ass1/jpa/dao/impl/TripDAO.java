package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.*;
import dst.ass1.jpa.model.*;
import dst.ass1.jpa.model.impl.Trip;
import dst.ass1.jpa.util.*;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;

public class TripDAO extends AbstractDAO<ITrip, Trip> implements ITripDAO {
    public TripDAO(EntityManager em) {super(em, Trip.class, Constants.T_TRIP);}

    @Override
    public List<ITrip> findByStatus(TripState state) {
        return this.em.createNamedQuery(Constants.Q_TRIP_BY_STATE, this.cls).setParameter("state", state).getResultList().stream().map(result -> (ITrip) result).collect(Collectors.toList());
    }
}
