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
import edu.harvard.iq.dvn.core.vdc.VDC;
import edu.harvard.iq.dvn.core.vdc.VDCServiceLocal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ServiceDocument;
import org.swordapp.server.ServiceDocumentManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordCollection;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.SwordWorkspace;

public class ServiceDocumentManagerImpl implements ServiceDocumentManager {

    private static final Logger logger = Logger.getLogger(ServiceDocumentManagerImpl.class.getCanonicalName());
    @EJB
    VDCServiceLocal vdcService;

    @Override
    public ServiceDocument getServiceDocument(String sdUri, AuthCredentials authCredentials, SwordConfiguration config)
            throws SwordError, SwordServerException, SwordAuthException {

        SwordAuth swordAuth = new SwordAuth();
        VDCUser vdcUser = swordAuth.auth(authCredentials);

        List<VDC> vdcList = vdcService.getUserVDCs(vdcUser.getId());

        if (vdcList.size() != 1) {
            String msg = "accounts used to look up a Journal Dataverse should find a single dataverse, not " + vdcList.size();
            logger.info(msg);
            throw new SwordError(msg);
        }

        if (vdcList.get(0) != null) {
            VDC journalDataverse = vdcList.get(0);
            String dvAlias = journalDataverse.getAlias();
            ServiceDocument service = new ServiceDocument();
            SwordWorkspace swordWorkspace = new SwordWorkspace();
            swordWorkspace.setTitle(journalDataverse.getVdcNetwork().getName());
            SwordCollection swordCollection = new SwordCollection();
            swordCollection.setTitle(journalDataverse.getName());
            try {
                URI u = new URI(sdUri);
                int port = u.getPort();
                String hostName = System.getProperty("dvn.inetAddress");
                // hard coding https on purpose
                swordCollection.setHref("https://" + hostName + ":" + port + "/dvn/api/data-deposit/swordv2/collection/dataverse/" + dvAlias);
                swordWorkspace.addCollection(swordCollection);
                service.addWorkspace(swordWorkspace);
                service.setMaxUploadSize(config.getMaxUploadSize());
                return service;
            } catch (URISyntaxException ex) {
                String msg = "problem with URL ( " + sdUri + " ): " + ex.getMessage();
                logger.info(msg);
                throw new SwordError(msg);
            }
        } else {
            String msg = "could not retrieve Journal Dataverse";
            logger.info(msg);
            throw new SwordError(msg);
        }
    }
}
