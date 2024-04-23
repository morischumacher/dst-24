package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.*;
import dst.ass1.jpa.model.*;
import dst.ass1.jpa.model.impl.Organization;
import dst.ass1.jpa.util.*;

import javax.persistence.EntityManager;

public class OrganizationDAO extends AbstractDAO<IOrganization, Organization> implements IOrganizationDAO {
    public OrganizationDAO(EntityManager em) {super(em, Organization.class, Constants.T_ORGANIZATION);}
}
