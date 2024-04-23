package dst.ass1.doc.impl;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import dst.ass1.doc.IDocumentQuery;
import dst.ass1.jpa.util.Constants;
import org.bson.Document;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Accumulators.avg;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Projections.*;

public class DocumentQuery implements IDocumentQuery {
    private MongoDatabase db;
    private static final Logger LOGGER = Logger.getLogger(DocumentQuery.class.getName());

    public DocumentQuery(MongoDatabase db) {this.db = db;}

    @Override
    public List<Document> getAverageOpeningHoursPerCategory() {
        List<Document> result = db.getCollection(Constants.COLL_LOCATION_DATA)
                .aggregate(
                        Arrays.asList(
                                match(Filters.exists("category")),
                                project(
                                        fields(
                                                include("category", "closingHour", "openHour"),
                                                computed("hours", new Document("$subtract", Arrays.asList("$closingHour", "$openHour")))
                                        )
                                ),
                                group("$category", avg("value", "$hours"))
                        )
                )
                .into(new ArrayList<>());

        // Log the result
        LOGGER.info("getAverageOpeningHoursPerCategory returned " + result.size() + " documents.");

        result.forEach(doc -> System.out.println(doc.toJson()));

        return result;

    }

    @Override
    public List<Document> findDocumentsByNameWithinPolygon(String name, List<List<Double>> polygon) {
        List<Document> result = this.db.getCollection(Constants.COLL_LOCATION_DATA)
                .find(Filters.and(
                        Filters.regex("name", ".*" + Pattern.quote(name) + ".*"),
                        Filters.geoWithinPolygon("geo", polygon)
                ))
                .projection(Projections.fields(Projections.include("location_id"), Projections.exclude("_id")))
                .into(new LinkedList<Document>());

        LOGGER.info("findDocumentsByNameWithinPolygon returned " + result.size() + " documents.");


        return result;
    }

    @Override
    public List<Document> findDocumentsByType(String type) {
        List<Document> documents = db.getCollection(Constants.COLL_LOCATION_DATA)
                .find(new Document("type", type))
                .into(new ArrayList<>());

        // Log the result
        LOGGER.info("findDocumentsByType returned " + documents.size() + " documents.");

        return documents;
    }

}
