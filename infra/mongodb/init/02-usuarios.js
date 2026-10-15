db.createUser({
  user: process.env.MONGO_LOGICA_USER,
  pwd: process.env.MONGO_LOGICA_PASSWORD,
  roles: [{ role: "read", db: process.env.MONGO_INITDB_DATABASE }]
});

db.createUser({
  user: process.env.MONGO_INGESTA_USER,
  pwd: process.env.MONGO_INGESTA_PASSWORD,
  roles: [{ role: "readWrite", db: process.env.MONGO_INITDB_DATABASE }]
});
