/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.service;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Functions;
import com.todoroo.andlib.sql.Order;
import com.todoroo.andlib.sql.Query;
import com.todoroo.astrid.api.PermaSql;
import com.todoroo.astrid.dao.TagDataDao;
import com.todoroo.astrid.dao.TaskDao;
import com.todoroo.astrid.dao.UpdateDao;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Update;

/**
 * Service layer for {@link TagData}-centered activities.
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class TagDataService {

    @Autowired TagDataDao tagDataDao;
    @Autowired TaskDao taskDao;
    @Autowired UpdateDao updateDao;

    public TagDataService() {
        DependencyInjectionService.getInstance().inject(this);
    }

    // --- service layer

    /**
     * Query underlying database
     * @param query
     * @return
     */
    public TodorooCursor<TagData> query(Query query) {
        return tagDataDao.query(query);
    }

    /**
     * Save a single piece of metadata
     * @param metadata
     */
    public boolean save(TagData tagData) {
        return tagDataDao.persist(tagData);
    }

    /**
     * Delete a model
     */
    public void delete(long id) {
        tagDataDao.delete(id);
    }

    /**
     * Delete many
     */
    public void deleteWhere(Criterion where) {
        tagDataDao.deleteWhere(where);
    }

    /**
     *
     * @param properties
     * @param id id
     * @return item, or null if it doesn't exist
     */
    public TagData fetchById(long id, Property<?>... properties) {
        return tagDataDao.fetch(id, properties);
    }

    /**
     * Find a tag by name
     * @return null if doesn't exist
     */
    public TagData getTag(String name, Property<?>... properties) {
        TodorooCursor<TagData> cursor = tagDataDao.query(Query.select(properties).where(TagData.NAME.eqCaseInsensitive(name)));
        try {
            if(cursor.getCount() == 0)
                return null;
            cursor.moveToFirst();
            return new TagData(cursor);
        } finally {
            cursor.close();
        }
    }

    /**
     * Fetch tag data
     * @param queryTemplate
     * @param constraint
     * @param properties
     * @return
     */
    @SuppressWarnings("nls")
    public TodorooCursor<TagData> fetchFiltered(String queryTemplate, CharSequence constraint,
            Property<?>... properties) {
        Criterion whereConstraint = null;
        if(constraint != null)
            whereConstraint = Functions.upper(TagData.NAME).like("%" +
                    constraint.toString().toUpperCase() + "%");

        if(queryTemplate == null) {
            if(whereConstraint == null)
                return tagDataDao.query(Query.select(properties));
            else
                return tagDataDao.query(Query.select(properties).where(whereConstraint));
        }

        String sql;
        if(whereConstraint != null) {
            if(!queryTemplate.toUpperCase().contains("WHERE"))
                sql = queryTemplate + " WHERE " + whereConstraint;
            else
                sql = queryTemplate.replace("WHERE ", "WHERE " + whereConstraint + " AND ");
        } else
            sql = queryTemplate;

        sql = PermaSql.replacePlaceholders(sql);

        return tagDataDao.query(Query.select(properties).withQueryTemplate(sql));
    }

    /**
     * Get updates for this tagData
     * @return
     */
    public TodorooCursor<Update> getUpdates(TagData tagData) {
        return getUpdatesWithExtraCriteria(tagData, Criterion.all);
    }

    @SuppressWarnings("nls")
    public TodorooCursor<Update> getUpdatesWithExtraCriteria(TagData tagData, Criterion criterion) {
        if (tagData == null)
            return updateDao.query(Query.select(Update.PROPERTIES).where(
                    criterion).
                    orderBy(Order.desc(Update.CREATION_DATE)));
        if(tagData.getValue(TagData.REMOTE_ID) == 0)
            return updateDao.query(Query.select(Update.PROPERTIES).where(Update.TAGS_LOCAL.like("%," + tagData.getId() + ",%")));
        return updateDao.query(Query.select(Update.PROPERTIES).where(Criterion.and(criterion,
                Criterion.or(Update.TAGS.like("%," + tagData.getValue(TagData.REMOTE_ID) + ",%"),
                Update.TAGS_LOCAL.like("%," + tagData.getId() + ",%")))).
                orderBy(Order.desc(Update.CREATION_DATE)));
    }

    /**
     * Return update
     * @param tagData
     * @return
     */
    public Update getLatestUpdate(TagData tagData) {
        if(tagData.getValue(TagData.REMOTE_ID) == 0)
            return null;

        @SuppressWarnings("nls")
        TodorooCursor<Update> updates = updateDao.query(Query.select(Update.PROPERTIES).where(
                Update.TAGS.like("%," + tagData.getValue(TagData.REMOTE_ID) + ",%")).
                orderBy(Order.desc(Update.CREATION_DATE)).limit(1));
        try {
            if(updates.getCount() == 0)
                return null;
            updates.moveToFirst();
            return new Update(updates);
        } finally {
            updates.close();
        }
    }

}
