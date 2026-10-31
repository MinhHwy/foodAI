package com.foodai.backend.entity;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.SmallIntJdbcType;

public class UnsignedSmallIntJdbcType extends SmallIntJdbcType {
	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>(javaType, this) {
			@Override
			protected void doBind(PreparedStatement statement, X value, int index, WrapperOptions options)
					throws SQLException {
				statement.setInt(index, toUnsignedSmallInt(getJavaType().unwrap(value, Integer.class, options)));
			}

			@Override
			protected void doBind(CallableStatement statement, X value, String name, WrapperOptions options)
					throws SQLException {
				statement.setInt(name, toUnsignedSmallInt(getJavaType().unwrap(value, Integer.class, options)));
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>(javaType, this) {
			@Override
			protected X doExtract(ResultSet resultSet, int index, WrapperOptions options) throws SQLException {
				return getJavaType().wrap(resultSet.getInt(index), options);
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
					throws SQLException {
				return getJavaType().wrap(statement.getInt(index), options);
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaType().wrap(statement.getInt(name), options);
			}
		};
	}

	private static int toUnsignedSmallInt(Integer value) throws SQLException {
		if (value < 0 || value > 65_535) {
			throw new SQLException("Value is outside the SMALLINT UNSIGNED range");
		}
		return value;
	}
}
