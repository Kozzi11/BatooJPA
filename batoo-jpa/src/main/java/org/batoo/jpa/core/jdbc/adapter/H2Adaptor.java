/*
 * Copyright (c) 2012 - Batoo Software ve Consultancy Ltd.
 * 
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.batoo.jpa.core.jdbc.adapter;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import javax.persistence.GenerationType;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder.Trimspec;
import javax.sql.DataSource;

import org.batoo.jpa.core.impl.jdbc.AbstractColumn;
import org.batoo.jpa.core.impl.jdbc.BasicColumn;
import org.batoo.jpa.core.impl.jdbc.dbutils.QueryRunner;
import org.batoo.jpa.core.impl.jdbc.dbutils.SingleValueHandler;
import org.batoo.jpa.core.impl.model.SequenceGenerator;
import org.batoo.jpa.core.jdbc.IdType;

import com.google.common.base.Joiner;

/**
 * JDBC Adapter for H2DB.
 * 
 * @author hceylan
 * @since $version
 */
public class H2Adaptor extends JdbcAdaptor {

	private static final String[] PRODUCT_NAMES = new String[] { "H2" };

	/**
	 * 
	 * @since $version
	 */
	public H2Adaptor() {
		super();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public String applyConcat(List<String> arguments) {
		return Joiner.on(" || ").join(arguments);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public String applyLikeEscape(String escapePattern) {
		return " {ESCAPE " + escapePattern + "}";

	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public String applyLock(String sql, LockModeType lockMode) {
		switch (lockMode) {
			case PESSIMISTIC_FORCE_INCREMENT:
			case PESSIMISTIC_READ:
				return sql + "\nFOR READ ONLY";
			case PESSIMISTIC_WRITE:
				return sql + "\nFOR UPDATE";
			default:
				break;
		}

		return sql;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public String applyPagination(String sql, int startPosition, int maxResult) {
		if ((startPosition != 0) || (maxResult != Integer.MAX_VALUE)) {
			sql = sql + "\nLIMIT ?";

			if (startPosition != 0) {
				sql = sql + "OFFSET ?";
			}
		}

		return sql;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public String applyTrim(Trimspec trimspec, String trimChar, String argument) {
		final StringBuilder builder = new StringBuilder("TRIM(");

		if ((trimspec != null) || (trimChar != null)) {
			if (trimspec == null) {
				trimspec = Trimspec.BOTH;
			}

			builder.append(trimspec.toString()).append(" ");
		}

		if (trimChar != null) {
			builder.append(trimChar).append(" ");
		}

		if ((trimspec != null) || (trimChar != null)) {
			builder.append("FROM ");
		}

		return builder.append(argument).append(")").toString();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public String createColumnDDL(AbstractColumn column) {
		final boolean identity = column.getIdType() == IdType.IDENTITY;

		return column.getName() + " " // name part
			+ this.getColumnType(column, column.getSqlType()) // data type part
			+ (!column.isNullable() ? " NOT NULL" : "") // not null part
			+ (column.isUnique() ? " UNIQUE" : "") // not null part
			+ (identity ? " GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1)" : ""); // auto increment part
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void createSequenceIfNecessary(DataSource datasource, SequenceGenerator sequence) {
		final String sql = "CREATE SEQUENCE " + sequence.getQName() // ;
			+ " START WITH " + sequence.getInitialValue() //
			+ " INCREMENT BY " + sequence.getAllocationSize();

		try {
			new QueryRunner(datasource).update(sql);
		}
		catch (final SQLException e) {
			this.logRelaxed(e, "Cannot create sequence " + sequence.getName());
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	protected String getCastClassName(Class<?> clazz) {
		if (clazz == Byte.class) {
			return "TINYINT";
		}
		else if (clazz == Short.class) {
			return "SMALLINT";
		}
		else if (clazz == Float.class) {
			return "FLOAT4";
		}

		return super.getCastClassName(clazz);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	protected String getColumnType(AbstractColumn cd, int sqlType) {
		switch (sqlType) {
			case Types.BLOB:
				return "VARBINARY(" + cd.getLength() + ")";
			case Types.CLOB:
				return "CLOB(" + cd.getLength() + ")";
			case Types.VARCHAR:
				return "VARCHAR(" + cd.getLength() + ")";
			case Types.TIME:
				return "TIME";
			case Types.DATE:
				return "DATE";
			case Types.TIMESTAMP:
				return "TIMESTAMP";
			case Types.CHAR:
				return "CHAR";
			case Types.BOOLEAN:
				return "BOOLEAN";
			case Types.TINYINT:
			case Types.SMALLINT:
			case Types.INTEGER:
				return "INTEGER";
			case Types.BIGINT:
				return "BIGINT";
			case Types.FLOAT:
				return "FLOAT" + (cd.getPrecision() > 0 ? "(" + cd.getPrecision() + ")" : "");
			case Types.DOUBLE:
				return "DOUBLE" + (cd.getPrecision() > 0 ? "(" + cd.getPrecision() + ")" : "");
			case Types.DECIMAL:
				return "DECIMAL" + (cd.getPrecision() > 0 ? "(" + cd.getPrecision() + (cd.getScale() > 0 ? "," + cd.getScale() : "") + ")" : "");
		}

		throw new IllegalArgumentException("Unhandled sql type: " + sqlType);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	protected String getDatabaseName() {
		return "HSqlDb";
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	protected String getDropForeignKeySql(String schema, String table, String foreignKey) {
		final String qualifiedName = Joiner.on(".").skipNulls().join(schema, table);

		return "ALTER TABLE " + qualifiedName + " DROP CONSTRAINT " + foreignKey;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public long getNextSequence(DataSource datasource, String sequenceName) throws SQLException {
		return new QueryRunner(datasource) //
		.query("CALL NEXT VALUE FOR " + sequenceName, new SingleValueHandler<Number>()).longValue();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public PaginationParamsOrder getPaginationParamsOrder() {
		return PaginationParamsOrder.SQL_MAX_START;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	protected String[] getProductNames() {
		return H2Adaptor.PRODUCT_NAMES;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public String getSelectLastIdentitySql(BasicColumn identityColumn) {
		return "CALL IDENTITY()";
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean paginationNeedsMaxResultsAlways() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean paginationNeedsStartAlways() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public IdType supports(GenerationType type) {
		if (type == null) {
			return IdType.SEQUENCE;
		}

		switch (type) {
			case IDENTITY:
				return IdType.IDENTITY;
			case SEQUENCE:
				return IdType.SEQUENCE;
			case TABLE:
				return IdType.TABLE;
			default:
				return IdType.SEQUENCE;
		}
	}
}
