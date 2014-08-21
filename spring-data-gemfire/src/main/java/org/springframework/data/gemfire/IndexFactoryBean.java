/*
 * Copyright 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.gemfire;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.gemstone.gemfire.cache.RegionService;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.query.Index;
import com.gemstone.gemfire.cache.query.IndexExistsException;
import com.gemstone.gemfire.cache.query.QueryService;

/**
 * Factory bean for easy declarative creation of GemFire Indexes.
 * 
 * @author Costin Leau
 * @author David Turanski
 */
public class IndexFactoryBean implements InitializingBean, BeanNameAware, FactoryBean<Index> {

	private Index index;
	private QueryService queryService;
	private RegionService cache;
	private String beanName;
	private String name;
	private String expression;
	private String from;
	private String imports;
	private String type;
	private boolean override = true;

	public void afterPropertiesSet() throws Exception {
		if (queryService == null) {
			if (cache != null) {
				queryService = cache.getQueryService();
			}
		}

		Assert.notNull(queryService, "Query service required for index creation");
		Assert.hasText(expression, "Index expression is required");
		Assert.hasText(from, "Index from clause (regionPath) is required");

		if (StringUtils.hasText(type)) {
			if (type.equalsIgnoreCase("KEY") || type.equalsIgnoreCase("PRIMARY_KEY")) {
				Assert.isNull(imports, "The imports property is not supported for a key index");
			}
		}

		String indexName = StringUtils.hasText(name) ? name : beanName;

		Assert.hasText(indexName, "Index bean id or name is required");

		index = createIndex(queryService, indexName);
	}

	private Index createIndex(QueryService queryService, String indexName) throws Exception {

		Index existingIndex = null;
		
		for (Index idx : queryService.getIndexes()) {
			if (idx.getName().equals(indexName)) {
				existingIndex = idx;
 				break;
			}
		}
		if (existingIndex != null) {
			if (!override) {
				return existingIndex;
			} else {
				queryService.removeIndex(existingIndex);
			}
		}

		Index index = null;
		try {
			if ("KEY".equalsIgnoreCase(type) || "PRIMARY_KEY".equalsIgnoreCase(type)) {

				index = queryService.createKeyIndex(indexName, expression, from);

			} else if ("HASH".equalsIgnoreCase(type)) {
				if (StringUtils.hasText(imports)) {
					index = queryService.createHashIndex(indexName, expression, from, imports);	
				} else {
					index = queryService.createHashIndex(indexName, expression, from);
				}
			} else {
				if (StringUtils.hasText(imports)) {
					index = queryService.createIndex(indexName, expression, from, imports);
				} else {
					index = queryService.createIndex(indexName, expression, from);
				}
			}
			return index;

		} catch (IndexExistsException e) {
			for (Index idx : queryService.getIndexes()) {
				if (idx.getName().equals(indexName)) {
					return idx;
				}
			}
		} catch (Exception e) {
			if (existingIndex != null) {
				if (CollectionUtils.isEmpty(queryService.getIndexes())
						|| !queryService.getIndexes().contains(existingIndex)) {
					queryService.getIndexes().add(existingIndex);
				}
			}
		}

		return index;
	}

	public Index getObject() {
		return index;
	}

	public Class<?> getObjectType() {
		return (index != null ? index.getClass() : Index.class);
	}

	public boolean isSingleton() {
		return true;
	}

	/**
	 * Sets the underlying cache used for creating indexes.
	 * 
	 * @param cache cache used for creating indexes.
	 */
	public void setCache(RegionService cache) {
		this.cache = cache;
	}

	/**
	 * Sets the query service used for creating indexes.
	 * 
	 * @param service query service used for creating indexes.
	 */
	public void setQueryService(QueryService service) {
		this.queryService = service;
	}

	public void setBeanName(String name) {
		this.beanName = name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param expression the expression to set
	 */
	public void setExpression(String expression) {
		this.expression = expression;
	}

	/**
	 * @param from the from to set
	 */
	public void setFrom(String from) {
		this.from = from;
	}

	/**
	 * @param imports the imports to set
	 */
	public void setImports(String imports) {
		this.imports = imports;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @param override the override to set
	 */
	public void setOverride(boolean override) {
		this.override = override;
	}
}