package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.*;
import dst.ass1.jpa.model.*;
import dst.ass1.jpa.model.impl.Location;
import dst.ass1.jpa.util.*;

import javax.persistence.EntityManager;

public class LocationDAO extends AbstractDAO<ILocation, Location> implements ILocationDAO {
    public LocationDAO(EntityManager em) {super(em, Location.class, Constants.T_LOCATION);}
}
