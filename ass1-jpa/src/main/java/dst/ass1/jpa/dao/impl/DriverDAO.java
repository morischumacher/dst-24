package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.IDriverDAO;
import dst.ass1.jpa.model.*;
import dst.ass1.jpa.model.impl.Driver;
import org.hibernate.Session;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.List;

public class DriverDAO implements IDriverDAO {

    private EntityManager em;

    public DriverDAO(EntityManager em) {
        this.em = em;
    }

    @Override
    public IDriver findById(Long id) {
        Session session = em.unwrap(org.hibernate.Session.class);
        return session.get(Driver.class, id);
    }

    @Override
    public List<IDriver> findAll() {
        Session session = em.unwrap(org.hibernate.Session.class);
        return (List<IDriver>) session.createQuery("SELECT e FROM Driver e").getResultList();
    }
}
