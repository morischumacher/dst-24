package dst.ass1.jpa.dao.impl;

import dst.ass1.jpa.dao.ITripReceiptDAO;
import dst.ass1.jpa.model.PaymentMethod;
import dst.ass1.jpa.model.impl.Money;
import dst.ass1.jpa.model.impl.PaymentInfo;
import dst.ass1.jpa.model.impl.TripInfo;
import dst.ass1.jpa.model.impl.TripReceipt;
import dst.ass1.jpa.util.TupleResult;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.*;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TripReceiptDAO implements ITripReceiptDAO {

    private EntityManager entityManager;

    public TripReceiptDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    //JPA Criteria API Queries
    @Override
    public List<TupleResult<PaymentMethod, Double>> calculateAverageTipPerPaymentMethod(Date startDate, Date endDate) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();
        Root<TripReceipt> tripReceiptRoot = criteriaQuery.from(TripReceipt.class);
        Join<TripReceipt, Money> tipJoin = tripReceiptRoot.join("tip");
        Join<TripReceipt, Money> totalJoin = tripReceiptRoot.join("total");
        Join<TripReceipt, TripInfo> tripInfoJoin = tripReceiptRoot.join("tripInfo");
        Join<TripReceipt, PaymentInfo> paymentInfoJoin = tripReceiptRoot.join("paymentInfo");

        Expression<PaymentMethod> paymentMethodExpression = paymentInfoJoin.get("method");
        Expression<Double> tipAverageExpression = criteriaBuilder.avg(criteriaBuilder.quot(tipJoin.get("currencyValue"), totalJoin.get("currencyValue")));

        Predicate datePredicate = criteriaBuilder.conjunction();

        if (startDate != null && endDate != null) {
            datePredicate = criteriaBuilder.between(tripInfoJoin.get("completed"), startDate, endDate);
        } else if (startDate != null) {
            datePredicate = criteriaBuilder.greaterThan(tripInfoJoin.get("completed"), startDate);
        } else if (endDate != null) {
            datePredicate = criteriaBuilder.lessThan(tripInfoJoin.get("completed"), endDate);
        }

        criteriaQuery.multiselect(paymentMethodExpression, tipAverageExpression)
                .where(datePredicate)
                .groupBy(paymentInfoJoin.get("method"))
                .orderBy(criteriaBuilder.desc(tipAverageExpression));

        List<Tuple> queryResults = entityManager.createQuery(criteriaQuery).getResultList();

        return queryResults.stream()
                .map(tuple -> new TupleResult<>(tuple.get(paymentMethodExpression), tuple.get(tipAverageExpression) * 100))
                .collect(Collectors.toList());
    }
}
