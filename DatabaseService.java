package com.fleurairlines.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.fleurairlines.model.Admin;
import com.fleurairlines.model.Aircraft;
import com.fleurairlines.model.Airport;
import com.fleurairlines.model.Flight;
import com.fleurairlines.model.Passenger;
import com.fleurairlines.model.Reservation;
import com.fleurairlines.model.Seat;
import com.fleurairlines.util.DatabaseException;
import com.fleurairlines.util.InputSanitizer;
import com.fleurairlines.util.PasswordUtil;

public class DatabaseService {
    private static DatabaseService instance;
    private Connection connection;
    private static final String DB_FILE_NAME = "fleur_airlines.db";
    private static final String DB_URL = resolveDatabaseUrl();
    private static final String BRUTE_FORCE_LIMIT = "5";
    private static final long LOCKOUT_DURATION_MINUTES = 5;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DatabaseService() throws DatabaseException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
            initializeDatabase();
        } catch (ClassNotFoundException | SQLException e) {
            throw new DatabaseException("Failed to initialize database connection", e);
        }
    }

    public static synchronized DatabaseService getInstance() throws DatabaseException {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    private static String resolveDatabaseUrl() {
        Path packagedDatabase = resolvePackagedDatabasePath();
        if (packagedDatabase != null) {
            return "jdbc:sqlite:" + packagedDatabase.toAbsolutePath();
        }
        return "jdbc:sqlite:" + DB_FILE_NAME;
    }

    private static Path resolvePackagedDatabasePath() {
        try {
            Path codePath = Paths.get(DatabaseService.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            Path basePath = Files.isRegularFile(codePath) ? codePath.getParent() : codePath;
            Path databasePath = basePath.resolve(DB_FILE_NAME);
            return Files.exists(databasePath) ? databasePath : null;
        } catch ( Exception ignored) {
            return null;
        }
    }

    private void initializeDatabase() throws DatabaseException {
        try {
            // Wipes corrupted flight seats (e.g. F001) to force a clean 1A, 1B regeneration
            try (Statement s = connection.createStatement()) {
                ResultSet rs = s.executeQuery("SELECT seat_number FROM seats LIMIT 1");
                if (rs.next()) {
                    String seatNum = rs.getString(1);
                    if (seatNum.contains("001")) {
                        s.execute("DROP TABLE IF EXISTS reservations");
                        s.execute("DROP TABLE IF EXISTS seats");
                        s.execute("DROP TABLE IF EXISTS flights");
                    }
                }
            } catch (SQLException ignored) {
                // Ignore schema inspection failures during initialization.
            }

            createTables();
            migrateDatabaseSchema1();
            
            // Ensure Admin@123 always works
            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hash("Admin@123", salt);
            try (Statement s = connection.createStatement()) {
                s.executeUpdate("UPDATE admins SET password_hash = '" + hash + "', password_salt = '" + salt + "' WHERE email = 'admin@fleur.com'");
            }
            
            seedDefaultData();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to initialize database", e);
        }
    }

    private void createTables() throws SQLException {
        String[] createTableStatements = {
            "CREATE TABLE IF NOT EXISTS passengers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "passenger_code TEXT UNIQUE NOT NULL," +
                "full_name TEXT NOT NULL," +
                "email TEXT UNIQUE NOT NULL," +
                "phone TEXT NOT NULL," +
                "passport_number TEXT UNIQUE NOT NULL," +
                "nationality TEXT NOT NULL," +
                "password_hash TEXT NOT NULL," +
                "password_salt TEXT NOT NULL," +
                "loyalty_points INTEGER DEFAULT 0," +
                "date_of_birth TEXT," +
                "preferred_currency TEXT DEFAULT 'USD'," +
                "is_active INTEGER DEFAULT 1," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL" +
            ")",
            "CREATE TABLE IF NOT EXISTS admins (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "admin_code TEXT UNIQUE NOT NULL," +
                "full_name TEXT NOT NULL," +
                "email TEXT UNIQUE NOT NULL," +
                "phone TEXT NOT NULL," +
                "password_hash TEXT NOT NULL," +
                "password_salt TEXT NOT NULL," +
                "department TEXT NOT NULL," +
                "permission_level INTEGER DEFAULT 1," +
                "is_active INTEGER DEFAULT 1," +
                "last_login TEXT," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL" +
            ")",
            "CREATE TABLE IF NOT EXISTS flights (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "flight_number TEXT UNIQUE NOT NULL," +
                "origin TEXT NOT NULL," +
                "destination TEXT NOT NULL," +
                "departure_time TEXT NOT NULL," +
                "arrival_time TEXT NOT NULL," +
                "aircraft_id TEXT NOT NULL," +
                "status TEXT DEFAULT 'SCHEDULED'," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL" +
            ")",
            "CREATE TABLE IF NOT EXISTS seats (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "flight_id INTEGER NOT NULL," +
                "seat_number TEXT NOT NULL," +
                "seat_class TEXT NOT NULL," +
                "price REAL NOT NULL," +
                "is_available INTEGER DEFAULT 1," +
                "UNIQUE(flight_id, seat_number)," +
                "FOREIGN KEY(flight_id) REFERENCES flights(id)" +
            ")",
            "CREATE TABLE IF NOT EXISTS reservations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "booking_code TEXT UNIQUE NOT NULL," +
                "passenger_id INTEGER NOT NULL," +
                "flight_id INTEGER NOT NULL," +
                "seat_id INTEGER NOT NULL," +
                "seat_class TEXT NOT NULL," +
                "total_price REAL NOT NULL," +
                "status TEXT DEFAULT 'CONFIRMED'," +
                "booked_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL," +
                "FOREIGN KEY(passenger_id) REFERENCES passengers(id)," +
                "FOREIGN KEY(flight_id) REFERENCES flights(id)," +
                "FOREIGN KEY(seat_id) REFERENCES seats(id)" +
            ")",
            "CREATE TABLE IF NOT EXISTS login_audit (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_code TEXT NOT NULL," +
                "user_role TEXT NOT NULL," +
                "attempt_type TEXT NOT NULL," +
                "attempted_at TEXT NOT NULL" +
            ")"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : createTableStatements) {
                stmt.execute(sql);
            }
        }
    }

    private void migrateDatabaseSchema1() throws SQLException {
        if (!columnExists("passengers", "date_of_birth")) {
            connection.createStatement().execute("ALTER TABLE passengers ADD COLUMN date_of_birth TEXT");
        }
        if (!columnExists("passengers", "preferred_currency")) {
            connection.createStatement().execute("ALTER TABLE passengers ADD COLUMN preferred_currency TEXT DEFAULT 'USD'");
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private void seedDefaultData() throws SQLException {
        try {
            String checkAdminSql = "SELECT COUNT(*) FROM admins WHERE email = 'admin@fleur.com'";
            try (PreparedStatement pstmt = connection.prepareStatement(checkAdminSql); ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    createDefaultAdmin();
                }
            }
            
            // Check if flights exist. If less than 100, generate the massive daily network.
            String checkFlights = "SELECT COUNT(*) FROM flights";
            try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(checkFlights)) {
                if (rs.next() && rs.getInt(1) < 100) {
                    createSampleFlights();
                }
            }
        } catch (DatabaseException e) {
            throw new SQLException("Failed to seed default data", e);
        }
    }

    private void createDefaultAdmin() throws DatabaseException, SQLException {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hash("Admin@123", salt);
        String now  = LocalDateTime.now().format(DATE_FORMATTER);

        String sql = "INSERT INTO admins (admin_code, full_name, email, phone, password_hash, password_salt, department, permission_level, is_active, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "A001");
            pstmt.setString(2, "Fleur Admin");
            pstmt.setString(3, "admin@fleur.com");
            pstmt.setString(4, "+1234567890");
            pstmt.setString(5, hash);
            pstmt.setString(6, salt);
            pstmt.setString(7, "Operations");
            pstmt.setInt(8, 2);
            pstmt.setInt(9, 1);
            pstmt.setString(10, now);
            pstmt.setString(11, now);
            pstmt.executeUpdate();
        }
    }

    private void createSampleFlights() throws DatabaseException, SQLException {
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 9, 30);
        
        Airport[] hubs = {
            new Airport("HBE", "HBE (Alexandria)", "Egypt"),
            new Airport("CAI", "CAI (Cairo)", "Egypt"),
            new Airport("CDG", "CDG (Paris)", "France"),
            new Airport("DXB", "DXB (Dubai)", "UAE"),
            new Airport("LHR", "LHR (London)", "UK"),
            new Airport("JFK", "JFK (New York)", "USA"),
            new Airport("BCN", "BCN (Barcelona)", "Spain"),
            new Airport("FCO", "FCO (Rome)", "Italy") ,
            new Airport ( "ATH" , "ATH (Athens)", "Greece")
        };
        Aircraft a320 = new Aircraft("A320", "Airbus A320", 180);

        String flightSql = "INSERT INTO flights (flight_number, origin, destination, departure_time, arrival_time, aircraft_id, status, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        connection.setAutoCommit(false);
        int flightCounter = 1000;

        try {
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                for (int src = 0; src < hubs.length; src++) {
                    for (int dest = 0; dest < hubs.length; dest++) {
                        if (src == dest) continue;

                        int randomHour = 6 + ((src + dest + date.getDayOfYear()) % 16); 
                        int randomMin = ((src * dest) % 12) * 5;
                        
                        LocalDateTime departure = LocalDateTime.of(date, LocalTime.of(randomHour, randomMin));
                        LocalDateTime arrival = departure.plusHours(2 + (src % 4));

                        Flight flightObj = new Flight("FL" + flightCounter++, hubs[src], hubs[dest], departure, arrival, a320, "SCHEDULED");

                        try (PreparedStatement pstmt = connection.prepareStatement(flightSql, Statement.RETURN_GENERATED_KEYS)) {
                            pstmt.setString(1, flightObj.getFlightNumber());
                            pstmt.setString(2, flightObj.getOrigin().getAirportCode()); 
                            pstmt.setString(3, flightObj.getDestination().getAirportCode());
                            pstmt.setString(4, flightObj.getDepartureTime().format(DATE_FORMATTER));
                            pstmt.setString(5, flightObj.getArrivalTime().format(DATE_FORMATTER));
                            pstmt.setString(6, flightObj.getAircraft().getAircraftId());
                            pstmt.setString(7, flightObj.getStatus());
                            pstmt.setString(8, now);
                            pstmt.setString(9, now);
                            pstmt.executeUpdate();

                            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                                if (rs.next()) {
                                    saveSeatsForFlight(rs.getInt(1), flightObj.getSeats());
                                }
                            }
                        }
                    }
                }
                connection.commit();
            }
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private void saveSeatsForFlight(int flightId, List<Seat> seats) throws DatabaseException {
        try {
            String seatSql = "INSERT OR IGNORE INTO seats (flight_id, seat_number, seat_class, price, is_available) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(seatSql)) {
                for (Seat seat : seats) {
                    pstmt.setInt(1, flightId);
                    pstmt.setString(2, seat.getSeatNumber());
                    pstmt.setString(3, seat.getSeatClass().toString());
                    pstmt.setDouble(4, seat.getPrice());
                    pstmt.setInt(5, seat.isAvailable() ? 1 : 0);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save seats for flight", e);
        }
    }

    private List<Seat> getSeatsForFlight(int flightId) throws DatabaseException {
        List<Seat> seats = new ArrayList<>();
        try {
            String sql = "SELECT id, seat_number, seat_class, price, is_available FROM seats WHERE flight_id = ? ORDER BY seat_number";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, flightId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Seat seat = new Seat();
                        seat.setId(rs.getInt("id"));
                        seat.setSeatNumber(rs.getString("seat_number"));
                        seat.setSeatClass(Seat.SeatClass.valueOf(rs.getString("seat_class")));
                        seat.setPrice(rs.getDouble("price"));
                        seat.setAvailable(rs.getInt("is_available") == 1);
                        seats.add(seat);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve seats for flight", e);
        }
        return seats;
    }

    private LocalDateTime parseFlightDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.replace(' ', 'T').trim());
    }

    public List<Flight> getFlightsByCriteria(String origin, String dest, String date) throws DatabaseException {
        List<Flight> flights = new ArrayList<>();
        String sql = "SELECT id, flight_number, origin, destination, departure_time, arrival_time, aircraft_id, status " +
                     "FROM flights WHERE origin = ? AND destination = ? AND departure_time LIKE ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, origin);
            pstmt.setString(2, dest);
            pstmt.setString(3, date + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Flight flight = new Flight();
                    int flightId = rs.getInt("id");
                    flight.setId(flightId);
                    flight.setFlightNumber(rs.getString("flight_number"));
                    flight.setStatus(rs.getString("status"));

                    Airport oApt = new Airport(rs.getString("origin"), rs.getString("origin") + " Airport", "");
                    Airport dApt = new Airport(rs.getString("destination"), rs.getString("destination") + " Airport", "");
                    flight.setOrigin(oApt);
                    flight.setDestination(dApt);

                    flight.setDepartureTime(LocalDateTime.parse(rs.getString("departure_time").replace(' ', 'T')));
                    flight.setArrivalTime(LocalDateTime.parse(rs.getString("arrival_time").replace(' ', 'T')));

                    Aircraft aircraft = new Aircraft(rs.getString("aircraft_id"), "Airbus A320", 180);
                    flight.setAircraft(aircraft);
                    flight.setSeats(getSeatsForFlight(flightId));
                    flights.add(flight);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed criteria filter query execution.", e);
        }
        return flights;
    }

    public List<Flight> searchFlights(String origin, String dest, String date, int limit) throws DatabaseException {
        List<Flight> flights = new ArrayList<>();
        if (limit <= 0) {
            limit = 50;
        }

        String sql = "SELECT id, flight_number, origin, destination, departure_time, arrival_time, aircraft_id, status " +
                     "FROM flights WHERE origin = ? AND destination = ? AND departure_time LIKE ? ORDER BY departure_time LIMIT ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, origin);
            pstmt.setString(2, dest);
            pstmt.setString(3, date + "%");
            pstmt.setInt(4, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Flight flight = new Flight();
                    int flightId = rs.getInt("id");
                    flight.setId(flightId);
                    flight.setFlightNumber(rs.getString("flight_number"));
                    flight.setStatus(rs.getString("status"));

                    Airport oApt = new Airport(rs.getString("origin"), rs.getString("origin") + " Airport", "");
                    Airport dApt = new Airport(rs.getString("destination"), rs.getString("destination") + " Airport", "");
                    flight.setOrigin(oApt);
                    flight.setDestination(dApt);

                    flight.setDepartureTime(LocalDateTime.parse(rs.getString("departure_time").replace(' ', 'T')));
                    flight.setArrivalTime(LocalDateTime.parse(rs.getString("arrival_time").replace(' ', 'T')));

                    Aircraft aircraft = new Aircraft(rs.getString("aircraft_id"), "Airbus A320", 180);
                    flight.setAircraft(aircraft);
                    flight.setSeats(getSeatsForFlight(flightId));
                    flights.add(flight);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed search query execution.", e);
        }
        return flights;
    }

    public void savePassenger(Passenger passenger) throws DatabaseException {
        try {
            String now                 = LocalDateTime.now().format(DATE_FORMATTER);
            String sanitizedName       = InputSanitizer.sanitizeText(passenger.getName());
            String sanitizedPassport   = InputSanitizer.sanitizeText(passenger.getPassportNumber());
            String sanitizedNationality = InputSanitizer.sanitizeText(passenger.getNationality());

            String checkSql = "SELECT id FROM passengers WHERE email = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, passenger.getEmail());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        String updateSql = "UPDATE passengers SET full_name = ?, phone = ?, passport_number = ?, nationality = ?, loyalty_points = ?, date_of_birth = ?, password_hash = ?, password_salt = ?, updated_at = ? WHERE email = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                            updateStmt.setString(1, sanitizedName);
                            updateStmt.setString(2, passenger.getPhone());
                            updateStmt.setString(3, sanitizedPassport);
                            updateStmt.setString(4, sanitizedNationality);
                            updateStmt.setInt(5, passenger.getLoyaltyPoints());
                            updateStmt.setString(6, passenger.getDateOfBirth());
                            updateStmt.setString(7, passenger.getPasswordHash());
                            updateStmt.setString(8, passenger.getPasswordSalt());
                            updateStmt.setString(9, now);
                            updateStmt.setString(10, passenger.getEmail());
                            updateStmt.executeUpdate();
                        }
                    } else {
                        String passengerCode = generatePassengerCode();
                        String salt          = passenger.getPasswordSalt();
                        String hash          = passenger.getPasswordHash();

                        String insertSql = "INSERT INTO passengers (passenger_code, full_name, email, phone, passport_number, nationality, password_hash, password_salt, loyalty_points, date_of_birth, preferred_currency, is_active, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                            insertStmt.setString(1, passengerCode);
                            insertStmt.setString(2, sanitizedName);
                            insertStmt.setString(3, passenger.getEmail());
                            insertStmt.setString(4, passenger.getPhone());
                            insertStmt.setString(5, sanitizedPassport);
                            insertStmt.setString(6, sanitizedNationality);
                            insertStmt.setString(7, hash);
                            insertStmt.setString(8, salt);
                            insertStmt.setInt(9, 0);
                            insertStmt.setString(10, passenger.getDateOfBirth());
                            insertStmt.setString(11, passenger.getPreferredCurrency());
                            insertStmt.setInt(12, 1);
                            insertStmt.setString(13, now);
                            insertStmt.setString(14, now);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException | DatabaseException e) {
            throw new DatabaseException("Failed to save passenger", e);
        }
    }

    public Passenger getPassengerByEmail(String email) throws DatabaseException {
        try {
            String sql = "SELECT id, passenger_code, full_name, email, phone, passport_number, nationality, password_hash, password_salt, loyalty_points, date_of_birth, preferred_currency " +
                "FROM passengers WHERE email = ? AND is_active = 1";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, email);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Passenger passenger = new Passenger();
                        passenger.setId(String.valueOf(rs.getInt("id")));
                        passenger.setName(rs.getString("full_name"));
                        passenger.setEmail(rs.getString("email"));
                        passenger.setPhone(rs.getString("phone"));
                        passenger.setPassportNumber(rs.getString("passport_number"));
                        passenger.setNationality(rs.getString("nationality"));
                        passenger.setPasswordHash(rs.getString("password_hash"));
                        passenger.setPasswordSalt(rs.getString("password_salt"));
                        passenger.setLoyaltyPoints(rs.getInt("loyalty_points"));
                        passenger.setDateOfBirth(rs.getString("date_of_birth"));
                        passenger.setPreferredCurrency(rs.getString("preferred_currency"));
                        return passenger;
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve passenger by email", e);
        }
        return null;
    }

    public List<Passenger> getAllPassengers() throws DatabaseException {
        List<Passenger> passengers = new ArrayList<>();
        try {
            String sql = "SELECT id, passenger_code, full_name, email, phone, passport_number, nationality, password_hash, password_salt, loyalty_points, date_of_birth, preferred_currency " +
                "FROM passengers WHERE is_active = 1";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Passenger passenger = new Passenger();
                    passenger.setId(String.valueOf(rs.getInt("id")));
                    passenger.setName(rs.getString("full_name"));
                    passenger.setEmail(rs.getString("email"));
                    passenger.setPhone(rs.getString("phone"));
                    passenger.setPassportNumber(rs.getString("passport_number"));
                    passenger.setNationality(rs.getString("nationality"));
                    passenger.setPasswordHash(rs.getString("password_hash"));
                    passenger.setPasswordSalt(rs.getString("password_salt"));
                    passenger.setLoyaltyPoints(rs.getInt("loyalty_points"));
                    passenger.setDateOfBirth(rs.getString("date_of_birth"));
                    passenger.setPreferredCurrency(rs.getString("preferred_currency"));
                    passengers.add(passenger);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve all passengers", e);
        }
        return passengers;
    }

    public void saveAdmin(Admin admin) throws DatabaseException {
        try {
            String now                 = LocalDateTime.now().format(DATE_FORMATTER);
            String sanitizedName       = InputSanitizer.sanitizeText(admin.getName());
            String sanitizedDepartment = InputSanitizer.sanitizeText(admin.getDepartment());

            String checkSql = "SELECT id FROM admins WHERE email = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, admin.getEmail());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        String updateSql = "UPDATE admins SET full_name = ?, phone = ?, department = ?, permission_level = ?, updated_at = ? WHERE email = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                            updateStmt.setString(1, sanitizedName);
                            updateStmt.setString(2, admin.getPhone());
                            updateStmt.setString(3, sanitizedDepartment);
                            updateStmt.setInt(4, admin.getPermissionLevel());
                            updateStmt.setString(5, now);
                            updateStmt.setString(6, admin.getEmail());
                            updateStmt.executeUpdate();
                        }
                    } else {
                        String adminCode = generateAdminCode();
                        String salt      = PasswordUtil.generateSalt();
                        String hash      = PasswordUtil.hash(admin.getPasswordHash(), salt);

                        String insertSql = "INSERT INTO admins (admin_code, full_name, email, phone, password_hash, password_salt, department, permission_level, is_active, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                            insertStmt.setString(1, adminCode);
                            insertStmt.setString(2, sanitizedName);
                            insertStmt.setString(3, admin.getEmail());
                            insertStmt.setString(4, admin.getPhone());
                            insertStmt.setString(5, hash);
                            insertStmt.setString(6, salt);
                            insertStmt.setString(7, sanitizedDepartment);
                            insertStmt.setInt(8, admin.getPermissionLevel());
                            insertStmt.setInt(9, 1);
                            insertStmt.setString(10, now);
                            insertStmt.setString(11, now);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException | DatabaseException e) {
            throw new DatabaseException("Failed to save admin", e);
        }
    }

    public Admin getAdminByEmail(String email) throws DatabaseException {
        try {
            String sql = "SELECT id, admin_code, full_name, email, phone, password_hash, password_salt, department, permission_level, last_login " +
                "FROM admins WHERE email = ? AND is_active = 1";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, email);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Admin admin = new Admin();
                        admin.setId(String.valueOf(rs.getInt("id")));
                        admin.setName(rs.getString("full_name"));
                        admin.setEmail(rs.getString("email"));
                        admin.setPhone(rs.getString("phone"));
                        admin.setPasswordHash(rs.getString("password_hash"));
                        admin.setPasswordSalt(rs.getString("password_salt"));
                        admin.setDepartment(rs.getString("department"));
                        admin.setPermissionLevel(rs.getInt("permission_level"));
                        return admin;
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve admin by email", e);
        }
        return null;
    }

    public void saveFlight(Flight flight) throws DatabaseException {
        try {
            String now                  = LocalDateTime.now().format(DATE_FORMATTER);
            String sanitizedOrigin      = InputSanitizer.sanitizeText(flight.getOrigin().getAirportCode());
            String sanitizedDestination = InputSanitizer.sanitizeText(flight.getDestination().getAirportCode());
            String sanitizedAircraftId  = InputSanitizer.sanitizeText(flight.getAircraft().getAircraftId());

            String checkSql = "SELECT id FROM flights WHERE flight_number = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, flight.getFlightNumber());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        int flightId  = rs.getInt("id");
                        String updateSql = "UPDATE flights SET origin = ?, destination = ?, departure_time = ?, arrival_time = ?, aircraft_id = ?, status = ?, updated_at = ? WHERE id = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                            updateStmt.setString(1, sanitizedOrigin);
                            updateStmt.setString(2, sanitizedDestination);
                            updateStmt.setString(3, flight.getDepartureTime().format(DATE_FORMATTER));
                            updateStmt.setString(4, flight.getArrivalTime().format(DATE_FORMATTER));
                            updateStmt.setString(5, sanitizedAircraftId);
                            updateStmt.setString(6, flight.getStatus());
                            updateStmt.setString(7, now);
                            updateStmt.setInt(8, flightId);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        String insertSql = "INSERT INTO flights (flight_number, origin, destination, departure_time, arrival_time, aircraft_id, status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                            insertStmt.setString(1, flight.getFlightNumber());
                            insertStmt.setString(2, sanitizedOrigin);
                            insertStmt.setString(3, sanitizedDestination);
                            insertStmt.setString(4, flight.getDepartureTime().format(DATE_FORMATTER));
                            insertStmt.setString(5, flight.getArrivalTime().format(DATE_FORMATTER));
                            insertStmt.setString(6, sanitizedAircraftId);
                            insertStmt.setString(7, flight.getStatus());
                            insertStmt.setString(8, now);
                            insertStmt.setString(9, now);
                            insertStmt.executeUpdate();

                            try (ResultSet rs2 = insertStmt.getGeneratedKeys()) {
                                if (rs2.next()) {
                                    saveSeatsForFlight(rs2.getInt(1), flight.getSeats());
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save flight", e);
        }
    }

    public Flight getFlightByNumber(String flightNumber) throws DatabaseException {
        try {
            String sql = "SELECT id, flight_number, origin, destination, departure_time, arrival_time, aircraft_id, status FROM flights WHERE flight_number = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, flightNumber);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Flight flight = new Flight();
                        int flightId  = rs.getInt("id");
                        flight.setId(flightId);
                        flight.setFlightNumber(rs.getString("flight_number"));
                        flight.setStatus(rs.getString("status"));

                        Airport origin = new Airport();
                        origin.setAirportCode(rs.getString("origin"));
                        origin.setName(rs.getString("origin") + " Airport");
                        flight.setOrigin(origin);

                        Airport destination = new Airport();
                        destination.setAirportCode(rs.getString("destination"));
                        destination.setName(rs.getString("destination") + " Airport");
                        flight.setDestination(destination);

                        String departure = rs.getString("departure_time");
                        if (departure != null && !departure.isBlank()) {
                            flight.setDepartureTime(parseFlightDateTime(departure));
                        }
                        String arrival = rs.getString("arrival_time");
                        if (arrival != null && !arrival.isBlank()) {
                            flight.setArrivalTime(parseFlightDateTime(arrival));
                        }

                        Aircraft aircraft = new Aircraft();
                        aircraft.setAircraftId(rs.getString("aircraft_id"));
                        flight.setAircraft(aircraft);

                        flight.setSeats(getSeatsForFlight(flightId));
                        return flight;
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve flight by number", e);
        }
        return null;
    }

    public List<Flight> getAllFlights() throws DatabaseException {
        List<Flight> flights = new ArrayList<>();
        try {
            String sql = "SELECT id, flight_number, origin, destination, departure_time, arrival_time, aircraft_id, status FROM flights ORDER BY departure_time";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Flight flight = new Flight();
                    int flightId  = rs.getInt("id");
                    flight.setId(flightId);
                    flight.setFlightNumber(rs.getString("flight_number"));
                    flight.setStatus(rs.getString("status"));

                    Airport origin = new Airport();
                    origin.setAirportCode(rs.getString("origin"));
                    origin.setName(rs.getString("origin") + " Airport");
                    flight.setOrigin(origin);

                    Airport destination = new Airport();
                    destination.setAirportCode(rs.getString("destination"));
                    destination.setName(rs.getString("destination") + " Airport");
                    flight.setDestination(destination);

                    String departure = rs.getString("departure_time");
                    if (departure != null && !departure.isBlank()) {
                        flight.setDepartureTime(parseFlightDateTime(departure));
                    }
                    String arrival = rs.getString("arrival_time");
                    if (arrival != null && !arrival.isBlank()) {
                        flight.setArrivalTime(parseFlightDateTime(arrival));
                    }

                    Aircraft aircraft = new Aircraft();
                    aircraft.setAircraftId(rs.getString("aircraft_id"));
                    flight.setAircraft(aircraft);

                    flight.setSeats(getSeatsForFlight(flightId));
                    flights.add(flight);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve all flights", e);
        }
        return flights;
    }

    public void saveReservation(Reservation reservation) throws DatabaseException {
        try {
            String now         = LocalDateTime.now().format(DATE_FORMATTER);
            String bookingCode = generateBookingCode();

            String sql = "INSERT INTO reservations (booking_code, passenger_id, flight_id, seat_id, seat_class, total_price, status, booked_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, bookingCode);
                pstmt.setInt(2, reservation.getPassengerId());
                pstmt.setInt(3, reservation.getFlightId());
                pstmt.setInt(4, reservation.getSeatId());
                pstmt.setString(5, reservation.getSeatClass().toString());
                pstmt.setDouble(6, reservation.getTotalPrice());
                pstmt.setString(7, reservation.getStatus().toString());
                pstmt.setString(8, now);
                pstmt.setString(9, now);
                pstmt.executeUpdate();
            }

            String updateSeatSql = "UPDATE seats SET is_available = 0 WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateSeatSql)) {
                pstmt.setInt(1, reservation.getSeatId());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save reservation", e);
        }
    }

    public Reservation getReservationByBookingCode(String bookingCode) throws DatabaseException {
        if (bookingCode == null || bookingCode.isBlank()) {
            throw new DatabaseException("Booking code cannot be empty.");
        }
        try {
            String sql = "SELECT booking_code, passenger_id, flight_id, seat_id, seat_class, total_price, status FROM reservations WHERE booking_code = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, bookingCode);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Reservation reservation = new Reservation();
                        reservation.setBookingCode(rs.getString("booking_code"));
                        reservation.setPassengerId(rs.getInt("passenger_id"));
                        reservation.setFlightId(rs.getInt("flight_id"));
                        reservation.setSeatId(rs.getInt("seat_id"));
                        reservation.setSeatClass(Seat.SeatClass.valueOf(rs.getString("seat_class")));
                        reservation.setTotalPrice(rs.getDouble("total_price"));
                        reservation.setStatus(Reservation.ReservationStatus.valueOf(rs.getString("status")));
                        return reservation;
                    }
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve reservation", e);
        }
    }

    public void cancelReservation(String bookingCode) throws DatabaseException {
        try {
            String now    = LocalDateTime.now().format(DATE_FORMATTER);
            int    seatId = 0;

            String getSeatSql = "SELECT seat_id FROM reservations WHERE booking_code = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(getSeatSql)) {
                pstmt.setString(1, bookingCode);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) seatId = rs.getInt("seat_id");
                }
            }

            String cancelSql = "UPDATE reservations SET status = ?, updated_at = ? WHERE booking_code = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(cancelSql)) {
                pstmt.setString(1, "CANCELLED");
                pstmt.setString(2, now);
                pstmt.setString(3, bookingCode);
                pstmt.executeUpdate();
            }

            if (seatId > 0) {
                String freeSeatSql = "UPDATE seats SET is_available = 1 WHERE id = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(freeSeatSql)) {
                    pstmt.setInt(1, seatId);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to cancel reservation", e);
        }
    }

    public List<Reservation> getReservationsByPassenger(String passengerId) throws DatabaseException {
        List<Reservation> reservations = new ArrayList<>();
        try {
            String sql = "SELECT id, booking_code, passenger_id, flight_id, seat_id, seat_class, total_price, status FROM reservations WHERE passenger_id = ? AND status != 'CANCELLED'";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, passengerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Reservation reservation = new Reservation();
                        reservation.setBookingCode(rs.getString("booking_code"));
                        reservation.setPassengerId(rs.getInt("passenger_id"));
                        reservation.setFlightId(rs.getInt("flight_id"));
                        reservation.setSeatId(rs.getInt("seat_id"));
                        reservation.setSeatClass(Seat.SeatClass.valueOf(rs.getString("seat_class")));
                        reservation.setTotalPrice(rs.getDouble("total_price"));
                        reservation.setStatus(Reservation.ReservationStatus.valueOf(rs.getString("status")));
                        reservations.add(reservation);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve reservations by passenger", e);
        }
        return reservations;
    }

    public List<Reservation> getAllReservations() throws DatabaseException {
        List<Reservation> reservations = new ArrayList<>();
        try {
            String sql = "SELECT id, booking_code, passenger_id, flight_id, seat_id, seat_class, total_price, status FROM reservations WHERE status != 'CANCELLED'";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Reservation reservation = new Reservation();
                    reservation.setBookingCode(rs.getString("booking_code"));
                    reservation.setPassengerId(rs.getInt("passenger_id"));
                    reservation.setFlightId(rs.getInt("flight_id"));
                    reservation.setSeatId(rs.getInt("seat_id"));
                    reservation.setSeatClass(Seat.SeatClass.valueOf(rs.getString("seat_class")));
                    reservation.setTotalPrice(rs.getDouble("total_price"));
                    reservation.setStatus(Reservation.ReservationStatus.valueOf(rs.getString("status")));
                    reservations.add(reservation);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to retrieve all reservations", e);
        }
        return reservations;
    }

    public void logLoginAttempt(String userCode, String role, String attemptType) throws DatabaseException {
        try {
            String now = LocalDateTime.now().format(DATE_FORMATTER);
            String sql = "INSERT INTO login_audit (user_code, user_role, attempt_type, attempted_at) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userCode);
                pstmt.setString(2, role);
                pstmt.setString(3, attemptType);
                pstmt.setString(4, now);
                pstmt.executeUpdate();
            }
            checkBruteForce(userCode);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to log login attempt", e);
        }
    }

    private void checkBruteForce(String userCode) throws DatabaseException {
        try {
            String timeThreshold = LocalDateTime.now().minusMinutes(LOCKOUT_DURATION_MINUTES).format(DATE_FORMATTER);
            String sql = "SELECT COUNT(*) as failed_count FROM login_audit WHERE user_code = ? AND attempt_type = 'FAILED' AND attempted_at > ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userCode);
                pstmt.setString(2, timeThreshold);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next() && rs.getInt("failed_count") >= Integer.parseInt(BRUTE_FORCE_LIMIT)) {
                        throw new DatabaseException("Account locked due to too many failed login attempts");
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check brute force attempts", e);
        }
    }

    private String generatePassengerCode() throws DatabaseException {
        try {
            String sql = "SELECT COUNT(*) as count FROM passengers";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) return String.format("P%06d", rs.getInt("count") + 1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to generate passenger code", e);
        }
        return "P000001";
    }

    private String generateAdminCode() throws DatabaseException {
        try {
            String sql = "SELECT COUNT(*) as count FROM admins";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) return String.format("A%03d", rs.getInt("count") + 1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to generate admin code", e);
        }
        return "A001";
    }

    private String generateBookingCode() throws DatabaseException {
        try {
            String sql = "SELECT COUNT(*) as count FROM reservations";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) return String.format("BK%08d", rs.getInt("count") + 1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to generate booking code", e);
        }
        return "BK00000001";
    }

    public void closeConnection() throws DatabaseException {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to close database connection", e);
        }
    }

    public int getFailedLoginCount(String email) throws DatabaseException {
        try {
            String timeThreshold = LocalDateTime.now().minusMinutes(LOCKOUT_DURATION_MINUTES).format(DATE_FORMATTER);
            String sql = "SELECT COUNT(*) as failed_count FROM login_audit WHERE user_code = ? AND attempt_type = 'FAILED' AND attempted_at > ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, email);
                pstmt.setString(2, timeThreshold);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) return rs.getInt("failed_count");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get failed login count", e);
        }
        return 0;
    }
}
