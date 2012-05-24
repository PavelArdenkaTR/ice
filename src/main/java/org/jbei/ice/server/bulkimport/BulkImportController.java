package org.jbei.ice.server.bulkimport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.jbei.ice.controllers.common.Controller;
import org.jbei.ice.controllers.common.ControllerException;
import org.jbei.ice.controllers.permissionVerifiers.EntryPermissionVerifier;
import org.jbei.ice.lib.logging.Logger;
import org.jbei.ice.lib.managers.AccountManager;
import org.jbei.ice.lib.managers.BulkImportManager;
import org.jbei.ice.lib.managers.ManagerException;
import org.jbei.ice.lib.models.Account;
import org.jbei.ice.lib.models.BulkImport;
import org.jbei.ice.lib.models.Entry;
import org.jbei.ice.lib.utils.BulkImportEntryData;
import org.jbei.ice.lib.utils.JbeirSettings;
import org.jbei.ice.server.InfoToModelFactory;
import org.jbei.ice.shared.EntryAddType;
import org.jbei.ice.shared.dto.AttachmentInfo;
import org.jbei.ice.shared.dto.EntryInfo;
import org.jbei.ice.shared.dto.SequenceAnalysisInfo;

public class BulkImportController extends Controller {

    private final String TEMPORARY_DIRECTORY = JbeirSettings.getSetting("TEMPORARY_DIRECTORY");

    public BulkImportController(Account account) {
        super(account, new EntryPermissionVerifier());
    }

    public BulkImport updateBulkImportDraft(long id, String name, Account account,
            ArrayList<EntryInfo> primary, ArrayList<EntryInfo> secondary, String email)
            throws ControllerException {

        try {
            BulkImport savedDraft = BulkImportManager.retrieveById(id);
            // callee should consider creating a new record and updating 
            if (savedDraft == null)
                throw new ControllerException("Could not located bulk import record with id " + id);

            savedDraft.setName(name);
            getDataForUpdate(savedDraft, primary, account, true);
            getDataForUpdate(savedDraft, secondary, account, false);

            BulkImport result = BulkImportManager.updateBulkImportRecord(id, savedDraft);
            return result;
        } catch (ManagerException me) {
            throw new ControllerException(me);
        }
    }

    private void getDataForUpdate(BulkImport bulkImport, ArrayList<EntryInfo> infoList,
            Account account, boolean isPrimary) {

        if (infoList == null || infoList.isEmpty())
            return;

        ArrayList<BulkImportEntryData> dataList = new ArrayList<BulkImportEntryData>(
                infoList.size());

        HashMap<String, File> attachmentFiles = new HashMap<String, File>();
        HashMap<String, File> sequenceFiles = new HashMap<String, File>();
        EntryAddType type = null;

        // primary data applies for single entry bulk
        for (EntryInfo info : infoList) {
            BulkImportEntryData data = new BulkImportEntryData();

            // convert dto to entry model
            Entry entry = InfoToModelFactory.infoToEntry(info, null);
            entry.setOwnerEmail(account.getEmail());
            entry.setOwner(account.getFullName());
            data.setEntry(entry);

            // deal with attachment files
            if (info.getAttachments() != null && !info.getAttachments().isEmpty()) {

                AttachmentInfo attachmentInfo = info.getAttachments().get(0); // only one attachment per bulk import entry
                String fileId = attachmentInfo.getFileId();
                String fileName = attachmentInfo.getFilename();

                if (fileId != null) {
                    File file = new File(TEMPORARY_DIRECTORY + File.separator + fileId);
                    if (file.exists()) {
                        attachmentFiles.put(fileName, file);
                    }
                } else {
                    // existing file
                    // TODO : we are assuming that the user did not update the file. It could be that it was updated but with the same name
                    // TODO : we need to validate this against the list of saved files 
                    data.setAttachmentFilename(fileName);
                }
            }

            // deal with sequence files
            if (info.getSequenceAnalysis() != null && !info.getSequenceAnalysis().isEmpty()) {

                SequenceAnalysisInfo sequenceInfo = info.getSequenceAnalysis().get(0);
                String fileId = sequenceInfo.getFileId();
                String fileName = sequenceInfo.getName();

                if (fileId != null) {
                    File file = new File(TEMPORARY_DIRECTORY + File.separator + fileId);
                    if (file.exists())
                        sequenceFiles.put(fileName, file);
                } else {
                    // TODO : ditto for existing files
                    data.setSequenceFilename(fileName);
                }
            }

            // type 
            type = EntryAddType.valueOf(info.getType().name());
            dataList.add(data);
        }

        bulkImport.setType(type.toString());
        if (isPrimary)
            bulkImport.setPrimaryData(dataList);
        else
            bulkImport.setSecondaryData(dataList);

        // update bulk import files
        if (!attachmentFiles.isEmpty()) {
            Byte[] newBytes = updateBulkImportFiles(attachmentFiles, bulkImport.getAttachmentFile());
            bulkImport.setAttachmentFile(newBytes);
        }

        // update sequence files
        if (!sequenceFiles.isEmpty()) {
            Byte[] newBytes = updateBulkImportFiles(sequenceFiles, bulkImport.getSequenceFile());
            bulkImport.setSequenceFile(newBytes);
        }
    }

    private Byte[] updateBulkImportFiles(HashMap<String, File> files, Byte[] zipFileBytes) {
        try {
            byte[] existingBytes = ArrayUtils.toPrimitive(zipFileBytes);
            byte[] newBytes;

            if (existingBytes == null) {
                newBytes = createZip(files);
            } else {
                newBytes = createZip(files, existingBytes);
            }

            return ArrayUtils.toObject(newBytes);
        } catch (IOException e) {
            Logger.error(e);
            return null;
        }
    }

    /**
     * Creates a new bulk import record from the given parameters
     * 
     * @param account
     * @param primary
     * @param secondary
     * @param email
     * @return
     */
    public BulkImport createBulkImport(Account account, ArrayList<EntryInfo> primary,
            ArrayList<EntryInfo> secondary, String email) {

        ArrayList<BulkImportEntryData> primaryDataList = new ArrayList<BulkImportEntryData>(
                primary.size());

        EntryAddType type = null;
        HashMap<String, File> attachmentFiles = new HashMap<String, File>();
        HashMap<String, File> sequenceFiles = new HashMap<String, File>();
        BulkImport bulkImport = new BulkImport();

        // primary data applies for single entry bulk
        for (EntryInfo info : primary) {
            BulkImportEntryData data = new BulkImportEntryData();

            // convert dto to entry model
            Entry entry = InfoToModelFactory.infoToEntry(info, null);
            entry.setOwnerEmail(account.getEmail());
            entry.setOwner(account.getFullName());
            data.setEntry(entry);

            // deal with files
            if (info.getAttachments() != null && !info.getAttachments().isEmpty()) {
                // deal with attachment files
                AttachmentInfo attachmentInfo = info.getAttachments().get(0);
                File file = new File(TEMPORARY_DIRECTORY + File.separator
                        + attachmentInfo.getFileId());
                if (file.exists()) {
                    attachmentFiles.put(attachmentInfo.getFilename(), file);
                    data.setAttachmentFilename(attachmentInfo.getFilename());
                }
            }

            if (info.getSequenceAnalysis() != null && !info.getSequenceAnalysis().isEmpty()) {
                // deal with sequence files
                SequenceAnalysisInfo sequenceInfo = info.getSequenceAnalysis().get(0);
                File file = new File(TEMPORARY_DIRECTORY + File.separator
                        + sequenceInfo.getFileId());
                if (file.exists()) {
                    sequenceFiles.put(sequenceInfo.getName(), file);
                    data.setSequenceFilename(sequenceInfo.getName());
                }
            }

            // type 
            type = EntryAddType.valueOf(info.getType().name());
            primaryDataList.add(data);
        }

        // save primary data
        bulkImport.setPrimaryData(primaryDataList);

        // secondary data
        ArrayList<BulkImportEntryData> secondaryDataList = new ArrayList<BulkImportEntryData>(
                secondary.size());

        if (secondary != null && !secondary.isEmpty()) {
            for (EntryInfo info : secondary) {
                BulkImportEntryData data = new BulkImportEntryData();

                Entry entry = InfoToModelFactory.infoToEntry(info, null);
                entry.setOwnerEmail(account.getEmail());
                entry.setOwner(account.getFullName());
                data.setEntry(entry);

                // deal with files
                if (!info.getAttachments().isEmpty()) {
                    // deal with attachment files
                    AttachmentInfo attachmentInfo = info.getAttachments().get(0);
                    File file = new File(TEMPORARY_DIRECTORY + File.separator
                            + attachmentInfo.getFileId());
                    if (file.exists())
                        attachmentFiles.put(attachmentInfo.getFilename(), file);
                }

                if (!info.getSequenceAnalysis().isEmpty()) {
                    // deal with sequence files
                    SequenceAnalysisInfo sequenceInfo = info.getSequenceAnalysis().get(0);
                    File file = new File(TEMPORARY_DIRECTORY + File.separator
                            + sequenceInfo.getFileId());
                    if (file.exists())
                        sequenceFiles.put(sequenceInfo.getName(), file);
                }

                secondaryDataList.add(data);
            }
            bulkImport.setSecondaryData(secondaryDataList);
        }

        // set primary data and attachments and sequence files if any
        if (!attachmentFiles.isEmpty()) {
            try {
                byte[] bytes = createZip(attachmentFiles);
                bulkImport.setAttachmentFile(ArrayUtils.toObject(bytes));
            } catch (IOException ioe) {
                Logger.error(ioe);
            }
        }

        if (!sequenceFiles.isEmpty()) {
            try {
                byte[] bytes = createZip(sequenceFiles);
                bulkImport.setSequenceFile(ArrayUtils.toObject(bytes));
            } catch (IOException ioe) {
                Logger.error(ioe);
            }
        }

        bulkImport.setType(type.toString());
        Account emailAccount;
        try {
            emailAccount = AccountManager.getByEmail(email);
            bulkImport.setAccount(emailAccount);
        } catch (ManagerException e) {
            Logger.error(e);
            return null;
        }

        return bulkImport;
    }

    private byte[] createZip(HashMap<String, File> attachmentFiles, byte[] existingBytes)
            throws IOException {
        HashMap<String, File> files = new HashMap<String, File>();
        ByteArrayInputStream stream = new ByteArrayInputStream(existingBytes);
        ZipInputStream zipInput = new ZipInputStream(stream);
        ZipEntry zipEntry;

        while ((zipEntry = zipInput.getNextEntry()) != null) {

            String entryName = zipEntry.getName();
            File file = new File(TEMPORARY_DIRECTORY + File.separator + entryName);
            FileOutputStream outStream = new FileOutputStream(file);

            IOUtils.copyLarge(zipInput, outStream);

            outStream.close();
            zipInput.closeEntry();

            files.put(entryName, file);
        }
        zipInput.close();
        files.putAll(attachmentFiles);
        return createZip(files);
    }

    private static byte[] createZip(HashMap<String, File> files) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zipfile = new ZipOutputStream(bos);
        String fileName = null;
        ZipEntry zipentry = null;
        Iterator<String> iter = files.keySet().iterator();
        while (iter.hasNext()) {
            fileName = iter.next();
            zipentry = new ZipEntry(fileName);
            zipfile.putNextEntry(zipentry);
            File file = files.get(fileName);
            FileInputStream input = new FileInputStream(file);
            byte[] bytes = IOUtils.toByteArray(input);
            zipfile.write(bytes);
        }
        zipfile.close();
        return bos.toByteArray();
    }

    public static LinkedList<String> extractZip(byte[] array) throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(array);
        ZipInputStream zipInput = new ZipInputStream(stream);
        ZipEntry zipEntry;
        LinkedList<String> files = new LinkedList<String>();

        while ((zipEntry = zipInput.getNextEntry()) != null) {
            files.add(zipEntry.getName());
        }
        zipInput.close();
        return files;
    }

}