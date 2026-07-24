package com.rkos.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rkos.modules.story.model.GenomeData;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 自定义 MyBatis TypeHandler，处理 PostgreSQL JSONB 字段与 {@link GenomeData} 的双向转换。
 * <p>
 * <b>为什么不用 MyBatis-Plus 内置 JacksonTypeHandler：</b>
 * 内置版本使用 {@code ps.setString()}（VARCHAR 类型），PostgreSQL 不接受
 * {@code varchar → jsonb} 隐式转换，会报
 * {@code column is of type jsonb but expression is of type character varying}。
 * 本实现使用 {@code ps.setObject(i, json, Types.OTHER)} 让 PostgreSQL JDBC 驱动正确识别为 JSONB。
 * <p>
 * 使用 Jackson 2.x {@link ObjectMapper} 序列化/反序列化，与 {@link Jackson2Config} 保持一致。
 */
@MappedTypes(GenomeData.class)
public class JsonbTypeHandler extends BaseTypeHandler<GenomeData> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, GenomeData parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            String json = MAPPER.writeValueAsString(parameter);
            ps.setObject(i, json, Types.OTHER);
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize GenomeData to JSON", e);
        }
    }

    @Override
    public GenomeData getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    public GenomeData getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    public GenomeData getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }

    private GenomeData parseJson(String json) throws SQLException {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, GenomeData.class);
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to deserialize JSON to GenomeData", e);
        }
    }
}
