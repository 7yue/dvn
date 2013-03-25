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
package edu.harvard.iq.dvn.core.web;

import edu.harvard.iq.dvn.core.admin.KeywordSearchServiceBean;
import edu.harvard.iq.dvn.core.index.IndexServiceLocal;
import edu.harvard.iq.dvn.core.index.ResultsWithFacets;
import edu.harvard.iq.dvn.core.index.SearchTerm;
import edu.harvard.iq.dvn.core.study.StudyServiceLocal;
import edu.harvard.iq.dvn.core.study.StudyVersion;
import edu.harvard.iq.dvn.core.study.VariableServiceLocal;
import edu.harvard.iq.dvn.core.web.common.VDCBaseBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import edu.harvard.iq.dvn.core.vdc.VDCCollectionServiceLocal;
import edu.harvard.iq.dvn.core.vdc.VDCServiceLocal;
import java.util.logging.Logger;
import javax.faces.bean.ViewScoped;
import javax.inject.Named;
import org.apache.lucene.facet.search.results.FacetResult;
import org.apache.lucene.facet.search.results.FacetResultNode;
import org.apache.lucene.facet.taxonomy.CategoryPath;


@Named ("BasicSearchFragment")
@ViewScoped
public class BasicSearchFragment extends VDCBaseBean implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dvn.core.web.BasicSearchFragment");
    @EJB
    IndexServiceLocal      indexService;
    @EJB
    VariableServiceLocal varService;
    @EJB
    StudyServiceLocal studyService;
    @EJB
    VDCServiceLocal vdcService;
    @EJB
    VDCCollectionServiceLocal vdcCollectionService;
    @EJB
    KeywordSearchServiceBean keywordSearchServiceBean;
    private String searchValue;
    private String searchField;
    private List <String> keywordSearchTerms;
    
    public void init () {
        super.init();
        if ( getVDCRequestBean().getCurrentVDC() == null ) {
            keywordSearchTerms = keywordSearchServiceBean.findAll();
            searchValue = "Enter keywords to search this Dataverse Network";
        } else {
            searchValue = "Enter keywords to search this Dataverse";
        }
    }

    public String search_action() {
        searchField = (searchField == null) ? "any" : searchField; // default searchField, in case no dropdown

        List searchTerms    = new ArrayList();
        SearchTerm st       = new SearchTerm();
        st.setFieldName( searchField );
        st.setValue( searchValue );
        StudyListing sl = getSearchResult(st);
        String studyListingIndex = StudyListing.addToStudyListingMap(sl, getSessionMap());
        return "/StudyListingPage.xhtml?faces-redirect=true&studyListingIndex=" + studyListingIndex + "&vdcId=" + getVDCRequestBean().getCurrentVDCId();
    }

    public String facet_search() {
        searchField = (searchField == null) ? "any" : searchField; // default searchField, in case no dropdown

        List searchTerms    = new ArrayList();
        SearchTerm st       = new SearchTerm();
        st.setFieldName( searchField );
        st.setValue( searchValue );
        StudyListing sl = getSearchResultsWithFacets(st);
        String studyListingIndex = StudyListing.addToStudyListingMap(sl, getSessionMap());
        return "/StudyListingPage.xhtml?faces-redirect=true&studyListingIndex=" + studyListingIndex + "&vdcId=" + getVDCRequestBean().getCurrentVDCId();
    }

    public String keywordSearch_action(String searchIn){
        System.out.print("searchIn: "+ searchIn);
        searchField = (searchField == null) ? "any" : searchField; // default searchField, in case no dropdown       
        SearchTerm st  = new SearchTerm();
        st.setFieldName( searchField );
        st.setValue( searchIn );
        StudyListing sl = getSearchResult(st);
        String studyListingIndex = StudyListing.addToStudyListingMap(sl, getSessionMap());
        return "/StudyListingPage.xhtml?faces-redirect=true&studyListingIndex=" + studyListingIndex + "&vdcId=" + getVDCRequestBean().getCurrentVDCId();
    }

    public String keywordSearchWithFacets_action(String searchIn) {
        System.out.print("searchIn: " + searchIn);
        searchField = (searchField == null) ? "any" : searchField; // default searchField, in case no dropdown       
        SearchTerm st = new SearchTerm();
        st.setFieldName(searchField);
        st.setValue(searchIn);
        StudyListing sl = getSearchResultsWithFacets(st);
        String studyListingIndex = StudyListing.addToStudyListingMap(sl, getSessionMap());
        return "/StudyListingPage.xhtml?faces-redirect=true&studyListingIndex=" + studyListingIndex + "&vdcId=" + getVDCRequestBean().getCurrentVDCId();
    }


    private StudyListing getSearchResult(SearchTerm st){
        
        List searchTerms    = new ArrayList();
        searchTerms.add(st);
        List studies        = new ArrayList();
        Map variableMap     = new HashMap();
        Map versionMap = new HashMap();
        List displayVersionList = new ArrayList();

        if ( searchField.equals("variable") ) {
            List variables  = indexService.searchVariables(getVDCRequestBean().getCurrentVDC(), st);
            varService.determineStudiesFromVariables(variables, studies, variableMap);

        } else {
            studies = indexService.search(getVDCRequestBean().getCurrentVDC(), searchTerms);
        }
        if (searchField.equals("any")) {
            List<Long> versionIds = indexService.searchVersionUnf(getVDCRequestBean().getCurrentVDC(), st.getValue());
            Iterator iter = versionIds.iterator();
            Long studyId = null;
            while (iter.hasNext()) {
                Long vId = (Long) iter.next();
                StudyVersion sv = null;
                try {
                    sv = studyService.getStudyVersionById(vId);
                    studyId = sv.getStudy().getId();
                    List<StudyVersion> svList = (List<StudyVersion>) versionMap.get(studyId);
                    if (svList == null) {
                        svList = new ArrayList<StudyVersion>();
                    }
                    svList.add(sv);
                    if (!studies.contains(studyId)) {
                        displayVersionList.add(studyId);
                        studies.add(studyId);
                    }
                    versionMap.put(studyId, svList);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

        }


        StudyListing sl = new StudyListing(StudyListing.SEARCH);
        sl.setVdcId(getVDCRequestBean().getCurrentVDCId());
        sl.setStudyIds(studies);
        sl.setSearchTerms(searchTerms);
        sl.setVariableMap(variableMap);
        sl.setVersionMap(versionMap);
        sl.setDisplayStudyVersionsList(displayVersionList);

        
        return sl;
        
    }

    public StudyListing getSearchResultsWithFacets(SearchTerm st) {

        List searchTerms = new ArrayList();
        searchTerms.add(st);
        List studies = new ArrayList();
        Map variableMap = new HashMap();
        Map versionMap = new HashMap();
        List displayVersionList = new ArrayList();
        ResultsWithFacets resultsWithFacets = null;

        // when is this true?
        if (searchField.equals("variable")) {
            List variables = indexService.searchVariables(getVDCRequestBean().getCurrentVDC(), st);
            // how will facetResults get set?
            varService.determineStudiesFromVariables(variables, studies, variableMap);

        } else {
            resultsWithFacets = indexService.searchwithFacets(getVDCRequestBean().getCurrentVDC(), searchTerms);
            studies = resultsWithFacets.getMatchIds();
            List<FacetResult> facetResults = resultsWithFacets.getResultList();
        }

        if (searchField.equals("any")) {
            List<Long> versionIds = indexService.searchVersionUnf(getVDCRequestBean().getCurrentVDC(), searchValue);
            Iterator iter = versionIds.iterator();
            Long studyId = null;
            while (iter.hasNext()) {
                Long vId = (Long) iter.next();
                StudyVersion sv = null;
                try {
                    sv = studyService.getStudyVersionById(vId);
                    studyId = sv.getStudy().getId();
                    List<StudyVersion> svList = (List<StudyVersion>) versionMap.get(studyId);
                    if (svList == null) {
                        svList = new ArrayList<StudyVersion>();
                    }
                    svList.add(sv);
                    if (!studies.contains(studyId)) {
                        displayVersionList.add(studyId);
                        studies.add(studyId);
                    }
                    versionMap.put(studyId, svList);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

        }


        StudyListing sl = new StudyListing(StudyListing.SEARCH);
        sl.setVdcId(getVDCRequestBean().getCurrentVDCId());
        sl.setStudyIds(studies);
        sl.setSearchTerms(searchTerms);
        sl.setResultsWithFacets(resultsWithFacets);
        sl.setVariableMap(variableMap);
        sl.setVersionMap(versionMap);
        sl.setDisplayStudyVersionsList(displayVersionList);

        return sl;
    }
 

 public void setSearchField(String searchField) {
        this.searchField = searchField;
    }

    public void setSearchValue(String searchValue) {
        this.searchValue = searchValue;
    }

    public String getSearchField() {
        return searchField;
    }

    public String getSearchValue() {
        return searchValue;
    }
    
    public List<String> getSearchTerms() {
        return keywordSearchTerms;
    }

    public void setSearchTerms(List<String> searchTerms) {
        this.keywordSearchTerms = searchTerms;
    }
}
