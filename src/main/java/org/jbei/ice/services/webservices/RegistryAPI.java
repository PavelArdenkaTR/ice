package org.jbei.ice.services.webservices;

import java.util.ArrayList;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.apache.commons.lang.NotImplementedException;
import org.jbei.ice.controllers.AccountController;
import org.jbei.ice.controllers.EntryController;
import org.jbei.ice.controllers.SearchController;
import org.jbei.ice.controllers.SequenceController;
import org.jbei.ice.controllers.common.ControllerException;
import org.jbei.ice.lib.authentication.InvalidCredentialsException;
import org.jbei.ice.lib.logging.Logger;
import org.jbei.ice.lib.managers.EntryManager;
import org.jbei.ice.lib.managers.ManagerException;
import org.jbei.ice.lib.models.Account;
import org.jbei.ice.lib.models.Entry;
import org.jbei.ice.lib.models.EntryFundingSource;
import org.jbei.ice.lib.models.Link;
import org.jbei.ice.lib.models.Name;
import org.jbei.ice.lib.models.Part;
import org.jbei.ice.lib.models.Plasmid;
import org.jbei.ice.lib.models.SelectionMarker;
import org.jbei.ice.lib.models.Sequence;
import org.jbei.ice.lib.models.SessionData;
import org.jbei.ice.lib.models.Strain;
import org.jbei.ice.lib.parsers.GeneralParser;
import org.jbei.ice.lib.permissions.PermissionException;
import org.jbei.ice.lib.search.blast.BlastResult;
import org.jbei.ice.lib.search.blast.ProgramTookTooLongException;
import org.jbei.ice.lib.search.lucene.SearchResult;
import org.jbei.ice.lib.vo.FeaturedDNASequence;
import org.jbei.ice.web.common.ViewException;

@WebService(targetNamespace = "https://api.registry.jbei.org/")
public class RegistryAPI {
    public String login(@WebParam(name = "login") String login,
            @WebParam(name = "password") String password) throws SessionException, ServiceException {
        String sessionId = null;

        try {
            SessionData sessionData = AccountController.authenticate(login, password);

            sessionId = sessionData.getSessionKey();
        } catch (InvalidCredentialsException e) {
            Logger.warn("Invalid credentials provided by user: " + login);

            throw new SessionException("Invalid credentials!");
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        log("User by login '" + login + "' successfully logged in");

        return sessionId;
    }

    public void logout(@WebParam(name = "sessionId") String sessionId) throws ServiceException {
        try {
            AccountController.deauthenticate(sessionId);

            log("User by sessionId '" + sessionId + "' successfully logged out");
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }
    }

    public boolean isAuthenticated(@WebParam(name = "sessionId") String sessionId)
            throws ServiceException {
        boolean authenticated = false;

        try {
            authenticated = AccountController.isAuthenticated(sessionId);
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return authenticated;
    }

    public int getNumberOfPublicEntries() throws ServiceException {
        int result = 0;

        try {
            result = EntryManager.getNumberOfVisibleEntries();
        } catch (ManagerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return result;
    }

    public ArrayList<SearchResult> search(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "query") String query) throws ServiceException, SessionException {
        ArrayList<SearchResult> results = null;

        try {
            SearchController searchController = getSearchController(sessionId);

            results = searchController.find(query);

            log("User '" + searchController.getAccount().getEmail() + "' searched for '" + query
                    + "'");
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return results;
    }

    public ArrayList<BlastResult> blastn(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "querySequence") String querySequence) throws SessionException,
            ServiceException {
        ArrayList<BlastResult> results = null;

        try {
            SearchController searchController = getSearchController(sessionId);

            results = searchController.blastn(querySequence);

            log("User '" + searchController.getAccount().getEmail() + "' blasted 'blastn' for "
                    + querySequence);
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (ProgramTookTooLongException e) {
            Logger.error(e);

            throw new ServiceException(
                    "It took to long to search for sequence, try shorter sequence.");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return results;
    }

    public ArrayList<BlastResult> tblastx(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "querySequence") String querySequence) throws SessionException,
            ServiceException {
        ArrayList<BlastResult> results = null;

        try {
            SearchController searchController = getSearchController(sessionId);

            results = searchController.tblastx(querySequence);

            log("User '" + searchController.getAccount().getEmail() + "' blasted 'tblastx' for "
                    + querySequence);
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (ProgramTookTooLongException e) {
            Logger.error(e);

            throw new ServiceException(
                    "It took to long to search for sequence, try shorter sequence.");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return results;
    }

    public Entry getByRecordId(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId) throws SessionException, ServiceException,
            ServicePermissionException {
        Entry entry = null;

        try {
            EntryController entryController = getEntryController(sessionId);

            entry = entryController.getByRecordId(entryId);
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (PermissionException e) {
            throw new ServicePermissionException("No permissions to read this entry by entryId: "
                    + entryId);
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return entry;
    }

    public Entry getByPartNumber(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "partNumber") String partNumber) throws SessionException,
            ServiceException, ServicePermissionException {
        Entry entry = null;

        try {
            EntryController entryController = getEntryController(sessionId);

            entry = entryController.getByPartNumber(partNumber);
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (PermissionException e) {
            throw new ServicePermissionException(
                    "No permissions to read this entry by partNumber: " + partNumber);
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return entry;
    }

    public boolean hasReadPermissions(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId) throws SessionException, ServiceException,
            ServicePermissionException {
        boolean result = false;

        try {
            EntryController entryController = getEntryController(sessionId);

            result = entryController.hasReadPermissionByRecordId(entryId);
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return result;
    }

    public boolean hasWritePermissions(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId) throws SessionException, ServiceException {
        boolean result = false;

        try {
            EntryController entryController = getEntryController(sessionId);

            result = entryController.hasWritePermissionByRecordId(entryId);
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return result;
    }

    public Plasmid createPlasmid(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "plasmid") Plasmid plasmid) throws SessionException, ServiceException {
        Entry newEntry = null;
        try {
            EntryController entryController = getEntryController(sessionId);

            Entry remoteEntry = createEntry(sessionId, plasmid);

            newEntry = entryController.createEntry(remoteEntry);

            log("User '" + entryController.getAccount().getEmail() + "' created plasmid: '"
                    + plasmid.getRecordId() + "', " + plasmid.getId());
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return (Plasmid) newEntry;
    }

    public Strain createStrain(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "strain") Strain strain) throws SessionException, ServiceException {
        Entry newEntry = null;
        try {
            EntryController entryController = getEntryController(sessionId);

            Entry remoteEntry = createEntry(sessionId, strain);

            newEntry = entryController.createEntry(remoteEntry);

            log("User '" + entryController.getAccount().getEmail() + "' created strain: '"
                    + strain.getRecordId() + "', " + strain.getId());
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return (Strain) newEntry;
    }

    public Part createPart(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "part") Part part) throws SessionException, ServiceException {
        Entry newEntry = null;
        try {
            EntryController entryController = getEntryController(sessionId);

            Entry remoteEntry = createEntry(sessionId, part);

            newEntry = entryController.createEntry(remoteEntry);

            log("User '" + entryController.getAccount().getEmail() + "' created part: '"
                    + part.getRecordId() + "', " + part.getId());
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return (Part) newEntry;
    }

    public Plasmid updatePlasmid(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "plasmid") Plasmid plasmid) throws SessionException, ServiceException,
            ServicePermissionException {
        Entry savedEntry = null;

        try {
            EntryController entryController = getEntryController(sessionId);

            savedEntry = entryController.save(updateEntry(sessionId, plasmid));

            log("User '" + entryController.getAccount().getEmail() + "' update plasmid: '"
                    + savedEntry.getRecordId() + "', " + savedEntry.getId());
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (PermissionException e) {
            throw new ServicePermissionException("No permissions to save this entry!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return (Plasmid) savedEntry;
    }

    public Strain updateStrain(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "strain") Strain strain) throws SessionException, ServiceException,
            ServicePermissionException {
        Entry savedEntry = null;

        try {
            EntryController entryController = getEntryController(sessionId);

            savedEntry = entryController.save(updateEntry(sessionId, strain));

            log("User '" + entryController.getAccount().getEmail() + "' update strain: '"
                    + savedEntry.getRecordId() + "', " + savedEntry.getId());
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (PermissionException e) {
            throw new ServicePermissionException("No permissions to save this entry!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return (Strain) savedEntry;
    }

    public Part updatePart(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "part") Part part) throws SessionException, ServiceException,
            ServicePermissionException {
        Entry savedEntry = null;

        try {
            EntryController entryController = getEntryController(sessionId);

            savedEntry = entryController.save(updateEntry(sessionId, part));

            log("User '" + entryController.getAccount().getEmail() + "' update part: '"
                    + savedEntry.getRecordId() + "', " + savedEntry.getId());
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (PermissionException e) {
            throw new ServicePermissionException("No permissions to save this entry!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return (Part) savedEntry;
    }

    protected Entry createEntry(String sessionId, Entry entry) throws SessionException,
            ServiceException {
        if (entry == null) {
            throw new ServiceException("Failed to create null Entry!");
        }

        // Validate recordType
        if (entry instanceof Plasmid) {
            entry.setRecordType("plasmid");
        } else if (entry instanceof Strain) {
            entry.setRecordType("strain");
        } else if (entry instanceof Part) {
            entry.setRecordType("part");
        } else {
            throw new ServiceException(
                    "Invalid entry class! Accepted entries with classes Plasmid, Strain and Part.");
        }

        // Validate creator
        if (entry.getCreator() == null || entry.getCreator().isEmpty()) {
            throw new ServiceException("Creator is mandatory field!");
        }

        // Validate owner and ownerEmail
        if (entry.getOwner() == null || entry.getOwner().isEmpty() || entry.getOwnerEmail() == null
                || entry.getOwnerEmail().isEmpty()) {
            throw new ServiceException("Owner and OwnerEmail are mandatory fields!");
        }

        // Validate short description
        if (entry.getShortDescription() == null || entry.getShortDescription().isEmpty()) {
            throw new ServiceException("Short Description is mandatory field!");
        }

        // Validate status
        if (entry.getStatus() == null) {
            throw new ServiceException(
                    "Invalid status! Expected type: 'complete', 'in progress' or 'planned'.");
        } else if (!entry.getStatus().equals("complete")
                && !entry.getStatus().equals("in progress") && !entry.getStatus().equals("planned")) {
            throw new ServiceException(
                    "Invalid status! Expected type: 'complete', 'in progress' or 'planned'.");
        }

        // Validate bioSafetyLevel
        if (entry.getBioSafetyLevel() != 1 && entry.getBioSafetyLevel() != 2) {
            throw new ServiceException("Invalid bio safety level! Expected: '1' or '2'");
        }

        // Validate name
        if (entry.getNames() == null || entry.getNames().size() == 0) {
            throw new ServiceException("Name is mandatory! Expected at least one name.");
        } else {
            for (Name name : entry.getNames()) {
                if (name.getName() == null || name.getName().isEmpty()) {
                    throw new ServiceException("Name can't be null or empty!");
                }

                name.setEntry(entry);
            }
        }

        // Validate selection markers
        if (entry.getSelectionMarkers() != null && entry.getSelectionMarkers().size() > 0) {
            for (SelectionMarker selectionMarker : entry.getSelectionMarkers()) {
                if (selectionMarker.getName() == null || selectionMarker.getName().isEmpty()) {
                    throw new ServiceException("Selection Marker can't be null or empty!");
                }

                selectionMarker.setEntry(entry);
            }
        }

        // Validate links
        if (entry.getLinks() != null && entry.getLinks().size() > 0) {
            for (Link link : entry.getLinks()) {
                if (link.getLink() == null || link.getLink().isEmpty()) {
                    throw new ServiceException("Link can't be null or empty!");
                }

                link.setEntry(entry);
            }
        }

        // Validate entry funding sources
        if (entry.getEntryFundingSources() == null || entry.getEntryFundingSources().size() == 0) {
            throw new ServiceException(
                    "FundingSource is mandatory! Expected at least one FundingSource.");
        } else {
            for (EntryFundingSource entryFundingSource : entry.getEntryFundingSources()) {
                if (entryFundingSource.getFundingSource() == null) {
                    throw new ServiceException("FundingSource can't be null!");
                }

                if (entryFundingSource.getFundingSource().getFundingSource() == null
                        || entryFundingSource.getFundingSource().getFundingSource().isEmpty()) {
                    throw new ServiceException("FundingSource can't be null or empty!");
                }

                if (entryFundingSource.getFundingSource().getPrincipalInvestigator() == null
                        || entryFundingSource.getFundingSource().getPrincipalInvestigator()
                                .isEmpty()) {
                    throw new ServiceException("PrincipalInvestigator can't be null or empty!");
                }

                entryFundingSource.setEntry(entry);
            }
        }

        return entry;
    }

    protected Entry updateEntry(String sessionId, Entry entry) throws SessionException,
            ServiceException, ServicePermissionException {
        Entry currentEntry = null;

        try {
            EntryController entryController = getEntryController(sessionId);

            try {
                currentEntry = entryController.getByRecordId(entry.getRecordId());
            } catch (PermissionException e) {
                throw new ServicePermissionException("No permissions to read this entry!");
            }

            if (currentEntry == null) {
                throw new ServiceException("Invalid recordId for entry!");
            }

            if (!entryController.hasWritePermission(currentEntry)) {
                throw new ServicePermissionException("No permissions to change this entry!");
            }
        } catch (ControllerException e) {
            throw new ServiceException(e);
        }

        // Validate and set creator
        if (entry.getCreator() == null || entry.getCreator().isEmpty()) {
            throw new ServiceException("Creator is mandatory field!");
        } else {
            currentEntry.setCreator(entry.getCreator());
            currentEntry.setCreatorEmail(entry.getCreatorEmail());
        }

        // Validate and set owner
        if (entry.getOwner() == null || entry.getOwner().isEmpty()) {
            throw new ServiceException("Owner is mandatory field!");
        } else {
            currentEntry.setOwner(entry.getOwner());
        }

        // Validate and set ownerEmail
        if (entry.getOwnerEmail() == null || entry.getOwnerEmail().isEmpty()) {
            throw new ServiceException("OwnerEmail is mandatory field!");
        } else {
            currentEntry.setOwnerEmail(entry.getOwnerEmail());
        }

        // Validate and set short description
        if (entry.getShortDescription() == null || entry.getShortDescription().isEmpty()) {
            throw new ServiceException("Short Description is mandatory field!");
        } else {
            currentEntry.setShortDescription(entry.getShortDescription());
        }

        // Validate status
        if (entry.getStatus() == null) {
            throw new ServiceException(
                    "Invalid status! Expected type: 'complete', 'in progress' or 'planned'.");
        } else if (!entry.getStatus().equals("complete")
                && !entry.getStatus().equals("in progress") && !entry.getStatus().equals("planned")) {
            throw new ServiceException(
                    "Invalid status! Expected type: 'complete', 'in progress' or 'planned'.");
        } else {
            currentEntry.setStatus(entry.getStatus());
        }

        // Validate bioSafetyLevel
        if (entry.getBioSafetyLevel() != 1 && entry.getBioSafetyLevel() != 2) {
            throw new ServiceException("Invalid bio safety level! Expected: '1' or '2'");
        } else {
            currentEntry.setBioSafetyLevel(entry.getBioSafetyLevel());
        }

        currentEntry.setAlias(entry.getAlias());
        currentEntry.setKeywords(entry.getKeywords());
        currentEntry.setLongDescription(entry.getLongDescription());
        currentEntry.setReferences(entry.getReferences());
        currentEntry.setIntellectualProperty(entry.getIntellectualProperty());

        if (entry instanceof Plasmid) {
            ((Plasmid) currentEntry).setBackbone(((Plasmid) entry).getBackbone());
            ((Plasmid) currentEntry).setCircular(((Plasmid) entry).getCircular());
            ((Plasmid) currentEntry).setOriginOfReplication(((Plasmid) entry)
                    .getOriginOfReplication());
            ((Plasmid) currentEntry).setPromoters(((Plasmid) entry).getPromoters());
        } else if (entry instanceof Strain) {
            ((Strain) currentEntry).setHost(((Strain) entry).getHost());
            ((Strain) currentEntry).setPlasmids(((Strain) entry).getPlasmids());
            ((Strain) currentEntry).setGenotypePhenotype(((Strain) entry).getGenotypePhenotype());
        } else if (entry instanceof Part) {
            ((Part) currentEntry).setPackageFormat(((Part) entry).getPackageFormat());
        }

        // Validate and set name
        if (entry.getNames() == null || entry.getNames().size() == 0) {
            throw new ServiceException("Name is mandatory! Expected at least one name.");
        } else {
            for (Name name : entry.getNames()) {
                if (name.getName() == null || name.getName().isEmpty()) {
                    throw new ServiceException("Name can't be null or empty!");
                }

                boolean existName = false;
                for (Name currentEntryName : currentEntry.getNames()) {
                    if (currentEntryName.getName().equals(name.getName())) {
                        existName = true;

                        break;
                    }
                }

                if (!existName) {
                    name.setEntry(currentEntry);

                    currentEntry.getNames().add(name);
                }
            }
        }

        // Validate and set selection markers
        if (entry.getSelectionMarkers() != null && entry.getSelectionMarkers().size() > 0) {
            for (SelectionMarker selectionMarker : entry.getSelectionMarkers()) {
                if (selectionMarker.getName() == null || selectionMarker.getName().isEmpty()) {
                    throw new ServiceException("Selection Marker can't be null or empty!");
                }

                boolean existSelectionMarker = false;
                for (SelectionMarker currentEntrySelectionMarker : currentEntry
                        .getSelectionMarkers()) {
                    if (currentEntrySelectionMarker.getName().equals(selectionMarker.getName())) {
                        existSelectionMarker = true;

                        break;
                    }
                }

                if (!existSelectionMarker) {
                    selectionMarker.setEntry(currentEntry);

                    currentEntry.getSelectionMarkers().add(selectionMarker);
                }
            }
        } else {
            currentEntry.setSelectionMarkers(null);
        }

        if (entry.getLinks() != null && entry.getLinks().size() > 0) {
            for (Link link : entry.getLinks()) {
                if (link.getLink() == null || link.getLink().isEmpty()) {
                    throw new ServiceException("Link can't be null or empty!");
                }

                boolean existLink = false;
                for (Link currentEntryLink : currentEntry.getLinks()) {
                    if (currentEntryLink.getUrl().equals(link.getUrl())
                            && currentEntryLink.getLink().equals(link.getLink())) {
                        existLink = true;

                        break;
                    }
                }

                if (!existLink) {
                    link.setEntry(currentEntry);

                    currentEntry.getLinks().add(link);
                }
            }
        } else {
            currentEntry.setLinks(null);
        }

        // Validate and set entry funding sources
        if (entry.getEntryFundingSources() == null || entry.getEntryFundingSources().size() == 0) {
            throw new ServiceException(
                    "FundingSource is mandatory! Expected at least one FundingSource.");
        } else {
            for (EntryFundingSource entryFundingSource : entry.getEntryFundingSources()) {
                if (entryFundingSource.getFundingSource() == null) {
                    throw new ServiceException("FundingSource can't be null!");
                }

                if (entryFundingSource.getFundingSource().getFundingSource() == null
                        || entryFundingSource.getFundingSource().getFundingSource().isEmpty()) {
                    throw new ServiceException("FundingSource can't be null or empty!");
                }

                if (entryFundingSource.getFundingSource().getPrincipalInvestigator() == null
                        || entryFundingSource.getFundingSource().getPrincipalInvestigator()
                                .isEmpty()) {
                    throw new ServiceException("PrincipalInvestigator can't be null or empty!");
                }

                boolean existEntryFundingSource = false;
                for (EntryFundingSource currentEntryEntryFundingSource : currentEntry
                        .getEntryFundingSources()) {

                    if (currentEntryEntryFundingSource.getFundingSource().getFundingSource()
                            .equals(entryFundingSource.getFundingSource().getFundingSource())
                            && currentEntryEntryFundingSource.getFundingSource()
                                    .getPrincipalInvestigator().equals(
                                        entryFundingSource.getFundingSource()
                                                .getPrincipalInvestigator())) {
                        existEntryFundingSource = true;

                        break;
                    }

                }

                if (!existEntryFundingSource) {
                    entryFundingSource.setEntry(currentEntry);

                    currentEntry.getEntryFundingSources().add(entryFundingSource);
                }

                entryFundingSource.setEntry(entry);
            }
        }

        return currentEntry;
    }

    public void removeEntry(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId) throws SessionException, ServiceException,
            ServicePermissionException {
        try {
            EntryController entryController = getEntryController(sessionId);

            Entry entry = entryController.getByRecordId(entryId);

            entryController.delete(entry);

            log("User '" + entryController.getAccount().getEmail() + "' removed entry: '" + entryId
                    + "'");
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (PermissionException e) {
            throw new ServicePermissionException("No permissions to delete this entry!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }
    }

    public FeaturedDNASequence getSequence(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId) throws SessionException, ServiceException,
            ServicePermissionException {

        FeaturedDNASequence sequence = null;

        try {
            SequenceController sequenceController = getSequenceController(sessionId);
            EntryController entryController = getEntryController(sessionId);

            Entry entry = entryController.getByRecordId(entryId);

            sequence = sequenceController.sequenceToDNASequence(sequenceController
                    .getByEntry(entry));

            log("User '" + entryController.getAccount().getEmail() + "' pulled sequence: '"
                    + entryId + "'");
        } catch (PermissionException e) {
            throw new ServicePermissionException("No permission to read this entry");
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return sequence;
    }

    public FeaturedDNASequence createSequence(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId,
            @WebParam(name = "sequence") FeaturedDNASequence featuredDNASequence)
            throws SessionException, ServiceException, ServicePermissionException {

        Entry entry = null;
        FeaturedDNASequence savedFeaturedDNASequence = null;

        try {
            EntryController entryController = getEntryController(sessionId);
            SequenceController sequenceController = getSequenceController(sessionId);

            try {
                entry = entryController.getByRecordId(entryId);
            } catch (PermissionException e) {
                throw new ServicePermissionException("No permissions to read this entry!");
            }

            if (entry == null) {
                throw new ServiceException("Entry doesn't exist!");
            }

            if (entryController.hasSequence(entry)) {
                throw new ServiceException(
                        "Entry has sequence already assigned. Remove it first and then create new one.");
            }

            Sequence sequence = sequenceController.dnaSequenceToSequence(featuredDNASequence);

            sequence.setEntry(entry);

            try {
                savedFeaturedDNASequence = sequenceController
                        .sequenceToDNASequence(sequenceController.save(sequence));

                log("User '" + entryController.getAccount().getEmail() + "' saved sequence: '"
                        + entryId + "'");
            } catch (PermissionException e) {
                throw new ServicePermissionException("No permissions to save this sequence!");
            }
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return savedFeaturedDNASequence;
    }

    @WebMethod(exclude = true)
    public FeaturedDNASequence updateSequence(FeaturedDNASequence sequence) {
        throw new NotImplementedException(
                "this method not implemented on purpose; remove and create new one");
    }

    public void removeSequence(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId) throws SessionException, ServiceException,
            ServicePermissionException {
        try {
            EntryController entryController = getEntryController(sessionId);

            Entry entry = null;

            try {
                entry = entryController.getByRecordId(entryId);
            } catch (PermissionException e) {
                throw new ServicePermissionException("No permission to read this entry");
            }

            SequenceController sequenceController = getSequenceController(sessionId);

            Sequence sequence = sequenceController.getByEntry(entry);

            if (sequence != null) {
                try {
                    sequenceController.delete(sequence);

                    log("User '" + entryController.getAccount().getEmail()
                            + "' removed sequence: '" + entryId + "'");
                } catch (PermissionException e) {
                    throw new ServicePermissionException("No permission to delete sequence");
                }
            }
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }
    }

    public FeaturedDNASequence uploadSequence(@WebParam(name = "sessionId") String sessionId,
            @WebParam(name = "entryId") String entryId, @WebParam(name = "sequence") String sequence)
            throws SessionException, ServiceException, ServicePermissionException {
        EntryController entryController = getEntryController(sessionId);
        SequenceController sequenceController = getSequenceController(sessionId);

        FeaturedDNASequence dnaSequence = (FeaturedDNASequence) sequenceController.parse(sequence);

        if (dnaSequence == null) {
            throw new ServiceException("Couldn't parse sequence file! Supported formats: "
                    + GeneralParser.getInstance().availableParsersToString()
                    + ".\nIf you are using ApE, try opening and re-saving using a recent version.");
        }

        Entry entry = null;

        FeaturedDNASequence savedFeaturedDNASequence = null;
        Sequence modelSequence = null;
        try {
            try {
                entry = entryController.getByRecordId(entryId);

                if (entryController.hasSequence(entry)) {
                    throw new ServiceException(
                            "Entry has sequence already assigned. Remove it first and then upload new one.");
                }
            } catch (PermissionException e) {
                throw new ServicePermissionException("No permissions to read entry!", e);
            }

            try {
                modelSequence = sequenceController.dnaSequenceToSequence(dnaSequence);

                modelSequence.setEntry(entry);

                Sequence savedSequence = sequenceController.save(modelSequence);

                savedFeaturedDNASequence = sequenceController.sequenceToDNASequence(savedSequence);

                log("User '" + entryController.getAccount().getEmail()
                        + "' uploaded new sequence: '" + entryId + "'");
            } catch (PermissionException e) {
                throw new ServicePermissionException("No permissions to save sequence to entry!", e);
            }
        } catch (ControllerException e) {
            throw new ViewException(e);
        } catch (Exception e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        return savedFeaturedDNASequence;
    }

    protected EntryController getEntryController(@WebParam(name = "sessionId") String sessionId)
            throws SessionException, ServiceException {
        Account account = validateAccount(sessionId);

        return new EntryController(account);
    }

    protected SequenceController getSequenceController(
            @WebParam(name = "sessionId") String sessionId) throws ServiceException,
            SessionException {
        Account account = validateAccount(sessionId);

        return new SequenceController(account);
    }

    protected SearchController getSearchController(@WebParam(name = "sessionId") String sessionId)
            throws SessionException, ServiceException {
        return new SearchController(validateAccount(sessionId));
    }

    protected Account validateAccount(@WebParam(name = "sessionId") String sessionId)
            throws ServiceException, SessionException {
        if (!isAuthenticated(sessionId)) {
            throw new SessionException("Not uauthorized access! Autorize first!");
        }

        Account account = null;

        try {
            account = AccountController.getAccountBySessionKey(sessionId);
        } catch (ControllerException e) {
            Logger.error(e);

            throw new ServiceException("Registry Service Internal Error!");
        }

        if (account == null) {
            Logger.error("Failed to lookup account!");

            throw new ServiceException("Registry Service Internal Error!");
        }

        return account;
    }

    private void log(String message) {
        Logger.info("RegistryAPI: " + message);
    }
}
