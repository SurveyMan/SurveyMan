package system.mturk;

import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.AssignmentStatus;
import qc.QC;
import qc.QCActions;
import survey.SurveyResponse;

import java.util.List;

class QCAction {
    public static boolean addAsValidResponse(QCActions[] actions, Assignment a, SurveyResponse sr) {
        boolean valid = false;
        for (QCActions action : actions) {
            synchronized (ResponseManager.service) {
                switch (action) {
                    case REJECT:
                        System.out.println("REJECT");
                        //ResponseManager.service.rejectAssignment(a.getAssignmentId(), QC.BOT);
                        System.out.println("(but actually approve for now) - REMOVE ME LATER.");
                        ResponseManager.service.approveAssignment(a.getAssignmentId(), "");
                        valid = false;
                        break;
                    case BLOCK:
                        System.out.println("BLOCK");
                        ResponseManager.service.blockWorker(a.getWorkerId(), QC.BOT);
                        valid = false;
                        break;
                    case APPROVE:
                        System.out.println("APPROVE");
                        if (a.getAssignmentStatus().equals(AssignmentStatus.Submitted)) {
                            ResponseManager.service.approveAssignment(a.getAssignmentId(), "Thanks.");
                            valid = true;
                        } else valid = false;
                        break;
                }
            }
        }
        return valid;
    }
}
