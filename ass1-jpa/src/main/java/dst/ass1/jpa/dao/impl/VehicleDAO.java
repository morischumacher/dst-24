package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.*;
import dst.ass1.jpa.model.*;
import dst.ass1.jpa.model.impl.Vehicle;
import org.hibernate.Session;

import javax.persistence.EntityManager;
import java.util.List;

public class VehicleDAO implements IVehicleDAO {

    private EntityManager em;

    public VehicleDAO(EntityManager em) {
        this.em = em;
    }

    @Override
    public IVehicle findById(Long id) {
        Session session = em.unwrap(org.hibernate.Session.class);
        return session.get(Vehicle.class, id);
    }

    @Override
    public List<IVehicle> findAll() {
        Session session = em.unwrap(org.hibernate.Session.class);
        return session.createQuery("SELECT e FROM Vehicle e").getResultList();
    }
}
