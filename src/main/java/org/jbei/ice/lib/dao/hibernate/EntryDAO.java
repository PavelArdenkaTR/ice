package org.jbei.ice.lib.dao.hibernate;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.jbei.ice.lib.access.Permission;
import org.jbei.ice.lib.account.model.Account;
import org.jbei.ice.lib.common.logging.Logger;
import org.jbei.ice.lib.dao.DAOException;
import org.jbei.ice.lib.dto.entry.EntryType;
import org.jbei.ice.lib.dto.entry.Visibility;
import org.jbei.ice.lib.entry.EntryUtil;
import org.jbei.ice.lib.entry.model.*;
import org.jbei.ice.lib.group.Group;
import org.jbei.ice.lib.models.SelectionMarker;
import org.jbei.ice.lib.shared.ColumnField;

import java.util.*;

/**
 * DAO to manipulate {@link Entry} objects in the database.
 *
 * @author Hector Plahar, Timothy Ham, Zinovii Dmytriv,
 */
@SuppressWarnings("unchecked")
public class EntryDAO extends HibernateRepository<Entry> {

    public String getEntrySummary(long id) throws DAOException {
        return (String) currentSession().createCriteria(Entry.class)
                .add(Restrictions.eq("id", id))
                .setProjection(Projections.property("shortDescription")).uniqueResult();
    }

    public Set<String> getMatchingSelectionMarkers(String token, int limit) throws DAOException {
        return getMatchingField("selectionMarker.name", "selection_markers selectionMarker", token, limit);
    }

    public Set<String> getMatchingOriginOfReplication(String token, int limit) throws DAOException {
        return getMatchingField("plasmid.origin_of_replication", "Plasmids plasmid", token, limit);
    }

    public Set<String> getMatchingPromoters(String token, int limit) throws DAOException {
        return getMatchingField("plasmid.promoters", "Plasmids plasmid", token, limit);
    }

    public Set<String> getMatchingReplicatesIn(String token, int limit) throws DAOException {
        return getMatchingField("plasmid.replicates_in", "Plasmids plasmid", token, limit);
    }

    @SuppressWarnings("unchecked")
    protected Set<String> getMatchingField(String field, String object, String token, int limit) throws DAOException {
        Session session = currentSession();
        try {
            token = token.toUpperCase();
            String queryString = "select distinct " + field + " from " + object + " where "
                    + " UPPER(" + field + ") like '%" + token + "%'";
            Query query = session.createSQLQuery(queryString);
            if (limit > 0)
                query.setMaxResults(limit);
            return new HashSet<>(query.list());
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> getMatchingPlasmidPartNumbers(String token, int limit) throws DAOException {
        try {
            token = token.toUpperCase();
            String qString = "select distinct plasmid.partNumber from Plasmid plasmid where UPPER(plasmid.partNumber) "
                    + "like '%" + token + "%'";
            Query query = currentSession().createQuery(qString);
            if (limit > 0)
                query.setMaxResults(limit);

            return new HashSet<>(query.list());
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }

    @SuppressWarnings("unchecked")
    public Set<Entry> getMatchingEntryPartNumbers(String token, int limit) throws DAOException {
        try {
            token = token.toUpperCase();
            String qString = "select distinct entry from " + Entry.class.getName()
                    + " entry where UPPER(entry.partNumber) "
                    + "like '%" + token + "%'";
            Query query = currentSession().createQuery(qString);
            if (limit > 0)
                query.setMaxResults(limit);

            return new HashSet<>(query.list());
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }


    /**
     * Retrieve an {@link Entry} object from the database by id.
     *
     * @param id unique local identifier for entry record (typically synthetic database id)
     * @return Entry entry record associated with id
     * @throws DAOException
     */
    public Entry get(long id) throws DAOException {
        return super.get(Entry.class, id);
    }

    /**
     * Retrieve an {@link Entry} object in the database by recordId field.
     *
     * @param recordId unique global identifier for entry record (typically UUID)
     * @return Entry entry record associated with recordId
     * @throws DAOException
     */
    public Entry getByRecordId(String recordId) throws DAOException {
        Session session = currentSession();
        try {
            Criteria criteria = session.createCriteria(Entry.class).add(Restrictions.eq("recordId", recordId));
            Object object = criteria.uniqueResult();
            if (object != null) {
                return (Entry) object;
            }
            return null;
        } catch (HibernateException e) {
            Logger.error(e);
            throw new DAOException("Failed to retrieve entry by recordId: " + recordId, e);
        }
    }

    /**
     * Retrieve an {@link Entry} by it's part number.
     * <p>
     * If multiple Entries exist with the same part number, this method throws an exception.
     *
     * @param partNumber part number associated with entry
     * @return Entry
     * @throws DAOException
     */
    public Entry getByPartNumber(String partNumber) throws DAOException {
        Session session = currentSession();
        try {
            Criteria criteria = session.createCriteria(Entry.class).add(Restrictions.eq("partNumber", partNumber));
            Object object = criteria.uniqueResult();
            if (object != null) {
                return (Entry) object;
            }
            return null;
        } catch (HibernateException e) {
            Logger.error(e);
            throw new DAOException("Failed to retrieve entry by partNumber: " + partNumber, e);
        }
    }

    /**
     * Retrieve an {@link Entry} by it's name.The name must be unique to the entry
     *
     * @param name name associated with entry
     * @return Entry.
     * @throws DAOException
     */
    public Entry getByUniqueName(String name) throws DAOException {
        Session session = currentSession();

        try {
            Query query = session.createQuery("from " + Entry.class.getName() + " where name=:name AND visibility=:v");
            query.setParameter("name", name);
            query.setParameter("v", Visibility.OK.getValue());

            List queryResult = query.list();
            if (queryResult == null || queryResult.isEmpty()) {
                return null;
            }

            if (queryResult.size() > 1) {
                String msg = "Duplicate entries found for name " + name;
                Logger.error(msg);
                throw new DAOException(msg);
            }

            return (Entry) queryResult.get(0);
        } catch (HibernateException e) {
            Logger.error("Failed to retrieve entry by name: " + name, e);
            throw new DAOException("Failed to retrieve entry by name: " + name, e);
        }
    }

    /**
     * Retrieve {@link Entry Entries} visible to everyone.
     *
     * @return Number of visible entries.
     * @throws DAOException on hibernate exception
     */
    @SuppressWarnings({"unchecked"})
    public Set<Entry> retrieveVisibleEntries(Account account, Set<Group> groups, ColumnField sortField, boolean asc,
                                             int start, int count) throws DAOException {
        try {
            Session session = currentSession();
            String fieldName = columnFieldToString(sortField);
            String ascString = asc ? " asc" : " desc";
            String queryString = "SELECT DISTINCT e FROM Entry e, Permission p WHERE ";
            if (account != null)
                queryString += "(p.group IN (:groups) OR p.account = :account)";
            else
                queryString += "p.group IN (:groups)";

            queryString += " AND e = p.entry AND e.visibility = :v ORDER BY e." + fieldName + ascString;

            Query query = session.createQuery(queryString);
            query.setParameterList("groups", groups);
            query.setParameter("v", Visibility.OK.getValue());
            if (account != null)
                query.setParameter("account", account);
            query.setFirstResult(start);
            query.setMaxResults(count);
            List list = query.list();
            return new LinkedHashSet<>(list);
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }

    public long visibleEntryCount(Account account, Set<Group> groups) throws DAOException {
        Session session = currentSession();
        Criteria criteria = session.createCriteria(Permission.class);

        // expect everyone to at least belong to the everyone group so groups should never be empty
        Junction disjunction = Restrictions.disjunction().add(Restrictions.in("group", groups));
        if (account != null)
            disjunction.add(Restrictions.eq("account", account));

        criteria.add(disjunction);
        criteria.add(Restrictions.disjunction()
                .add(Restrictions.eq("canWrite", true))
                .add(Restrictions.eq("canRead", true)));

        Criteria entryCriteria = criteria.createCriteria("entry");
        entryCriteria.add(Restrictions.disjunction()
                .add(Restrictions.eq("visibility", Visibility.OK.getValue()))
                .add(Restrictions.isNull("visibility")));

        entryCriteria.setProjection(Projections.countDistinct("id"));
        Number rowCount = (Number) entryCriteria.uniqueResult();
        return rowCount.longValue();
    }

    // calculated as "available - userowned"
    public long sharedEntryCount(Account requester, Set<Group> accountGroups) throws DAOException {
        try {
            Session session = currentSession();
            Criteria criteria = session.createCriteria(Permission.class);

            // user owned
            criteria.setProjection(Projections.property("entry"));

            // expect everyone to at least belong to the everyone group so groups should never be empty
            criteria.add(Restrictions.disjunction()
                    .add(Restrictions.in("group", accountGroups)));
//                                     .add(Restrictions.eq("account", requester)));

            // should be able to either read or write
            criteria.add(Restrictions.disjunction()
                    .add(Restrictions.eq("canWrite", true))
                    .add(Restrictions.eq("canRead", true)));

            Criteria entryCriteria = criteria.createCriteria("entry");
            entryCriteria.add(Restrictions.disjunction()
                    .add(Restrictions.eq("visibility", Visibility.OK.getValue()))
                    .add(Restrictions.isNull("visibility")));
            entryCriteria.add(Restrictions.ne("ownerEmail", requester.getEmail()));
            criteria.setProjection(Projections.rowCount());
            Number rowCount = (Number) criteria.uniqueResult();
            return rowCount.longValue();
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Entry> sharedWithUserEntries(Account requester, Set<Group> groups, ColumnField sort,
                                             boolean asc, int start, int limit) throws DAOException {
        try {

            Session session = currentSession();
            String fieldName = columnFieldToString(sort);
            String ascString = asc ? " asc" : " desc";
            String queryString = "SELECT DISTINCT e FROM Entry e, Permission p WHERE p.group IN (:groups) "
                    + " AND e.ownerEmail <> :oe AND e = p.entry AND e.visibility = :v ORDER BY e." + fieldName +
                    ascString;

            Query query = session.createQuery(queryString);
            query.setParameterList("groups", groups);
            query.setParameter("v", Visibility.OK.getValue());
            query.setParameter("oe", requester.getEmail());
            query.setFirstResult(start);
            query.setMaxResults(limit);
            List list = query.list();
            return new ArrayList<>(list);
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }

    // checks permission (does not include pending entries)
    @SuppressWarnings("unchecked")
    public List<Entry> retrieveUserEntries(Account requestor, String user, Set<Group> groups,
                                           ColumnField sortField, boolean asc, int start, int limit) throws DAOException {
        Session session = currentSession();
        Criteria criteria = session.createCriteria(Permission.class);
        criteria.setProjection(Projections.property("entry"));

        // expect everyone to at least belong to the everyone group so groups should never be empty
        Junction disjunction = Restrictions.disjunction().add(Restrictions.in("group", groups));
        disjunction.add(Restrictions.eq("account", requestor));

        criteria.add(disjunction);
        criteria.add(Restrictions.disjunction()
                .add(Restrictions.eq("canWrite", true))
                .add(Restrictions.eq("canRead", true)));

        Criteria entryCriteria = criteria.createCriteria("entry");
        entryCriteria.add(Restrictions.disjunction()
                .add(Restrictions.eq("visibility", Visibility.OK.getValue()))
                .add(Restrictions.isNull("visibility")));
        entryCriteria.add(Restrictions.eq("ownerEmail", user));

        // sort
        String fieldName = columnFieldToString(sortField);

        entryCriteria.addOrder(asc ? Order.asc(fieldName) : Order.desc(fieldName));
        entryCriteria.setFirstResult(start);
        entryCriteria.setMaxResults(limit);
        entryCriteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        return new LinkedList<>(entryCriteria.list());
    }

    /**
     * @return number of entries that have visibility of "OK" or null (which is a legacy equivalent to "OK")
     * @throws DAOException
     */
    public long getAllEntryCount() throws DAOException {
        Session session = currentSession();
        Criteria criteria = session.createCriteria(Entry.class.getName());
        criteria.add(Restrictions.disjunction()
                .add(Restrictions.eq("visibility", Visibility.OK.getValue()))
                .add(Restrictions.isNull("visibility")));
        return (Long) criteria.setProjection(Projections.rowCount()).uniqueResult();
    }

    /**
     * Retrieve {@link Entry} objects of the given list of ids.
     *
     * @param ids list of ids to retrieve
     * @return ArrayList of Entry objects.
     * @throws DAOException
     */
    @SuppressWarnings("unchecked")
    public List<Entry> getEntriesByIdSet(List<Long> ids) throws DAOException {
        if (ids == null || ids.isEmpty()) {
            return new LinkedList<>();
        }

        try {
            Query query = currentSession().createQuery(
                    "FROM " + Entry.class.getName() + " e WHERE e.id IN (:ids) order by id asc");
            ArrayList<Long> list = new ArrayList<>(ids.size());
            for (Number id : ids) {
                list.add(id.longValue());
            }
            query.setParameterList("ids", list);
            List result = query.list();
            return new LinkedList<>(result);
        } catch (HibernateException e) {
            Logger.error(e);
            throw new DAOException("Failed to retrieve entries!", e);
        }
    }

    @Override
    public Entry create(Entry entry) throws DAOException {
        try {
            entry = super.create(entry);
            if (entry == null)
                throw new DAOException("Could not save entry");

            // partNumber
            String partNumberPrefix = EntryUtil.getPartNumberPrefix();
            String formatted = String.format("%06d", entry.getId());
            entry.setPartNumber(partNumberPrefix + formatted);
            return update(entry);
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }

    public synchronized void generateNextStrainNameForEntry(Entry entry, String prefix) throws DAOException {
        Session session = currentSession();
        String queryString = "select name from entries where (LEFT(name, 6)) in (";
        for (int i = 0; i < 10; i += 1) {
            if (i != 0)
                queryString += ", ";
            queryString += ("\'" + prefix + i + "\'");
        }
        queryString += ") ORDER by name DESC";
        Query query = session.createSQLQuery(queryString);
        query.setMaxResults(5);
        List results;
        try {
            results = query.list();
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }

        if (results.isEmpty()) {
            String name = prefix + "0001";
            entry.setName(name);
            session.update(entry);
            return;
        }

        @SuppressWarnings("unchecked")
        ArrayList<Object> tempList = new ArrayList<>(results);
        for (int i = 0; i < results.size(); i += 1) {
            String name = (String) tempList.get(i);
            String[] split = name.split(prefix);
            if (split.length != 2)
                continue;

            try {
                int value = Integer.valueOf(split[1]);
                value += 1;
                entry.setName(prefix + String.format("%0" + 4 + "d", value));
                session.update(entry);
                return;
            } catch (NumberFormatException nfe) {
                Logger.warn(nfe.getMessage());
            }
        }

        throw new DAOException("Could not parse any of the retrieved strain names");
    }

    @SuppressWarnings("unchecked")
    public List<Entry> getByVisibility(String ownerEmail, Visibility visibility, ColumnField field, boolean asc,
                                       int start, int limit) throws DAOException {
        try {
            String fieldName = columnFieldToString(field);
            Session session = currentSession();
            String orderSuffix = (" ORDER BY e." + fieldName + " " + (asc ? "ASC" : "DESC"));
            String queryString = "from " + Entry.class.getName() + " e where ";
            if (ownerEmail != null)
                queryString += " owner_email = :oe AND";

            queryString += " visibility = " + visibility.getValue() + orderSuffix;
            Query query = session.createQuery(queryString);
            if (ownerEmail != null)
                query.setParameter("oe", ownerEmail);

            query.setMaxResults(limit);
            query.setFirstResult(start);
            List list = query.list();
            return new LinkedList<>(list);
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }

    public long getByVisibilityCount(String ownerEmail, Visibility visibility) throws DAOException {
        Criteria criteria = currentSession().createCriteria(Entry.class);
        if (ownerEmail != null)
            criteria = criteria.add(Restrictions.eq("ownerEmail", ownerEmail));
        criteria.add(Restrictions.eq("visibility", visibility.getValue()));
        return (Long) criteria.setProjection(Projections.rowCount()).uniqueResult();
    }

    public long getPendingCount() throws DAOException {
        Criteria criteria = currentSession().createCriteria(Entry.class)
                .add(Restrictions.eq("visibility", Visibility.PENDING.getValue()));
        return (Long) criteria.setProjection(Projections.rowCount()).uniqueResult();
    }

    protected String columnFieldToString(ColumnField field) {
        if (field == null)
            return "creationTime";

        switch (field) {
            case TYPE:
                return "recordType";

            case STATUS:
                return "status";

            case PART_ID:
                return "partNumber";

            case NAME:
                return "name";

            case SUMMARY:
                return "shortDescription";

            case CREATED:
            default:
                return "creationTime";
        }
    }

    // does not check permission (includes pending entries)
    @SuppressWarnings("unchecked")
    public List<Entry> retrieveOwnerEntries(String ownerEmail, ColumnField sort, boolean asc, int start, int limit)
            throws DAOException {
        try {
            String fieldName = columnFieldToString(sort);
            Session session = currentSession();
            String orderSuffix = (" ORDER BY e." + fieldName + " " + (asc ? "ASC" : "DESC"));
            String queryString = "from " + Entry.class.getName() + " e where owner_email = :oe "
                    + "AND (visibility is null or visibility = " + Visibility.OK.getValue() + " OR visibility = "
                    + Visibility.PENDING.getValue() + ")" + orderSuffix;
            Query query = session.createQuery(queryString);
            query.setParameter("oe", ownerEmail);
            query.setMaxResults(limit);
            query.setFirstResult(start);
            List list = query.list();
            return new LinkedList<>(list);
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }

    public List<Long> getOwnerEntryIds(String ownerEmail, EntryType type) {
        Criteria criteria = currentSession().createCriteria(Entry.class)
                .add(Restrictions.eq("ownerEmail", ownerEmail));

        if (type != null)
            criteria.add(Restrictions.eq("recordType", type.getName()));

        return criteria.setProjection(Projections.id())
                .list();
    }

    public Set<Entry> retrieveAllEntries(ColumnField sort, boolean asc, int start, int limit)
            throws DAOException {
        try {
            if (sort == null)
                sort = ColumnField.CREATED;

            String fieldName;
            switch (sort) {
                case TYPE:
                    fieldName = "recordType";
                    break;

                case STATUS:
                    fieldName = "status";
                    break;

                case NAME:
                    fieldName = "name";
                    break;

                case PART_ID:
                    fieldName = "partNumber";
                    break;

                case SUMMARY:
                    fieldName = "shortDescription";
                    break;

                case CREATED:
                default:
                    fieldName = "creationTime";
                    break;
            }

            Session session = currentSession();
            String orderSuffix = (" ORDER BY e." + fieldName + " " + (asc ? "ASC" : "DESC"));
            String queryString = "from " + Entry.class.getName() + " e where (visibility is null or visibility = "
                    + Visibility.OK.getValue() + " OR visibility = "
                    + Visibility.PENDING.getValue() + ")" + orderSuffix;
            Query query = session.createQuery(queryString);
            query.setMaxResults(limit);
            query.setFirstResult(start);
            List list = query.list();
            return new LinkedHashSet<>(list);
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }

    // does not check permissions (includes pending entries)

    public long ownerEntryCount(String ownerEmail) throws DAOException {
        Session session = currentSession();
        try {
            Criteria criteria = session.createCriteria(Entry.class)
                    .add(Restrictions.eq("ownerEmail", ownerEmail));
            criteria.add(Restrictions.disjunction()
                    .add(Restrictions.eq("visibility", Visibility.OK.getValue()))
                    .add(Restrictions.eq("visibility", Visibility.PENDING.getValue()))
                    .add(Restrictions.isNull("visibility")));
            criteria.setProjection(Projections.rowCount());
            Number rowCount = (Number) criteria.uniqueResult();
            return rowCount.longValue();
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }

    // checks permission, does not include pending entries
    public long ownerEntryCount(Account requester, String ownerEmail, Set<Group> accountGroups) throws DAOException {
        try {
            Session session = currentSession();
            Criteria criteria = session.createCriteria(Permission.class);
            criteria.setProjection(Projections.property("entry"));

            // expect everyone to at least belong to the everyone group so groups should never be empty
            Junction disjunction = Restrictions.disjunction().add(Restrictions.in("group", accountGroups));
            disjunction.add(Restrictions.eq("account", requester));

            criteria.add(disjunction);
            criteria.add(Restrictions.disjunction()
                    .add(Restrictions.eq("canWrite", true))
                    .add(Restrictions.eq("canRead", true)));

            Criteria entryCriteria = criteria.createCriteria("entry");
            entryCriteria.add(Restrictions.disjunction()
                    .add(Restrictions.eq("visibility", Visibility.OK.getValue()))
                    .add(Restrictions.isNull("visibility")));
            entryCriteria.add(Restrictions.eq("ownerEmail", ownerEmail));
            criteria.setProjection(Projections.rowCount());
            Number rowCount = (Number) criteria.uniqueResult();
            return rowCount.longValue();
        } catch (HibernateException he) {
            Logger.error(he);
            throw new DAOException(he);
        }
    }

    // experimental. do not use
    public void fullDelete(Entry entry) throws DAOException {
        // delete from sub class (plasmid, strain, seed)
        Class<? extends Entry> clazz;

        if (entry.getRecordType().equalsIgnoreCase(EntryType.PLASMID.toString())) {
            clazz = Plasmid.class;
        } else if (entry.getRecordType().equalsIgnoreCase(EntryType.STRAIN.toString())) {
            clazz = Strain.class;
        } else if (entry.getRecordType().equalsIgnoreCase(EntryType.PART.toString())) {
            clazz = Part.class;
        } else if (entry.getRecordType().equalsIgnoreCase(EntryType.ARABIDOPSIS.toString())) {
            clazz = ArabidopsisSeed.class;
        } else
            throw new DAOException("Unrecognized entry type");

        // delete from bulk upload entry
        String hql = "delete from bulk_upload_entry where entry_id=" + entry.getId();
        currentSession().createSQLQuery(hql).executeUpdate();

        hql = "delete from " + clazz.getName() + " where entries_id=:entry";
        currentSession().createQuery(hql).setParameter("entry", entry.getId()).executeUpdate();

        // delete from links
        hql = "delete from " + Link.class.getName() + " where entry=:entry";
        currentSession().createQuery(hql).setParameter("entry", entry).executeUpdate();

        // delete from selection markers
        hql = "delete from " + SelectionMarker.class.getName() + " where entry=:entry";
        currentSession().createQuery(hql).setParameter("entry", entry).executeUpdate();

        // finally delete actual entry
        delete(entry);

        currentSession().clear();
    }

    /**
     * links are stored in a join table in the form [entry_id, linked_entry_id] which is used
     * to represent a parent child reln. This method returns the parents in the reln
     */
    @SuppressWarnings("unchecked")
    public List<Entry> getParents(long entryId) throws DAOException {
        String sql = "select entry_id from entry_entry where linked_entry_id=" + entryId;
        List list = currentSession().createSQLQuery(sql).list();
        return getEntriesByIdSet(list);
    }

    public int getDeletedCount(String ownerUserId) {
        Number itemCount = (Number) currentSession()
                .createCriteria(Entry.class)
                .setProjection(Projections.countDistinct("id"))
                .add(Restrictions.eq("ownerEmail", ownerUserId))
                .add(Restrictions.eq("visibility", Visibility.DELETED.getValue()))
                .uniqueResult();
        return itemCount.intValue();
    }
}
