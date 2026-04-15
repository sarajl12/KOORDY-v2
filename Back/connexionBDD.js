import pkg from 'pg';
const { Pool } = pkg;

let connection;

async function initDB() {
  connection = new Pool({
    host: 'localhost',
    user: 'postgres',
    password: 'aurelie',  // ← change ici
    database: 'koordybdd',
    port: 5432,
  });
  console.log('Connexion PostgreSQL établie');
  return connection;
}

export { initDB };