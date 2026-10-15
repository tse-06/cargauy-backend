db.createCollection("eventos_tracking", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["matricula", "timestamp"],
      properties: {
        matricula: { bsonType: "string" },
        timestamp: { bsonType: "date" }
      }
    }
  }
});

db.eventos_tracking.createIndex({ matricula: 1, timestamp: 1 });
