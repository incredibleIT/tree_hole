package com.rkos.modules.story.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rkos.modules.story.TestGenomeFactory;
import com.rkos.modules.story.model.*;
import com.rkos.modules.story.seed.SeedDataLoader;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Genome JSONB 读写集成测试（直连 PostgreSQL，不启动 Spring 上下文）。
 * <p>
 * 验证：
 * 1. 自定义 JsonbTypeHandler 序列化逻辑写入 JSONB（Types.OTHER）
 * 2. 读取 JSONB 并反序列化为 Java 对象
 * 3. 扁平化列与 JSONB 内部字段一致性
 * 4. 按 storyId 查询
 * <p>
 * 需要真实 PostgreSQL（Docker Compose localhost:5432）。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GenomeMapperTest {

    private static final String PG_URL = "jdbc:postgresql://localhost:5432/rkos_dev";
    private static final String PG_USER = "dev_user";
    private static final String PG_PASS = "dev_password";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private Connection connection;
    private String testStoryId;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection(PG_URL, PG_USER, PG_PASS);
        testStoryId = UUID.randomUUID().toString();
        // 清理当前测试数据
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM relationship_genomes WHERE story_id = ?")) {
            ps.setString(1, testStoryId);
            ps.executeUpdate();
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            // 清理测试数据
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM relationship_genomes WHERE story_id = ?")) {
                ps.setString(1, testStoryId);
                ps.executeUpdate();
            }
            connection.close();
        }
    }

    // ====== JSONB 写入 + 读取往返 ======

    @Test
    @Order(1)
    void insertAndSelect_genomeData_jsonbRoundTrip() throws Exception {
        GenomeData genomeData = buildFullGenomeData(testStoryId);
        String json = MAPPER.writeValueAsString(genomeData);

        // 写入（使用 Types.OTHER — 与 JsonbTypeHandler 一致）
        String insertSql = """
                INSERT INTO relationship_genomes
                    (story_id, agent_version, genome_data, overall_confidence, relationship_type, outcome_type)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, testStoryId);
            ps.setString(2, "v1.0");
            ps.setObject(3, json, Types.OTHER);
            ps.setBigDecimal(4, new BigDecimal("0.85"));
            ps.setString(5, "情侣");
            ps.setString(6, "分手");
            int rows = ps.executeUpdate();
            assertThat(rows).isEqualTo(1);

            // 获取自增 ID
            try (ResultSet keys = ps.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                assertThat(keys.getLong(1)).isGreaterThan(0);
            }
        }

        // 读取
        String selectSql = "SELECT * FROM relationship_genomes WHERE story_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setString(1, testStoryId);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();

                // 扁平化列
                assertThat(rs.getString("story_id")).isEqualTo(testStoryId);
                assertThat(rs.getString("agent_version")).isEqualTo("v1.0");
                assertThat(rs.getString("relationship_type")).isEqualTo("情侣");
                assertThat(rs.getString("outcome_type")).isEqualTo("分手");
                assertThat(rs.getBigDecimal("overall_confidence")).isEqualByComparingTo("0.85");

                // JSONB 反序列化
                String jsonbResult = rs.getString("genome_data");
                assertThat(jsonbResult).isNotNull();

                GenomeData loaded = MAPPER.readValue(jsonbResult, GenomeData.class);
                assertThat(loaded).isNotNull();

                // 9 维度验证
                assertThat(loaded.getGenomeId()).isEqualTo("test-genome-001");
                assertThat(loaded.getStoryId()).isEqualTo(testStoryId);
                assertThat(loaded.getVersion()).isEqualTo("v1.0");

                // Relationship
                assertThat(loaded.getRelationship().getType()).isEqualTo("情侣");
                assertThat(loaded.getRelationship().getDuration()).isEqualTo("3年");
                assertThat(loaded.getRelationship().getStage()).isEqualTo("冷淡期");
                assertThat(loaded.getRelationship().getStartContext()).isEqualTo("大学校园");

                // Participants
                assertThat(loaded.getParticipants()).containsKeys("A", "B");
                assertThat(loaded.getParticipants().get("A").getRole()).isEqualTo("叙述者");
                assertThat(loaded.getParticipants().get("A").getAttachment()).isEqualTo("焦虑型");
                assertThat(loaded.getParticipants().get("A").getBehaviors()).containsExactly("索取确认", "频繁追问");
                assertThat(loaded.getParticipants().get("B").getRole()).isEqualTo("对方");

                // KeyEvents
                assertThat(loaded.getKeyEvents()).hasSize(2);
                assertThat(loaded.getKeyEvents().get(0).getEvent()).isEqualTo("工作压力增加");
                assertThat(loaded.getKeyEvents().get(0).getPosition()).isEqualTo("beginning");

                // CausalChain (List<String>)
                assertThat(loaded.getCausalChain()).containsExactly("工作压力增加", "沟通减少", "争吵升级");

                // ConflictPatterns
                assertThat(loaded.getConflictPatterns()).hasSize(1);
                assertThat(loaded.getConflictPatterns().get(0).getType()).isEqualTo("communication");
                assertThat(loaded.getConflictPatterns().get(0).getFrequency()).isEqualTo("recurring");

                // Outcome
                assertThat(loaded.getOutcome().getType()).isEqualTo("分手");
                assertThat(loaded.getOutcome().getInitiator()).isEqualTo("B");
                assertThat(loaded.getOutcome().getManner()).isEqualTo("direct");

                // Lessons (List<String>)
                assertThat(loaded.getLessons()).containsExactly("沟通是关系的基础", "需要关注对方的情感需求");

                // Confidence
                assertThat(loaded.getConfidence().getOverall()).isEqualByComparingTo("0.85");
                assertThat(loaded.getConfidence().getRelationship()).isEqualByComparingTo("0.90");
                assertThat(loaded.getConfidence().getParticipants()).isEqualByComparingTo("0.80");
                assertThat(loaded.getConfidence().getCausalChain()).isEqualByComparingTo("0.75");
                assertThat(loaded.getConfidence().getConflictPatterns()).isEqualByComparingTo("0.88");

                // EmotionalArc
                assertThat(loaded.getEmotionalArc().getDominantEmotions()).containsExactly("遗憾", "不舍");
                assertThat(loaded.getEmotionalArc().getTrajectory()).isEqualTo("decline");
            }
        }
    }

    // ====== 扁平化列一致性 ======

    @Test
    @Order(2)
    void insertAndSelect_flatColumns_matchGenomeData() throws Exception {
        GenomeData genomeData = buildFullGenomeData(testStoryId);
        String json = MAPPER.writeValueAsString(genomeData);

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO relationship_genomes (story_id, agent_version, genome_data, overall_confidence, relationship_type, outcome_type) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, testStoryId);
            ps.setString(2, "v1.0");
            ps.setObject(3, json, Types.OTHER);
            ps.setBigDecimal(4, genomeData.getConfidence().getOverall());
            ps.setString(5, genomeData.getRelationship().getType());
            ps.setString(6, genomeData.getOutcome().getType());
            ps.executeUpdate();
        }

        // 读取并验证一致性
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM relationship_genomes WHERE story_id = ?")) {
            ps.setString(1, testStoryId);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();

                String flatRelType = rs.getString("relationship_type");
                String flatOutcomeType = rs.getString("outcome_type");
                BigDecimal flatConfidence = rs.getBigDecimal("overall_confidence");

                GenomeData loaded = MAPPER.readValue(rs.getString("genome_data"), GenomeData.class);

                assertThat(flatRelType).isEqualTo(loaded.getRelationship().getType());
                assertThat(flatOutcomeType).isEqualTo(loaded.getOutcome().getType());
                assertThat(flatConfidence).isEqualByComparingTo(loaded.getConfidence().getOverall());
            }
        }
    }

    // ====== selectByStoryId 模拟 ======

    @Test
    @Order(3)
    void selectByStoryId_existingRecord_returnsGenome() throws Exception {
        GenomeData genomeData = buildFullGenomeData(testStoryId);
        String json = MAPPER.writeValueAsString(genomeData);

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO relationship_genomes (story_id, agent_version, genome_data, overall_confidence, relationship_type, outcome_type) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, testStoryId);
            ps.setString(2, "v1.0");
            ps.setObject(3, json, Types.OTHER);
            ps.setBigDecimal(4, new BigDecimal("0.85"));
            ps.setString(5, "情侣");
            ps.setString(6, "分手");
            ps.executeUpdate();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM relationship_genomes WHERE story_id = ?")) {
            ps.setString(1, testStoryId);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("story_id")).isEqualTo(testStoryId);
                assertThat(rs.getString("agent_version")).isEqualTo("v1.0");
            }
        }
    }

    @Test
    @Order(4)
    void selectByStoryId_nonExisting_returnsEmpty() throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM relationship_genomes WHERE story_id = ?")) {
            ps.setString(1, "non-existent-story-id");
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isFalse();
            }
        }
    }

    // ====== 更新 JSONB ======

    @Test
    @Order(5)
    void update_genomeData_jsonbOverwritten() throws Exception {
        GenomeData genomeData = buildFullGenomeData(testStoryId);
        String json = MAPPER.writeValueAsString(genomeData);

        // 插入
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO relationship_genomes (story_id, agent_version, genome_data, overall_confidence, relationship_type, outcome_type) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, testStoryId);
            ps.setString(2, "v1.0");
            ps.setObject(3, json, Types.OTHER);
            ps.setBigDecimal(4, new BigDecimal("0.85"));
            ps.setString(5, "情侣");
            ps.setString(6, "分手");
            ps.executeUpdate();
        }

        // 修改 genomeData 并更新
        genomeData.getRelationship().setStage("修复期");
        genomeData.getOutcome().setType("和好");
        String updatedJson = MAPPER.writeValueAsString(genomeData);

        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE relationship_genomes SET genome_data = ?, outcome_type = ? WHERE story_id = ?")) {
            ps.setObject(1, updatedJson, Types.OTHER);
            ps.setString(2, "和好");
            ps.setString(3, testStoryId);
            int rows = ps.executeUpdate();
            assertThat(rows).isEqualTo(1);
        }

        // 验证更新
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM relationship_genomes WHERE story_id = ?")) {
            ps.setString(1, testStoryId);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                GenomeData loaded = MAPPER.readValue(rs.getString("genome_data"), GenomeData.class);
                assertThat(loaded.getRelationship().getStage()).isEqualTo("修复期");
                assertThat(loaded.getOutcome().getType()).isEqualTo("和好");
                assertThat(rs.getString("outcome_type")).isEqualTo("和好");
            }
        }
    }

    // ====== 删除 ======

    @Test
    @Order(6)
    void delete_removesRecord() throws Exception {
        GenomeData genomeData = buildFullGenomeData(testStoryId);
        String json = MAPPER.writeValueAsString(genomeData);

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO relationship_genomes (story_id, agent_version, genome_data, overall_confidence, relationship_type, outcome_type) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, testStoryId);
            ps.setString(2, "v1.0");
            ps.setObject(3, json, Types.OTHER);
            ps.setBigDecimal(4, new BigDecimal("0.85"));
            ps.setString(5, "情侣");
            ps.setString(6, "分手");
            ps.executeUpdate();
        }

        // 删除
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM relationship_genomes WHERE story_id = ?")) {
            ps.setString(1, testStoryId);
            int rows = ps.executeUpdate();
            assertThat(rows).isEqualTo(1);
        }

        // 验证删除
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM relationship_genomes WHERE story_id = ?")) {
            ps.setString(1, testStoryId);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0);
            }
        }
    }

    // ====== 种子数据写入 + 读回集成验证 ======

    @Test
    @Order(7)
    void seedData_writeAndReadBack_allFiveGenomes() throws Exception {
        List<RelationshipGenome> seedGenomes = SeedDataLoader.loadGenomes();
        assertThat(seedGenomes).hasSize(5);

        String insertSql = """
                INSERT INTO relationship_genomes
                    (story_id, agent_version, genome_data, overall_confidence, relationship_type, outcome_type)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        String selectSql = "SELECT * FROM relationship_genomes WHERE story_id = ?";
        String deleteSql = "DELETE FROM relationship_genomes WHERE story_id = ?";

        // 先清理种子数据（防止残留）
        for (RelationshipGenome seed : seedGenomes) {
            try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
                ps.setString(1, seed.getStoryId());
                ps.executeUpdate();
            }
        }

        try {
            // 逐条写入 + 读回验证
            for (RelationshipGenome seed : seedGenomes) {
                String json = MAPPER.writeValueAsString(seed.getGenomeData());

                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    ps.setString(1, seed.getStoryId());
                    ps.setString(2, seed.getAgentVersion());
                    ps.setObject(3, json, Types.OTHER);
                    ps.setBigDecimal(4, seed.getOverallConfidence());
                    ps.setString(5, seed.getRelationshipType());
                    ps.setString(6, seed.getOutcomeType());
                    int rows = ps.executeUpdate();
                    assertThat(rows).as("insert rows for %s", seed.getStoryId()).isEqualTo(1);
                }

                // 读回验证
                try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                    ps.setString(1, seed.getStoryId());
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).as("select for %s", seed.getStoryId()).isTrue();

                        // 扁平化列验证
                        assertThat(rs.getString("story_id")).isEqualTo(seed.getStoryId());
                        assertThat(rs.getString("agent_version")).isEqualTo(seed.getAgentVersion());
                        assertThat(rs.getString("relationship_type")).isEqualTo(seed.getRelationshipType());
                        assertThat(rs.getString("outcome_type")).isEqualTo(seed.getOutcomeType());
                        assertThat(rs.getBigDecimal("overall_confidence"))
                                .isEqualByComparingTo(seed.getOverallConfidence());

                        // JSONB 反序列化验证
                        String jsonbResult = rs.getString("genome_data");
                        assertThat(jsonbResult).isNotNull();
                        GenomeData loaded = MAPPER.readValue(jsonbResult, GenomeData.class);
                        assertThat(loaded).isNotNull();
                        assertThat(loaded.getStoryId()).isEqualTo(seed.getStoryId());
                        assertThat(loaded.getGenomeId()).isEqualTo(seed.getGenomeData().getGenomeId());
                        assertThat(loaded.getRelationship().getType()).isEqualTo(seed.getRelationshipType());
                        assertThat(loaded.getOutcome().getType()).isEqualTo(seed.getOutcomeType());
                        assertThat(loaded.getConfidence().getOverall())
                                .isEqualByComparingTo(seed.getOverallConfidence());
                    }
                }
            }
        } finally {
            // 清理种子数据
            for (RelationshipGenome seed : seedGenomes) {
                try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
                    ps.setString(1, seed.getStoryId());
                    ps.executeUpdate();
                }
            }
        }
    }

    // ====== 辅助方法（委托给 TestGenomeFactory）======

    private GenomeData buildFullGenomeData(String storyId) {
        return TestGenomeFactory.buildFullGenomeData(storyId);
    }
}
