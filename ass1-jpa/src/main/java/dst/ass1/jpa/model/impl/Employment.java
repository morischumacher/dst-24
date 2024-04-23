package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.*;
import dst.ass1.jpa.util.*;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Employment implements IEmployment {

    @EmbeddedId
    @Column(name = Constants.I_EMPLOYMENT)
    private EmploymentKey id;

    @Temporal(TemporalType.DATE)
    @Column(name = Constants.M_EMPLOYMENT_SINCE)
    private Date since;

    @Column(name = Constants.M_EMPLOYMENT_ACTIVE)
    private boolean active;

    @Override
    public IEmploymentKey getId() {
        return id;
    }

    @Override
    public void setId(IEmploymentKey id) {
        this.id = (EmploymentKey) id;
    }

    @Override
    public Date getSince() {
        return since;
    }

    @Override
    public void setSince(Date since) {
        this.since = since;
    }

    @Override
    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}