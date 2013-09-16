package system.mturk;

import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.AssignmentStatus;
import qc.QC;
import qc.QCActions;
import survey.SurveyResponse;

import java.util.List;

class QCAction {
    public static boolean addAsValidResponse(QCActions[] actions, Assignment a) {
        boolean valid = false;
        for (QCActions action : actions) {
            switch (action) {
                case REJECT:
                    ResponseManager.service.rejectAssignment(a.getAssignmentId(), QC.BOT);
                    valid = false;
                    break;
                case BLOCK:
                    ResponseManager.service.blockWorker(a.getWorkerId(), QC.BOT);
                    valid = false;
                    break;
                case APPROVE:
                    if (a.getAssignmentStatus().equals(AssignmentStatus.Submitted)) {
                        ResponseManager.service.approveAssignment(a.getAssignmentId(), "Thanks.");
                        valid = true;
                    } else valid = false;
                    break;
            }
        }
        return valid;
    }
}
