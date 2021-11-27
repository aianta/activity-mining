package com.activity.mining.persistence;

import cc.kave.commons.model.events.IDEEvent;


import cc.kave.commons.utils.io.json.JsonUtils;
import com.activity.mining.records.FrequentSubSequence;
import com.activity.mining.records.MiningRecord;
import com.activity.mining.records.Sequence;
import com.activity.mining.mappers.EventToActivityMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import java.util.function.Consumer;


public class DataStore {

    private static final Logger log = LoggerFactory.getLogger(DataStore.class);

    private static DataStore instance;

    private static final String EVENTS_TABLE = "CREATE TABLE IF NOT EXISTS EVENTS ( " +
            "EventId TEXT PRIMARY KEY,"+
            "IDESessionUUID TEXT NOT NULL," +
            "TriggeredAt NUMERIC NOT NULL," +
            "TimezoneId TEXT NOT NULL," +
            "Activity TEXT," +
            "Json TEXT NOT NULL" +
            ");";

    private static final String SEQUENCE_TABLE = "CREATE TABLE IF NOT EXISTS SEQUENCES (" +
            "SessionId TEXT NOT NULL," +
            "Sequence TEXT NOT NULL," +
            "Sequencer TEXT NOT NULL," +
            "PRIMARY KEY (SessionId, Sequencer));";

    private static final String FREQUENT_SUBSEQUENCE_TABLE = "CREATE TABLE IF NOT EXISTS FREQUENT_SUBSEQUENCES (" +
            "ExecutionId TEXT NOT NULL," +
            "Sequence TEXT NOT NULL," +
            "Frequency INTEGER NOT NULL," +
            "PRIMARY KEY (ExecutionId,Sequence));";

    private static final String MINING_RECORDS_TABLE = "CREATE TABLE IF NOT EXISTS MINING_RECORDS(" +
            "ExecutionId TEXT PRIMARY KEY," +
            "Timestamp TEXT NOT NULL," +
            "StartTimestamp NUMERIC NOT NULL," +
            "EndTimestamp NUMERIC NOT NULL," +
            "Support INTEGER NOT NULL," +
            "Gamma INTEGER NOT NULL," +
            "Lambda INTEGER NOT NULL," +
            "Sequencer TEXT," +
            "Duration INTEGER GENERATED ALWAYS AS (endTimestamp - startTimestamp) STORED);" ;

    private static final String INSERT_EVENT = "INSERT INTO EVENTS (" +
            "EventId, " +
            "IDESessionUUID, " +
            "TriggeredAt, " +
            "TimezoneId, " +
            "Activity, Json) " +
            "VALUES (?,?,?,?,?,?);";

    private static final String INSERT_SEQUENCE = "INSERT INTO SEQUENCES (" +
            "SessionId," +
            "Sequencer," +
            "Sequence) " +
            "VALUES (?,?,?);";

    private static final String INSERT_FREQUENT_SUBSEQUENCE = "INSERT INTO FREQUENT_SUBSEQUENCES (" +
            "ExecutionId, Sequence, Frequency) " +
            "VALUES (?,?,?);";

    private static final String INSERT_MINING_RECORD = "INSERT INTO MINING_RECORDS(" +
            "ExecutionId,Timestamp,StartTimestamp,EndTimestamp,Support,Gamma,Lambda,Sequencer) " +
            "VALUES (?,?,?,?,?,?,?,?);";


    /**
     * SELECT QUERIES
     */
    private static final String SELECT_FREQUENT_SUBSEQUENCES = "SELECT Sequence FROM FREQUENT_SUBSEQUENCES WHERE ExecutionId = ?;";

    private static final String SELECT_DISTINCT_SESSIONS = "SELECT DISTINCT IDESessionUUID FROM " +
            "EVENTS;";

    private static final String SELECT_SESSION_EVENTS = "SELECT Json FROM EVENTS WHERE IDESessionUUID = ? ORDER BY TriggeredAt ASC;";

    private static final String SELECT_SEQUENCES = "SELECT Sequence, SessionId FROM SEQUENCES WHERE Sequencer = ?;";

    private String dbPath;
    private String url;


    private DataStore(String path){
        this.dbPath = path;
        this.url = "jdbc:sqlite:" + dbPath;
        initalize(url);
    }

    private void initalize(String url){
        try(Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement()
        ){
            stmt.execute(EVENTS_TABLE);
            stmt.execute(SEQUENCE_TABLE);
            stmt.execute(MINING_RECORDS_TABLE);
            stmt.execute(FREQUENT_SUBSEQUENCE_TABLE);

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static DataStore getInstance(String dbPath){
        if (instance == null){
            instance = new DataStore(dbPath);
        }
        return instance;
    }

    public static DataStore getInstance(){
        return instance;
    }


    public void insert(Connection connection, Sequence s){
        withPreparedStatement(connection, INSERT_SEQUENCE, pstmt->{
            try{
                pstmt.setString(1, s.sessionId());
                pstmt.setString(2, s.sequencer());
                pstmt.setString(3, s.sequence());
                pstmt.executeUpdate();
            } catch (SQLException throwables) {
                log.error(throwables.getMessage(), throwables);
            }
        });
    }

    public void insert(Sequence s){
        withConnection(conn->{
            insert(conn, s);
        });
    }

    public void insert (Connection connection, IDEEvent event){
        withPreparedStatement(connection, INSERT_EVENT, pstmt->{
            try {
                pstmt.setString(1, UUID.randomUUID().toString());
                pstmt.setString(2, event.IDESessionUUID);
                pstmt.setLong(3, event.getTriggeredAt().toEpochSecond());
                pstmt.setString(4, event.getTriggeredAt().getZone().getId());
                pstmt.setString(5, String.valueOf(EventToActivityMapper.mapEvent(event).orElse(null) == null?null:EventToActivityMapper.mapEvent(event).get().symbol));
                pstmt.setString(6,JsonUtils.toJson(event));

                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void insert(IDEEvent event){
        withConnection(conn->{
            insert(conn, event);
        });
    }

    public void insert(MiningRecord record){
        withPreparedStatement(INSERT_MINING_RECORD, stmt->{

            try{
                stmt.setString(1, record.executionId());
                stmt.setString(2, record.timestamp());
                stmt.setLong(3, record.startTimestamp());
                stmt.setLong(4, record.endTimestamp());
                stmt.setInt(5,record.support());
                stmt.setInt(6, record.gamma());
                stmt.setInt(7, record.lambda());
                stmt.setString(8,record.sequencer());

                stmt.executeUpdate();
            }catch (SQLException e){
                log.error(e.getMessage(),e);
            }

        });
    }

    public void insert(List<FrequentSubSequence> fssList){
        log.info("Saving {} frequent subsequences, please wait...", fssList.size());
        withConnection(connection -> {
            ListIterator<FrequentSubSequence> it = fssList.listIterator();
            while (it.hasNext()){
                log.info("Inserting {}/{} frequent subsequence", it.nextIndex(), fssList.size());
                insert(connection,it.next());
            }
        });
    }

    public void insert(FrequentSubSequence fss){
        withConnection(connection -> insert(connection, fss));
    }

    public void insert(Connection connection,FrequentSubSequence fss){
        withPreparedStatement(connection,INSERT_FREQUENT_SUBSEQUENCE, stmt->{
            try{
                stmt.setString(1, fss.executionId());
                stmt.setString(2, fss.sequence());
                stmt.setInt(3, fss.frequency());

                stmt.executeUpdate();
            }catch (SQLException e){
                log.error(e.getMessage(), e);
            }
        });
    }

    /**
     * Returns the IDEEvents from a session in chronological order.
     * @param sessionUUID
     * @return
     */
    public List<IDEEvent> getSessionEvents(String sessionUUID){
        List<IDEEvent> events = new ArrayList<>();
        withPreparedStatement(SELECT_SESSION_EVENTS, pstmt->{
            try{
                pstmt.setString(1,sessionUUID);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()){
                    events.add(JsonUtils.fromJson(rs.getString("Json"), IDEEvent.class));
                }
            }catch (SQLException e){
                log.error(e.getMessage(),e);
            }
        });
        return events;
    }

    public List<String> getUniqueSessions(){
        List<String> result = new ArrayList<>();
        withPreparedStatement(SELECT_DISTINCT_SESSIONS, stmt->{
            try {
                ResultSet rs = stmt.executeQuery();

                while (rs.next()){
                    result.add(rs.getString("IDESessionUUID"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return result;
    }

    /** Returns sequences for all sessions for a given sequencer.
     *
     * @param sequencer Class of the sequencer used to produce the sequences.
     * @return The sequences for all sessions produced by the given sequencer.
     */
    public List<Sequence> getSequences(Class sequencer){
        List<Sequence> result = new ArrayList<>();
        withPreparedStatement(SELECT_SEQUENCES, pstmt->{
            try{
                pstmt.setString(1, sequencer.getName());
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()){
                    result.add(new Sequence(rs.getString("SessionId"),sequencer.getName(),rs.getString("Sequence")));
                }
            }catch (SQLException e){
                log.error(e.getMessage(), e);
            }
        });
        return result;
    }

    private Connection connect(){
        Connection connection = null;
        try{
            connection = DriverManager.getConnection(url);
        }catch (SQLException e){
            log.error(e.getMessage(),e);
        }
        return connection;
    }

    /** Utility wrapper method for working with sql connections.
     *  Ensures connections close, and keeps duplicated code to a minimum.
     *
     * @param connectionConsumer
     */
    public void withConnection(Consumer<Connection> connectionConsumer){
        try(Connection conn = connect()){
            connectionConsumer.accept(conn);
        } catch (SQLException throwables) {
           log.error(throwables.getMessage(), throwables);
        }
    }

    /** Utility wrapper method for working with prepared statements.
     *  Keeps duplicated code to a minimum.
     * @param consumer The chunk of code expecting the created prepared statement.
     * @param sql the SQL string to create the prepared statement with.
     */
    public void withPreparedStatement( String sql, Consumer<PreparedStatement> consumer){
        withConnection((connection -> {
            withPreparedStatement(connection, sql, consumer);
        }));
    }

    /** Same as {@link #withPreparedStatement(String, Consumer)} but allows caller to provide an existing
     *  connection to allow connection reuse.
     *
     * @param connection
     * @param sql
     * @param consumer
     */
    public void withPreparedStatement(Connection connection, String sql, Consumer<PreparedStatement> consumer){
        try(PreparedStatement stmt = connection.prepareStatement(sql)){
            consumer.accept(stmt);
        }catch (SQLException e){
            log.error(e.getMessage(), e);
        }
    }

}
