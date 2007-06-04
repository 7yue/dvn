
package edu.harvard.hmdc.vdcnet.study;

import edu.harvard.hmdc.vdcnet.jaxb.ddi20.CodeBook;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.ejb.Local;
import org.w3c.dom.Node;

/**
 * This is the business interface for EditStudyService enterprise bean.
 */
@Local
public interface EditStudyService {
    public void setStudy( Long studyId);
    public void newStudy(Long vdcId, Long userId);
    public void cancel();
    public void save(Long vdcId, Long userId);
    public Study getStudy();
    public void deleteStudy();
    public void removeCollectionElement(Collection coll, Object elem);

    java.util.List getCurrentFiles();

    void setCurrentFiles(List currentFiles);

    java.util.List getNewFiles();

    void setNewFiles(List newFiles);

    void removeCollectionElement(Iterator iter, Object elem);
    
    public HashMap getStudyMap();
    public void setStudyMap(HashMap studyMap);

    void setIngestEmail(String ingestEmail);
    
    public boolean isNewStudy();

    public void importHarvestStudy(Node xmlNode);
    public void importLegacyStudy(File xmlFile); 
    public void importStudy(Node xmlNode, boolean checkRestrictions, boolean generateStudyId, boolean allowUpdates);
    public void importStudy(File xmlFile, boolean checkRestrictions, boolean generateStudyId, boolean allowUpdates);
//    public void importStudy(CodeBook _cb, boolean checkRestrictions, boolean generateStudyId);
   

    void retrieveFilesAndSave(Long vdcId, Long userId );
}
