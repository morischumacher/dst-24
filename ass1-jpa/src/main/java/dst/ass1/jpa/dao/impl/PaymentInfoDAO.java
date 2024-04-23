package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.*;
import dst.ass1.jpa.model.*;
import dst.ass1.jpa.model.impl.PaymentInfo;
import dst.ass1.jpa.util.*;

import javax.persistence.EntityManager;

public class PaymentInfoDAO extends AbstractDAO<IPaymentInfo, PaymentInfo> implements IPaymentInfoDAO {
    public PaymentInfoDAO(EntityManager em) {super(em, PaymentInfo.class, Constants.T_PAYMENT_INFO);}
}
