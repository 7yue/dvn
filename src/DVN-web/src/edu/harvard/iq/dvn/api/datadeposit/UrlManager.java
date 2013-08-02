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

import edu.harvard.iq.dvn.core.study.Study;
import edu.harvard.iq.dvn.core.study.StudyServiceLocal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import org.apache.commons.lang.StringUtils;
import org.swordapp.server.SwordError;

public class UrlManager {

    private static final Logger logger = Logger.getLogger(UrlManager.class.getCanonicalName());
    @EJB
    StudyServiceLocal studyService;
    String originalUrl;
    SwordConfigurationImpl swordConfiguration = new SwordConfigurationImpl();
    String servlet;
    String targetType;
    String targetIdentifier;
    int port;

    void processUrl(String url) throws SwordError {
        this.originalUrl = url;
        URI javaNetUri;
        try {
            javaNetUri = new URI(url);
        } catch (URISyntaxException ex) {
            throw new SwordError("Invalid URL syntax: " + url);
        }
        this.port = javaNetUri.getPort();
        String[] urlPartsArray = javaNetUri.getPath().split("/");
        List<String> urlParts = Arrays.asList(urlPartsArray);
        String dataDepositApiBasePath;
        try {
            List<String> dataDepositApiBasePathParts;
            //             1 2   3   4            5  6       7          8         9
            // for example: /dvn/api/data-deposit/v1/swordv2/collection/dataverse/sword
            dataDepositApiBasePathParts = urlParts.subList(0, 6);
            dataDepositApiBasePath = StringUtils.join(dataDepositApiBasePathParts, "/");
        } catch (IndexOutOfBoundsException ex) {
            throw new SwordError("Error processing URL: " + url);
        }
        if (!dataDepositApiBasePath.equals(swordConfiguration.getBaseUrlPath())) {
            throw new SwordError(dataDepositApiBasePath + " found but " + swordConfiguration.getBaseUrlPath() + " expected");
        }
        try {
            this.servlet = urlParts.get(6);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new SwordError("Unable to determine servlet path from URL: " + url);
        }
        List<String> targetTypeAndIdentifier;
        try {
            //               6          7         8
            // for example: /collection/dataverse/sword
            targetTypeAndIdentifier = urlParts.subList(7, urlParts.size());
        } catch (IndexOutOfBoundsException ex) {
            throw new SwordError("No target components specified in URL: " + url);
        }
        this.targetType = determineTargetType(targetTypeAndIdentifier);
        if (targetType != null) {
            if (targetType.equals("dataverse")) {
                String dvAlias;
                try {
                    dvAlias = targetTypeAndIdentifier.get(1);
                } catch (IndexOutOfBoundsException ex) {
                    throw new SwordError("No dataverse alias provided in url: " + url);
                }
                this.targetIdentifier = dvAlias;
            } else if (targetType.equals("study")) {
                String globalId = getStudyGlobalId(targetTypeAndIdentifier);
                logger.info("study found: " + globalId);
                this.targetIdentifier = globalId;
            } else {
                throw new SwordError("unsupported target type: " + targetType);
            }
        } else {
            throw new SwordError("Unable to determine target type from url: " + url);
        }
        System.out.println("target type: " + targetType);
        System.out.println("target identifier: " + targetIdentifier);
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getServlet() {
        return servlet;
    }

    public void setServlet(String servlet) {
        this.servlet = servlet;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public void setTargetIdentifier(String targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    private String determineTargetType(List<String> targetTypeAndIdentifier) {
        if (!targetTypeAndIdentifier.isEmpty()) {
            String index0 = targetTypeAndIdentifier.get(0);
            System.out.println("index0: " + index0);
            if (index0.equals("dataverse")) {
                return "dataverse";
            } else {
                String globalId = getStudyGlobalId(targetTypeAndIdentifier);
                if (globalId != null) {
                    return "study";
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    private String getStudyGlobalId(List<String> targetTypeAndIdentifier) {
        String potentialGlobalId = StringUtils.join(targetTypeAndIdentifier, "/");
        logger.info("potential globalId: " + potentialGlobalId);
        Study study = null;
        try {
            logger.info("running studyService.getStudyByGlobalId");
            study = studyService.getStudyByGlobalId(potentialGlobalId);
        } catch (Exception ex) {
            // shouldn't studyService.getStudyByGlobalId throw some sort of exception?
            logger.info("problem running studyService.getStudyByGlobalId: " + ex.getMessage());
            return null;
        }
        if (study != null) {
            return study.getGlobalId();
        } else {
            return null;
        }
    }
}
