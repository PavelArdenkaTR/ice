package org.jbei.ice.lib.entry.sample;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jbei.ice.ControllerException;
import org.jbei.ice.lib.account.AccountType;
import org.jbei.ice.lib.account.model.Account;
import org.jbei.ice.lib.common.logging.Logger;
import org.jbei.ice.lib.dao.DAOException;
import org.jbei.ice.lib.dao.DAOFactory;
import org.jbei.ice.lib.dao.hibernate.RequestDAO;
import org.jbei.ice.lib.dto.ConfigurationKey;
import org.jbei.ice.lib.dto.sample.SampleRequest;
import org.jbei.ice.lib.dto.sample.SampleRequestStatus;
import org.jbei.ice.lib.dto.sample.SampleRequestType;
import org.jbei.ice.lib.entry.EntryController;
import org.jbei.ice.lib.entry.model.Entry;
import org.jbei.ice.lib.entry.sample.model.Request;
import org.jbei.ice.lib.utils.Emailer;
import org.jbei.ice.lib.utils.Utils;

/**
 * Handler for sample requests
 *
 * @author Hector Plahar
 */
public class SampleRequests {

    private final RequestDAO dao;

    public SampleRequests() {
        this.dao = DAOFactory.getRequestDAO();
    }

    /**
     * Creates a new sample request for the specified user and specified entry.
     * The default status is "IN CART"
     *
     * @param account user account
     * @param entryID unique identifier for the entry
     * @param type    type of sample request
     */
    public SampleRequest placeSampleInCart(Account account, long entryID, SampleRequestType type) {
        Entry entry;
        try {
            entry = new EntryController().get(account, entryID);
        } catch (ControllerException e) {
            Logger.error(e);
            return null;
        }

        if (entry == null)
            throw new IllegalArgumentException("Cannot find entry with id: " + entryID);

        // check if sample is already in cart with status of "IN CART"
        try {
            ArrayList<Request> requests = dao.getSampleRequestByStatus(account, entry, SampleRequestStatus.IN_CART);
            if (requests != null && !requests.isEmpty())
                return null;

            Request request = new Request();
            request.setAccount(account);
            request.setEntry(entry);
            request.setType(type);
            request.setRequested(new Date(System.currentTimeMillis()));
            request.setUpdated(request.getRequested());

            request = dao.create(request);
            return request.toDataTransferObject();
        } catch (DAOException e) {
            Logger.error(e);
        }
        return null;
    }

    public ArrayList<SampleRequest> getSampleRequestsInCart(Account account) {
        if (account == null)
            return null;

        try {
            List<Request> requestList = dao.getRequestListInCart(account);
            if (requestList == null)
                return null;

            ArrayList<SampleRequest> requests = new ArrayList<>();
            for (Request request : requestList)
                requests.add(request.toDataTransferObject());
            return requests;
        } catch (DAOException e) {
            return null;
        }
    }

    public ArrayList<SampleRequest> getPendingRequests(Account account) {
        List<Request> requests;
        try {
            if (account.getType() == AccountType.ADMIN) {
                requests = dao.getAllRequestList();
            } else
                requests = dao.getAccountRequestList(account, 0, 100, "requested", true);
        } catch (DAOException de) {
            return null;
        }

        ArrayList<SampleRequest> results = new ArrayList<>();
        for (Request request : requests) {
            results.add(request.toDataTransferObject());
        }
        return results;
    }

    public SampleRequest removeSampleFromCart(Account account, long entryId) {
        if (account == null)
            return null;

        Entry entry;
        try {
            entry = new EntryController().get(account, entryId);
        } catch (ControllerException e) {
            Logger.error(e);
            return null;
        }

        if (entry == null)
            throw new IllegalArgumentException("Cannot find entry with id: " + entryId);

        try {
            Request request = dao.getSampleRequestInCart(account, entry);
            if (request == null)
                return null;

            Logger.info(account.getEmail() + ": Removing sample from cart for entry " + entryId);
            dao.delete(request);

            return request.toDataTransferObject();
        } catch (DAOException de) {
            Logger.error(de);
            return null;
        }
    }

    public SampleRequest updateRequest(Account account, SampleRequest request) {
        if (account == null)
            return null;

        try {
            Request existing = dao.get(request.getId());
            if (existing == null)
                return null;

            // todo this should handle other parameters; for now only using it for status
            if (existing.getStatus() == request.getStatus())
                return request;

            existing.setStatus(request.getStatus());
            existing = dao.update(existing);
            return existing.toDataTransferObject();
        } catch (DAOException de) {
            return null;
        }
    }

    public boolean request(Account account, ArrayList<SampleRequest> requests) {
        if (account == null || requests == null)
            return false;

        try {
            for (SampleRequest sampleRequest : requests) {
                Request request = dao.get(sampleRequest.getId());
                if (request == null || (account.getType() != AccountType.ADMIN && request.getAccount() != account)) {
                    Logger.error("Cannot find sample request " + sampleRequest.getId() + " or accounts do not match");
                    return false;
                }

                request.setStatus(SampleRequestStatus.PENDING);
                dao.update(request);
            }

            // send email to strain archivist
            String email = Utils.getConfigValue(ConfigurationKey.BULK_UPLOAD_APPROVER_EMAIL);
            if (email != null && !email.isEmpty()) {
                String subject = "Sample request";
                String body = "A sample request has been received from" + account.getFullName() + " for "
                        + requests.size() + " samples.\n\n";
                body += "Please go to the following link to review pending requests.\n\n";
                body += Utils.getConfigValue(ConfigurationKey.URI_PREFIX) + "/#page=admin;id=sample_requests";
                Emailer.send(email, subject, body);
            }
        } catch (DAOException de) {
            Logger.error("Could not fulfil request", de);
            return false;
        }
        return true;
    }
}