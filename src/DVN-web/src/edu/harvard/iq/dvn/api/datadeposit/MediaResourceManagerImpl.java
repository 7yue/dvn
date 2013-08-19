/*
 Copyright (C) 2005-2012, by the President and Fellows of Harvard College.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 Dataverse Network - A web application to share, preserve and analyze research data.
 Developed at the Institute for Quantitative Social Science, Harvard University.
 Version 3.0.
 */
package edu.harvard.iq.dvn.api.datadeposit;

import edu.harvard.iq.dvn.core.admin.VDCUser;
import edu.harvard.iq.dvn.core.study.EditStudyFilesService;
import edu.harvard.iq.dvn.core.study.EditStudyService;
import edu.harvard.iq.dvn.core.study.Study;
import edu.harvard.iq.dvn.core.study.StudyFile;
import edu.harvard.iq.dvn.core.study.StudyFileEditBean;
import edu.harvard.iq.dvn.core.study.StudyFileServiceLocal;
import edu.harvard.iq.dvn.core.study.StudyServiceLocal;
import edu.harvard.iq.dvn.core.util.FileUtil;
import edu.harvard.iq.dvn.core.vdc.VDC;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.MediaResource;
import org.swordapp.server.MediaResourceManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

@EJB(name = "editStudyFiles", beanInterface = edu.harvard.iq.dvn.core.study.EditStudyFilesService.class)
public class MediaResourceManagerImpl implements MediaResourceManager {

    @EJB
    StudyServiceLocal studyService;
    @EJB
    StudyFileServiceLocal studyFileService;
    @Inject
    SwordAuth swordAuth;
    @Inject
    UrlManager urlManager;
    private static final Logger logger = Logger.getLogger(MediaResourceManagerImpl.class.getCanonicalName());

    @Override
    public MediaResource getMediaResourceRepresentation(String uri, Map<String, String> map, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {

        VDCUser vdcUser = swordAuth.auth(authCredentials);
        urlManager.processUrl(uri);
        String globalId = urlManager.getTargetIdentifier();
        if (urlManager.getTargetType().equals("study") && globalId != null) {
            EditStudyService editStudyService;
            Context ctx;
            try {
                ctx = new InitialContext();
                editStudyService = (EditStudyService) ctx.lookup("java:comp/env/editStudy");
            } catch (NamingException ex) {
                throw new SwordServerException("problem looking up editStudyService");
            }
            logger.info("looking up study with globalId " + globalId);
            Study study = editStudyService.getStudyByGlobalId(globalId);
            if (study != null) {
                /**
                 * @todo: support this
                 */
                boolean getMediaResourceRepresentationSupported = false;
                if (getMediaResourceRepresentationSupported) {
                    VDC dvThatOwnsStudy = study.getOwner();
                    if (swordAuth.hasAccessToModifyDataverse(vdcUser, dvThatOwnsStudy)) {
                        InputStream fixmeInputStream = new ByteArrayInputStream("FIXME: replace with zip of all study files".getBytes());
                        String contentType = "application/zip";
                        String packaging = UriRegistry.PACKAGE_SIMPLE_ZIP;
                        boolean isPackaged = true;
                        MediaResource mediaResource = new MediaResource(fixmeInputStream, contentType, packaging, isPackaged);
                        return mediaResource;
                    } else {
                        throw new SwordError("user " + vdcUser.getUserName() + " is not authorized to get a media resource representation of the study with global ID " + study.getGlobalId());
                    }
                } else {
                    throw new SwordError("Please use the Dataverse Network Data Sharing API instead");
                }
            } else {
                throw new SwordError("couldn't find study with global ID of " + globalId);
            }
        } else {
            throw new SwordError("Couldn't dermine target type or identifier from URL: " + uri);
        }
    }

    @Override
    public DepositReceipt replaceMediaResource(String uri, Deposit deposit, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        /**
         * @todo: Perhaps create a new version of a study here?
         *
         * "The server MUST effectively replace all the existing content in the
         * item, although implementations may choose to provide versioning or
         * some other mechanism for retaining the overwritten content." --
         * http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html#protocoloperations_editingcontent_binary
         */
        throw new SwordError("Replacing the files of a study is not supported. Please delete and add files separately instead.");
    }

    @Override
    public void deleteMediaResource(String uri, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        VDCUser vdcUser = swordAuth.auth(authCredentials);
        urlManager.processUrl(uri);
        String targetType = urlManager.getTargetType();
        String fileId = urlManager.getTargetIdentifier();
        if (targetType != null && fileId != null) {
            if ("file".equals(targetType)) {
                String fileIdString = urlManager.getTargetIdentifier();
                if (fileIdString != null) {
                    Long fileIdLong;
                    try {
                        fileIdLong = Long.valueOf(fileIdString);
                    } catch (NumberFormatException ex) {
                        throw new SwordError("File id must be a number, not '" + fileIdString + "'. uri was: " + uri);
                    }
                    if (fileIdLong != null) {
                        logger.info("preparing to delete file id " + fileIdLong);
                        StudyFile fileToDelete;
                        try {
                            fileToDelete = studyFileService.getStudyFile(fileIdLong);
                        } catch (EJBException ex) {
                            throw new SwordError("Unable to find file id " + fileIdLong);
                        }
                        if (fileToDelete != null) {
                            String globalId = fileToDelete.getStudy().getGlobalId();
                            VDC dvThatOwnsFile = fileToDelete.getStudy().getOwner();
                            if (swordAuth.hasAccessToModifyDataverse(vdcUser, dvThatOwnsFile)) {
                                EditStudyFilesService editStudyFilesService;
                                try {
                                    Context ctx = new InitialContext();
                                    editStudyFilesService = (EditStudyFilesService) ctx.lookup("java:comp/env/editStudyFiles");
                                } catch (NamingException ex) {
                                    throw new SwordServerException("problem looking up editStudyFilesService");
                                }
                                editStudyFilesService.setStudyVersionByGlobalId(globalId);
                                // editStudyFilesService.findStudyFileEditBeanById() would be nice
                                List studyFileEditBeans = editStudyFilesService.getCurrentFiles();
                                for (Iterator it = studyFileEditBeans.iterator(); it.hasNext();) {
                                    StudyFileEditBean studyFileEditBean = (StudyFileEditBean) it.next();
                                    if (studyFileEditBean.getStudyFile().getId().equals(fileToDelete.getId())) {
                                        logger.info("marked for deletion: " + studyFileEditBean.getStudyFile().getFileName());
                                        studyFileEditBean.setDeleteFlag(true);
                                    } else {
                                        logger.info("not marked for deletion: " + studyFileEditBean.getStudyFile().getFileName());
                                    }
                                }
                                editStudyFilesService.save(dvThatOwnsFile.getId(), vdcUser.getId());
                            } else {
                                throw new SwordError("User " + vdcUser.getUserName() + " is not authorized to modify " + dvThatOwnsFile.getAlias());
                            }
                        } else {
                            throw new SwordError("Unable to find file id " + fileIdLong + " from url: " + uri);
                        }
                    } else {
                        throw new SwordError("Unable to find file id in url: " + uri);
                    }
                } else {
                    throw new SwordError("Could not file file to delete in url: " + uri);
                }
            } else {
                throw new SwordError("Unsupported file type found in url: " + uri);
            }
        } else {
            throw new SwordError("Target or identifer not specified in url: " + uri);
        }
    }

    @Override
    public DepositReceipt addResource(String uri, Deposit deposit, AuthCredentials authCredentials, SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        boolean shouldReplace = false;
        return replaceOrAddFiles(uri, deposit, authCredentials, swordConfiguration, shouldReplace);
    }

    DepositReceipt replaceOrAddFiles(String uri, Deposit deposit, AuthCredentials authCredentials, SwordConfiguration swordConfiguration, boolean shouldReplace) throws SwordError, SwordAuthException, SwordServerException {
        if (!deposit.getPackaging().equals(UriRegistry.PACKAGE_SIMPLE_ZIP)) {
            throw new SwordError(UriRegistry.ERROR_CONTENT, 415, "Package format " + UriRegistry.PACKAGE_SIMPLE_ZIP + " is required but format specified in 'Packaging' HTTP header was " + deposit.getPackaging());
        }

        VDCUser vdcUser = swordAuth.auth(authCredentials);

        urlManager.processUrl(uri);
        String globalId = urlManager.getTargetIdentifier();
        if (urlManager.getTargetType().equals("study") && globalId != null) {
            EditStudyService editStudyService;
            Context ctx;
            try {
                ctx = new InitialContext();
                editStudyService = (EditStudyService) ctx.lookup("java:comp/env/editStudy");
            } catch (NamingException ex) {
                throw new SwordServerException("problem looking up editStudyService");
            }
            logger.info("looking up study with globalId " + globalId);
            Study study = editStudyService.getStudyByGlobalId(globalId);
            Long studyId;
            try {
                studyId = study.getId();
            } catch (NullPointerException ex) {
                throw new SwordError("couldn't find study with global ID of " + globalId);
            }
            VDC dvThatOwnsStudy = study.getOwner();
            if (swordAuth.hasAccessToModifyDataverse(vdcUser, dvThatOwnsStudy)) {
                editStudyService.setStudyVersion(studyId);
                editStudyService.save(dvThatOwnsStudy.getId(), vdcUser.getId());

                EditStudyFilesService editStudyFilesService;
                try {
                    editStudyFilesService = (EditStudyFilesService) ctx.lookup("java:comp/env/editStudyFiles");
                } catch (NamingException ex) {
                    throw new SwordServerException("problem looking up editStudyFilesService");
                }
                editStudyFilesService.setStudyVersionByGlobalId(globalId);
                List studyFileEditBeans = editStudyFilesService.getCurrentFiles();
                List<String> exisitingFilenames = new ArrayList<String>();
                for (Iterator it = studyFileEditBeans.iterator(); it.hasNext();) {
                    StudyFileEditBean studyFileEditBean = (StudyFileEditBean) it.next();
                    if (shouldReplace) {
                        studyFileEditBean.setDeleteFlag(true);
                        logger.info("marked for deletion: " + studyFileEditBean.getStudyFile().getFileName());
                    } else {
                        String filename = studyFileEditBean.getStudyFile().getFileName();
                        exisitingFilenames.add(filename);
                    }
                }
                editStudyFilesService.save(dvThatOwnsStudy.getId(), vdcUser.getId());

                String tempDirectory = swordConfiguration.getTempDirectory();
                String uploadDirPath = tempDirectory + File.separator + "uploads" + File.separator + study.getId().toString();
                File uploadDir = new File(uploadDirPath);
                if (!uploadDir.exists()) {
                    if (!uploadDir.mkdirs()) {
                        throw new SwordServerException("couldn't create directory: " + uploadDir.getAbsolutePath());
                    }
                }
                String filename = uploadDirPath + File.separator + deposit.getFilename();
                /**
                 * @todo: fix known "first character is removed from filename"
                 * issue with sword2-server-1.0-classes.jar (SWORD v2 :: Common
                 * Server Library) described at
                 * https://github.com/swordapp/JavaServer2.0/pull/2
                 *
                 * The author of the jar has not published it (
                 * http://www.mail-archive.com/sword-app-tech@lists.sourceforge.net/msg00318.html
                 * ) so we build it ourselves from the "master" branch of the
                 * official repo: https://github.com/swordapp/JavaServer2.0
                 *
                 */
                logger.info("attempting write to " + filename);
                ZipInputStream ziStream = new ZipInputStream(deposit.getInputStream());
                ZipEntry zEntry;
                FileOutputStream tempOutStream = null;
                List<StudyFileEditBean> fbList = new ArrayList<StudyFileEditBean>();

                try {
                    // copied from createStudyFilesFromZip in AddFilesPage
                    while ((zEntry = ziStream.getNextEntry()) != null) {
                        // Note that some zip entries may be directories - we 
                        // simply skip them:
                        if (!zEntry.isDirectory()) {

                            String fileEntryName = zEntry.getName();
                            logger.info("file found: " + fileEntryName);

                            String dirName = null;
                            String finalFileName = null;

                            int ind = fileEntryName.lastIndexOf('/');

                            if (ind > -1) {
                                finalFileName = fileEntryName.substring(ind + 1);
                                if (ind > 0) {
                                    dirName = fileEntryName.substring(0, ind);
                                    dirName = dirName.replace('/', '-');
                                }
                            } else {
                                finalFileName = fileEntryName;
                            }

                            validateFileName(exisitingFilenames, finalFileName, study);

                            File tempUploadedFile = null;
                            try {
                                tempUploadedFile = FileUtil.createTempFile(tempDirectory, finalFileName);
                            } catch (Exception ex) {
                                Logger.getLogger(MediaResourceManagerImpl.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            tempOutStream = new FileOutputStream(tempUploadedFile);

                            byte[] dataBuffer = new byte[8192];
                            int i = 0;

                            while ((i = ziStream.read(dataBuffer)) > 0) {
                                tempOutStream.write(dataBuffer, 0, i);
                                tempOutStream.flush();
                            }

                            tempOutStream.close();

                            // We now have the unzipped file saved in the upload directory;

                            StudyFileEditBean tempFileBean = new StudyFileEditBean(tempUploadedFile, studyService.generateFileSystemNameSequence(), study);
                            tempFileBean.setSizeFormatted(tempUploadedFile.length());

                            // And, if this file was in a legit (non-null) directory, 
                            // we'll use its name as the file category: 

                            if (dirName != null) {
                                tempFileBean.getFileMetadata().setCategory(dirName);
                            }

                            fbList.add(tempFileBean);

                        } else {
                            logger.info("directory found: " + zEntry.getName());
                        }
                    }
                } catch (IOException ex) {
                    logger.info("Problem getting zip entry");
                } finally {
                    if (!uploadDir.delete()) {
                        logger.info("Unable to delete " + uploadDir.getAbsolutePath());
                    }
                }
                StudyFileServiceLocal studyFileService;
                try {
                    studyFileService = (StudyFileServiceLocal) ctx.lookup("java:comp/env/studyFileService");
                } catch (NamingException ex) {
                    throw new SwordServerException("problem looking up studyFileService");
                }
                studyFileService.addFiles(study.getLatestVersion(), fbList, vdcUser);
                if (!uploadDir.delete()) {
                    logger.info("Unable to delete " + uploadDir.getAbsolutePath());
                }
                /**
                 * @todo: when should we release the study?
                 */
//        studyService.setReleased(studyId);
                ReceiptGenerator receiptGenerator = new ReceiptGenerator();
                String baseUrl = urlManager.getHostnamePlusBaseUrlPath(uri);
                DepositReceipt depositReceipt = receiptGenerator.createReceipt(baseUrl, study);
                return depositReceipt;
            } else {
                throw new SwordError("user " + vdcUser.getUserName() + " is not authorized to modify study with global ID " + study.getGlobalId());
            }
        } else {
            throw new SwordError("Unable to determine target type or identifier from url: " + uri);
        }
    }

    // copied from AddFilesPage
    private void validateFileName(List<String> existingFilenames, String fileName, Study study) throws SwordError {
        if (fileName.contains("\\")
                || fileName.contains("/")
                || fileName.contains(":")
                || fileName.contains("*")
                || fileName.contains("?")
                || fileName.contains("\"")
                || fileName.contains("<")
                || fileName.contains(">")
                || fileName.contains("|")
                || fileName.contains(";")
                || fileName.contains("#")) {
            throw new SwordError("Invalid File Name - cannot contain any of the following characters: \\ / : * ? \" < > | ; . Filename was '" + fileName + "'");
        }
        if (existingFilenames.contains(fileName)) {
            throw new SwordError("Filename " + fileName + " already exists in study " + study.getGlobalId());
        }
    }
}
