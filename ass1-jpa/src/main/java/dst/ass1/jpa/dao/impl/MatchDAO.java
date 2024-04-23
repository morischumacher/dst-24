package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.*;
import dst.ass1.jpa.model.*;
import dst.ass1.jpa.model.impl.Match;
import dst.ass1.jpa.util.*;

import javax.persistence.EntityManager;

public class MatchDAO extends AbstractDAO<IMatch, Match> implements IMatchDAO {
    public MatchDAO(EntityManager em) {super(em, Match.class, Constants.T_MATCH);}
}
