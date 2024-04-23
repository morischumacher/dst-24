package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.*;
import dst.ass1.jpa.model.*;
import dst.ass1.jpa.model.impl.Employment;
import dst.ass1.jpa.util.*;

import javax.persistence.EntityManager;

public class EmploymentDAO extends AbstractDAO<IEmployment, Employment> implements IEmploymentDAO {
    public EmploymentDAO(EntityManager em) {super(em, Employment.class, Constants.T_EMPLOYMENT);}
}

