package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.*;
import dst.ass1.jpa.model.*;
import dst.ass1.jpa.model.impl.Rider;
import dst.ass1.jpa.util.*;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class RiderDAO extends AbstractDAO<IRider, Rider> implements IRiderDAO {

    public RiderDAO(EntityManager entityManager) {
        super(entityManager, Rider.class, Constants.T_RIDER);
    }

    @Override
    public List<IRider> findRidersWithNoTrips(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end parameters are required.");
        }

        TypedQuery<Rider> inactiveRiderQuery = em.createNamedQuery(Constants.Q_INACTIVE_RIDER, Rider.class)
                .setParameter("start", start)
                .setParameter("end", end);

        List<Rider> ridersWithTrips = inactiveRiderQuery.getResultList();
        List<Rider> allRiders = em.createQuery("FROM " + tableName, cls).getResultList();

        allRiders.removeAll(ridersWithTrips);
        return allRiders.stream().map(rider -> (IRider) rider).collect(Collectors.toList());
    }

    @Override
    public IRider findByEmail(String email) {
        TypedQuery<Rider> query = em.createNamedQuery(Constants.Q_RIDER_BY_EMAIL, cls)
                .setParameter("email", email);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
