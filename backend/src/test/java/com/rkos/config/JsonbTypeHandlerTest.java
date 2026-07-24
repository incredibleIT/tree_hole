package com.rkos.config;

import com.rkos.modules.story.model.GenomeData;
import com.rkos.modules.story.model.Relationship;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link JsonbTypeHandler} 单元测试。
 * <p>
 * 使用 Mock JDBC 对象验证：
 * 1. setNonNullParameter 使用 Types.OTHER 写入
 * 2. getNullableResult 正确反序列化 JSONB → GenomeData
 * 3. null/空 JSON 处理
 * 4. 序列化/反序列化异常处理
 */
@ExtendWith(MockitoExtension.class)
class JsonbTypeHandlerTest {

    private JsonbTypeHandler handler;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private CallableStatement callableStatement;

    @BeforeEach
    void setUp() {
        handler = new JsonbTypeHandler();
    }

    // ====== 写入测试 ======

    @Test
    void setNonNullParameter_usesTypesOther() throws SQLException {
        GenomeData data = GenomeData.builder()
                .genomeId("g1")
                .storyId("s1")
                .version("v1")
                .relationship(Relationship.builder().type("友谊").build())
                .build();

        handler.setNonNullParameter(preparedStatement, 1, data, JdbcType.OTHER);

        // 验证使用 Types.OTHER 传参（关键：PostgreSQL JSONB 要求）
        verify(preparedStatement).setObject(eq(1), anyString(), eq(Types.OTHER));
    }

    @Test
    void setNonNullParameter_serializesAllFields() throws SQLException {
        GenomeData data = GenomeData.builder()
                .genomeId("g1")
                .storyId("s1")
                .version("v1")
                .relationship(Relationship.builder().type("情侣").duration("3年").build())
                .build();

        handler.setNonNullParameter(preparedStatement, 2, data, JdbcType.OTHER);

        // 捕获传入的 JSON 字符串
        org.mockito.ArgumentCaptor<String> jsonCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(preparedStatement).setObject(eq(2), jsonCaptor.capture(), eq(Types.OTHER));

        String json = jsonCaptor.getValue();
        assertThat(json).contains("\"genomeId\":\"g1\"");
        assertThat(json).contains("\"storyId\":\"s1\"");
        assertThat(json).contains("\"type\":\"情侣\"");
        assertThat(json).contains("\"duration\":\"3年\"");
    }

    // ====== 读取测试（按列名） ======

    @Test
    void getNullableResult_byColumnName_deserializesCorrectly() throws SQLException {
        String json = """
                {
                  "genomeId": "g1",
                  "storyId": "s1",
                  "version": "v1",
                  "relationship": { "type": "情侣", "duration": "3年" },
                  "confidence": { "overall": 0.85 }
                }
                """;
        when(resultSet.getString("genome_data")).thenReturn(json);

        GenomeData result = handler.getNullableResult(resultSet, "genome_data");

        assertThat(result).isNotNull();
        assertThat(result.getGenomeId()).isEqualTo("g1");
        assertThat(result.getRelationship().getType()).isEqualTo("情侣");
        assertThat(result.getRelationship().getDuration()).isEqualTo("3年");
        assertThat(result.getConfidence().getOverall()).isEqualByComparingTo("0.85");
    }

    @Test
    void getNullableResult_byColumnName_nullJson_returnsNull() throws SQLException {
        when(resultSet.getString("genome_data")).thenReturn(null);

        GenomeData result = handler.getNullableResult(resultSet, "genome_data");

        assertThat(result).isNull();
    }

    @Test
    void getNullableResult_byColumnName_emptyString_returnsNull() throws SQLException {
        when(resultSet.getString("genome_data")).thenReturn("");

        GenomeData result = handler.getNullableResult(resultSet, "genome_data");

        assertThat(result).isNull();
    }

    // ====== 读取测试（按列索引） ======

    @Test
    void getNullableResult_byColumnIndex_deserializesCorrectly() throws SQLException {
        String json = """
                {
                  "genomeId": "g2",
                  "storyId": "s2",
                  "version": "v1"
                }
                """;
        when(resultSet.getString(1)).thenReturn(json);

        GenomeData result = handler.getNullableResult(resultSet, 1);

        assertThat(result).isNotNull();
        assertThat(result.getGenomeId()).isEqualTo("g2");
    }

    @Test
    void getNullableResult_byColumnIndex_nullJson_returnsNull() throws SQLException {
        when(resultSet.getString(1)).thenReturn(null);

        GenomeData result = handler.getNullableResult(resultSet, 1);

        assertThat(result).isNull();
    }

    // ====== CallableStatement 测试 ======

    @Test
    void getNullableResult_callableStatement_deserializesCorrectly() throws SQLException {
        String json = """
                {
                  "genomeId": "g3",
                  "storyId": "s3",
                  "version": "v1"
                }
                """;
        when(callableStatement.getString(1)).thenReturn(json);

        GenomeData result = handler.getNullableResult(callableStatement, 1);

        assertThat(result).isNotNull();
        assertThat(result.getGenomeId()).isEqualTo("g3");
    }

    // ====== 异常处理 ======

    @Test
    void getNullableResult_invalidJson_throwsSqlException() throws SQLException {
        when(resultSet.getString("genome_data")).thenReturn("{invalid json}}}");

        assertThatThrownBy(() -> handler.getNullableResult(resultSet, "genome_data"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("Failed to deserialize JSON to GenomeData");
    }

    // ====== 往返测试（写入 → 读取） ======

    @Test
    void roundTrip_writeAndRead_preservesData() throws Exception {
        GenomeData original = GenomeData.builder()
                .genomeId("round-trip-001")
                .storyId("story-rt")
                .version("v1.0")
                .relationship(Relationship.builder().type("家庭").stage("修复期").build())
                .build();

        // 模拟写入：捕获 JSON
        handler.setNonNullParameter(preparedStatement, 1, original, JdbcType.OTHER);
        org.mockito.ArgumentCaptor<String> jsonCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(preparedStatement).setObject(eq(1), jsonCaptor.capture(), eq(Types.OTHER));

        // 模拟读取：用捕获的 JSON 反序列化
        when(resultSet.getString("genome_data")).thenReturn(jsonCaptor.getValue());
        GenomeData result = handler.getNullableResult(resultSet, "genome_data");

        assertThat(result.getGenomeId()).isEqualTo("round-trip-001");
        assertThat(result.getStoryId()).isEqualTo("story-rt");
        assertThat(result.getRelationship().getType()).isEqualTo("家庭");
        assertThat(result.getRelationship().getStage()).isEqualTo("修复期");
    }

    @Test
    void getNullableResult_unknownFieldsIgnored_noException() throws SQLException {
        String json = """
                {
                  "genomeId": "g4",
                  "storyId": "s4",
                  "version": "v1",
                  "futureField": "should be ignored"
                }
                """;
        when(resultSet.getString("genome_data")).thenReturn(json);

        GenomeData result = handler.getNullableResult(resultSet, "genome_data");

        assertThat(result).isNotNull();
        assertThat(result.getGenomeId()).isEqualTo("g4");
    }
}
