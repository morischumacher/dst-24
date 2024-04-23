package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractDAO<I, T> implements GenericDAO<I> {
    protected EntityManager em;
    protected Class<T> cls;
    protected String tableName;

    public AbstractDAO(EntityManager em, Class<T> cls, String tableName) {
        this.em = em;
        this.cls = cls;
        this.tableName = tableName;
    }

    @Override
    public I findById(Long id) {
        return (I) em.find(this.cls, id);
    }

    @Override
    public List<I> findAll() {
        return em.createQuery("from " + this.tableName, this.cls).getResultList().stream().map(result -> (I) result).distinct().collect(Collectors.toCollection(LinkedList::new));
    }
}
