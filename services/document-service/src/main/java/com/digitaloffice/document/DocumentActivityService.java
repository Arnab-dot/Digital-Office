package com.digitaloffice.document;
import org.springframework.stereotype.Service; import java.util.*;
@Service public class DocumentActivityService {
 private final DocumentActivityEventRepository events; public DocumentActivityService(DocumentActivityEventRepository e){events=e;}
 public DocumentActivityEvent recordEvent(UUID project,UUID actor,DocumentActivityActionType type,UUID subject,String subjectType,String metadata){return events.save(new DocumentActivityEvent(project,actor,type,subject,subjectType,metadata));}
 public List<DocumentActivityEvent> byProject(UUID id){return events.findByProjectIdOrderByEventAtDesc(id);}
}
