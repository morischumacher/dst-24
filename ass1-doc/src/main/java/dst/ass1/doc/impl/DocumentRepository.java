package dst.ass1.doc.impl;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import dst.ass1.doc.IDocumentRepository;
import dst.ass1.jpa.model.ILocation;
import dst.ass1.jpa.util.Constants;
import org.bson.Document;

import java.util.Map;

public class DocumentRepository implements IDocumentRepository {
    private com.mongodb.client.MongoClient mongoClient;
    private final MongoDatabase db;
    {
    mongoClient = MongoClients.create();
    db = mongoClient.getDatabase(Constants.MONGO_DB_NAME);
    }
    @Override
    public void insert(ILocation location, Map<String, Object> locationProperties) {
        var coll = db.getCollection(Constants.COLL_LOCATION_DATA);
        var doc = new Document(locationProperties);
        doc.put("location_id", location.getLocationId());
        doc.put("name", location.getName());
        coll.insertOne(doc);
        coll.createIndex(Indexes.ascending("location_id"));
        coll.createIndex(Indexes.geo2dsphere("geo"));
    }
    public void close() {
        this.mongoClient.close();
    }
}
