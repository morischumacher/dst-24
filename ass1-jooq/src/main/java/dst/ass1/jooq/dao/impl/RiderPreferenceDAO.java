package dst.ass1.jooq.dao.impl;

import dst.ass1.jooq.connection.DataSource;
import dst.ass1.jooq.dao.IRiderPreferenceDAO;
import dst.ass1.jooq.model.IRiderPreference;
import dst.ass1.jooq.model.impl.RiderPreference;
import org.jooq.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dst.ass1.jooq.model.public_.Tables.PREFERENCE;
import static dst.ass1.jooq.model.public_.Tables.RIDER_PREFERENCE;

public class RiderPreferenceDAO implements IRiderPreferenceDAO {

    private DSLContext getContext() {
        try {
            return DataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IRiderPreference findById(Long id) {
        DSLContext context = getContext();
        Record rp = context.select()
                .from(RIDER_PREFERENCE)
                .where(RIDER_PREFERENCE.RIDER_ID.eq(id))
                .fetchOne();
        if (rp == null) return null;
        Result<Record> p = context.select()
                .from(PREFERENCE)
                .where(PREFERENCE.RIDER_ID.eq(id))
                .fetch();
        return mapToRiderPreference(rp, p);
    }

    @Override
    public List<IRiderPreference> findAll() {
        DSLContext context = getContext();
        Result<Record> records = context.select()
                .from(RIDER_PREFERENCE.join(PREFERENCE).on(PREFERENCE.RIDER_ID.eq(RIDER_PREFERENCE.RIDER_ID)))
                .fetch();
        Map<Long, RiderPreference> riderPreferences = new HashMap<>();
        records.forEach(record -> mapRecordToRiderPreference(record, riderPreferences));
        return new ArrayList<>(riderPreferences.values());
    }

    @Override
    public IRiderPreference insert(IRiderPreference model) {
        getContext().transaction((Configuration trx) -> {
            insertRiderPreference(trx, model);
            insertPreferences(trx, model);
        });
        return model;
    }

    @Override
    public void delete(Long id) {
        getContext().transaction((Configuration trx) -> {
            trx.dsl().deleteFrom(PREFERENCE).where(PREFERENCE.RIDER_ID.eq(id)).execute();
            trx.dsl().deleteFrom(RIDER_PREFERENCE).where(RIDER_PREFERENCE.RIDER_ID.eq(id)).execute();
        });
    }

    @Override
    public void updatePreferences(IRiderPreference model) {
        DSLContext context = getContext();
        context.transaction((Configuration trx) -> {
            updateRiderPreference(trx, model);
            updateOrInsertPreferences(trx, model);
        });
    }

    private IRiderPreference mapToRiderPreference(Record rp, Result<Record> p) {
        return new RiderPreference(
                rp.get(RIDER_PREFERENCE.RIDER_ID),
                rp.get(RIDER_PREFERENCE.VEHICLE_CLASS),
                rp.get(RIDER_PREFERENCE.AREA),
                p.stream().collect(Collectors.toMap(record -> record.get(PREFERENCE.PREF_KEY), record -> record.get(PREFERENCE.PREF_VALUE)))
        );
    }

    private void mapRecordToRiderPreference(Record record, Map<Long, RiderPreference> riderPreferences) {
        Long id = record.get(RIDER_PREFERENCE.RIDER_ID);
        riderPreferences.computeIfAbsent(id, k -> new RiderPreference(
                id,
                record.get(RIDER_PREFERENCE.VEHICLE_CLASS),
                record.get(RIDER_PREFERENCE.AREA),
                new HashMap<>()));
        riderPreferences.get(id).getPreferences().put(
                record.get(PREFERENCE.PREF_KEY),
                record.get(PREFERENCE.PREF_VALUE)
        );
    }

    private void insertRiderPreference(Configuration trx, IRiderPreference model) {
        trx.dsl().insertInto(RIDER_PREFERENCE)
                .set(RIDER_PREFERENCE.RIDER_ID, model.getRiderId())
                .set(RIDER_PREFERENCE.VEHICLE_CLASS, model.getVehicleClass())
                .set(RIDER_PREFERENCE.AREA, model.getArea())
                .execute();
    }

    private void insertPreferences(Configuration trx, IRiderPreference model) {
        var query = trx.dsl().batch(
                trx.dsl().insertInto(PREFERENCE, PREFERENCE.RIDER_ID, PREFERENCE.PREF_KEY, PREFERENCE.PREF_VALUE)
                        .values((Long) null, null, null)
        );
        model.getPreferences().forEach((key, value) -> query.bind(model.getRiderId(), key, value));
        query.execute();
    }

    private void updateRiderPreference(Configuration trx, IRiderPreference model) {
        trx.dsl()
                .update(RIDER_PREFERENCE)
                .set(RIDER_PREFERENCE.AREA, model.getArea())
                .set(RIDER_PREFERENCE.VEHICLE_CLASS, model.getVehicleClass())
                .where(RIDER_PREFERENCE.RIDER_ID.eq(model.getRiderId()))
                .execute();
    }

    private void updateOrInsertPreferences(Configuration trx, IRiderPreference model) {
        // TODO: 08.04.24
    }
}
