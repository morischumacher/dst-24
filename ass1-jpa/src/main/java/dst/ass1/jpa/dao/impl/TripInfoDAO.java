package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.*;
import dst.ass1.jpa.model.*;
import dst.ass1.jpa.model.impl.TripInfo;
import dst.ass1.jpa.util.*;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.stream.Collectors;

public class TripInfoDAO extends AbstractDAO<ITripInfo, TripInfo> implements ITripInfoDAO {
    public TripInfoDAO(EntityManager em) {super(em, TripInfo.class, Constants.T_TRIP_INFO);}

    @Override
    public List<TupleResult<Long, Double>> findRidersAverageRating() {
        return this.em.createNamedQuery(Constants.Q_AVERAGE_RATING_RIDER, Object[].class)
                .getResultList().stream().map(tuple -> new TupleResult<Long, Double>((Long) tuple[0], (Double) tuple[1])).collect(Collectors.toList());
    }
}
